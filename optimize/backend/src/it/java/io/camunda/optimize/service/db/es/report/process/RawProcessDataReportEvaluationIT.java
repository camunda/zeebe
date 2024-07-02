/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process;

import static com.google.common.collect.Lists.newArrayList;
import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
import static io.camunda.optimize.dto.optimize.ReportConstants.PAGINATION_DEFAULT_LIMIT;
import static io.camunda.optimize.dto.optimize.ReportConstants.PAGINATION_DEFAULT_OFFSET;
import static io.camunda.optimize.dto.optimize.query.report.single.configuration.TableColumnDto.VARIABLE_PREFIX;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.ComparisonOperator.GREATER_THAN;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.ComparisonOperator.LESS_THAN;
import static io.camunda.optimize.service.db.DatabaseConstants.MAX_RESPONSE_SIZE_LIMIT;
import static io.camunda.optimize.service.db.es.report.command.process.mapping.RawProcessDataResultDtoMapper.OBJECT_VARIABLE_VALUE_PLACEHOLDER;
import static io.camunda.optimize.service.db.es.report.process.single.incident.duration.IncidentDataDeployer.IncidentProcessType.ONE_TASK;
import static io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension.DEFAULT_ENGINE_ALIAS;
import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
import static io.camunda.optimize.util.BpmnModels.FLONODE_NAME;
import static io.camunda.optimize.util.BpmnModels.MERGE_GATEWAY_ID;
import static io.camunda.optimize.util.BpmnModels.SCRIPT_TASK;
import static io.camunda.optimize.util.BpmnModels.SERVICE_TASK_ID_1;
import static io.camunda.optimize.util.BpmnModels.SERVICE_TASK_ID_2;
import static io.camunda.optimize.util.BpmnModels.SPLITTING_GATEWAY_ID;
import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.FlowNodeTotalDurationDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DurationUnit;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.variable.BooleanVariableFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessVisualization;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.VariableFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataCountDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessInstanceDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto;
import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
import io.camunda.optimize.dto.optimize.rest.pagination.PaginationDto;
import io.camunda.optimize.dto.optimize.rest.pagination.PaginationRequestDto;
import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.rest.optimize.dto.VariableDto;
import io.camunda.optimize.service.db.es.report.process.single.incident.duration.IncidentDataDeployer;
import io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex;
import io.camunda.optimize.service.util.ProcessReportDataBuilderHelper;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import io.camunda.optimize.test.util.DateCreationFreezer;
import io.camunda.optimize.util.BpmnModels;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag(OPENSEARCH_PASSING)
public class RawProcessDataReportEvaluationIT extends AbstractProcessDefinitionIT {

