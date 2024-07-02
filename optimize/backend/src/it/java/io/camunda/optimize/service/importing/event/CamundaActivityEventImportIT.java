/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.event;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_DEFINITION_INDEX_NAME;
import static io.camunda.optimize.service.events.CamundaEventService.PROCESS_END_TYPE;
import static io.camunda.optimize.service.events.CamundaEventService.PROCESS_START_TYPE;
import static io.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaProcessInstanceEndEventSuffix;
import static io.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaProcessInstanceStartEventSuffix;
import static io.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaTaskEndEventSuffix;
import static io.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaTaskStartEventSuffix;
import static io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension.DEFAULT_ENGINE_ALIAS;
import static io.camunda.optimize.test.optimize.CollectionClient.DEFAULT_TENANT;
import static io.camunda.optimize.util.BpmnModels.USER_TASK_1;
import static io.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
import static io.camunda.optimize.util.BpmnModels.getSingleServiceTaskProcess;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.datasource.EngineDataSourceDto;
import io.camunda.optimize.dto.optimize.query.event.process.CamundaActivityEventDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.schema.index.events.CamundaActivityEventIndexES;
import io.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import io.camunda.optimize.service.db.schema.index.events.CamundaActivityEventIndex;
import io.camunda.optimize.service.importing.AbstractImportIT;
import io.camunda.optimize.util.BpmnModels;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.assertj.core.groups.Tuple;
import org.camunda.bpm.engine.ActivityTypes;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class CamundaActivityEventImportIT extends AbstractImportIT {

  private static final String START_EVENT = ActivityTypes.START_EVENT;
  private static final String END_EVENT = ActivityTypes.END_EVENT_NONE;
  private static final String USER_TASK = ActivityTypes.TASK_USER_TASK;
  private static final String USER_TASK_2 = USER_TASK + "_2";

  @BeforeEach
  public void init() {
    embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(true);
  }

  @Test
  public void expectedEventsAreCreatedOnImportOfCompletedProcess() throws IOException {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto =
        deployAndStartUserTaskProcessWithName("eventsDef");

    // when
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromScratch();

    // then
    final List<CamundaActivityEventDto> storedEvents =
        getSavedEventsForProcessDefinitionKey(processInstanceEngineDto.getProcessDefinitionKey());

    assertThat(storedEvents)
        .hasSize(6)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
            CamundaActivityEventDto.Fields.activityInstanceId,
            CamundaActivityEventDto.Fields.processDefinitionVersion,
            CamundaActivityEventDto.Fields.engine,
            CamundaActivityEventDto.Fields.timestamp,
            CamundaActivityEventDto.Fields.orderCounter)
        .containsExactlyInAnyOrder(
            createAssertionEvent(START_EVENT, START_EVENT, START_EVENT, processInstanceEngineDto),
            createAssertionEvent(END_EVENT, END_EVENT, END_EVENT, processInstanceEngineDto),
            createAssertionEvent(
                applyCamundaTaskStartEventSuffix(USER_TASK),
                applyCamundaTaskStartEventSuffix(USER_TASK),
                USER_TASK,
                processInstanceEngineDto),
            createAssertionEvent(
                applyCamundaTaskEndEventSuffix(USER_TASK),
                applyCamundaTaskEndEventSuffix(USER_TASK),
                USER_TASK,
                processInstanceEngineDto),
            createAssertionEvent(
                applyCamundaProcessInstanceStartEventSuffix(
                    processInstanceEngineDto.getProcessDefinitionKey()),
                PROCESS_START_TYPE,
                PROCESS_START_TYPE,
                processInstanceEngineDto),
            createAssertionEvent(
                applyCamundaProcessInstanceEndEventSuffix(
                    processInstanceEngineDto.getProcessDefinitionKey()),
                PROCESS_END_TYPE,
                PROCESS_END_TYPE,
                processInstanceEngineDto))
        .extracting(CamundaActivityEventDto.Fields.activityInstanceId)
        .contains(
            applyCamundaProcessInstanceEndEventSuffix(processInstanceEngineDto.getId()),
            applyCamundaProcessInstanceStartEventSuffix(processInstanceEngineDto.getId()));
    assertOrderCounters(storedEvents);
  }

  @Test
  public void expectedEventsCreatedOnImportOfRunningProcess() throws IOException {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto =
        deployAndStartUserTaskProcessWithName("runningActivities");

    // when
    importAllEngineEntitiesFromScratch();

    // then
    final List<CamundaActivityEventDto> storedEvents =
        getSavedEventsForProcessDefinitionKey(processInstanceEngineDto.getProcessDefinitionKey());

    assertThat(storedEvents)
        .hasSize(3)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
            CamundaActivityEventDto.Fields.activityInstanceId,
            CamundaActivityEventDto.Fields.processDefinitionVersion,
            CamundaActivityEventDto.Fields.engine,
            CamundaActivityEventDto.Fields.timestamp,
            CamundaActivityEventDto.Fields.orderCounter)
        .containsExactlyInAnyOrder(
            createAssertionEvent(
                applyCamundaProcessInstanceStartEventSuffix(
                    processInstanceEngineDto.getProcessDefinitionKey()),
                PROCESS_START_TYPE,
                PROCESS_START_TYPE,
                processInstanceEngineDto),
            createAssertionEvent(START_EVENT, START_EVENT, START_EVENT, processInstanceEngineDto),
            createAssertionEvent(
                applyCamundaTaskStartEventSuffix(USER_TASK),
                applyCamundaTaskStartEventSuffix(USER_TASK),
                USER_TASK,
                processInstanceEngineDto))
        .extracting(CamundaActivityEventDto.Fields.activityInstanceId)
        .contains(applyCamundaProcessInstanceStartEventSuffix(processInstanceEngineDto.getId()));
    assertOrderCounters(storedEvents);
  }

  @Test
  public void expectedEventsCreatedWithCorrectCancellationStateForCanceledActivity()
      throws IOException {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartUserTaskProcess();
    engineIntegrationExtension.cancelActivityInstance(
        processInstanceEngineDto.getId(), USER_TASK_1);

    // when
    importAllEngineEntitiesFromScratch();

    // then
    final List<CamundaActivityEventDto> storedEvents =
        getSavedEventsForProcessDefinitionKey(processInstanceEngineDto.getProcessDefinitionKey());
    assertThat(storedEvents)
        .extracting(
            CamundaActivityEventDto.Fields.activityId, CamundaActivityEventDto.Fields.canceled)
        .containsExactlyInAnyOrder(
            Tuple.tuple(
                applyCamundaProcessInstanceStartEventSuffix(
                    processInstanceEngineDto.getProcessDefinitionKey()),
                false),
            Tuple.tuple(
                applyCamundaProcessInstanceEndEventSuffix(
                    processInstanceEngineDto.getProcessDefinitionKey()),
                false),
            Tuple.tuple(START_EVENT, false),
            // the user task was canceled
            Tuple.tuple(applyCamundaTaskEndEventSuffix(USER_TASK_1), true),
            Tuple.tuple(applyCamundaTaskStartEventSuffix(USER_TASK_1), true));
    assertOrderCounters(storedEvents);
  }

  @Test
  public void expectedEventsCreatedWithCorrectCancellationStateForCanceledProcessInstance()
      throws IOException {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartUserTaskProcess();
    engineIntegrationExtension.deleteProcessInstance(processInstanceEngineDto.getId());

    // when
    importAllEngineEntitiesFromScratch();

    // then
    final List<CamundaActivityEventDto> storedEvents =
        getSavedEventsForProcessDefinitionKey(processInstanceEngineDto.getProcessDefinitionKey());
    assertThat(storedEvents)
        .extracting(
            CamundaActivityEventDto.Fields.activityId, CamundaActivityEventDto.Fields.canceled)
        .containsExactlyInAnyOrder(
            Tuple.tuple(
                applyCamundaProcessInstanceStartEventSuffix(
                    processInstanceEngineDto.getProcessDefinitionKey()),
                false),
            Tuple.tuple(
                applyCamundaProcessInstanceEndEventSuffix(
                    processInstanceEngineDto.getProcessDefinitionKey()),
                false),
            Tuple.tuple(START_EVENT, false),
            // the process was canceled on the user task, so the task is canceled
            Tuple.tuple(applyCamundaTaskEndEventSuffix(USER_TASK_1), true),
            Tuple.tuple(applyCamundaTaskStartEventSuffix(USER_TASK_1), true));
    assertOrderCounters(storedEvents);
  }

  @Test
  public void noEventsAreCreatedOnImportOfProcessInstanceForDeletedDefinition() throws IOException {
    // given
    final String processKey = "eventsDef";
    final ProcessInstanceEngineDto processInstanceEngineDto =
        deployAndStartUserTaskProcessWithName(processKey);
    createCamundaActivityEventsIndexForKey(processKey);

    engineIntegrationExtension.deleteProcessInstance(processInstanceEngineDto.getId());
    engineIntegrationExtension.deleteProcessDefinition(processInstanceEngineDto.getDefinitionId());

    // when
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromScratch();

    // then
    final List<CamundaActivityEventDto> storedEvents =
        getSavedEventsForProcessDefinitionKey(processInstanceEngineDto.getProcessDefinitionKey());

    assertThat(storedEvents).isEmpty();
  }

  @Test
  public void expectedEventsAreCreatedOnImportOfProcessInstanceForDeletedDefinitionAlreadyImported()
      throws IOException {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto =
        deployAndStartUserTaskProcessWithName("eventsDef");
    importAllEngineEntitiesFromScratch();

    // then the original definition and events are stored
    final List<ProcessDefinitionOptimizeDto> allProcessDefinitions =
        databaseIntegrationTestExtension.getAllProcessDefinitions();
    assertThat(allProcessDefinitions)
        .singleElement()
        .satisfies(
            definition ->
                assertThat(definition.getKey())
                    .isEqualTo(processInstanceEngineDto.getProcessDefinitionKey()));
    final List<CamundaActivityEventDto> savedEvents =
        getSavedEventsForProcessDefinitionKey(processInstanceEngineDto.getProcessDefinitionKey());
    assertThat(savedEvents)
        .hasSize(3)
        .allSatisfy(
            activityEvent ->
                assertThat(activityEvent.getProcessDefinitionKey())
                    .isEqualTo(processInstanceEngineDto.getProcessDefinitionKey()));

    // when the definition is deleted
    engineIntegrationExtension.deleteProcessInstance(processInstanceEngineDto.getId());
    engineIntegrationExtension.deleteProcessDefinition(processInstanceEngineDto.getDefinitionId());

    // when
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromLastIndex();

    // then the remaining events are still stored event though the definition has been deleted from
    // the engine
    assertThat(
            getSavedEventsForProcessDefinitionKey(
                processInstanceEngineDto.getProcessDefinitionKey()))
        .hasSize(7);
  }

  @Test
  public void expectedEventsCreatedOnImportOfMultipleInstances() throws IOException {
    // given
    final ProcessInstanceEngineDto firstInstance =
        deployAndStartUserTaskProcessWithName("processName");
    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.startProcessInstance(firstInstance.getDefinitionId());
    secondInstance.setProcessDefinitionKey(firstInstance.getProcessDefinitionKey());
    secondInstance.setProcessDefinitionVersion(firstInstance.getProcessDefinitionVersion());

    // when
    importAllEngineEntitiesFromScratch();

    // then
    final List<CamundaActivityEventDto> storedEvents =
        getSavedEventsForProcessDefinitionKey(firstInstance.getProcessDefinitionKey());

    assertThat(storedEvents)
        .hasSize(6)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
            CamundaActivityEventDto.Fields.activityInstanceId,
            CamundaActivityEventDto.Fields.processDefinitionVersion,
            CamundaActivityEventDto.Fields.engine,
            CamundaActivityEventDto.Fields.timestamp,
            CamundaActivityEventDto.Fields.orderCounter)
        .containsExactlyInAnyOrder(
            createAssertionEvent(
                applyCamundaProcessInstanceStartEventSuffix(
                    firstInstance.getProcessDefinitionKey()),
                PROCESS_START_TYPE,
                PROCESS_START_TYPE,
                firstInstance),
            createAssertionEvent(START_EVENT, START_EVENT, START_EVENT, firstInstance),
            createAssertionEvent(
                applyCamundaTaskStartEventSuffix(USER_TASK),
                applyCamundaTaskStartEventSuffix(USER_TASK),
                USER_TASK,
                firstInstance),
            createAssertionEvent(
                applyCamundaProcessInstanceStartEventSuffix(
                    secondInstance.getProcessDefinitionKey()),
                PROCESS_START_TYPE,
                PROCESS_START_TYPE,
                secondInstance),
            createAssertionEvent(START_EVENT, START_EVENT, START_EVENT, secondInstance),
            createAssertionEvent(
                applyCamundaTaskStartEventSuffix(USER_TASK),
                applyCamundaTaskStartEventSuffix(USER_TASK),
                USER_TASK,
                secondInstance))
        .extracting(CamundaActivityEventDto.Fields.activityInstanceId)
        .contains(
            applyCamundaProcessInstanceStartEventSuffix(firstInstance.getId()),
            applyCamundaProcessInstanceStartEventSuffix(secondInstance.getId()));
    assertOrderCounters(storedEvents);
  }

  @Test
  public void expectedEventsCreatedOnImportOfMultipleSplitEventsFromSameModel() throws IOException {
    // given
    final ProcessInstanceEngineDto processInstance =
        deployAndStartTwoUserTasksProcess("processName");
    engineIntegrationExtension.finishAllRunningUserTasks(processInstance.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(processInstance.getId());

    // when
    importAllEngineEntitiesFromScratch();

    // then
    final List<CamundaActivityEventDto> storedEvents =
        getSavedEventsForProcessDefinitionKey(processInstance.getProcessDefinitionKey());

    assertThat(storedEvents)
        .hasSize(8)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
            CamundaActivityEventDto.Fields.activityInstanceId,
            CamundaActivityEventDto.Fields.processDefinitionVersion,
            CamundaActivityEventDto.Fields.engine,
            CamundaActivityEventDto.Fields.timestamp,
            CamundaActivityEventDto.Fields.orderCounter)
        .containsExactlyInAnyOrder(
            createAssertionEvent(
                applyCamundaProcessInstanceStartEventSuffix(
                    processInstance.getProcessDefinitionKey()),
                PROCESS_START_TYPE,
                PROCESS_START_TYPE,
                processInstance),
            createAssertionEvent(START_EVENT, START_EVENT, START_EVENT, processInstance),
            createAssertionEvent(
                applyCamundaTaskStartEventSuffix(USER_TASK),
                applyCamundaTaskStartEventSuffix(USER_TASK),
                USER_TASK,
                processInstance),
            createAssertionEvent(
                applyCamundaTaskEndEventSuffix(USER_TASK),
                applyCamundaTaskEndEventSuffix(USER_TASK),
                USER_TASK,
                processInstance),
            createAssertionEvent(
                applyCamundaTaskStartEventSuffix(USER_TASK_2),
                applyCamundaTaskStartEventSuffix(USER_TASK_2),
                USER_TASK,
                processInstance),
            createAssertionEvent(
                applyCamundaTaskEndEventSuffix(USER_TASK_2),
                applyCamundaTaskEndEventSuffix(USER_TASK_2),
                USER_TASK,
                processInstance),
            createAssertionEvent(END_EVENT, END_EVENT, END_EVENT, processInstance),
            createAssertionEvent(
                applyCamundaProcessInstanceEndEventSuffix(
                    processInstance.getProcessDefinitionKey()),
                PROCESS_END_TYPE,
                PROCESS_END_TYPE,
                processInstance))
        .extracting(CamundaActivityEventDto.Fields.activityInstanceId)
        .contains(applyCamundaProcessInstanceStartEventSuffix(processInstance.getId()));
    assertOrderCounters(storedEvents);
  }

  @Test
  public void noEventsCreatedOnImportWithFeatureDisabled() throws JsonProcessingException {
    // given the index has been created, the process start, the start Event, and start of user task
    // has been saved
    // already
    final ProcessInstanceEngineDto processInstanceEngineDto =
        deployAndStartUserTaskProcessWithName("noEventsDef");
    importAllEngineEntitiesFromScratch();
    final List<CamundaActivityEventDto> initialStoredEvents =
        getSavedEventsForProcessDefinitionKey(processInstanceEngineDto.getProcessDefinitionKey());
    assertThat(initialStoredEvents)
        .hasSize(3)
        .extracting(CamundaActivityEventDto::getActivityId)
        .containsExactlyInAnyOrder(
            START_EVENT,
            applyCamundaTaskStartEventSuffix(USER_TASK),
            applyCamundaProcessInstanceStartEventSuffix(
                processInstanceEngineDto.getProcessDefinitionKey()));

    // when the feature is disabled
    embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(false);

    // when engine events happen and import triggered
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromLastIndex();

    // then no additional events are stored
    final List<CamundaActivityEventDto> storedEvents =
        getSavedEventsForProcessDefinitionKey(processInstanceEngineDto.getProcessDefinitionKey());
    assertThat(storedEvents)
        .usingRecursiveFieldByFieldElementComparator()
        .isEqualTo(initialStoredEvents);
  }

  @Test
  public void expectedIndicesCreatedWithMultipleDefinitionsImportedInSameBatch()
      throws IOException {
    // given
    final ProcessInstanceEngineDto firstProcessInstanceEngineDto =
        deployAndStartUserTaskProcessWithName("firstProcessSameBatch");
    final ProcessInstanceEngineDto secondProcessInstanceEngineDto =
        deployAndStartUserTaskProcessWithName("secondProcessSameBatch");

    // when
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromScratch();

    // then
    assertThat(
            databaseIntegrationTestExtension.indexExists(
                createExpectedIndexNameForProcessDefinition(
                    firstProcessInstanceEngineDto.getProcessDefinitionKey())))
        .isTrue();
    assertThat(
            databaseIntegrationTestExtension.indexExists(
                createExpectedIndexNameForProcessDefinition(
                    secondProcessInstanceEngineDto.getProcessDefinitionKey())))
        .isTrue();

    // then events have been saved in each index
    assertThat(
            getSavedEventsForProcessDefinitionKey(
                firstProcessInstanceEngineDto.getProcessDefinitionKey()))
        .hasSize(6);
    assertThat(
            getSavedEventsForProcessDefinitionKey(
                secondProcessInstanceEngineDto.getProcessDefinitionKey()))
        .hasSize(6);
  }

  @Test
  public void expectedIndicesCreatedWithMultipleDefinitionsImportedInMultipleBatches()
      throws IOException {
    // given
    final ProcessInstanceEngineDto firstProcessInstanceEngineDto =
        deployAndStartUserTaskProcessWithName("aProcessFirstBatch");
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromScratch();

    final ProcessInstanceEngineDto secondProcessInstanceEngineDto =
        deployAndStartUserTaskProcessWithName("aProcessSecondBatch");

    // when
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromLastIndex();

    // then
    assertThat(
            databaseIntegrationTestExtension.indexExists(
                createExpectedIndexNameForProcessDefinition(
                    firstProcessInstanceEngineDto.getProcessDefinitionKey())))
        .isTrue();
    assertThat(
            databaseIntegrationTestExtension.indexExists(
                createExpectedIndexNameForProcessDefinition(
                    secondProcessInstanceEngineDto.getProcessDefinitionKey())))
        .isTrue();

    // then events have been saved in each index. The conversion to set is to remove duplicate
    // entries due to
    // multiple import batches
    final Set<String> idsInFirstIndex =
        getSavedEventsForProcessDefinitionKey(
                firstProcessInstanceEngineDto.getProcessDefinitionKey())
            .stream()
            .map(CamundaActivityEventDto::getActivityId)
            .collect(Collectors.toSet());
    assertThat(idsInFirstIndex).hasSize(6);
    assertThat(
            getSavedEventsForProcessDefinitionKey(
                secondProcessInstanceEngineDto.getProcessDefinitionKey()))
        .hasSize(6);
  }

  @Test
  public void noIndexCreatedOnImportWithFeatureDisabled() throws IOException {
    // given
    embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(false);
    final ProcessInstanceEngineDto processInstanceEngineDto =
        deployAndStartUserTaskProcessWithName("shouldNotBeCreated");

    // when
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromScratch();

    // then
    assertThat(
            databaseIntegrationTestExtension.indexExists(
                createExpectedIndexNameForProcessDefinition(
                    processInstanceEngineDto.getProcessDefinitionKey())))
        .isFalse();
  }

  @Test
  public void processDefinitionIsResolvedAsDeletedWhenImportingInstanceData() {
    // given
    final BpmnModelInstance processModel = getSingleServiceTaskProcess();
    final ProcessDefinitionEngineDto deletedDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            processModel, DEFAULT_TENANT);
    engineIntegrationExtension.startProcessInstance(deletedDefinition.getId());
    engineIntegrationExtension.deleteDeploymentById(deletedDefinition.getDeploymentId());
    saveDeletedDefinitionToElasticsearch(deletedDefinition);
    importAllEngineEntitiesFromScratch();

    // when
    final List<CamundaActivityEventDto> savedCamundaEvents =
        databaseIntegrationTestExtension.getAllStoredCamundaActivityEventsForDefinition(
            deletedDefinition.getKey());
    final List<ProcessDefinitionOptimizeDto> allProcessDefinitions =
        databaseIntegrationTestExtension.getAllProcessDefinitions();

    // then
    assertThat(allProcessDefinitions)
        .singleElement()
        .satisfies(def -> assertThat(def.isDeleted()).isTrue());
    assertThat(savedCamundaEvents)
        .isNotEmpty()
        .extracting(CamundaActivityEventDto::getProcessDefinitionKey)
        .allMatch(key -> key.equals(deletedDefinition.getKey()));
  }

  private CamundaActivityEventDto createAssertionEvent(
      final String activityId,
      final String activityName,
      final String activityType,
      final ProcessInstanceEngineDto processInstanceEngineDto) {
    return CamundaActivityEventDto.builder()
        .activityId(activityId)
        .activityName(activityName)
        .activityType(activityType)
        .processInstanceId(processInstanceEngineDto.getId())
        .processDefinitionKey(processInstanceEngineDto.getProcessDefinitionKey())
        .processDefinitionVersion(processInstanceEngineDto.getProcessDefinitionVersion())
        .tenantId(processInstanceEngineDto.getTenantId())
        .build();
  }

  private String createExpectedIndexNameForProcessDefinition(final String processDefinitionKey) {
    return CamundaActivityEventIndex.constructIndexName(processDefinitionKey);
  }

  private List<CamundaActivityEventDto> getSavedEventsForProcessDefinitionKey(
      final String processDefinitionKey) throws JsonProcessingException {
    return databaseIntegrationTestExtension.getAllDocumentsOfIndexAs(
        CamundaActivityEventIndex.constructIndexName(processDefinitionKey),
        CamundaActivityEventDto.class);
  }

  private ProcessInstanceEngineDto deployAndStartUserTaskProcessWithName(final String processName) {
    return engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(processName, START_EVENT, END_EVENT, USER_TASK));
  }

  protected ProcessInstanceEngineDto deployAndStartTwoUserTasksProcess(final String processName) {
    return engineIntegrationExtension.deployAndStartProcess(
        getDoubleUserTaskDiagram(processName, START_EVENT, END_EVENT, USER_TASK, USER_TASK_2));
  }

  private void assertOrderCounters(final List<CamundaActivityEventDto> storedEvents) {
    final List<CamundaActivityEventDto> orderedEvents =
        storedEvents.stream()
            .filter(
                event ->
                    !event.getActivityType().equalsIgnoreCase(PROCESS_START_TYPE)
                        && !event.getActivityType().equalsIgnoreCase(PROCESS_END_TYPE))
            .collect(Collectors.toList());
    assertThat(orderedEvents)
        .extracting(CamundaActivityEventDto::getOrderCounter)
        .doesNotContainNull();
  }

  @SneakyThrows
  private void createCamundaActivityEventsIndexForKey(final String key) {
    final OptimizeIndexNameService indexNameService =
        databaseIntegrationTestExtension.getIndexNameService();
    final CamundaActivityEventIndexES newIndex = new CamundaActivityEventIndexES(key);
    databaseIntegrationTestExtension.createIndex(
        indexNameService.getOptimizeIndexNameWithVersion(newIndex),
        indexNameService.getOptimizeIndexAliasForIndex(newIndex.getIndexName()));
  }

  private void saveDeletedDefinitionToElasticsearch(
      final ProcessDefinitionEngineDto definitionEngineDto) {
    final ProcessDefinitionOptimizeDto expectedDto =
        ProcessDefinitionOptimizeDto.builder()
            .id(definitionEngineDto.getId())
            .key(definitionEngineDto.getKey())
            .name(definitionEngineDto.getName())
            .version(definitionEngineDto.getVersionAsString())
            .tenantId(definitionEngineDto.getTenantId().orElse(null))
            .dataSource(new EngineDataSourceDto(DEFAULT_ENGINE_ALIAS))
            .deleted(true)
            .bpmn20Xml("someXml")
            .build();
    databaseIntegrationTestExtension.addEntryToDatabase(
        PROCESS_DEFINITION_INDEX_NAME, expectedDto.getId(), expectedDto);
  }
}
