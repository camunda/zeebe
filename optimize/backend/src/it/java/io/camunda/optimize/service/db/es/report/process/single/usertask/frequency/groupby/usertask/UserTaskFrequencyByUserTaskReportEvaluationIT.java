/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.usertask.frequency.groupby.usertask;
//
// import static com.google.common.collect.Lists.newArrayList;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.IN;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.NOT_IN;
// import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_KEY;
// import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_LABEL;
// import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_VALUE;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// import static io.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
// import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.google.common.collect.ImmutableList;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.ReportConstants;
// import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime;
// import
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
// import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
// import io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto;
// import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
// import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.db.es.report.process.AbstractProcessDefinitionIT;
// import io.camunda.optimize.service.db.es.report.util.MapResultAsserter;
// import io.camunda.optimize.service.db.es.report.util.MapResultUtil;
// import io.camunda.optimize.service.util.ProcessReportDataType;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import io.camunda.optimize.util.BpmnModels;
// import jakarta.ws.rs.core.Response;
// import java.time.OffsetDateTime;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.Comparator;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Objects;
// import java.util.Optional;
// import java.util.stream.Collectors;
// import java.util.stream.Stream;
// import lombok.AllArgsConstructor;
// import lombok.Data;
// import org.assertj.core.groups.Tuple;
// import org.camunda.bpm.model.bpmn.Bpmn;
// import org.camunda.bpm.model.bpmn.BpmnModelInstance;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.Arguments;
// import org.junit.jupiter.params.provider.MethodSource;
//
// public class UserTaskFrequencyByUserTaskReportEvaluationIT extends AbstractProcessDefinitionIT {
//
//   private static final String PROCESS_DEFINITION_KEY = "123";
//   private static final String USER_TASK_1 = "userTask1";
//   private static final String USER_TASK_2 = "userTask2";
//
//   @Test
//   public void reportEvaluationForOneProcessInstance() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishAllUserTasks(processInstanceDto);
//
//     importAllEngineEntitiesFromScratch();
//
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//
//     // when
//     final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>>
// evaluationResponse =
//         reportClient.evaluateMapReport(reportData);
//
//     // then
//     final ProcessReportDataDto resultReportDataDto =
//         evaluationResponse.getReportDefinition().getData();
//
// assertThat(resultReportDataDto.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
//     assertThat(resultReportDataDto.getDefinitionVersions())
//         .contains(processDefinition.getVersionAsString());
//     assertThat(resultReportDataDto.getView()).isNotNull();
//     assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
//
// assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.FREQUENCY);
//
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
// evaluationResponse.getResult();
//     assertThat(result.getFirstMeasureData()).isNotNull();
//     assertThat(result.getFirstMeasureData()).hasSize(2);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
//                 .get()
//                 .getValue())
//         .isEqualTo(1.);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2)
//                 .get()
//                 .getValue())
//         .isEqualTo(1.);
//     assertThat(result.getInstanceCount()).isEqualTo(1L);
//   }
//
//   @Test
//   public void reportEvaluationForSeveralProcessInstances() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishAllUserTasks(processInstanceDto1);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishAllUserTasks(processInstanceDto2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(2);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
//                 .get()
//                 .getValue())
//         .isEqualTo(2.);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2)
//                 .get()
//                 .getValue())
//         .isEqualTo(2.);
//     assertThat(result.getInstanceCount()).isEqualTo(2L);
//   }
//
//   @Test
//   public void reportEvaluationForSeveralProcessDefinitions() {
//     // given
//     final String key1 = "key1";
//     final String key2 = "key2";
//     final ProcessDefinitionEngineDto processDefinition1 =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             BpmnModels.getSingleUserTaskDiagram(key1, USER_TASK_1));
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto1.getId());
//     final ProcessDefinitionEngineDto processDefinition2 =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             BpmnModels.getSingleUserTaskDiagram(key2, USER_TASK_2));
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto2.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition1);
//     reportData.getDefinitions().add(createReportDataDefinitionDto(key2));
//     AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>> evaluationResponse =
//         reportClient.evaluateMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
// evaluationResponse.getResult();
//     // @formatter:off
//     MapResultAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupedByContains(USER_TASK_1, 1.)
//         .groupedByContains(USER_TASK_2, 1.)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void testCustomOrderOnResultKeyIsApplied() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishAllUserTasks(processInstanceDto1);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishAllUserTasks(processInstanceDto2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_KEY, SortOrder.DESC));
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//     assertThat(resultData).hasSize(2);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
//     final List<String> resultKeys =
//         resultData.stream().map(MapResultEntryDto::getKey).collect(Collectors.toList());
//     // expect ascending order
//     assertThat(resultKeys).isSortedAccordingTo(Comparator.reverseOrder());
//   }
//
//   @Test
//   public void testCustomOrderOnResultLabelIsApplied() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishAllUserTasks(processInstanceDto1);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishAllUserTasks(processInstanceDto2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_LABEL,
// SortOrder.DESC));
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//     assertThat(resultData).hasSize(2);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
//     final List<String> resultLabels =
//         resultData.stream().map(MapResultEntryDto::getLabel).collect(Collectors.toList());
//     assertThat(resultLabels).isSortedAccordingTo(Comparator.reverseOrder());
//   }
//
//   @Test
//   public void testCustomOrderOnResultValueIsApplied() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishAllUserTasks(processInstanceDto1);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto2.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_VALUE, SortOrder.ASC));
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(2);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
//     assertCorrectValueOrdering(result);
//   }
//
//   @Test
//   public void allVersionsRespectLatestNodesOnlyWhereLatestHasMoreNodes() {
//     // given
//     final ProcessDefinitionEngineDto firstDefinition = deployOneUserTasksDefinition();
//     final ProcessDefinitionEngineDto latestDefinition = deployTwoUserTasksDefinition();
//     assertThat(latestDefinition.getVersion()).isEqualTo(2);
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
//     finishAllUserTasks(processInstanceDto1);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     finishAllUserTasks(processInstanceDto2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(latestDefinition.getKey(), ReportConstants.ALL_VERSIONS);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(2);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
//                 .get()
//                 .getValue())
//         .isEqualTo(2.);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2)
//                 .get()
//                 .getValue())
//         .isEqualTo(1.);
//   }
//
//   @Test
//   public void multipleVersionsRespectLatestNodesOnlyWhereLatestHasMoreNodes() {
//     // given
//     final ProcessDefinitionEngineDto firstDefinition = deployOneUserTasksDefinition();
//     deployOneUserTasksDefinition();
//     final ProcessDefinitionEngineDto latestDefinition = deployTwoUserTasksDefinition();
//     assertThat(latestDefinition.getVersion()).isEqualTo(3);
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
//     finishAllUserTasks(processInstanceDto1);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     finishAllUserTasks(processInstanceDto2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(
//             latestDefinition.getKey(),
//             ImmutableList.of(
//                 firstDefinition.getVersionAsString(), latestDefinition.getVersionAsString()));
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(2);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
//                 .get()
//                 .getValue())
//         .isEqualTo(2.);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2)
//                 .get()
//                 .getValue())
//         .isEqualTo(1.);
//   }
//
//   @Test
//   public void allVersionsRespectLatestNodesOnlyWhereLatestHasLessNodes() {
//     // given
//     final ProcessDefinitionEngineDto firstDefinition = deployTwoUserTasksDefinition();
//     final ProcessDefinitionEngineDto latestDefinition = deployOneUserTasksDefinition();
//     assertThat(latestDefinition.getVersion()).isEqualTo(2);
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
//     finishAllUserTasks(processInstanceDto1);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     finishAllUserTasks(processInstanceDto2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(latestDefinition.getKey(), ReportConstants.ALL_VERSIONS);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(1);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
//                 .get()
//                 .getValue())
//         .isEqualTo(2.);
//   }
//
//   @Test
//   public void multipleVersionsRespectLatestNodesOnlyWhereLatestHasLessNodes() {
//     // given
//     final ProcessDefinitionEngineDto firstDefinition = deployTwoUserTasksDefinition();
//     deployTwoUserTasksDefinition();
//     final ProcessDefinitionEngineDto latestDefinition = deployOneUserTasksDefinition();
//     assertThat(latestDefinition.getVersion()).isEqualTo(3);
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
//     finishAllUserTasks(processInstanceDto1);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     finishAllUserTasks(processInstanceDto2);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(
//             latestDefinition.getKey(),
//             ImmutableList.of(
//                 firstDefinition.getVersionAsString(), latestDefinition.getVersionAsString()));
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(1);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
//                 .get()
//                 .getValue())
//         .isEqualTo(2.);
//   }
//
//   @Test
//   public void otherProcessDefinitionsDoNotInfluenceResult() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition1 = deployOneUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
//     finishAllUserTasks(processInstanceDto1);
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
//     finishAllUserTasks(processInstanceDto2);
//
//     final ProcessDefinitionEngineDto processDefinition2 = deployOneUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto3 =
//         engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
//     finishAllUserTasks(processInstanceDto3);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData1 = createReport(processDefinition1);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result1 =
//         reportClient.evaluateMapReport(reportData1).getResult();
//     final ProcessReportDataDto reportData2 = createReport(processDefinition2);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result2 =
//         reportClient.evaluateMapReport(reportData2).getResult();
//
//     // then
//     assertThat(result1.getFirstMeasureData()).hasSize(1);
//     assertThat(getExecutedFlowNodeCount(result1)).isEqualTo(1L);
//     assertThat(
//             MapResultUtil.getEntryForKey(result1.getFirstMeasureData(), USER_TASK_1)
//                 .get()
//                 .getValue())
//         .isEqualTo(2.);
//
//     assertThat(result2.getFirstMeasureData()).hasSize(1);
//     assertThat(getExecutedFlowNodeCount(result2)).isEqualTo(1L);
//     assertThat(
//             MapResultUtil.getEntryForKey(result2.getFirstMeasureData(), USER_TASK_1)
//                 .get()
//                 .getValue())
//         .isEqualTo(1.);
//   }
//
//   @Test
//   public void reportEvaluationSingleBucketFilteredBySingleTenant() {
//     // given
//     final String tenantId1 = "tenantId1";
//     final String tenantId2 = "tenantId2";
//     final List<String> selectedTenants = newArrayList(tenantId1);
//     final String processKey =
//         deployAndStartMultiTenantUserTaskProcess(newArrayList(null, tenantId1, tenantId2));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData = createReport(processKey, ReportConstants.ALL_VERSIONS);
//     reportData.setTenantIds(selectedTenants);
//     ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo((long) selectedTenants.size());
//   }
//
//   @Test
//   public void noUserTaskMatchesReturnsEmptyResult() {
//     // when
//     final ProcessReportDataDto reportData = createReport("nonExistingProcessDefinitionId", "1");
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).isEmpty();
//   }
//
//   @Data
//   @AllArgsConstructor
//   static class FlowNodeStatusTestValues {
//     List<ProcessFilterDto<?>> processFilter;
//     Map<String, Double> expectedFrequencyValues;
//     Integer expectedDataSize;
//     long expectedInstanceCount;
//   }
//
//   private static Map<String, Double> getExpectedResultsMap(
//       Double userTask1Results, Double userTask2Results) {
//     Map<String, Double> result = new HashMap<>();
//     result.put(USER_TASK_1, userTask1Results);
//     result.put(USER_TASK_2, userTask2Results);
//     return result;
//   }
//
//   protected static Stream<FlowNodeStatusTestValues> getFlowNodeStatusExpectedValues() {
//     return Stream.of(
//         new FlowNodeStatusTestValues(
//             ProcessFilterBuilder.filter().runningFlowNodesOnly().add().buildList(),
//             getExpectedResultsMap(1., 1.),
//             2,
//             2L),
//         new FlowNodeStatusTestValues(
//             ProcessFilterBuilder.filter().completedFlowNodesOnly().add().buildList(),
//             getExpectedResultsMap(1., null),
//             1,
//             1L),
//         new FlowNodeStatusTestValues(
//             ProcessFilterBuilder.filter().completedOrCanceledFlowNodesOnly().add().buildList(),
//             getExpectedResultsMap(1., null),
//             1,
//             1L));
//   }
//
//   @ParameterizedTest
//   @MethodSource("getFlowNodeStatusExpectedValues")
//   public void evaluateReportWithFlowNodeStatusFilter(
//       FlowNodeStatusTestValues flowNodeStatusTestValues) {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     final ProcessInstanceEngineDto firstInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     // finish first running task, second now runs but unclaimed
//     engineIntegrationExtension.finishAllRunningUserTasks(firstInstance.getId());
//
//     final ProcessInstanceEngineDto secondInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     // claim first running task
//     engineIntegrationExtension.claimAllRunningUserTasks(secondInstance.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.setFilter(flowNodeStatusTestValues.processFilter);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(flowNodeStatusTestValues.expectedDataSize);
//     assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1))
//         .isPresent()
//         .get()
//         .extracting(MapResultEntryDto::getValue)
//         .isEqualTo(flowNodeStatusTestValues.getExpectedFrequencyValues().get(USER_TASK_1));
//     Optional.ofNullable(flowNodeStatusTestValues.getExpectedFrequencyValues().get(USER_TASK_2))
//         .ifPresent(
//             expectedValue ->
//                 assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(),
// USER_TASK_2))
//                     .isPresent()
//                     .get()
//                     .extracting(MapResultEntryDto::getValue)
//                     .isEqualTo(expectedValue));
//     assertThat(result.getInstanceCount())
//         .isEqualTo(flowNodeStatusTestValues.getExpectedInstanceCount());
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2L);
//   }
//
//   @Test
//   public void evaluateReportWithFlowNodeStatusFilterCanceled() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     final ProcessInstanceEngineDto firstInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     // finish first running task, cancel second task
//     engineIntegrationExtension.finishAllRunningUserTasks(firstInstance.getId());
//     engineIntegrationExtension.cancelActivityInstance(firstInstance.getId(), USER_TASK_2);
//
//     final ProcessInstanceEngineDto secondInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     // claim then cancel first running task
//     engineIntegrationExtension.claimAllRunningUserTasks(secondInstance.getId());
//     engineIntegrationExtension.cancelActivityInstance(secondInstance.getId(), USER_TASK_1);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//
// reportData.setFilter(ProcessFilterBuilder.filter().canceledFlowNodesOnly().add().buildList());
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(2);
//     assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1))
//         .isPresent()
//         .get()
//         .extracting(MapResultEntryDto::getValue)
//         .isEqualTo(1.);
//     assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2))
//         .isPresent()
//         .get()
//         .extracting(MapResultEntryDto::getValue)
//         .isEqualTo(1.);
//     assertThat(result.getInstanceCount()).isEqualTo(2L);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2L);
//   }
//
//   @Test
//   public void processDefinitionContainsMultiInstanceBody() {
//     // given
//     BpmnModelInstance processWithMultiInstanceUserTask =
//         Bpmn
//             // @formatter:off
//             .createExecutableProcess("processWithMultiInstanceUserTask")
//             .startEvent()
//             .userTask(USER_TASK_1)
//             .multiInstance()
//             .cardinality("2")
//             .multiInstanceDone()
//             .endEvent()
//             .done();
//     // @formatter:on
//
//     final ProcessDefinitionEngineDto processDefinition =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             processWithMultiInstanceUserTask);
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(1);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
//     assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1))
//         .isPresent()
//         .get()
//         .extracting(MapResultEntryDto::getValue)
//         .isEqualTo(2.);
//   }
//
//   @Test
//   public void evaluateReportForMoreThanTenEvents() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
//
//     for (int i = 0; i < 11; i++) {
//       final ProcessInstanceEngineDto processInstanceDto =
//           engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//       engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
//     }
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).hasSize(1);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
//     assertThat(
//             MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
//                 .get()
//                 .getValue())
//         .isEqualTo(11.);
//   }
//
//   @Test
//   public void filterInReport() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
//
//     final OffsetDateTime processStartTime =
//         engineIntegrationExtension
//             .getHistoricProcessInstance(processInstanceDto.getId())
//             .getStartTime();
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.setFilter(createStartDateFilter(null, processStartTime.minusSeconds(1L)));
//     ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).isNotNull();
//     assertThat(result.getFirstMeasureData()).hasSize(1);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(0L);
//
//     // when
//     reportData = createReport(processDefinition);
//     reportData.setFilter(createStartDateFilter(processStartTime, null));
//     result = reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).isNotNull();
//     assertThat(result.getFirstMeasureData()).hasSize(1);
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
//     assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1))
//         .isPresent()
//         .get()
//         .extracting(MapResultEntryDto::getValue)
//         .isEqualTo(1.);
//   }
//
//   @Test
//   public void canAlsoBeEvaluatedIfAggregationTypeAndUserTaskDurationTimeIsDifferentFromDefault()
// {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
//     ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishAllUserTasks(processInstanceDto);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.getConfiguration().setAggregationTypes(new AggregationDto(AggregationType.MAX));
//     reportData.getConfiguration().setUserTaskDurationTimes(UserTaskDurationTime.IDLE);
//     final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>>
// evaluationResponse =
//         reportClient.evaluateMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
// evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(1L);
//     assertThat(result.getFirstMeasureData()).isNotNull();
//     assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
//     assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1))
//         .isPresent()
//         .get()
//         .extracting(MapResultEntryDto::getValue)
//         .isEqualTo(1.);
//   }
//
//   @Test
//   public void optimizeExceptionOnViewEntityIsNull() {
//     // given
//     final ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
//     dataDto.getView().setEntity(null);
//
//     // when
//     final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void optimizeExceptionOnViewPropertyIsNull() {
//     // given
//     final ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
//     dataDto.getView().setProperties((ViewProperty) null);
//
//     // when
//     final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void optimizeExceptionOnGroupByTypeIsNull() {
//     // given
//     final ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
//     dataDto.getGroupBy().setType(null);
//
//     // when
//     final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   public static Stream<Arguments> viewLevelAssigneeFilterScenarios() {
//     return Stream.of(
//         Arguments.of(
//             IN,
//             new String[] {SECOND_USER},
//             1L,
//             Collections.singletonList(Tuple.tuple(USER_TASK_2, 1.))),
//         Arguments.of(
//             IN,
//             new String[] {DEFAULT_USERNAME, SECOND_USER},
//             1L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, 1.), Tuple.tuple(USER_TASK_2, 1.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {SECOND_USER},
//             1L,
//             Collections.singletonList(Tuple.tuple(USER_TASK_1, 1.))),
//         Arguments.of(
//             NOT_IN, new String[] {DEFAULT_USERNAME, SECOND_USER}, 0L, Collections.emptyList()));
//   }
//
//   @ParameterizedTest
//   @MethodSource("viewLevelAssigneeFilterScenarios")
//   public void viewLevelFilterByAssigneeOnlyCountsUserTasksWithThatAssignee(
//       final MembershipFilterOperator filterOperator,
//       final String[] filterValues,
//       final Long expectedInstanceCount,
//       final List<Tuple> expectedResults) {
//     // given
//     engineIntegrationExtension.addUser(SECOND_USER, SECOND_USERS_PASSWORD);
//     engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);
//
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final List<ProcessFilterDto<?>> assigneeFilter =
//         ProcessFilterBuilder.filter()
//             .assignee()
//             .ids(filterValues)
//             .operator(filterOperator)
//             .filterLevel(FilterApplicationLevel.VIEW)
//             .add()
//             .buildList();
//     reportData.setFilter(assigneeFilter);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
//     assertThat(result.getFirstMeasureData())
//         .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
//         .containsExactlyInAnyOrderElementsOf(expectedResults);
//   }
//
//   public static Stream<Arguments> instanceLevelAssigneeFilterScenarios() {
//     return Stream.of(
//         Arguments.of(
//             IN,
//             new String[] {SECOND_USER},
//             1L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, 1.), Tuple.tuple(USER_TASK_2, 1.))),
//         Arguments.of(
//             IN,
//             new String[] {DEFAULT_USERNAME, SECOND_USER},
//             2L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, 2.), Tuple.tuple(USER_TASK_2, 2.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {SECOND_USER},
//             2L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, 2.), Tuple.tuple(USER_TASK_2, 2.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {DEFAULT_USERNAME, SECOND_USER},
//             0L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, null), Tuple.tuple(USER_TASK_2, null))));
//   }
//
//   @ParameterizedTest
//   @MethodSource("instanceLevelAssigneeFilterScenarios")
//   public void instanceLevelFilterByAssigneeOnlyCountsUserTasksFromInstancesWithThatAssignee(
//       final MembershipFilterOperator filterOperator,
//       final String[] filterValues,
//       final Long expectedInstanceCount,
//       final List<Tuple> expectedResults) {
//     // given
//     engineIntegrationExtension.addUser(SECOND_USER, SECOND_USERS_PASSWORD);
//     engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);
//
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     final ProcessInstanceEngineDto firstInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, firstInstance.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         SECOND_USER, SECOND_USERS_PASSWORD, firstInstance.getId());
//     final ProcessInstanceEngineDto secondInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, secondInstance.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, secondInstance.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final List<ProcessFilterDto<?>> assigneeFilter =
//         ProcessFilterBuilder.filter()
//             .assignee()
//             .ids(filterValues)
//             .operator(filterOperator)
//             .filterLevel(FilterApplicationLevel.INSTANCE)
//             .add()
//             .buildList();
//     reportData.setFilter(assigneeFilter);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
//     assertThat(result.getFirstMeasureData())
//         .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
//         .containsExactlyInAnyOrderElementsOf(expectedResults);
//   }
//
//   public static Stream<Arguments> viewLevelCandidateGroupFilterScenarios() {
//     return Stream.of(
//         Arguments.of(
//             IN,
//             new String[] {SECOND_CANDIDATE_GROUP_ID},
//             1L,
//             Collections.singletonList(Tuple.tuple(USER_TASK_2, 1.))),
//         Arguments.of(
//             IN,
//             new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
//             1L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, 1.), Tuple.tuple(USER_TASK_2, 1.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {SECOND_CANDIDATE_GROUP_ID},
//             1L,
//             Collections.singletonList(Tuple.tuple(USER_TASK_1, 1.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
//             0L,
//             Collections.emptyList()));
//   }
//
//   @ParameterizedTest
//   @MethodSource("viewLevelCandidateGroupFilterScenarios")
//   public void viewLevelFilterByCandidateGroupOnlyCountsUserTasksWithThatCandidateGroup(
//       final MembershipFilterOperator filterOperator,
//       final String[] filterValues,
//       final Long expectedInstanceCount,
//       final List<Tuple> expectedResults) {
//     // given
//     engineIntegrationExtension.createGroup(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.createGroup(SECOND_CANDIDATE_GROUP_ID);
//
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
// engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final List<ProcessFilterDto<?>> candidateGroupFilter =
//         ProcessFilterBuilder.filter()
//             .candidateGroups()
//             .ids(filterValues)
//             .operator(filterOperator)
//             .filterLevel(FilterApplicationLevel.VIEW)
//             .add()
//             .buildList();
//     reportData.setFilter(candidateGroupFilter);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
//     assertThat(result.getFirstMeasureData())
//         .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
//         .containsExactlyInAnyOrderElementsOf(expectedResults);
//   }
//
//   public static Stream<Arguments> instanceLevelCandidateGroupFilterScenarios() {
//     return Stream.of(
//         Arguments.of(
//             IN,
//             new String[] {SECOND_CANDIDATE_GROUP_ID},
//             1L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, 1.), Tuple.tuple(USER_TASK_2, 1.))),
//         Arguments.of(
//             IN,
//             new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
//             2L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, 2.), Tuple.tuple(USER_TASK_2, 2.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {SECOND_CANDIDATE_GROUP_ID},
//             2L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, 2.), Tuple.tuple(USER_TASK_2, 2.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
//             0L,
//             Arrays.asList(Tuple.tuple(USER_TASK_1, null), Tuple.tuple(USER_TASK_2, null))));
//   }
//
//   @ParameterizedTest
//   @MethodSource("instanceLevelCandidateGroupFilterScenarios")
//   public void
//       instanceLevelFilterByCandidateGroupOnlyCountsUserTasksFromInstanceWithThatCandidateGroup(
//           final MembershipFilterOperator filterOperator,
//           final String[] filterValues,
//           final Long expectedInstanceCount,
//           final List<Tuple> expectedResults) {
//     // given
//     engineIntegrationExtension.createGroup(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.createGroup(SECOND_CANDIDATE_GROUP_ID);
//
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
// engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final List<ProcessFilterDto<?>> candidateGroupFilter =
//         ProcessFilterBuilder.filter()
//             .candidateGroups()
//             .ids(filterValues)
//             .operator(filterOperator)
//             .filterLevel(FilterApplicationLevel.INSTANCE)
//             .add()
//             .buildList();
//     reportData.setFilter(candidateGroupFilter);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
//     assertThat(result.getFirstMeasureData())
//         .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
//         .containsExactlyInAnyOrderElementsOf(expectedResults);
//   }
//
//   protected ProcessReportDataDto createReport(
//       final String processDefinitionKey, final String version) {
//     return createReport(processDefinitionKey, ImmutableList.of(version));
//   }
//
//   protected ProcessReportDataDto createReport(
//       final String processDefinitionKey, final List<String> versions) {
//     return TemplatedProcessReportDataBuilder.createReportData()
//         .setProcessDefinitionKey(processDefinitionKey)
//         .setProcessDefinitionVersions(versions)
//         .setReportDataType(ProcessReportDataType.USER_TASK_FREQ_GROUP_BY_USER_TASK)
//         .build();
//   }
//
//   private ProcessReportDataDto createReport(final ProcessDefinitionEngineDto processDefinition) {
//     return createReport(processDefinition.getKey(),
// String.valueOf(processDefinition.getVersion()));
//   }
//
//   private List<ProcessFilterDto<?>> createStartDateFilter(
//       OffsetDateTime startDate, OffsetDateTime endDate) {
//     return ProcessFilterBuilder.filter()
//         .fixedInstanceStartDate()
//         .start(startDate)
//         .end(endDate)
//         .add()
//         .buildList();
//   }
//
//   private void finishAllUserTasks(final ProcessInstanceEngineDto processInstanceDto1) {
//     // finish first task
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto1.getId());
//     // finish second task
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto1.getId());
//   }
//
//   private String deployAndStartMultiTenantUserTaskProcess(final List<String> deployedTenants) {
//     final String processKey = "multiTenantProcess";
//     deployedTenants.stream()
//         .filter(Objects::nonNull)
//         .forEach(tenantId -> engineIntegrationExtension.createTenant(tenantId));
//     deployedTenants.forEach(
//         tenant -> {
//           final ProcessDefinitionEngineDto processDefinitionEngineDto =
//               deployOneUserTasksDefinition(processKey, tenant);
//           engineIntegrationExtension.startProcessInstance(processDefinitionEngineDto.getId());
//         });
//
//     return processKey;
//   }
//
//   private ProcessDefinitionEngineDto deployOneUserTasksDefinition() {
//     return deployOneUserTasksDefinition("aProcess", null);
//   }
//
//   private ProcessDefinitionEngineDto deployOneUserTasksDefinition(String key, String tenantId) {
//     return engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//         getSingleUserTaskDiagram(key), tenantId);
//   }
//
//   private ProcessDefinitionEngineDto deployTwoUserTasksDefinition() {
//     BpmnModelInstance modelInstance = getDoubleUserTaskDiagram();
//     return engineIntegrationExtension.deployProcessAndGetProcessDefinition(modelInstance);
//   }
//
//   private long getExecutedFlowNodeCount(
//       ReportResultResponseDto<List<MapResultEntryDto>> resultList) {
//     return resultList.getFirstMeasureData().stream()
//         .map(MapResultEntryDto::getValue)
//         .filter(Objects::nonNull)
//         .count();
//   }
//
//   private void assertCorrectValueOrdering(ReportResultResponseDto<List<MapResultEntryDto>>
// result) {
//     List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//     final List<Double> bucketValues =
//         resultData.stream().map(MapResultEntryDto::getValue).collect(Collectors.toList());
//     final List<Double> bucketValuesWithoutNullValue =
//         bucketValues.stream().filter(Objects::nonNull).collect(Collectors.toList());
//     assertThat(bucketValuesWithoutNullValue).isSortedAccordingTo(Comparator.naturalOrder());
//     for (int i = resultData.size() - 1; i > getExecutedFlowNodeCount(result) - 1; i--) {
//       assertThat(bucketValues.get(i)).isNull();
//     }
//   }
// }
