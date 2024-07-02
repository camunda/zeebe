/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.processinstance.duration.groupby.none;

import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.ComparisonOperator.GREATER_THAN_EQUALS;
import static io.camunda.optimize.service.util.ProcessReportDataType.PROC_INST_DUR_GROUP_BY_NONE;
import static io.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurationsDefaultAggr;
import static io.camunda.optimize.test.util.DurationAggregationUtil.getSupportedAggregationTypes;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DurationUnit;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.report.process.AbstractProcessDefinitionIT;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ProcessInstanceDurationByNoneReportEvaluationIT extends AbstractProcessDefinitionIT {

  public static final String PROCESS_DEFINITION_KEY = "123";
  private static final String TEST_ACTIVITY = "testActivity";

  @Test
  public void reportEvaluationForOneProcess() {

    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    OffsetDateTime endDate = startDate.plusSeconds(1);
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseExtension.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(processInstanceDto.getId(), endDate);
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        createReport(
            processInstanceDto.getProcessDefinitionKey(),
            processInstanceDto.getProcessDefinitionVersion());

    AuthorizedProcessReportEvaluationResponseDto<Double> evaluationResponse =
        reportClient.evaluateNumberReport(reportData);

    // then
    ProcessReportDataDto resultReportDataDto = evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey())
        .isEqualTo(processInstanceDto.getProcessDefinitionKey());
    assertThat(resultReportDataDto.getDefinitionVersions())
        .contains(processInstanceDto.getProcessDefinitionVersion());
    assertThat(resultReportDataDto.getView()).isNotNull();
    assertThat(resultReportDataDto.getView().getEntity())
        .isEqualTo(ProcessViewEntity.PROCESS_INSTANCE);
    assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
    assertThat(resultReportDataDto.getGroupBy().getType()).isEqualTo(ProcessGroupByType.NONE);
    assertThat(resultReportDataDto.getConfiguration().getProcessPart()).isNotPresent();

    assertThat(evaluationResponse.getResult().getInstanceCount()).isEqualTo(1L);
    Double calculatedResult = evaluationResponse.getResult().getFirstMeasureData();
    assertThat(calculatedResult).isEqualTo(1000.);
  }

  @Test
  public void reportEvaluationById() {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();

    engineDatabaseExtension.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        processInstanceDto.getId(), startDate.plusSeconds(1));
    importAllEngineEntitiesFromScratch();
    ProcessReportDataDto reportDataDto =
        createReport(
            processInstanceDto.getProcessDefinitionKey(),
            processInstanceDto.getProcessDefinitionVersion());

    String reportId = createNewReport(reportDataDto);

    // when
    AuthorizedProcessReportEvaluationResponseDto<Double> evaluationResponse =
        reportClient.evaluateNumberReportById(reportId);

    // then
    ProcessReportDataDto resultReportDataDto = evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey())
        .isEqualTo(processInstanceDto.getProcessDefinitionKey());
    assertThat(resultReportDataDto.getDefinitionVersions())
        .contains(processInstanceDto.getProcessDefinitionVersion());
    assertThat(resultReportDataDto.getView()).isNotNull();
    assertThat(resultReportDataDto.getView().getEntity())
        .isEqualTo(ProcessViewEntity.PROCESS_INSTANCE);
    assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
    assertThat(resultReportDataDto.getGroupBy().getType()).isEqualTo(ProcessGroupByType.NONE);

    Double calculatedResult = evaluationResponse.getResult().getFirstMeasureData();
    assertThat(calculatedResult).isEqualTo(1000.);
  }

  @Test
  public void evaluateReportForMultipleEvents() {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
    ProcessInstanceEngineDto processInstanceDto3 =
        engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
    Map<String, OffsetDateTime> startDatesToUpdate = new HashMap<>();
    startDatesToUpdate.put(processInstanceDto.getId(), startDate);
    startDatesToUpdate.put(processInstanceDto2.getId(), startDate);
    startDatesToUpdate.put(processInstanceDto3.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceStartDates(startDatesToUpdate);
    Map<String, OffsetDateTime> endDatesToUpdate = new HashMap<>();
    endDatesToUpdate.put(processInstanceDto.getId(), startDate.plusSeconds(1));
    endDatesToUpdate.put(processInstanceDto2.getId(), startDate.plusSeconds(2));
    endDatesToUpdate.put(processInstanceDto3.getId(), startDate.plusSeconds(9));
    engineDatabaseExtension.changeProcessInstanceEndDates(endDatesToUpdate);
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportDataDto =
        createReport(
            processInstanceDto.getProcessDefinitionKey(),
            processInstanceDto.getProcessDefinitionVersion());
    ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportDataDto).getResult();

    // then
    assertThat(resultDto.getFirstMeasureData()).isNotNull();
    assertThat(resultDto.getFirstMeasureData()).isNotNull().isEqualTo(4000.);
  }

  @Test
  public void evaluateReportForMultipleEventsWithAllAggregationTypes() {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
    ProcessInstanceEngineDto processInstanceDto3 =
        engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
    Map<String, OffsetDateTime> startDatesToUpdate = new HashMap<>();
    startDatesToUpdate.put(processInstanceDto.getId(), startDate);
    startDatesToUpdate.put(processInstanceDto2.getId(), startDate);
    startDatesToUpdate.put(processInstanceDto3.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceStartDates(startDatesToUpdate);
    Map<String, OffsetDateTime> endDatesToUpdate = new HashMap<>();
    endDatesToUpdate.put(processInstanceDto.getId(), startDate.plusSeconds(1));
    endDatesToUpdate.put(processInstanceDto2.getId(), startDate.plusSeconds(2));
    endDatesToUpdate.put(processInstanceDto3.getId(), startDate.plusSeconds(9));
    engineDatabaseExtension.changeProcessInstanceEndDates(endDatesToUpdate);
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        createReport(
            processInstanceDto.getProcessDefinitionKey(),
            processInstanceDto.getProcessDefinitionVersion());
    reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());

    final AuthorizedProcessReportEvaluationResponseDto<Double> evaluationResponse =
        reportClient.evaluateNumberReport(reportData);

    // then
    assertAggregationResults(evaluationResponse, 1000., 2000., 9000.);
  }

  @Test
  public void noAvailableProcessInstancesReturnsNull() {
    // when
    ProcessReportDataDto reportData = createReport("fooProcDef", "1");

    ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getFirstMeasureData()).isNull();
  }

  @Test
  public void otherProcessDefinitionsDoNoAffectResult() {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();

    String processDefinitionKey = processInstanceDto.getProcessDefinitionKey();
    String processDefinitionVersion = processInstanceDto.getProcessDefinitionVersion();

    engineDatabaseExtension.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        processInstanceDto.getId(), startDate.plusSeconds(1));
    processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
    engineDatabaseExtension.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        processInstanceDto.getId(), startDate.plusSeconds(9));
    processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
    engineDatabaseExtension.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        processInstanceDto.getId(), startDate.plusSeconds(2));
    deployAndStartSimpleServiceTaskProcess();
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(processDefinitionKey, processDefinitionVersion);
    reportData.getConfiguration().setAggregationTypes(getSupportedAggregationTypes());

    final AuthorizedProcessReportEvaluationResponseDto<Double> evaluationResponse =
        reportClient.evaluateNumberReport(reportData);

    // then
    assertAggregationResults(evaluationResponse, 1000., 9000., 2000.);
  }

  @Test
  public void reportEvaluationSingleBucketFilteredBySingleTenant() {
    // given
    final String tenantId1 = "tenantId1";
    final String tenantId2 = "tenantId2";
    final List<String> selectedTenants = Collections.singletonList(tenantId1);
    final String processKey =
        deployAndStartMultiTenantSimpleServiceTaskProcess(
            Arrays.asList(null, tenantId1, tenantId2));

    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(processKey, ALL_VERSIONS);
    reportData.setTenantIds(selectedTenants);
    ReportResultResponseDto<Double> result =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo(selectedTenants.size());
  }

  @Test
  public void filterInReportWorks() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("var", true);
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto =
        deployAndStartSimpleServiceTaskProcessWithVariables(variables);
    engineDatabaseExtension.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        processInstanceDto.getId(), startDate.plusSeconds(1));
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineIntegrationExtension.startProcessInstance(processDefinitionId);
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        createReport(
            processInstanceDto.getProcessDefinitionKey(),
            processInstanceDto.getProcessDefinitionVersion());

    reportData.setFilter(
        ProcessFilterBuilder.filter().variable().booleanTrue().name("var").add().buildList());
    ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    Double calculatedResult = resultDto.getFirstMeasureData();
    assertThat(calculatedResult).isEqualTo(1000.);

    // when
    reportData.setFilter(
        ProcessFilterBuilder.filter().variable().booleanFalse().name("var").add().buildList());
    resultDto = reportClient.evaluateNumberReport(reportData).getResult();

    // then
    calculatedResult = resultDto.getFirstMeasureData();
    assertThat(calculatedResult).isNull();
  }

  @ParameterizedTest
  @MethodSource("viewLevelFilters")
  public void viewLevelFiltersOnlyAppliedToInstances(
      final List<ProcessFilterDto<?>> filtersToApply) {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    deployAndStartSimpleServiceTaskProcess();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseExtension.changeProcessInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        processInstanceDto.getId(), startDate.plusSeconds(1));
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineIntegrationExtension.startProcessInstance(processDefinitionId);
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        createReport(
            processInstanceDto.getProcessDefinitionKey(),
            processInstanceDto.getProcessDefinitionVersion());

    reportData.setFilter(filtersToApply);
    ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isZero();
    assertThat(resultDto.getInstanceCountWithoutFilters()).isEqualTo(2L);
  }

  @Test
  public void calculateDurationForRunningProcessInstances() {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    // 1 completed proc inst
    ProcessInstanceEngineDto completeProcessInstanceDto = deployAndStartSimpleUserTaskProcess();
    engineIntegrationExtension.finishAllRunningUserTasks(completeProcessInstanceDto.getId());

    OffsetDateTime completedProcInstStartDate = now.minusDays(2);
    OffsetDateTime completedProcInstEndDate = completedProcInstStartDate.plus(1000, MILLIS);
    engineDatabaseExtension.changeProcessInstanceStartDate(
        completeProcessInstanceDto.getId(), completedProcInstStartDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        completeProcessInstanceDto.getId(), completedProcInstEndDate);

    // 1 running proc inst
    final ProcessInstanceEngineDto newRunningProcessInstance =
        engineIntegrationExtension.startProcessInstance(
            completeProcessInstanceDto.getDefinitionId());
    OffsetDateTime runningProcInstStartDate = now.minusDays(1);
    engineDatabaseExtension.changeProcessInstanceStartDate(
        newRunningProcessInstance.getId(), runningProcInstStartDate);

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> runningInstanceFilter =
        ProcessFilterBuilder.filter().runningInstancesOnly().add().buildList();

    ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
            .setProcessDefinitionKey(completeProcessInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(completeProcessInstanceDto.getProcessDefinitionVersion())
            .setFilter(runningInstanceFilter)
            .build();
    final ReportResultResponseDto<Double> result =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData())
        .isNotNull()
        .isEqualTo((double) runningProcInstStartDate.until(now, MILLIS));
  }

  @Test
  public void calculateDurationForCompletedProcessInstances() {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    // 1 completed proc inst
    ProcessInstanceEngineDto completeProcessInstanceDto = deployAndStartSimpleUserTaskProcess();
    engineIntegrationExtension.finishAllRunningUserTasks(completeProcessInstanceDto.getId());

    OffsetDateTime completedProcInstStartDate = now.minusDays(2);
    OffsetDateTime completedProcInstEndDate = completedProcInstStartDate.plus(1000, MILLIS);
    engineDatabaseExtension.changeProcessInstanceStartDate(
        completeProcessInstanceDto.getId(), completedProcInstStartDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        completeProcessInstanceDto.getId(), completedProcInstEndDate);

    // 1 running proc inst
    final ProcessInstanceEngineDto newRunningProcessInstance =
        engineIntegrationExtension.startProcessInstance(
            completeProcessInstanceDto.getDefinitionId());
    OffsetDateTime runningProcInstStartDate = now.minusDays(1);
    engineDatabaseExtension.changeProcessInstanceStartDate(
        newRunningProcessInstance.getId(), runningProcInstStartDate);

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> completedInstanceFilter =
        ProcessFilterBuilder.filter().completedInstancesOnly().add().buildList();

    ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
            .setProcessDefinitionKey(completeProcessInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(completeProcessInstanceDto.getProcessDefinitionVersion())
            .setFilter(completedInstanceFilter)
            .build();
    final ReportResultResponseDto<Double> result =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    final Double resultData = result.getFirstMeasureData();
    assertThat(resultData).isEqualTo(1000.);
  }

  @Test
  public void calculateDurationForRunningAndCompletedProcessInstances() {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    // 1 completed proc inst
    ProcessInstanceEngineDto completeProcessInstanceDto = deployAndStartSimpleUserTaskProcess();
    engineIntegrationExtension.finishAllRunningUserTasks(completeProcessInstanceDto.getId());

    OffsetDateTime completedProcInstStartDate = now.minusDays(2);
    OffsetDateTime completedProcInstEndDate = completedProcInstStartDate.plus(1000, MILLIS);
    engineDatabaseExtension.changeProcessInstanceStartDate(
        completeProcessInstanceDto.getId(), completedProcInstStartDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        completeProcessInstanceDto.getId(), completedProcInstEndDate);

    // 1 running proc inst
    final ProcessInstanceEngineDto newRunningProcessInstance =
        engineIntegrationExtension.startProcessInstance(
            completeProcessInstanceDto.getDefinitionId());
    OffsetDateTime runningProcInstStartDate = now.minusDays(1);
    engineDatabaseExtension.changeProcessInstanceStartDate(
        newRunningProcessInstance.getId(), runningProcInstStartDate);

    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
            .setProcessDefinitionKey(completeProcessInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(completeProcessInstanceDto.getProcessDefinitionVersion())
            .build();

    final ReportResultResponseDto<Double> result =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    final Double resultData = result.getFirstMeasureData();
    assertThat(resultData)
        .isEqualTo(
            calculateExpectedValueGivenDurationsDefaultAggr(
                1000., (double) runningProcInstStartDate.until(now, MILLIS)));
  }

  @Test
  public void durationFilterWorksForRunningProcessInstances() {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    // 1 completed proc inst
    ProcessInstanceEngineDto completeProcessInstanceDto = deployAndStartSimpleUserTaskProcess();
    engineIntegrationExtension.finishAllRunningUserTasks(completeProcessInstanceDto.getId());

    OffsetDateTime completedProcInstStartDate = now.minusDays(2);
    OffsetDateTime completedProcInstEndDate = completedProcInstStartDate.plus(1000, MILLIS);
    engineDatabaseExtension.changeProcessInstanceStartDate(
        completeProcessInstanceDto.getId(), completedProcInstStartDate);
    engineDatabaseExtension.changeProcessInstanceEndDate(
        completeProcessInstanceDto.getId(), completedProcInstEndDate);

    // 1 running proc inst
    final ProcessInstanceEngineDto newRunningProcessInstance =
        engineIntegrationExtension.startProcessInstance(
            completeProcessInstanceDto.getDefinitionId());
    OffsetDateTime runningProcInstStartDate = now.minusDays(1);
    engineDatabaseExtension.changeProcessInstanceStartDate(
        newRunningProcessInstance.getId(), runningProcInstStartDate);

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> durationFilter =
        ProcessFilterBuilder.filter()
            .duration()
            .operator(GREATER_THAN_EQUALS)
            .unit(DurationUnit.HOURS)
            .value(1L)
            .add()
            .buildList();

    ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
            .setProcessDefinitionKey(completeProcessInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(completeProcessInstanceDto.getProcessDefinitionVersion())
            .setFilter(durationFilter)
            .build();
    final ReportResultResponseDto<Double> result =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    final Double resultData = result.getFirstMeasureData();
    assertThat(resultData).isEqualTo((double) runningProcInstStartDate.until(now, MILLIS));
  }

  @Test
  public void optimizeExceptionOnViewPropertyIsNull() {
    // given
    ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");

    dataDto.getView().setProperties((ViewProperty) null);

    // when
    Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void optimizeExceptionOnGroupByTypeIsNull() {
    // given
    ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");

    dataDto.getGroupBy().setType(null);

    // when
    Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  private ProcessInstanceEngineDto deployAndStartSimpleServiceTaskProcessWithVariables(
      Map<String, Object> variables) {
    BpmnModelInstance processModel =
        Bpmn.createExecutableProcess("aProcess")
            .name("aProcessName")
            .startEvent()
            .serviceTask(ProcessInstanceDurationByNoneReportEvaluationIT.TEST_ACTIVITY)
            .camundaExpression("${true}")
            .endEvent()
            .done();
    return engineIntegrationExtension.deployAndStartProcessWithVariables(processModel, variables);
  }

  private void assertAggregationResults(
      AuthorizedProcessReportEvaluationResponseDto<Double> evaluationResponse,
      Number... durationsToCalculate) {
    final Map<AggregationDto, Double> expectedAggregationResults =
        databaseIntegrationTestExtension.calculateExpectedValueGivenDurations(durationsToCalculate);
    final Map<AggregationDto, Double> resultByAggregationType =
        evaluationResponse.getResult().getMeasures().stream()
            .collect(
                Collectors.toMap(
                    MeasureResponseDto::getAggregationType, MeasureResponseDto::getData));
    assertThat(resultByAggregationType)
        .hasSize(getSupportedAggregationTypes().length)
        .containsAllEntriesOf(expectedAggregationResults);
  }

  private ProcessReportDataDto createReport(String processKey, String definitionVersion) {
    return TemplatedProcessReportDataBuilder.createReportData()
        .setProcessDefinitionKey(processKey)
        .setProcessDefinitionVersion(definitionVersion)
        .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
        .build();
  }
}
