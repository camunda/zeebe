/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.eventprocess.autogeneration;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.util.EventDtoBuilderUtil.createCamundaEventTypeDto;
import static io.camunda.optimize.test.optimize.EventProcessClient.createExternalEventAllGroupsSourceEntry;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.event.process.EventMappingDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessState;
import io.camunda.optimize.dto.optimize.query.event.process.EventTypeDto;
import io.camunda.optimize.dto.optimize.query.event.process.source.CamundaEventSourceEntryDto;
import io.camunda.optimize.dto.optimize.query.event.process.source.EventScopeType;
import io.camunda.optimize.dto.optimize.query.event.process.source.EventSourceEntryDto;
import io.camunda.optimize.dto.optimize.rest.EventProcessMappingCreateRequestDto;
import io.camunda.optimize.dto.optimize.rest.event.EventProcessMappingResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.util.BpmnModelUtil;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class EventBasedProcessAutogenerationSourceOrderingMoreThanTwoSourcesIT
    extends AbstractEventProcessAutogenerationIT {

  @Test
  public void createFromTwoCamundaAndExternalSources_useSecondaryPlacingForExternalSource() {
    // the first and second Camunda sources have instances that can be correlated together by
    // business key
    final String firstBusinessKey = "businessKey1";
    BpmnModelInstance firstModelInstance =
        singleStartSingleEndModel(PROCESS_ID_1, START_EVENT_ID_1, END_EVENT_ID_1);
    final CamundaEventSourceEntryDto firstCamundaSource =
        deployDefinitionWithInstanceAndCreateEventSource(
            firstModelInstance, EventScopeType.START_END, firstBusinessKey);
    final EventTypeDto firstStart =
        createCamundaEventTypeDto(PROCESS_ID_1, START_EVENT_ID_1, START_EVENT_ID_1);
    final EventTypeDto firstEnd =
        createCamundaEventTypeDto(PROCESS_ID_1, END_EVENT_ID_1, END_EVENT_ID_1);

    BpmnModelInstance secondModelInstance =
        singleStartSingleEndModel(PROCESS_ID_2, START_EVENT_ID_2, END_EVENT_ID_2);
    final ProcessInstanceEngineDto secondInstanceEngineDto =
        deployDefinitionWithInstance(secondModelInstance, Collections.emptyMap(), firstBusinessKey);
    final CamundaEventSourceEntryDto secondCamundaSource =
        createCamundaSourceEntry(
            secondInstanceEngineDto.getProcessDefinitionKey(), EventScopeType.START_END);

    final EventTypeDto secondStart =
        createCamundaEventTypeDto(PROCESS_ID_2, START_EVENT_ID_2, START_EVENT_ID_2);
    final EventTypeDto secondEnd =
        createCamundaEventTypeDto(PROCESS_ID_2, END_EVENT_ID_2, END_EVENT_ID_2);

    // the external source and second Camunda source have instances that can be correlated together
    // by another
    // business key
    final String secondBusinessKey = "businessKey2";
    final Instant now = Instant.now();
    ingestEventAndProcessTraces(
        Arrays.asList(
            createCloudEventOfType(EVENT_A, secondBusinessKey, now.minusSeconds(10)),
            createCloudEventOfType(EVENT_B, secondBusinessKey, now)));

    engineIntegrationExtension.startProcessInstance(
        secondInstanceEngineDto.getDefinitionId(), Collections.emptyMap(), secondBusinessKey);
    importAllEngineEntitiesFromLastIndex();

    // We supply the sources in an order different to our expectations to test that order
    // determination is applied
    final List<EventSourceEntryDto<?>> sources =
        Arrays.asList(
            createExternalEventAllGroupsSourceEntry(), secondCamundaSource, firstCamundaSource);
    final EventProcessMappingCreateRequestDto createRequestDto =
        buildAutogenerateCreateRequestDto(sources);

    // when
    final EventProcessMappingResponseDto processMapping =
        autogenerateProcessAndGetMappingResponse(createRequestDto);

    // then
    final Map<String, EventMappingDto> mappings = processMapping.getMappings();
    final BpmnModelInstance generatedInstance =
        BpmnModelUtil.parseBpmnModel(processMapping.getXml());
    assertProcessMappingConfiguration(processMapping, sources, EventProcessState.MAPPED);

    // then the mappings contain the correct events and are all in the model
    assertCorrectMappingsAndContainsEvents(
        mappings,
        generatedInstance,
        Arrays.asList(firstStart, firstEnd, EVENT_A, EVENT_B, secondStart, secondEnd));
    assertThat(generatedInstance.getModelElementsByType(FlowNode.class)).hasSize(mappings.size());

    // then the model elements are sequenced by source in order of the start date of the instance of
    // the correlated
    // value across all correlatable instance
    assertNodeConnection(
        idOf(firstStart), START_EVENT, idOf(firstEnd), INTERMEDIATE_EVENT, generatedInstance);
    assertNodeConnection(
        idOf(firstEnd), INTERMEDIATE_EVENT, idOf(EVENT_A), INTERMEDIATE_EVENT, generatedInstance);
    assertNodeConnection(
        idOf(EVENT_A), INTERMEDIATE_EVENT, idOf(EVENT_B), INTERMEDIATE_EVENT, generatedInstance);
    assertNodeConnection(
        idOf(EVENT_B),
        INTERMEDIATE_EVENT,
        idOf(secondStart),
        INTERMEDIATE_EVENT,
        generatedInstance);
    assertNodeConnection(
        idOf(secondStart), INTERMEDIATE_EVENT, idOf(secondEnd), END_EVENT, generatedInstance);
    assertNodeConnection(idOf(secondEnd), END_EVENT, null, null, generatedInstance);

    // and the expected number of sequence flows exist
    assertThat(generatedInstance.getModelElementsByType(SequenceFlow.class)).hasSize(5);
  }
}
