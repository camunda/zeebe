/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.cleanup;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static
// io.camunda.optimize.service.util.InstanceIndexUtil.getProcessInstanceIndexAliasName;
// import static io.camunda.optimize.util.SuppressionConstants.UNCHECKED_CAST;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockserver.model.JsonBody.json;
//
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.persistence.BusinessKeyDto;
// import io.camunda.optimize.dto.optimize.query.event.process.CamundaActivityEventDto;
// import io.camunda.optimize.dto.optimize.query.variable.VariableUpdateInstanceDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.util.configuration.cleanup.CleanupMode;
// import
// io.camunda.optimize.service.util.configuration.cleanup.ProcessDefinitionCleanupConfiguration;
// import io.camunda.optimize.util.BpmnModels;
// import io.github.netmikey.logunit.api.LogCapturer;
// import java.time.OffsetDateTime;
// import java.util.List;
// import lombok.SneakyThrows;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.RegisterExtension;
// import org.mockserver.integration.ClientAndServer;
// import org.mockserver.model.HttpRequest;
// import org.mockserver.verify.VerificationTimes;
//
// @Tag(OPENSEARCH_PASSING)
// public class EngineDataProcessCleanupServiceIT extends AbstractCleanupIT {
//
//   @RegisterExtension
//   LogCapturer cleanupServiceLogs = LogCapturer.create().captureForType(CleanupService.class);
//
//   @RegisterExtension
//   LogCapturer engineDataCleanupLogs =
//       LogCapturer.create().captureForType(EngineDataProcessCleanupService.class);
//
//   @BeforeEach
//   public void enableProcessCleanup() {
//     embeddedOptimizeExtension
//         .getConfigurationService()
//         .getCleanupServiceConfiguration()
//         .getProcessDataCleanupConfiguration()
//         .setEnabled(true);
//   }
//
//   @Test
//   @SneakyThrows
//   public void testCleanupModeAll() {
//     // given
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     final ProcessInstanceEngineDto unaffectedProcessInstanceForSameDefinition =
//         startNewInstanceWithEndTime(OffsetDateTime.now(), instancesToGetCleanedUp.get(0));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertNoProcessInstanceDataExists(instancesToGetCleanedUp);
//
// assertPersistedProcessInstanceDataComplete(unaffectedProcessInstanceForSameDefinition.getId());
//   }
//
//   @Test
//   @SneakyThrows
//   public void testCleanupModeAll_disabled() {
//     // given
//     getProcessDataCleanupConfiguration().setEnabled(false);
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final List<ProcessInstanceEngineDto> unaffectedProcessInstances =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertPersistedProcessInstanceDataComplete(
//         extractProcessInstanceIds(unaffectedProcessInstances));
//   }
//
//   @Test
//   @SneakyThrows
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void testCleanupModeAll_customBatchSize() {
//     // given
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     getProcessDataCleanupConfiguration().setBatchSize(1);
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     final ProcessInstanceEngineDto unaffectedProcessInstanceForSameDefinition =
//         startNewInstanceWithEndTime(OffsetDateTime.now(), instancesToGetCleanedUp.get(0));
//
//     importAllEngineEntitiesFromScratch();
//
//     final ClientAndServer elasticsearchFacade = useAndGetDbMockServer();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertNoProcessInstanceDataExists(instancesToGetCleanedUp);
//
// assertPersistedProcessInstanceDataComplete(unaffectedProcessInstanceForSameDefinition.getId());
//     instancesToGetCleanedUp.forEach(
//         instance ->
//             elasticsearchFacade.verify(
//                 HttpRequest.request()
//                     .withPath("/_bulk")
//                     .withBody(
//                         json(
//                             createBulkDeleteProcessInstanceRequestJson(
//                                 instance.getId(), instance.getProcessDefinitionKey()))),
//                 VerificationTimes.exactly(1)));
//   }
//
//   @Test
//   @SneakyThrows
//   public void testCleanupModeAll_specificKeyTtl() {
//     // given
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//
//     final List<ProcessInstanceEngineDto> instancesOfDefinitionWithHigherTtl =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     configureHigherProcessSpecificTtl(
//         instancesOfDefinitionWithHigherTtl.get(0).getProcessDefinitionKey());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertNoProcessInstanceDataExists(instancesToGetCleanedUp);
//     assertPersistedProcessInstanceDataComplete(
//         extractProcessInstanceIds(instancesOfDefinitionWithHigherTtl));
//   }
//
//   @Test
//   @SneakyThrows
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void testCleanupModeAll_camundaEventData() {
//     // given
//     embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(true);
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     final ProcessInstanceEngineDto unaffectedProcessInstanceForSameDefinition =
//         startNewInstanceWithEndTime(OffsetDateTime.now(), instancesToGetCleanedUp.get(0));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertNoProcessInstanceDataExists(instancesToGetCleanedUp);
//
// assertPersistedProcessInstanceDataComplete(unaffectedProcessInstanceForSameDefinition.getId());
//     assertThat(getCamundaActivityEvents())
//         .extracting(CamundaActivityEventDto::getProcessInstanceId)
//         .containsOnly(unaffectedProcessInstanceForSameDefinition.getId());
//     assertThat(getAllCamundaEventBusinessKeys())
//         .extracting(BusinessKeyDto::getProcessInstanceId)
//         .containsOnly(unaffectedProcessInstanceForSameDefinition.getId());
//     assertThat(databaseIntegrationTestExtension.getAllStoredVariableUpdateInstanceDtos())
//         .isNotEmpty()
//         .extracting(VariableUpdateInstanceDto::getProcessInstanceId)
//         .containsOnly(unaffectedProcessInstanceForSameDefinition.getId());
//   }
//
//   @Test
//   @SneakyThrows
//   public void testCleanupModeAll_camundaEventData_specificKeyTtl() {
//     // given
//     embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(true);
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//
//     final List<ProcessInstanceEngineDto> instancesOfDefinitionWithHigherTtl =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     configureHigherProcessSpecificTtl(
//         instancesOfDefinitionWithHigherTtl.get(0).getProcessDefinitionKey());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertNoProcessInstanceDataExists(instancesToGetCleanedUp);
//     assertPersistedProcessInstanceDataComplete(
//         extractProcessInstanceIds(instancesOfDefinitionWithHigherTtl));
//     assertThat(getCamundaActivityEvents())
//         .extracting(CamundaActivityEventDto::getProcessInstanceId)
//         .containsAnyElementsOf(extractProcessInstanceIds(instancesOfDefinitionWithHigherTtl));
//     assertThat(getAllCamundaEventBusinessKeys())
//         .extracting(BusinessKeyDto::getProcessInstanceId)
//         .containsAnyElementsOf(extractProcessInstanceIds(instancesOfDefinitionWithHigherTtl));
//     assertThat(databaseIntegrationTestExtension.getAllStoredVariableUpdateInstanceDtos())
//         .isNotEmpty()
//         .extracting(VariableUpdateInstanceDto::getProcessInstanceId)
//         .containsAnyElementsOf(extractProcessInstanceIds(instancesOfDefinitionWithHigherTtl));
//   }
//
//   @Test
//   @SneakyThrows
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void testCleanupModeVariables() {
//     // given
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.VARIABLES);
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     final ProcessInstanceEngineDto unaffectedProcessInstanceForSameDefinition =
//         startNewInstanceWithEndTime(OffsetDateTime.now(), instancesToGetCleanedUp.get(0));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertVariablesEmptyInProcessInstances(extractProcessInstanceIds(instancesToGetCleanedUp));
//
// assertPersistedProcessInstanceDataComplete(unaffectedProcessInstanceForSameDefinition.getId());
//   }
//
//   @Test
//   @SneakyThrows
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void testCleanupModeVariables_customBatchSize() {
//     // given
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.VARIABLES);
//     getProcessDataCleanupConfiguration().setBatchSize(1);
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     final ProcessInstanceEngineDto unaffectedProcessInstanceForSameDefinition =
//         startNewInstanceWithEndTime(OffsetDateTime.now(), instancesToGetCleanedUp.get(0));
//
//     importAllEngineEntitiesFromScratch();
//
//     final ClientAndServer elasticsearchFacade = useAndGetDbMockServer();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertVariablesEmptyInProcessInstances(extractProcessInstanceIds(instancesToGetCleanedUp));
//
// assertPersistedProcessInstanceDataComplete(unaffectedProcessInstanceForSameDefinition.getId());
//     instancesToGetCleanedUp.forEach(
//         instance ->
//             elasticsearchFacade.verify(
//                 HttpRequest.request()
//                     .withPath("/_bulk")
//                     .withBody(
//                         json(
//                             createBulkUpdateProcessInstanceRequestJson(
//                                 instance.getId(), instance.getProcessDefinitionKey()))),
//                 VerificationTimes.exactly(1)));
//   }
//
//   @Test
//   @SneakyThrows
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void testCleanupModeVariables_specificKeyCleanupMode() {
//     // given
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final List<ProcessInstanceEngineDto> instancesOfDefinitionWithVariableMode =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     getCleanupConfiguration()
//         .getProcessDataCleanupConfiguration()
//         .getProcessDefinitionSpecificConfiguration()
//         .put(
//             instancesOfDefinitionWithVariableMode.get(0).getProcessDefinitionKey(),
//             ProcessDefinitionCleanupConfiguration.builder()
//                 .cleanupMode(CleanupMode.VARIABLES)
//                 .build());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertVariablesEmptyInProcessInstances(
//         extractProcessInstanceIds(instancesOfDefinitionWithVariableMode));
//   }
//
//   @Test
//   @SneakyThrows
//   public void testCleanupModeVariables_specificKeyCleanupMode_noInstanceDataExists() {
//     // given
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
//     final ProcessDefinitionEngineDto processDefinitionEngineDto =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             BpmnModels.getSingleUserTaskDiagram());
//     getCleanupConfiguration()
//         .getProcessDataCleanupConfiguration()
//         .getProcessDefinitionSpecificConfiguration()
//         .put(
//             processDefinitionEngineDto.getKey(),
//             ProcessDefinitionCleanupConfiguration.builder()
//                 .cleanupMode(CleanupMode.VARIABLES)
//                 .build());
//
//     importAllEngineEntitiesFromScratch();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//
//     // then
//     engineDataCleanupLogs.assertContains(
//         "Finished cleanup on process instances for processDefinitionKey: aProcess, with ttl: P2Y
// and mode:VARIABLES");
//   }
//
//   @Test
//   @SneakyThrows
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void testCleanupModeVariables_specificKeyTtl() {
//     // given
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.VARIABLES);
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//
//     final List<ProcessInstanceEngineDto> instancesOfDefinitionWithHigherTtl =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     configureHigherProcessSpecificTtl(
//         instancesOfDefinitionWithHigherTtl.get(0).getProcessDefinitionKey());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertVariablesEmptyInProcessInstances(extractProcessInstanceIds(instancesToGetCleanedUp));
//     assertPersistedProcessInstanceDataComplete(
//         extractProcessInstanceIds(instancesOfDefinitionWithHigherTtl));
//   }
//
//   @Test
//   @SneakyThrows
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void testCleanupModeVariables_camundaEventData() {
//     // given
//     embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(true);
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.VARIABLES);
//
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     final ProcessInstanceEngineDto unaffectedProcessInstanceForSameDefinition =
//         startNewInstanceWithEndTime(OffsetDateTime.now(), instancesToGetCleanedUp.get(0));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertVariablesEmptyInProcessInstances(extractProcessInstanceIds(instancesToGetCleanedUp));
//
// assertPersistedProcessInstanceDataComplete(unaffectedProcessInstanceForSameDefinition.getId());
//     assertThat(databaseIntegrationTestExtension.getAllStoredVariableUpdateInstanceDtos())
//         .isNotEmpty()
//         .extracting(VariableUpdateInstanceDto::getProcessInstanceId)
//         .containsOnly(unaffectedProcessInstanceForSameDefinition.getId());
//   }
//
//   @Test
//   @SneakyThrows
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void testCleanupModeVariables_camundaEventData_specificKey() {
//     // given
//     embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(true);
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.VARIABLES);
//
//     final List<ProcessInstanceEngineDto> instancesToGetCleanedUp =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//
//     final List<ProcessInstanceEngineDto> instancesOfDefinitionWithHigherTtl =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     configureHigherProcessSpecificTtl(
//         instancesOfDefinitionWithHigherTtl.get(0).getProcessDefinitionKey());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertVariablesEmptyInProcessInstances(extractProcessInstanceIds(instancesToGetCleanedUp));
//     assertPersistedProcessInstanceDataComplete(
//         extractProcessInstanceIds(instancesOfDefinitionWithHigherTtl));
//     assertThat(databaseIntegrationTestExtension.getAllStoredVariableUpdateInstanceDtos())
//         .isNotEmpty()
//         .extracting(VariableUpdateInstanceDto::getProcessInstanceId)
//         .containsAnyElementsOf(extractProcessInstanceIds(instancesOfDefinitionWithHigherTtl));
//   }
//
//   @Test
//   @SneakyThrows
//   @SuppressWarnings(UNCHECKED_CAST)
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void testCleanupOnSpecificKeyConfigWithNoMatchingProcessDefinitionLogsWarning() {
//     // given I have a key specific config
//     final String configuredKey = "myMistypedKey";
//     getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.VARIABLES);
//     getProcessDataCleanupConfiguration()
//         .getProcessDefinitionSpecificConfiguration()
//         .put(configuredKey, new ProcessDefinitionCleanupConfiguration(CleanupMode.VARIABLES));
//     // and deploy processes with different keys
//     final List<ProcessInstanceEngineDto> instancesWithEndTimeLessThanTtl =
//         deployProcessAndStartTwoProcessInstancesWithEndTimeLessThanTtl();
//     final List<ProcessInstanceEngineDto> instancesWithEndTimeWithinTtl =
//         deployProcessAndStartTwoProcessInstancesWithEndTime(OffsetDateTime.now());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then data clear up has succeeded as expected
//     assertPersistedProcessInstanceDataComplete(
//         extractProcessInstanceIds(instancesWithEndTimeWithinTtl));
//     assertVariablesEmptyInProcessInstances(
//         extractProcessInstanceIds(instancesWithEndTimeLessThanTtl));
//     // and the misconfigured process is logged
//     cleanupServiceLogs.assertContains(
//         String.format(
//             "History Cleanup Configuration contains definition keys for which there is no "
//                 + "definition imported yet. The keys without a match in the database are: [%s]",
//             configuredKey));
//   }
//
//   private String createBulkDeleteProcessInstanceRequestJson(
//       final String processInstanceId, final String processDefinitionKey) {
//     return createBulkProcessInstanceRequestJson(processInstanceId, processDefinitionKey,
// "delete");
//   }
//
//   private String createBulkUpdateProcessInstanceRequestJson(
//       final String processInstanceId, final String processDefinitionKey) {
//     return createBulkProcessInstanceRequestJson(processInstanceId, processDefinitionKey,
// "update");
//   }
//
//   private String createBulkProcessInstanceRequestJson(
//       final String processInstanceId, final String processDefinitionKey, final String operation)
// {
//     return String.format(
//         "{\"%s\":{\"_index\":\"%s\",\"_id\":\"%s\"}}",
//         operation,
//         embeddedOptimizeExtension
//             .getOptimizeDatabaseClient()
//             .getIndexNameService()
//
// .getOptimizeIndexAliasForIndex(getProcessInstanceIndexAliasName(processDefinitionKey)),
//         processInstanceId);
//   }
// }
