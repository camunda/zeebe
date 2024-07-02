/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.processinstance.percentage.groupby.none;
//
// import static com.google.common.collect.Lists.newArrayList;
// import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator.IN;
// import static io.camunda.optimize.service.util.ProcessReportDataType.PROC_INST_PER_GROUP_BY_NONE;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.ReportConstants;
// import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
// import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
// import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.db.es.report.process.AbstractProcessDefinitionIT;
// import io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import io.camunda.optimize.test.util.DateCreationFreezer;
// import jakarta.ws.rs.core.Response;
// import java.time.OffsetDateTime;
// import java.util.Collections;
// import java.util.List;
// import java.util.Map;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.MethodSource;
//
// public class ProcessInstancePercentageByNoneReportEvaluationIT extends
// AbstractProcessDefinitionIT {
//
//   public static final String PROCESS_DEFINITION_KEY = "123";
//
//   @Test
//   public void percentageReportEvaluationForOneProcess() {
//     // given
//     ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReport(
//             processInstanceDto.getProcessDefinitionKey(),
//             processInstanceDto.getProcessDefinitionVersion());
//     AuthorizedProcessReportEvaluationResponseDto<Double> evaluationResponse =
//         reportClient.evaluateNumberReport(reportData);
//
//     // then
//     ProcessReportDataDto resultReportDataDto =
// evaluationResponse.getReportDefinition().getData();
//     assertThat(resultReportDataDto.getProcessDefinitionKey())
//         .isEqualTo(processInstanceDto.getProcessDefinitionKey());
//     assertThat(resultReportDataDto.getDefinitionVersions())
//         .containsExactly(processInstanceDto.getProcessDefinitionVersion());
//     assertThat(resultReportDataDto.getView()).isNotNull();
//     assertThat(resultReportDataDto.getView().getEntity())
//         .isEqualTo(ProcessViewEntity.PROCESS_INSTANCE);
//
// assertThat(resultReportDataDto.getView().getFirstProperty()).isEqualTo(ViewProperty.PERCENTAGE);
//     assertThat(resultReportDataDto.getGroupBy().getType()).isEqualTo(ProcessGroupByType.NONE);
//
//     final ReportResultResponseDto<Double> result = evaluationResponse.getResult();
//     assertThat(result.getInstanceCount()).isEqualTo(1L);
//     assertThat(result.getFirstMeasureData()).isNotNull();
//     assertThat(result.getFirstMeasureData()).isEqualTo(100.);
//   }
//
//   @Test
//   public void percentageReportEvaluationForMultipleInstances() {
//     // given
//     ProcessInstanceEngineDto engineDto = deployAndStartSimpleServiceTaskProcess();
//     engineIntegrationExtension.startProcessInstance(engineDto.getDefinitionId());
//     engineIntegrationExtension.startProcessInstance(engineDto.getDefinitionId());
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReport(engineDto.getProcessDefinitionKey(),
// engineDto.getProcessDefinitionVersion());
//     ReportResultResponseDto<Double> result =
//         reportClient.evaluateNumberReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(3L);
//     assertThat(result.getFirstMeasureData()).isEqualTo(100.);
//   }
//
//   @Test
//   public void percentageReportEvaluationForZeroInstances() {
//     // given
//     final ProcessDefinitionEngineDto definition = deploySimpleOneUserTasksDefinition();
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReport(definition.getKey(), definition.getVersionAsString());
//     ReportResultResponseDto<Double> result =
//         reportClient.evaluateNumberReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isZero();
//     assertThat(result.getFirstMeasureData()).isNull();
//   }
//
//   @Test
//   public void otherProcessDefinitionsNotInReportDoNotAffectResult() {
//     // given
//     ProcessInstanceEngineDto engineDto = deployAndStartSimpleServiceTaskProcess();
//     engineIntegrationExtension.startProcessInstance(engineDto.getDefinitionId());
//     deployAndStartSimpleServiceTaskProcess();
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReport(engineDto.getProcessDefinitionKey(),
// engineDto.getProcessDefinitionVersion());
//     ReportResultResponseDto<Double> result =
//         reportClient.evaluateNumberReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(2L);
//     assertThat(result.getFirstMeasureData()).isEqualTo(100.);
//   }
//
//   @Test
//   public void reportEvaluationOnlyConsidersSelectedTenants() {
//     // given
//     final String tenantId1 = "tenantId1";
//     final String tenantId2 = "tenantId2";
//     final List<String> selectedTenants = newArrayList(tenantId1);
//     final String processKey =
//         deployAndStartMultiTenantSimpleServiceTaskProcess(newArrayList(null, tenantId1,
// tenantId2));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData = createReport(processKey, ReportConstants.ALL_VERSIONS);
//     reportData.setTenantIds(selectedTenants);
//     ReportResultResponseDto<Double> result =
//         reportClient.evaluateNumberReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(1L);
//     assertThat(result.getFirstMeasureData()).isEqualTo(100.);
//   }
//
//   @Test
//   public void instancesFilteredOutByInstanceFilter() {
//     // given
//     ProcessInstanceEngineDto engineDto = deployAndStartSimpleUserTaskProcess();
//     engineIntegrationExtension.finishAllRunningUserTasks();
//     engineIntegrationExtension.startProcessInstance(engineDto.getDefinitionId());
//     engineIntegrationExtension.startProcessInstance(engineDto.getDefinitionId());
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReport(engineDto.getProcessDefinitionKey(),
// engineDto.getProcessDefinitionVersion());
//
// reportData.setFilter(ProcessFilterBuilder.filter().completedInstancesOnly().add().buildList());
//     ReportResultResponseDto<Double> result =
//         reportClient.evaluateNumberReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(1L);
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(3L);
//     assertThat(result.getFirstMeasureData()).isEqualTo(33.33333333333333);
//   }
//
//   @Test
//   public void instanceFilteredOutByDateInstanceFilter() {
//     // given
//     final OffsetDateTime now = DateCreationFreezer.dateFreezer().freezeDateAndReturn();
//     ProcessInstanceEngineDto processInstance = deployAndStartSimpleServiceTaskProcess();
//     engineDatabaseExtension.changeProcessInstanceStartDate(
//         processInstance.getId(), now.minusDays(1));
//     engineIntegrationExtension.startProcessInstance(processInstance.getDefinitionId());
//     engineIntegrationExtension.startProcessInstance(
//         processInstance.getDefinitionId(), Map.of("varName", "varVal"));
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReport(
//             processInstance.getProcessDefinitionKey(),
//             processInstance.getProcessDefinitionVersion());
//     reportData.setFilter(
//         ProcessFilterBuilder.filter()
//             .fixedInstanceStartDate()
//             .start(now.minusMinutes(10))
//             .end(null)
//             .add()
//             .variable()
//             .name("varName")
//             .stringType()
//             .values(Collections.singletonList("varVal"))
//             .operator(IN)
//             .add()
//             .buildList());
//     ReportResultResponseDto<Double> result =
//         reportClient.evaluateNumberReport(reportData).getResult();
//     final Integer storedInstanceCount =
//         databaseIntegrationTestExtension.getDocumentCountOf(
//             ProcessInstanceIndex.constructIndexName(processInstance.getProcessDefinitionKey()));
//
//     // then all three instance have been imported
//     assertThat(storedInstanceCount).isEqualTo(3);
//     // one is removed from baseline total as it doesn't match the instance level date filter
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2L);
//     // and only one matches the variable filter
//     assertThat(result.getInstanceCount()).isEqualTo(1L);
//     // and the percentage is relative to the baseline total
//     assertThat(result.getFirstMeasureData()).isEqualTo(50.);
//   }
//
//   @ParameterizedTest
//   @MethodSource("viewLevelFilters")
//   public void viewLevelFiltersOnlyAppliedToInstances(
//       final List<ProcessFilterDto<?>> filtersToApply) {
//     // given
//     ProcessDefinitionEngineDto processDefinition =
// deploySimpleServiceTaskProcessAndGetDefinition();
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     engineIntegrationExtension.startProcessInstance(processDefinition.getId());
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReport(processDefinition.getKey(), processDefinition.getVersionAsString());
//     reportData.getFilter().addAll(filtersToApply);
//     ReportResultResponseDto<Double> result =
//         reportClient.evaluateNumberReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isZero();
//     assertThat(result.getInstanceCountWithoutFilters()).isEqualTo(2L);
//     assertThat(result.getFirstMeasureData()).isEqualTo(0.);
//   }
//
//   @Test
//   public void optimizeExceptionOnViewPropertyIsNull() {
//     // given
//     ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
//     dataDto.getView().setProperties((ViewProperty) null);
//
//     // when
//     Response response = reportClient.evaluateReportAndReturnResponse(dataDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   @Test
//   public void optimizeExceptionOnGroupByTypeIsNull() {
//     // given
//     ProcessReportDataDto dataDto = createReport(PROCESS_DEFINITION_KEY, "1");
//     dataDto.getGroupBy().setType(null);
//
//     // when
//     Response response = reportClient.evaluateReportAndReturnResponse(dataDto);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//   }
//
//   private ProcessReportDataDto createReport(
//       String processDefinitionKey, String processDefinitionVersion) {
//     return TemplatedProcessReportDataBuilder.createReportData()
//         .setProcessDefinitionKey(processDefinitionKey)
//         .setProcessDefinitionVersion(processDefinitionVersion)
//         .setReportDataType(PROC_INST_PER_GROUP_BY_NONE)
//         .build();
//   }
// }
