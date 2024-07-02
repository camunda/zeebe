/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.usertask.duration.groupby.usertask.distributedby.process;

import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.MAX;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.PERCENTILE;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.SUM;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime.TOTAL;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.UserTaskDurationTime.WORK;
import static io.camunda.optimize.service.util.ProcessReportDataType.USER_TASK_DUR_GROUP_BY_USER_TASK_BY_PROCESS;
import static io.camunda.optimize.util.BpmnModels.USER_TASK_1;
import static io.camunda.optimize.util.BpmnModels.USER_TASK_2;
import static io.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedByType;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.util.IdGenerator;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import java.util.Collections;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

public class UserTaskDurationByUserTaskByProcessReportEvaluationIT extends AbstractPlatformIT {

  private static final String V_1_IDENTIFIER = "v1Identifier";
  private static final String ALL_IDENTIFIER = "allIdentifier";
  private static final String V_1_DISPLAY_NAME = "v1";
  private static final String ALL_VERSIONS_DISPLAY_NAME = "all";

  @Test
  public void reportEvaluationWithSingleProcessDefinitionSource() {
    // given
    final ProcessInstanceEngineDto instance =
        engineIntegrationExtension.deployAndStartProcess(getDoubleUserTaskDiagram());
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(instance.getId(), 1000);
    importAllEngineEntitiesFromScratch();
    final String processDisplayName = "processDisplayName";
    final String processIdentifier = IdGenerator.getNextId();
    ReportDataDefinitionDto definition =
        new ReportDataDefinitionDto(
            processIdentifier, instance.getProcessDefinitionKey(), processDisplayName);

    // when
    final ProcessReportDataDto reportData = createReport(Collections.singletonList(definition));
    final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
        evaluationResponse = reportClient.evaluateHyperMapReport(reportData);

    // then
    final ProcessReportDataDto resultReportDataDto =
        evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey())
        .isEqualTo(instance.getProcessDefinitionKey());
    assertThat(resultReportDataDto.getDefinitionVersions())
        .containsExactly(definition.getVersions().get(0));
    assertThat(resultReportDataDto.getView()).isNotNull();
    assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
    assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
    assertThat(resultReportDataDto.getGroupBy()).isNotNull();
    assertThat(resultReportDataDto.getGroupBy().getType()).isEqualTo(ProcessGroupByType.USER_TASKS);
    assertThat(resultReportDataDto.getGroupBy().getValue()).isNull();
    assertThat(resultReportDataDto.getDistributedBy().getType())
        .isEqualTo(DistributedByType.PROCESS);

    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        evaluationResponse.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(1);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(1);
    assertThat(result.getMeasures())
        .hasSize(1)
        .extracting(MeasureResponseDto::getData)
        .containsExactly(
            List.of(
                createHyperMapResult(
                    USER_TASK_1,
                    new MapResultEntryDto(processIdentifier, 1000.0, processDisplayName)),
                createHyperMapResult(
                    USER_TASK_2,
                    new MapResultEntryDto(processIdentifier, 1000.0, processDisplayName))));
  }

  @Test
  public void reportEvaluationWithMultipleProcessDefinitionSources() {
    // given
    final ProcessInstanceEngineDto firstInstance =
        engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("first"));
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(firstInstance.getId(), 1000);
    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.deployAndStartProcess(getDoubleUserTaskDiagram("second"));
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(secondInstance.getId(), 5000);
    importAllEngineEntitiesFromScratch();
    final String firstDisplayName = "firstName";
    final String secondDisplayName = "secondName";
    final String firstIdentifier = "first";
    final String secondIdentifier = "second";
    ReportDataDefinitionDto firstDefinition =
        new ReportDataDefinitionDto(
            firstIdentifier, firstInstance.getProcessDefinitionKey(), firstDisplayName);
    ReportDataDefinitionDto secondDefinition =
        new ReportDataDefinitionDto(
            secondIdentifier, secondInstance.getProcessDefinitionKey(), secondDisplayName);

    // when
    final ProcessReportDataDto reportData =
        createReport(List.of(firstDefinition, secondDefinition));
    final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
        evaluationResponse = reportClient.evaluateHyperMapReport(reportData);

    // then
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        evaluationResponse.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(2);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
    assertThat(result.getMeasures())
        .hasSize(1)
        .extracting(MeasureResponseDto::getData)
        .containsExactly(
            List.of(
                createHyperMapResult(
                    USER_TASK_1,
                    new MapResultEntryDto(firstIdentifier, 1000.0, firstDisplayName),
                    new MapResultEntryDto(secondIdentifier, 5000.0, secondDisplayName)),
                createHyperMapResult(
                    USER_TASK_2,
                    new MapResultEntryDto(firstIdentifier, null, firstDisplayName),
                    new MapResultEntryDto(secondIdentifier, 5000.0, secondDisplayName))));
  }

  @Test
  public void reportEvaluationWithMultipleProcessDefinitionSourcesAndOverlappingInstances() {
    // given
    final ProcessInstanceEngineDto v1Instance =
        engineIntegrationExtension.deployAndStartProcess(getSingleUserTaskDiagram("definition"));
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(v1Instance.getId(), 1000);
    final ProcessInstanceEngineDto v2Instance =
        engineIntegrationExtension.deployAndStartProcess(getDoubleUserTaskDiagram("definition"));
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(v2Instance.getId(), 5000);
    importAllEngineEntitiesFromScratch();
    final String v1displayName = "v1";
    final String allVersionsDisplayName = "all";
    final String v1Identifier = "v1Identifier";
    final String allVersionsIdentifier = "allIdentifier";
    ReportDataDefinitionDto v1definition =
        new ReportDataDefinitionDto(
            v1Identifier, v1Instance.getProcessDefinitionKey(), v1displayName);
    v1definition.setVersion("1");
    ReportDataDefinitionDto allVersionsDefinition =
        new ReportDataDefinitionDto(
            allVersionsIdentifier, v2Instance.getProcessDefinitionKey(), allVersionsDisplayName);
    allVersionsDefinition.setVersion(ALL_VERSIONS);

    // when
    final ProcessReportDataDto reportData =
        createReport(List.of(v1definition, allVersionsDefinition));
    final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
        evaluationResponse = reportClient.evaluateHyperMapReport(reportData);

    // then
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        evaluationResponse.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(2);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
    assertThat(result.getMeasures())
        .hasSize(1)
        .extracting(MeasureResponseDto::getData)
        .containsExactly(
            List.of(
                createHyperMapResult(
                    USER_TASK_1,
                    new MapResultEntryDto(allVersionsIdentifier, 3000.0, allVersionsDisplayName),
                    new MapResultEntryDto(v1Identifier, 1000.0, v1displayName)),
                createHyperMapResult(
                    USER_TASK_2,
                    new MapResultEntryDto(allVersionsIdentifier, 5000.0, allVersionsDisplayName),
                    new MapResultEntryDto(v1Identifier, null, v1displayName))));
  }

  @Test
  public void
      reportEvaluationWithMultipleProcessDefinitionSourcesAndOverlappingInstancesAcrossAggregationAndDurationTypes() {
    // given
    final ProcessInstanceEngineDto v1Instance =
        engineIntegrationExtension.deployAndStartProcess(getDoubleUserTaskDiagram("definition"));
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(v1Instance.getId(), 1000);
    final ProcessInstanceEngineDto v2Instance =
        engineIntegrationExtension.deployAndStartProcess(getDoubleUserTaskDiagram("definition"));
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(v2Instance.getId(), 5000);
    importAllEngineEntitiesFromScratch();
    ReportDataDefinitionDto v1definition =
        new ReportDataDefinitionDto(
            V_1_IDENTIFIER, v1Instance.getProcessDefinitionKey(), V_1_DISPLAY_NAME);
    v1definition.setVersion("1");
    ReportDataDefinitionDto allVersionsDefinition =
        new ReportDataDefinitionDto(
            ALL_IDENTIFIER, v2Instance.getProcessDefinitionKey(), ALL_VERSIONS_DISPLAY_NAME);
    allVersionsDefinition.setVersion(ALL_VERSIONS);
    final ProcessReportDataDto reportData =
        createReport(List.of(v1definition, allVersionsDefinition));
    reportData
        .getConfiguration()
        .setAggregationTypes(
            new AggregationDto(MAX),
            new AggregationDto(SUM),
            new AggregationDto(PERCENTILE, 50.),
            new AggregationDto(PERCENTILE, 99.));
    reportData.getConfiguration().setUserTaskDurationTimes(TOTAL, WORK);

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
        evaluationResponse = reportClient.evaluateHyperMapReport(reportData);

    // then
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        evaluationResponse.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(2);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
    assertThat(result.getMeasures())
        .hasSize(8)
        .extracting(
            MeasureResponseDto::getAggregationType,
            MeasureResponseDto::getUserTaskDurationTime,
            MeasureResponseDto::getData)
        .contains(
            Tuple.tuple(
                new AggregationDto(MAX), TOTAL, createResultsForAllFlowNodes(5000.0, 1000.0)),
            Tuple.tuple(
                new AggregationDto(SUM), TOTAL, createResultsForAllFlowNodes(6000.0, 1000.0)),
            // We cannot support percentile aggregation types with this distribution as the
            // information is
            // lost on merging
            Tuple.tuple(
                new AggregationDto(PERCENTILE, 50.),
                TOTAL,
                createResultsForAllFlowNodes(null, null)),
            Tuple.tuple(
                new AggregationDto(PERCENTILE, 99.),
                TOTAL,
                createResultsForAllFlowNodes(null, null)));
  }

  private List<HyperMapResultEntryDto> createResultsForAllFlowNodes(
      final Double allVersionsResult, final Double v1versionsResult) {
    return List.of(
        createHyperMapResult(
            USER_TASK_1,
            new MapResultEntryDto(ALL_IDENTIFIER, allVersionsResult, ALL_VERSIONS_DISPLAY_NAME),
            new MapResultEntryDto(V_1_IDENTIFIER, v1versionsResult, V_1_DISPLAY_NAME)),
        createHyperMapResult(
            USER_TASK_2,
            new MapResultEntryDto(ALL_IDENTIFIER, allVersionsResult, ALL_VERSIONS_DISPLAY_NAME),
            new MapResultEntryDto(V_1_IDENTIFIER, v1versionsResult, V_1_DISPLAY_NAME)));
  }

  private HyperMapResultEntryDto createHyperMapResult(
      final String flowNodeId, final MapResultEntryDto... results) {
    return new HyperMapResultEntryDto(flowNodeId, List.of(results), flowNodeId);
  }

  private ProcessReportDataDto createReport(final List<ReportDataDefinitionDto> definitionDtos) {
    final ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(USER_TASK_DUR_GROUP_BY_USER_TASK_BY_PROCESS)
            .build();
    reportData.setDefinitions(definitionDtos);
    return reportData;
  }
}
