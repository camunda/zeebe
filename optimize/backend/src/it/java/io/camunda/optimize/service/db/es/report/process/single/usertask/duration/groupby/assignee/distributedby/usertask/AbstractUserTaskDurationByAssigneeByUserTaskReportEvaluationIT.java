/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.usertask.duration.groupby.assignee.distributedby.usertask;
//
// import static com.google.common.collect.Lists.newArrayList;
// import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_KEY;
// import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_LABEL;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// import static
// io.camunda.optimize.service.db.es.report.command.modules.distributed_by.process.identity.ProcessDistributedByIdentity.DISTRIBUTE_BY_IDENTITY_MISSING_KEY;
// import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
// import static
// io.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurationsDefaultAggr;
// import static io.camunda.optimize.test.util.DurationAggregationUtil.getSupportedAggregationTypes;
// import static io.camunda.optimize.util.SuppressionConstants.SAME_PARAM_VALUE;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.google.common.collect.ImmutableList;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.ReportConstants;
// import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedByType;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
// import io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto;
// import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
// import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.db.es.report.process.AbstractProcessDefinitionIT;
// import io.camunda.optimize.service.db.es.report.util.HyperMapAsserter;
// import io.camunda.optimize.service.security.util.LocalDateUtil;
// import io.camunda.optimize.test.util.DateCreationFreezer;
// import io.camunda.optimize.util.BpmnModels;
// import jakarta.ws.rs.core.Response;
// import java.time.OffsetDateTime;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Objects;
// import java.util.stream.Stream;
// import lombok.Data;
// import org.camunda.bpm.model.bpmn.Bpmn;
// import org.camunda.bpm.model.bpmn.BpmnModelInstance;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.MethodSource;
//
// @SuppressWarnings(SAME_PARAM_VALUE)
// public abstract class AbstractUserTaskDurationByAssigneeByUserTaskReportEvaluationIT
//     extends AbstractProcessDefinitionIT {
//
//   private static final String PROCESS_DEFINITION_KEY = "123";
//   protected static final String USER_TASK_1 = "userTask1";
//   protected static final String USER_TASK_2 = "userTask2";
//   protected static final String USER_TASK_A = "userTaskA";
//   protected static final String USER_TASK_B = "userTaskB";
//   protected static final String USER_TASK_1_NAME = "userTask1Name";
//   protected static final String USER_TASK_2_NAME = "userTask2Name";
//   private static final Double UNASSIGNED_TASK_DURATION = 500.;
//   protected static final Double[] SET_DURATIONS = new Double[] {10., 20.};
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
//     ProcessDefinitionEngineDto processDefinition = deployFourUserTasksDefinition();
//     ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto);
//
//     final long setDuration = 20L;
//     changeDuration(processInstanceDto, setDuration);
//     importAllEngineEntitiesFromScratch();
//
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//
//     // when
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
//     assertThat(resultReportDataDto.getView()).isNotNull();
//     assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
//
// assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
//     assertThat(resultReportDataDto.getConfiguration().getUserTaskDurationTimes())
//         .containsExactly(getUserTaskDurationTime());
//     assertThat(resultReportDataDto.getDistributedBy().getType())
//         .isEqualTo(DistributedByType.USER_TASK);
//
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(USER_TASK_1, 20.)
//         .distributedByContains(USER_TASK_2, null)
//         .distributedByContains(USER_TASK_A, 20.)
//         .distributedByContains(USER_TASK_B, null)
//         .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//         .distributedByContains(USER_TASK_1, null)
//         .distributedByContains(USER_TASK_2, 20.)
//         .distributedByContains(USER_TASK_A, null)
//         .distributedByContains(USER_TASK_B, 20.)
//         .doAssert(actualResult);
//     // @formatter:on
//   }
//
//   @Test
//   public void reportEvaluationForOneProcessInstance_whenAssigneeCacheEmptyLabelEqualsKey() {
//     // given
//     ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
//
//     changeDuration(processInstanceDto, 1.);
//     importAllEngineEntitiesFromScratch();
//
//     // cache is empty
//     embeddedOptimizeExtension.getUserTaskIdentityCache().resetCache();
//
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//
//     // when
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         evaluationResponse.getResult();
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_USERNAME)
//         .distributedByContains(USER_TASK_1, 1., USER_TASK_1_NAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void reportEvaluationForOneProcessInstanceWithUnassignedTasks() {
//     // given
//     // set current time to now for easier evaluation of duration of unassigned tasks
//     OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     ProcessDefinitionEngineDto processDefinition = deployFourUserTasksDefinition();
//
//     ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishCurrentUserTaskWithDefaultUser(processInstanceDto);
//
//     changeDuration(processInstanceDto, USER_TASK_1, SET_DURATIONS[1]);
//     changeDuration(processInstanceDto, USER_TASK_A, SET_DURATIONS[1]);
//     changeUserTaskStartDate(processInstanceDto, now, USER_TASK_2, UNASSIGNED_TASK_DURATION);
//     changeUserTaskStartDate(processInstanceDto, now, USER_TASK_B, UNASSIGNED_TASK_DURATION);
//     importAllEngineEntitiesFromScratch();
//
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//
//     // when
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
//     assertThat(resultReportDataDto.getView()).isNotNull();
//     assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
//
// assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
//     assertThat(resultReportDataDto.getConfiguration().getUserTaskDurationTimes())
//         .containsExactly(getUserTaskDurationTime());
//     assertThat(resultReportDataDto.getDistributedBy().getType())
//         .isEqualTo(DistributedByType.USER_TASK);
//
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     assertHyperMap_ForOneProcessInstanceWithUnassignedTasks(actualResult);
//   }
//
//   protected void assertHyperMap_ForOneProcessInstanceWithUnassignedTasks(
//       final ReportResultResponseDto<List<HyperMapResultEntryDto>> result) {
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(
//             USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[1]))
//         .distributedByContains(USER_TASK_2, null)
//         .distributedByContains(
//             USER_TASK_A, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[1]))
//         .distributedByContains(USER_TASK_B, null)
//         .groupByContains(DISTRIBUTE_BY_IDENTITY_MISSING_KEY, getLocalizedUnassignedLabel())
//         .distributedByContains(USER_TASK_1, null)
//         .distributedByContains(USER_TASK_2, UNASSIGNED_TASK_DURATION)
//         .distributedByContains(USER_TASK_A, null)
//         .distributedByContains(USER_TASK_B, UNASSIGNED_TASK_DURATION)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void reportEvaluationForSeveralProcessDefinitions() {
//     // given
//     final String key1 = "key1";
//     final String key2 = "key2";
//
//     final ProcessDefinitionEngineDto processDefinition1 =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             BpmnModels.getSingleUserTaskDiagram(key1, USER_TASK_1));
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
//     finishCurrentUserTaskWithDefaultUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, SET_DURATIONS[0]);
//     final ProcessDefinitionEngineDto processDefinition2 =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             BpmnModels.getSingleUserTaskDiagram(key2, USER_TASK_2));
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto2.getId());
//     changeDuration(processInstanceDto2, SET_DURATIONS[1]);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition1);
//     reportData.getDefinitions().add(createReportDataDefinitionDto(key2));
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(
//             USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[0]))
//         .distributedByContains(USER_TASK_2, null)
//         .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//         .distributedByContains(USER_TASK_1, null)
//         .distributedByContains(
//             USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[1]))
//         .doAssert(actualResult);
//     // @formatter:on
//   }
//
//   @Test
//   public void reportEvaluationForSeveralProcessInstances() {
//     // given
//     // set current time to now for easier evaluation of duration of unassigned tasks
//     OffsetDateTime now = OffsetDateTime.now();
//     LocalDateUtil.setCurrentTime(now);
//     final ProcessDefinitionEngineDto processDefinition = deployFourUserTasksDefinition();
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//
//     changeDuration(processInstanceDto1, SET_DURATIONS[0]);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishCurrentUserTaskWithDefaultUser(processInstanceDto2);
//
//     changeDuration(processInstanceDto2, SET_DURATIONS[1]);
//     changeUserTaskStartDate(processInstanceDto2, now, USER_TASK_2, UNASSIGNED_TASK_DURATION);
//     changeUserTaskStartDate(processInstanceDto2, now, USER_TASK_B, UNASSIGNED_TASK_DURATION);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     assertHyperMap_ForSeveralProcessInstances(actualResult);
//   }
//
//   protected void assertHyperMap_ForSeveralProcessInstances(
//       final ReportResultResponseDto<List<HyperMapResultEntryDto>> result) {
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(
//             USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS))
//         .distributedByContains(USER_TASK_2, null)
//         .distributedByContains(
//             USER_TASK_A, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS))
//         .distributedByContains(USER_TASK_B, null)
//         .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//         .distributedByContains(USER_TASK_1, null)
//         .distributedByContains(
//             USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[0]))
//         .distributedByContains(USER_TASK_A, null)
//         .distributedByContains(
//             USER_TASK_B, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[0]))
//         .groupByContains(DISTRIBUTE_BY_IDENTITY_MISSING_KEY, getLocalizedUnassignedLabel())
//         .distributedByContains(USER_TASK_1, null)
//         .distributedByContains(USER_TASK_2, UNASSIGNED_TASK_DURATION)
//         .distributedByContains(USER_TASK_A, null)
//         .distributedByContains(USER_TASK_B, UNASSIGNED_TASK_DURATION)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void reportEvaluationForSeveralProcessInstancesWithAllAggregationTypes() {
//     // given
//     // set current time to now for easier evaluation of duration of unassigned tasks
//     OffsetDateTime now = OffsetDateTime.now();
//     LocalDateUtil.setCurrentTime(now);
//
//     final ProcessDefinitionEngineDto processDefinition = deployFourUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, SET_DURATIONS[0]);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishCurrentUserTaskWithDefaultUser(processInstanceDto2);
//
//     changeDuration(processInstanceDto2, USER_TASK_1, SET_DURATIONS[1]);
//     changeDuration(processInstanceDto2, USER_TASK_A, SET_DURATIONS[1]);
//     changeUserTaskStartDate(processInstanceDto2, now, USER_TASK_2, UNASSIGNED_TASK_DURATION);
//     changeUserTaskStartDate(processInstanceDto2, now, USER_TASK_B, UNASSIGNED_TASK_DURATION);
//
//     importAllEngineEntitiesFromScratch();
//
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());
//
//     // when
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertHyperMap_ForSeveralProcessInstancesWithAllAggregationTypes(result);
//   }
//
//   protected void assertHyperMap_ForSeveralProcessInstancesWithAllAggregationTypes(
//       final ReportResultResponseDto<List<HyperMapResultEntryDto>> result) {
//     assertThat(result.getMeasures())
//         .extracting(MeasureResponseDto::getAggregationType)
//         .containsExactly(getSupportedAggregationTypes());
//     final HyperMapAsserter hyperMapAsserter =
//
// HyperMapAsserter.asserter().processInstanceCount(2L).processInstanceCountWithoutFilters(2L);
//     Arrays.stream(getSupportedAggregationTypes())
//         .forEach(
//             aggType -> {
//               // @formatter:off
//               hyperMapAsserter
//                   .measure(ViewProperty.DURATION, aggType, getUserTaskDurationTime())
//                   .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//                   .distributedByContains(
//                       USER_TASK_1,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(SET_DURATIONS)
//                           .get(aggType))
//                   .distributedByContains(USER_TASK_2, null)
//                   .distributedByContains(
//                       USER_TASK_A,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(SET_DURATIONS)
//                           .get(aggType))
//                   .distributedByContains(USER_TASK_B, null)
//                   .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//                   .distributedByContains(USER_TASK_1, null)
//                   .distributedByContains(
//                       USER_TASK_2,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(SET_DURATIONS[0])
//                           .get(aggType))
//                   .distributedByContains(USER_TASK_A, null)
//                   .distributedByContains(
//                       USER_TASK_B,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(SET_DURATIONS[0])
//                           .get(aggType))
//                   .groupByContains(
//                       DISTRIBUTE_BY_IDENTITY_MISSING_KEY, getLocalizedUnassignedLabel())
//                   .distributedByContains(USER_TASK_1, null)
//                   .distributedByContains(USER_TASK_2, UNASSIGNED_TASK_DURATION)
//                   .distributedByContains(USER_TASK_A, null)
//                   .distributedByContains(USER_TASK_B, UNASSIGNED_TASK_DURATION)
//                   .add()
//                   .add();
//               // @formatter:on
//             });
//     hyperMapAsserter.doAssert(result);
//   }
//
//   @Test
//   public void evaluateReportForMultipleEvents() {
//     // given
//     // set current time to now for easier evaluation of duration of unassigned tasks
//     OffsetDateTime now = OffsetDateTime.now();
//     LocalDateUtil.setCurrentTime(now);
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, USER_TASK_1, SET_DURATIONS[0]);
//     changeDuration(processInstanceDto1, USER_TASK_2, SET_DURATIONS[1]);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishCurrentUserTaskWithDefaultUser(processInstanceDto2);
//     changeDuration(processInstanceDto2, USER_TASK_1, SET_DURATIONS[0]);
//     changeUserTaskStartDate(processInstanceDto2, now, USER_TASK_2, UNASSIGNED_TASK_DURATION);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     assertHyperMap_ForMultipleEvents(actualResult);
//   }
//
//   protected void assertHyperMap_ForMultipleEvents(
//       final ReportResultResponseDto<List<HyperMapResultEntryDto>> result) {
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(USER_TASK_1, SET_DURATIONS[0], USER_TASK_1_NAME)
//         .distributedByContains(USER_TASK_2, null, USER_TASK_2_NAME)
//         .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//         .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
//         .distributedByContains(USER_TASK_2, SET_DURATIONS[1], USER_TASK_2_NAME)
//         .groupByContains(DISTRIBUTE_BY_IDENTITY_MISSING_KEY, getLocalizedUnassignedLabel())
//         .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
//         .distributedByContains(USER_TASK_2, UNASSIGNED_TASK_DURATION, USER_TASK_2_NAME)
//         .doAssert(result);
//     // @formatter:on
//   }
//
//   @Test
//   public void evaluateReportForMultipleEventsWithAllAggregationTypes() {
//     // given
//     // set current time to now for easier evaluation of duration of unassigned tasks
//     OffsetDateTime now = OffsetDateTime.now();
//     LocalDateUtil.setCurrentTime(now);
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, USER_TASK_1, SET_DURATIONS[0]);
//     changeDuration(processInstanceDto1, USER_TASK_2, SET_DURATIONS[0]);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishCurrentUserTaskWithDefaultUser(processInstanceDto2);
//     changeDuration(processInstanceDto2, USER_TASK_1, SET_DURATIONS[1]);
//     changeDuration(processInstanceDto2, USER_TASK_2, SET_DURATIONS[1]);
//     changeUserTaskStartDate(processInstanceDto2, now, USER_TASK_2, UNASSIGNED_TASK_DURATION);
//     changeUserTaskStartDate(processInstanceDto2, now, USER_TASK_B, UNASSIGNED_TASK_DURATION);
//
//     importAllEngineEntitiesFromScratch();
//
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());
//
//     // when
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertHyperMap_ForMultipleEventsWithAllAggregationTypes(result);
//   }
//
//   protected void assertHyperMap_ForMultipleEventsWithAllAggregationTypes(
//       final ReportResultResponseDto<List<HyperMapResultEntryDto>> result) {
//     assertThat(result.getMeasures())
//         .extracting(MeasureResponseDto::getAggregationType)
//         .containsExactly(getSupportedAggregationTypes());
//     final HyperMapAsserter hyperMapAsserter =
//
// HyperMapAsserter.asserter().processInstanceCount(2L).processInstanceCountWithoutFilters(2L);
//     Arrays.stream(getSupportedAggregationTypes())
//         .forEach(
//             aggType -> {
//               // @formatter:off
//               hyperMapAsserter
//                   .measure(ViewProperty.DURATION, aggType, getUserTaskDurationTime())
//                   .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//                   .distributedByContains(
//                       USER_TASK_1,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(SET_DURATIONS)
//                           .get(aggType),
//                       USER_TASK_1_NAME)
//                   .distributedByContains(USER_TASK_2, null, USER_TASK_2_NAME)
//                   .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//                   .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
//                   .distributedByContains(USER_TASK_2, SET_DURATIONS[0], USER_TASK_2_NAME)
//                   .groupByContains(
//                       DISTRIBUTE_BY_IDENTITY_MISSING_KEY, getLocalizedUnassignedLabel())
//                   .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
//                   .distributedByContains(USER_TASK_2, UNASSIGNED_TASK_DURATION, USER_TASK_2_NAME)
//                   .add()
//                   .add();
//               // @formatter:on
//             });
//     hyperMapAsserter.doAssert(result);
//   }
//
//   @Test
//   public void testCustomOrderOnResultKeyIsApplied() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, USER_TASK_1, SET_DURATIONS[0]);
//     changeDuration(processInstanceDto1, USER_TASK_2, SET_DURATIONS[0]);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto2);
//     changeDuration(processInstanceDto2, USER_TASK_1, SET_DURATIONS[1]);
//     changeDuration(processInstanceDto2, USER_TASK_2, SET_DURATIONS[1]);
//
//     importAllEngineEntitiesFromScratch();
//
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_KEY, SortOrder.DESC));
//     reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());
//
//     // when
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getMeasures())
//         .extracting(MeasureResponseDto::getAggregationType)
//         .containsExactly(getSupportedAggregationTypes());
//     final HyperMapAsserter hyperMapAsserter =
//
// HyperMapAsserter.asserter().processInstanceCount(2L).processInstanceCountWithoutFilters(2L);
//     Arrays.stream(getSupportedAggregationTypes())
//         .forEach(
//             aggType -> {
//               // @formatter:off
//               hyperMapAsserter
//                   .measure(ViewProperty.DURATION, aggType, getUserTaskDurationTime())
//                   .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//                   .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
//                   .distributedByContains(
//                       USER_TASK_2,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(SET_DURATIONS)
//                           .get(aggType),
//                       USER_TASK_2_NAME)
//                   .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//                   .distributedByContains(
//                       USER_TASK_1,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(SET_DURATIONS)
//                           .get(aggType),
//                       USER_TASK_1_NAME)
//                   .distributedByContains(USER_TASK_2, null, USER_TASK_2_NAME)
//                   .add()
//                   .add();
//               // @formatter:on
//             });
//     hyperMapAsserter.doAssert(result);
//   }
//
//   @Test
//   public void testCustomOrderOnResultLabelIsApplied() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, USER_TASK_1, SET_DURATIONS[0]);
//     changeDuration(processInstanceDto1, USER_TASK_2, SET_DURATIONS[0]);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto2);
//     changeDuration(processInstanceDto2, USER_TASK_1, SET_DURATIONS[1]);
//     changeDuration(processInstanceDto2, USER_TASK_2, SET_DURATIONS[1]);
//
//     importAllEngineEntitiesFromScratch();
//
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_LABEL,
// SortOrder.DESC));
//     reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());
//
//     // when
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getMeasures())
//         .extracting(MeasureResponseDto::getAggregationType)
//         .containsExactly(getSupportedAggregationTypes());
//     final HyperMapAsserter hyperMapAsserter =
//
// HyperMapAsserter.asserter().processInstanceCount(2L).processInstanceCountWithoutFilters(2L);
//     result
//         .getMeasures()
//         .forEach(
//             measureResult -> {
//               final AggregationDto aggType = measureResult.getAggregationType();
//               // @formatter:off
//               hyperMapAsserter
//                   .measure(ViewProperty.DURATION, aggType, getUserTaskDurationTime())
//                   .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//                   .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
//                   .distributedByContains(
//                       USER_TASK_2,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(SET_DURATIONS)
//                           .get(aggType),
//                       USER_TASK_2_NAME)
//                   .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//                   .distributedByContains(
//                       USER_TASK_1,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(SET_DURATIONS)
//                           .get(aggType),
//                       USER_TASK_1_NAME)
//                   .distributedByContains(USER_TASK_2, null, USER_TASK_2_NAME)
//                   .add()
//                   .add();
//               // @formatter:on
//             });
//     hyperMapAsserter.doAssert(result);
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
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, 20L);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto2);
//     changeDuration(processInstanceDto2, 40L);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(latestDefinition.getKey(), ReportConstants.ALL_VERSIONS);
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(
//             USER_TASK_1,
//             calculateExpectedValueGivenDurationsDefaultAggr(20., 40.),
//             USER_TASK_1_NAME)
//         .distributedByContains(USER_TASK_2, null, USER_TASK_2_NAME)
//         .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//         .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
//         .distributedByContains(
//             USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(40.), USER_TASK_2_NAME)
//         .doAssert(actualResult);
//     // @formatter:on
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
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, 20L);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto2);
//     changeDuration(processInstanceDto2, 40L);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(
//             latestDefinition.getKey(),
//             ImmutableList.of(
//                 firstDefinition.getVersionAsString(), latestDefinition.getVersionAsString()));
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(
//             USER_TASK_1,
//             calculateExpectedValueGivenDurationsDefaultAggr(20., 40.),
//             USER_TASK_1_NAME)
//         .distributedByContains(USER_TASK_2, null, USER_TASK_2_NAME)
//         .groupByContains(SECOND_USER, SECOND_USER_FULL_NAME)
//         .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
//         .distributedByContains(
//             USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(40.), USER_TASK_2_NAME)
//         .doAssert(actualResult);
//     // @formatter:on
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
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, 20L);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto2);
//     changeDuration(processInstanceDto2, 40L);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(latestDefinition.getKey(), ReportConstants.ALL_VERSIONS);
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(
//             USER_TASK_1,
//             calculateExpectedValueGivenDurationsDefaultAggr(20., 40.),
//             USER_TASK_1_NAME)
//         .doAssert(actualResult);
//     // @formatter:on
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
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     changeDuration(processInstanceDto1, 20L);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto2);
//     changeDuration(processInstanceDto2, 40L);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         createReport(
//             latestDefinition.getKey(),
//             ImmutableList.of(
//                 firstDefinition.getVersionAsString(), latestDefinition.getVersionAsString()));
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(
//             USER_TASK_1,
//             calculateExpectedValueGivenDurationsDefaultAggr(20., 40.),
//             USER_TASK_1_NAME)
//         .doAssert(actualResult);
//     // @formatter:on
//   }
//
//   @Test
//   public void otherProcessDefinitionsDoNotInfluenceResult() {
//     // given
//     // set current time to now for easier evaluation of duration of unassigned tasks
//     OffsetDateTime now = OffsetDateTime.now();
//     LocalDateUtil.setCurrentTime(now);
//
//     final ProcessDefinitionEngineDto processDefinition1 = deployOneUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto1 =
//         engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto1);
//     final Double[] setDurations1 = new Double[] {40., 20.};
//     changeDuration(processInstanceDto1, setDurations1[0]);
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto2);
//     changeDuration(processInstanceDto2, setDurations1[1]);
//
//     final ProcessDefinitionEngineDto processDefinition2 = deployOneUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto3 =
//         engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto3);
//     final Double[] setDurations2 = new Double[] {60., 80.};
//     changeDuration(processInstanceDto3, setDurations2[0]);
//     final ProcessInstanceEngineDto processInstanceDto4 =
//         engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
//     changeUserTaskStartDate(processInstanceDto4, now, USER_TASK_1, UNASSIGNED_TASK_DURATION);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData1 = createReport(processDefinition1);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult1 =
//         reportClient.evaluateHyperMapReport(reportData1).getResult();
//     final ProcessReportDataDto reportData2 = createReport(processDefinition2);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult2 =
//         reportClient.evaluateHyperMapReport(reportData2).getResult();
//
//     // then
//     assertHyperMap_otherProcessDefinitionsDoNotInfluenceResult(
//         setDurations1, setDurations2, actualResult1, actualResult2);
//   }
//
//   protected void assertHyperMap_otherProcessDefinitionsDoNotInfluenceResult(
//       final Double[] setDurations1,
//       final Double[] setDurations2,
//       final ReportResultResponseDto<List<HyperMapResultEntryDto>> result1,
//       final ReportResultResponseDto<List<HyperMapResultEntryDto>> result2) {
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(
//             USER_TASK_1,
//             calculateExpectedValueGivenDurationsDefaultAggr(setDurations1),
//             USER_TASK_1_NAME)
//         .doAssert(result1);
//     HyperMapAsserter.asserter()
//         .processInstanceCount(2L)
//         .processInstanceCountWithoutFilters(2L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(
//             USER_TASK_1,
//             calculateExpectedValueGivenDurationsDefaultAggr(setDurations2[0]),
//             USER_TASK_1_NAME)
//         .groupByContains(DISTRIBUTE_BY_IDENTITY_MISSING_KEY, getLocalizedUnassignedLabel())
//         .distributedByContains(USER_TASK_1, UNASSIGNED_TASK_DURATION, USER_TASK_1_NAME)
//         .doAssert(result2);
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
//     ProcessReportDataDto reportData = createReport(processKey, ReportConstants.ALL_VERSIONS);
//     reportData.setTenantIds(selectedTenants);
//     ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(selectedTenants.size());
//   }
//
//   @Test
//   public void evaluateReportWithIrrationalNumberAsResult() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
//     Double[] setDurations = new Double[] {100., 300., 600.};
//     ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto);
//     changeDuration(processInstanceDto, setDurations[0]);
//     processInstanceDto =
// engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto);
//     changeDuration(processInstanceDto, setDurations[1]);
//     processInstanceDto =
// engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     finishUserTaskRoundsOneWithDefaultAndSecondUser(processInstanceDto);
//     changeDuration(processInstanceDto, setDurations[2]);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());
//
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getMeasures())
//         .extracting(MeasureResponseDto::getAggregationType)
//         .containsExactly(getSupportedAggregationTypes());
//     final HyperMapAsserter hyperMapAsserter =
//
// HyperMapAsserter.asserter().processInstanceCount(3L).processInstanceCountWithoutFilters(3L);
//     result
//         .getMeasures()
//         .forEach(
//             measureResult -> {
//               final AggregationDto aggType = measureResult.getAggregationType();
//               // @formatter:off
//               hyperMapAsserter
//                   .measure(ViewProperty.DURATION, aggType, getUserTaskDurationTime())
//                   .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//                   .distributedByContains(
//                       USER_TASK_1,
//                       databaseIntegrationTestExtension
//                           .calculateExpectedValueGivenDurations(setDurations)
//                           .get(aggType),
//                       USER_TASK_1_NAME)
//                   .add()
//                   .add();
//               // @formatter:on
//             });
//     hyperMapAsserter.doAssert(result);
//   }
//
//   @Test
//   public void noUserTaskMatchesReturnsEmptyResult() {
//     // when
//     final ProcessReportDataDto reportData = createReport("nonExistingProcessDefinitionId", "1");
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getFirstMeasureData()).isEmpty();
//   }
//
//   @Data
//   static class FlowNodeStatusTestValues {
//     List<ProcessFilterDto<?>> processFilter;
//     HyperMapResultEntryDto expectedIdleDurationValues;
//     HyperMapResultEntryDto expectedWorkDurationValues;
//     HyperMapResultEntryDto expectedTotalDurationValues;
//   }
//
//   private static HyperMapResultEntryDto getExpectedResultsMap(
//       Double userTask1Result, Double userTask2Result) {
//     List<MapResultEntryDto> groupByResults = new ArrayList<>();
//     MapResultEntryDto firstUserTask =
//         new MapResultEntryDto(USER_TASK_1, userTask1Result, USER_TASK_1_NAME);
//     groupByResults.add(firstUserTask);
//     MapResultEntryDto secondUserTask =
//         new MapResultEntryDto(USER_TASK_2, userTask2Result, USER_TASK_2_NAME);
//     groupByResults.add(secondUserTask);
//     return new HyperMapResultEntryDto(DEFAULT_USERNAME, groupByResults, DEFAULT_FULLNAME);
//   }
//
//   protected static Stream<FlowNodeStatusTestValues> getFlowNodeStatusExpectedValues() {
//     FlowNodeStatusTestValues runningStateValues = new FlowNodeStatusTestValues();
//     runningStateValues.processFilter =
//         ProcessFilterBuilder.filter().runningFlowNodesOnly().add().buildList();
//     runningStateValues.expectedIdleDurationValues = getExpectedResultsMap(200., 200.);
//     runningStateValues.expectedWorkDurationValues = getExpectedResultsMap(500., 500.);
//     runningStateValues.expectedTotalDurationValues = getExpectedResultsMap(700., 700.);
//
//     FlowNodeStatusTestValues completedStateValues = new FlowNodeStatusTestValues();
//     completedStateValues.processFilter =
//         ProcessFilterBuilder.filter().completedFlowNodesOnly().add().buildList();
//     completedStateValues.expectedIdleDurationValues = getExpectedResultsMap(100., null);
//     completedStateValues.expectedWorkDurationValues = getExpectedResultsMap(100., null);
//     completedStateValues.expectedTotalDurationValues = getExpectedResultsMap(100., null);
//
//     FlowNodeStatusTestValues completedOrCanceled = new FlowNodeStatusTestValues();
//     completedOrCanceled.processFilter =
//         ProcessFilterBuilder.filter().completedOrCanceledFlowNodesOnly().add().buildList();
//     completedOrCanceled.expectedIdleDurationValues = getExpectedResultsMap(100., null);
//     completedOrCanceled.expectedWorkDurationValues = getExpectedResultsMap(100., null);
//     completedOrCanceled.expectedTotalDurationValues = getExpectedResultsMap(100., null);
//
//     return Stream.of(runningStateValues, completedStateValues, completedOrCanceled);
//   }
//
//   @ParameterizedTest
//   @MethodSource("getFlowNodeStatusExpectedValues")
//   public void evaluateReportWithFlowNodeStatusFilter(
//       FlowNodeStatusTestValues flowNodeStatusTestValues) {
//     // given
//     OffsetDateTime now = OffsetDateTime.now();
//     LocalDateUtil.setCurrentTime(now);
//
//     final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     // finish first running task, second now runs but unclaimed
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
//     changeDuration(processInstanceDto, USER_TASK_1, 100L);
//     engineIntegrationExtension.claimAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
//     changeUserTaskStartDate(processInstanceDto, now, USER_TASK_2, 700.);
//     changeUserTaskClaimDate(processInstanceDto, now, USER_TASK_2, 500.);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     // claim first running task
//     engineIntegrationExtension.claimAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto2.getId());
//     changeUserTaskStartDate(processInstanceDto2, now, USER_TASK_1, 700.);
//     changeUserTaskClaimDate(processInstanceDto2, now, USER_TASK_1, 500.);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     reportData.setFilter(flowNodeStatusTestValues.processFilter);
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(actualResult.getFirstMeasureData()).hasSize(1);
//     assertEvaluateReportWithFlowNodeStatusFilter(actualResult, flowNodeStatusTestValues);
//   }
//
//   protected abstract void assertEvaluateReportWithFlowNodeStatusFilter(
//       ReportResultResponseDto<List<HyperMapResultEntryDto>> result,
//       FlowNodeStatusTestValues expectedValues);
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
//     changeDuration(processInstanceDto, 10L);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(USER_TASK_1, 10.)
//         .doAssert(actualResult);
//     // @formatter:on
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
//       changeDuration(processInstanceDto, i);
//     }
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData = createReport(processDefinition);
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(11L)
//         .processInstanceCountWithoutFilters(11L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(USER_TASK_1, 5., USER_TASK_1_NAME)
//         .doAssert(actualResult);
//     // @formatter:on
//   }
//
//   @Test
//   public void filterInReport() {
//     // given
//     final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
//     final ProcessInstanceEngineDto processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
//     changeDuration(processInstanceDto, 10L);
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
//     final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
//         evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
//
//     // then
//     ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
//         evaluationResponse.getResult();
//     assertThat(actualResult.getFirstMeasureData()).isNotNull();
//     assertThat(actualResult.getFirstMeasureData()).isEmpty();
//
//     // when
//     reportData = createReport(processDefinition);
//     reportData.setFilter(createStartDateFilter(processStartTime, null));
//     actualResult = reportClient.evaluateHyperMapReport(reportData).getResult();
//
//     // then
//     assertThat(actualResult.getFirstMeasureData()).isNotNull();
//     assertThat(actualResult.getFirstMeasureData()).hasSize(1);
//     // @formatter:off
//     HyperMapAsserter.asserter()
//         .processInstanceCount(1L)
//         .processInstanceCountWithoutFilters(1L)
//         .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
//         .groupByContains(DEFAULT_USERNAME, DEFAULT_FULLNAME)
//         .distributedByContains(USER_TASK_1, 10., USER_TASK_1_NAME)
//         .doAssert(actualResult);
//     // @formatter:on
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
//   protected abstract UserTaskDurationTime getUserTaskDurationTime();
//
//   protected abstract void changeDuration(
//       final ProcessInstanceEngineDto processInstanceDto,
//       final String userTaskKey,
//       final double durationInMs);
//
//   protected abstract void changeDuration(
//       final ProcessInstanceEngineDto processInstanceDto, final double durationInMs);
//
//   protected abstract ProcessReportDataDto createReport(
//       final String processDefinitionKey, final List<String> versions);
//
//   private ProcessReportDataDto createReport(
//       final String processDefinitionKey, final String version) {
//     return createReport(processDefinitionKey, newArrayList(version));
//   }
//
//   private ProcessReportDataDto createReport(final ProcessDefinitionEngineDto processDefinition) {
//     return createReport(processDefinition.getKey(),
// String.valueOf(processDefinition.getVersion()));
//   }
//
//   private void finishUserTaskRoundsOneWithDefaultAndSecondUser(
//       final ProcessInstanceEngineDto processInstanceDto1) {
//     // finish first task
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto1.getId());
//     // finish second task with
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto1.getId());
//   }
//
//   private void finishCurrentUserTaskWithDefaultUser(
//       final ProcessInstanceEngineDto processInstanceDto) {
//     // finish first task
//     engineIntegrationExtension.finishAllRunningUserTasks(
//         DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
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
//     BpmnModelInstance modelInstance =
//         Bpmn.createExecutableProcess(key)
//             .startEvent(START_EVENT)
//             .userTask(USER_TASK_1)
//             .name(USER_TASK_1_NAME)
//             .endEvent(END_EVENT)
//             .done();
//     return engineIntegrationExtension.deployProcessAndGetProcessDefinition(modelInstance,
// tenantId);
//   }
//
//   private ProcessDefinitionEngineDto deployTwoUserTasksDefinition() {
//     BpmnModelInstance modelInstance =
//         Bpmn.createExecutableProcess("aProcess")
//             .startEvent(START_EVENT)
//             .userTask(USER_TASK_1)
//             .name(USER_TASK_1_NAME)
//             .userTask(USER_TASK_2)
//             .name(USER_TASK_2_NAME)
//             .endEvent(END_EVENT)
//             .done();
//     return engineIntegrationExtension.deployProcessAndGetProcessDefinition(modelInstance);
//   }
//
//   private ProcessDefinitionEngineDto deployFourUserTasksDefinition() {
//     BpmnModelInstance modelInstance =
//         Bpmn.createExecutableProcess("aProcess")
//             .startEvent()
//             .parallelGateway()
//             .userTask(USER_TASK_1)
//             .userTask(USER_TASK_2)
//             .endEvent()
//             .moveToLastGateway()
//             .userTask(USER_TASK_A)
//             .userTask(USER_TASK_B)
//             .endEvent()
//             .done();
//     return engineIntegrationExtension.deployProcessAndGetProcessDefinition(modelInstance);
//   }
//
//   private String getLocalizedUnassignedLabel() {
//     return embeddedOptimizeExtension
//         .getLocalizationService()
//         .getDefaultLocaleMessageForMissingAssigneeLabel();
//   }
// }
