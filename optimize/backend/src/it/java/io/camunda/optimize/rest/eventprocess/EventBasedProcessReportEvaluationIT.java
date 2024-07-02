/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// // TODO recreate C8 IT equivalent of this with #13337
// // package io.camunda.optimize.rest.eventprocess;
// //
// // import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// // import static
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel.VIEW;
// // import static
// io.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaTaskStartEventSuffix;
// // import static org.assertj.core.api.Assertions.assertThat;
// //
// // import io.camunda.optimize.dto.optimize.query.event.process.EventProcessInstanceDto;
// // import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// // import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.CanceledFlowNodesOnlyFilterDto;
// // import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.CompletedFlowNodesOnlyFilterDto;
// // import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.CompletedOrCanceledFlowNodesOnlyFilterDto;
// // import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
// // import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.RunningFlowNodesOnlyFilterDto;
// // import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// // import
// io.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessInstanceDto;
// // import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
// // import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// // import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// // import io.camunda.optimize.service.importing.eventprocess.AbstractEventProcessIT;
// // import io.camunda.optimize.service.util.ProcessReportDataType;
// // import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// // import java.util.Arrays;
// // import java.util.Collections;
// // import java.util.List;
// // import java.util.stream.Stream;
// // import org.assertj.core.groups.Tuple;
// // import org.junit.jupiter.api.Tag;
// // import org.junit.jupiter.api.Test;
// // import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
// // import org.junit.jupiter.params.ParameterizedTest;
// // import org.junit.jupiter.params.provider.Arguments;
// // import org.junit.jupiter.params.provider.MethodSource;
// //
// // @Tag(OPENSEARCH_PASSING)
// // public class EventBasedProcessReportEvaluationIT extends AbstractEventProcessIT {
// //
// //   @Test
// //   public void reportsUsingEventBasedProcessCanBeEvaluated() {
// //     // given
// //     final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
// //     importEngineEntities();
// //     publishEventMappingUsingProcessInstanceCamundaEvents(
// //         processInstanceEngineDto,
// //         createMappingsForEventProcess(
// //             processInstanceEngineDto,
// //             BPMN_START_EVENT_ID,
// //             applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
// //             BPMN_END_EVENT_ID));
// //     engineIntegrationExtension.finishAllRunningUserTasks();
// //     importEngineEntities();
// //
// //     // when
// //     executeImportCycle();
// //
// //     // then
// //     final List<EventProcessInstanceDto> processInstances =
// getEventProcessInstancesFromDatabase();
// //     assertThat(processInstances)
// //         .singleElement()
// //         .satisfies(
// //             processInstanceDto ->
// //                 assertProcessInstance(
// //                     processInstanceDto,
// //                     processInstanceEngineDto.getBusinessKey(),
// //                     Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)));
// //
// //     // when a report that uses the definition for that instance is evaluated
// //     final EventProcessInstanceDto savedInstance = processInstances.get(0);
// //     ProcessReportDataDto processReportDataDto =
// //         TemplatedProcessReportDataBuilder.createReportData()
// //             .setProcessDefinitionKey(savedInstance.getProcessDefinitionKey())
// //             .setProcessDefinitionVersion(savedInstance.getProcessDefinitionVersion())
// //             .setReportDataType(ProcessReportDataType.RAW_DATA)
// //             .build();
// //     final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
// //         reportClient.evaluateRawReport(processReportDataDto).getResult();
// //
// //     // then the event process instance appears in the results
// //     assertThat(result.getInstanceCount()).isEqualTo(1);
// //     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(1);
// //     assertThat(result.getData())
// //         .singleElement()
// //         .satisfies(
// //             resultInstance ->
// //                 assertThat(resultInstance.getProcessInstanceId())
// //                     .isEqualTo(savedInstance.getProcessInstanceId()));
// //   }
// //
// //   @ParameterizedTest(name = "using filter class {0}")
// //   @MethodSource("flowNodeStatusFiltersAndExpectedResults")
// //   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
// //   // Dependent on implementation of Reporting functionality for OpenSearch
// //   @EnabledIfSystemProperty(named = "CAMUNDA_OPTIMIZE_DATABASE", matches = "elasticsearch")
// //   public void reportsUsingEventBasedProcessCanBeEvaluatedUsingFlowNodeStatusFilters(
// //       Class<?> filterType,
// //       List<ProcessFilterDto<?>> filters,
// //       List<Tuple> expectedResults,
// //       Long expectedInstanceCount) {
// //     // given
// //     final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
// //     if (CanceledFlowNodesOnlyFilterDto.class.equals(filterType)) {
// //       engineIntegrationExtension.cancelActivityInstance(
// //           processInstanceEngineDto.getId(), USER_TASK_ID_ONE);
// //     }
// //     final ProcessInstanceEngineDto completedInstance =
// //         engineIntegrationExtension.startProcessInstance(
// //             processInstanceEngineDto.getDefinitionId(),
// //             Collections.emptyMap(),
// //             "completedBusinessKey");
// //     engineIntegrationExtension.finishAllRunningUserTasks(completedInstance.getId());
// //     engineIntegrationExtension.startProcessInstance(
// //         processInstanceEngineDto.getDefinitionId(), Collections.emptyMap(),
// "runningBusinessKey");
// //     importEngineEntities();
// //     publishEventMappingUsingProcessInstanceCamundaEvents(
// //         processInstanceEngineDto,
// //         createMappingsForEventProcess(
// //             processInstanceEngineDto,
// //             BPMN_START_EVENT_ID,
// //             applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
// //             BPMN_END_EVENT_ID));
// //     importEngineEntities();
// //     executeImportCycle();
// //     executeImportCycle();
// //
// //     // when a report that uses the definition for that instance is evaluated
// //     final EventProcessInstanceDto savedInstance =
// getEventProcessInstancesFromDatabase().get(0);
// //     ProcessReportDataDto processReportDataDto =
// //         TemplatedProcessReportDataBuilder.createReportData()
// //             .setProcessDefinitionKey(savedInstance.getProcessDefinitionKey())
// //             .setProcessDefinitionVersion(savedInstance.getProcessDefinitionVersion())
// //             .setReportDataType(ProcessReportDataType.FLOW_NODE_FREQ_GROUP_BY_FLOW_NODE)
// //             .build();
// //     processReportDataDto.setFilter(filters);
// //     ReportResultResponseDto<List<MapResultEntryDto>> result =
// //         reportClient.evaluateMapReport(processReportDataDto).getResult();
// //
// //     // then the event process instance appears in the results
// //     assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
// //     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(3L);
// //     assertThat(result.getFirstMeasureData())
// //         .hasSize(expectedResults.size())
// //         .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
// //         .containsExactlyInAnyOrderElementsOf(expectedResults);
// //   }
// //
// //   private static Stream<Arguments> flowNodeStatusFiltersAndExpectedResults() {
// //     return Stream.of(
// //         Arguments.of(
// //             RunningFlowNodesOnlyFilterDto.class,
// //             ProcessFilterBuilder.filter()
// //                 .runningFlowNodesOnly()
// //                 .filterLevel(VIEW)
// //                 .add()
// //                 .buildList(),
// //             Collections.singletonList(Tuple.tuple(USER_TASK_ID_ONE, 2.)),
// //             2L),
// //         Arguments.of(
// //             CompletedFlowNodesOnlyFilterDto.class,
// //             ProcessFilterBuilder.filter()
// //                 .completedFlowNodesOnly()
// //                 .filterLevel(VIEW)
// //                 .add()
// //                 .buildList(),
// //             Arrays.asList(
// //                 Tuple.tuple(BPMN_START_EVENT_ID, 3.),
// //                 Tuple.tuple(USER_TASK_ID_ONE, 1.),
// //                 Tuple.tuple(BPMN_END_EVENT_ID, 1.)),
// //             3L),
// //         Arguments.of(
// //             CompletedOrCanceledFlowNodesOnlyFilterDto.class,
// //             ProcessFilterBuilder.filter()
// //                 .completedOrCanceledFlowNodesOnly()
// //                 .filterLevel(VIEW)
// //                 .add()
// //                 .buildList(),
// //             Arrays.asList(
// //                 Tuple.tuple(BPMN_START_EVENT_ID, 3.),
// //                 Tuple.tuple(USER_TASK_ID_ONE, 1.),
// //                 Tuple.tuple(BPMN_END_EVENT_ID, 1.)),
// //             3L),
// //         Arguments.of(
// //             CanceledFlowNodesOnlyFilterDto.class,
// //             ProcessFilterBuilder.filter()
// //                 .canceledFlowNodesOnly()
// //                 .filterLevel(VIEW)
// //                 .add()
// //                 .buildList(),
// //             Collections.singletonList(Tuple.tuple(USER_TASK_ID_ONE, 1.)),
// //             1L));
// //   }
// // }
