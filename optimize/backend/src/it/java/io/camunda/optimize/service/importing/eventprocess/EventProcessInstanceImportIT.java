/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.eventprocess;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.db.DatabaseConstants.EVENT_PROCESS_INSTANCE_INDEX_PREFIX;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_INSTANCE_INDEX_PREFIX;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_INSTANCE_MULTI_ALIAS;
import static io.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaTaskStartEventSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessInstanceDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessPublishStateDto;
import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceDto;
import io.camunda.optimize.dto.optimize.query.variable.SimpleProcessVariableDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.schema.index.events.EventProcessInstanceIndexES;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.test.optimize.EventProcessClient;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.SneakyThrows;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag(OPENSEARCH_PASSING)
public class EventProcessInstanceImportIT extends AbstractEventProcessIT {

  @Test
  public void dedicatedInstanceIndexIsCreatedForPublishedEventProcess() {
    // given
    final String eventProcessMappingId =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId);

    executeImportCycle();

    // then
    final EventProcessPublishStateDto eventProcessPublishState =
        getEventPublishStateForEventProcessMappingId(eventProcessMappingId);

    final Map<String, Set<String>> eventProcessInstanceIndicesAndAliases =
        getEventProcessInstanceIndicesWithAliasesFromDatabase();
    assertThat(eventProcessInstanceIndicesAndAliases)
        .hasSize(1)
        .hasEntrySatisfying(
            getVersionedEventProcessInstanceIndexNameForPublishedStateId(
                eventProcessPublishState.getId()),
            aliases ->
                assertThat(aliases)
                    .containsExactlyInAnyOrder(
                        getOptimizeIndexAliasForIndexName(
                            new EventProcessInstanceIndexES(eventProcessPublishState.getId())
                                .getIndexName()),
                        getOptimizeIndexAliasForIndexName(PROCESS_INSTANCE_MULTI_ALIAS),
                        getOptimizeIndexAliasForIndexName(
                            PROCESS_INSTANCE_INDEX_PREFIX
                                + eventProcessPublishState.getProcessKey())));
  }

  @Test
  public void dedicatedInstanceIndexesAreCreatedForMultipleEventProcesses() {
    // given
    final String eventProcessMappingId1 =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);
    final String eventProcessMappingId2 =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId1);
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId2);

    executeImportCycle();

    // then
    final String eventProcessPublishStateId1 =
        getEventPublishStateIdForEventProcessMappingId(eventProcessMappingId1);
    final String eventProcessPublishStateId2 =
        getEventPublishStateIdForEventProcessMappingId(eventProcessMappingId2);

    assertThat(getEventProcessInstanceIndicesWithAliasesFromDatabase())
        .hasSize(2)
        .containsKeys(
            getVersionedEventProcessInstanceIndexNameForPublishedStateId(
                eventProcessPublishStateId1),
            getVersionedEventProcessInstanceIndexNameForPublishedStateId(
                eventProcessPublishStateId2));
  }

  @ParameterizedTest(name = "Instance index is deleted on {0}.")
  @MethodSource("cancelAction")
  public void dedicatedInstanceIndexIsDeletedOn(
      final String actionName, final BiConsumer<EventProcessClient, String> action) {
    // given
    final String eventProcessMappingId =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId);

    executeImportCycle();

    action.accept(eventProcessClient, eventProcessMappingId);

    executeImportCycle();

    // then
    final boolean eventProcessInstanceIndicesExist = eventProcessInstanceIndicesExist();
    assertThat(eventProcessInstanceIndicesExist).isFalse();
  }

  @ParameterizedTest(
      name = "Only expected instance index is deleted on {0}, other is still present.")
  @MethodSource("cancelAction")
  public void dedicatedInstanceIndexIsDeletedOtherInstanceIndexNotAffected(
      final String actionName, final BiConsumer<EventProcessClient, String> action) {
    // given
    final String eventProcessMappingId1 =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);
    final String eventProcessMappingId2 =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId1);
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId2);
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    final String eventProcessPublishStateId2 =
        getEventPublishStateIdForEventProcessMappingId(eventProcessMappingId2);

    executeImportCycle();

    action.accept(eventProcessClient, eventProcessMappingId1);

    executeImportCycle();

    // then
    assertThat(getEventProcessInstanceIndicesWithAliasesFromDatabase())
        .containsOnlyKeys(
            getVersionedEventProcessInstanceIndexNameForPublishedStateId(
                eventProcessPublishStateId2));
  }

  @Test
  public void instancesAreGeneratedForExistingEventsAfterPublish() {
    // given
    final OffsetDateTime timeBaseLine = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(60));
    final OffsetDateTime startDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedStartEventName = "startedEvent";
    final String ingestedStartEventId = ingestTestEvent(ingestedStartEventName, startDateTime);

    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(30));
    final OffsetDateTime endDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedEndEventName = "finishedEvent";
    final String ingestedEndEventId = ingestTestEvent(ingestedEndEventName, endDateTime);

    final String eventProcessMappingId =
        createSimpleEventProcessMapping(ingestedStartEventName, ingestedEndEventName);

    // when
    LocalDateUtil.setCurrentTime(OffsetDateTime.now());
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId);

    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromDatabase();
    assertThat(processInstances)
        .hasSize(1)
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto)
                  .usingRecursiveComparison()
                  .ignoringFields(
                      ProcessInstanceDto.Fields.flowNodeInstances,
                      EventProcessInstanceDto.Fields.pendingFlowNodeInstanceUpdates,
                      EventProcessInstanceDto.Fields.correlatedEventsById)
                  .isEqualTo(
                      createExpectedEventProcessInstanceForTraceId(
                          eventProcessMappingId,
                          startDateTime,
                          endDateTime,
                          PROCESS_INSTANCE_STATE_COMPLETED));
              assertThat(processInstanceDto.getFlowNodeInstances())
                  .containsOnly(
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_START_EVENT_ID,
                              "startEvent",
                              ingestedStartEventId)
                          .setStartDate(startDateTime)
                          .setEndDate(startDateTime)
                          .setTotalDurationInMs(0L),
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_END_EVENT_ID,
                              "endEvent",
                              ingestedEndEventId)
                          .setStartDate(endDateTime)
                          .setEndDate(endDateTime)
                          .setTotalDurationInMs(0L));
            });
  }

  @Test
  public void instancesAreGeneratedForMultipleEventProcesses() {
    // given
    ingestTestEvent(STARTED_EVENT);
    ingestTestEvent(FINISHED_EVENT);

    final String eventProcessMappingId1 =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);
    final String eventProcessMappingId2 =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId1);
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId2);

    executeImportCycle();

    // then
    final String eventProcessPublishStateId1 =
        getEventPublishStateIdForEventProcessMappingId(eventProcessMappingId1);
    final String eventProcessPublishStateId2 =
        getEventPublishStateIdForEventProcessMappingId(eventProcessMappingId2);

    final List<EventProcessInstanceDto> eventProcess1ProcessInstances =
        getEventProcessInstancesFromDatabaseForProcessPublishStateId(eventProcessPublishStateId1);
    final List<EventProcessInstanceDto> eventProcess2ProcessInstances =
        getEventProcessInstancesFromDatabaseForProcessPublishStateId(eventProcessPublishStateId2);

    assertThat(eventProcess1ProcessInstances).hasSize(1);
    assertThat(eventProcess2ProcessInstances).hasSize(1);
  }

  @Test
  public void canceledFlowNodesAreCorrelatedToInstanceCorrectlyOnEventProcessInstancePublish() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    engineIntegrationExtension.cancelActivityInstance(
        processInstanceEngineDto.getId(), USER_TASK_ID_ONE);
    importEngineEntities();
    publishEventMappingUsingProcessInstanceCamundaEvents(
        processInstanceEngineDto,
        createMappingsForEventProcess(
            processInstanceEngineDto,
            BPMN_START_EVENT_ID,
            applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
            BPMN_END_EVENT_ID));
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    assertThat(getEventProcessInstancesFromDatabase())
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto.getFlowNodeInstances())
                  .extracting(FlowNodeInstanceDto::getFlowNodeId, FlowNodeInstanceDto::getCanceled)
                  .containsExactlyInAnyOrder(
                      Tuple.tuple(BPMN_START_EVENT_ID, false), Tuple.tuple(USER_TASK_ID_ONE, true));
            });
  }

  @Test
  public void canceledFlowNodesUpdateAlreadyPublishedInstanceFlowNodes() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    importEngineEntities();
    publishEventMappingUsingProcessInstanceCamundaEvents(
        processInstanceEngineDto,
        createMappingsForEventProcess(
            processInstanceEngineDto,
            BPMN_START_EVENT_ID,
            applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
            BPMN_END_EVENT_ID));
    importEngineEntities();

    // when
    executeImportCycle();

    // then all flow nodes for the process are not canceled
    assertThat(getEventProcessInstancesFromDatabase())
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto.getFlowNodeInstances())
                  .extracting(FlowNodeInstanceDto::getFlowNodeId, FlowNodeInstanceDto::getCanceled)
                  .containsExactlyInAnyOrder(
                      Tuple.tuple(BPMN_START_EVENT_ID, false),
                      Tuple.tuple(USER_TASK_ID_ONE, false));
            });

    // when we cancel the running user task and reimport the activity
    engineIntegrationExtension.cancelActivityInstance(
        processInstanceEngineDto.getId(), USER_TASK_ID_ONE);
    importEngineEntities();
    executeImportCycle();

    // then the user task is marked as canceled on the event process
    assertThat(getEventProcessInstancesFromDatabase())
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto.getFlowNodeInstances())
                  .extracting(FlowNodeInstanceDto::getFlowNodeId, FlowNodeInstanceDto::getCanceled)
                  .containsExactlyInAnyOrder(
                      Tuple.tuple(BPMN_START_EVENT_ID, false), Tuple.tuple(USER_TASK_ID_ONE, true));
            });
  }

  @Test
  public void instancesAreGeneratedForExistingEventsAfterPublish_otherEventsHaveNoEffect() {
    // given
    final OffsetDateTime timeBaseLine = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(60));
    final OffsetDateTime startDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedStartEventId = ingestTestEvent(STARTED_EVENT, startDateTime);

    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(30));
    final OffsetDateTime endDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedEndEventId = ingestTestEvent(FINISHED_EVENT, endDateTime);

    final String eventProcessMappingId =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    ingestTestEvent("randomOtherEvent", LocalDateUtil.getCurrentDateTime());
    ingestTestEvent("evenAnotherEvent", LocalDateUtil.getCurrentDateTime());

    // when
    LocalDateUtil.setCurrentTime(OffsetDateTime.now());
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId);

    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromDatabase();
    assertThat(processInstances)
        .hasSize(1)
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto)
                  .usingRecursiveComparison()
                  .ignoringFields(
                      ProcessInstanceDto.Fields.flowNodeInstances,
                      EventProcessInstanceDto.Fields.pendingFlowNodeInstanceUpdates,
                      EventProcessInstanceDto.Fields.correlatedEventsById)
                  .isEqualTo(
                      createExpectedEventProcessInstanceForTraceId(
                          eventProcessMappingId,
                          startDateTime,
                          endDateTime,
                          PROCESS_INSTANCE_STATE_COMPLETED));
              assertThat(processInstanceDto.getFlowNodeInstances())
                  .containsOnly(
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_START_EVENT_ID,
                              "startEvent",
                              ingestedStartEventId)
                          .setStartDate(startDateTime)
                          .setEndDate(startDateTime)
                          .setTotalDurationInMs(0L),
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_END_EVENT_ID,
                              "endEvent",
                              ingestedEndEventId)
                          .setStartDate(endDateTime)
                          .setEndDate(endDateTime)
                          .setTotalDurationInMs(0L));
            });
  }

  @Test
  public void instancesAreGeneratedWhenEventsAreIngestedAfterPublish() {
    // given
    LocalDateUtil.setCurrentTime(OffsetDateTime.now());

    final String eventProcessMappingId =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId);

    executeImportCycle();

    final OffsetDateTime timeBaseLine = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(60));
    final OffsetDateTime startDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedStartEventId = ingestTestEvent(STARTED_EVENT, startDateTime);

    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(30));
    final OffsetDateTime endDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedEndEventId = ingestTestEvent(FINISHED_EVENT, endDateTime);

    LocalDateUtil.setCurrentTime(timeBaseLine.plusSeconds(30));
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromDatabase();
    assertThat(processInstances)
        .hasSize(1)
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto)
                  .usingRecursiveComparison()
                  .ignoringFields(
                      ProcessInstanceDto.Fields.flowNodeInstances,
                      EventProcessInstanceDto.Fields.pendingFlowNodeInstanceUpdates,
                      EventProcessInstanceDto.Fields.correlatedEventsById)
                  .isEqualTo(
                      createExpectedEventProcessInstanceForTraceId(
                          eventProcessMappingId,
                          startDateTime,
                          endDateTime,
                          PROCESS_INSTANCE_STATE_COMPLETED));

              assertThat(processInstanceDto.getFlowNodeInstances())
                  .containsOnly(
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_START_EVENT_ID,
                              "startEvent",
                              ingestedStartEventId)
                          .setStartDate(startDateTime)
                          .setEndDate(startDateTime)
                          .setTotalDurationInMs(0L),
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_END_EVENT_ID,
                              "endEvent",
                              ingestedEndEventId)
                          .setStartDate(endDateTime)
                          .setEndDate(endDateTime)
                          .setTotalDurationInMs(0L));
            });
  }

  @Test
  public void newEventsWithSameIdUpdateActivityInstances() {
    // given
    LocalDateUtil.setCurrentTime(OffsetDateTime.now());

    final String eventProcessMappingId =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId);
    executeImportCycle();

    final OffsetDateTime timeBaseLine = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(60));
    final OffsetDateTime startDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedStartEventId = ingestTestEvent(STARTED_EVENT, startDateTime);

    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(30));
    final String ingestedEndEventId = ingestTestEvent(FINISHED_EVENT, startDateTime);

    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(10));
    final OffsetDateTime updatedEndDateTime = LocalDateUtil.getCurrentDateTime();
    ingestTestEvent(ingestedEndEventId, FINISHED_EVENT, updatedEndDateTime);

    LocalDateUtil.setCurrentTime(timeBaseLine.plusSeconds(30));
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromDatabase();
    assertThat(processInstances)
        .hasSize(1)
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto)
                  .usingRecursiveComparison()
                  .ignoringFields(
                      ProcessInstanceDto.Fields.flowNodeInstances,
                      EventProcessInstanceDto.Fields.pendingFlowNodeInstanceUpdates,
                      EventProcessInstanceDto.Fields.correlatedEventsById)
                  .isEqualTo(
                      createExpectedEventProcessInstanceForTraceId(
                          eventProcessMappingId,
                          startDateTime,
                          updatedEndDateTime,
                          PROCESS_INSTANCE_STATE_COMPLETED));

              assertThat(processInstanceDto.getFlowNodeInstances())
                  .containsOnly(
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_START_EVENT_ID,
                              "startEvent",
                              ingestedStartEventId)
                          .setStartDate(startDateTime)
                          .setEndDate(startDateTime)
                          .setTotalDurationInMs(0L),
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_END_EVENT_ID,
                              "endEvent",
                              ingestedEndEventId)
                          .setStartDate(updatedEndDateTime)
                          .setEndDate(updatedEndDateTime)
                          .setTotalDurationInMs(0L));
            });
  }

  @Test
  public void instanceIsRunningIfNoEndEventYetIngested() {
    // given

    final OffsetDateTime timeBaseLine = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(60));

    final OffsetDateTime startDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedStartEventId = ingestTestEvent(STARTED_EVENT, startDateTime);

    final String eventProcessMappingId =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    LocalDateUtil.setCurrentTime(OffsetDateTime.now());
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId);

    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromDatabase();
    assertThat(processInstances)
        .hasSize(1)
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto)
                  .usingRecursiveComparison()
                  .ignoringFields(
                      ProcessInstanceDto.Fields.flowNodeInstances,
                      EventProcessInstanceDto.Fields.pendingFlowNodeInstanceUpdates,
                      EventProcessInstanceDto.Fields.correlatedEventsById)
                  .isEqualTo(
                      createExpectedEventProcessInstanceForTraceId(
                          eventProcessMappingId,
                          startDateTime,
                          null,
                          PROCESS_INSTANCE_STATE_ACTIVE));

              assertThat(processInstanceDto.getFlowNodeInstances())
                  .containsOnly(
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_START_EVENT_ID,
                              "startEvent",
                              ingestedStartEventId)
                          .setStartDate(startDateTime)
                          .setEndDate(startDateTime)
                          .setTotalDurationInMs(0L));
            });
  }

  @Test
  public void instanceIsCompletedOnceEndEventGotIngested() {
    // given
    final OffsetDateTime timeBaseLine = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(60));

    final OffsetDateTime startDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedStartEventId = ingestTestEvent(STARTED_EVENT, startDateTime);

    final String eventProcessMappingId =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    LocalDateUtil.setCurrentTime(OffsetDateTime.now());
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId);

    executeImportCycle();

    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(30));
    final OffsetDateTime endDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedEndEventId = ingestTestEvent(FINISHED_EVENT, endDateTime);

    LocalDateUtil.setCurrentTime(timeBaseLine.plusSeconds(30));
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromDatabase();
    assertThat(processInstances)
        .hasSize(1)
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto)
                  .usingRecursiveComparison()
                  .ignoringFields(
                      ProcessInstanceDto.Fields.flowNodeInstances,
                      EventProcessInstanceDto.Fields.pendingFlowNodeInstanceUpdates,
                      EventProcessInstanceDto.Fields.correlatedEventsById)
                  .isEqualTo(
                      createExpectedEventProcessInstanceForTraceId(
                          eventProcessMappingId,
                          startDateTime,
                          endDateTime,
                          PROCESS_INSTANCE_STATE_COMPLETED));

              assertThat(processInstanceDto.getFlowNodeInstances())
                  .containsOnly(
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_START_EVENT_ID,
                              "startEvent",
                              ingestedStartEventId)
                          .setStartDate(startDateTime)
                          .setEndDate(startDateTime)
                          .setTotalDurationInMs(0L),
                      new FlowNodeInstanceDto(
                              eventProcessMappingId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_END_EVENT_ID,
                              "endEvent",
                              ingestedEndEventId)
                          .setStartDate(endDateTime)
                          .setEndDate(endDateTime)
                          .setTotalDurationInMs(0L));
            });
  }

  @Test
  public void instanceIsCompletedEvenIfOnlyEndEventGotIngested() {
    // given
    final OffsetDateTime timeBaseLine = OffsetDateTime.now();

    LocalDateUtil.setCurrentTime(timeBaseLine.minusSeconds(30));
    final OffsetDateTime endDateTime = LocalDateUtil.getCurrentDateTime();
    final String ingestedEndEventId = ingestTestEvent(FINISHED_EVENT, endDateTime);

    final String eventProcessId = createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    LocalDateUtil.setCurrentTime(OffsetDateTime.now());
    eventProcessClient.publishEventProcessMapping(eventProcessId);

    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromDatabase();
    assertThat(processInstances)
        .hasSize(1)
        .singleElement()
        .satisfies(
            processInstanceDto -> {
              assertThat(processInstanceDto)
                  .usingRecursiveComparison()
                  .ignoringFields(
                      ProcessInstanceDto.Fields.flowNodeInstances,
                      EventProcessInstanceDto.Fields.pendingFlowNodeInstanceUpdates,
                      EventProcessInstanceDto.Fields.correlatedEventsById)
                  .isEqualTo(
                      createExpectedEventProcessInstanceForTraceId(
                          eventProcessId, null, endDateTime, PROCESS_INSTANCE_STATE_COMPLETED));
              assertThat(processInstanceDto.getFlowNodeInstances())
                  .containsOnly(
                      new FlowNodeInstanceDto(
                              eventProcessId,
                              "1",
                              null,
                              processInstanceDto.getProcessInstanceId(),
                              BPMN_END_EVENT_ID,
                              "endEvent",
                              ingestedEndEventId)
                          .setStartDate(endDateTime)
                          .setEndDate(endDateTime)
                          .setTotalDurationInMs(0L));
            });
  }

  private EventProcessInstanceDto createExpectedEventProcessInstanceForTraceId(
      final String eventProcessId,
      final OffsetDateTime startDateTime,
      final OffsetDateTime endDateTime,
      final String state) {
    Long duration = null;
    if (startDateTime != null && endDateTime != null) {
      duration = startDateTime.until(endDateTime, ChronoUnit.MILLIS);
    }
    return EventProcessInstanceDto.eventProcessInstanceBuilder()
        .processDefinitionId(eventProcessId)
        .processDefinitionKey(eventProcessId)
        .processDefinitionVersion("1")
        .processInstanceId(MY_TRACE_ID_1)
        .duration(duration)
        .startDate(startDateTime)
        .endDate(endDateTime)
        .state(state)
        .variables(
            Collections.singletonList(
                SimpleProcessVariableDto.builder()
                    .id(VARIABLE_ID)
                    .name(VARIABLE_ID)
                    .value(Collections.singletonList(VARIABLE_VALUE))
                    .type(VARIABLE_VALUE.getClass().getSimpleName())
                    .version(1L)
                    .build()))
        .build();
  }

  private String getVersionedEventProcessInstanceIndexNameForPublishedStateId(
      final String eventProcessPublishStateId) {
    // Since we're not really creating a new index here, but just instantiating it in order to get
    // the name, it's not
    // a database dependency that we are instantiating the ES index here.
    return databaseIntegrationTestExtension
        .getIndexNameService()
        .getOptimizeIndexNameWithVersion(
            new EventProcessInstanceIndexES(eventProcessPublishStateId));
  }

  private String getOptimizeIndexAliasForIndexName(final String indexName) {
    return databaseIntegrationTestExtension
        .getIndexNameService()
        .getOptimizeIndexAliasForIndex(indexName);
  }

  @SneakyThrows
  private boolean eventProcessInstanceIndicesExist() {
    final String indexAlias =
        databaseIntegrationTestExtension
                .getIndexNameService()
                .getOptimizeIndexAliasForIndex(EVENT_PROCESS_INSTANCE_INDEX_PREFIX)
            + "*";
    return databaseIntegrationTestExtension.indexExists(indexAlias);
  }
}
