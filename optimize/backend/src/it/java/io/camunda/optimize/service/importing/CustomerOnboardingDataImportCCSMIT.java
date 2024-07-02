/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing;

import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_DEFINITION_INDEX_NAME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractCCSMIT;
import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
import io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex;
import io.camunda.optimize.test.util.DateCreationFreezer;
import io.github.netmikey.logunit.api.LogCapturer;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// Passes locally, but fails on CI
// @Tag(OPENSEARCH_PASSING)
public class CustomerOnboardingDataImportCCSMIT extends AbstractCCSMIT {

  public static final String CUSTOMER_ONBOARDING_PROCESS_INSTANCES =
      "customer_onboarding_test_process_instances.json";
  public static final String CUSTOMER_ONBOARDING_DEFINITION_FILE_NAME =
      "customer_onboarding_definition.json";
  private static final String CUSTOMER_ONBOARDING_DEFINITION_NAME = "customer_onboarding_en";

  @RegisterExtension
  @Order(1)
  public final LogCapturer logCapturer =
      LogCapturer.create().captureForType(CustomerOnboardingDataImportService.class);

  @BeforeEach
  public void cleanUpExistingProcessInstanceIndices() {
    databaseIntegrationTestExtension.deleteAllProcessInstanceIndices();
  }

  @Test
  public void importCanBeDisabled() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setCustomerOnboardingImport(false);
    embeddedOptimizeExtension.reloadConfiguration();

    // when
    addDataToOptimize(
        CUSTOMER_ONBOARDING_PROCESS_INSTANCES, CUSTOMER_ONBOARDING_DEFINITION_FILE_NAME);

