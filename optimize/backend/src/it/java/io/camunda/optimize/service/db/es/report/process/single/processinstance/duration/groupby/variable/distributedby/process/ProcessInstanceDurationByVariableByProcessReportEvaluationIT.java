/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.processinstance.duration.groupby.variable.distributedby.process;

import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.AVERAGE;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.MAX;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.MIN;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.PERCENTILE;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType.SUM;
import static io.camunda.optimize.util.BpmnModels.getSingleServiceTaskProcess;
import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
import static io.camunda.optimize.util.BpmnModels.getTwoServiceTasksProcess;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedByType;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.VariableGroupByDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.util.IdGenerator;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import io.camunda.optimize.test.util.DateCreationFreezer;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

public class ProcessInstanceDurationByVariableByProcessReportEvaluationIT
    extends AbstractPlatformIT {

  private static final String STRING_VAR = "stringVar";

  @Test
  public void reportEvaluationWithSingleProcessDefinitionSource() {
    // given
    final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getTwoServiceTasksProcess("aProcess"),
            Collections.singletonMap(STRING_VAR, "aStringValue"));
    changeInstanceDuration(processInstanceDto, now.minusSeconds(5), now);
    importAllEngineEntitiesFromScratch();
    final String processDisplayName = "processDisplayName";
    final String processIdentifier = IdGenerator.getNextId();
    ReportDataDefinitionDto definition =
        new ReportDataDefinitionDto(
            processIdentifier, processInstanceDto.getProcessDefinitionKey(), processDisplayName);
    final ProcessReportDataDto reportData =
        createReport(List.of(definition), STRING_VAR, VariableType.STRING);

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
        evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        evaluationResponse.getResult();
    final ProcessReportDataDto resultReportDataDto =
        evaluationResponse.getReportDefinition().getData();

    // then
    assertThat(resultReportDataDto.getProcessDefinitionKey())
        .isEqualTo(processInstanceDto.getProcessDefinitionKey());
    assertThat(resultReportDataDto.getDefinitionVersions())
        .containsExactly(definition.getVersions().get(0));
    assertThat(resultReportDataDto.getView()).isNotNull();
    assertThat(resultReportDataDto.getView().getEntity())
        .isEqualTo(ProcessViewEntity.PROCESS_INSTANCE);
    assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.DURATION);
    assertThat(resultReportDataDto.getGroupBy().getType()).isEqualTo(ProcessGroupByType.VARIABLE);
    assertThat(resultReportDataDto.getDistributedBy().getType())
        .isEqualTo(DistributedByType.PROCESS);
    final VariableGroupByDto variableGroupByDto =
        (VariableGroupByDto) resultReportDataDto.getGroupBy();
    assertThat(variableGroupByDto.getValue().getName()).isEqualTo(STRING_VAR);
    assertThat(variableGroupByDto.getValue().getType()).isEqualTo(VariableType.STRING);

    assertThat(result.getInstanceCount()).isEqualTo(1);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(1);
    assertThat(result.getMeasures())
        .hasSize(1)
        .extracting(
            MeasureResponseDto::getProperty,
            MeasureResponseDto::getAggregationType,
            MeasureResponseDto::getData)
        .hasSize(1)
        .containsExactly(
            Tuple.tuple(
                ViewProperty.DURATION,
                new AggregationDto(AVERAGE),
                List.of(
                    createHyperMapResult(
                        "aStringValue",
                        new MapResultEntryDto(processIdentifier, 5000.0, processDisplayName)))));
  }

  @Test
  public void reportEvaluationWithMultipleProcessDefinitionSources() {
    // given
    final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
    final ProcessInstanceEngineDto firstInstance =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getTwoServiceTasksProcess("first"),
            Collections.singletonMap(STRING_VAR, "aStringValue"));
    changeInstanceDuration(firstInstance, now.minusSeconds(1), now);
    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getSingleUserTaskDiagram("second"), Collections.singletonMap(STRING_VAR, "aDiffValue"));
    engineIntegrationExtension.finishAllRunningUserTasks();
    changeInstanceDuration(secondInstance, now.minusSeconds(5), now);
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
    final ProcessReportDataDto reportData =
        createReport(List.of(firstDefinition, secondDefinition), STRING_VAR, VariableType.STRING);

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
        evaluationResponse = reportClient.evaluateHyperMapReport(reportData);
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        evaluationResponse.getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo(2);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
    assertThat(result.getMeasures())
        .hasSize(1)
        .extracting(MeasureResponseDto::getData)
        .containsExactly(
            List.of(
                createHyperMapResult(
                    "aDiffValue",
                    new MapResultEntryDto(firstIdentifier, null, firstDisplayName),
                    new MapResultEntryDto(secondIdentifier, 5000.0, secondDisplayName)),
                createHyperMapResult(
                    "aStringValue",
                    new MapResultEntryDto(firstIdentifier, 1000.0, firstDisplayName),
                    new MapResultEntryDto(secondIdentifier, null, secondDisplayName))));
  }

  @Test
  public void reportEvaluationWithMultipleProcessDefinitionSourcesAndOverlappingInstances() {
    // given
    final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
    final ProcessInstanceEngineDto v1instance =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getSingleServiceTaskProcess("definition"),
            Collections.singletonMap(STRING_VAR, "aStringValue"));
    changeInstanceDuration(v1instance, now.minusSeconds(1), now);
    final ProcessInstanceEngineDto v2instance =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getSingleUserTaskDiagram("definition"),
            Collections.singletonMap(STRING_VAR, "aStringValue"));
    engineIntegrationExtension.finishAllRunningUserTasks();
    changeInstanceDuration(v2instance, now.minusSeconds(5), now);
    importAllEngineEntitiesFromScratch();

    final String v1displayName = "v1";
    final String allVersionsDisplayName = "all";
    final String v1Identifier = "v1Identifier";
    final String allVersionsIdentifier = "allIdentifier";
    ReportDataDefinitionDto v1definition =
        new ReportDataDefinitionDto(
            v1Identifier, v1instance.getProcessDefinitionKey(), v1displayName);
    v1definition.setVersion("1");
    ReportDataDefinitionDto allVersionsDefinition =
        new ReportDataDefinitionDto(
            allVersionsIdentifier, v2instance.getProcessDefinitionKey(), allVersionsDisplayName);
    allVersionsDefinition.setVersion(ALL_VERSIONS);

    // when
    final ProcessReportDataDto reportData =
        createReport(List.of(v1definition, allVersionsDefinition), STRING_VAR, VariableType.STRING);
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
                    "aStringValue",
                    new MapResultEntryDto(allVersionsIdentifier, 3000.0, allVersionsDisplayName),
                    new MapResultEntryDto(v1Identifier, 1000.0, v1displayName))));
  }

  @Test
  public void
      reportEvaluationWithMultipleProcessDefinitionSourcesAndOverlappingInstancesAcrossAggregations() {
    // given
    final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
    final ProcessInstanceEngineDto v1instance =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getSingleServiceTaskProcess("definition"),
            Collections.singletonMap(STRING_VAR, "aStringValue"));
    changeInstanceDuration(v1instance, now.minusSeconds(1), now);
    final ProcessInstanceEngineDto v2instance =
        engineIntegrationExtension.deployAndStartProcessWithVariables(
            getSingleUserTaskDiagram("definition"),
            Collections.singletonMap(STRING_VAR, "aStringValue"));
    engineIntegrationExtension.finishAllRunningUserTasks();
    changeInstanceDuration(v2instance, now.minusSeconds(5), now);
    importAllEngineEntitiesFromScratch();

    final String v1displayName = "v1";
    final String allVersionsDisplayName = "all";
    final String v1Identifier = "v1Identifier";
    final String allVersionsIdentifier = "allIdentifier";
    ReportDataDefinitionDto v1definition =
        new ReportDataDefinitionDto(
            v1Identifier, v1instance.getProcessDefinitionKey(), v1displayName);
    v1definition.setVersion("1");
    ReportDataDefinitionDto allVersionsDefinition =
        new ReportDataDefinitionDto(
            allVersionsIdentifier, v2instance.getProcessDefinitionKey(), allVersionsDisplayName);
    allVersionsDefinition.setVersion(ALL_VERSIONS);

    // when
    final ProcessReportDataDto reportData =
        createReport(List.of(v1definition, allVersionsDefinition), STRING_VAR, VariableType.STRING);
    reportData
        .getConfiguration()
        .setAggregationTypes(
            new AggregationDto(MAX),
            new AggregationDto(MIN),
            new AggregationDto(AVERAGE),
            new AggregationDto(SUM),
            new AggregationDto(PERCENTILE, 50.),
            new AggregationDto(PERCENTILE, 99.));
    final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
        evaluationResponse = reportClient.evaluateHyperMapReport(reportData);

    // then
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        evaluationResponse.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(2);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2);
    assertThat(result.getMeasures())
        .hasSize(6)
        .extracting(MeasureResponseDto::getAggregationType, MeasureResponseDto::getData)
        .containsExactly(
            Tuple.tuple(
                new AggregationDto(MAX),
                List.of(
                    createHyperMapResult(
                        "aStringValue",
                        new MapResultEntryDto(
                            allVersionsIdentifier, 5000.0, allVersionsDisplayName),
                        new MapResultEntryDto(v1Identifier, 1000.0, v1displayName)))),
            Tuple.tuple(
                new AggregationDto(MIN),
                List.of(
                    createHyperMapResult(
                        "aStringValue",
                        new MapResultEntryDto(
                            allVersionsIdentifier, 1000.0, allVersionsDisplayName),
                        new MapResultEntryDto(v1Identifier, 1000.0, v1displayName)))),
            Tuple.tuple(
                new AggregationDto(AVERAGE),
                List.of(
                    createHyperMapResult(
                        "aStringValue",
                        new MapResultEntryDto(
                            allVersionsIdentifier, 3000.0, allVersionsDisplayName),
                        new MapResultEntryDto(v1Identifier, 1000.0, v1displayName)))),
            Tuple.tuple(
                new AggregationDto(SUM),
                List.of(
                    createHyperMapResult(
                        "aStringValue",
                        new MapResultEntryDto(
                            allVersionsIdentifier, 6000.0, allVersionsDisplayName),
                        new MapResultEntryDto(v1Identifier, 1000.0, v1displayName)))),
            // We cannot support percentile aggregation types with this distribution as the
            // information is
            // lost on merging
            Tuple.tuple(
                new AggregationDto(PERCENTILE, 50.),
                List.of(
                    createHyperMapResult(
                        "aStringValue",
                        new MapResultEntryDto(allVersionsIdentifier, null, allVersionsDisplayName),
                        new MapResultEntryDto(v1Identifier, null, v1displayName)))),
            Tuple.tuple(
                new AggregationDto(PERCENTILE, 99.),
                List.of(
                    createHyperMapResult(
                        "aStringValue",
                        new MapResultEntryDto(allVersionsIdentifier, null, allVersionsDisplayName),
                        new MapResultEntryDto(v1Identifier, null, v1displayName)))));
  }

  private void changeInstanceDuration(
      final ProcessInstanceEngineDto processInstance,
      final OffsetDateTime startDate,
      final OffsetDateTime endDate) {
    engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
        processInstance.getId(), startDate, endDate);
  }

  private HyperMapResultEntryDto createHyperMapResult(
      final String flowNodeId, final MapResultEntryDto... results) {
    return new HyperMapResultEntryDto(flowNodeId, List.of(results), flowNodeId);
  }

  private ProcessReportDataDto createReport(
      final List<ReportDataDefinitionDto> definitionDtos,
      final String variableName,
      final VariableType variableType) {
    final ProcessReportDataDto report =
        TemplatedProcessReportDataBuilder.createReportData()
            .setTenantIds(Collections.singletonList(null))
            .setVariableName(variableName)
            .setVariableType(variableType)
            .setReportDataType(ProcessReportDataType.PROC_INST_DUR_GROUP_BY_VARIABLE_BY_PROCESS)
            .build();
    report.setDefinitions(definitionDtos);
    return report;
  }
}
