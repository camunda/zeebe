/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.usertask.frequency.groupby.candidategroup;

import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.IN;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.NOT_IN;
import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_KEY;
import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_LABEL;
import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_VALUE;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.service.db.es.report.command.modules.distributed_by.process.identity.ProcessDistributedByIdentity.DISTRIBUTE_BY_IDENTITY_MISSING_KEY;
import static io.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurationsDefaultAggr;
import static io.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.ReportConstants;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
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
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.report.process.AbstractProcessDefinitionIT;
import io.camunda.optimize.service.db.es.report.util.MapResultAsserter;
import io.camunda.optimize.service.db.es.report.util.MapResultUtil;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import io.camunda.optimize.util.BpmnModels;
import io.camunda.optimize.util.SuppressionConstants;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.assertj.core.groups.Tuple;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UserTaskFrequencyByCandidateGroupReportEvaluationIT
    extends AbstractProcessDefinitionIT {

  private static final String PROCESS_DEFINITION_KEY = "123";

  @BeforeEach
  public void init() {
    engineIntegrationExtension.createGroup(FIRST_CANDIDATE_GROUP_ID, FIRST_CANDIDATE_GROUP_NAME);
    engineIntegrationExtension.createGroup(SECOND_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_NAME);
  }

  @Test
  @SuppressWarnings(SuppressionConstants.UNCHECKED_CAST)
  public void reportEvaluationForOneProcessInstance() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto);

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
    assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.FREQUENCY);

    final ReportResultResponseDto<List<MapResultEntryDto>> result = evaluationResponse.getResult();
    assertThat(result.getFirstMeasureData()).isNotNull();
    assertThat(result.getFirstMeasureData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue, MapResultEntryDto::getLabel)
        .containsExactly(1., FIRST_CANDIDATE_GROUP_NAME);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), SECOND_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue, MapResultEntryDto::getLabel)
        .containsExactly(1., SECOND_CANDIDATE_GROUP_NAME);
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  @SuppressWarnings(SuppressionConstants.UNCHECKED_CAST)
  public void reportEvaluationForOneProcessInstance_whenCandidateCacheEmptyLabelEqualsKey() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
    engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks();

    importAllEngineEntitiesFromScratch();

    // cache is empty
    embeddedOptimizeExtension.getUserTaskIdentityCache().resetCache();

    final ProcessReportDataDto reportData = createReport(processDefinition);

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>> evaluationResponse =
        reportClient.evaluateMapReport(reportData);

    // then
    final ReportResultResponseDto<List<MapResultEntryDto>> result = evaluationResponse.getResult();
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue, MapResultEntryDto::getLabel)
        .containsExactly(
            calculateExpectedValueGivenDurationsDefaultAggr(1.), FIRST_CANDIDATE_GROUP_ID);
  }

  @Test
  public void reportEvaluationForOneProcessInstanceWithUnassignedTasks() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto);

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
    assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.FREQUENCY);

    final ReportResultResponseDto<List<MapResultEntryDto>> result = evaluationResponse.getResult();
    assertThat(result.getFirstMeasureData()).isNotNull();
    assertThat(result.getFirstMeasureData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(2L);
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
    assertThat(
            MapResultUtil.getEntryForKey(
                result.getFirstMeasureData(), DISTRIBUTE_BY_IDENTITY_MISSING_KEY))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  public void reportEvaluationForMultipleCandidateGroups() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // finish first task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    // finish second task with
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());

    importAllEngineEntitiesFromScratch();

    final ProcessReportDataDto reportData = createReport(processDefinition);

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>> evaluationResponse =
        reportClient.evaluateMapReport(reportData);

    // then
    final ProcessReportDataDto resultReportDataDto =
        evaluationResponse.getReportDefinition().getData();
    final ReportResultResponseDto<List<MapResultEntryDto>> result = evaluationResponse.getResult();
    assertThat(result.getFirstMeasureData()).isNotNull();
    assertThat(result.getFirstMeasureData()).hasSize(2);
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(2.);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), SECOND_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  public void reportEvaluationForSeveralProcessInstances() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto2);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(3);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(3L);
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(2.);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), SECOND_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
    assertThat(
            MapResultUtil.getEntryForKey(
                result.getFirstMeasureData(), DISTRIBUTE_BY_IDENTITY_MISSING_KEY))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
    assertThat(result.getInstanceCount()).isEqualTo(2L);
  }

  @Test
  public void reportEvaluationForSeveralProcessDefinitions() {
    // given
    final String key1 = "key1";
    final String key2 = "key2";

    final ProcessDefinitionEngineDto processDefinition1 =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            BpmnModels.getSingleUserTaskDiagram(key1, USER_TASK_1));
    final ProcessInstanceEngineDto processInstance1 =
        engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(
        processInstance1.getId(), FIRST_CANDIDATE_GROUP_ID);
    final ProcessDefinitionEngineDto processDefinition2 =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            BpmnModels.getSingleUserTaskDiagram(key2, USER_TASK_2));
    final ProcessInstanceEngineDto processInstance2 =
        engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(
        processInstance2.getId(), SECOND_CANDIDATE_GROUP_ID);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition1);
    reportData.getDefinitions().add(createReportDataDefinitionDto(key2));
    final AuthorizedProcessReportEvaluationResponseDto<List<MapResultEntryDto>> evaluationResponse =
        reportClient.evaluateMapReport(reportData);

    // then
    final ReportResultResponseDto<List<MapResultEntryDto>> actualResult =
        evaluationResponse.getResult();
    // @formatter:off
    MapResultAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.FREQUENCY)
        .groupedByContains(FIRST_CANDIDATE_GROUP_ID, 1., FIRST_CANDIDATE_GROUP_NAME)
        .groupedByContains(SECOND_CANDIDATE_GROUP_ID, 1., SECOND_CANDIDATE_GROUP_NAME)
        .doAssert(actualResult);
    // @formatter:on
  }

  @Test
  public void testCustomOrderOnResultKeyIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto2);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_KEY, SortOrder.DESC));
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
    assertThat(resultData).hasSize(3);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(3L);
    final List<String> resultKeys =
        resultData.stream()
            .map(entry -> entry.getKey().toLowerCase(Locale.ENGLISH))
            .collect(Collectors.toList());
    // expect descending order ignoring case
    assertThat(resultKeys).isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  public void testCustomOrderOnResultLabelIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto2);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_LABEL, SortOrder.DESC));
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
    assertThat(resultData).hasSize(3);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(3L);
    final List<String> resultLabels =
        resultData.stream()
            .map(entry -> entry.getLabel().toLowerCase(Locale.ENGLISH))
            .collect(Collectors.toList());
    // expect descending order ignoring case
    assertThat(resultLabels).isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  public void testCustomOrderOnResultValueIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto2);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_VALUE, SortOrder.ASC));
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(3);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(3L);
    assertCorrectValueOrdering(result);
  }

  @Test
  public void otherProcessDefinitionsDoNotInfluenceResult() {
    // given
    final ProcessDefinitionEngineDto processDefinition1 = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto1);
    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
    finishTwoUserTasksOneWithFirstAndSecondGroup(processInstanceDto2);

    final ProcessDefinitionEngineDto processDefinition2 = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto3 =
        engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
    finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(processInstanceDto3);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData1 = createReport(processDefinition1);
    final ReportResultResponseDto<List<MapResultEntryDto>> result1 =
        reportClient.evaluateMapReport(reportData1).getResult();
    final ProcessReportDataDto reportData2 = createReport(processDefinition2);
    final ReportResultResponseDto<List<MapResultEntryDto>> result2 =
        reportClient.evaluateMapReport(reportData2).getResult();

    // then
    assertThat(result1.getFirstMeasureData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result1)).isEqualTo(2L);
    assertThat(
            MapResultUtil.getEntryForKey(result1.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(2.);
    assertThat(
            MapResultUtil.getEntryForKey(result1.getFirstMeasureData(), SECOND_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(2.);

    assertThat(result2.getFirstMeasureData()).hasSize(2);
    assertThat(getExecutedFlowNodeCount(result2)).isEqualTo(2L);
    assertThat(
            MapResultUtil.getEntryForKey(result2.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
    assertThat(
            MapResultUtil.getEntryForKey(
                result2.getFirstMeasureData(), DISTRIBUTE_BY_IDENTITY_MISSING_KEY))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
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
    ProcessReportDataDto reportData = createReport(processKey, ReportConstants.ALL_VERSIONS);
    reportData.setTenantIds(selectedTenants);
    ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount()).isEqualTo((long) selectedTenants.size());
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
            Collections.singletonList(Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            IN,
            new String[] {DEFAULT_USERNAME, SECOND_USER},
            1L,
            Arrays.asList(
                Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 1.),
                Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_USER},
            1L,
            Collections.singletonList(Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            NOT_IN, new String[] {DEFAULT_USERNAME, SECOND_USER}, 0L, Collections.emptyList()));
  }

  @ParameterizedTest
  @MethodSource("viewLevelAssigneeFilterScenarios")
  public void viewLevelFilterByAssigneeOnlyCountsCandidateGroupsFromThoseMatchingFilter(
      final MembershipFilterOperator filterOperator,
      final String[] filterValues,
      final Long expectedInstanceCount,
      final List<Tuple> expectedResult) {
    // given
    engineIntegrationExtension.addUser(SECOND_USER, SECOND_USERS_PASSWORD);
    engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto.getId());

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
        .containsExactlyInAnyOrderElementsOf(expectedResult);
  }

  public static Stream<Arguments> instanceLevelAssigneeFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_USER},
            1L,
            Arrays.asList(
                Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 1.),
                Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            IN,
            new String[] {DEFAULT_USERNAME, SECOND_USER},
            2L,
            Arrays.asList(
                Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 3.),
                Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_USER},
            2L,
            Arrays.asList(
                Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 3.),
                Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            NOT_IN, new String[] {DEFAULT_USERNAME, SECOND_USER}, 0L, Collections.emptyList()));
  }

  @ParameterizedTest
  @MethodSource("instanceLevelAssigneeFilterScenarios")
  public void instanceLevelFilterByAssigneeOnlyCountsCandidateGroupsFromInstancesMatchingFilter(
      final MembershipFilterOperator filterOperator,
      final String[] filterValues,
      final Long expectedInstanceCount,
      final List<Tuple> expectedResult) {
    // given
    engineIntegrationExtension.addUser(SECOND_USER, SECOND_USERS_PASSWORD);
    engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto firstInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, firstInstance.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, firstInstance.getId());
    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, secondInstance.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, secondInstance.getId());

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
        .containsExactlyInAnyOrderElementsOf(expectedResult);
  }

  public static Stream<Arguments> viewLevelCandidateGroupFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            1L,
            Collections.singletonList(Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            1L,
            Arrays.asList(
                Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 1.),
                Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            1L,
            Collections.singletonList(Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            NOT_IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            0L,
            Collections.emptyList()));
  }

  @ParameterizedTest
  @MethodSource("viewLevelCandidateGroupFilterScenarios")
  public void viewLevelFilterByCandidateGroupOnlyCountsThoseCandidateGroups(
      final MembershipFilterOperator filterOperator,
      final String[] filterValues,
      final Long expectedInstanceCount,
      final List<Tuple> expectedResult) {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());

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
                Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 1.),
                Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            2L,
            Arrays.asList(
                Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 3.),
                Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            2L,
            Arrays.asList(
                Tuple.tuple(FIRST_CANDIDATE_GROUP_ID, 3.),
                Tuple.tuple(SECOND_CANDIDATE_GROUP_ID, 1.))),
        Arguments.of(
            NOT_IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            0L,
            Collections.emptyList()));
  }

  @ParameterizedTest
  @MethodSource("instanceLevelCandidateGroupFilterScenarios")
  public void
      instanceLevelFilterByCandidateGroupOnlyCountsThoseCandidateGroupsFromInstancesMatchingFilter(
          final MembershipFilterOperator filterOperator,
          final String[] filterValues,
          final Long expectedInstanceCount,
          final List<Tuple> expectedResult) {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto firstInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(firstInstance.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(firstInstance.getId());
    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(secondInstance.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(secondInstance.getId());

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
  @AllArgsConstructor
  static class FlowNodeStatusTestValues {
    List<ProcessFilterDto<?>> processFilter;
    Map<String, Double> expectedFrequencyValues;
    long expectedInstanceCount;
  }

  private static Map<String, Double> getExpectedResultsMap(
      Double userTask1Results, Double userTask2Results) {
    Map<String, Double> result = new HashMap<>();
    result.put(FIRST_CANDIDATE_GROUP_ID, userTask1Results);
    result.put(SECOND_CANDIDATE_GROUP_ID, userTask2Results);
    return result;
  }

  protected static Stream<FlowNodeStatusTestValues> getFlowNodeStatusExpectedValues() {
    return Stream.of(
        new FlowNodeStatusTestValues(
            ProcessFilterBuilder.filter().runningFlowNodesOnly().add().buildList(),
            getExpectedResultsMap(2., 1.),
            2L),
        new FlowNodeStatusTestValues(
            ProcessFilterBuilder.filter().completedFlowNodesOnly().add().buildList(),
            getExpectedResultsMap(1., null),
            1L),
        new FlowNodeStatusTestValues(
            ProcessFilterBuilder.filter().completedOrCanceledFlowNodesOnly().add().buildList(),
            getExpectedResultsMap(1., null),
            1L));
  }

  @ParameterizedTest
  @MethodSource("getFlowNodeStatusExpectedValues")
  public void evaluateReportWithFlowNodeStatusFilter(
      FlowNodeStatusTestValues flowNodeStatusTestValues) {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // finish first running task, second now runs but unclaimed
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // claim first running task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.claimAllRunningUserTasks(processInstanceDto2.getId());

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.setFilter(flowNodeStatusTestValues.processFilter);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat((long) result.getFirstMeasureData().size())
        .isEqualTo(
            flowNodeStatusTestValues.getExpectedFrequencyValues().values().stream()
                .filter(Objects::nonNull)
                .count());
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(
            flowNodeStatusTestValues.getExpectedFrequencyValues().get(FIRST_CANDIDATE_GROUP_ID));
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), SECOND_CANDIDATE_GROUP_ID)
                .map(MapResultEntryDto::getValue)
                .orElse(null))
        .isEqualTo(
            flowNodeStatusTestValues.getExpectedFrequencyValues().get(SECOND_CANDIDATE_GROUP_ID));
    assertThat(result.getInstanceCount())
        .isEqualTo(flowNodeStatusTestValues.getExpectedInstanceCount());
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2L);
  }

  @Test
  public void evaluateReportWithFlowNodeStatusFilterCanceled() {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto firstInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // finish first running task, second now runs unclaimed and canceled
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(firstInstance.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.cancelActivityInstance(firstInstance.getId(), USER_TASK_2);

    final ProcessInstanceEngineDto secondInstance =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // claim and cancel first running task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.claimAllRunningUserTasks(secondInstance.getId());
    engineIntegrationExtension.cancelActivityInstance(secondInstance.getId(), USER_TASK_1);

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.setFilter(ProcessFilterBuilder.filter().canceledFlowNodesOnly().add().buildList());
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(2);
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
    assertThat(
            MapResultUtil.getEntryForKey(result.getFirstMeasureData(), SECOND_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
    assertThat(result.getInstanceCount()).isEqualTo(2L);
    assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2L);
  }

  @Test
  public void processDefinitionContainsMultiInstanceBody() {
    // given
    BpmnModelInstance processWithMultiInstanceUserTask =
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
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(2.);
  }

  @Test
  public void evaluateReportForMoreThanTenEvents() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();

    for (int i = 0; i < 11; i++) {
      final ProcessInstanceEngineDto processInstanceDto =
          engineIntegrationExtension.startProcessInstance(processDefinition.getId());
      engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
      engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    }

    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(11.);
  }

  @Test
  public void filterInReport() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);

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
    assertThat(result.getFirstMeasureData()).isEmpty();
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(0L);

    // when
    reportData = createReport(processDefinition);
    reportData.setFilter(createStartDateFilter(processStartTime, null));
    result = reportClient.evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getFirstMeasureData()).isNotNull();
    assertThat(result.getFirstMeasureData()).hasSize(1);
    assertThat(getExecutedFlowNodeCount(result)).isEqualTo(1L);
    assertThat(MapResultUtil.getEntryForKey(result.getFirstMeasureData(), FIRST_CANDIDATE_GROUP_ID))
        .isPresent()
        .get()
        .extracting(MapResultEntryDto::getValue)
        .isEqualTo(1.);
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

  protected ProcessReportDataDto createReport(
      final String processDefinitionKey, final String version) {
    return TemplatedProcessReportDataBuilder.createReportData()
        .setProcessDefinitionKey(processDefinitionKey)
        .setProcessDefinitionVersion(version)
        .setReportDataType(ProcessReportDataType.USER_TASK_FREQ_GROUP_BY_CANDIDATE)
        .build();
  }

  private ProcessReportDataDto createReport(final ProcessDefinitionEngineDto processDefinition) {
    return createReport(processDefinition.getKey(), String.valueOf(processDefinition.getVersion()));
  }

  private List<ProcessFilterDto<?>> createStartDateFilter(
      OffsetDateTime startDate, OffsetDateTime endDate) {
    return ProcessFilterBuilder.filter()
        .fixedInstanceStartDate()
        .start(startDate)
        .end(endDate)
        .add()
        .buildList();
  }

  private void finishTwoUserTasksOneWithFirstAndSecondGroup(
      final ProcessInstanceEngineDto processInstanceDto) {
    // finish first task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
    // finish second task with
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(SECOND_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
  }

  private void finishOneUserTaskOneWithFirstAndLeaveSecondUnassigned(
      final ProcessInstanceEngineDto processInstanceDto) {
    // finish first task
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(FIRST_CANDIDATE_GROUP_ID);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstanceDto.getId());
  }

  private String deployAndStartMultiTenantUserTaskProcess(final List<String> deployedTenants) {
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

  private ProcessDefinitionEngineDto deployOneUserTasksDefinition(String key, String tenantId) {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(
        getSingleUserTaskDiagram(key), tenantId);
  }

  private ProcessDefinitionEngineDto deployTwoUserTasksDefinition() {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(
        getDoubleUserTaskDiagram());
  }

  private long getExecutedFlowNodeCount(
      ReportResultResponseDto<List<MapResultEntryDto>> resultList) {
    return resultList.getFirstMeasureData().stream()
        .map(MapResultEntryDto::getValue)
        .filter(Objects::nonNull)
        .count();
  }

  private void assertCorrectValueOrdering(ReportResultResponseDto<List<MapResultEntryDto>> result) {
    List<MapResultEntryDto> resultData = result.getFirstMeasureData();
    final List<Double> bucketValues =
        resultData.stream().map(MapResultEntryDto::getValue).collect(Collectors.toList());
    final List<Double> bucketValuesWithoutNullValue =
        bucketValues.stream().filter(Objects::nonNull).collect(Collectors.toList());
    assertThat(bucketValuesWithoutNullValue).isSortedAccordingTo(Comparator.naturalOrder());
    for (int i = resultData.size() - 1; i > getExecutedFlowNodeCount(result) - 1; i--) {
      assertThat(bucketValues.get(i)).isNull();
    }
  }

  private String getLocalizedUnassignedLabel() {
    return embeddedOptimizeExtension
        .getLocalizationService()
        .getDefaultLocaleMessageForMissingAssigneeLabel();
  }
}