    // then
    assertThat(
            databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
                PROCESS_DEFINITION_INDEX_NAME, ProcessDefinitionOptimizeDto.class))
        .isEmpty();
    assertThat(
            indexExist(
                ProcessInstanceIndex.constructIndexName(CUSTOMER_ONBOARDING_DEFINITION_NAME)))
        .isFalse();
  }

  @Test
  public void dataGetsImportedToOptimize() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setCustomerOnboardingImport(true);
    embeddedOptimizeExtension.reloadConfiguration();

    // when
    addDataToOptimize(
        CUSTOMER_ONBOARDING_PROCESS_INSTANCES, CUSTOMER_ONBOARDING_DEFINITION_FILE_NAME);

    // then
    List<ProcessDefinitionOptimizeDto> processDefinitionDocuments =
        databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
            PROCESS_DEFINITION_INDEX_NAME, ProcessDefinitionOptimizeDto.class);
    assertThat(processDefinitionDocuments).hasSize(1);
    final ProcessDefinitionOptimizeDto processDefinition = processDefinitionDocuments.get(0);
    // the onboarding data should already be considered onboarded to avoid the notification being
    // sent upon import
    assertThat(processDefinition.isOnboarded()).isTrue();
    assertThat(indexExist(ProcessInstanceIndex.constructIndexName(processDefinition.getKey())))
        .isTrue();
    assertThat(
            databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
                ProcessInstanceIndex.constructIndexName(processDefinition.getKey()),
                ProcessDefinitionOptimizeDto.class))
        .hasSize(3);
    List<ProcessInstanceDto> processInstanceDtos =
        databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
            ProcessInstanceIndex.constructIndexName(processDefinition.getKey()),
            ProcessInstanceDto.class);
    assertThat(processInstanceDtos)
        .anyMatch(processInstanceDto -> processInstanceDto.getIncidents() != null);
  }

  @Test
  public void processDefinitionFileDoesntExist() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setCustomerOnboardingImport(true);
    embeddedOptimizeExtension.reloadConfiguration();

    // when
    addDataToOptimize(CUSTOMER_ONBOARDING_PROCESS_INSTANCES, "doesntexist");

    // then
    List<ProcessDefinitionOptimizeDto> processDefinitionDocuments =
        databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
            PROCESS_DEFINITION_INDEX_NAME, ProcessDefinitionOptimizeDto.class);
    assertThat(processDefinitionDocuments).isEmpty();
    assertThat(
            indexExist(
                ProcessInstanceIndex.constructIndexName(CUSTOMER_ONBOARDING_DEFINITION_NAME)))
        .isFalse();
    logCapturer.assertContains(
        "Process definition could not be loaded. Please validate your json file.");
  }

  @Test
  public void processInstanceFileDoesntExist() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setCustomerOnboardingImport(true);
    embeddedOptimizeExtension.reloadConfiguration();

    // when
    addDataToOptimize("doesntExist", CUSTOMER_ONBOARDING_DEFINITION_FILE_NAME);

    // then
    List<ProcessDefinitionOptimizeDto> processDefinitionDocuments =
        databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
            PROCESS_DEFINITION_INDEX_NAME, ProcessDefinitionOptimizeDto.class);
    assertThat(processDefinitionDocuments).hasSize(1);
    assertThat(
            indexExist(
                ProcessInstanceIndex.constructIndexName(CUSTOMER_ONBOARDING_DEFINITION_NAME)))
        .isFalse();
    logCapturer.assertContains(
        "Could not load Camunda Customer Onboarding Demo process instances to input stream. Please validate the process instance "
            + "json file.");
  }

  @Test
  public void processInstanceDataAreInvalid() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setCustomerOnboardingImport(true);
    embeddedOptimizeExtension.reloadConfiguration();

    // when
    addDataToOptimize("invalid_data.json", CUSTOMER_ONBOARDING_DEFINITION_FILE_NAME);

    // then
    List<ProcessDefinitionOptimizeDto> processDefinitionDocuments =
        databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
            PROCESS_DEFINITION_INDEX_NAME, ProcessDefinitionOptimizeDto.class);
    assertThat(processDefinitionDocuments).hasSize(1);
    assertThat(
            indexExist(
                ProcessInstanceIndex.constructIndexName(CUSTOMER_ONBOARDING_DEFINITION_NAME)))
        .isFalse();
    logCapturer.assertContains(
        "Could not load Camunda Customer Onboarding Demo process instances to input stream. Please validate the process instance "
            + "json file.");
  }

  @Test
  public void processDefinitionDataAreInvalid() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setCustomerOnboardingImport(true);
    embeddedOptimizeExtension.reloadConfiguration();

    // when
    addDataToOptimize(CUSTOMER_ONBOARDING_PROCESS_INSTANCES, "invalid_data.json");

    // then
    List<ProcessDefinitionOptimizeDto> processDefinitionDocuments =
        databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
            PROCESS_DEFINITION_INDEX_NAME, ProcessDefinitionOptimizeDto.class);
    assertThat(processDefinitionDocuments).isEmpty();
    assertThat(
            indexExist(
                ProcessInstanceIndex.constructIndexName(CUSTOMER_ONBOARDING_DEFINITION_NAME)))
        .isFalse();
    logCapturer.assertContains(
        "Process definition could not be loaded. Please validate your json file.");
  }

  @Test
  public void verifyProcessDatesAreUpToDate() {
    // given
    final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
    final OffsetDateTime instanceStartDate =
        OffsetDateTime.parse("2022-02-04T21:24:14+01:00", ISO_OFFSET_DATE_TIME);
    final OffsetDateTime instanceEndDate =
        OffsetDateTime.parse("2022-02-04T21:25:18+01:00", ISO_OFFSET_DATE_TIME);
    final OffsetDateTime flowNodeStartDate =
        OffsetDateTime.parse("2022-02-04T21:24:15+01:00", ISO_OFFSET_DATE_TIME);
    final OffsetDateTime flowNodeEndDate =
        OffsetDateTime.parse("2022-02-04T21:24:16+01:00", ISO_OFFSET_DATE_TIME);
    final long offset = ChronoUnit.SECONDS.between(instanceEndDate, now);
    final OffsetDateTime newStartDate = instanceStartDate.plusSeconds(offset);
    final OffsetDateTime newEndDate = instanceEndDate.plusSeconds(offset);
    final OffsetDateTime newFlowNodeStartDate = flowNodeStartDate.plusSeconds(offset);
    final OffsetDateTime newFlowNodeEndDate = flowNodeEndDate.plusSeconds(offset);
    embeddedOptimizeExtension.getConfigurationService().setCustomerOnboardingImport(true);
    embeddedOptimizeExtension.reloadConfiguration();

    // when
    addDataToOptimize(
        "customer_onboarding_process_instance_date_modification.json",
        CUSTOMER_ONBOARDING_DEFINITION_FILE_NAME);

    // then
    List<ProcessDefinitionOptimizeDto> processDefinitionDocuments =
        databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
            PROCESS_DEFINITION_INDEX_NAME, ProcessDefinitionOptimizeDto.class);
    assertThat(processDefinitionDocuments).hasSize(1);
    assertThat(
            indexExist(
                ProcessInstanceIndex.constructIndexName(CUSTOMER_ONBOARDING_DEFINITION_NAME)))
        .isTrue();
    List<ProcessInstanceDto> processInstanceDto =
        databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
            ProcessInstanceIndex.constructIndexName(processDefinitionDocuments.get(0).getKey()),
            ProcessInstanceDto.class);

    assertThat(processInstanceDto)
        .singleElement()
        .satisfies(
            instance -> {
              assertThat(instance.getFlowNodeInstances())
                  .singleElement()
                  .satisfies(
                      flowNode -> {
                        assertThat(flowNode.getStartDate()).isEqualTo(newFlowNodeStartDate);
                        assertThat(flowNode.getEndDate()).isEqualTo(newFlowNodeEndDate);
                      });
              assertThat(instance.getStartDate()).isEqualTo(newStartDate);
              assertThat(instance.getEndDate()).isEqualTo(newEndDate);
            });
  }

  protected boolean indexExist(final String indexName) {
    return embeddedOptimizeExtension
        .getDatabaseSchemaManager()
        .indexExists(embeddedOptimizeExtension.getOptimizeDatabaseClient(), indexName);
  }

  private void addDataToOptimize(
      final String processInstanceFile, final String processDefinitionFile) {
    CustomerOnboardingDataImportService customerOnboardingDataImportService =
        embeddedOptimizeExtension.getBean(CustomerOnboardingDataImportService.class);
    customerOnboardingDataImportService.importData(processInstanceFile, processDefinitionFile, 1);
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
  }
}
