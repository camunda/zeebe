/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.data.generation.onboarding;

import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_DEFINITION_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_INSTANCE_MULTI_ALIAS;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
import io.camunda.optimize.service.db.es.OptimizeElasticsearchClient;
import io.camunda.optimize.service.db.es.schema.ElasticSearchMetadataService;
import io.camunda.optimize.service.db.es.schema.ElasticSearchSchemaManager;
import io.camunda.optimize.service.db.es.schema.index.ProcessDefinitionIndexES;
import io.camunda.optimize.service.db.es.schema.index.ProcessInstanceIndexES;
import io.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex;
import io.camunda.optimize.service.exceptions.DataGenerationException;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.ConfigurationServiceBuilder;
import io.camunda.optimize.service.util.configuration.DatabaseType;
import io.camunda.optimize.service.util.mapper.CustomOffsetDateTimeDeserializer;
import io.camunda.optimize.service.util.mapper.CustomOffsetDateTimeSerializer;
import io.camunda.optimize.upgrade.es.ElasticsearchHighLevelRestClientBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Slf4j
public class OnboardingDataGenerator {

  private static final String CUSTOMER_ONBOARDING_DEFINITION =
      "onboarding-data/customer-onboarding-definition.json";
  private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
  private final OptimizeElasticsearchClient elasticsearchClient;
  private final ElasticSearchSchemaManager elasticSearchSchemaManager;
  private final OptimizeIndexNameService optimizeIndexNameService;

  public OnboardingDataGenerator() {
    final ConfigurationService configurationService =
        ConfigurationServiceBuilder.createDefaultConfiguration();
    this.optimizeIndexNameService =
        new OptimizeIndexNameService(configurationService, DatabaseType.ELASTICSEARCH);
    ElasticSearchMetadataService elasticsearchMetadataService =
        new ElasticSearchMetadataService(OBJECT_MAPPER);
    this.elasticSearchSchemaManager =
        new ElasticSearchSchemaManager(
            elasticsearchMetadataService,
            configurationService,
            optimizeIndexNameService,
            List.of(new ProcessDefinitionIndexES()));
    this.elasticsearchClient =
        new OptimizeElasticsearchClient(
            ElasticsearchHighLevelRestClientBuilder.build(configurationService),
            optimizeIndexNameService,
            OBJECT_MAPPER);
    elasticSearchSchemaManager.initializeSchema(elasticsearchClient);
  }

  public void executeDataGeneration(Map<String, OnboardingDataGeneratorParameters> arguments) {
    addCustomerOnboardingDefinitionToElasticSearch();
    for (Map.Entry<String, OnboardingDataGeneratorParameters> argument : arguments.entrySet()) {
      ProcessInstanceDto processInstanceDto = readProcessInstanceJson(argument.getValue());
      if (processInstanceDto != null) {
        addProcessInstanceCopiesToElasticSearch(
            Integer.parseInt(argument.getValue().getNumberOfProcessInstances()),
            processInstanceDto);
      } else {
        throw new DataGenerationException(
            "The given json file does not contain a process instance.");
      }
    }
    closeEsConnection();
  }

  private void addCustomerOnboardingDefinitionToElasticSearch() {
    try {
      ClassLoader classLoader = OnboardingDataGeneratorMain.class.getClassLoader();
      URL resource = classLoader.getResource(CUSTOMER_ONBOARDING_DEFINITION);
      if (resource != null) {
        File file = new File(resource.getFile());
        ProcessDefinitionOptimizeDto processDefinitionDto =
            OBJECT_MAPPER.readValue(file, ProcessDefinitionOptimizeDto.class);
        if (processDefinitionDto != null) {
          String json = OBJECT_MAPPER.writeValueAsString(processDefinitionDto);
          IndexRequest request =
              new IndexRequest(
                      optimizeIndexNameService.getOptimizeIndexAliasForIndex(
                          PROCESS_DEFINITION_INDEX_NAME))
                  .id(processDefinitionDto.getId())
                  .source(json, XContentType.JSON);
          elasticsearchClient.index(request);
        } else {
          throw new DataGenerationException(
              "Could not read process definition json file in path: "
                  + CUSTOMER_ONBOARDING_DEFINITION);
        }
      } else {
        throw new DataGenerationException(
            "The json file "
                + CUSTOMER_ONBOARDING_DEFINITION
                + " does not contain a "
                + "process definition.");
      }
    } catch (IOException e) {
      throw new DataGenerationException("Unable to add a process definition to elasticsearch", e);
    }
  }

  private void addProcessInstanceCopiesToElasticSearch(
      final int amountOfProcessInstances, final ProcessInstanceDto processInstanceDto) {
    BulkRequest bulkRequest = new BulkRequest();
    elasticSearchSchemaManager.createOrUpdateOptimizeIndex(
        elasticsearchClient,
        new ProcessInstanceIndexES(processInstanceDto.getProcessDefinitionKey()),
        Collections.singleton(PROCESS_INSTANCE_MULTI_ALIAS));
    try {
      for (int counter = 0; counter < amountOfProcessInstances; counter++) {
        String processInstanceId = UUID.randomUUID().toString();
        processInstanceDto.setProcessInstanceId(processInstanceId);
        processInstanceDto
            .getFlowNodeInstances()
            .forEach(
                flowNodeInstanceDto -> flowNodeInstanceDto.setProcessInstanceId(processInstanceId));
        String json = OBJECT_MAPPER.writeValueAsString(processInstanceDto);
        IndexRequest request =
            new IndexRequest(
                    ProcessInstanceIndex.constructIndexName(
                        processInstanceDto.getProcessDefinitionKey()))
                .id(processInstanceDto.getProcessInstanceId())
                .source(json, XContentType.JSON);
        bulkRequest.add(request);
      }
      elasticsearchClient.bulk(bulkRequest);
    } catch (IOException e) {
      throw new DataGenerationException("Unable to add process instances to elasticsearch", e);
    }
  }

  private static ObjectMapper createObjectMapper() {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(OPTIMIZE_DATE_FORMAT);
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(
        OffsetDateTime.class, new CustomOffsetDateTimeSerializer(dateTimeFormatter));
    javaTimeModule.addDeserializer(
        OffsetDateTime.class, new CustomOffsetDateTimeDeserializer(dateTimeFormatter));

    return Jackson2ObjectMapperBuilder.json()
        .modules(javaTimeModule)
        .featuresToDisable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
            DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE,
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .featuresToEnable(JsonParser.Feature.ALLOW_COMMENTS, SerializationFeature.INDENT_OUTPUT)
        .build();
  }

  @SneakyThrows
  private void closeEsConnection() {
    elasticsearchClient.close();
  }

  private ProcessInstanceDto readProcessInstanceJson(
      final OnboardingDataGeneratorParameters onboardingDataGeneratorParameter) {
    ProcessInstanceDto processInstanceDto;
    try {
      ClassLoader classLoader = OnboardingDataGeneratorMain.class.getClassLoader();
      URL resource = classLoader.getResource(onboardingDataGeneratorParameter.getFilePath());
      if (resource != null) {
        File file = new File(resource.getFile());
        processInstanceDto = OBJECT_MAPPER.readValue(file, ProcessInstanceDto.class);
      } else {
        throw new DataGenerationException(
            "Could not read process instance json file in path: "
                + onboardingDataGeneratorParameter.getFilePath());
      }
    } catch (IOException e) {
      throw new DataGenerationException(
          "Could not read process instance json file in path: "
              + onboardingDataGeneratorParameter.getFilePath());
    }
    return processInstanceDto;
  }
}
