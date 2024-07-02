/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.event;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaTaskEndEventSuffix;
import static io.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaTaskStartEventSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import io.camunda.optimize.dto.optimize.query.event.process.CamundaActivityEventDto;
import io.camunda.optimize.dto.optimize.query.event.sequence.EventSequenceCountDto;
import io.camunda.optimize.dto.optimize.query.event.sequence.EventTraceStateDto;
import io.camunda.optimize.dto.optimize.query.event.sequence.TracedEventDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.schema.index.events.EventSequenceCountIndex;
import io.camunda.optimize.service.db.schema.index.events.EventTraceStateIndex;
import io.camunda.optimize.service.events.CamundaEventService;
import io.camunda.optimize.util.BpmnModels;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.ActivityTypes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class CamundaEventTraceStateImportIT extends AbstractEventTraceStateImportIT {

  private static final String START_EVENT = ActivityTypes.START_EVENT;
  private static final String END_EVENT = ActivityTypes.END_EVENT_NONE;
  private static final String USER_TASK = ActivityTypes.TASK_USER_TASK;

  @SneakyThrows
  @Test
  public void noCamundaEventStateTraceIndicesCreatedIfEventBasedProcessesDisabled() {
    // given
    embeddedOptimizeExtension.getDefaultEngineConfiguration().setEventImportEnabled(false);
    final String definitionKey = "myCamundaProcess";
    deployAndStartUserTaskProcessWithName(definitionKey);
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromScratch();

    // when
    processEventCountAndTraces();

    // then
    assertThat(
            databaseIntegrationTestExtension.indexExists(
                getTraceStateIndexNameForDefinitionKey(definitionKey)))
        .isFalse();
    assertThat(
            databaseIntegrationTestExtension.indexExists(
                getSequenceCountIndexNameForDefinitionKey(definitionKey)))
        .isFalse();
  }

  @Test
  public void expectedEventTracesAndCountsAreCreatedForMultipleCamundaProcesses() {
    // given
    final String definitionKey1 = "myCamundaProcess1";
    final String definitionKey2 = "myCamundaProcess2";
    final ProcessInstanceEngineDto processInstanceEngineDto1 =
        deployAndStartUserTaskProcessWithName(definitionKey1);
    final ProcessInstanceEngineDto processInstanceEngineDto2 =
        deployAndStartUserTaskProcessWithName(definitionKey2);
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromScratch();

    // when
    processEventCountAndTraces();

    // then
    assertTracesAndCountsArePresentForDefinitionKey(
        definitionKey1, processInstanceEngineDto1, true);
    assertTracesAndCountsArePresentForDefinitionKey(
        definitionKey2, processInstanceEngineDto2, true);
  }

  @Test
  public void eventTracesAndCountsAreCreatedCorrectlyForProcessWithEventsWithIdenticalTimestamps() {
    // given
    final String definitionKey = "myCamundaProcess1";
    final ProcessInstanceEngineDto processInstanceEngineDto =
        deployAndStartUserTaskProcessWithName(definitionKey);
    engineIntegrationExtension.finishAllRunningUserTasks();
    final OffsetDateTime eventTimestamp = OffsetDateTime.now().withNano(0);
    engineDatabaseExtension.changeProcessInstanceStartDates(
        ImmutableMap.of(processInstanceEngineDto.getId(), eventTimestamp));
    engineDatabaseExtension.changeProcessInstanceEndDates(
        ImmutableMap.of(processInstanceEngineDto.getId(), eventTimestamp));
    engineDatabaseExtension.changeAllFlowNodeStartDates(
        ImmutableMap.of(processInstanceEngineDto.getId(), eventTimestamp));
    engineDatabaseExtension.changeAllFlowNodeEndDates(
        ImmutableMap.of(processInstanceEngineDto.getId(), eventTimestamp));

    importAllEngineEntitiesFromScratch();

    // when
    processEventCountAndTraces();

    // then
    assertThat(
            databaseIntegrationTestExtension.getAllStoredCamundaActivityEventsForDefinition(
                definitionKey))
        .extracting(CamundaActivityEventDto::getTimestamp)
        .allMatch(offsetDateTime -> offsetDateTime.equals(eventTimestamp));
    assertTracesAndCountsArePresentForDefinitionKey(definitionKey, processInstanceEngineDto, true);
  }

  @Test
  public void
      eventTracesAndCountsAreCreatedCorrectlyForProcessWithEventsWithIdenticalTimestamps_noOrderCountersFromEngine() {
    // given
    final String definitionKey = "myCamundaProcess1";
    final ProcessInstanceEngineDto processInstanceEngineDto =
        deployAndStartUserTaskProcessWithName(definitionKey);
    engineIntegrationExtension.finishAllRunningUserTasks();
    final OffsetDateTime eventTimestamp = OffsetDateTime.now().withNano(0);
    engineDatabaseExtension.changeProcessInstanceStartDates(
        ImmutableMap.of(processInstanceEngineDto.getId(), eventTimestamp));
    engineDatabaseExtension.changeProcessInstanceEndDates(
        ImmutableMap.of(processInstanceEngineDto.getId(), eventTimestamp));
    engineDatabaseExtension.changeAllFlowNodeStartDates(
        ImmutableMap.of(processInstanceEngineDto.getId(), eventTimestamp));
    engineDatabaseExtension.changeAllFlowNodeEndDates(
        ImmutableMap.of(processInstanceEngineDto.getId(), eventTimestamp));

    importAllEngineEntitiesFromScratch();
    removeStoredOrderCountersForDefinitionKey(definitionKey);

    // when
    processEventCountAndTraces();

    // then
    assertThat(
            databaseIntegrationTestExtension.getAllStoredCamundaActivityEventsForDefinition(
                definitionKey))
        .extracting(CamundaActivityEventDto::getTimestamp)
        .allMatch(offsetDateTime -> offsetDateTime.equals(eventTimestamp));
    assertTracesAndCountsArePresentForDefinitionKey(definitionKey, processInstanceEngineDto, false);
  }

  @Test
  public void eventTracesAndCountsAreCreatedCorrectlyForReimportedEvents_idempotentTraceStates() {
    // given
    final String definitionKey = "myCamundaProcess";
    final ProcessInstanceEngineDto processInstanceEngineDto1 =
        deployAndStartUserTaskProcessWithName(definitionKey);
    engineIntegrationExtension.finishAllRunningUserTasks();
    importAllEngineEntitiesFromScratch();

    // when
    processEventCountAndTraces();

    // then
    assertTracesAndCountsArePresentForDefinitionKey(definitionKey, processInstanceEngineDto1, true);
    final List<EventTraceStateDto> initialStoredTraceStates =
        getAllStoredCamundaEventTraceStatesForDefinitionKey(definitionKey);
    assertThat(getAllStoredCamundaEventsForEventDefinitionKey(definitionKey)).hasSize(6);

    // when import index reset and traces reprocessed after event reimport
    deleteTraceStateImportIndexForDefinitionKey(definitionKey);
    importAllEngineEntitiesFromScratch();
    processEventCountAndTraces();

    // then traces are the same as after first process
    assertTracesAndCountsArePresentForDefinitionKey(definitionKey, processInstanceEngineDto1, true);
    assertThat(initialStoredTraceStates)
        .containsExactlyElementsOf(
            getAllStoredCamundaEventTraceStatesForDefinitionKey(definitionKey));
    assertThat(getLastProcessedEntityTimestampFromElasticsearch(definitionKey))
        .isEqualTo(findMostRecentEventTimestamp(definitionKey));
    assertThat(getAllStoredCamundaEventsForEventDefinitionKey(definitionKey)).hasSize(12);
  }

  @SneakyThrows
  private void deleteTraceStateImportIndexForDefinitionKey(final String definitionKey) {
    databaseIntegrationTestExtension.deleteTraceStateImportIndexForDefinitionKey(definitionKey);
  }

  private void removeStoredOrderCountersForDefinitionKey(final String definitionKey) {
    databaseIntegrationTestExtension.removeStoredOrderCountersForDefinitionKey(definitionKey);
  }

  private void assertTracesAndCountsArePresentForDefinitionKey(
      final String definitionKey,
      final ProcessInstanceEngineDto processInstanceEngineDto,
      final boolean useOrderCounters) {
    assertThat(getAllStoredCamundaEventTraceStatesForDefinitionKey(definitionKey))
        .hasSize(1)
        .allSatisfy(
            eventTraceStateDto -> {
              assertThat(eventTraceStateDto.getTraceId())
                  .isEqualTo(processInstanceEngineDto.getId());
              assertThat(eventTraceStateDto.getEventTrace())
                  .hasSize(4)
                  .allSatisfy(
                      tracedEventDto -> {
                        assertThat(tracedEventDto.getGroup()).isEqualTo(definitionKey);
                        assertThat(tracedEventDto.getSource()).isEqualTo("camunda");
                        assertThat(tracedEventDto.getTimestamp()).isNotNull();
                        assertThat(useOrderCounters == (tracedEventDto.getOrderCounter() != null));
                      })
                  .extracting(TracedEventDto::getEventName)
                  .containsExactlyInAnyOrder(
                      START_EVENT,
                      applyCamundaTaskStartEventSuffix(USER_TASK),
                      applyCamundaTaskEndEventSuffix(USER_TASK),
                      END_EVENT);
              if (useOrderCounters) {
                assertThat(eventTraceStateDto.getEventTrace())
                    .isSortedAccordingTo(
                        Comparator.comparing(TracedEventDto::getTimestamp)
                            .thenComparing(TracedEventDto::getOrderCounter));
              } else {
                assertThat(eventTraceStateDto.getEventTrace())
                    .isSortedAccordingTo(Comparator.comparing(TracedEventDto::getTimestamp));
              }
            });
    assertThat(
            getAllStoredEventSequenceCountsForDefinitionKey(definitionKey).stream()
                .map(EventSequenceCountDto::getCount)
                .mapToLong(value -> value)
                .sum())
        .isEqualTo(4L);

    assertThat(getLastProcessedEntityTimestampFromElasticsearch(definitionKey))
        .isEqualTo(findMostRecentEventTimestamp(definitionKey));
  }

  private ProcessInstanceEngineDto deployAndStartUserTaskProcessWithName(final String processName) {
    return engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(processName, START_EVENT, END_EVENT, USER_TASK));
  }

  private Long findMostRecentEventTimestamp(final String definitionKey) {
    return getAllStoredCamundaEventsForEventDefinitionKey(definitionKey).stream()
        .filter(this::isStateTraceable)
        .map(CamundaActivityEventDto::getTimestamp)
        .mapToLong(e -> e.toInstant().toEpochMilli())
        .max()
        .getAsLong();
  }

  private boolean isStateTraceable(final CamundaActivityEventDto camundaActivityEventDto) {
    return !camundaActivityEventDto
            .getActivityType()
            .equalsIgnoreCase(CamundaEventService.PROCESS_START_TYPE)
        && !camundaActivityEventDto
            .getActivityType()
            .equalsIgnoreCase(CamundaEventService.PROCESS_END_TYPE);
  }

  private List<CamundaActivityEventDto> getAllStoredCamundaEventsForEventDefinitionKey(
      final String processDefinitionKey) {
    return databaseIntegrationTestExtension.getAllStoredCamundaActivityEventsForDefinition(
        processDefinitionKey);
  }

  private List<EventTraceStateDto> getAllStoredCamundaEventTraceStatesForDefinitionKey(
      final String definitionKey) {
    return getAllStoredDocumentsForIndexAsClass(
        getTraceStateIndexNameForDefinitionKey(definitionKey), EventTraceStateDto.class);
  }

  private List<EventSequenceCountDto> getAllStoredEventSequenceCountsForDefinitionKey(
      final String definitionKey) {
    return getAllStoredDocumentsForIndexAsClass(
        getSequenceCountIndexNameForDefinitionKey(definitionKey), EventSequenceCountDto.class);
  }

  private String getSequenceCountIndexNameForDefinitionKey(final String definitionKey) {
    return EventSequenceCountIndex.constructIndexName(definitionKey);
  }

  private String getTraceStateIndexNameForDefinitionKey(final String definitionKey) {
    return EventTraceStateIndex.constructIndexName(definitionKey);
  }
}