  @Test
  public void otherProcessDefinitionsDoNoAffectResult() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();
    deployAndStartSimpleProcess();
    importAllEngineEntitiesFromScratch();

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = createReportAndReturnEvaluationResult(processInstance);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertBasicResultData(evaluationResult, processInstance, 1);
    assertResultInstance(processInstance, result.getData().get(0));
  }

  @Test
  public void reportEvaluationForOneProcessInstance() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();

    importAllEngineEntitiesFromScratch();

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = createReportAndReturnEvaluationResult(processInstance);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertBasicResultData(evaluationResult, processInstance, 1);
    assertResultInstance(processInstance, result.getData().get(0));
  }

  @Test
  public void reportEvaluationForRunningProcessInstance() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleUserTaskProcess();
    importAllEngineEntitiesFromScratch();

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = createReportAndReturnEvaluationResult(processInstance);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertBasicResultData(evaluationResult, processInstance, 1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto = result.getData().get(0);
    assertThat(rawDataProcessInstanceDto.getProcessDefinitionKey())
        .isEqualTo(processInstance.getProcessDefinitionKey());
    assertThat(rawDataProcessInstanceDto.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(rawDataProcessInstanceDto.getStartDate()).isNotNull();
    assertThat(rawDataProcessInstanceDto.getEndDate()).isNull();
    assertThat(rawDataProcessInstanceDto.getDuration()).isNotNull();
    assertThat(rawDataProcessInstanceDto.getEngineName()).isEqualTo(DEFAULT_ENGINE_ALIAS);
    assertThat(rawDataProcessInstanceDto.getBusinessKey()).isEqualTo(BUSINESS_KEY);
    assertThat(rawDataProcessInstanceDto.getVariables()).isNotNull().isEmpty();
  }

  @Test
  public void getFlowNodeDurationsAndNamesForProcessInstanceWhenAllFlowNodeInstancesHaveFinished() {
    // given
    final ProcessInstanceEngineDto processInstance =
        deployAndStartSimpleUserTaskProcessWithFlowNodeNames();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), START_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), USER_TASK_1, 10);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), END_EVENT, 0);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    assertBasicResultData(evaluationResult, processInstance, 1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto =
        evaluationResult.getResult().getData().get(0);

    // then
    assertThat(rawDataProcessInstanceDto.getFlowNodeDurations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                START_EVENT, new FlowNodeTotalDurationDataDto(START_EVENT, 0),
                USER_TASK_1, new FlowNodeTotalDurationDataDto(FLONODE_NAME, 10),
                END_EVENT, new FlowNodeTotalDurationDataDto(END_EVENT, 0)));
  }

  @Test
  public void getFlowNodeDurationsAndNamesForProcessInstanceWhenFlowNodeDurationIsLong() {
    // given
    final ProcessInstanceEngineDto processInstance =
        deployAndStartSimpleUserTaskProcessWithFlowNodeNames();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), START_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(
        processInstance.getId(), USER_TASK_1, Long.MAX_VALUE);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), END_EVENT, 0);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    assertBasicResultData(evaluationResult, processInstance, 1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto =
        evaluationResult.getResult().getData().get(0);

    // then
    assertThat(rawDataProcessInstanceDto.getFlowNodeDurations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                START_EVENT, new FlowNodeTotalDurationDataDto(START_EVENT, 0),
                USER_TASK_1, new FlowNodeTotalDurationDataDto(FLONODE_NAME, Long.MAX_VALUE),
                END_EVENT, new FlowNodeTotalDurationDataDto(END_EVENT, 0)));
  }

  @Test
  public void getFlowNodeDurationsAndNamesForProcessInstanceWhenAllFlowNodesHaveTheSameName() {
    // given
    final ProcessInstanceEngineDto processInstance =
        engineIntegrationExtension.deployAndStartProcess(
            BpmnModels.getSingleUserTaskDiagramWithAllFlowNodesHavingSameNames());
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), START_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), USER_TASK_1, 10);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), END_EVENT, 0);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResultWithUserTask = evaluateRawReportWithDefaultPagination(reportData);
    assertBasicResultData(evaluationResultWithUserTask, processInstance, 1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto =
        evaluationResultWithUserTask.getResult().getData().get(0);

    // then
    assertThat(rawDataProcessInstanceDto.getFlowNodeDurations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                START_EVENT, new FlowNodeTotalDurationDataDto(FLONODE_NAME, 0),
                USER_TASK_1, new FlowNodeTotalDurationDataDto(FLONODE_NAME, 10),
                END_EVENT, new FlowNodeTotalDurationDataDto(FLONODE_NAME, 0)));
  }

  @Test
  public void getFlowNodeDurationsAndNamesForProcessInstanceWhenFlowNodesHaveNoNames() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleUserTaskProcess();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), START_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), USER_TASK_1, 10);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), END_EVENT, 0);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResultWithUserTask = evaluateRawReportWithDefaultPagination(reportData);
    assertBasicResultData(evaluationResultWithUserTask, processInstance, 1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto =
        evaluationResultWithUserTask.getResult().getData().get(0);

    // then the flow node IDs are used instead of names
    assertThat(rawDataProcessInstanceDto.getFlowNodeDurations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                START_EVENT, new FlowNodeTotalDurationDataDto(START_EVENT, 0),
                USER_TASK_1, new FlowNodeTotalDurationDataDto(USER_TASK_1, 10),
                END_EVENT, new FlowNodeTotalDurationDataDto(END_EVENT, 0)));
  }

  @Test
  public void getFlowNodeDurationsAndNamesForProcessInstanceWhenFlowNodeHasNotFinished() {
    // given
    final OffsetDateTime now = dateFreezer().freezeDateAndReturn();
    final OffsetDateTime instanceStartDate = now.minusSeconds(10);
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleUserTaskProcess();
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), START_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeStartDate(
        processInstance.getId(), USER_TASK_1, instanceStartDate);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResultWithUserTask = evaluateRawReportWithDefaultPagination(reportData);
    assertBasicResultData(evaluationResultWithUserTask, processInstance, 1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto =
        evaluationResultWithUserTask.getResult().getData().get(0);

    // then
    assertThat(rawDataProcessInstanceDto.getFlowNodeDurations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                START_EVENT,
                new FlowNodeTotalDurationDataDto(START_EVENT, 0),
                USER_TASK_1,
                new FlowNodeTotalDurationDataDto(
                    USER_TASK_1,
                    now.toInstant().toEpochMilli()
                        - instanceStartDate.toInstant().toEpochMilli())));
  }

  @Test
  public void getFlowNodeDurationsAndNamesForProcessInstanceWhenProcessIsLooping() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartLoopingProcess();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), START_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), END_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance.getId(), SCRIPT_TASK, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(
        processInstance.getId(), SERVICE_TASK_ID_1, 10);
    engineDatabaseExtension.changeFlowNodeTotalDuration(
        processInstance.getId(), MERGE_GATEWAY_ID, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(
        processInstance.getId(), SERVICE_TASK_ID_2, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(
        processInstance.getId(), SPLITTING_GATEWAY_ID, 1);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    assertBasicResultData(evaluationResult, processInstance, 1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto =
        evaluationResult.getResult().getData().get(0);

    // then
    assertThat(rawDataProcessInstanceDto.getFlowNodeDurations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap
                .of( // service tesk 1 loops in the process and that's why its total duration
                    // equates to 20
                    SERVICE_TASK_ID_1,
                    new FlowNodeTotalDurationDataDto(SERVICE_TASK_ID_1, 20),
                    SERVICE_TASK_ID_2,
                    new FlowNodeTotalDurationDataDto(SERVICE_TASK_ID_2, 0),
                    START_EVENT,
                    new FlowNodeTotalDurationDataDto(START_EVENT, 0),
                    END_EVENT,
                    new FlowNodeTotalDurationDataDto(END_EVENT, 0),
                    SCRIPT_TASK,
                    new FlowNodeTotalDurationDataDto(SCRIPT_TASK, 0),
                    MERGE_GATEWAY_ID,
                    new FlowNodeTotalDurationDataDto(MERGE_GATEWAY_ID, 0),
                    // splitting gateway loops in the process and that's why its total duration
                    // equates to 2
                    SPLITTING_GATEWAY_ID,
                    new FlowNodeTotalDurationDataDto(SPLITTING_GATEWAY_ID, 2)));
  }

  @Test
  public void getFlowNodeDurationsAndNamesForDefinitionWithDifferentVersions() {
    // given
    final ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleUserTaskProcess();
    engineIntegrationExtension.finishAllRunningUserTasks();
    final ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.deployAndStartProcess(
            BpmnModels.getSimpleStartEventOnlyDiagram());
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance1.getId(), START_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance1.getId(), END_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance1.getId(), USER_TASK_1, 10);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance2.getId(), FLONODE_NAME, 12);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        createReportForAllDefinitionVersions(processInstance2.getProcessDefinitionKey());
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto =
        evaluationResult.getResult().getData().get(0);

    // then
    final List<FlowNodeTotalDurationDataDto> flowNodeTotalDurationDataDtos =
        evaluationResult.getResult().getData().stream()
            .map(RawDataProcessInstanceDto::getFlowNodeDurations)
            .map(Map::values)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    assertThat(flowNodeTotalDurationDataDtos)
        .containsExactlyInAnyOrder(
            new FlowNodeTotalDurationDataDto(USER_TASK_1, 10),
            new FlowNodeTotalDurationDataDto(START_EVENT, 0),
            new FlowNodeTotalDurationDataDto(END_EVENT, 0),
            new FlowNodeTotalDurationDataDto(FLONODE_NAME, 12));
  }

  @Test
  public void getFlowNodeDurationsAndNamesForProcessInstanceWhenReportHasTwoDefinitions() {
    // given
    final ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleUserTaskProcess();
    engineIntegrationExtension.finishAllRunningUserTasks();
    final ProcessInstanceEngineDto processInstance2 =
        deployAndStartSimpleUserTaskProcessWithFlowNodeNames();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance1.getId(), START_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance1.getId(), USER_TASK_1, 10);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance1.getId(), END_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance2.getId(), START_EVENT, 0);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance2.getId(), USER_TASK_1, 20);
    engineDatabaseExtension.changeFlowNodeTotalDuration(processInstance2.getId(), END_EVENT, 0);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto rawDataReportWithTwoDefinitions =
        createReportWithTwoDefinitions(
            processInstance1.getProcessDefinitionKey(), processInstance2.getProcessDefinitionKey());
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResultWithUserTask =
            evaluateRawReportWithDefaultPagination(rawDataReportWithTwoDefinitions);

    // then
    assertBasicResultDataForMultiDefinitionReport(
        evaluationResultWithUserTask, processInstance1, 2);
    final List<FlowNodeTotalDurationDataDto> flowNodeTotalDurationDataDtos =
        evaluationResultWithUserTask.getResult().getData().stream()
            .map(RawDataProcessInstanceDto::getFlowNodeDurations)
            .map(Map::values)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    assertThat(flowNodeTotalDurationDataDtos)
        .containsExactlyInAnyOrder(
            new FlowNodeTotalDurationDataDto(USER_TASK_1, 20),
            new FlowNodeTotalDurationDataDto(START_EVENT, 0),
            new FlowNodeTotalDurationDataDto(START_EVENT, 0),
            new FlowNodeTotalDurationDataDto(END_EVENT, 0),
            new FlowNodeTotalDurationDataDto(END_EVENT, 0),
            new FlowNodeTotalDurationDataDto(USER_TASK_1, 10));
  }

  @Test
  public void getNumberOfUserTasksForProcessInstance() {
    // given
    final ProcessInstanceEngineDto processInstanceWithUserTask =
        deployAndStartSimpleUserTaskProcess();
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.startProcessInstance(processInstanceWithUserTask.getDefinitionId());
    final ProcessInstanceEngineDto processInstanceWithoutUserTask = deployAndStartSimpleProcess();
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportDataWithUserTask = createReport(processInstanceWithUserTask);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResultWithUserTask =
            evaluateRawReportWithDefaultPagination(reportDataWithUserTask);
    final ProcessReportDataDto reportDataWithoutUserTask =
        createReport(processInstanceWithoutUserTask);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResultWithoutUserTask =
            evaluateRawReportWithDefaultPagination(reportDataWithoutUserTask);

    // then
    assertThat(evaluationResultWithUserTask.getResult().getData())
        .extracting(RawDataProcessInstanceDto::getCounts)
        .extracting(RawDataCountDto::getUserTasks)
        .containsExactlyInAnyOrder(1L, 1L);
    assertThat(evaluationResultWithoutUserTask.getResult().getData())
        .extracting(RawDataProcessInstanceDto::getCounts)
        .extracting(RawDataCountDto::getUserTasks)
        .containsExactly(0L);
  }

  @Test
  public void reportEvaluationByIdForOneProcessInstance() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();

    importAllEngineEntitiesFromScratch();
    final String reportId =
        createAndStoreDefaultReportDefinition(
            processInstance.getProcessDefinitionKey(),
            processInstance.getProcessDefinitionVersion());

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = reportClient.evaluateRawReportById(reportId);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertBasicResultData(evaluationResult, processInstance, 1);
    assertResultInstance(processInstance, result.getData().get(0));
  }

  @Test
  public void reportEvaluationWithSeveralProcessInstances() {
    // given
    final ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcess();

    final ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.startProcessInstance(processInstance1.getDefinitionId());
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance1);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertBasicResultData(evaluationResult, processInstance1, 2);
    final Set<String> expectedProcessInstanceIds = new HashSet<>();
    expectedProcessInstanceIds.add(processInstance1.getId());
    expectedProcessInstanceIds.add(processInstance2.getId());
    for (final RawDataProcessInstanceDto rawDataProcessInstanceDto : result.getData()) {
      assertThat(rawDataProcessInstanceDto.getProcessDefinitionId())
          .isEqualTo(processInstance1.getDefinitionId());
      assertThat(rawDataProcessInstanceDto.getProcessDefinitionKey())
          .isEqualTo(processInstance1.getProcessDefinitionKey());
      final String actualProcessInstanceId = rawDataProcessInstanceDto.getProcessInstanceId();
      assertThat(expectedProcessInstanceIds).contains(actualProcessInstanceId);
      expectedProcessInstanceIds.remove(actualProcessInstanceId);
    }
  }

  @Test
  public void reportEvaluationForNoInstances() {
    // given
    final ProcessDefinitionEngineDto definition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            getSimpleBpmnDiagram("aUniqueDefinitionKey"));
    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(definition.getKey())
            .processDefinitionVersions(Collections.singletonList(ALL_VERSIONS))
            .viewProperty(ViewProperty.RAW_DATA)
            .visualization(ProcessVisualization.TABLE)
            .build();

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = reportClient.evaluateRawReport(reportData, null);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertThat(result.getInstanceCount()).isZero();
    assertThat(result.getPagination()).isNotNull();
  }

  @Test
  public void reportEvaluationOnProcessInstanceWithAllVariableTypes() {
    // given
    final Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "Hello World!");
    variables.put("boolVar", true);
    variables.put("shortVar", (short) 2);
    variables.put("intVar", 2);
    variables.put("longVar", "Hello World!");
    variables.put("dateVar", new Date());

    final ProcessInstanceEngineDto processInstance =
        deployAndStartSimpleProcessWithVariables(variables);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertBasicResultData(evaluationResult, processInstance, 1);

    final RawDataProcessInstanceDto rawDataProcessInstanceDto = result.getData().get(0);
    assertThat(rawDataProcessInstanceDto.getProcessDefinitionId())
        .isEqualTo(processInstance.getDefinitionId());
    rawDataProcessInstanceDto
        .getVariables()
        .forEach(
            (varName, varValue) -> {
              assertThat(variables).containsKey(varName);
              assertThat(variables.get(varName)).isNotNull();
            });
  }

  @Test
  public void reportEvaluationSingleBucketFilteredBySingleTenant() {
    // given
    final String tenantId1 = "tenantId1";
    final String tenantId2 = "tenantId2";
    final List<String> selectedTenants = newArrayList(tenantId1);
    final String processKey =
        deployAndStartMultiTenantSimpleServiceTaskProcess(newArrayList(null, tenantId1, tenantId2));

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(processKey)
            .processDefinitionVersions(Collections.singletonList(ALL_VERSIONS))
            .viewProperty(ViewProperty.RAW_DATA)
            .build();
    reportData.setTenantIds(selectedTenants);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluateRawReportWithDefaultPagination(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo((long) selectedTenants.size());
    assertThat(result.getData())
        .isNotNull()
        .extracting(RawDataProcessInstanceDto::getTenantId)
        .containsAnyElementsOf(selectedTenants);
  }

  @Test
  public void resultShouldBeOrderAccordingToStartDate() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();
    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId());
    engineDatabaseExtension.changeProcessInstanceStartDate(
        processInstanceDto2.getId(), OffsetDateTime.now().minusDays(2));
    final ProcessInstanceEngineDto processInstanceDto3 =
        engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId());
    engineDatabaseExtension.changeProcessInstanceStartDate(
        processInstanceDto3.getId(), OffsetDateTime.now().minusDays(1));
    importAllEngineEntitiesFromScratch();

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = createReportAndReturnEvaluationResult(processInstance);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then the given list should be sorted in ascending order
    final List<RawDataProcessInstanceDto> rawDataList = result.getData();
    assertThat(rawDataList)
        .isNotNull()
        .isSortedAccordingTo(
            Comparator.comparing(RawDataProcessInstanceDto::getStartDate).reversed());
  }

  @Test
  public void testCustomOrderOnProcessInstancePropertyIsApplied() {
    // given
    final ProcessInstanceEngineDto processInstanceDto1 = deployAndStartSimpleProcess();
    engineIntegrationExtension.startProcessInstance(processInstanceDto1.getDefinitionId());
    engineIntegrationExtension.startProcessInstance(processInstanceDto1.getDefinitionId());

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstanceDto1);
    reportData
        .getConfiguration()
        .setSorting(new ReportSortingDto(ProcessInstanceIndex.PROCESS_INSTANCE_ID, SortOrder.DESC));
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final List<RawDataProcessInstanceDto> rawDataList = result.getData();
    assertThat(rawDataList)
        .isNotNull()
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  public void testInvalidSortPropertyNameSilentlyIgnored() {
    // given
    final ProcessInstanceEngineDto processInstanceDto1 = deployAndStartSimpleProcess();

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstanceDto1);
    reportData.getConfiguration().setSorting(new ReportSortingDto("lalalala", SortOrder.ASC));
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final List<RawDataProcessInstanceDto> rawDataList = result.getData();
    assertThat(rawDataList).isNotNull().hasSize(1);
  }

  @Test
  public void testCustomOrderOnProcessInstanceVariableIsApplied() {
    // given
    final ProcessInstanceEngineDto processInstanceDto1 =
        deployAndStartSimpleProcessWithVariables(ImmutableMap.of("intVar", 0));

    final ProcessInstanceEngineDto processInstanceDto3 =
        engineIntegrationExtension.startProcessInstance(
            processInstanceDto1.getDefinitionId(), ImmutableMap.of("intVar", 2));

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(
            processInstanceDto1.getDefinitionId(), ImmutableMap.of("intVar", 1));

    final ProcessInstanceEngineDto processInstanceDto4 =
        engineIntegrationExtension.startProcessInstance(
            processInstanceDto1.getDefinitionId(), ImmutableMap.of("otherVar", 0));

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstanceDto1);
    reportData
        .getConfiguration()
        .setSorting(new ReportSortingDto("variable:intVar", SortOrder.ASC));
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final List<RawDataProcessInstanceDto> rawDataList = result.getData();
    assertThat(rawDataList)
        .isNotNull()
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .containsExactly(
            processInstanceDto1.getId(),
            processInstanceDto2.getId(),
            processInstanceDto3.getId(),
            processInstanceDto4.getId());
  }

  @Test
  public void testInvalidSortVariableNameSilentlyIgnored() {
    // given
    final ProcessInstanceEngineDto processInstanceDto1 = deployAndStartSimpleProcess();

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstanceDto1);
    reportData
        .getConfiguration()
        .setSorting(new ReportSortingDto("variable:lalalala", SortOrder.ASC));
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final List<RawDataProcessInstanceDto> rawDataList = result.getData();
    assertThat(rawDataList).isNotNull().hasSize(1);
  }

  @Test
  public void variablesOfOneProcessInstanceAreAddedToOther() {
    // given
    final Map<String, Object> variables = new HashMap<>();
    variables.put("varName1", "value1");

    final ProcessInstanceEngineDto processInstance =
        deployAndStartSimpleProcessWithVariables(variables);

    variables.clear();
    variables.put("varName2", "value2");
    engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId(), variables);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = createReportAndReturnEvaluationResult(processInstance);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final ProcessReportDataDto resultDataDto = evaluationResult.getReportDefinition().getData();
    assertThat(resultDataDto.getProcessDefinitionKey())
        .isEqualTo(processInstance.getProcessDefinitionKey());
    assertThat(resultDataDto.getDefinitionVersions())
        .containsExactly(processInstance.getProcessDefinitionVersion());
    assertThat(result.getData()).isNotNull().hasSize(2);
    result
        .getData()
        .forEach(
            rawDataProcessInstanceDto1 -> {
              final Map<String, Object> vars = rawDataProcessInstanceDto1.getVariables();
              assertThat(vars.keySet()).hasSize(2);
              assertThat(vars).containsValue("");
              // ensure is ordered
              final List<String> actual = new ArrayList<>(vars.keySet());
              assertThat(actual).isSortedAccordingTo(Comparator.naturalOrder());
            });
  }

  @Test
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void
      variablesOfOneProcessInstanceAreAddedToOtherIncludingVariablesFromInstancesNotOnPage() {
    // given
    final Map<String, Object> variables = new HashMap<>();
    variables.put("varName1", "value1");

    final ProcessInstanceEngineDto processInstance =
        deployAndStartSimpleProcessWithVariables(variables);

    variables.clear();
    variables.put("varName2", "value2");
    engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId(), variables);
    importAllEngineEntitiesFromScratch();

    // when we request the first page of results
    final ProcessReportDataDto reportData = createReport(processInstance);
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setOffset(0);
    paginationDto.setLimit(1);
    AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>> evaluationResult =
        reportClient.evaluateRawReport(reportData, paginationDto);
    ReportResultResponseDto<List<RawDataProcessInstanceDto>> result = evaluationResult.getResult();

    // then both variables appear even though no result on the page has a value for the second
    // variable
    ProcessReportDataDto resultDataDto = evaluationResult.getReportDefinition().getData();
    assertThat(resultDataDto.getProcessDefinitionKey())
        .isEqualTo(processInstance.getProcessDefinitionKey());
    assertThat(resultDataDto.getDefinitionVersions())
        .containsExactly(processInstance.getProcessDefinitionVersion());
    assertThat(result.getData())
        .isNotNull()
        .singleElement()
        .satisfies(
            rawDataProcessInstanceDto1 -> {
              final Map<String, Object> vars = rawDataProcessInstanceDto1.getVariables();
              assertThat(vars.keySet()).hasSize(2);
              assertThat(vars).containsValue("");
              // ensure is ordered alphabetically
              final List<String> actual = new ArrayList<>(vars.keySet());
              assertThat(actual).isSortedAccordingTo(Comparator.naturalOrder());
            });

    // when we request the second page of results
    paginationDto.setOffset(1);
    paginationDto.setLimit(1);
    evaluationResult = reportClient.evaluateRawReport(reportData, paginationDto);
    result = evaluationResult.getResult();

    // then both variables appear even though no result on the page has a value for the first
    // variable
    resultDataDto = evaluationResult.getReportDefinition().getData();
    assertThat(resultDataDto.getProcessDefinitionKey())
        .isEqualTo(processInstance.getProcessDefinitionKey());
    assertThat(resultDataDto.getDefinitionVersions())
        .containsExactly(processInstance.getProcessDefinitionVersion());
    assertThat(result.getData())
        .isNotNull()
        .singleElement()
        .satisfies(
            rawDataProcessInstanceDto1 -> {
              final Map<String, Object> vars = rawDataProcessInstanceDto1.getVariables();
              assertThat(vars.keySet()).hasSize(2);
              assertThat(vars).containsValue("");
              // ensure is ordered alphabetically
              final List<String> actual = new ArrayList<>(vars.keySet());
              assertThat(actual).isSortedAccordingTo(Comparator.naturalOrder());
            });
  }

  @Test
  public void allValuesOfListVariablesAreInResult() {
    // given
    final VariableDto listVariable =
        variablesClient.createListJsonObjectVariableDto(List.of("firstValue", "secondValue"));
    final Map<String, Object> variables = new HashMap<>();
    variables.put("listVariable", listVariable);
    final ProcessInstanceEngineDto processInstance =
        deployAndStartSimpleProcessWithVariables(variables);
    importAllEngineEntitiesFromScratch();

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(createReport(processInstance));
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertThat(result.getData())
        .isNotNull()
        .flatExtracting(RawDataProcessInstanceDto::getVariables)
        .containsExactly(
            Map.of(
                "listVariable", "firstValue, secondValue",
                "listVariable._listSize", "2"));
  }

  @Test
  public void objectVariable_placeholderForObjectAndPropertyVariablesAreIncluded() {
    // given
    final Map<String, Object> objectVar = new HashMap<>();
    objectVar.put("name", "Kermit");
    objectVar.put("age", "50");
    objectVar.put("likes", List.of("MissPiggy", "Optimize"));
    final VariableDto objectVariableDto = variablesClient.createMapJsonObjectVariableDto(objectVar);
    final Map<String, Object> variables = new HashMap<>();
    variables.put("objectVar", objectVariableDto);
    final ProcessInstanceEngineDto processInstance =
        deployAndStartSimpleProcessWithVariables(variables);
    importAllEngineEntitiesFromScratch();

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(createReport(processInstance));
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertThat(result.getData())
        .isNotNull()
        .flatExtracting(RawDataProcessInstanceDto::getVariables)
        .containsExactly(
            Map.of(
                "objectVar.name", "Kermit",
                "objectVar.age", "50",
                "objectVar.likes", "MissPiggy, Optimize",
                "objectVar.likes._listSize", "2",
                "objectVar", OBJECT_VARIABLE_VALUE_PLACEHOLDER));
  }

  @Test
  public void evaluationReturnsOnlyDataToGivenProcessDefinitionId() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();
    deployAndStartSimpleProcess();
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = createReportAndReturnEvaluationResult(processInstance);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final ProcessReportDataDto resultDataDto = evaluationResult.getReportDefinition().getData();
    assertThat(resultDataDto.getProcessDefinitionKey())
        .isEqualTo(processInstance.getProcessDefinitionKey());
    assertThat(resultDataDto.getDefinitionVersions())
        .containsExactly(processInstance.getProcessDefinitionVersion());
    assertThat(result.getData()).isNotNull().hasSize(1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto = result.getData().get(0);
    assertThat(rawDataProcessInstanceDto.getProcessDefinitionId())
        .isEqualTo(processInstance.getDefinitionId());
  }

  // test that basic support for filter is there
  @Test
  public void durationFilterInReport() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();

    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(processInstance);
    reportData.setFilter(
        ProcessFilterBuilder.filter()
            .duration()
            .unit(DurationUnit.DAYS)
            .value((long) 1)
            .operator(GREATER_THAN)
            .add()
            .buildList());
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult1 = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result1 =
        evaluationResult1.getResult();

    // then
    final ProcessReportDataDto resultDataDto1 = evaluationResult1.getReportDefinition().getData();
    assertThat(resultDataDto1.getProcessDefinitionKey())
        .isEqualTo(processInstance.getProcessDefinitionKey());
    assertThat(resultDataDto1.getDefinitionVersions())
        .containsExactly(processInstance.getProcessDefinitionVersion());
    assertThat(resultDataDto1.getView())
        .isNotNull()
        .extracting(ProcessViewDto::getFirstProperty)
        .isEqualTo(ViewProperty.RAW_DATA);
    assertThat(result1.getData()).isNotNull().isEmpty();

    // when
    reportData = createReport(processInstance);
    reportData.setFilter(
        ProcessFilterBuilder.filter()
            .duration()
            .unit(DurationUnit.DAYS)
            .value((long) 1)
            .operator(LESS_THAN)
            .add()
            .buildList());

    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult2 = reportClient.evaluateRawReport(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result2 =
        evaluationResult2.getResult();

    // then
    assertBasicResultData(evaluationResult2, processInstance, 1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto = result2.getData().get(0);
    assertThat(rawDataProcessInstanceDto.getProcessInstanceId()).isEqualTo(processInstance.getId());
  }

  @Test
  public void dateFilterInReport() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();
    final OffsetDateTime past =
        engineIntegrationExtension
            .getHistoricProcessInstance(processInstance.getId())
            .getStartTime();
    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData = createReport(processInstance);
    reportData.setFilter(
        ProcessFilterBuilder.filter()
            .fixedInstanceStartDate()
            .start(null)
            .end(past.minusSeconds(1L))
            .add()
            .buildList());
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult1 = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result1 =
        evaluationResult1.getResult();

    // then
    final ProcessReportDataDto resultDataDto1 = evaluationResult1.getReportDefinition().getData();
    assertThat(resultDataDto1.getProcessDefinitionKey())
        .isEqualTo(processInstance.getProcessDefinitionKey());
    assertThat(resultDataDto1.getDefinitionVersions())
        .containsExactly(processInstance.getProcessDefinitionVersion());
    assertThat(resultDataDto1.getView()).isNotNull();
    assertThat(resultDataDto1.getView().getFirstProperty()).isEqualTo(ViewProperty.RAW_DATA);
    assertThat(result1.getData()).isNotNull();
    assertThat(result1.getData()).isEmpty();

    // when
    reportData = createReport(processInstance);
    reportData.setFilter(
        ProcessFilterBuilder.filter()
            .fixedInstanceStartDate()
            .start(past)
            .end(null)
            .add()
            .buildList());
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult2 = reportClient.evaluateRawReport(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result2 =
        evaluationResult2.getResult();

    // then
    assertBasicResultData(evaluationResult2, processInstance, 1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto = result2.getData().get(0);
    assertThat(rawDataProcessInstanceDto.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(rawDataProcessInstanceDto.getDuration()).isNotNull();
  }

  @EnumSource(SortOrder.class)
  @ParameterizedTest
  public void runningAndCompletedProcessInstancesSortByDuration(final SortOrder order) {
    // given
    final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
    final OffsetDateTime oneDayAgo = now.minusDays(1L);
    final OffsetDateTime twoDaysAgo = now.minusDays(2L);
    final OffsetDateTime threeDaysAgo = now.minusDays(3L);
    final OffsetDateTime oneWeekAgo = now.minusWeeks(1L);
    final OffsetDateTime twoWeeksAgo = now.minusWeeks(2L);
    final ProcessDefinitionEngineDto processDefinition = deploySimpleOneUserTasksDefinition();

    final ProcessInstanceEngineDto completedInstanceOneWeek =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(completedInstanceOneWeek.getId());
    engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
        completedInstanceOneWeek.getId(), twoWeeksAgo, oneWeekAgo);

    final ProcessInstanceEngineDto completedInstanceOneDay =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(completedInstanceOneDay.getId());
    engineDatabaseExtension.changeProcessInstanceStartAndEndDate(
        completedInstanceOneDay.getId(), threeDaysAgo, twoDaysAgo);

    final ProcessInstanceEngineDto runningInstanceOneDay =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineDatabaseExtension.changeProcessInstanceStartDate(
        runningInstanceOneDay.getId(), oneDayAgo);

    final ProcessInstanceEngineDto runningInstanceTwoWeeks =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineDatabaseExtension.changeProcessInstanceStartDate(
        runningInstanceTwoWeeks.getId(), twoWeeksAgo);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(processDefinition.getKey())
            .processDefinitionVersions(
                Collections.singletonList(processDefinition.getVersionAsString()))
            .viewProperty(ViewProperty.RAW_DATA)
            .visualization(ProcessVisualization.TABLE)
            .build();

    reportData
        .getConfiguration()
        .setSorting(new ReportSortingDto(ProcessInstanceIndex.DURATION, order));
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final List<RawDataProcessInstanceDto> rawDataList = result.getData();
    assertThat(rawDataList).isNotNull().hasSize(4);
    assertThat(result.getData())
        .isNotNull()
        .hasSize(4)
        .extracting(RawDataProcessInstanceDto::getDuration)
        .isSortedAccordingTo(
            SortOrder.ASC.equals(order) ? Comparator.naturalOrder() : Comparator.reverseOrder())
        .containsExactlyInAnyOrder(
            now.toInstant().toEpochMilli() - oneDayAgo.toInstant().toEpochMilli(),
            twoDaysAgo.toInstant().toEpochMilli() - threeDaysAgo.toInstant().toEpochMilli(),
            oneWeekAgo.toInstant().toEpochMilli() - twoWeeksAgo.toInstant().toEpochMilli(),
            now.toInstant().toEpochMilli() - twoWeeksAgo.toInstant().toEpochMilli());
  }

  @Test
  public void durationIsSetCorrectlyEvenWhenNotSortingByDuration() {
    // given
    final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
    final OffsetDateTime threeDaysAgo = now.minusDays(3L);
    final OffsetDateTime twoWeeksAgo = now.minusWeeks(2L);
    final ProcessDefinitionEngineDto processDefinition = deploySimpleOneUserTasksDefinition();

    final ProcessInstanceEngineDto runningInstanceOneWeek =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineDatabaseExtension.changeProcessInstanceStartDate(
        runningInstanceOneWeek.getId(), twoWeeksAgo);

    final ProcessInstanceEngineDto runningInstanceOneDay =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineDatabaseExtension.changeProcessInstanceStartDate(
        runningInstanceOneDay.getId(), threeDaysAgo);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(processDefinition.getKey())
            .processDefinitionVersions(
                Collections.singletonList(processDefinition.getVersionAsString()))
            .viewProperty(ViewProperty.RAW_DATA)
            .visualization(ProcessVisualization.TABLE)
            .build();

    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final List<RawDataProcessInstanceDto> rawDataList = result.getData();
    assertThat(rawDataList).isNotNull().hasSize(2);
    assertThat(result.getData())
        .isNotNull()
        .hasSize(2)
        .extracting(RawDataProcessInstanceDto::getDuration)
        .containsExactlyInAnyOrder(
            now.toInstant().toEpochMilli() - threeDaysAgo.toInstant().toEpochMilli(),
            now.toInstant().toEpochMilli() - twoWeeksAgo.toInstant().toEpochMilli());
  }

  @Test
  public void variableFilterInReport() {
    // given
    final Map<String, Object> variables = new HashMap<>();
    variables.put("var", true);
    final ProcessInstanceEngineDto processInstance =
        deployAndStartSimpleProcessWithVariables(variables);

    engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId());
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processInstance);
    reportData.setFilter(createVariableFilter());
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final ProcessReportDataDto resultDataDto = evaluationResult.getReportDefinition().getData();
    assertThat(resultDataDto.getProcessDefinitionKey())
        .isEqualTo(processInstance.getProcessDefinitionKey());
    assertThat(resultDataDto.getDefinitionVersions())
        .containsExactly(processInstance.getProcessDefinitionVersion());
    assertThat(result.getData()).hasSize(1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto = result.getData().get(0);
    assertThat(rawDataProcessInstanceDto.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(rawDataProcessInstanceDto.getDuration()).isNotNull();
  }

  @Test
  public void flowNodeFilterInReport() {
    // given
    final Map<String, Object> variables = new HashMap<>();
    variables.put("goToTask1", true);
    final ProcessDefinitionEngineDto processDefinition = deploySimpleGatewayProcessDefinition();
    final ProcessInstanceEngineDto processInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
    variables.put("goToTask1", false);
    engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(processDefinition.getKey())
            .processDefinitionVersions(
                Collections.singletonList(processDefinition.getVersionAsString()))
            .viewProperty(ViewProperty.RAW_DATA)
            .build();

    final List<ProcessFilterDto<?>> flowNodeFilter =
        ProcessFilterBuilder.filter().executedFlowNodes().id(SERVICE_TASK_ID_1).add().buildList();

    reportData.getFilter().addAll(flowNodeFilter);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    final ProcessReportDataDto resultDataDto = evaluationResult.getReportDefinition().getData();
    assertThat(resultDataDto.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(resultDataDto.getDefinitionVersions())
        .containsExactly(processDefinition.getVersionAsString());
    assertThat(result.getData()).hasSize(1);
    final RawDataProcessInstanceDto rawDataProcessInstanceDto = result.getData().get(0);
    assertThat(rawDataProcessInstanceDto.getProcessInstanceId()).isEqualTo(processInstance.getId());
  }

  @Test
  public void openIncidentCountAndTotalIncidentCountAppearsOnRawDataReport() {
    // given
    final String definitionKey = "definitionKey";
    final String definitionVersion = "1";
    // @formatter:off
    final ProcessInstanceEngineDto firstInstance =
        IncidentDataDeployer.dataDeployer(incidentClient)
            .key(definitionKey)
            .deployProcess(ONE_TASK)
            .startProcessInstance()
            .withOpenIncident()
            .startProcessInstance()
            .withResolvedIncident()
            .executeDeployment()
            .get(0);
    // @formatter:on
    importAllEngineEntitiesFromScratch();
    firstInstance.setProcessDefinitionKey(definitionKey);
    firstInstance.setProcessDefinitionVersion(definitionVersion);

    // when
    final ProcessReportDataDto reportData = createReport(firstInstance);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);

    // then
    assertThat(evaluationResult.getReportDefinition().getData())
        .extracting(
            ProcessReportDataDto::getProcessDefinitionKey,
            ProcessReportDataDto::getDefinitionVersions)
        .containsExactly(definitionKey, List.of(definitionVersion));
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();
    assertThat(result.getData())
        .extracting(RawDataProcessInstanceDto::getCounts)
        .extracting(RawDataCountDto::getOpenIncidents)
        .containsExactlyInAnyOrder(0L, 1L);
  }

  @ParameterizedTest
  @MethodSource("viewLevelFilters")
  public void viewLevelFiltersAllAppliedOnlyToInstances(
      final List<ProcessFilterDto<?>> filtersToApply) {
    // given
    final ProcessDefinitionEngineDto processDefinition =
        deploySimpleServiceTaskProcessAndGetDefinition();
    engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(processDefinition.getKey())
            .processDefinitionVersions(
                Collections.singletonList(processDefinition.getVersionAsString()))
            .viewProperty(ViewProperty.RAW_DATA)
            .build();

    reportData.getFilter().addAll(filtersToApply);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();

    // then
    assertThat(result.getInstanceCount()).isZero();
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2L);
  }

  @Test
  public void testValidationExceptionOnNullDto() {
    // when
    final Response response =
        reportClient.evaluateReportAndReturnResponse((ProcessReportDataDto) null);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void missingProcessDefinition() {
    // when
    final ProcessReportDataDto dataDto =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(null)
            .processDefinitionVersions(null)
            .viewEntity(null)
            .viewProperty(ViewProperty.RAW_DATA)
            .visualization(ProcessVisualization.TABLE)
            .build();

    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void missingViewField() {
    // when
    final ProcessReportDataDto dataDto =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(null)
            .processDefinitionVersions(null)
            .viewEntity(null)
            .viewProperty(null)
            .visualization(ProcessVisualization.TABLE)
            .build();
    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void missingPropertyField() {
    // when
    final ProcessReportDataDto dataDto =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(null)
            .processDefinitionVersions(null)
            .viewEntity(ProcessViewEntity.PROCESS_INSTANCE)
            .viewProperty(null)
            .visualization(ProcessVisualization.TABLE)
            .build();
    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void missingVisualizationField() {
    // when
    final ProcessReportDataDto dataDto =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(null)
            .processDefinitionVersions(null)
            .viewEntity(null)
            .viewProperty(ViewProperty.RAW_DATA)
            .visualization(null)
            .build();

    final Response response = reportClient.evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void addNewVariablesToIncludedColumnsByDefault() {
    // given
    final ProcessInstanceEngineDto processInstanceDto =
        deployAndStartSimpleProcessWithVariables(
            ImmutableMap.of(
                "existingExcludedVar", 1,
                "existingIncludedVar", 1,
                "aNewVar", 1,
                "anotherNewVar", 1));
    importAllEngineEntitiesFromScratch();

    // when we have a report with some existing included and excluded columns
    final ProcessReportDataDto reportData = createReport(processInstanceDto);
    reportData
        .getConfiguration()
        .getTableColumns()
        .getExcludedColumns()
        .add(VARIABLE_PREFIX + "existingExcludedVar");
    reportData
        .getConfiguration()
        .getTableColumns()
        .getIncludedColumns()
        .add(VARIABLE_PREFIX + "existingIncludedVar");
    reportData.getConfiguration().getTableColumns().setIncludeNewVariables(true);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);

    // then the new vars are added in alphabetical order to the included columns
    assertThat(
            evaluationResult
                .getReportDefinition()
                .getData()
                .getConfiguration()
                .getTableColumns()
                .getExcludedColumns())
        .contains(VARIABLE_PREFIX + "existingExcludedVar");
    assertThat(
            evaluationResult
                .getReportDefinition()
                .getData()
                .getConfiguration()
                .getTableColumns()
                .getIncludedColumns())
        .contains(
            VARIABLE_PREFIX + "aNewVar",
            VARIABLE_PREFIX + "anotherNewVar",
            VARIABLE_PREFIX + "existingIncludedVar");
  }

  @Test
  public void addNewVariablesToExcludedColumns() {
    // given
    final ProcessInstanceEngineDto processInstanceDto =
        deployAndStartSimpleProcessWithVariables(
            ImmutableMap.of(
                "existingExcludedVar", 1,
                "existingIncludedVar", 1,
                "aNewVar", 1,
                "anotherNewVar", 1));
    importAllEngineEntitiesFromScratch();

    // when we have a report with some existing included and excluded columns and includeNew false
    final ProcessReportDataDto reportData = createReport(processInstanceDto);
    reportData
        .getConfiguration()
        .getTableColumns()
        .getExcludedColumns()
        .add(VARIABLE_PREFIX + "existingExcludedVar");
    reportData
        .getConfiguration()
        .getTableColumns()
        .getIncludedColumns()
        .add(VARIABLE_PREFIX + "existingIncludedVar");
    reportData.getConfiguration().getTableColumns().setIncludeNewVariables(false);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);

    // then the new vars are added in alphabetical order to the excluded columns
    assertThat(
            evaluationResult
                .getReportDefinition()
                .getData()
                .getConfiguration()
                .getTableColumns()
                .getIncludedColumns())
        .contains(VARIABLE_PREFIX + "existingIncludedVar");
    assertThat(
            evaluationResult
                .getReportDefinition()
                .getData()
                .getConfiguration()
                .getTableColumns()
                .getExcludedColumns())
        .contains(
            VARIABLE_PREFIX + "aNewVar",
            VARIABLE_PREFIX + "anotherNewVar",
            VARIABLE_PREFIX + "existingExcludedVar");
  }

  @Test
  public void removeNonExistentVariableColumns() {
    // given
    final String nonExistentVariable1 = VARIABLE_PREFIX + "nonExistentVariable1";
    final String nonExistentVariable2 = VARIABLE_PREFIX + "nonExistentVariable2";
    final ProcessInstanceEngineDto processInstanceDto =
        deployAndStartSimpleProcessWithVariables(
            ImmutableMap.of(
                "someVar", 1,
                "someOtherVar", 1));
    importAllEngineEntitiesFromScratch();

    // when we have a report with variables columns that no longer exist in the instance data
    final ProcessReportDataDto reportData = createReport(processInstanceDto);
    reportData.getConfiguration().getTableColumns().getExcludedColumns().add(nonExistentVariable1);
    reportData.getConfiguration().getTableColumns().getIncludedColumns().add(nonExistentVariable2);
    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = evaluateRawReportWithDefaultPagination(reportData);

    // then the nonexistent variable columns are removed from the column configurations
    final List<String> allColumns =
        evaluationResult
            .getReportDefinition()
            .getData()
            .getConfiguration()
            .getTableColumns()
            .getExcludedColumns();
    allColumns.addAll(
        evaluationResult
            .getReportDefinition()
            .getData()
            .getConfiguration()
            .getTableColumns()
            .getIncludedColumns());

    assertThat(allColumns).doesNotContain(nonExistentVariable1, nonExistentVariable2);
  }

  @Test
  public void reportEvaluation_pageThroughResults() {
    // given
    final ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcess();
    final String instanceId2 =
        engineIntegrationExtension.startProcessInstance(processInstance1.getDefinitionId()).getId();
    final String instanceId3 =
        engineIntegrationExtension.startProcessInstance(processInstance1.getDefinitionId()).getId();
    final String instanceId4 =
        engineIntegrationExtension.startProcessInstance(processInstance1.getDefinitionId()).getId();
    final String instanceId5 =
        engineIntegrationExtension.startProcessInstance(processInstance1.getDefinitionId()).getId();
    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processInstance1);

    // when we request the first page of results
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setOffset(0);
    paginationDto.setLimit(2);
    AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>> evaluationResult =
        reportClient.evaluateRawReport(reportData, paginationDto);

    // then we get the first page of results, sorted according to their default order
    ReportResultResponseDto<List<RawDataProcessInstanceDto>> result = evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(5L);
    assertThat(result.getData())
        .isNotNull()
        .hasSize(2)
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .containsExactly(instanceId5, instanceId4);
    assertThat(result.getPagination())
        .isEqualTo(PaginationDto.fromPaginationRequest(paginationDto));

    // when we request the next page of results
    paginationDto.setOffset(2);
    paginationDto.setLimit(2);
    evaluationResult = reportClient.evaluateRawReport(reportData, paginationDto);

    // then we get the second page of results
    result = evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(5L);
    assertThat(result.getData())
        .isNotNull()
        .hasSize(2)
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .containsExactly(instanceId3, instanceId2);
    assertThat(result.getPagination())
        .isEqualTo(PaginationDto.fromPaginationRequest(paginationDto));

    // when we request the next page of results
    paginationDto.setOffset(4);
    paginationDto.setLimit(2);
    evaluationResult = reportClient.evaluateRawReport(reportData, paginationDto);

    // then we get the last page of results, which contains less results than the limit
    result = evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(5L);
    assertThat(result.getData())
        .isNotNull()
        .hasSize(1)
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .containsExactly(processInstance1.getId());
    assertThat(result.getPagination())
        .isEqualTo(PaginationDto.fromPaginationRequest(paginationDto));
  }

  @Test
  public void reportEvaluation_negativeOffset() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleServiceTaskProcess();
    final ProcessReportDataDto reportData = createReport(processInstance);

    // when
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setLimit(20);
    paginationDto.setOffset(-1);

    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildEvaluateSingleUnsavedReportRequestWithPagination(reportData, paginationDto)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, MAX_RESPONSE_SIZE_LIMIT + 1})
  public void reportEvaluation_limitOutOfAcceptableBounds(final int limit) {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleServiceTaskProcess();
    final ProcessReportDataDto reportData = createReport(processInstance);

    // when
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setOffset(0);
    paginationDto.setLimit(limit);

    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildEvaluateSingleUnsavedReportRequestWithPagination(reportData, paginationDto)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void reportEvaluation_limitedToZeroResults() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();
    engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId());

    importAllEngineEntitiesFromScratch();
    final ProcessReportDataDto reportData = createReport(processInstance);

    // when
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setOffset(0);
    paginationDto.setLimit(0);

    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = reportClient.evaluateRawReport(reportData, paginationDto);

    // then
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(2L);
    assertThat(result.getData()).isNotNull().isEmpty();
    assertThat(result.getPagination())
        .isEqualTo(PaginationDto.fromPaginationRequest(paginationDto));
  }

  @Test
  public void reportEvaluation_offsetGreaterThanMaxResultReturnsEmptyData() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();
    engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId());

    importAllEngineEntitiesFromScratch();
    final ProcessReportDataDto reportData = createReport(processInstance);

    // when
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setLimit(20);
    paginationDto.setOffset(10);

    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = reportClient.evaluateRawReport(reportData, paginationDto);

    // then
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(2L);
    assertThat(result.getData()).isNotNull().isEmpty();
    assertThat(result.getPagination())
        .isEqualTo(PaginationDto.fromPaginationRequest(paginationDto));
  }

  @Test
  public void reportEvaluation_defaultOffset() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();
    final ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId());

    importAllEngineEntitiesFromScratch();
    final ProcessReportDataDto reportData = createReport(processInstance);

    // when
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setLimit(1);

    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = reportClient.evaluateRawReport(reportData, paginationDto);

    // then
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(2L);
    assertThat(result.getData())
        .hasSize(1)
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .containsExactly(processInstance2.getId());
    assertThat(result.getPagination().getLimit()).isEqualTo(1);
    assertThat(result.getPagination().getOffset()).isEqualTo(PAGINATION_DEFAULT_OFFSET);
  }

  @Test
  public void reportEvaluation_defaultLimit() {
    // given
    final ProcessInstanceEngineDto processInstance = deployAndStartSimpleProcess();
    IntStream.range(0, 29)
        .forEach(
            i ->
                engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId()));

    importAllEngineEntitiesFromScratch();
    final ProcessReportDataDto reportData = createReport(processInstance);

    // when
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setOffset(0);

    final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
        evaluationResult = reportClient.evaluateRawReport(reportData, paginationDto);

    // then
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(30L);
    assertThat(result.getData()).hasSize(20);
    assertThat(result.getPagination().getLimit()).isEqualTo(PAGINATION_DEFAULT_LIMIT);
    assertThat(result.getPagination().getOffset()).isZero();
  }

  @Test
  public void reportEvaluation_pageThroughSavedReportResults() {
    // given
    final ProcessInstanceEngineDto processInstance1 = deployAndStartSimpleProcess();
    final String instanceId2 =
        engineIntegrationExtension.startProcessInstance(processInstance1.getDefinitionId()).getId();
    final String instanceId3 =
        engineIntegrationExtension.startProcessInstance(processInstance1.getDefinitionId()).getId();
    final String instanceId4 =
        engineIntegrationExtension.startProcessInstance(processInstance1.getDefinitionId()).getId();
    final String instanceId5 =
        engineIntegrationExtension.startProcessInstance(processInstance1.getDefinitionId()).getId();
    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processInstance1);
    final String reportId = reportClient.createSingleProcessReport(reportData);

    // when we request the first page of results
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setOffset(0);
    paginationDto.setLimit(2);
    AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>> evaluationResult =
        evaluateSavedRawDataProcessReport(reportId, paginationDto);

    // then we get the first page of results, sorted according to their default order
    ReportResultResponseDto<List<RawDataProcessInstanceDto>> result = evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(5L);
    assertThat(result.getData())
        .isNotNull()
        .hasSize(2)
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .containsExactly(instanceId5, instanceId4);
    assertThat(result.getPagination())
        .isEqualTo(PaginationDto.fromPaginationRequest(paginationDto));

    // when we request the next page of results
    paginationDto.setOffset(2);
    paginationDto.setLimit(2);
    evaluationResult = evaluateSavedRawDataProcessReport(reportId, paginationDto);

    // then we get the second page of results
    result = evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(5L);
    assertThat(result.getData())
        .isNotNull()
        .hasSize(2)
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .containsExactly(instanceId3, instanceId2);
    assertThat(result.getPagination())
        .isEqualTo(PaginationDto.fromPaginationRequest(paginationDto));

    // when we request the next page of results
    paginationDto.setOffset(4);
    paginationDto.setLimit(2);
    evaluationResult = evaluateSavedRawDataProcessReport(reportId, paginationDto);

    // then we get the last page of results, which contains less results than the limit
    result = evaluationResult.getResult();
    assertThat(result.getInstanceCount()).isEqualTo(5L);
    assertThat(result.getData())
        .isNotNull()
        .hasSize(1)
        .extracting(RawDataProcessInstanceDto::getProcessInstanceId)
        .containsExactly(processInstance1.getId());
    assertThat(result.getPagination())
        .isEqualTo(PaginationDto.fromPaginationRequest(paginationDto));
  }

  private AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
      evaluateSavedRawDataProcessReport(
          final String reportId, final PaginationRequestDto paginationDto) {
    return embeddedOptimizeExtension
        .getRequestExecutor()
        .buildEvaluateSavedReportRequest(reportId, paginationDto)
        .execute(new TypeReference<>() {});
  }

  private AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
      evaluateRawReportWithDefaultPagination(final ProcessReportDataDto reportData) {
    final PaginationRequestDto paginationDto = new PaginationRequestDto();
    paginationDto.setOffset(0);
    paginationDto.setLimit(20);
    return reportClient.evaluateRawReport(reportData, paginationDto);
  }

  private void assertBasicResultData(
      final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>> result,
      final ProcessInstanceEngineDto instance,
      final int expectedDataSize) {
    final ProcessReportDataDto resultDataDto = result.getReportDefinition().getData();
    assertThat(resultDataDto.getProcessDefinitionKey())
        .isEqualTo(instance.getProcessDefinitionKey());
    assertThat(resultDataDto.getDefinitionVersions())
        .containsExactly(instance.getProcessDefinitionVersion());
    assertThat(resultDataDto.getView())
        .isNotNull()
        .extracting(ProcessViewDto::getFirstProperty)
        .isEqualTo(ViewProperty.RAW_DATA);
    assertThat(result.getResult().getData()).isNotNull().hasSize(expectedDataSize);
  }

  private void assertBasicResultDataForMultiDefinitionReport(
      final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>> result,
      final ProcessInstanceEngineDto instance,
      final int expectedDataSize) {
    final ProcessReportDataDto resultDataDto = result.getReportDefinition().getData();
    assertThat(resultDataDto.getProcessDefinitionKey())
        .isEqualTo(instance.getProcessDefinitionKey());
    assertThat(resultDataDto.getDefinitionVersions()).containsExactly(ALL_VERSIONS);
    assertThat(resultDataDto.getView())
        .isNotNull()
        .extracting(ProcessViewDto::getFirstProperty)
        .isEqualTo(ViewProperty.RAW_DATA);
    assertThat(result.getResult().getData()).isNotNull().hasSize(expectedDataSize);
  }

  private void assertResultInstance(
      final ProcessInstanceEngineDto expectedInstance,
      final RawDataProcessInstanceDto resultInstance) {
    assertThat(resultInstance.getProcessDefinitionKey())
        .isEqualTo(expectedInstance.getProcessDefinitionKey());
    assertThat(resultInstance.getProcessInstanceId()).isEqualTo(expectedInstance.getId());
    assertThat(resultInstance.getStartDate()).isNotNull();
    assertThat(resultInstance.getEndDate()).isNotNull();
    assertThat(resultInstance.getDuration()).isNotNull();
    assertThat(resultInstance.getEngineName()).isEqualTo(DEFAULT_ENGINE_ALIAS);
    assertThat(resultInstance.getBusinessKey()).isEqualTo(BUSINESS_KEY);
    assertThat(resultInstance.getVariables()).isNotNull().isEmpty();
    assertThat(resultInstance.getCounts().getOpenIncidents()).isZero();
    assertThat(resultInstance.getCounts().getUserTasks()).isZero();
  }

  private String createAndStoreDefaultReportDefinition(
      final String processDefinitionKey, final String processDefinitionVersion) {
    final ProcessReportDataDto reportData =
        new ProcessReportDataBuilderHelper()
            .processDefinitionKey(processDefinitionKey)
            .processDefinitionVersions(Collections.singletonList(processDefinitionVersion))
            .viewProperty(ViewProperty.RAW_DATA)
            .visualization(ProcessVisualization.TABLE)
            .build();

    return createNewReport(reportData);
  }

  private ProcessReportDataDto createReport(final ProcessInstanceEngineDto processInstance) {
    return new ProcessReportDataBuilderHelper()
        .processDefinitionKey(processInstance.getProcessDefinitionKey())
        .processDefinitionVersions(
            Collections.singletonList(processInstance.getProcessDefinitionVersion()))
        .viewProperty(ViewProperty.RAW_DATA)
        .visualization(ProcessVisualization.TABLE)
        .build();
  }

  private List<ProcessFilterDto<?>> createVariableFilter() {
    final BooleanVariableFilterDataDto data =
        new BooleanVariableFilterDataDto("var", Collections.singletonList(true));

    final VariableFilterDto variableFilterDto = new VariableFilterDto();
    variableFilterDto.setData(data);
    variableFilterDto.setFilterLevel(FilterApplicationLevel.INSTANCE);
    return Collections.singletonList(variableFilterDto);
  }

  private ProcessInstanceEngineDto deployAndStartSimpleUserTaskProcessWithFlowNodeNames() {
    return engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagramWithFlowNodeNames());
  }

  private ProcessReportDataDto createReportWithTwoDefinitions(
      final String processDefinitionKey1, final String processDefinitionKey2) {
    return TemplatedProcessReportDataBuilder.createReportData()
        .setReportDataType(ProcessReportDataType.RAW_DATA)
        .definitions(
            List.of(
                new ReportDataDefinitionDto(processDefinitionKey1),
                new ReportDataDefinitionDto(processDefinitionKey2)))
        .build();
  }

  private AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>>
      createReportAndReturnEvaluationResult(
          final ProcessInstanceEngineDto processInstanceEngineDto) {
    final ProcessReportDataDto reportData = createReport(processInstanceEngineDto);
    return evaluateRawReportWithDefaultPagination(reportData);
  }

  private ProcessReportDataDto createReportForAllDefinitionVersions(final String definitionKey) {
    return new ProcessReportDataBuilderHelper()
        .processDefinitionKey(definitionKey)
        .processDefinitionVersions(Collections.singletonList(ALL_VERSIONS))
        .viewProperty(ViewProperty.RAW_DATA)
        .visualization(ProcessVisualization.TABLE)
        .build();
  }
}
