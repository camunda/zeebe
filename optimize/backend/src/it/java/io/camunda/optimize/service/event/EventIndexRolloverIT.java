/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.event;
//
// import static
// io.camunda.optimize.service.db.DatabaseConstants.CAMUNDA_ACTIVITY_EVENT_INDEX_PREFIX;
// import static io.camunda.optimize.service.db.DatabaseConstants.EXTERNAL_EVENTS_INDEX_NAME;
// import static
// io.camunda.optimize.service.db.DatabaseConstants.VARIABLE_UPDATE_INSTANCE_INDEX_NAME;
// import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.optimize.query.event.process.CamundaActivityEventDto;
// import io.camunda.optimize.dto.optimize.query.event.process.EventDto;
// import io.camunda.optimize.dto.optimize.query.variable.VariableUpdateInstanceDto;
// import io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.db.es.schema.index.VariableUpdateInstanceIndexES;
// import io.camunda.optimize.service.db.es.schema.index.events.CamundaActivityEventIndexES;
// import io.camunda.optimize.service.db.es.schema.index.events.EventIndexES;
// import io.camunda.optimize.service.events.rollover.EventIndexRolloverService;
// import io.camunda.optimize.service.util.configuration.IndexRolloverConfiguration;
// import java.io.IOException;
// import java.util.List;
// import java.util.stream.IntStream;
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
//
// public class EventIndexRolloverIT extends AbstractPlatformIT {
//
//   private static final int NUMBER_OF_EVENTS_IN_BATCH = 10;
//   private static final String EXPECTED_SUFFIX_AFTER_FIRST_ROLLOVER = "-000002";
//   private static final String EXPECTED_SUFFIX_AFTER_SECOND_ROLLOVER = "-000003";
//
//   @BeforeEach
//   public void before() {
//     embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(true);
//     embeddedOptimizeExtension
//         .getConfigurationService()
//         .getEventBasedProcessConfiguration()
//         .getEventImport()
//         .setEnabled(true);
//   }
//
//   @BeforeEach
//   @AfterEach
//   public void cleanUpEventIndices() {
//     databaseIntegrationTestExtension.deleteAllExternalEventIndices();
//     databaseIntegrationTestExtension.deleteAllVariableUpdateInstanceIndices();
//     embeddedOptimizeExtension
//         .getDatabaseSchemaManager()
//         .createOrUpdateOptimizeIndex(
//             embeddedOptimizeExtension.getOptimizeDatabaseClient(), new EventIndexES());
//     embeddedOptimizeExtension
//         .getDatabaseSchemaManager()
//         .createOrUpdateOptimizeIndex(
//             embeddedOptimizeExtension.getOptimizeDatabaseClient(),
//             new VariableUpdateInstanceIndexES());
//   }
//
//   @Test
//   public void testRolloverNoCamundaActivitiesImported() {
//     // given
//     ingestExternalEvents();
//     getEventIndexRolloverConfiguration().setMaxIndexSizeGB(0);
//
//     // when
//     final List<String> rolledOverIndices = getEventIndexRolloverService().triggerRollover();
//
//     // then
//     assertThat(rolledOverIndices)
//         .containsExactlyInAnyOrder(EXTERNAL_EVENTS_INDEX_NAME,
// VARIABLE_UPDATE_INSTANCE_INDEX_NAME);
//   }
//
//   @Test
//   public void testRolloverIncludingAllRelevantIndices() {
//     // given
//     final ProcessInstanceEngineDto processInstanceEngineDto = importCamundaEvents();
//     getEventIndexRolloverConfiguration().setMaxIndexSizeGB(0);
//
//     // when
//     final List<String> rolledOverIndices = getEventIndexRolloverService().triggerRollover();
//
//     // then
//     assertThat(rolledOverIndices)
//         .containsExactlyInAnyOrder(
//             EXTERNAL_EVENTS_INDEX_NAME,
//             CAMUNDA_ACTIVITY_EVENT_INDEX_PREFIX
//                 + processInstanceEngineDto.getProcessDefinitionKey(),
//             VARIABLE_UPDATE_INSTANCE_INDEX_NAME);
//   }
//
//   @Test
//   public void testMultipleRolloversSuccessful() throws IOException {
//     // given
//     getEventIndexRolloverConfiguration().setMaxIndexSizeGB(0);
//
//     // when
//     ingestExternalEvents();
//     importVariableUpdateInstances(3);
//     final ProcessInstanceEngineDto processInstanceEngineDto = importCamundaEvents();
//     final CamundaActivityEventIndexES camundaActivityIndex =
//         new CamundaActivityEventIndexES(processInstanceEngineDto.getProcessDefinitionKey());
//     final List<String> rolledOverIndicesFirstRollover =
//         getEventIndexRolloverService().triggerRollover();
//     final List<String> indicesWithExternalEventWriteAliasFirstRollover =
//         getAllIndicesWithWriteAlias(EXTERNAL_EVENTS_INDEX_NAME);
//     final List<String> indicesWithCamundaActivityWriteAliasFirstRollover =
//         getAllIndicesWithWriteAlias(camundaActivityIndex.getIndexName());
//     final List<String> indicesWithVariableUpdateInstanceWriteAliasFirstRollover =
//         getAllIndicesWithWriteAlias(VARIABLE_UPDATE_INSTANCE_INDEX_NAME);
//
//     // then
//     assertThat(rolledOverIndicesFirstRollover)
//         .containsExactlyInAnyOrder(
//             EXTERNAL_EVENTS_INDEX_NAME,
//             CAMUNDA_ACTIVITY_EVENT_INDEX_PREFIX
//                 + processInstanceEngineDto.getProcessDefinitionKey(),
//             VARIABLE_UPDATE_INSTANCE_INDEX_NAME);
//     assertThat(indicesWithExternalEventWriteAliasFirstRollover)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_FIRST_ROLLOVER));
//     assertThat(indicesWithCamundaActivityWriteAliasFirstRollover)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_FIRST_ROLLOVER));
//     assertThat(indicesWithVariableUpdateInstanceWriteAliasFirstRollover)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_FIRST_ROLLOVER));
//
//     assertThat(getAllStoredExternalEvents()).hasSize(NUMBER_OF_EVENTS_IN_BATCH);
//     // The process start, start event and start of user tasks have been imported, so we expect 3
// in
//     // ES
//     assertThat(
//             getAllStoredCamundaActivityEventsForDefinitionKey(
//                 processInstanceEngineDto.getProcessDefinitionKey()))
//         .hasSize(3);
//     assertThat(getAllStoredVariableUpdateInstanceDtos()).hasSize(3);
//
//     // when
//     ingestExternalEvents();
//     importVariableUpdateInstances(3);
//     importNextCamundaEventsForProcessInstance(processInstanceEngineDto);
//     final List<String> rolledOverIndicesSecondRollover =
//         getEventIndexRolloverService().triggerRollover();
//     final List<String> indicesWithExternalEventWriteAliasSecondRollover =
//         getAllIndicesWithWriteAlias(EXTERNAL_EVENTS_INDEX_NAME);
//     final List<String> indicesWithCamundaActivityWriteAliasSecondRollover =
//         getAllIndicesWithWriteAlias(camundaActivityIndex.getIndexName());
//     final List<String> indicesWithVariableUpdateInstanceWriteAliasSecondRollover =
//         getAllIndicesWithWriteAlias(VARIABLE_UPDATE_INSTANCE_INDEX_NAME);
//
//     // then
//     assertThat(rolledOverIndicesSecondRollover)
//         .containsExactlyInAnyOrder(
//             EXTERNAL_EVENTS_INDEX_NAME,
//             CAMUNDA_ACTIVITY_EVENT_INDEX_PREFIX
//                 + processInstanceEngineDto.getProcessDefinitionKey(),
//             VARIABLE_UPDATE_INSTANCE_INDEX_NAME);
//     assertThat(indicesWithExternalEventWriteAliasSecondRollover)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_SECOND_ROLLOVER));
//     assertThat(indicesWithCamundaActivityWriteAliasSecondRollover)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_SECOND_ROLLOVER));
//     assertThat(indicesWithVariableUpdateInstanceWriteAliasSecondRollover)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_SECOND_ROLLOVER));
//     assertThat(getAllStoredExternalEvents()).hasSize(NUMBER_OF_EVENTS_IN_BATCH * 2);
//     // Over the two imports, we expect 3 activities to be imported in the first and 5 in the
// second
//     assertThat(
//             getAllStoredCamundaActivityEventsForDefinitionKey(
//                 processInstanceEngineDto.getProcessDefinitionKey()))
//         .hasSize(8);
//     assertThat(getAllStoredVariableUpdateInstanceDtos()).hasSize(6);
//   }
//
//   @Test
//   public void testRolloverConditionsNotMet() {
//     // given
//     ingestExternalEvents();
//     importCamundaEvents();
//
//     // when
//     final List<String> rolledOverIndices = getEventIndexRolloverService().triggerRollover();
//
//     // then
//     assertThat(rolledOverIndices).isEmpty();
//   }
//
//   @Test
//   public void aliasAssociatedWithCorrectIndexAfterRollover() throws IOException {
//     // given
//     ingestExternalEvents();
//     final ProcessInstanceEngineDto processInstanceEngineDto = importCamundaEvents();
//     final CamundaActivityEventIndexES camundaActivityIndex =
//         new CamundaActivityEventIndexES(processInstanceEngineDto.getProcessDefinitionKey());
//     getEventIndexRolloverConfiguration().setMaxIndexSizeGB(0);
//
//     // when
//     getEventIndexRolloverService().triggerRollover();
//     final List<String> indicesWithExternalEventWriteAlias =
//         getAllIndicesWithWriteAlias(EXTERNAL_EVENTS_INDEX_NAME);
//     final List<String> indicesWithCamundaActivityWriteAlias =
//         getAllIndicesWithWriteAlias(camundaActivityIndex.getIndexName());
//     final List<String> indicesWithVariableUpdateInstanceWriteAlias =
//         getAllIndicesWithWriteAlias(VARIABLE_UPDATE_INSTANCE_INDEX_NAME);
//
//     // then
//     assertThat(indicesWithExternalEventWriteAlias)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_FIRST_ROLLOVER));
//     assertThat(indicesWithCamundaActivityWriteAlias)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_FIRST_ROLLOVER));
//     assertThat(indicesWithVariableUpdateInstanceWriteAlias)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_FIRST_ROLLOVER));
//   }
//
//   @Test
//   public void noDataLossAfterRollover() {
//     // given
//     ingestExternalEvents();
//     final ProcessInstanceEngineDto processInstanceEngineDto = importCamundaEvents();
//     getEventIndexRolloverConfiguration().setMaxIndexSizeGB(0);
//
//     // when
//     getEventIndexRolloverService().triggerRollover();
//
//     // then
//     assertThat(getAllStoredExternalEvents()).hasSize(NUMBER_OF_EVENTS_IN_BATCH);
//     // The process start, start event and start of user tasks have been imported, so we expect 3
// in
//     // ES
//     assertThat(
//             getAllStoredCamundaActivityEventsForDefinitionKey(
//                 processInstanceEngineDto.getProcessDefinitionKey()))
//         .hasSize(3);
//   }
//
//   @Test
//   public void searchAliasPointsToAllIndicesAfterRollover() {
//     // given
//     ingestExternalEvents();
//     final ProcessInstanceEngineDto processInstanceEngineDto = importCamundaEvents();
//     getEventIndexRolloverConfiguration().setMaxIndexSizeGB(0);
//
//     // when
//     getEventIndexRolloverService().triggerRollover();
//     ingestExternalEvents();
//     importNextCamundaEventsForProcessInstance(processInstanceEngineDto);
//
//     // then there are 2 * EXPECTED_NUMBER_OF_EVENTS present (half in the old index and half in
// the
//     // new)
//     assertThat(getAllStoredExternalEvents()).hasSize(NUMBER_OF_EVENTS_IN_BATCH * 2);
//     // Over the two imports, we expect 3 activities to be imported in the first and 5 in the
// second
//     assertThat(
//             getAllStoredCamundaActivityEventsForDefinitionKey(
//                 processInstanceEngineDto.getProcessDefinitionKey()))
//         .hasSize(8);
//   }
//
//   @Test
//   public void aliasAssociatedWithCorrectIndexAfterRolloverOfMultipleCamundaActivityIndices()
//       throws IOException {
//     // given
//     getEventIndexRolloverConfiguration().setMaxIndexSizeGB(0);
//     final ProcessInstanceEngineDto firstProcessInstanceEngineDto = importCamundaEvents();
//     final CamundaActivityEventIndexES firstCamundaActivityIndex =
//         new CamundaActivityEventIndexES(firstProcessInstanceEngineDto.getProcessDefinitionKey());
//     final ProcessInstanceEngineDto secondProcessInstanceEngineDto = importCamundaEvents();
//     final CamundaActivityEventIndexES secondCamundaActivityIndex =
//         new
// CamundaActivityEventIndexES(secondProcessInstanceEngineDto.getProcessDefinitionKey());
//
//     // when
//     getEventIndexRolloverService().triggerRollover();
//     final List<String> indicesWithFirstCamundaActivityWriteAlias =
//         getAllIndicesWithWriteAlias(firstCamundaActivityIndex.getIndexName());
//     final List<String> indicesWithSecondCamundaActivityWriteAlias =
//         getAllIndicesWithWriteAlias(secondCamundaActivityIndex.getIndexName());
//
//     // then
//     assertThat(indicesWithFirstCamundaActivityWriteAlias)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_FIRST_ROLLOVER));
//     assertThat(indicesWithSecondCamundaActivityWriteAlias)
//         .hasSize(1)
//         .singleElement()
//         .satisfies(
//             indexName -> assertThat(indexName).contains(EXPECTED_SUFFIX_AFTER_FIRST_ROLLOVER));
//   }
//
//   private EventIndexRolloverService getEventIndexRolloverService() {
//     return embeddedOptimizeExtension.getEventIndexRolloverService();
//   }
//
//   private IndexRolloverConfiguration getEventIndexRolloverConfiguration() {
//     return
// embeddedOptimizeExtension.getConfigurationService().getEventIndexRolloverConfiguration();
//   }
//
//   private List<EventDto> getAllStoredExternalEvents() {
//     return databaseIntegrationTestExtension.getAllStoredExternalEvents();
//   }
//
//   private List<CamundaActivityEventDto> getAllStoredCamundaActivityEventsForDefinitionKey(
//       final String indexName) {
//     return databaseIntegrationTestExtension.getAllStoredCamundaActivityEventsForDefinition(
//         indexName);
//   }
//
//   private List<VariableUpdateInstanceDto> getAllStoredVariableUpdateInstanceDtos() {
//     return databaseIntegrationTestExtension.getAllStoredVariableUpdateInstanceDtos();
//   }
//
//   private void ingestExternalEvents() {
//     final List<CloudEventRequestDto> eventDtos =
//         IntStream.range(0, NUMBER_OF_EVENTS_IN_BATCH)
//             .mapToObj(operand -> ingestionClient.createCloudEventDto())
//             .toList();
//
//     ingestionClient.ingestEventBatch(eventDtos);
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//   }
//
//   private void importNextCamundaEventsForProcessInstance(
//       final ProcessInstanceEngineDto processInstanceEngineDto) {
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstanceEngineDto.getId());
//     importAllEngineEntitiesFromLastIndex();
//   }
//
//   private void importVariableUpdateInstances(final int count) {
//     IntStream.range(0, count)
//         .mapToObj(operand -> createSimpleVariableUpdateInstanceDto(String.valueOf(operand)))
//         .forEach(
//             variableUpdateInstanceDto ->
//                 databaseIntegrationTestExtension.addEntryToDatabase(
//                     VARIABLE_UPDATE_INSTANCE_INDEX_NAME,
//                     variableUpdateInstanceDto.getInstanceId(),
//                     variableUpdateInstanceDto));
//
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//   }
//
//   private VariableUpdateInstanceDto createSimpleVariableUpdateInstanceDto(final String
// instanceId) {
//     return VariableUpdateInstanceDto.builder()
//         .instanceId(instanceId)
//         .name("someName")
//         .processInstanceId("someProcessInstanceId")
//         .build();
//   }
//
//   private ProcessInstanceEngineDto importCamundaEvents() {
//     final ProcessInstanceEngineDto processInstanceEngineDto =
//         engineIntegrationExtension.deployAndStartProcess(
//             getSingleUserTaskDiagram("akey", "start_event_id", "end_event_id",
// "some_user_task"));
//     importAllEngineEntitiesFromScratch();
//     return processInstanceEngineDto;
//   }
//
//   private List<String> getAllIndicesWithWriteAlias(final String indexName) throws IOException {
//     final String aliasNameWithPrefix =
//         embeddedOptimizeExtension
//             .getOptimizeDatabaseClient()
//             .getIndexNameService()
//             .getOptimizeIndexAliasForIndex(indexName);
//     return databaseIntegrationTestExtension.getAllIndicesWithWriteAlias(aliasNameWithPrefix);
//   }
// }
