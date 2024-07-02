/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.usertask.frequency.groupby.date.distributedby.usertask;

import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.report.util.HyperMapAsserter;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.test.util.DateCreationFreezer;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UserTaskFrequencyByUserTaskStartDateByUserTaskReportEvaluationIT
    extends UserTaskFrequencyByUserTaskDateByUserTaskReportEvaluationIT {

  @ParameterizedTest
  @MethodSource("getFlowNodeStatusExpectedValues")
  public void evaluateReportWithFlowNodeStatusFilter(
      FlowNodeStatusTestValues flowNodeStatusTestValues) {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());

    engineIntegrationExtension.startProcessInstance(processDefinition.getId());

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        createReportData(processDefinition, AggregateByDateUnit.DAY);
    reportData.setFilter(flowNodeStatusTestValues.processFilter);
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(flowNodeStatusTestValues.expectedInstanceCount)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(groupedByDayDateAsString(OffsetDateTime.now()))
        .distributedByContains(
            USER_TASK_1, flowNodeStatusTestValues.expectedUserTask1Count, USER_TASK_1_NAME)
        .distributedByContains(
            USER_TASK_2, flowNodeStatusTestValues.expectedUserTask2Count, USER_TASK_2_NAME)
        .doAssert(result);
    // @formatter:on
  }

  @Test
  public void evaluateReportWithFlowNodeStatusFilterCanceled() {
    // given
    final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto firstInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(firstInstance.getId());
    engineIntegrationExtension.cancelActivityInstance(firstInstance.getId(), USER_TASK_2);
    engineDatabaseExtension.changeFlowNodeStartDate(
        firstInstance.getId(), USER_TASK_2, now.minus(100, ChronoUnit.MILLIS));

    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.cancelActivityInstance(secondInstance.getId(), USER_TASK_1);
    engineDatabaseExtension.changeFlowNodeStartDate(
        secondInstance.getId(), USER_TASK_1, now.minus(100, ChronoUnit.MILLIS));

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        createReportData(processDefinition, AggregateByDateUnit.DAY);
    reportData.setFilter(ProcessFilterBuilder.filter().canceledFlowNodesOnly().add().buildList());
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(groupedByDayDateAsString(OffsetDateTime.now()))
        .distributedByContains(USER_TASK_1, 1., USER_TASK_1_NAME)
        .distributedByContains(USER_TASK_2, 1., USER_TASK_2_NAME)
        .doAssert(result);
    // @formatter:on
  }

  @Data
  @AllArgsConstructor
  static class FlowNodeStatusTestValues {
    List<ProcessFilterDto<?>> processFilter;
    Double expectedUserTask1Count;
    Double expectedUserTask2Count;
    Long expectedInstanceCount;
  }

  protected static Stream<FlowNodeStatusTestValues> getFlowNodeStatusExpectedValues() {
    return Stream.of(
        new FlowNodeStatusTestValues(
            ProcessFilterBuilder.filter().runningFlowNodesOnly().add().buildList(), 1., null, 1L),
        new FlowNodeStatusTestValues(
            ProcessFilterBuilder.filter().completedFlowNodesOnly().add().buildList(), 1., 1., 1L),
        new FlowNodeStatusTestValues(
            ProcessFilterBuilder.filter().completedOrCanceledFlowNodesOnly().add().buildList(),
            1.,
            1.,
            1L));
  }

  @Override
  protected ProcessReportDataType getReportDataType() {
    return ProcessReportDataType.USER_TASK_FREQ_GROUP_BY_USER_TASK_START_DATE_BY_USER_TASK;
  }

  @Override
  protected ProcessGroupByType getGroupByType() {
    return ProcessGroupByType.START_DATE;
  }

  @Override
  protected void changeModelElementDates(final Map<String, OffsetDateTime> updates) {
    engineDatabaseExtension.changeAllFlowNodeStartDates(updates);
  }

  @Override
  protected void changeModelElementDate(
      final ProcessInstanceEngineDto processInstance,
      final String userTaskKey,
      final OffsetDateTime dateToChangeTo) {
    engineDatabaseExtension.changeFlowNodeStartDate(
        processInstance.getId(), userTaskKey, dateToChangeTo);
  }
}
