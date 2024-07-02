/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.process.kpi;

import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.query.processoverview.KpiResultDto;
import io.camunda.optimize.dto.optimize.query.processoverview.KpiType;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessOverviewResponseDto;
import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.target_value.TargetDto;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.target_value.TargetValueUnit;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RollingDateFilterStartDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.instance.RollingDateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ExecutedFlowNodeFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.InstanceStartDateFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

public class ProcessKpiRetrievalIT extends AbstractPlatformIT {

  private static final String PROCESS_DEFINITION_KEY = "aProcessDefKey";

  @Test
  public void getKpisForDefinition() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    String reportId1 = createKpiReport("1", PROCESS_DEFINITION_KEY);
    String reportId2 = createKpiReport("2", PROCESS_DEFINITION_KEY);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(null);

    // then
    assertThat(processes)
        .hasSize(1)
        .extracting(ProcessOverviewResponseDto::getProcessDefinitionKey)
        .containsExactly(PROCESS_DEFINITION_KEY);
    assertThat(processes.get(0).getKpis())
        .containsExactlyInAnyOrder(
            createExpectedKpiResponse(reportId1, "1"), createExpectedKpiResponse(reportId2, "2"));
  }

  @Test
  public void getKpisForDefinitionIncludingCollectionKpiReports() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto =
        engineIntegrationExtension.deployAndStartProcess(
            getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    final String collectionId =
        collectionClient.createNewCollectionWithProcessScope(processInstanceEngineDto);
    String reportId1 = createKpiReport("1", PROCESS_DEFINITION_KEY);
    String reportId2 = createReport("2", true, PROCESS_DEFINITION_KEY, collectionId);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(null);

    // then
    assertThat(processes)
        .hasSize(1)
        .extracting(ProcessOverviewResponseDto::getProcessDefinitionKey)
        .containsExactly(PROCESS_DEFINITION_KEY);
    assertThat(processes.get(0).getKpis())
        .containsExactlyInAnyOrder(
            createExpectedKpiResponse(reportId1, "1", null),
            createExpectedKpiResponse(reportId2, "2", collectionId));
  }

  @Test
  public void getKpiWithDateFilterForDefinition() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    String reportId1 = createKpiReport("1", PROCESS_DEFINITION_KEY);
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .definitions(List.of(new ReportDataDefinitionDto(PROCESS_DEFINITION_KEY)))
            .build();
    reportDataDto.getConfiguration().getTargetValue().setIsKpi(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setIsBelow(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setTarget("1");
    final RollingDateFilterDataDto dateFilterDataDto =
        new RollingDateFilterDataDto(new RollingDateFilterStartDto(4L, DateUnit.DAYS));
    final InstanceStartDateFilterDto startDateFilterDto = new InstanceStartDateFilterDto();
    startDateFilterDto.setData(dateFilterDataDto);
    startDateFilterDto.setFilterLevel(FilterApplicationLevel.INSTANCE);
    reportDataDto.setFilter(Collections.singletonList(startDateFilterDto));
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(null);

    // then
    assertThat(processes)
        .hasSize(1)
        .extracting(
            ProcessOverviewResponseDto::getProcessDefinitionKey,
            ProcessOverviewResponseDto::getKpis)
        .containsExactlyInAnyOrder(
            Tuple.tuple(
                PROCESS_DEFINITION_KEY, List.of(createExpectedKpiResponse(reportId1, "1"))));
  }

  @Test
  public void reportIsNotReturnedIfNotKpi() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    String kpiReportId = createKpiReport("1", PROCESS_DEFINITION_KEY);
    createReport("2", false, PROCESS_DEFINITION_KEY);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(null);

    // then
    assertThat(processes)
        .hasSize(1)
        .extracting(
            ProcessOverviewResponseDto::getProcessDefinitionKey,
            ProcessOverviewResponseDto::getKpis)
        .containsExactlyInAnyOrder(
            Tuple.tuple(
                PROCESS_DEFINITION_KEY, List.of(createExpectedKpiResponse(kpiReportId, "1"))));
  }

  @Test
  public void otherProcessDefinitionKpiReportIsNotReturned() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    final String defKey = "someDefinition";
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(defKey));
    importAllEngineEntitiesFromScratch();
    String reportId1 = createKpiReport("1", PROCESS_DEFINITION_KEY);
    String reportId2 = createKpiReport("2", PROCESS_DEFINITION_KEY);
    String reportId3 = createKpiReport("1", defKey);
    String reportId4 = createKpiReport("2", defKey);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(null);

    // then
    assertThat(processes)
        .hasSize(2)
        .extracting(
            ProcessOverviewResponseDto::getProcessDefinitionKey,
            ProcessOverviewResponseDto::getKpis)
        .map(
            tuple2 ->
                Tuple.tuple(tuple2.toList().get(0), Set.copyOf((List) tuple2.toList().get(1))))
        .containsExactlyInAnyOrder(
            Tuple.tuple(
                PROCESS_DEFINITION_KEY,
                Set.of(
                    createExpectedKpiResponse(reportId1, "1"),
                    createExpectedKpiResponse(reportId2, "2"))),
            Tuple.tuple(
                defKey,
                Set.of(
                    createExpectedKpiResponse(reportId3, "1"),
                    createExpectedKpiResponse(reportId4, "2"))));
  }

  @Test
  public void kpiTypeGetsAssignedCorrectly() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    ExecutedFlowNodeFilterDto executedFlowNodeFilterDto = new ExecutedFlowNodeFilterDto();
    executedFlowNodeFilterDto.setFilterLevel(FilterApplicationLevel.INSTANCE);
    String reportId1 = createKpiReport("1", PROCESS_DEFINITION_KEY);
    String reportId2 = createKpiReportWithMeasures("2", ViewProperty.DURATION);
    String reportId3 = createKpiReportWithMeasures("3", ViewProperty.FREQUENCY);
    String reportId4 = createKpiReportWithMeasures("4", ViewProperty.PERCENTAGE);
    String reportId5 =
        createKpiReportWithMeasuresAndFilters(
            "5",
            ViewProperty.PERCENTAGE,
            addInstanceDateFilterToBuilder(ProcessFilterBuilder.filter(), now).buildList());
    String reportId6 =
        createKpiReportWithMeasuresAndFilters(
            "6",
            ViewProperty.PERCENTAGE,
            addNoIncidentFilterToBuilder(ProcessFilterBuilder.filter()).buildList());
    String reportId7 =
        createKpiReportWithMeasuresAndFilters(
            "7",
            ViewProperty.PERCENTAGE,
            addNoIncidentFilterToBuilder(
                    addInstanceDateFilterToBuilder(ProcessFilterBuilder.filter(), now))
                .buildList());
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(null);

    // then
    assertThat(processes).hasSize(1);
    assertThat(processes.get(0).getKpis())
        .extracting(KpiResultDto::getReportId, KpiResultDto::getType)
        .containsExactlyInAnyOrder(
            Tuple.tuple(reportId1, KpiType.QUALITY),
            Tuple.tuple(reportId2, KpiType.TIME),
            Tuple.tuple(reportId3, KpiType.QUALITY),
            Tuple.tuple(reportId4, KpiType.TIME),
            Tuple.tuple(reportId5, KpiType.TIME),
            Tuple.tuple(reportId6, KpiType.QUALITY),
            Tuple.tuple(reportId7, KpiType.QUALITY));
  }

  @Test
  public void kpiUnitGetsReturned() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessInstanceEngineDto procInst =
        engineIntegrationExtension.deployAndStartProcess(
            getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    engineDatabaseExtension.changeProcessInstanceStartAndEndDate(procInst.getId(), now, now);
    importAllEngineEntitiesFromScratch();
    ExecutedFlowNodeFilterDto executedFlowNodeFilterDto = new ExecutedFlowNodeFilterDto();
    executedFlowNodeFilterDto.setFilterLevel(FilterApplicationLevel.INSTANCE);
    String reportId1 = createKpiReportWithDurationProgress();

    KpiResultDto expectedResponse = new KpiResultDto();
    expectedResponse.setReportId(reportId1);
    expectedResponse.setReportName("My test report");
    expectedResponse.setValue("0.0");
    expectedResponse.setTarget("1.0");
    expectedResponse.setBelow(false);
    expectedResponse.setType(KpiType.TIME);
    expectedResponse.setMeasure(ViewProperty.DURATION);
    expectedResponse.setUnit(TargetValueUnit.DAYS);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(null);

    // then
    assertThat(processes)
        .hasSize(1)
        .extracting(
            ProcessOverviewResponseDto::getProcessDefinitionKey,
            ProcessOverviewResponseDto::getKpis)
        .containsExactlyInAnyOrder(Tuple.tuple(PROCESS_DEFINITION_KEY, List.of(expectedResponse)));
  }

  @Test
  public void kpiReportsGetRetrievedWithGroupBy() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    String reportId = createKpiReport("1", PROCESS_DEFINITION_KEY);
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .definitions(List.of(new ReportDataDefinitionDto(PROCESS_DEFINITION_KEY)))
            .build();
    reportDataDto.getConfiguration().getTargetValue().setIsKpi(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setIsBelow(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setTarget("2");
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes =
        processOverviewClient.getProcessOverviews(null);

    // then
    assertThat(processes)
        .hasSize(1)
        .extracting(
            ProcessOverviewResponseDto::getProcessDefinitionKey,
            ProcessOverviewResponseDto::getKpis)
        .containsExactlyInAnyOrder(
            Tuple.tuple(PROCESS_DEFINITION_KEY, List.of(createExpectedKpiResponse(reportId, "1"))));
  }

  @Test
  public void userCanSeeUnauthorizedKpiReports() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionRequestDto =
        new SingleProcessReportDefinitionRequestDto();
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .definitions(List.of(new ReportDataDefinitionDto(PROCESS_DEFINITION_KEY)))
            .build();
    reportDataDto.getConfiguration().getTargetValue().setIsKpi(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setIsBelow(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setTarget("9999999");
    singleProcessReportDefinitionRequestDto.setData(reportDataDto);
    singleProcessReportDefinitionRequestDto.setId("someId");
    reportClient.createSingleProcessReportAsUser(
        singleProcessReportDefinitionRequestDto, KERMIT_USER, KERMIT_USER);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes = processOverviewClient.getProcessOverviews();

    // then
    assertThat(processes).hasSize(1);
    assertThat(processes.get(0).getKpis().get(0).getTarget()).isEqualTo("9999999");
  }

  @Test
  public void reportHasNoEvaluationValue() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    TargetDto targetDto = new TargetDto();
    targetDto.setValue("999");
    targetDto.setIsBelow(Boolean.TRUE);
    targetDto.setUnit(TargetValueUnit.HOURS);
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_DUR_GROUP_BY_NONE)
            .definitions(List.of(new ReportDataDefinitionDto(PROCESS_DEFINITION_KEY)))
            .build();
    reportDataDto.setFilter(
        ProcessFilterBuilder.filter()
            .withDeletedIncident()
            .filterLevel(FilterApplicationLevel.INSTANCE)
            .add()
            .buildList());
    reportDataDto.getConfiguration().getTargetValue().setIsKpi(true);
    reportDataDto.getConfiguration().getTargetValue().getDurationProgress().setTarget(targetDto);
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionRequestDto =
        new SingleProcessReportDefinitionRequestDto();
    singleProcessReportDefinitionRequestDto.setData(reportDataDto);
    String reportId =
        reportClient.createSingleProcessReport(singleProcessReportDefinitionRequestDto);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes = processOverviewClient.getProcessOverviews();

    // then
    KpiResultDto expectedKpiResponseDto = new KpiResultDto();
    expectedKpiResponseDto.setReportId(reportId);
    expectedKpiResponseDto.setReportName("New Report");
    expectedKpiResponseDto.setValue(null);
    expectedKpiResponseDto.setTarget("999");
    expectedKpiResponseDto.setBelow(true);
    expectedKpiResponseDto.setType(KpiType.TIME);
    expectedKpiResponseDto.setMeasure(ViewProperty.DURATION);
    expectedKpiResponseDto.setUnit(TargetValueUnit.HOURS);

    assertThat(processes)
        .singleElement()
        .satisfies(
            overviewDto -> {
              assertThat(overviewDto.getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
              assertThat(overviewDto.getKpis())
                  .singleElement()
                  .isEqualTo(expectedKpiResponseDto)
                  .satisfies(kpi -> assertThat(kpi.isTargetMet()).isFalse());
            });
  }

  @Test
  public void reportHasNoReportConfigurationData() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .definitions(List.of(new ReportDataDefinitionDto(PROCESS_DEFINITION_KEY)))
            .build();
    reportDataDto.getConfiguration().setTargetValue(null);
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionRequestDto =
        new SingleProcessReportDefinitionRequestDto();
    singleProcessReportDefinitionRequestDto.setData(reportDataDto);
    reportClient.createSingleProcessReport(singleProcessReportDefinitionRequestDto);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes = processOverviewClient.getProcessOverviews();

    // then
    assertThat(processes).hasSize(1);
    assertThat(processes.get(0).getKpis()).hasSize(0);
  }

  @Test
  public void reportHasNoTarget() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .definitions(List.of(new ReportDataDefinitionDto(PROCESS_DEFINITION_KEY)))
            .build();
    reportDataDto.getConfiguration().getTargetValue().setIsKpi(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setIsBelow(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setTarget(null);
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionRequestDto =
        new SingleProcessReportDefinitionRequestDto();
    singleProcessReportDefinitionRequestDto.setData(reportDataDto);
    reportClient.createSingleProcessReport(singleProcessReportDefinitionRequestDto);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes = processOverviewClient.getProcessOverviews();

    // then
    assertThat(processes).hasSize(1);
    assertThat(processes.get(0).getKpis())
        .singleElement()
        .satisfies(kpiResult -> assertThat(kpiResult.isTargetMet()).isFalse());
  }

  @Test
  public void reportHasNoIsBelowValue() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    importAllEngineEntitiesFromScratch();
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .definitions(List.of(new ReportDataDefinitionDto(PROCESS_DEFINITION_KEY)))
            .build();
    reportDataDto.getConfiguration().getTargetValue().setIsKpi(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setIsBelow(null);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setTarget("999");
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionRequestDto =
        new SingleProcessReportDefinitionRequestDto();
    singleProcessReportDefinitionRequestDto.setData(reportDataDto);
    reportClient.createSingleProcessReport(singleProcessReportDefinitionRequestDto);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes = processOverviewClient.getProcessOverviews();

    // then
    assertThat(processes).hasSize(1);
    assertThat(processes.get(0).getKpis()).hasSize(1);
  }

  @Test
  public void multiProcessGroupByProcessReportDoesNotGetReturned() {
    // given
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(PROCESS_DEFINITION_KEY));
    final String secondDefinitionKey = "secondDefinitionKey";
    engineIntegrationExtension.deployAndStartProcess(getSimpleBpmnDiagram(secondDefinitionKey));
    importAllEngineEntitiesFromScratch();
    ProcessReportDataDto multiProcessReport =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_DUR_GROUP_BY_START_DATE)
            .definitions(
                List.of(
                    new ReportDataDefinitionDto(PROCESS_DEFINITION_KEY),
                    new ReportDataDefinitionDto(secondDefinitionKey)))
            .build();
    SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionRequestDto =
        new SingleProcessReportDefinitionRequestDto();
    multiProcessReport.getConfiguration().getTargetValue().setIsKpi(true);
    multiProcessReport.getConfiguration().getTargetValue().getCountProgress().setIsBelow(true);
    multiProcessReport.getConfiguration().getTargetValue().getCountProgress().setTarget("9999999");
    singleProcessReportDefinitionRequestDto.setData(multiProcessReport);
    reportClient.createSingleProcessReport(singleProcessReportDefinitionRequestDto);
    runKpiSchedulerAndRefreshIndices();

    // when
    final List<ProcessOverviewResponseDto> processes = processOverviewClient.getProcessOverviews();

    // then
    assertThat(processes).hasSize(2);
    assertThat(processes).hasSize(2).flatMap(ProcessOverviewResponseDto::getKpis).isEmpty();
  }

  private KpiResultDto createExpectedKpiResponse(final String reportId, final String target) {
    return createExpectedKpiResponse(reportId, target, null);
  }

  private KpiResultDto createExpectedKpiResponse(
      final String reportId, final String target, final String collectionId) {
    KpiResultDto kpiResponseDto = new KpiResultDto();
    kpiResponseDto.setReportId(reportId);
    kpiResponseDto.setReportName("My test report");
    kpiResponseDto.setValue("1.0");
    kpiResponseDto.setTarget(target);
    kpiResponseDto.setBelow(true);
    kpiResponseDto.setType(KpiType.QUALITY);
    kpiResponseDto.setMeasure(ViewProperty.FREQUENCY);
    kpiResponseDto.setUnit(null);
    kpiResponseDto.setCollectionId(collectionId);
    return kpiResponseDto;
  }

  private String createKpiReport(final String target, final String definitionKey) {
    return createReport(target, true, definitionKey);
  }

  private String createReport(
      final String target, final Boolean isKpi, final String definitionKey) {
    return createReport(target, isKpi, definitionKey, null);
  }

  private String createReport(
      final String target,
      final Boolean isKpi,
      final String definitionKey,
      final String collectionId) {
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .definitions(List.of(new ReportDataDefinitionDto(definitionKey)))
            .build();
    reportDataDto.getConfiguration().getTargetValue().setIsKpi(isKpi);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setIsBelow(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setTarget(target);
    return collectionId == null
        ? reportClient.createSingleProcessReport(reportDataDto)
        : reportClient.createSingleProcessReport(reportDataDto, collectionId);
  }

  private String createKpiReportWithDurationProgress() {
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .definitions(List.of(new ReportDataDefinitionDto(PROCESS_DEFINITION_KEY)))
            .build();
    TargetDto targetDto = new TargetDto();
    targetDto.setValue("1.0");
    targetDto.setUnit(TargetValueUnit.DAYS);
    reportDataDto.getView().setProperties(ViewProperty.DURATION);
    reportDataDto.getConfiguration().getTargetValue().setIsKpi(true);
    reportDataDto.getConfiguration().getTargetValue().getDurationProgress().setTarget(targetDto);
    return reportClient.createSingleProcessReport(reportDataDto);
  }

  private String createKpiReportWithMeasures(final String target, final ViewProperty viewProperty) {
    return createKpiReportWithMeasuresAndFilters(target, viewProperty, Collections.emptyList());
  }

  private String createKpiReportWithMeasuresAndFilters(
      final String target, final ViewProperty viewProperty, List<ProcessFilterDto<?>> filers) {
    final ProcessReportDataDto reportDataDto =
        TemplatedProcessReportDataBuilder.createReportData()
            .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_NONE)
            .definitions(
                List.of(new ReportDataDefinitionDto(ProcessKpiRetrievalIT.PROCESS_DEFINITION_KEY)))
            .build();
    reportDataDto.getConfiguration().getTargetValue().setIsKpi(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setIsBelow(true);
    reportDataDto.getConfiguration().getTargetValue().getCountProgress().setTarget(target);
    reportDataDto.getView().setProperties(List.of(viewProperty));
    if (filers != null && !filers.isEmpty()) {
      reportDataDto.setFilter(filers);
    }
    return reportClient.createSingleProcessReport(reportDataDto);
  }

  private ProcessFilterBuilder addNoIncidentFilterToBuilder(final ProcessFilterBuilder builder) {
    return builder.noIncidents().add();
  }

  private ProcessFilterBuilder addInstanceDateFilterToBuilder(
      final ProcessFilterBuilder builder, OffsetDateTime startDate) {
    return builder.fixedInstanceStartDate().start(startDate).add();
  }

  private void runKpiSchedulerAndRefreshIndices() {
    embeddedOptimizeExtension.getKpiSchedulerService().runKpiImportTask();
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
  }
}
