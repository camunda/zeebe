/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.usertask.frequency.groupby.date.distributedby.assignee;
//
// import static com.google.common.collect.Lists.newArrayList;
// import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.IN;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.NOT_IN;
// import static
// io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnitMapper.mapToChronoUnit;
// import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_KEY;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// import static
// io.camunda.optimize.service.db.DatabaseConstants.NUMBER_OF_DATA_POINTS_FOR_AUTOMATIC_INTERVAL_SELECTION;
// import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
// import static io.camunda.optimize.test.util.DateModificationHelper.truncateToStartOfUnit;
// import static io.camunda.optimize.util.SuppressionConstants.UNCHECKED_CAST;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.google.common.collect.ImmutableList;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedByType;
// import
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator;
// import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
// import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.group.value.DateGroupByValueDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
// import io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto;
// import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
// import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.db.es.report.process.AbstractProcessDefinitionIT;
// import io.camunda.optimize.service.db.es.report.util.HyperMapAsserter;
// import io.camunda.optimize.service.util.ProcessReportDataType;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import io.camunda.optimize.test.util.DateCreationFreezer;
// import io.camunda.optimize.util.BpmnModels;
// import java.time.OffsetDateTime;
// import java.time.ZonedDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Objects;
// import java.util.stream.Collectors;
// import java.util.stream.IntStream;
// import java.util.stream.Stream;
// import org.assertj.core.groups.Tuple;
// import org.camunda.bpm.model.bpmn.Bpmn;
// import org.camunda.bpm.model.bpmn.BpmnModelInstance;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.Arguments;
// import org.junit.jupiter.params.provider.MethodSource;
//
// public abstract class UserTaskFrequencyByUserTaskDateByAssigneeReportEvaluationIT
//     extends AbstractProcessDefinitionIT {
//
//   @BeforeEach
//   public void init() {
//     // create second user
//     engineIntegrationExtension.addUser(SECOND_USER, SECOND_USER_FIRST_NAME,
// SECOND_USER_LAST_NAME);
//     engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);
//   }
//
//   @Test
//   public void reportEvaluationForOneProcessInstance() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deployOneUserTaskDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ProcessReportDataDto resultReportDataDto =
//         evaluationResponse.getReportDefinition().getData();
//
// assertThat(resultReportDataDto.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
//     assertThat(resultReportDataDto.getDefinitionVersions())
//         .containsExactly(processDefinition.getVersionAsString());
//     assertThat(resultReportDataDto.getView())
//         .usingRecursiveComparison()
//         .isEqualTo(new ProcessViewDto(ProcessViewEntity.USER_TASK, ViewProperty.FREQUENCY));
//     assertThat(resultReportDataDto.getGroupBy()).isNotNull();
//     assertThat(resultReportDataDto.getGroupBy().getType()).isEqualTo(getGroupByType());
//     assertThat(resultReportDataDto.getGroupBy().getValue())
//         .extracting(DateGroupByValueDto.class::cast)
//         .extracting(DateGroupByValueDto::getUnit)
//         .isEqualTo(AggregateByDateUnit.DAY);
//     assertThat(resultReportDataDto.getDistributedBy().getType())
//         .isEqualTo(DistributedByType.ASSIGNEE);
//
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     ZonedDateTime startOfToday = truncateToStartOfUnit(OffsetDateTime.now(), ChronoUnit.DAYS);
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(localDateTimeToString(startOfToday))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void reportEvaluationForOneProcessInstance_whenAssigneeCacheEmptyLabelEqualsKey() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deployOneUserTaskDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//
//     importAllEngineEntitiesFromScratch();
//
//     // cache is empty
//     embeddedOptimizeExtension.getUserTaskIdentityCache().resetCache();
//
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//
//     // when
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     ZonedDateTime startOfToday = truncateToStartOfUnit(OffsetDateTime.now(), ChronoUnit.DAYS);
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(localDateTimeToString(startOfToday))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_USERNAME)
//         .doAssert(actualResult);
//     // @formatter:on
//   }
//
//   @Test
//   public void reportEvaluationForMultipleProcessDefinitions() {
//     // given
//     final String key1 = "key1";
//     final String key2 = "key2";
//     // freeze date to avoid instability when test runs on the edge of the day
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     final ProcessDefinitionEngineDto processDefinition1 =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             BpmnModels.getSingleUserTaskDiagram(key1, USER_TASK_1));
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto1.getId());
//     final ProcessDefinitionEngineDto processDefinition2 =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             BpmnModels.getSingleUserTaskDiagram(key2, USER_TASK_2));
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto2.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition1);
//     reportData.getDefinitions().add(createReportDataDefinitionDto(key2));
//
//     // when
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     ZonedDateTime startOfToday = truncateToStartOfUnit(now, ChronoUnit.DAYS);
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(localDateTimeToString(startOfToday))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void resultIsSortedInAscendingOrder() {
//     // given
//     final OffsetDateTime referenceDate = OffsetDateTime.now();
//     ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     ProcessInstanceEngineDto processInstance1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks(SECOND_USER, SECOND_USERS_PASSWORD);
//     changeUserTaskDate(processInstance1, USER_TASK_1, referenceDate.minusDays(3));
//     changeUserTaskDate(processInstance1, USER_TASK_2, referenceDate.minusDays(1));
//
//     ProcessInstanceEngineDto processInstance2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks(SECOND_USER, SECOND_USERS_PASSWORD);
//     changeUserTaskDate(processInstance2, USER_TASK_1, referenceDate.minusDays(2));
//     changeUserTaskDate(processInstance2, USER_TASK_2, referenceDate.minusDays(4));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(4)))
//         .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(3)))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(2)))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(1)))
//         .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void testCustomOrderOnResultKeyIsApplied() {
//     // given
//     final OffsetDateTime referenceDate = OffsetDateTime.now();
//     ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     ProcessInstanceEngineDto processInstance1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks(SECOND_USER, SECOND_USERS_PASSWORD);
//     changeUserTaskDate(processInstance1, USER_TASK_1, referenceDate.minusDays(3));
//     changeUserTaskDate(processInstance1, USER_TASK_2, referenceDate.minusDays(1));
//
//     ProcessInstanceEngineDto processInstance2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks(SECOND_USER, SECOND_USERS_PASSWORD);
//     changeUserTaskDate(processInstance2, USER_TASK_1, referenceDate.minusDays(2));
//     changeUserTaskDate(processInstance2, USER_TASK_2, referenceDate.minusDays(4));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_KEY, SortOrder.DESC));
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(1)))
//         .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(2)))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(3)))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(4)))
//         .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void userTasksStartedAtSameIntervalAreGroupedTogether() {
//     // given
//     final OffsetDateTime referenceDate = OffsetDateTime.now();
//     ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     ProcessInstanceEngineDto processInstance1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks(SECOND_USER, SECOND_USERS_PASSWORD);
//     changeUserTaskDate(processInstance1, USER_TASK_1, referenceDate.minusDays(1));
//     changeUserTaskDate(processInstance1, USER_TASK_2, referenceDate.minusDays(2));
//
//     ProcessInstanceEngineDto processInstance2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks(SECOND_USER, SECOND_USERS_PASSWORD);
//     changeUserTaskDate(processInstance2, USER_TASK_1, referenceDate.minusDays(1));
//     changeUserTaskDate(processInstance2, USER_TASK_2, referenceDate.minusDays(2));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(2)))
//         .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, 2., SECOND_USER_FULL_NAME)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(1)))
//         .distributedByContains(DEFAULT_USERNAME, 2., DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void emptyIntervalBetweenTwoUserTaskDates() {
//     // given
//     final OffsetDateTime referenceDate = OffsetDateTime.now();
//     ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     ProcessInstanceEngineDto processInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks(SECOND_USER, SECOND_USERS_PASSWORD);
//     changeUserTaskDate(processInstance, USER_TASK_1, referenceDate.minusDays(1));
//     changeUserTaskDate(processInstance, USER_TASK_2, referenceDate.minusDays(3));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(3)))
//         .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(2)))
//         .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(1)))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @ParameterizedTest
//   @MethodSource("staticAggregateByDateUnits")
//   public void countGroupByDateUnit(final AggregateByDateUnit groupByDateUnit) {
//     // given
//     final ChronoUnit groupByUnitAsChrono = mapToChronoUnit(groupByDateUnit);
//     final int groupingCount = 5;
//     OffsetDateTime referenceDate = OffsetDateTime.now();
//
//     ProcessDefinitionEngineDto processDefinition = deployOneUserTaskDefinition();
//     List<ProcessInstanceEngineDto> processInstanceDtos =
//         IntStream.range(0, groupingCount)
//             .mapToObj(
//                 i -> {
//                   ProcessInstanceEngineDto processInstanceEngineDto =
//                       engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//                   processInstanceEngineDto.setProcessDefinitionKey(processDefinition.getKey());
//                   processInstanceEngineDto.setProcessDefinitionVersion(
//                       String.valueOf(processDefinition.getVersion()));
//                   return processInstanceEngineDto;
//                 })
//             .collect(Collectors.toList());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     updateUserTaskTime(processInstanceDtos, referenceDate, groupByUnitAsChrono);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReportData(processDefinition, groupByDateUnit);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     // we need to do the first assert here so that every loop has access to the the groupByAdder
//     // of the previous loop.
//     HyperMapAsserter.GroupByAdder groupByAdder =
//         HyperMapAsserter.asserter()
//             .processInstanceCount(groupingCount)
//             .processInstanceCountWithoutFilters(groupingCount)
//             .measure(ViewProperty.FREQUENCY)
//             .groupByContains(
//                 groupedByDateAsString(
//                     referenceDate.plus(0, groupByUnitAsChrono), groupByUnitAsChrono))
//             .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME);
//
//     for (int i = 1; i < groupingCount; i++) {
//       groupByAdder =
//           groupByAdder
//               .groupByContains(
//                   groupedByDateAsString(
//                       referenceDate.plus(i, groupByUnitAsChrono), groupByUnitAsChrono))
//               .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME);
//     }
//     groupByAdder.doAssert(result);
//   }
//
//   @Test
//   public void otherProcessDefinitionsDoNotAffectResult() {
//     // given
//     final OffsetDateTime referenceDate = OffsetDateTime.now();
//     ProcessDefinitionEngineDto processDefinition1 = deployOneUserTaskDefinition();
//     ProcessInstanceEngineDto processInstance1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     changeUserTaskDate(processInstance1, USER_TASK_1, referenceDate.minusDays(1));
//
//     ProcessDefinitionEngineDto processDefinition2 = deployOneUserTaskDefinition();
//     ProcessInstanceEngineDto processInstance2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     changeUserTaskDate(processInstance2, USER_TASK_1, referenceDate.minusDays(1));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition1);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(groupedByDayDateAsString(referenceDate.minusDays(1)))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
//         .doAssert(result);
//     // @formatter:on
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
//     final ProcessReportDataDto reportData =
//         createReportData(processKey, "1", AggregateByDateUnit.DAY);
//     reportData.setTenantIds(selectedTenants);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(selectedTenants.size());
//   }
//
//   @Test
//   public void filterWorks() {
//     // given
//     final OffsetDateTime referenceDate = OffsetDateTime.now();
//     ProcessDefinitionEngineDto processDefinition = deployOneUserTaskDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     final List<ProcessFilterDto<?>> processFilterDtoList =
//         ProcessFilterBuilder.filter().completedInstancesOnly().add().buildList();
//     reportData.setFilter(processFilterDtoList);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(groupedByDayDateAsString(referenceDate))
//         .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   public static Stream<Arguments> viewLevelAssigneeFilterScenarios() {
//     return Stream.of(
//         Arguments.of(
//             IN,
//             new String[] {SECOND_USER},
//             1L,
//             Collections.singletonList(Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             IN,
//             new String[] {DEFAULT_USERNAME, SECOND_USER},
//             1L,
//             Arrays.asList(Tuple.tuple(DEFAULT_USERNAME, 1.), Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {SECOND_USER},
//             1L,
//             Collections.singletonList(Tuple.tuple(DEFAULT_USERNAME, 1.))),
//         Arguments.of(
//             NOT_IN, new String[] {DEFAULT_USERNAME, SECOND_USER}, 0L, Collections.emptyList()));
//   }
//
//   @ParameterizedTest
//   @MethodSource("viewLevelAssigneeFilterScenarios")
//   @SuppressWarnings(UNCHECKED_CAST)
//   public void viewLevelFilterByAssigneeOnlyCountsThoseAssignees(
//       final MembershipFilterOperator filterOperator,
//       final String[] filterValues,
//       final Long expectedInstanceCount,
//       final List<Tuple> expectedResult) {
//     // given
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
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     final List<ProcessFilterDto<?>> assigneeFilter =
//         ProcessFilterBuilder.filter()
//             .assignee()
//             .ids(filterValues)
//             .operator(filterOperator)
//             .filterLevel(FilterApplicationLevel.VIEW)
//             .add()
//             .buildList();
//     reportData.setFilter(assigneeFilter);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
//     assertThat(result.getFirstMeasureData())
//         .flatExtracting(HyperMapResultEntryDto::getValue)
//         .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
//         .containsExactlyInAnyOrderElementsOf(expectedResult);
//   }
//
//   public static Stream<Arguments> instanceLevelAssigneeFilterScenarios() {
//     return Stream.of(
//         Arguments.of(
//             IN,
//             new String[] {SECOND_USER},
//             1L,
//             Arrays.asList(Tuple.tuple(DEFAULT_USERNAME, 1.), Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             IN,
//             new String[] {DEFAULT_USERNAME, SECOND_USER},
//             2L,
//             Arrays.asList(Tuple.tuple(DEFAULT_USERNAME, 3.), Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {SECOND_USER},
//             2L,
//             Arrays.asList(Tuple.tuple(DEFAULT_USERNAME, 3.), Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             NOT_IN, new String[] {DEFAULT_USERNAME, SECOND_USER}, 0L, Collections.emptyList()));
//   }
//
//   @ParameterizedTest
//   @MethodSource("instanceLevelAssigneeFilterScenarios")
//   @SuppressWarnings(UNCHECKED_CAST)
//   public void instanceLevelFilterByAssigneeOnlyCountsThoseAssigneesFromInstancesMatchingFilter(
//       final MembershipFilterOperator filterOperator,
//       final String[] filterValues,
//       final Long expectedInstanceCount,
//       final List<Tuple> expectedResult) {
//     // given
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
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     final List<ProcessFilterDto<?>> assigneeFilter =
//         ProcessFilterBuilder.filter()
//             .assignee()
//             .ids(filterValues)
//             .operator(filterOperator)
//             .filterLevel(FilterApplicationLevel.INSTANCE)
//             .add()
//             .buildList();
//     reportData.setFilter(assigneeFilter);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
//     assertThat(result.getFirstMeasureData())
//         .flatExtracting(HyperMapResultEntryDto::getValue)
//         .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
//         .containsExactlyInAnyOrderElementsOf(expectedResult);
//   }
//
//   public static Stream<Arguments> viewLevelCandidateGroupFilterScenarios() {
//     return Stream.of(
//         Arguments.of(
//             IN,
//             new String[] {SECOND_CANDIDATE_GROUP_ID},
//             1L,
//             Collections.singletonList(Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             IN,
//             new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
//             1L,
//             Arrays.asList(Tuple.tuple(DEFAULT_USERNAME, 1.), Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {SECOND_CANDIDATE_GROUP_ID},
//             1L,
//             Collections.singletonList(Tuple.tuple(DEFAULT_USERNAME, 1.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
//             0L,
//             Collections.emptyList()));
//   }
//
//   @ParameterizedTest
//   @MethodSource("viewLevelCandidateGroupFilterScenarios")
//   @SuppressWarnings(UNCHECKED_CAST)
//   public void viewLevelFilterByCandidateGroupOnlyCountsAssigneesFromThoseUserTasks(
//       final MembershipFilterOperator filterOperator,
//       final String[] filterValues,
//       final Long expectedInstanceCount,
//       final List<Tuple> expectedResult) {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
//
// engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     final List<ProcessFilterDto<?>> candidateGroupFilter =
//         ProcessFilterBuilder.filter()
//             .candidateGroups()
//             .ids(filterValues)
//             .operator(filterOperator)
//             .filterLevel(FilterApplicationLevel.VIEW)
//             .add()
//             .buildList();
//     reportData.setFilter(candidateGroupFilter);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
//     assertThat(result.getFirstMeasureData())
//         .flatExtracting(HyperMapResultEntryDto::getValue)
//         .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
//         .containsExactlyInAnyOrderElementsOf(expectedResult);
//   }
//
//   public static Stream<Arguments> instanceLevelCandidateGroupFilterScenarios() {
//     return Stream.of(
//         Arguments.of(
//             IN,
//             new String[] {SECOND_CANDIDATE_GROUP_ID},
//             1L,
//             Arrays.asList(Tuple.tuple(DEFAULT_USERNAME, 1.), Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             IN,
//             new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
//             2L,
//             Arrays.asList(Tuple.tuple(DEFAULT_USERNAME, 3.), Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {SECOND_CANDIDATE_GROUP_ID},
//             2L,
//             Arrays.asList(Tuple.tuple(DEFAULT_USERNAME, 3.), Tuple.tuple(SECOND_USER, 1.))),
//         Arguments.of(
//             NOT_IN,
//             new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
//             0L,
//             Collections.emptyList()));
//   }
//
//   @ParameterizedTest
//   @MethodSource("instanceLevelCandidateGroupFilterScenarios")
//   @SuppressWarnings(UNCHECKED_CAST)
//   public void
// instanceLevelFilterByCandidateGroupOnlyCountsAssigneesFromInstancesWithThoseUserTasks(
//       final MembershipFilterOperator filterOperator,
//       final String[] filterValues,
//       final Long expectedInstanceCount,
//       final List<Tuple> expectedResult) {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     final ProcessInstanceEngineDto firstInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, firstInstance.getId());
//
// engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         SECOND_USER, SECOND_USERS_PASSWORD, firstInstance.getId());
//     final ProcessInstanceEngineDto secondInstance =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, secondInstance.getId());
//     engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, secondInstance.getId());
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(processDefinition);
//     final List<ProcessFilterDto<?>> candidateGroupFilter =
//         ProcessFilterBuilder.filter()
//             .candidateGroups()
//             .ids(filterValues)
//             .operator(filterOperator)
//             .filterLevel(FilterApplicationLevel.INSTANCE)
//             .add()
//             .buildList();
//     reportData.setFilter(candidateGroupFilter);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
//     assertThat(result.getFirstMeasureData())
//         .flatExtracting(HyperMapResultEntryDto::getValue)
//         .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
//         .containsExactlyInAnyOrderElementsOf(expectedResult);
//   }
//
//   @Test
//   public void automaticIntervalSelection_simpleSetup() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployOneUserTaskDefinition();
//     ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     ProcessInstanceEngineDto processInstanceDto3 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     Map<String, OffsetDateTime> updates = new HashMap<>();
//     OffsetDateTime startOfToday = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);
//     updates.put(processInstanceDto1.getId(), startOfToday);
//     updates.put(processInstanceDto2.getId(), startOfToday);
//     updates.put(processInstanceDto3.getId(), startOfToday.minusDays(1));
//     changeUserTaskDates(updates);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReportData(processDefinition, AggregateByDateUnit.AUTOMATIC);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     final List<HyperMapResultEntryDto> resultData = result.getFirstMeasureData();
//     assertThat(resultData).hasSize(NUMBER_OF_DATA_POINTS_FOR_AUTOMATIC_INTERVAL_SELECTION);
//     assertFirstValueEquals(resultData, 1.);
//     assertLastValueEquals(resultData, 2.);
//   }
//
//   @Test
//   public void automaticIntervalSelection_takesAllUserTasksIntoAccount() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployOneUserTaskDefinition();
//     ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     ProcessInstanceEngineDto processInstanceDto3 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     Map<String, OffsetDateTime> updates = new HashMap<>();
//     OffsetDateTime startOfToday = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);
//     updates.put(processInstanceDto1.getId(), startOfToday);
//     updates.put(processInstanceDto2.getId(), startOfToday.plusDays(2));
//     updates.put(processInstanceDto3.getId(), startOfToday.plusDays(5));
//     changeUserTaskDates(updates);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReportData(processDefinition, AggregateByDateUnit.AUTOMATIC);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     final List<HyperMapResultEntryDto> resultData = result.getFirstMeasureData();
//     assertThat(resultData).hasSize(NUMBER_OF_DATA_POINTS_FOR_AUTOMATIC_INTERVAL_SELECTION);
//     assertFirstValueEquals(resultData, 1.);
//     assertLastValueEquals(resultData, 1.);
//     final int sumOfAllValues =
//         resultData.stream()
//             .map(HyperMapResultEntryDto::getValue)
//             .flatMap(List::stream)
//             .filter(Objects::nonNull)
//             .map(MapResultEntryDto::getValue)
//             .filter(Objects::nonNull)
//             .mapToInt(Double::intValue)
//             .sum();
//     assertThat(sumOfAllValues).isEqualTo(3);
//   }
//
//   @Test
//   public void automaticIntervalSelection_forNoData() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deployOneUserTaskDefinition();
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReportData(processDefinition, AggregateByDateUnit.AUTOMATIC);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     final List<HyperMapResultEntryDto> resultData = result.getFirstMeasureData();
//     assertThat(resultData).isEmpty();
//   }
//
//   @Test
//   public void automaticIntervalSelection_forOneDataPoint() {
//     // given there is only one data point
//     final ProcessDefinitionEngineDto processDefinition = deployOneUserTaskDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReportData(processDefinition, AggregateByDateUnit.AUTOMATIC);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then the single data point should be grouped by month
//     final List<HyperMapResultEntryDto> resultData = result.getFirstMeasureData();
//     ZonedDateTime nowStrippedToMonth =
//         truncateToStartOfUnit(OffsetDateTime.now(), ChronoUnit.MONTHS);
//     String nowStrippedToMonthAsString = localDateTimeToString(nowStrippedToMonth);
//     assertThat(resultData).hasSize(1);
//     assertThat(resultData)
//         .first()
//         .extracting(HyperMapResultEntryDto::getKey)
//         .isEqualTo(nowStrippedToMonthAsString);
//   }
//
//   @ParameterizedTest
//   @MethodSource("multiVersionArguments")
//   public void multipleVersionsRespectLatestNodesWhereLatestHasMoreFlowNodes(
//       final List<String> definitionVersionsThatSpanMultipleDefinitions) {
//     // given
//     ProcessDefinitionEngineDto firstDefinition = deployOneUserTaskDefinition();
//     engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks();
//
//     ProcessDefinitionEngineDto latestDefinition = deployTwoUserTasksDefinition();
//     engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks(SECOND_USER, SECOND_USERS_PASSWORD);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(latestDefinition);
//     reportData.setProcessDefinitionVersions(definitionVersionsThatSpanMultipleDefinitions);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(groupedByDayDateAsString(OffsetDateTime.now()))
//         .distributedByContains(DEFAULT_USERNAME, 2., DEFAULT_FULLNAME)
//         .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @ParameterizedTest
//   @MethodSource("multiVersionArguments")
//   public void multipleVersionsRespectLatestNodesWhereLatestHasFewerFlowNodes(
//       final List<String> definitionVersionsThatSpanMultipleDefinitions) {
//     // given
//     ProcessDefinitionEngineDto firstDefinition = deployTwoUserTasksDefinition();
//     engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//     engineIntegrationExtension.finishAllRunningUserTasks(SECOND_USER, SECOND_USERS_PASSWORD);
//
//     ProcessDefinitionEngineDto latestDefinition = deployOneUserTaskDefinition();
//     engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createGroupedByDayReport(latestDefinition);
//     reportData.setProcessDefinitionVersions(definitionVersionsThatSpanMultipleDefinitions);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.FREQUENCY)
//         .groupByContains(groupedByDayDateAsString(OffsetDateTime.now()))
//         .distributedByContains(DEFAULT_USERNAME, 2., DEFAULT_FULLNAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   private static Stream<List<String>> multiVersionArguments() {
//     return Stream.of(Arrays.asList("1", "2"), Collections.singletonList(ALL_VERSIONS));
//   }
//
//   private void assertLastValueEquals(
//       final List<HyperMapResultEntryDto> resultData, final Double expected) {
//     assertThat(resultData)
//         .last()
//         .extracting(HyperMapResultEntryDto::getValue)
//         .extracting(e -> e.get(0))
//         .extracting(MapResultEntryDto::getValue)
//         .isEqualTo(expected);
//   }
//
//   private void assertFirstValueEquals(
//       final List<HyperMapResultEntryDto> resultData, final Double expected) {
//     assertThat(resultData)
//         .first()
//         .extracting(HyperMapResultEntryDto::getValue)
//         .extracting(e -> e.get(0))
//         .extracting(MapResultEntryDto::getValue)
//         .isEqualTo(expected);
//   }
//
//   private void updateUserTaskTime(
//       List<ProcessInstanceEngineDto> procInsts, OffsetDateTime now, ChronoUnit unit) {
//     Map<String, OffsetDateTime> idToNewStartDate = new HashMap<>();
//     IntStream.range(0, procInsts.size())
//         .forEach(
//             i -> {
//               String id = procInsts.get(i).getId();
//               OffsetDateTime newStartDate = now.plus(i, unit);
//               idToNewStartDate.put(id, newStartDate);
//             });
//     changeUserTaskDates(idToNewStartDate);
//   }
//
//   protected ProcessReportDataDto createReportData(
//       final String processDefinitionKey,
//       final String version,
//       final AggregateByDateUnit groupByDateUnit) {
//     return createReportData(processDefinitionKey, ImmutableList.of(version), groupByDateUnit);
//   }
//
//   protected ProcessReportDataDto createReportData(
//       final String processDefinitionKey,
//       final List<String> versions,
//       final AggregateByDateUnit groupByDateUnit) {
//     return TemplatedProcessReportDataBuilder.createReportData()
//         .setProcessDefinitionKey(processDefinitionKey)
//         .setProcessDefinitionVersions(versions)
//         .setReportDataType(getReportDataType())
//         .setGroupByDateInterval(groupByDateUnit)
//         .build();
//   }
//
//   protected ProcessReportDataDto createGroupedByDayReport(
//       final ProcessDefinitionEngineDto processDefinition) {
//     return createReportData(processDefinition, AggregateByDateUnit.DAY);
//   }
//
//   protected ProcessReportDataDto createReportData(
//       final ProcessDefinitionEngineDto processDefinition,
//       final AggregateByDateUnit groupByDateUnit) {
//     return createReportData(
//         processDefinition.getKey(),
//         String.valueOf(processDefinition.getVersion()),
//         groupByDateUnit);
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
//               deployOneUserTaskDefinition(processKey, tenant);
//           engineIntegrationExtension.startProcessInstance(processDefinitionEngineDto.getId());
//         });
//     return processKey;
//   }
//
//   private ProcessDefinitionEngineDto deployOneUserTaskDefinition() {
//     return deployOneUserTaskDefinition("aProcess", null);
//   }
//
//   private ProcessDefinitionEngineDto deployOneUserTaskDefinition(String key, String tenantId) {
//     // @formatter:off
//     BpmnModelInstance modelInstance =
//         Bpmn.createExecutableProcess(key)
//             .startEvent(START_EVENT)
//             .userTask(USER_TASK_1)
//             .name(USER_TASK_1_NAME)
//             .endEvent(END_EVENT)
//             .done();
//     // @formatter:on
//     return engineIntegrationExtension.deployProcessAndGetProcessDefinition(modelInstance,
// tenantId);
//   }
//
//   protected ProcessDefinitionEngineDto deployTwoUserTasksDefinition() {
//     // @formatter:off
//     BpmnModelInstance modelInstance =
//         Bpmn.createExecutableProcess("aProcess")
//             .startEvent(START_EVENT)
//             .userTask(USER_TASK_1)
//             .name(USER_TASK_1_NAME)
//             .userTask(USER_TASK_2)
//             .name(USER_TASK_2_NAME)
//             .endEvent(END_EVENT)
//             .done();
//     // @formatter:on
//     return engineIntegrationExtension.deployProcessAndGetProcessDefinition(modelInstance);
//   }
//
//   protected String groupedByDayDateAsString(final OffsetDateTime referenceDate) {
//     return groupedByDateAsString(referenceDate, ChronoUnit.DAYS);
//   }
//
//   private String groupedByDateAsString(
//       final OffsetDateTime referenceDate, final ChronoUnit chronoUnit) {
//     return localDateTimeToString(truncateToStartOfUnit(referenceDate, chronoUnit));
//   }
//
//   protected abstract ProcessGroupByType getGroupByType();
//
//   protected abstract ProcessReportDataType getReportDataType();
//
//   protected abstract void changeUserTaskDates(final Map<String, OffsetDateTime> updates);
//
//   protected abstract void changeUserTaskDate(
//       final ProcessInstanceEngineDto processInstance,
//       final String userTaskKey,
//       final OffsetDateTime dateToChangeTo);
// }
