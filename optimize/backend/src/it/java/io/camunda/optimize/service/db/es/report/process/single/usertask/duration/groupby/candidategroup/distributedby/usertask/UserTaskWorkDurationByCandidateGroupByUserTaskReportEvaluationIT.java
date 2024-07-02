/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.usertask.duration.groupby.candidategroup.distributedby.usertask;

import static io.camunda.optimize.service.util.ProcessReportDataType.USER_TASK_DUR_GROUP_BY_CANDIDATE_BY_USER_TASK;
import static io.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurationsDefaultAggr;
import static io.camunda.optimize.test.util.DurationAggregationUtil.getSupportedAggregationTypes;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.report.util.HyperMapAsserter;
import io.camunda.optimize.service.db.es.report.util.MapResultUtil;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import java.util.Arrays;
import java.util.List;

public class UserTaskWorkDurationByCandidateGroupByUserTaskReportEvaluationIT
    extends AbstractUserTaskDurationByCandidateGroupByUserTaskReportEvaluationIT {

  @Override
  protected UserTaskDurationTime getUserTaskDurationTime() {
    return UserTaskDurationTime.WORK;
  }

  @Override
  protected void changeDuration(
      final ProcessInstanceEngineDto processInstanceDto, final Double durationInMs) {
    changeUserTaskWorkDuration(processInstanceDto, durationInMs);
  }

  @Override
  protected void changeDuration(
      final ProcessInstanceEngineDto processInstanceDto,
      final String userTaskKey,
      final Double durationInMs) {
    changeUserTaskWorkDuration(processInstanceDto, userTaskKey, durationInMs);
  }

  @Override
  protected ProcessReportDataDto createReport(
      final String processDefinitionKey, final List<String> versions) {
    return TemplatedProcessReportDataBuilder.createReportData()
        .setProcessDefinitionKey(processDefinitionKey)
        .setProcessDefinitionVersions(versions)
        .setUserTaskDurationTime(UserTaskDurationTime.WORK)
        .setReportDataType(USER_TASK_DUR_GROUP_BY_CANDIDATE_BY_USER_TASK)
        .build();
  }

  @Override
  protected void assertEvaluateReportWithFlowNodeStatusFilters(
      final ReportResultResponseDto<List<HyperMapResultEntryDto>> result,
      final FlowNodeStatusTestValues expectedValues) {
    assertThat(
            MapResultUtil.getDataEntryForKey(
                result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .isEqualTo(expectedValues.getExpectedWorkDurationValues());
  }

  @Override
  protected void assertHyperMap_ForOneProcessInstanceWithUnassignedTasks(
      final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult) {
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(1L)
        .processInstanceCountWithoutFilters(1L)
        .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
        .groupByContains(FIRST_CANDIDATE_GROUP_ID, FIRST_CANDIDATE_GROUP_NAME)
        .distributedByContains(
            USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[1]))
        .distributedByContains(USER_TASK_2, null)
        .distributedByContains(
            USER_TASK_A, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[1]))
        .distributedByContains(USER_TASK_B, null)
        .doAssert(actualResult);
    // @formatter:on
  }

  @Override
  protected void assertHyperMap_ForSeveralProcessInstances(
      final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult) {
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
        .groupByContains(FIRST_CANDIDATE_GROUP_ID, FIRST_CANDIDATE_GROUP_NAME)
        .distributedByContains(
            USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS))
        .distributedByContains(USER_TASK_2, null)
        .distributedByContains(
            USER_TASK_A, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS))
        .distributedByContains(USER_TASK_B, null)
        .groupByContains(SECOND_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_NAME)
        .distributedByContains(USER_TASK_1, null)
        .distributedByContains(
            USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[0]))
        .distributedByContains(USER_TASK_A, null)
        .distributedByContains(
            USER_TASK_B, calculateExpectedValueGivenDurationsDefaultAggr(SET_DURATIONS[0]))
        .doAssert(actualResult);
    // @formatter:on
  }

  @Override
  protected void assertHyperMap_ForSeveralProcessInstancesWithAllAggregationTypes(
      final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult) {
    assertThat(actualResult.getMeasures())
        .extracting(MeasureResponseDto::getAggregationType)
        .containsExactly(getSupportedAggregationTypes());
    final HyperMapAsserter hyperMapAsserter =
        HyperMapAsserter.asserter().processInstanceCount(2L).processInstanceCountWithoutFilters(2L);
    Arrays.stream(getSupportedAggregationTypes())
        .forEach(
            aggType -> {
              // @formatter:off
              hyperMapAsserter
                  .measure(ViewProperty.DURATION, aggType, getUserTaskDurationTime())
                  .groupByContains(FIRST_CANDIDATE_GROUP_ID, FIRST_CANDIDATE_GROUP_NAME)
                  .distributedByContains(
                      USER_TASK_1,
                      databaseIntegrationTestExtension
                          .calculateExpectedValueGivenDurations(SET_DURATIONS)
                          .get(aggType))
                  .distributedByContains(USER_TASK_2, null)
                  .distributedByContains(
                      USER_TASK_A,
                      databaseIntegrationTestExtension
                          .calculateExpectedValueGivenDurations(SET_DURATIONS)
                          .get(aggType))
                  .distributedByContains(USER_TASK_B, null)
                  .groupByContains(SECOND_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_NAME)
                  .distributedByContains(USER_TASK_1, null)
                  .distributedByContains(
                      USER_TASK_2,
                      databaseIntegrationTestExtension
                          .calculateExpectedValueGivenDurations(SET_DURATIONS[0])
                          .get(aggType))
                  .distributedByContains(USER_TASK_A, null)
                  .distributedByContains(
                      USER_TASK_B,
                      databaseIntegrationTestExtension
                          .calculateExpectedValueGivenDurations(SET_DURATIONS[0])
                          .get(aggType))
                  .add()
                  .add();
              // @formatter:on
            });
    hyperMapAsserter.doAssert(actualResult);
  }

  @Override
  protected void assertHyperMap_ForMultipleEvents(
      final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult) {
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
        .groupByContains(FIRST_CANDIDATE_GROUP_ID, FIRST_CANDIDATE_GROUP_NAME)
        .distributedByContains(USER_TASK_1, SET_DURATIONS[0], USER_TASK_1_NAME)
        .distributedByContains(USER_TASK_2, null, USER_TASK_2_NAME)
        .groupByContains(SECOND_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_NAME)
        .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
        .distributedByContains(USER_TASK_2, SET_DURATIONS[1], USER_TASK_2_NAME)
        .doAssert(actualResult);
    // @formatter:on
  }

  @Override
  protected void assertHyperMap_ForMultipleEventsWithAllAggregationTypes(
      final ReportResultResponseDto<List<HyperMapResultEntryDto>> result) {
    // @formatter:off
    assertThat(result.getMeasures())
        .extracting(MeasureResponseDto::getAggregationType)
        .containsExactly(getSupportedAggregationTypes());
    final HyperMapAsserter hyperMapAsserter =
        HyperMapAsserter.asserter().processInstanceCount(2L).processInstanceCountWithoutFilters(2L);
    Arrays.stream(getSupportedAggregationTypes())
        .forEach(
            aggType -> {
              // @formatter:off
              hyperMapAsserter
                  .measure(ViewProperty.DURATION, aggType, getUserTaskDurationTime())
                  .groupByContains(FIRST_CANDIDATE_GROUP_ID, FIRST_CANDIDATE_GROUP_NAME)
                  .distributedByContains(
                      USER_TASK_1,
                      databaseIntegrationTestExtension
                          .calculateExpectedValueGivenDurations(SET_DURATIONS)
                          .get(aggType),
                      USER_TASK_1_NAME)
                  .distributedByContains(USER_TASK_2, null, USER_TASK_2_NAME)
                  .groupByContains(SECOND_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_NAME)
                  .distributedByContains(USER_TASK_1, null, USER_TASK_1_NAME)
                  .distributedByContains(
                      USER_TASK_2,
                      databaseIntegrationTestExtension
                          .calculateExpectedValueGivenDurations(SET_DURATIONS[0])
                          .get(aggType),
                      USER_TASK_2_NAME)
                  .add()
                  .add();
              // @formatter:on
            });
    hyperMapAsserter.doAssert(result);
  }

  @Override
  protected void assertHyperMap_otherProcessDefinitionsDoNotInfluenceResult(
      final Double[] setDurations1,
      final Double[] setDurations2,
      final ReportResultResponseDto<List<HyperMapResultEntryDto>> result1,
      final ReportResultResponseDto<List<HyperMapResultEntryDto>> result2) {
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
        .groupByContains(FIRST_CANDIDATE_GROUP_ID, FIRST_CANDIDATE_GROUP_NAME)
        .distributedByContains(
            USER_TASK_1,
            calculateExpectedValueGivenDurationsDefaultAggr(setDurations1),
            USER_TASK_1_NAME)
        .doAssert(result1);

    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.DURATION, AggregationType.AVERAGE, getUserTaskDurationTime())
        .groupByContains(FIRST_CANDIDATE_GROUP_ID, FIRST_CANDIDATE_GROUP_NAME)
        .distributedByContains(
            USER_TASK_1,
            calculateExpectedValueGivenDurationsDefaultAggr(setDurations2[0]),
            USER_TASK_1_NAME)
        .doAssert(result2);
    // @formatter:on
  }
}
