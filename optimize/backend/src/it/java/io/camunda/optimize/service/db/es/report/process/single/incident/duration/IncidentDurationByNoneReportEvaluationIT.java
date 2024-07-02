/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.incident.duration;

import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.AVERAGE;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.MAX;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.MIN;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.PERCENTILE;
import static io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel.INSTANCE;
import static io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel.VIEW;
import static io.camunda.optimize.service.db.es.report.process.single.incident.duration.IncidentDataDeployer.IncidentProcessType.ONE_TASK;
import static io.camunda.optimize.service.db.es.report.process.single.incident.duration.IncidentDataDeployer.PROCESS_DEFINITION_KEY;
import static io.camunda.optimize.service.util.ProcessReportDataType.INCIDENT_DUR_GROUP_BY_NONE;
import static io.camunda.optimize.test.optimize.CollectionClient.DEFAULT_TENANT;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.ReportConstants;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.report.process.AbstractProcessDefinitionIT;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import io.camunda.optimize.test.util.DateCreationFreezer;
import io.camunda.optimize.util.BpmnModels;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IncidentDurationByNoneReportEvaluationIT extends AbstractProcessDefinitionIT {

  private Stream<Consumer<Long>> startInstanceWithDifferentIncidentStates() {
    // @formatter:off
    return Stream.of(
        (durationInSec) ->
            IncidentDataDeployer.dataDeployer(incidentClient)
                .deployProcess(ONE_TASK)
                .startProcessInstance()
                .withOpenIncident()
                .withIncidentDurationInSec(durationInSec)
                .executeDeployment(),
        (durationInSec) ->
            IncidentDataDeployer.dataDeployer(incidentClient)
                .deployProcess(ONE_TASK)
                .startProcessInstance()
                .withResolvedIncident()
                .withIncidentDurationInSec(durationInSec)
                .executeDeployment(),
        (durationInSec) ->
            IncidentDataDeployer.dataDeployer(incidentClient)
                .deployProcess(ONE_TASK)
                .startProcessInstance()
                .withDeletedIncident()
                .withIncidentDurationInSec(durationInSec)
                .executeDeployment());
    // @formatter:on
  }

  @ParameterizedTest
  @MethodSource("startInstanceWithDifferentIncidentStates")
  public void allIncidentStates(Consumer<Long> startProcessWithIncidentWithDurationInSec) {
    // given
    startProcessWithIncidentWithDurationInSec.accept(1L);
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(PROCESS_DEFINITION_KEY, "1");
    AuthorizedProcessReportEvaluationResponseDto<Double> evaluationResponse =
        reportClient.evaluateNumberReport(reportData);

    // then
    ProcessReportDataDto resultReportDataDto = evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
    assertThat(resultReportDataDto.getDefinitionVersions()).containsExactly("1");
    assertThat(resultReportDataDto.getView()).isNotNull();
    assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.INCIDENT);
    assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
    assertThat(resultReportDataDto.getGroupBy().getType()).isEqualTo(ProcessGroupByType.NONE);

    final ReportResultResponseDto<Double> resultDto = evaluationResponse.getResult();
    assertThat(resultDto.getInstanceCount()).isEqualTo(1L);
    assertThat(resultDto.getFirstMeasureData()).isNotNull().isEqualTo(1000.);
  }

  @Test
  public void customIncidentTypes() {
    // given
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(1L)
        .startProcessInstance()
        .withOpenIncidentOfCustomType("myCustomIncidentType")
        .withIncidentDurationInSec(3L)
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        createReport(IncidentDataDeployer.PROCESS_DEFINITION_KEY, "1");
    final ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(2L);
    assertThat(resultDto.getFirstMeasureData()).isNotNull();
    assertThat(resultDto.getFirstMeasureData()).isEqualTo(2000.);
  }

  @Test
  public void severalOpenIncidentsForMultipleProcessInstances() {
    // given
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withOpenIncident()
        .withIncidentDurationInSec(3L)
        .startProcessInstance()
        .withOpenIncident()
        .withIncidentDurationInSec(3L)
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(PROCESS_DEFINITION_KEY, "1");
    final ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(2L);
    assertThat(resultDto.getFirstMeasureData()).isNotNull().isEqualTo(3000.);
  }

  @Test
  public void severalResolvedIncidentsForMultipleProcessInstances() {
    // given
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(3L)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(1L)
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(PROCESS_DEFINITION_KEY, "1");
    final ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(2L);
    assertThat(resultDto.getFirstMeasureData()).isNotNull().isEqualTo(2000.);
  }

  @Test
  public void differentIncidentTypesInTheSameReport() {
    // given
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withOpenIncident()
        .withIncidentDurationInSec(1L)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(2L)
        .startProcessInstance()
        .withDeletedIncident()
        .withIncidentDurationInSec(6L)
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(PROCESS_DEFINITION_KEY, "1");
    final ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(3L);
    assertThat(resultDto.getFirstMeasureData())
        .isNotNull()
        .isEqualTo(3000.); // uses the average by default
  }

  @Test
  public void otherProcessDefinitionVersionsDoNoAffectResult() {
    // given
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(55L)
        .executeDeployment();

    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(22L)
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(PROCESS_DEFINITION_KEY, "1");
    final ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(1L);
    assertThat(resultDto.getFirstMeasureData()).isNotNull().isEqualTo(55_000.);
  }

  @Test
  public void incidentReportAcrossMultipleDefinitionVersions() {
    // given
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(1L)
        .executeDeployment();

    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(5L)
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        createReport(PROCESS_DEFINITION_KEY, ReportConstants.ALL_VERSIONS);
    final ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(2L);
    assertThat(resultDto.getFirstMeasureData()).isNotNull().isEqualTo(3000.);
  }

  @Test
  public void reportEvaluationSingleBucketFilteredBySingleTenant() {
    // given
    final String tenantId1 = "tenantId1";
    final String tenantId2 = "tenantId2";
    final List<String> selectedTenants = Collections.singletonList(tenantId1);
    engineIntegrationExtension.createTenant(tenantId1);
    engineIntegrationExtension.createTenant(tenantId2);

    OffsetDateTime creationDate = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
    final ProcessInstanceEngineDto processInstanceEngineDto1 =
        incidentClient.deployAndStartProcessInstanceWithTenantAndWithOpenIncident(tenantId1);
    engineDatabaseExtension.changeIncidentCreationDate(
        processInstanceEngineDto1.getId(), creationDate.minusSeconds(1L));
    final ProcessInstanceEngineDto processInstanceEngineDto2 =
        incidentClient.deployAndStartProcessInstanceWithTenantAndWithOpenIncident(tenantId2);
    engineDatabaseExtension.changeIncidentCreationDate(
        processInstanceEngineDto2.getId(), creationDate.minusSeconds(2L));
    final ProcessInstanceEngineDto processInstanceEngineDto3 =
        incidentClient.deployAndStartProcessInstanceWithTenantAndWithOpenIncident(DEFAULT_TENANT);
    engineDatabaseExtension.changeIncidentCreationDate(
        processInstanceEngineDto3.getId(), creationDate.minusSeconds(3L));

    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        createReport(
            processInstanceEngineDto1.getProcessDefinitionKey(), ReportConstants.ALL_VERSIONS);
    reportData.setTenantIds(selectedTenants);
    ReportResultResponseDto<Double> result =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo(1L);
    assertThat(result.getFirstMeasureData()).isEqualTo(1000.);
  }

  @Test
  public void filterInReport() {
    // given two process instances
    // Instance 1: one incident in task 1 (resolved) which completes the process instance
    // Instance 2: one incident in task 1 (open) and because of that the task is still pending.
    // Hint: failExternalTasks method does not complete the tasks
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(1L)
        .startProcessInstance()
        .withOpenIncident()
        .withIncidentDurationInSec(3L)
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when I create a report without filters
    ProcessReportDataDto reportData =
        createReport(PROCESS_DEFINITION_KEY, ReportConstants.ALL_VERSIONS);
    ReportResultResponseDto<Double> result =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then the result has two process instances
    assertThat(result.getInstanceCount()).isEqualTo(2L);
    assertThat(result.getFirstMeasureData()).isEqualTo(2000.);

    // when I create a running process instances only filter
    List<ProcessFilterDto<?>> runningProcessInstancesOnly =
        ProcessFilterBuilder.filter().runningInstancesOnly().add().buildList();
    reportData.setFilter(runningProcessInstancesOnly);
    result = reportClient.evaluateNumberReport(reportData).getResult();

    // then we only get instance 1 because there's only one running
    assertThat(result.getInstanceCount()).isEqualTo(1L);
    assertThat(result.getFirstMeasureData()).isEqualTo(3000.);
  }

  private Stream<Arguments> filterAndExpectedResult() {
    return Stream.of(
        Arguments.of(
            ProcessFilterBuilder.filter()
                .withOpenIncident()
                .filterLevel(INSTANCE)
                .add()
                .buildList(),
            3000.),
        Arguments.of(
            ProcessFilterBuilder.filter().withOpenIncident().filterLevel(VIEW).add().buildList(),
            3000.),
        Arguments.of(
            ProcessFilterBuilder.filter()
                .withResolvedIncident()
                .filterLevel(INSTANCE)
                .add()
                .buildList(),
            1000.),
        Arguments.of(
            ProcessFilterBuilder.filter()
                .withResolvedIncident()
                .filterLevel(VIEW)
                .add()
                .buildList(),
            1000.),
        Arguments.of(
            ProcessFilterBuilder.filter().noIncidents().filterLevel(INSTANCE).add().buildList(),
            null));
  }

  @ParameterizedTest
  @MethodSource("filterAndExpectedResult")
  public void incidentFilterIsAppliedAtCorrectLevel(
      final List<ProcessFilterDto<?>> filter, final Double expectedResult) {
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(1L)
        .startProcessInstance()
        .withOpenIncident()
        .withIncidentDurationInSec(3L)
        .startProcessInstance()
        .withoutIncident()
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        createReport(PROCESS_DEFINITION_KEY, ReportConstants.ALL_VERSIONS);
    reportData.setFilter(filter);
    ReportResultResponseDto<Double> result =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo(1L);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(3L);
    assertThat(result.getFirstMeasureData()).isEqualTo(expectedResult);
  }

  @Test
  public void noIncidentReturnsNullAsResult() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto =
        engineIntegrationExtension.deployAndStartProcess(BpmnModels.getSimpleBpmnDiagram());
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        createReport(
            processInstanceEngineDto.getProcessDefinitionKey(),
            processInstanceEngineDto.getProcessDefinitionVersion());
    final ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(1L);
    assertThat(resultDto.getFirstMeasureData()).isNull();
  }

  @Test
  public void processInstanceWithIncidentAndOneWithoutIncident() {
    // given
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withoutIncident()
        .startProcessInstance()
        .withOpenIncident()
        .withIncidentDurationInSec(3L)
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(PROCESS_DEFINITION_KEY, "1");
    final ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(2L);
    assertThat(resultDto.getFirstMeasureData()).isNotNull().isEqualTo(3000.);
  }

  private Stream<Arguments> aggregations() {
    // @formatter:off
    return Stream.of(
        Arguments.of(new AggregationDto(MIN), 1000.),
        Arguments.of(new AggregationDto(MAX), 9000.),
        Arguments.of(new AggregationDto(AVERAGE), 4000.),
        Arguments.of(new AggregationDto(PERCENTILE, 50.), 2000.));
    // @formatter:on
  }

  @ParameterizedTest
  @MethodSource("aggregations")
  public void aggregations(final AggregationDto aggregationDto, final double expectedResult) {
    // given
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(ONE_TASK)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(1L)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(2L)
        .startProcessInstance()
        .withResolvedIncident()
        .withIncidentDurationInSec(9L)
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(PROCESS_DEFINITION_KEY, "1");
    reportData.getConfiguration().setAggregationTypes(aggregationDto);
    final ReportResultResponseDto<Double> resultDto =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(resultDto.getInstanceCount()).isEqualTo(3L);
    assertThat(resultDto.getFirstMeasureData()).isNotNull().isEqualTo(expectedResult);
  }

  private ProcessReportDataDto createReport(
      String processDefinitionKey, String processDefinitionVersion) {
    return TemplatedProcessReportDataBuilder.createReportData()
        .setProcessDefinitionKey(processDefinitionKey)
        .setProcessDefinitionVersion(processDefinitionVersion)
        .setReportDataType(INCIDENT_DUR_GROUP_BY_NONE)
        .build();
  }
}
