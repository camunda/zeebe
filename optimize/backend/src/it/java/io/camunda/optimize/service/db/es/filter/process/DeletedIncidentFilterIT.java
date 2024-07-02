/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter.process;

import static io.camunda.optimize.service.db.es.report.process.single.incident.duration.IncidentDataDeployer.IncidentProcessType.ONE_TASK;
import static io.camunda.optimize.service.db.es.report.process.single.incident.duration.IncidentDataDeployer.IncidentProcessType.TWO_SEQUENTIAL_TASKS;
import static io.camunda.optimize.util.BpmnModels.SERVICE_TASK_ID_2;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessInstanceDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.report.process.single.incident.duration.IncidentDataDeployer;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DeletedIncidentFilterIT extends AbstractFilterIT {

  @Test
  public void instanceLevelFilterByDeletedIncident() {
    // given
    // @formatter:off
    final List<ProcessInstanceEngineDto> deployedInstances =
        IncidentDataDeployer.dataDeployer(incidentClient)
            .deployProcess(ONE_TASK)
            .startProcessInstance()
            .withOpenIncident()
            .startProcessInstance()
            .withResolvedIncident()
            .startProcessInstance()
            .withDeletedIncident()
            .startProcessInstance()
            .withoutIncident()
            .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();
    final List<ProcessFilterDto<?>> filter = deletedIncidentFilter();

    // when
    ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(IncidentDataDeployer.PROCESS_DEFINITION_KEY)
            .setProcessDefinitionVersion("1")
            .setReportDataType(ProcessReportDataType.RAW_DATA)
            .build();
    reportData.setFilter(filter);
    ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        reportClient.evaluateRawReport(reportData).getResult();

    // then the result contains only the instance with a deleted incident
    assertThat(result.getInstanceCount()).isEqualTo(1L);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(4L);
    assertThat(result.getData())
        .hasSize(1)
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .containsExactly(deployedInstances.get(2).getId());

    // when
    reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(IncidentDataDeployer.PROCESS_DEFINITION_KEY)
            .setProcessDefinitionVersion("1")
            .setReportDataType(ProcessReportDataType.INCIDENT_FREQ_GROUP_BY_NONE)
            .build();
    reportData.setFilter(filter);
    ReportResultResponseDto<Double> numberResult =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(numberResult.getInstanceCount()).isEqualTo(1L);
    assertThat(numberResult.getInstanceCountWithoutFilters()).isEqualTo(4L);
    assertThat(numberResult.getFirstMeasureData()).isEqualTo(1.);
  }

  @Test
  public void canBeMixedWithOtherFilters() {
    // given
    // @formatter:off
    IncidentDataDeployer.dataDeployer(incidentClient)
        .deployProcess(TWO_SEQUENTIAL_TASKS)
        .startProcessInstance()
        .withOpenIncident()
        .startProcessInstance()
        .withDeletedIncident()
        .executeDeployment();
    // @formatter:on
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(IncidentDataDeployer.PROCESS_DEFINITION_KEY)
            .setProcessDefinitionVersion("1")
            .setReportDataType(ProcessReportDataType.INCIDENT_FREQ_GROUP_BY_NONE)
            .build();
    reportData.setFilter(deletedIncidentFilter());
    ReportResultResponseDto<Double> numberResult =
        reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(numberResult.getInstanceCount()).isEqualTo(1L);
    assertThat(numberResult.getInstanceCountWithoutFilters()).isEqualTo(2L);
    assertThat(numberResult.getFirstMeasureData()).isEqualTo(1.);

    // when I add the flow node filter as well
    reportData.setFilter(
        ProcessFilterBuilder.filter()
            .withDeletedIncident()
            .filterLevel(FilterApplicationLevel.INSTANCE)
            .add()
            .executedFlowNodes()
            .id(SERVICE_TASK_ID_2)
            .add()
            .buildList());
    numberResult = reportClient.evaluateNumberReport(reportData).getResult();

    // then
    assertThat(numberResult.getInstanceCount()).isEqualTo(0L);
    assertThat(numberResult.getInstanceCountWithoutFilters()).isEqualTo(2L);
    assertThat(numberResult.getFirstMeasureData()).isEqualTo(0.);
  }

  private List<ProcessFilterDto<?>> deletedIncidentFilter() {
    return ProcessFilterBuilder.filter()
        .withDeletedIncident()
        .filterLevel(FilterApplicationLevel.INSTANCE)
        .add()
        .buildList();
  }
}
