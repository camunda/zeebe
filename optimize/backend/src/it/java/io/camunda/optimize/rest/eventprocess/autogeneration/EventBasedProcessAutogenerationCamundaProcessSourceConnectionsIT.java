/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// // TODO recreate C8 IT equivalent of this with #13337
// // // TODO recreate C8 IT equivalent of this with #13337
// // // package io.camunda.optimize.rest.eventprocess.autogeneration;
// // //
// // // import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// // // import static
// io.camunda.optimize.service.util.EventDtoBuilderUtil.createCamundaProcessEndEventTypeDto;
// // // import static
// io.camunda.optimize.service.util.EventDtoBuilderUtil.createCamundaProcessStartEventTypeDto;
// // // import static org.assertj.core.api.Assertions.assertThat;
// // //
// // // import io.camunda.optimize.dto.optimize.query.event.process.EventMappingDto;
// // // import io.camunda.optimize.dto.optimize.query.event.process.EventProcessState;
// // // import io.camunda.optimize.dto.optimize.query.event.process.EventTypeDto;
// // // import
// io.camunda.optimize.dto.optimize.query.event.process.source.CamundaEventSourceEntryDto;
// // // import io.camunda.optimize.dto.optimize.query.event.process.source.EventScopeType;
// // // import io.camunda.optimize.dto.optimize.query.event.process.source.EventSourceEntryDto;
// // // import io.camunda.optimize.dto.optimize.rest.EventProcessMappingCreateRequestDto;
// // // import io.camunda.optimize.dto.optimize.rest.event.EventProcessMappingResponseDto;
// // // import io.camunda.optimize.service.util.BpmnModelUtil;
// // // import java.util.Arrays;
// // // import java.util.List;
// // // import java.util.Map;
// // // import java.util.stream.Stream;
// // // import org.camunda.bpm.model.bpmn.BpmnModelInstance;
// // // import org.camunda.bpm.model.bpmn.instance.FlowNode;
// // // import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
// // // import org.junit.jupiter.api.Tag;
// // // import org.junit.jupiter.params.ParameterizedTest;
// // // import org.junit.jupiter.params.provider.Arguments;
// // // import org.junit.jupiter.params.provider.MethodSource;
// // //
// // // @Tag(OPENSEARCH_PASSING)
// // // public class EventBasedProcessAutogenerationCamundaProcessSourceConnectionsIT
// // //     extends AbstractEventProcessAutogenerationIT {
// // //
// // //   @ParameterizedTest
// // //   @MethodSource("processStartEventModelCombinations")
// // //   public void createFromTwoCamundaProcessStartEndSources_singleStartEndEvents(
// // //       final BpmnModelInstance firstInstance, final BpmnModelInstance secondInstance) {
// // //     final CamundaEventSourceEntryDto firstCamundaSource =
// // //         deployDefinitionWithInstanceAndCreateEventSource(
// // //             firstInstance, EventScopeType.PROCESS_INSTANCE);
// // //     final EventTypeDto firstStart = createCamundaProcessStartEventTypeDto(PROCESS_ID_1);
// // //     final EventTypeDto firstEnd = createCamundaProcessEndEventTypeDto(PROCESS_ID_1);
// // //
// // //     final CamundaEventSourceEntryDto secondCamundaSource =
// // //         deployDefinitionWithInstanceAndCreateEventSource(
// // //             secondInstance, EventScopeType.PROCESS_INSTANCE);
// // //     final EventTypeDto secondStart = createCamundaProcessStartEventTypeDto(PROCESS_ID_2);
// // //     final EventTypeDto secondEnd = createCamundaProcessEndEventTypeDto(PROCESS_ID_2);
// // //
// // //     final List<EventSourceEntryDto<?>> sources =
// // //         Arrays.asList(firstCamundaSource, secondCamundaSource);
// // //     final EventProcessMappingCreateRequestDto createRequestDto =
// // //         buildAutogenerateCreateRequestDto(sources);
// // //
// // //     // when
// // //     final EventProcessMappingResponseDto processMapping =
// // //         autogenerateProcessAndGetMappingResponse(createRequestDto);
// // //
// // //     // then
// // //     final Map<String, EventMappingDto> mappings = processMapping.getMappings();
// // //     final BpmnModelInstance generatedInstance =
// // //         BpmnModelUtil.parseBpmnModel(processMapping.getXml());
// // //     assertProcessMappingConfiguration(processMapping, sources, EventProcessState.MAPPED);
// // //
// // //     // then the mappings contain the correct events and are all in the model
// // //     assertCorrectMappingsAndContainsEvents(
// // //         mappings, generatedInstance, Arrays.asList(firstStart, firstEnd, secondStart,
// secondEnd));
// // //
// assertThat(generatedInstance.getModelElementsByType(FlowNode.class)).hasSize(mappings.size());
// // //
// // //     // then the model elements are of the correct type and connected to expected nodes
// correctly
// // //     assertNodeConnection(
// // //         idOf(firstStart), START_EVENT, idOf(firstEnd), INTERMEDIATE_EVENT,
// generatedInstance);
// // //     assertNodeConnection(
// // //         idOf(firstEnd),
// // //         INTERMEDIATE_EVENT,
// // //         idOf(secondStart),
// // //         INTERMEDIATE_EVENT,
// // //         generatedInstance);
// // //     assertNodeConnection(
// // //         idOf(secondStart), INTERMEDIATE_EVENT, idOf(secondEnd), END_EVENT,
// generatedInstance);
// // //     assertNodeConnection(idOf(secondEnd), END_EVENT, null, null, generatedInstance);
// // //
// // //     // and the expected number of sequence flows exist
// // //     assertThat(generatedInstance.getModelElementsByType(SequenceFlow.class)).hasSize(3);
// // //   }
// // //
// // //   private static Stream<Arguments> processStartEventModelCombinations() {
// // //     return Stream.of(
// // //         Arguments.of(
// // //             singleStartSingleEndModel(PROCESS_ID_1, START_EVENT_ID_1, END_EVENT_ID_1),
// // //             singleStartSingleEndModel(PROCESS_ID_2, START_EVENT_ID_2, END_EVENT_ID_2)),
// // //         Arguments.of(
// // //             multipleStartMultipleEndModel(PROCESS_ID_1),
// // //             singleStartSingleEndModel(PROCESS_ID_2, START_EVENT_ID_2, END_EVENT_ID_2)),
// // //         Arguments.of(
// // //             singleStartSingleEndModel(PROCESS_ID_1, START_EVENT_ID_2, END_EVENT_ID_2),
// // //             multipleStartMultipleEndModel(PROCESS_ID_2)),
// // //         Arguments.of(
// // //             multipleStartMultipleEndModel(PROCESS_ID_1),
// // //             multipleStartMultipleEndModel(PROCESS_ID_2)),
// // //         Arguments.of(
// // //             multipleStartNoEndModel(PROCESS_ID_1),
// multipleStartSingleEndModel(PROCESS_ID_2)));
// // //   }
// // // }
