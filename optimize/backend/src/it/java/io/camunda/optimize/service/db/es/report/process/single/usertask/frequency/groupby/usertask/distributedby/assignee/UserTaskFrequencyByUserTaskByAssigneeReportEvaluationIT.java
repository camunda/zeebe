/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.usertask.frequency.groupby.usertask.distributedby.assignee;

import static com.google.common.collect.Lists.newArrayList;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.IN;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator.NOT_IN;
import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_KEY;
import static io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto.SORT_BY_LABEL;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.service.db.es.report.command.modules.distributed_by.process.identity.ProcessDistributedByIdentity.DISTRIBUTE_BY_IDENTITY_MISSING_KEY;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
import static io.camunda.optimize.util.BpmnModels.START_EVENT_ID;
import static io.camunda.optimize.util.BpmnModels.VERSION_TAG;
import static io.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
import static io.camunda.optimize.util.BpmnModels.getSingleUserTaskDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.ReportConstants;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.MembershipFilterOperator;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
import io.camunda.optimize.dto.optimize.query.sorting.ReportSortingDto;
import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.db.es.report.process.AbstractProcessDefinitionIT;
import io.camunda.optimize.service.db.es.report.util.HyperMapAsserter;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import io.camunda.optimize.util.BpmnModels;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UserTaskFrequencyByUserTaskByAssigneeReportEvaluationIT
    extends AbstractProcessDefinitionIT {

  private static final String PROCESS_DEFINITION_KEY = "aProcessDefinitionKey";
  private static final String USER_TASK_A = "userTaskA";
  private static final String USER_TASK_B = "userTaskB";

  @BeforeEach
  public void init() {
    // create second user
    engineIntegrationExtension.addUser(SECOND_USER, SECOND_USER_FIRST_NAME, SECOND_USER_LAST_NAME);
    engineIntegrationExtension.grantAllAuthorizations(SECOND_USER);
  }

  @Test
  public void reportEvaluationForOneProcessInstance() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployFourUserTasksDefinition();
    ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishUserTask1AWithDefaultAndTaskB2WithSecondUser(processInstanceDto);

    importAndRefresh();

    final ProcessReportDataDto reportData = createReport(processDefinition);

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
        evaluationResponse = reportClient.evaluateHyperMapReport(reportData);

    // then
    final ProcessReportDataDto resultReportDataDto =
        evaluationResponse.getReportDefinition().getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(resultReportDataDto.getDefinitionVersions())
        .containsExactly(processDefinition.getVersionAsString());
    assertThat(resultReportDataDto.getView()).isNotNull();
    assertThat(resultReportDataDto.getView().getEntity()).isEqualTo(ProcessViewEntity.USER_TASK);
    assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.FREQUENCY);

    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        evaluationResponse.getResult();
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(1L)
        .processInstanceCountWithoutFilters(1L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(USER_TASK_1)
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
        .groupByContains(USER_TASK_2)
        .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
        .groupByContains(USER_TASK_A)
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
        .groupByContains(USER_TASK_B)
        .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
        .doAssert(actualResult);
    // @formatter:on
  }

  @Test
  public void reportEvaluationForOneProcessInstance_whenAssigneeCacheEmptyLabelEqualsKey() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployOneUserTasksDefinition();
    engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    importAndRefresh();

    // cache is empty
    embeddedOptimizeExtension.getUserTaskIdentityCache().resetCache();

    final ProcessReportDataDto reportData = createReport(processDefinition);

    // when
    final AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>>
        evaluationResponse = reportClient.evaluateHyperMapReport(reportData);

    // then
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        evaluationResponse.getResult();
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(1L)
        .processInstanceCountWithoutFilters(1L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(USER_TASK_1)
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_USERNAME)
        .doAssert(actualResult);
    // @formatter:on
  }

  @Test
  public void reportEvaluationForOneProcessInstanceWithUnassignedTasks() {
    // given
    ProcessDefinitionEngineDto processDefinition = deployFourUserTasksDefinition();
    ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishUserTask1AWithDefaultAndLeaveTasks2BUnassigned(processInstanceDto);

    importAndRefresh();

    final ProcessReportDataDto reportData = createReport(processDefinition);

    // when
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(1L)
        .processInstanceCountWithoutFilters(1L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(USER_TASK_1)
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, null, getLocalizedUnassignedLabel())
        .groupByContains(USER_TASK_2)
        .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, 1., getLocalizedUnassignedLabel())
        .groupByContains(USER_TASK_A)
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, null, getLocalizedUnassignedLabel())
        .groupByContains(USER_TASK_B)
        .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, 1., getLocalizedUnassignedLabel())
        .doAssert(actualResult);
    // @formatter:on
  }

  @Test
  public void reportEvaluationForSeveralProcessInstances() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployFourUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishUserTask1AWithDefaultAndTaskB2WithSecondUser(processInstanceDto1);

    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    finishUserTask1AWithDefaultAndLeaveTasks2BUnassigned(processInstanceDto2);

    importAndRefresh();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(USER_TASK_1)
        .distributedByContains(DEFAULT_USERNAME, 2., DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, null, getLocalizedUnassignedLabel())
        .groupByContains(USER_TASK_2)
        .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, 1., getLocalizedUnassignedLabel())
        .groupByContains(USER_TASK_A)
        .distributedByContains(DEFAULT_USERNAME, 2., DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, null, getLocalizedUnassignedLabel())
        .groupByContains(USER_TASK_B)
        .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, 1., getLocalizedUnassignedLabel())
        .doAssert(actualResult);
    // @formatter:on
  }

  @Test
  public void reportEvaluationForSeveralProcessDefinitions() {
    // given
    final String key1 = "key1";
    final String key2 = "key2";
    final Double expectedDuration = 20.;
    final ProcessDefinitionEngineDto processDefinition1 =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            BpmnModels.getSingleUserTaskDiagram(key1, USER_TASK_1));
    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto1.getId());
    final ProcessDefinitionEngineDto processDefinition2 =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(
            BpmnModels.getSingleUserTaskDiagram(key2, USER_TASK_2));
    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto2.getId());
    importAllEngineEntitiesFromScratch();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition1);
    reportData.getDefinitions().add(createReportDataDefinitionDto(key2));
    AuthorizedProcessReportEvaluationResponseDto<List<HyperMapResultEntryDto>> evaluationResponse =
        reportClient.evaluateHyperMapReport(reportData);

    // then
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> result =
        evaluationResponse.getResult();
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(USER_TASK_1)
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, null, SECOND_USER_FULL_NAME)
        .groupByContains(USER_TASK_2)
        .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
        .doAssert(result);
    // @formatter:on
  }

  @Test
  public void testCustomOrderOnResultKeyIsApplied() {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // finish first task with default user
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto1.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto1.getId());
    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto2.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto2.getId());

    importAndRefresh();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_KEY, SortOrder.DESC));
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(USER_TASK_2)
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
        .groupByContains(USER_TASK_1)
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
        .doAssert(actualResult);
    // @formatter:on
  }

  @Test
  public void testCustomOrderOnResultLabelIsApplied() {
    // given
    BpmnModelInstance bpmnModelInstance =
        Bpmn.createExecutableProcess("aProcess")
            .camundaVersionTag(VERSION_TAG)
            .startEvent(START_EVENT_ID)
            .userTask(USER_TASK_1)
            .name("thisLabelComesSecond")
            .userTask(USER_TASK_2)
            .name("thisLabelComesFirst")
            .endEvent(END_EVENT)
            .done();

    final ProcessDefinitionEngineDto processDefinition =
        engineIntegrationExtension.deployProcessAndGetProcessDefinition(bpmnModelInstance);

    final ProcessInstanceEngineDto processInstanceDto1 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // finish tasks of first instance with default user
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto1.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto1.getId());
    final ProcessInstanceEngineDto processInstanceDto2 =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // finish tasks of instance 2 with second user
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto2.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto2.getId());

    importAndRefresh();

    // when
    final ProcessReportDataDto reportData = createReport(processDefinition);
    reportData.getConfiguration().setSorting(new ReportSortingDto(SORT_BY_LABEL, SortOrder.DESC));
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(USER_TASK_1, "thisLabelComesSecond")
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
        .groupByContains(USER_TASK_2, "thisLabelComesFirst")
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(SECOND_USER, 1., SECOND_USER_FULL_NAME)
        .doAssert(actualResult);
    // @formatter:on
  }

  @Test
  public void otherProcessDefinitionsDoNotInfluenceResult() {
    // given
    final ProcessDefinitionEngineDto processDefinition1 = deployOneUserTasksDefinition();
    engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
    engineIntegrationExtension.startProcessInstance(processDefinition1.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    final ProcessDefinitionEngineDto processDefinition2 = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto3 =
        engineIntegrationExtension.startProcessInstance(processDefinition2.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto3.getId());

    importAndRefresh();

    // when
    final ProcessReportDataDto reportData1 = createReport(processDefinition1);
    final ProcessReportDataDto reportData2 = createReport(processDefinition2);
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult1 =
        reportClient.evaluateHyperMapReport(reportData1).getResult();
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult2 =
        reportClient.evaluateHyperMapReport(reportData2).getResult();

    // then
    // @formatter:off
    HyperMapAsserter.asserter()
        .processInstanceCount(2L)
        .processInstanceCountWithoutFilters(2L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(USER_TASK_1)
        .distributedByContains(DEFAULT_USERNAME, 2., DEFAULT_FULLNAME)
        .doAssert(actualResult1);

    HyperMapAsserter.asserter()
        .processInstanceCount(1L)
        .processInstanceCountWithoutFilters(1L)
        .measure(ViewProperty.FREQUENCY)
        .groupByContains(USER_TASK_1)
        .distributedByContains(DEFAULT_USERNAME, 1., DEFAULT_FULLNAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, null, getLocalizedUnassignedLabel())
        .groupByContains(USER_TASK_2)
        .distributedByContains(DEFAULT_USERNAME, null, DEFAULT_FULLNAME)
        .distributedByContains(
            DISTRIBUTE_BY_IDENTITY_MISSING_KEY, 1., getLocalizedUnassignedLabel())
        .doAssert(actualResult2);
    // @formatter:on
  }

  @Test
  public void reportEvaluationSingleBucketFilteredBySingleTenant() {
    // given
    final String tenantId1 = "tenantId1";
    final String tenantId2 = "tenantId2";
    final List<String> selectedTenants = newArrayList(tenantId1);
    final String processKey =
        deployAndStartMultiTenantUserTaskProcess(newArrayList(null, tenantId1, tenantId2));

    importAndRefresh();

    // when
    ProcessReportDataDto reportData = createReport(processKey, ReportConstants.ALL_VERSIONS);
    reportData.setTenantIds(selectedTenants);
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    assertThat(actualResult.getInstanceCount()).isEqualTo(selectedTenants.size());
  }

  @Test
  public void noUserTaskMatchesReturnsEmptyResult() {
    // when
    final ProcessReportDataDto reportData = createReport("nonExistingProcessDefinitionId", "1");
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    assertThat(actualResult.getFirstMeasureData()).hasSize(0);
  }

  public static Stream<Arguments> viewLevelAssigneeFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_USER},
            1L,
            ImmutableMap.builder()
                .put(USER_TASK_2, Collections.singletonList(createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            IN,
            new String[] {DEFAULT_USERNAME, SECOND_USER},
            1L,
            ImmutableMap.builder()
                .put(
                    USER_TASK_1,
                    Arrays.asList(createDefaultUserTriple(1.), createSecondUserTriple(null)))
                .put(
                    USER_TASK_2,
                    Arrays.asList(createDefaultUserTriple(null), createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_USER},
            1L,
            ImmutableMap.builder()
                .put(USER_TASK_1, Lists.newArrayList(createDefaultUserTriple(1.)))
                .build()),
        Arguments.of(
            NOT_IN, new String[] {DEFAULT_USERNAME, SECOND_USER}, 0L, Collections.emptyMap()));
  }

  @ParameterizedTest
  @MethodSource("viewLevelAssigneeFilterScenarios")
  public void viewLevelFilterByAssigneeOnlyCountsUserTasksWithThatAssignee(
      final MembershipFilterOperator filterOperator,
      final String[] filterValues,
      final Long expectedInstanceCount,
      final Map<String, List<Triple<String, Double, String>>> expectedResult) {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployTwoUserTasksDefinition();
    final ProcessInstanceEngineDto processInstanceDto =
        engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
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
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    final HyperMapAsserter.MeasureAdder hyperMapAsserter =
        HyperMapAsserter.asserter()
            .processInstanceCount(expectedInstanceCount)
            .processInstanceCountWithoutFilters(1)
            .measure(ViewProperty.FREQUENCY);
    expectedResult.forEach(
        (userTaskId, distributionResults) -> {
          final HyperMapAsserter.GroupByAdder groupByAdder =
              hyperMapAsserter.groupByContains(userTaskId);
          distributionResults.forEach(
              assigneeGroupAndCount ->
                  groupByAdder.distributedByContains(
                      assigneeGroupAndCount.getLeft(),
                      assigneeGroupAndCount.getMiddle(),
                      assigneeGroupAndCount.getRight()));
          groupByAdder.add();
        });
    hyperMapAsserter.doAssert(actualResult);
  }

  public static Stream<Arguments> instanceLevelAssigneeFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_USER},
            1L,
            ImmutableMap.builder()
                .put(
                    USER_TASK_1,
                    Arrays.asList(createDefaultUserTriple(1.), createSecondUserTriple(null)))
                .put(
                    USER_TASK_2,
                    Arrays.asList(createDefaultUserTriple(null), createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            IN,
            new String[] {DEFAULT_USERNAME, SECOND_USER},
            2L,
            ImmutableMap.builder()
                .put(
                    USER_TASK_1,
                    Arrays.asList(createDefaultUserTriple(2.), createSecondUserTriple(null)))
                .put(
                    USER_TASK_2,
                    Arrays.asList(createDefaultUserTriple(1.), createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_USER},
            2L,
            ImmutableMap.builder()
                .put(
                    USER_TASK_1,
                    Arrays.asList(createDefaultUserTriple(2.), createSecondUserTriple(null)))
                .put(
                    USER_TASK_2,
                    Arrays.asList(createDefaultUserTriple(1.), createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            NOT_IN,
            new String[] {DEFAULT_USERNAME, SECOND_USER},
            0L,
            ImmutableMap.builder()
                .put(USER_TASK_1, Collections.emptyList())
                .put(USER_TASK_2, Collections.emptyList())
                .build()));
  }

  @ParameterizedTest
  @MethodSource("instanceLevelAssigneeFilterScenarios")
  public void instanceLevelFilterByAssigneeOnlyCountsUserTasksFromInstancesWithThatAssignee(
      final MembershipFilterOperator filterOperator,
      final String[] filterValues,
      final Long expectedInstanceCount,
      final Map<String, List<Triple<String, Double, String>>> expectedResult) {
    // given
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
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    final HyperMapAsserter.MeasureAdder hyperMapAsserter =
        HyperMapAsserter.asserter()
            .processInstanceCount(expectedInstanceCount)
            .processInstanceCountWithoutFilters(2)
            .measure(ViewProperty.FREQUENCY);
    expectedResult.forEach(
        (userTaskId, distributionResults) -> {
          final HyperMapAsserter.GroupByAdder groupByAdder =
              hyperMapAsserter.groupByContains(userTaskId);
          distributionResults.forEach(
              assigneeGroupAndCount ->
                  groupByAdder.distributedByContains(
                      assigneeGroupAndCount.getLeft(),
                      assigneeGroupAndCount.getMiddle(),
                      assigneeGroupAndCount.getRight()));
          groupByAdder.add();
        });
    hyperMapAsserter.doAssert(actualResult);
  }

  public static Stream<Arguments> viewLevelCandidateGroupFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            1L,
            ImmutableMap.builder()
                .put(USER_TASK_2, Collections.singletonList(createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            1L,
            ImmutableMap.builder()
                .put(
                    USER_TASK_1,
                    Arrays.asList(createDefaultUserTriple(1.), createSecondUserTriple(null)))
                .put(
                    USER_TASK_2,
                    Arrays.asList(createDefaultUserTriple(null), createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            1L,
            ImmutableMap.builder()
                .put(USER_TASK_1, Collections.singletonList(createDefaultUserTriple(1.)))
                .build()),
        Arguments.of(
            NOT_IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            0L,
            Collections.emptyMap()));
  }

  @ParameterizedTest
  @MethodSource("viewLevelCandidateGroupFilterScenarios")
  public void viewLevelFilterByCandidateGroupOnlyCountsUserTasksWithThatCandidateGroup(
      final MembershipFilterOperator filterOperator,
      final String[] filterValues,
      final Long expectedInstanceCount,
      final Map<String, List<Triple<String, Double, String>>> expectedResult) {
    // given
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
    final List<ProcessFilterDto<?>> candidateGroupFilter =
        ProcessFilterBuilder.filter()
            .candidateGroups()
            .ids(filterValues)
            .operator(filterOperator)
            .filterLevel(FilterApplicationLevel.VIEW)
            .add()
            .buildList();
    reportData.setFilter(candidateGroupFilter);
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    final HyperMapAsserter.MeasureAdder hyperMapAsserter =
        HyperMapAsserter.asserter()
            .processInstanceCount(expectedInstanceCount)
            .processInstanceCountWithoutFilters(1)
            .measure(ViewProperty.FREQUENCY);
    expectedResult.forEach(
        (userTaskId, distributionResults) -> {
          final HyperMapAsserter.GroupByAdder groupByAdder =
              hyperMapAsserter.groupByContains(userTaskId);
          distributionResults.forEach(
              candidateGroupAndCount ->
                  groupByAdder.distributedByContains(
                      candidateGroupAndCount.getLeft(),
                      candidateGroupAndCount.getMiddle(),
                      candidateGroupAndCount.getRight()));
          groupByAdder.add();
        });
    hyperMapAsserter.doAssert(actualResult);
  }

  public static Stream<Arguments> instanceLevelCandidateGroupFilterScenarios() {
    return Stream.of(
        Arguments.of(
            IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            1L,
            ImmutableMap.builder()
                .put(
                    USER_TASK_1,
                    Arrays.asList(createDefaultUserTriple(1.), createSecondUserTriple(null)))
                .put(
                    USER_TASK_2,
                    Arrays.asList(createDefaultUserTriple(null), createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            2L,
            ImmutableMap.builder()
                .put(
                    USER_TASK_1,
                    Arrays.asList(createDefaultUserTriple(2.), createSecondUserTriple(null)))
                .put(
                    USER_TASK_2,
                    Arrays.asList(createDefaultUserTriple(1.), createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            NOT_IN,
            new String[] {SECOND_CANDIDATE_GROUP_ID},
            2L,
            ImmutableMap.builder()
                .put(
                    USER_TASK_1,
                    Arrays.asList(createDefaultUserTriple(2.), createSecondUserTriple(null)))
                .put(
                    USER_TASK_2,
                    Arrays.asList(createDefaultUserTriple(1.), createSecondUserTriple(1.)))
                .build()),
        Arguments.of(
            NOT_IN,
            new String[] {FIRST_CANDIDATE_GROUP_ID, SECOND_CANDIDATE_GROUP_ID},
            0L,
            ImmutableMap.builder()
                .put(USER_TASK_1, Collections.emptyList())
                .put(USER_TASK_2, Collections.emptyList())
                .build()));
  }

  @ParameterizedTest
  @MethodSource("instanceLevelCandidateGroupFilterScenarios")
  public void
      instanceLevelFilterByCandidateGroupOnlyCountsUserTasksFromInstancesWithThatCandidateGroup(
          final MembershipFilterOperator filterOperator,
          final String[] filterValues,
          final Long expectedInstanceCount,
          final Map<String, List<Triple<String, Double, String>>> expectedResult) {
    // given
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
    final List<ProcessFilterDto<?>> candidateGroupFilter =
        ProcessFilterBuilder.filter()
            .candidateGroups()
            .ids(filterValues)
            .operator(filterOperator)
            .filterLevel(FilterApplicationLevel.INSTANCE)
            .add()
            .buildList();
    reportData.setFilter(candidateGroupFilter);
    final ReportResultResponseDto<List<HyperMapResultEntryDto>> actualResult =
        reportClient.evaluateHyperMapReport(reportData).getResult();

    // then
    final HyperMapAsserter.MeasureAdder hyperMapAsserter =
        HyperMapAsserter.asserter()
            .processInstanceCount(expectedInstanceCount)
            .processInstanceCountWithoutFilters(2)
            .measure(ViewProperty.FREQUENCY);
    expectedResult.forEach(
        (userTaskId, distributionResults) -> {
          final HyperMapAsserter.GroupByAdder groupByAdder =
              hyperMapAsserter.groupByContains(userTaskId);
          distributionResults.forEach(
              candidateGroupAndCount ->
                  groupByAdder.distributedByContains(
                      candidateGroupAndCount.getLeft(),
                      candidateGroupAndCount.getMiddle(),
                      candidateGroupAndCount.getRight()));
          groupByAdder.add();
        });
    hyperMapAsserter.doAssert(actualResult);
  }

  private ProcessReportDataDto createReport(final ProcessDefinitionEngineDto processDefinition) {
    return createReport(processDefinition.getKey(), String.valueOf(processDefinition.getVersion()));
  }

  protected ProcessReportDataDto createReport(
      final String processDefinitionKey, final String version) {
    return createReport(processDefinitionKey, ImmutableList.of(version));
  }

  protected ProcessReportDataDto createReport(
      final String processDefinitionKey, final List<String> versions) {
    return TemplatedProcessReportDataBuilder.createReportData()
        .setProcessDefinitionKey(processDefinitionKey)
        .setProcessDefinitionVersions(versions)
        .setReportDataType(ProcessReportDataType.USER_TASK_FREQ_GROUP_BY_USER_TASK_BY_ASSIGNEE)
        .build();
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
    return deployOneUserTasksDefinition(PROCESS_DEFINITION_KEY, null);
  }

  private ProcessDefinitionEngineDto deployOneUserTasksDefinition(String key, String tenantId) {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(
        getSingleUserTaskDiagram(key), tenantId);
  }

  private ProcessDefinitionEngineDto deployTwoUserTasksDefinition() {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(
        getDoubleUserTaskDiagram());
  }

  private ProcessDefinitionEngineDto deployFourUserTasksDefinition() {
    BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
            .startEvent()
            .parallelGateway()
            .userTask(USER_TASK_1)
            .userTask(USER_TASK_2)
            .endEvent()
            .moveToLastGateway()
            .userTask(USER_TASK_A)
            .userTask(USER_TASK_B)
            .endEvent()
            .done();
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(modelInstance);
  }

  private void finishUserTask1AWithDefaultAndTaskB2WithSecondUser(
      final ProcessInstanceEngineDto processInstanceDto) {
    // finish user task 1 and A with default user
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
    // finish user task 2 and B with second user
    engineIntegrationExtension.finishAllRunningUserTasks(
        SECOND_USER, SECOND_USERS_PASSWORD, processInstanceDto.getId());
  }

  private void finishUserTask1AWithDefaultAndLeaveTasks2BUnassigned(
      final ProcessInstanceEngineDto processInstanceDto) {
    // finish user task 1 and A with default user
    engineIntegrationExtension.finishAllRunningUserTasks(
        DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceDto.getId());
  }

  private String getLocalizedUnassignedLabel() {
    return embeddedOptimizeExtension
        .getLocalizationService()
        .getDefaultLocaleMessageForMissingAssigneeLabel();
  }

  private void importAndRefresh() {
    importAllEngineEntitiesFromScratch();
  }
}
