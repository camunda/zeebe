/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.usertask.duration.groupby.usertask.distributedby.none;

import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.AVERAGE;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.IN;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.NOT_IN;
import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_KEY;
import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_LABEL;
import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_VALUE;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurationsDefaultAggr;
import static io.camunda.optimize.test.util.DurationAggregationUtil.getSupportedAggregationTypes;
import static io.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.ReportConstants;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto;
import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.report.process.AbstractProcessDefinitionIT;
import io.camunda.optimize.service.db.es.report.util.MapResultAsserter;
import io.camunda.optimize.service.db.es.report.util.MapResultUtil;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.util.BpmnModels;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import org.assertj.core.groups.Tuple;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractUserTaskDurationByUserTaskReportEvaluationIT
    extends AbstractProcessDefinitionIT {

  private static final String PROCESS_DEFINITION_KEY = "123";
  protected static final String USER_TASK_1 = "userTask1";
  protected static final String USER_TASK_2 = "userTask2";

  @Test
  public void reportEvaluationForOneProcessInstance() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto);

    final Double setDuration = 20.;
    changeDuration(processInstanceDto, setDuration);
    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>> evaluationResponse =
        reportClient.evaluateMapReport(reportData);

    // then
    final ProcessReportDataDto resultReportDataDto =
        evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(resultReportDataDto.getDefinitionVersions())
        .contains(processDefinition.getVersionAsString());
    assertThat(resultReportDataDto.getView()).isNotNull();
    assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
    assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
    assertThat(resultReportDataDto.getConfiguration().getUserTaskDurationTimes())
        .containsExactly(getUserTaskDurationTime());

    final ReportResultResponseDto<List<MapResultEntryDto>> result = evaluationResponse.getResult();
    assertThat(result.getFirstMeasureData()).isNotNull();
    assertThat(result.getFirstMeasureData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(2L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(setDuration));
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(setDuration));
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  public void reportEvaluationForSeveralProcessInstances() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    final Double[] setDurations = new Double[] {10., 30.};
    changeDuration(processInstanceDto1, setDurations[0]);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, setDurations[1]);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(2L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(setDurations));
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(setDurations));
    assertThat(result.getInstanceCount()).isEqualTo(2L);
  }

  @Test
  public void reportEvaluationForSeveralProcessInstancesWithAllAggregationTypes() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    final Double[] setDurations = new Double[] {10., 30.};
    changeDuration(processInstanceDto1, setDurations[0]);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, setDurations[1]);

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());

    // when
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo(2L);
    assertDurationMapReportResults(
        result, ImmutableMap.of(USER_TASK_1, setDurations, USER_TASK_2, setDurations));
  }

  @Test
  public void reportEvaluationForSeveralProcessDefinitions() {
    // given
    final String key1 = "key1";
    final String key2 = "key2";
    final ProcessDefinitionEngineDto processDefinition1 =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            BpmnModels.getSingleUserTaskDiagram(key1, USER_TASK_1));
    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto1.getId());
    final ProcessDefinitionEngineDto processDefinition2 =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            BpmnModels.getSingleUserTaskDiagram(key2, USER_TASK_2));
    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto2.getId());

    final Double expectedDuration = 20.;
    changeDuration(processInstanceDto1, expectedDuration);
    changeDuration(processInstanceDto2, expectedDuration);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition1);
    reportData.getDefinitions().add(createReportDataDefinitionDto(key2));
    final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>> evaluationResponse =
        reportClient.evaluateMapReport(reportData);

    // then
    final ReportResultResponseDto<List<MapResultEntryDto>> result = evaluationResponse.getResult();
    // @formatter:off
    MapResultAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.DURATION, new AggregationDto(AVERAGE), getUserTaskDurationTime())
        .groupedByContains(USER_TASK_1, expectedDuration)
        .groupedByContains(USER_TASK_2, expectedDuration)
        .doAssert(result);
    // @formatter:on
  }

  @Test
  public void evaluateReportForMultipleEvents() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, USER_TASK_1, 10.);
    changeDuration(processInstanceDto1, USER_TASK_2, 20.);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, USER_TASK_1, 10.);
    changeDuration(processInstanceDto2, USER_TASK_2, 20.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(2L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(10.));
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(20.));
  }

  @Test
  public void evaluateReportForMultipleEventsWithAllAggregationTypes() {
    // given
    final double duration1 = 10.;
    final double duration2 = 20.;
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, USER_TASK_1, duration1);
    changeDuration(processInstanceDto1, USER_TASK_2, duration2);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, USER_TASK_1, duration1);
    changeDuration(processInstanceDto2, USER_TASK_2, duration2);

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());

    // when
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertDurationMapReportResults(
        result,
        ImmutableMap.of(
            USER_TASK_1,
            new Double[] {duration1, duration1},
            USER_TASK_2,
            new Double[] {duration2, duration2}));
  }

  @Test
  public void testCustomOrderOnResultKeyIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, USER_TASK_1, 10.);
    changeDuration(processInstanceDto1, USER_TASK_2, 20.);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, USER_TASK_1, 10.);
    changeDuration(processInstanceDto2, USER_TASK_2, 20.);

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_KEY, SortOrder.DESC));

    // when
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getMeasures())
        .extracting(MeasureResponseDto::getAggregationType)
        .containsExactly(getSupportedAggregationTypes());
    result
        .getMeasures()
        .forEach(
            measureResult -> {
              assertThat(measureResult.getData())
                  .hasSize(2)
                  .extracting(MapResultEntryDto::getKey)
                  .isSortedAccordingTo(Comparator.reverseOrder());
              assertThat(getExecutedFlowNodeCount(measureResult.getData())).isEqualTo(2L);
            });
  }

  @Test
  public void testCustomOrderOnResultLabelIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, USER_TASK_1, 10.);
    changeDuration(processInstanceDto1, USER_TASK_2, 20.);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, USER_TASK_1, 10.);
    changeDuration(processInstanceDto2, USER_TASK_2, 20.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_LABEL, SortOrder.DESC));
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
    assertThat(resultData).hasSize(2);
    assertThat(getExecutedFlowNodeCount(resultData)).isEqualTo(2L);
    final List<String> resultLabels =
        resultData.stream().map(MapResultEntryDto::getLabel).collect(Collectors.toList());
    assertThat(resultLabels).isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  public void testCustomOrderOnResultValueIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, USER_TASK_1, 10.);
    changeDuration(processInstanceDto1, USER_TASK_2, 20.);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, USER_TASK_1, 100.);
    changeDuration(processInstanceDto2, USER_TASK_2, 2.);

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_VALUE, SortOrder.ASC));

    // when
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getMeasures())
        .extracting(MeasureResponseDto::getAggregationType)
        .containsExactly(getSupportedAggregationTypes());
    result
        .getMeasures()
        .forEach(
            measureResult -> {
              assertThat(measureResult.getData()).hasSize(2);
              assertCorrectValueOrdering(measureResult.getData());
              assertThat(getExecutedFlowNodeCount(measureResult.getData())).isEqualTo(2L);
            });
  }

  @Test
  public void allVersionsRespectLatestNodesOnlyWhereLatestHasMoreNodes() {
    // given
    final ProcessDefinitionEngineDto firstDefinition = deployOneUserTasksDefinition();
    final ProcessDefinitionEngineDto latestDefinition = deployTwoUserTasksDefinition();
    assertThat(latestDefinition.getVersion()).isEqualTo(2);

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, 20.);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, 40.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        createReport(latestDefinition.getKey(), ReportConstants.ALL_VERSIONS);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(2L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(20., 40.));
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(40.));
  }

  @Test
  public void multipleVersionsRespectLatestNodesOnlyWhereLatestHasMoreNodes() {
    // given
    final ProcessDefinitionEngineDto firstDefinition = deployOneUserTasksDefinition();
    deployOneUserTasksDefinition();
    final ProcessDefinitionEngineDto latestDefinition = deployTwoUserTasksDefinition();
    assertThat(latestDefinition.getVersion()).isEqualTo(3);

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, 20.);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, 40.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        createReport(
            latestDefinition.getKey(),
            ImmutableList.of(
                firstDefinition.getVersionAsString(), latestDefinition.getVersionAsString()));
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(2L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(20., 40.));
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_2)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(40.));
  }

  @Test
  public void allVersionsRespectLatestNodesOnlyWhereLatestHasFewerNodes() {
    // given
    final ProcessDefinitionEngineDto firstDefinition = deployTwoUserTasksDefinition();
    final ProcessDefinitionEngineDto latestDefinition = deployOneUserTasksDefinition();
    assertThat(latestDefinition.getVersion()).isEqualTo(2);

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, 20.);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, 40.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        createReport(latestDefinition.getKey(), ReportConstants.ALL_VERSIONS);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(1L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(20., 40.));
  }

  @Test
  public void multipleVersionsRespectLatestNodesOnlyWhereLatestHasFewerNodes() {
    // given
    final ProcessDefinitionEngineDto firstDefinition = deployTwoUserTasksDefinition();
    deployTwoUserTasksDefinition();
    final ProcessDefinitionEngineDto latestDefinition = deployOneUserTasksDefinition();
    assertThat(latestDefinition.getVersion()).isEqualTo(3);

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(firstDefinition.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, 20.);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(latestDefinition.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, 40.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        createReport(
            latestDefinition.getKey(),
            ImmutableList.of(
                firstDefinition.getVersionAsString(), latestDefinition.getVersionAsString()));
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(1L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(20., 40.));
  }

  @Test
  public void otherProcessDefinitionsDoNotInfluenceResult() {
    // given
    final ProcessDefinitionEngineDto processDefinition1 = deployOneUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
    finishAllUserTasks(processInstanceDto1);
    changeDuration(processInstanceDto1, 40.);
    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
    finishAllUserTasks(processInstanceDto2);
    changeDuration(processInstanceDto2, 40.);

    final ProcessDefinitionEngineDto processDefinition2 = deployOneUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto3 =
        engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
    finishAllUserTasks(processInstanceDto3);
    changeDuration(processInstanceDto3, 20.);
    final ProcessInstanceEngineDto processInstanceDto4 =
        engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
    finishAllUserTasks(processInstanceDto4);
    changeDuration(processInstanceDto4, 20.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData1 = createReport(processDefinition1);
    final ReportResultResponseDto<List<MapResultEntryDto>> result1 =
        reportClient.evaluateMapReport(reportData1).getResult();
    final ProcessReportDataDto reportData2 = createReport(processDefinition2);
    final ReportResultResponseDto<List<MapResultEntryDto>> result2 =
        reportClient.evaluateMapReport(reportData2).getResult();

    // then
    assertThat(result1.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result1.getFirstMeasureData())).isEqualTo(1L);
    assertThat(
            MapResultUtil.getEntryForKey(result1.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(40.));

    assertThat(result2.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result2.getFirstMeasureData())).isEqualTo(1L);
    assertThat(
            MapResultUtil.getEntryForKey(result2.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(20.));
  }

  @Test
  public void reportEvaluationSingleBucketFilteredBySingleTenant() {
    // given
    final String tenantId1 = "tenantId1";
    final String tenantId2 = "tenantId2";
    final List<String> selectedTenants = Collections.singletonList(tenantId1);
    final String processKey =
        deployAndStartMultiTenantUserTaskProcess(Arrays.asList(null, tenantId1, tenantId2));

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processKey, ReportConstants.ALL_VERSIONS);
    reportData.setTenantIds(selectedTenants);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo((long) selectedTenants.size());
  }

  @Test
  public void evaluateReportWithIrrationalNumberAsResult() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
    ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto);
    changeDuration(processInstanceDto, 100.);
    processInstanceDto = engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto);
    changeDuration(processInstanceDto, 300.);
    processInstanceDto = engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto);
    changeDuration(processInstanceDto, 600.);

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());

    // when
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertDurationMapReportResults(
        result, ImmutableMap.of(USER_TASK_1, new Double[] {100., 300., 600.}));
  }

  @Test
  public void noUserTaskMatchesReturnsEmptyResult() {
    // when
    final ProcessReportDataDto reportData = createReport("nonExistingProcessDefinitionId", "1");
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).isEmpty();
  }

  public static Stream<Arguments> viewLevelAssigneeFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_USER},
            1L,
            Collections.singletonList(
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(10.)))),
        Arguments.of(
            IN,
            new String[] {DEFAULT_USERNAME, SECOND_USER},
            1L,
            Arrays.asList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(10.)),
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(10.)))),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_USER},
            1L,
            Collections.singletonList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(10.)))),
        Arguments.of(
            NOT_IN, new String[] {DEFAULT_USERNAME, SECOND_USER}, 0L, Collections.emptyList()));
  }

  @ParameterizedTest
  @MethodSource("viewLevelAssigneeFilterScenarios")
  public void viewLevelFilterByAssigneeOnlyIncludesUserTasksWithThatAssignee(
      final MembershipFilterOperator filterOperator,
      final String[] filterValues,
      final Long expectedInstanceCount,
      final List<Tuple> expectedResults) {
    // given
    engineIntegrationExtension.addUser(SECOND_USER, SECOND_USERS_PASSWORD);
    engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);

    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto.getId());
    changeDuration(processInstanceDto, USER_TASK_1, 10.);
    changeDuration(processInstanceDto, USER_TASK_2, 10.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final List<ProcessFilterDto<?>> assigneeFilter =
        ProcessFilterBuilder.filter()
            .assignee()
            .ids(filterValues)
            .operator(filterOperator)
            .filterLevel(FilterApplicationLevel.VIEW)
            .add()
            .buildList();
    reportData.setFilter(assigneeFilter);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
    assertThat(result.getFirstMeasureData())
        .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
        .containsExactlyInAnyOrderElementsOf(expectedResults);
  }

  public static Stream<Arguments> instanceLevelAssigneeFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_USER},
            1L,
            Arrays.asList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(10.)),
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(10.)))),
        Arguments.of(
            IN,
            new String[] {DEFAULT_USERNAME, SECOND_USER},
            2L,
            Arrays.asList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(15.)),
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(20.)))),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_USER},
            2L,
            Arrays.asList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(15.)),
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(20.)))),
        Arguments.of(
            NOT_IN,
            new String[] {DEFAULT_USERNAME, SECOND_USER},
            0L,
            Arrays.asList(Tuple.tuple(USER_TASK_1, null), Tuple.tuple(USER_TASK_2, null))));
  }

  @ParameterizedTest
  @MethodSource("instanceLevelAssigneeFilterScenarios")
  public void instanceLevelFilterByAssigneeOnlyIncludesUserTasksFromInstancesWithThatAssignee(
      final MembershipFilterOperator filterOperator,
      final String[] filterValues,
      final Long expectedInstanceCount,
      final List<Tuple> expectedResults) {
    // given
    engineIntegrationExtension.addUser(SECOND_USER, SECOND_USERS_PASSWORD);
    engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);

    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto firstInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, firstInstance.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, firstInstance.getId());
    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, secondInstance.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, secondInstance.getId());

    changeDuration(firstInstance, USER_TASK_1, 10.);
    changeDuration(firstInstance, USER_TASK_2, 10.);
    changeDuration(secondInstance, USER_TASK_1, 20.);
    changeDuration(secondInstance, USER_TASK_2, 30.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final List<ProcessFilterDto<?>> assigneeFilter =
        ProcessFilterBuilder.filter()
            .assignee()
            .ids(filterValues)
            .operator(filterOperator)
            .filterLevel(FilterApplicationLevel.INSTANCE)
            .add()
            .buildList();
    reportData.setFilter(assigneeFilter);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
    assertThat(result.getFirstMeasureData())
        .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
        .containsExactlyInAnyOrderElementsOf(expectedResults);
  }

  public static Stream<Arguments> viewLevelCandidateGroupFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            1L,
            Collections.singletonList(
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(10.)))),
        Arguments.of(
            IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            1L,
            Arrays.asList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(10.)),
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(10.)))),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            1L,
            Collections.singletonList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(10.)))),
        Arguments.of(
            NOT_IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            0L,
            Collections.emptyList()));
  }

  @ParameterizedTest
  @MethodSource("viewLevelCandidateGroupFilterScenarios")
  public void viewLevelFilterByCandidateGroupOnlyIncludesUserTasksWithThatCandidateGroup(
      final MembershipFilterOperator filterOperator,
      final String[] filterValues,
      final Long expectedInstanceCount,
      final List<Tuple> expectedResult) {
    // given
    engineIntegrationExtension.createGroup(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.createGroup(SECOND_CANDIDATE_GROUP_ID);

    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks();

    changeDuration(processInstanceDto, USER_TASK_1, 10.);
    changeDuration(processInstanceDto, USER_TASK_2, 10.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final List<ProcessFilterDto<?>> candidateGroupFilter =
        ProcessFilterBuilder.filter()
            .candidateGroups()
            .ids(filterValues)
            .operator(filterOperator)
            .filterLevel(FilterApplicationLevel.VIEW)
            .add()
            .buildList();
    reportData.setFilter(candidateGroupFilter);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
    assertThat(result.getFirstMeasureData())
        .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
        .containsExactlyInAnyOrderElementsOf(expectedResult);
  }

  public static Stream<Arguments> instanceLevelCandidateGroupFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            1L,
            Arrays.asList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(10.)),
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(10.)))),
        Arguments.of(
            IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            2L,
            Arrays.asList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(15.)),
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(20.)))),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            2L,
            Arrays.asList(
                Tuple.tuple(USER_TASK_1, calculateExpectedValueGivenDurationsDefaultAggr(15.)),
                Tuple.tuple(USER_TASK_2, calculateExpectedValueGivenDurationsDefaultAggr(20.)))),
        Arguments.of(
            NOT_IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            0L,
            Arrays.asList(Tuple.tuple(USER_TASK_1, null), Tuple.tuple(USER_TASK_2, null))));
  }

  @ParameterizedTest
  @MethodSource("instanceLevelCandidateGroupFilterScenarios")
  public void
      instanceLevelFilterByCandidateGroupOnlyIncludesUserTasksFromInstancesWithThatCandidateGroup(
          final MembershipFilterOperator filterOperator,
          final String[] filterValues,
          final Long expectedInstanceCount,
          final List<Tuple> expectedResult) {
    // given
    engineIntegrationExtension.createGroup(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.createGroup(SECOND_CANDIDATE_GROUP_ID);

    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto firstInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks();
    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks();

    changeDuration(firstInstance, USER_TASK_1, 10.);
    changeDuration(firstInstance, USER_TASK_2, 10.);
    changeDuration(secondInstance, USER_TASK_1, 20.);
    changeDuration(secondInstance, USER_TASK_2, 30.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final List<ProcessFilterDto<?>> candidateGroupFilter =
        ProcessFilterBuilder.filter()
            .candidateGroups()
            .ids(filterValues)
            .operator(filterOperator)
            .filterLevel(FilterApplicationLevel.INSTANCE)
            .add()
            .buildList();
    reportData.setFilter(candidateGroupFilter);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
    assertThat(result.getFirstMeasureData())
        .extracting(MapResultEntryDto::getKey, MapResultEntryDto::getValue)
        .containsExactlyInAnyOrderElementsOf(expectedResult);
  }

  @Data
  static class FlowNodeStatusTestValues {
    List<ProcessFilterDto<?>> processFilter;
    Map<String, Double> expectedIdleDurationValues;
    Map<String, Double> expectedWorkDurationValues;
    Map<String, Double> expectedTotalDurationValues;
  }

  private static Map<String, Double> getExpectedResultsMap(
      final Double userTask1Results, final Double userTask2Results) {
    final Map<String, Double> result = new HashMap<>();
    result.put(USER_TASK_1, userTask1Results);
    result.put(USER_TASK_2, userTask2Results);
    return result;
  }

  protected static Stream<FlowNodeStatusTestValues> getFlowNodeStatusExpectedValues() {
    final FlowNodeStatusTestValues runningStateValues = new FlowNodeStatusTestValues();
    runningStateValues.processFilter =
        ProcessFilterBuilder.filter().runningFlowNodesOnly().add().buildList();
    runningStateValues.expectedIdleDurationValues = getExpectedResultsMap(200., 500.);
    runningStateValues.expectedWorkDurationValues = getExpectedResultsMap(500., null);
    runningStateValues.expectedTotalDurationValues = getExpectedResultsMap(700., 500.);

    final FlowNodeStatusTestValues completedOrCanceled = new FlowNodeStatusTestValues();
    completedOrCanceled.processFilter =
        ProcessFilterBuilder.filter().completedOrCanceledFlowNodesOnly().add().buildList();
    completedOrCanceled.expectedIdleDurationValues = getExpectedResultsMap(100., null);
    completedOrCanceled.expectedWorkDurationValues = getExpectedResultsMap(100., null);
    completedOrCanceled.expectedTotalDurationValues = getExpectedResultsMap(100., null);

    return Stream.of(runningStateValues, completedOrCanceled);
  }

  @ParameterizedTest
  @MethodSource("getFlowNodeStatusExpectedValues")
  public void evaluateReportWithFlowNodeStatusFilter(
      final FlowNodeStatusTestValues flowNodeStatusTestValues) {
    // given
    final OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto firstInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // finish first running task, second now runs but unclaimed
    engineIntegrationExtension.finishAllRunningUserTasks(firstInstance.getId());
    changeDuration(firstInstance, USER_TASK_1, 100.);
    changeUserTaskStartDate(firstInstance, now, USER_TASK_2, 500.);

    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // claim first running task
    engineIntegrationExtension.claimAllRunningUserTasks(secondInstance.getId());
    changeUserTaskStartDate(secondInstance, now, USER_TASK_1, 700.);
    changeUserTaskClaimDate(secondInstance, now, USER_TASK_1, 500.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.setFilter(flowNodeStatusTestValues.processFilter);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertEvaluateReportWithFlowNodeStatusFilter(result, flowNodeStatusTestValues);
  }

  protected abstract void assertEvaluateReportWithFlowNodeStatusFilter(
      ReportResultResponseDto<List<MapResultEntryDto>> result,
      FlowNodeStatusTestValues expectedValues);

  @Test
  public void processDefinitionContainsMultiInstanceBody() {
    // given
    final BpmnModelInstance processWithMultiInstanceUserTask =
        Bpmn
            // @formatter:off
            .createExecutableProcess("processWithMultiInstanceUserTask")
            .startEvent()
            .userTask(USER_TASK_1)
            .multiInstance()
            .cardinality("2")
            .multiInstanceDone()
            .endEvent()
            .done();
    // @formatter:on

    final ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            processWithMultiInstanceUserTask);
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    changeDuration(processInstanceDto, 10.);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(1L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(10., 10.));
  }

  @Test
  public void evaluateReportForMoreThanTenEvents() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();

    for (int i = 0; i < 11; i++) {
      final ProcessInstanceEngineDto processInstanceDto =
          engineIntegrationExtension.startProcessInstance(processDefinition.getId());
      engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
      changeDuration(processInstanceDto, 10.);
    }

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(1L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(10.));
  }

  @Test
  public void filterInReport() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    changeDuration(processInstanceDto, 10.);

    final OffsetDateTime processStartTime =
        engineIntegrationExtension
            .getHistoricProcessInstance(processInstanceDto.getId())
            .getStartTime();

    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.setFilter(createStartDateFilter(null, processStartTime.minusSeconds(1L)));
    ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).isNotNull();
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(0L);

    // when
    reportData = createReport(processDefinition);
    reportData.setFilter(createStartDateFilter(processStartTime, null));
    result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).isNotNull();
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result.getFirstMeasureData())).isEqualTo(1L);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), USER_TASK_1)
                .get()
                .getValue())
        .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(10.));
  }

  private List<ProcessFilterDto<?>> createStartDateFilter(
      final OffsetDateTime startDate, final OffsetDateTime endDate) {
    return ProcessFilterBuilder.filter()
        .fixedInstanceStartDate()
        .start(startDate)
        .end(endDate)
        .add()
        .buildList();
  }

  @Test
  public void optimizeExceptionOnViewEntityIsNull() {
    // given
    final ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getView().setEntity(null);

    // when
    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void optimizeExceptionOnViewPropertyIsNull() {
    // given
    final ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getView().setProperties((ViewProperty) null);

    // when
    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void optimizeExceptionOnGroupByTypeIsNull() {
    // given
    final ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getGroupBy().setType(null);

    // when
    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void aggregationIsNullSafeForDurationFields() {
    // given a userTask that is completed but the total/idle/work duration field is still null
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishAllUserTasks(processInstanceDto);
    importAllEngineEntitiesFromScratch();

    // set duration field to null
    setDurationFieldToNullInElasticsearch(processInstanceDto.getId());
    final ProcessReportDataDto reportData = createReport(processDefinition);

    // then the report can be evaluated without errors
    reportClient.evaluateMapReport(reportData);
  }

  protected abstract UserTaskDurationTime getUserTaskDurationTime();

  protected abstract void changeDuration(
      final ProcessInstanceEngineDto processInstanceDto,
      final String userTaskKey,
      final Double durationInMs);

  protected abstract void changeDuration(
      final ProcessInstanceEngineDto processInstanceDto, final Double durationInMs);

  protected abstract ProcessReportDataDto createReport(
      final String processDefinitionKey, final List<String> versions);

  protected abstract void setDurationFieldToNullInElasticsearch(final String processInstanceId);

  protected void setUserTaskDurationToNull(
      final String processInstanceId, final String durationFieldName) {
    databaseIntegrationTestExtension.setUserTaskDurationToNull(
        processInstanceId, durationFieldName);
  }

  private ProcessReportDataDto createReport(
      final String processDefinitionKey, final String version) {
    return createReport(processDefinitionKey, Collections.singletonList(version));
  }

  protected ProcessReportDataDto createReport(final ProcessDefinitionEngineDto processDefinition) {
    return createReport(processDefinition.getKey(), String.valueOf(processDefinition.getVersion()));
  }

  private void finishAllUserTasks(final ProcessInstanceEngineDto processInstanceDto1) {
    // finish first task
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto1.getId());
    // finish second task
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto1.getId());
  }

  protected String deployAndStartMultiTenantUserTaskProcess(final List<String> deployedTenants) {
    final String processKey = "multiTenantProcess";
    deployedTenants.stream()
        .filter(Objects::nonNull)
        .forEach(tenantId -> engineIntegrationExtension.createTenant(tenantId));
    deployedTenants.forEach(
        tenant -> {
          final ProcessDefinitionEngineDto processDefinitionEngineDto =
              deployOneUserTasksDefinition(processKey, tenant);
          engineIntegrationExtension.startProcessInstance(processDefinitionEngineDto.getId());
        });

    return processKey;
  }

  private ProcessDefinitionEngineDto deployOneUserTasksDefinition() {
    return deployOneUserTasksDefinition("aProcess", null);
  }

  private ProcessDefinitionEngineDto deployOneUserTasksDefinition(
      final String key, final String tenantId) {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(
        getSingleUserTaskDiagram(key), tenantId);
  }

  private ProcessDefinitionEngineDto deployTwoUserTasksDefinition() {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(
        getDoubleUserTaskDiagram());
  }

  long getExecutedFlowNodeCount(final List<MapResultEntryDto> resultData) {
    return resultData.stream().map(MapResultEntryDto::getValue).filter(Objects::nonNull).count();
  }

  private void assertCorrectValueOrdering(final List<MapResultEntryDto> resultData) {
    final List<Double> bucketValues =
        resultData.stream().map(MapResultEntryDto::getValue).collect(Collectors.toList());
    final List<Double> bucketValuesWithoutNullValue =
        bucketValues.stream().filter(Objects::nonNull).collect(Collectors.toList());
    assertThat(bucketValuesWithoutNullValue).isSorted();
    for (int i = resultData.size() - 1; i > getExecutedFlowNodeCount(resultData) - 1; i--) {
      assertThat(bucketValues.get(i)).isNull();
    }
  }

  private void assertDurationMapReportResults(
      final ReportResultResponseDto<List<MapResultEntryDto>> result,
      final Map<String, Double[]> expectedUserTaskValues) {
    assertThat(result.getMeasures())
        .extracting(MeasureResponseDto::getAggregationType)
        .containsExactly(getSupportedAggregationTypes());

    final Map<AggregationDto, List<MapResultEntryDto>> resultByAggregationType =
        result.getMeasures().stream()
            .collect(
                Collectors.toMap(
                    MeasureResponseDto::getAggregationType, MeasureResponseDto::getData));

    Arrays.stream(getSupportedAggregationTypes())
        .forEach(
            aggType -> {
              expectedUserTaskValues
                  .keySet()
                  .forEach(
                      (String userTaskKey) ->
                          assertThat(
                                  MapResultUtil.getEntryForKey(
                                      resultByAggregationType.get(aggType), userTaskKey))
                              .isPresent()
                              .get()
                              .extracting(MapResultEntryDto::getValue)
                              .isEqualTo(
                                  databaseIntegrationTestExtension
                                      .calculateExpectedValueGivenDurations(
                                          expectedUserTaskValues.get(userTaskKey))
                                      .get(aggType)));
            });
  }
}
