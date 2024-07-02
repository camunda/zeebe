/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.rest.providers.BeanConstraintViolationExceptionHandler.THE_REQUEST_BODY_WAS_INVALID;
import static io.camunda.optimize.service.db.DatabaseConstants.EXTERNAL_PROCESS_VARIABLE_INDEX_NAME;
import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableDto;
import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import io.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
import io.camunda.optimize.dto.optimize.rest.ValidationErrorResponseDto;
import io.camunda.optimize.jetty.IngestionQoSFilter;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class ExternalVariableIngestionRestIT extends AbstractPlatformIT {

  @BeforeEach
  public void before() {
    dateFreezer().freezeDateAndReturn();
  }

  @Test
  public void ingestExternalVariables() {
    // given
    final List<ExternalProcessVariableRequestDto> variables =
        IntStream.range(0, 10)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();

    // when
    final Response response = ingestionClient.ingestVariablesAndReturnResponse(variables);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExternalVariablesArePersisted(variables);
  }

  @SneakyThrows
  @Test
  public void ingestExternalObjectVariable() {
    // given
    final Map<String, Object> objectVariable = new HashMap<>();
    objectVariable.put("property1", 1);
    objectVariable.put("property2", "2");
    ExternalProcessVariableRequestDto objectVariableDto =
        new ExternalProcessVariableRequestDto()
            .setId("id1")
            .setProcessInstanceId("instanceId")
            .setProcessDefinitionKey("defKey")
            .setName("objectVar")
            .setType(VariableType.OBJECT)
            .setValue(new ObjectMapper().writeValueAsString(objectVariable))
            .setSerializationDataFormat(MediaType.APPLICATION_JSON);
    final List<ExternalProcessVariableRequestDto> variablesToIngest =
        Collections.singletonList(objectVariableDto);

    // when
    final Response response = ingestionClient.ingestVariablesAndReturnResponse(variablesToIngest);

    // then
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExternalVariablesArePersisted(variablesToIngest);
  }

  @Test
  public void ingestExternalVariables_ingestSameBatchTwice() {
    // given
    final List<ExternalProcessVariableRequestDto> variables =
        IntStream.range(0, 10)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();
    final OffsetDateTime ingestionTimestamp1 = LocalDateUtil.getCurrentDateTime();
    final OffsetDateTime ingestionTimestamp2 = LocalDateUtil.getCurrentDateTime().minusDays(1);

    // when
    final Response response1 = ingestionClient.ingestVariablesAndReturnResponse(variables);
    dateFreezer().dateToFreeze(ingestionTimestamp2).freezeDateAndReturn();
    final Response response2 = ingestionClient.ingestVariablesAndReturnResponse(variables);

    // then
    assertThat(response1.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExternalVariablesArePersisted(
        Map.of(
            ingestionTimestamp1.toInstant().toEpochMilli(),
            variables,
            ingestionTimestamp2.toInstant().toEpochMilli(),
            variables));
  }

  @Test
  public void ingestExternalVariables_duplicateVariableInBatch() {
    // given
    final List<ExternalProcessVariableRequestDto> variables =
        IntStream.range(0, 2)
            .mapToObj(
                i ->
                    ingestionClient
                        .createPrimitiveExternalVariable()
                        .setId("sameId")
                        .setValue("value" + i))
            .toList();
    final OffsetDateTime ingestionTimestamp = LocalDateUtil.getCurrentDateTime();

    // when
    final Response response = ingestionClient.ingestVariablesAndReturnResponse(variables);

    // then only the latest value in the batch is persisted
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExternalVariablesArePersisted(
        Map.of(ingestionTimestamp.toInstant().toEpochMilli(), variables.subList(1, 2)));
  }

  @Test
  public void ingestExternalVariables_ingestTwoDifferentBatches() {
    // given
    final List<ExternalProcessVariableRequestDto> variables1 =
        IntStream.range(0, 10)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();
    final List<ExternalProcessVariableRequestDto> variables2 =
        IntStream.range(20, 30)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();
    final OffsetDateTime ingestionTimestamp1 = LocalDateUtil.getCurrentDateTime();
    final OffsetDateTime ingestionTimestamp2 = LocalDateUtil.getCurrentDateTime().minusDays(1);

    // when
    final Response response1 = ingestionClient.ingestVariablesAndReturnResponse(variables1);
    dateFreezer().dateToFreeze(ingestionTimestamp2).freezeDateAndReturn();
    final Response response2 = ingestionClient.ingestVariablesAndReturnResponse(variables2);

    // then
    assertThat(response1.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExternalVariablesArePersisted(
        Map.of(
            ingestionTimestamp1.toInstant().toEpochMilli(),
            variables1,
            ingestionTimestamp2.toInstant().toEpochMilli(),
            variables2));
  }

  @Test
  public void ingestExternalVariables_emptyBatch() {
    // when
    final Response response =
        ingestionClient.ingestVariablesAndReturnResponse(Collections.emptyList());

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertThat(getAllStoredExternalProcessVariables()).isEmpty();
  }

  @Test
  public void ingestExternalVariable_customAccessToken() {
    // given
    final ExternalProcessVariableRequestDto variable =
        ingestionClient.createPrimitiveExternalVariable();

    final String accessToken = "aToken";
    embeddedOptimizeExtension
        .getConfigurationService()
        .getOptimizeApiConfiguration()
        .setAccessToken(accessToken);

    // when
    final Response response =
        ingestionClient.ingestVariablesAndReturnResponse(
            Collections.singletonList(variable), accessToken);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExternalVariablesArePersisted(Collections.singletonList(variable));
  }

  @Test
  public void ingestExternalVariable_accessTokenUsingBearerScheme() {
    // given
    final ExternalProcessVariableRequestDto variable =
        ingestionClient.createPrimitiveExternalVariable();

    final String accessToken = "aToken";
    embeddedOptimizeExtension
        .getConfigurationService()
        .getOptimizeApiConfiguration()
        .setAccessToken(accessToken);

    // when
    final Response ingestResponse =
        ingestionClient.ingestVariablesAndReturnResponse(
            Collections.singletonList(variable), accessToken);

    // then
    assertThat(ingestResponse.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExternalVariablesArePersisted(Collections.singletonList(variable));
  }

  @Test
  public void ingestExternalVariable_incorrectToken() {
    // given
    final ExternalProcessVariableRequestDto variable =
        ingestionClient.createPrimitiveExternalVariable();

    // when
    final Response response =
        ingestionClient.ingestVariablesAndReturnResponse(
            Collections.singletonList(variable), "falseToken");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
    assertThat(getAllStoredExternalProcessVariables()).isEmpty();
  }

  @Test
  public void ingestExternalVariable_invalidVariable() {
    // given
    final ExternalProcessVariableRequestDto invalidVariable =
        new ExternalProcessVariableRequestDto()
            .setId("")
            .setName("  ")
            .setValue("value")
            .setType(null)
            .setProcessInstanceId("")
            .setProcessDefinitionKey(" ");

    // when
    final ValidationErrorResponseDto response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildIngestExternalVariables(
                Collections.singletonList(invalidVariable),
                ingestionClient.getVariableIngestionToken())
            .execute(ValidationErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());

    // then
    assertThat(response.getErrorMessage()).isEqualTo(THE_REQUEST_BODY_WAS_INVALID);
    assertThat(response.getValidationErrors())
        .hasSize(5)
        .extracting(ValidationErrorResponseDto.ValidationError::getProperty)
        .map(property -> property.split("\\.")[1])
        .containsExactlyInAnyOrder(
            ExternalProcessVariableRequestDto.Fields.id,
            ExternalProcessVariableRequestDto.Fields.name,
            ExternalProcessVariableRequestDto.Fields.type,
            ExternalProcessVariableRequestDto.Fields.processInstanceId,
            ExternalProcessVariableRequestDto.Fields.processDefinitionKey);
    assertThat(response.getValidationErrors())
        .extracting(ValidationErrorResponseDto.ValidationError::getErrorMessage)
        .doesNotContainNull();
    assertThat(getAllStoredExternalProcessVariables()).isEmpty();
  }

  @Test
  public void ingestExternalVariable_invalidAndValidVariables() {
    // given
    final ExternalProcessVariableRequestDto validVariable =
        ingestionClient.createPrimitiveExternalVariable();
    final ExternalProcessVariableRequestDto invalidVariable =
        ingestionClient.createPrimitiveExternalVariable();
    invalidVariable.setId(null);

    // when
    final ValidationErrorResponseDto response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildIngestExternalVariables(
                List.of(validVariable, invalidVariable),
                ingestionClient.getVariableIngestionToken())
            .execute(ValidationErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());

    // then
    assertThat(response.getErrorMessage()).isEqualTo(THE_REQUEST_BODY_WAS_INVALID);
    assertThat(response.getValidationErrors())
        .hasSize(1)
        .extracting(ValidationErrorResponseDto.ValidationError::getProperty)
        .containsExactly("element[1]." + ExternalProcessVariableRequestDto.Fields.id);
    assertThat(response.getValidationErrors())
        .extracting(ValidationErrorResponseDto.ValidationError::getErrorMessage)
        .doesNotContainNull();
    assertThat(getAllStoredExternalProcessVariables()).isEmpty();
  }

  @Test
  public void ingestExternalVariable_nullValueAllowed() {
    // given
    final ExternalProcessVariableRequestDto variable =
        ingestionClient.createPrimitiveExternalVariable();
    variable.setValue(null);

    // when
    final Response response =
        ingestionClient.ingestVariablesAndReturnResponse(Collections.singletonList(variable));

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExternalVariablesArePersisted(Collections.singletonList(variable));
  }

  @Test
  public void ingestExternalVariable_maxRequestsConfiguredReached() {
    // given
    embeddedOptimizeExtension
        .getConfigurationService()
        .getVariableIngestionConfiguration()
        .setMaxRequests(0);
    final ExternalProcessVariableRequestDto variable =
        ingestionClient.createPrimitiveExternalVariable();

    // when
    final Response response =
        ingestionClient.ingestVariablesAndReturnResponse(Collections.singletonList(variable));

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.TOO_MANY_REQUESTS.getStatusCode());
    assertThat(response.getHeaderString(HttpHeaders.RETRY_AFTER))
        .isEqualTo(IngestionQoSFilter.RETRY_AFTER_SECONDS);
    assertExternalVariablesArePersisted(Collections.emptyList());
  }

  @Test
  public void ingestExternalVariables_maxRequestBytesReached() {
    // given
    embeddedOptimizeExtension
        .getConfigurationService()
        .getVariableIngestionConfiguration()
        .setMaxBatchRequestBytes(1L);
    final List<ExternalProcessVariableRequestDto> variables =
        IntStream.range(0, 10)
            .mapToObj(i -> ingestionClient.createPrimitiveExternalVariable().setId("id" + i))
            .toList();

    // when
    final ErrorResponseDto response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildIngestExternalVariables(variables, ingestionClient.getVariableIngestionToken())
            .execute(
                ErrorResponseDto.class, Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode());

    // then
    assertThat(response.getErrorMessage()).contains("Request too large");
    assertExternalVariablesArePersisted(Collections.emptyList());
  }

  private void assertExternalVariablesArePersisted(
      final List<ExternalProcessVariableRequestDto> variables) {
    final Map<Long, List<ExternalProcessVariableRequestDto>> variablesByIngestionTimestamp =
        Map.of(LocalDateUtil.getCurrentDateTime().toInstant().toEpochMilli(), variables);
    assertExternalVariablesArePersisted(variablesByIngestionTimestamp);
  }

  private void assertExternalVariablesArePersisted(
      final Map<Long, List<ExternalProcessVariableRequestDto>> variablesByIngestionTimestamp) {
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    final List<ExternalProcessVariableDto> expectedVariables = new ArrayList<>();
    variablesByIngestionTimestamp.forEach(
        (ingestionTimestamp, variables) -> {
          final List<ExternalProcessVariableDto> varsPerTimestamp =
              variables.stream()
                  .map(
                      variable ->
                          new ExternalProcessVariableDto()
                              .setVariableId(variable.getId())
                              .setVariableName(variable.getName())
                              .setVariableType(variable.getType())
                              .setVariableValue(variable.getValue())
                              .setIngestionTimestamp(ingestionTimestamp)
                              .setProcessInstanceId(variable.getProcessInstanceId())
                              .setProcessDefinitionKey(variable.getProcessDefinitionKey())
                              .setSerializationDataFormat(variable.getSerializationDataFormat()))
                  .collect(Collectors.toList());
          expectedVariables.addAll(varsPerTimestamp);
        });
    expectedVariables.sort(
        Comparator.comparing(ExternalProcessVariableDto::getIngestionTimestamp).reversed());
    final List<ExternalProcessVariableDto> actualVariables = getAllStoredExternalProcessVariables();
    assertThat(actualVariables).containsExactlyInAnyOrderElementsOf(expectedVariables);
  }

  private List<ExternalProcessVariableDto> getAllStoredExternalProcessVariables() {
    return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
        EXTERNAL_PROCESS_VARIABLE_INDEX_NAME, ExternalProcessVariableDto.class);
  }
}
