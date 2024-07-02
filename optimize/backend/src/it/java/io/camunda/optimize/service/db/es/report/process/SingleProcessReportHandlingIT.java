/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.db.es.report.process;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static io.camunda.optimize.service.db.DatabaseConstants.SINGLE_PROCESS_REPORT_INDEX_NAME;
// import static
// io.camunda.optimize.service.util.InstanceIndexUtil.getProcessInstanceIndexAliasName;
// import static io.camunda.optimize.service.util.ProcessReportDataType.INCIDENT_DUR_GROUP_BY_NONE;
// import static io.camunda.optimize.service.util.ProcessReportDataType.INCIDENT_FREQ_GROUP_BY_NONE;
// import static io.camunda.optimize.service.util.ProcessReportDataType.PROC_INST_DUR_GROUP_BY_NONE;
// import static
// io.camunda.optimize.service.util.ProcessReportDataType.PROC_INST_DUR_GROUP_BY_NONE_WITH_PART;
// import static
// io.camunda.optimize.service.util.ProcessReportDataType.VARIABLE_AGGREGATION_GROUP_BY_NONE;
// import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
// import static io.camunda.optimize.util.BpmnModels.getSingleServiceTaskProcess;
// import static jakarta.ws.rs.HttpMethod.POST;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockserver.model.HttpRequest.request;
//
// import com.fasterxml.jackson.core.type.TypeReference;
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.ReportConstants;
// import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.configuration.SingleReportConfigurationDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.configuration.heatmap_target_value.HeatmapTargetValueEntryDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.configuration.process_part.ProcessPartDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.configuration.target_value.CountProgressDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.configuration.target_value.DurationProgressDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.configuration.target_value.SingleReportTargetValueDto;
// import io.camunda.optimize.dto.optimize.query.report.single.configuration.target_value.TargetDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.configuration.target_value.TargetValueUnit;
// import
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.variable.BooleanVariableFilterDataDto;
// import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
// import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.filter.VariableFilterDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessInstanceDto;
// import io.camunda.optimize.dto.optimize.query.variable.VariableType;
// import io.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.exception.OptimizeIntegrationTestException;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.rest.providers.ElasticsearchStatusExceptionMapper;
// import io.camunda.optimize.service.util.ProcessReportDataType;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import io.github.netmikey.logunit.api.LogCapturer;
// import jakarta.ws.rs.core.Response;
// import java.time.OffsetDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import java.util.Set;
// import lombok.SneakyThrows;
// import org.elasticsearch.ElasticsearchStatusException;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.RegisterExtension;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.EnumSource;
// import org.mockserver.integration.ClientAndServer;
// import org.mockserver.matchers.Times;
// import org.mockserver.model.HttpRequest;
// import org.mockserver.model.HttpResponse;
// import org.mockserver.model.HttpStatusCode;
// import org.slf4j.event.Level;
//
// @Tag(OPENSEARCH_PASSING)
// public class SingleProcessReportHandlingIT extends AbstractPlatformIT {
//
//   @RegisterExtension
//   protected final LogCapturer logCapturer =
//       LogCapturer.create()
//           .forLevel(Level.ERROR)
//           .captureForType(ElasticsearchStatusExceptionMapper.class);
//
//   @Test
//   public void reportIsWrittenToDatabase() {
//     // given
//     final String id = reportClient.createEmptySingleProcessReport();
//
//     // when
//     final Optional<SingleProcessReportDefinitionRequestDto> definitionDto =
//         databaseIntegrationTestExtension.getDatabaseEntryById(
//             SINGLE_PROCESS_REPORT_INDEX_NAME, id, SingleProcessReportDefinitionRequestDto.class);
//
//     // then
//     assertThat(definitionDto).isPresent();
//     final ProcessReportDataDto data = definitionDto.get().getData();
//     assertThat(data).isNotNull();
//     assertThat(data.getFilter()).isNotNull();
//     assertThat(data.getConfiguration()).isNotNull();
//     assertThat(data.getConfiguration()).isEqualTo(new SingleReportConfigurationDto());
//     assertThat(data.getConfiguration().getColor())
//         .isEqualTo(ReportConstants.DEFAULT_CONFIGURATION_COLOR);
//   }
//
//   @Test
//   public void writeAndThenReadGivesTheSameResult() {
//     // given
//     final String id = reportClient.createEmptySingleProcessReport();
//
//     // when
//     final List<ReportDefinitionDto> reports = getAllPrivateReports();
//
//     // then
//     assertThat(reports).isNotNull();
//     assertThat(reports.size()).isEqualTo(1);
//     assertThat(reports.get(0).getId()).isEqualTo(id);
//   }
//
//   @Test
//   public void createAndGetSeveralReports() {
//     // given
//     final String id = reportClient.createEmptySingleProcessReport();
//     final String id2 = reportClient.createEmptySingleProcessReport();
//     final Set<String> ids = new HashSet<>();
//     ids.add(id);
//     ids.add(id2);
//
//     // when
//     final List<ReportDefinitionDto> reports = getAllPrivateReports();
//
//     // then
//     assertThat(reports).isNotNull();
//     assertThat(reports.size()).isEqualTo(2);
//     final String reportId1 = reports.get(0).getId();
//     final String reportId2 = reports.get(1).getId();
//     assertThat(ids.contains(reportId1)).isTrue();
//     ids.remove(reportId1);
//     assertThat(ids.contains(reportId2)).isTrue();
//   }
//
//   @Test
//   public void noReportAvailableReturnsEmptyList() {
//
//     // when
//     final List<ReportDefinitionDto> reports = getAllPrivateReports();
//
//     // then
//     assertThat(reports).isNotNull();
//     assertThat(reports.isEmpty()).isTrue();
//   }
//
//   @Test
//   public void updateProcessReport() {
//     // given
//     final String shouldNotBeUpdatedString = "shouldNotBeUpdated";
//     final String id = reportClient.createEmptySingleProcessReport();
//     final ProcessReportDataDto reportData = new ProcessReportDataDto();
//     reportData.setProcessDefinitionKey("procdef");
//     reportData.setProcessDefinitionVersion("123");
//     reportData.setFilter(Collections.emptyList());
//     final SingleReportConfigurationDto configuration = new SingleReportConfigurationDto();
//     final SingleReportTargetValueDto singleReportTargetValueDto = new
// SingleReportTargetValueDto();
//     singleReportTargetValueDto.setIsKpi(true);
//     configuration.setTargetValue(singleReportTargetValueDto);
//     configuration.setLogScale(true);
//     configuration.setYLabel("fooYLabel");
//     reportData.setConfiguration(configuration);
//     final ProcessPartDto processPartDto = new ProcessPartDto();
//     processPartDto.setStart("start123");
//     processPartDto.setEnd("end123");
//     reportData.getConfiguration().setProcessPart(processPartDto);
//     final SingleProcessReportDefinitionRequestDto report =
//         new SingleProcessReportDefinitionRequestDto();
//     report.setData(reportData);
//     report.setId(shouldNotBeUpdatedString);
//     report.setLastModifier("shouldNotBeUpdatedManually");
//     report.setName("MyReport");
//     final OffsetDateTime shouldBeIgnoredDate = OffsetDateTime.now().plusHours(1);
//     report.setCreated(shouldBeIgnoredDate);
//     report.setLastModified(shouldBeIgnoredDate);
//     report.setOwner(shouldNotBeUpdatedString);
//
//     // when
//     reportClient.updateSingleProcessReport(id, report);
//     final List<ReportDefinitionDto> reports = getAllPrivateReports();
//
//     // then
//     assertThat(reports.size()).isEqualTo(1);
//     final SingleProcessReportDefinitionRequestDto newReport =
//         (SingleProcessReportDefinitionRequestDto) reports.get(0);
//     assertThat(newReport.getData().getProcessDefinitionKey()).isEqualTo("procdef");
//     assertThat(newReport.getData().getDefinitionVersions()).containsExactly("123");
//     assertThat(newReport.getData().getConfiguration().getLogScale()).isTrue();
//     assertThat(newReport.getData().getConfiguration().getYLabel()).isEqualTo("fooYLabel");
//     assertThat(newReport.getData().getConfiguration().getProcessPart()).isNotEmpty();
//     assertThat(newReport.getData().getConfiguration().getProcessPart().get().getStart())
//         .isEqualTo("start123");
//     assertThat(newReport.getData().getConfiguration().getProcessPart().get().getEnd())
//         .isEqualTo("end123");
//
// assertThat(newReport.getData().getConfiguration().getTargetValue().getIsKpi()).isEqualTo(true);
//     assertThat(newReport.getId()).isEqualTo(id);
//     assertThat(newReport.getCreated()).isNotEqualTo(shouldBeIgnoredDate);
//     assertThat(newReport.getLastModified()).isNotEqualTo(shouldBeIgnoredDate);
//     assertThat(newReport.getName()).isEqualTo("MyReport");
//     assertThat(newReport.getOwner()).isEqualTo(DEFAULT_FULLNAME);
//   }
//
//   @Test
//   public void updateProcessReportICanSetAboveBelowValuesOnProgressBars() {
//     // given
//     final String shouldNotBeUpdatedString = "shouldNotBeUpdated";
//     final String id = reportClient.createEmptySingleProcessReport();
//     final ProcessReportDataDto reportData = new ProcessReportDataDto();
//     reportData.setProcessDefinitionKey("procdef");
//     reportData.setProcessDefinitionVersion("123");
//     reportData.setFilter(Collections.emptyList());
//     final SingleReportConfigurationDto configuration = new SingleReportConfigurationDto();
//     final SingleReportTargetValueDto singleReportTargetValueDto = new
// SingleReportTargetValueDto();
//     singleReportTargetValueDto.setIsKpi(true);
//     final CountProgressDto countProgressDto = new CountProgressDto();
//     countProgressDto.setTarget("10");
//     countProgressDto.setIsBelow(true);
//     singleReportTargetValueDto.setCountProgress(countProgressDto);
//     final TargetDto targetDto = new TargetDto();
//     targetDto.setIsBelow(true);
//     targetDto.setValue("20");
//     final DurationProgressDto durationProgressDto = new DurationProgressDto();
//     durationProgressDto.setTarget(targetDto);
//     singleReportTargetValueDto.setDurationProgress(durationProgressDto);
//     configuration.setTargetValue(singleReportTargetValueDto);
//     configuration.setLogScale(true);
//     reportData.setConfiguration(configuration);
//     final ProcessPartDto processPartDto = new ProcessPartDto();
//     processPartDto.setStart("start123");
//     processPartDto.setEnd("end123");
//     reportData.getConfiguration().setProcessPart(processPartDto);
//     final SingleProcessReportDefinitionRequestDto report =
//         new SingleProcessReportDefinitionRequestDto();
//     report.setData(reportData);
//     report.setId(shouldNotBeUpdatedString);
//     report.setLastModifier("shouldNotBeUpdatedManually");
//     report.setName("MyReport");
//
//     // when
//     reportClient.updateSingleProcessReport(id, report);
//     final List<ReportDefinitionDto> reports = getAllPrivateReports();
//
//     // then
//     assertThat(reports.size()).isEqualTo(1);
//     final SingleProcessReportDefinitionRequestDto newReport =
//         (SingleProcessReportDefinitionRequestDto) reports.get(0);
//     assertThat(newReport.getData().getProcessDefinitionKey()).isEqualTo("procdef");
//     assertThat(newReport.getData().getDefinitionVersions()).containsExactly("123");
//     assertThat(newReport.getData().getConfiguration().getLogScale()).isTrue();
//     assertThat(newReport.getData().getConfiguration().getProcessPart()).isNotEmpty();
//     assertThat(newReport.getData().getConfiguration().getProcessPart().get().getStart())
//         .isEqualTo("start123");
//     assertThat(newReport.getData().getConfiguration().getProcessPart().get().getEnd())
//         .isEqualTo("end123");
//     assertThat(newReport.getData().getConfiguration().getTargetValue().getIsKpi()).isTrue();
//     assertThat(
//
// newReport.getData().getConfiguration().getTargetValue().getCountProgress().getIsBelow())
//         .isTrue();
//     assertThat(
//
// newReport.getData().getConfiguration().getTargetValue().getCountProgress().getTarget())
//         .isEqualTo("10");
//     assertThat(
//             newReport
//                 .getData()
//                 .getConfiguration()
//                 .getTargetValue()
//                 .getDurationProgress()
//                 .getTarget()
//                 .getValue())
//         .isEqualTo("20");
//     assertThat(
//             newReport
//                 .getData()
//                 .getConfiguration()
//                 .getTargetValue()
//                 .getDurationProgress()
//                 .getTarget()
//                 .getIsBelow())
//         .isTrue();
//     assertThat(newReport.getId()).isEqualTo(id);
//     assertThat(newReport.getName()).isEqualTo("MyReport");
//     assertThat(newReport.getOwner()).isEqualTo(DEFAULT_FULLNAME);
//   }
//
//   @Test
//   public void updateProcessReportRemoveHeatMapTargetValue() {
//     // given
//     final String id = reportClient.createEmptySingleProcessReport();
//     final ProcessReportDataDto reportData = new ProcessReportDataDto();
//     reportData.setProcessDefinitionKey("procdef");
//     reportData.setProcessDefinitionVersion("123");
//     reportData.setFilter(Collections.emptyList());
//     final SingleReportConfigurationDto configuration = new SingleReportConfigurationDto();
//     configuration
//         .getHeatmapTargetValue()
//         .getValues()
//         .put("flowNodeId", new HeatmapTargetValueEntryDto(TargetValueUnit.DAYS, "55"));
//     reportData.setConfiguration(configuration);
//
//     final SingleProcessReportDefinitionRequestDto report =
//         new SingleProcessReportDefinitionRequestDto();
//     report.setData(reportData);
//     report.setId("shouldNotBeUpdated");
//     report.setLastModifier("shouldNotBeUpdatedManually");
//     report.setName("MyReport");
//     report.setOwner("NewOwner");
//     reportClient.updateSingleProcessReport(id, report);
//
//     // when
//     configuration.getHeatmapTargetValue().setValues(new HashMap<>());
//     reportClient.updateSingleProcessReport(id, report);
//     final List<ReportDefinitionDto> reports = getAllPrivateReports();
//
//     // then
//     assertThat(reports.size()).isEqualTo(1);
//     final SingleProcessReportDefinitionRequestDto newReport =
//         (SingleProcessReportDefinitionRequestDto) reports.get(0);
//     assertThat(newReport.getData().getConfiguration().getHeatmapTargetValue().getValues())
//         .isEmpty();
//   }
//
//   @Test
//   public void updateReportWithoutPDInformation() {
//     // given
//     final String id = reportClient.createEmptySingleProcessReport();
//     final SingleProcessReportDefinitionRequestDto updatedReport =
//         new SingleProcessReportDefinitionRequestDto();
//     updatedReport.setData(new ProcessReportDataDto());
//
//     // when
//     Response updateReportResponse = reportClient.updateSingleProcessReport(id, updatedReport);
//
//     // then
//     assertThat(updateReportResponse.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//
//     // when
//     ProcessReportDataDto data = new ProcessReportDataDto();
//     data.setProcessDefinitionVersion("BLAH");
//     updatedReport.setData(data);
//     updateReportResponse = reportClient.updateSingleProcessReport(id, updatedReport);
//
//     // then
//     assertThat(updateReportResponse.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//
//     // when
//     data = new ProcessReportDataDto();
//     data.setProcessDefinitionKey("BLAH");
//     updatedReport.setData(data);
//     updateReportResponse = reportClient.updateSingleProcessReport(id, updatedReport);
//
//     // then
//     assertThat(updateReportResponse.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//
//     // when
//     data = new ProcessReportDataDto();
//     data.setProcessDefinitionKey("BLAH");
//     data.setProcessDefinitionVersion("BLAH");
//     updatedReport.setData(data);
//     updateReportResponse = reportClient.updateSingleProcessReport(id, updatedReport);
//
//     // then
//     assertThat(updateReportResponse.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//   }
//
//   @Test
//   public void updateReportWithFilters() {
//     // given
//     final String id = reportClient.createEmptySingleProcessReport();
//     ProcessReportDataDto reportData = new ProcessReportDataDto();
//     reportData.setProcessDefinitionKey("procdef");
//     reportData.setProcessDefinitionVersion("123");
//
//     reportData
//         .getFilter()
//         .addAll(
//             ProcessFilterBuilder.filter()
//                 .fixedInstanceStartDate()
//                 .start(OffsetDateTime.now().minusDays(1L))
//                 .end(OffsetDateTime.now())
//                 .filterLevel(FilterApplicationLevel.INSTANCE)
//                 .add()
//                 .buildList());
//     reportData.getFilter().addAll(createVariableFilter());
//     reportData.getFilter().addAll(createExecutedFlowNodeFilter());
//     final SingleProcessReportDefinitionRequestDto report =
//         new SingleProcessReportDefinitionRequestDto();
//     report.setData(reportData);
//     report.setId("shouldNotBeUpdated");
//     report.setLastModifier("shouldNotBeUpdatedManually");
//     report.setName("MyReport");
//     final OffsetDateTime shouldBeIgnoredDate = OffsetDateTime.now().plusHours(1);
//     report.setCreated(shouldBeIgnoredDate);
//     report.setLastModified(shouldBeIgnoredDate);
//     report.setOwner("NewOwner");
//
//     // when
//     reportClient.updateSingleProcessReport(id, report);
//     final List<ReportDefinitionDto> reports = getAllPrivateReports();
//
//     // then
//     assertThat(reports.size()).isEqualTo(1);
//     final SingleProcessReportDefinitionRequestDto newReport =
//         (SingleProcessReportDefinitionRequestDto) reports.get(0);
//     assertThat(newReport.getData()).isNotNull();
//     reportData = newReport.getData();
//     assertThat(reportData.getFilter().size()).isEqualTo(3);
//   }
//
//   private List<ProcessFilterDto<?>> createVariableFilter() {
//     final VariableFilterDto variableFilterDto = new VariableFilterDto();
//     variableFilterDto.setData(
//         new BooleanVariableFilterDataDto("foo", Collections.singletonList(true)));
//     variableFilterDto.setFilterLevel(FilterApplicationLevel.INSTANCE);
//     return Collections.singletonList(variableFilterDto);
//   }
//
//   private List<ProcessFilterDto<?>> createExecutedFlowNodeFilter() {
//     final List<ProcessFilterDto<?>> flowNodeFilter =
//         ProcessFilterBuilder.filter()
//             .executedFlowNodes()
//             .id("task1")
//             .filterLevel(FilterApplicationLevel.INSTANCE)
//             .add()
//             .buildList();
//     return new ArrayList<>(flowNodeFilter);
//   }
//
//   @Test
//   public void doNotUpdateNullFieldsInReport() {
//     // given
//     final String id = reportClient.createEmptySingleProcessReport();
//     final SingleProcessReportDefinitionRequestDto report =
// constructSingleProcessReportWithFakePD();
//
//     // when
//     reportClient.updateSingleProcessReport(id, report);
//     final List<ReportDefinitionDto> reports = getAllPrivateReports();
//
//     // then
//     assertThat(reports.size()).isEqualTo(1);
//     final ReportDefinitionDto newDashboard = reports.get(0);
//     assertThat(newDashboard.getId()).isEqualTo(id);
//     assertThat(newDashboard.getCreated()).isNotNull();
//     assertThat(newDashboard.getLastModified()).isNotNull();
//     assertThat(newDashboard.getLastModifier()).isNotNull();
//     assertThat(newDashboard.getName()).isNotNull();
//     assertThat(newDashboard.getOwner()).isNotNull();
//   }
//
//   @Test
//   public void reportEvaluationReturnsMetaData() {
//     // given
//     final String reportId = reportClient.createEmptySingleProcessReport();
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setProcessDefinitionKey("fooProcessDefinitionKey")
//             .setProcessDefinitionVersion("1")
//             .setReportDataType(ProcessReportDataType.RAW_DATA)
//             .build();
//     final SingleProcessReportDefinitionRequestDto report =
//         new SingleProcessReportDefinitionRequestDto();
//     report.setData(reportData);
//     report.setName("name");
//     final OffsetDateTime now = OffsetDateTime.now();
//     reportClient.updateSingleProcessReport(reportId, report);
//
//     // when
//     final AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>> result =
//         reportClient.evaluateRawReportById(reportId);
//
//     // then
//     final SingleProcessReportDefinitionRequestDto reportDefinition =
// result.getReportDefinition();
//     assertThat(reportDefinition.getId()).isEqualTo(reportId);
//     assertThat(reportDefinition.getName()).isEqualTo("name");
//     assertThat(reportDefinition.getOwner()).isEqualTo(DEFAULT_FULLNAME);
//     assertThat(reportDefinition.getCreated().truncatedTo(ChronoUnit.DAYS))
//         .isEqualTo(now.truncatedTo(ChronoUnit.DAYS));
//     assertThat(reportDefinition.getLastModifier()).isEqualTo(DEFAULT_FULLNAME);
//     assertThat(reportDefinition.getLastModified().truncatedTo(ChronoUnit.DAYS))
//         .isEqualTo(now.truncatedTo(ChronoUnit.DAYS));
//   }
//
//   @Test
//   public void evaluateReportWithoutVisualization() {
//     // given
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setProcessDefinitionKey("foo")
//             .setProcessDefinitionVersion("1")
//             .setReportDataType(PROC_INST_DUR_GROUP_BY_NONE)
//             .build();
//     reportData.setVisualization(null);
//
//     // when
//     final Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildEvaluateSingleUnsavedReportRequest(reportData)
//             .execute();
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
//   }
//
//   @ParameterizedTest
//   @EnumSource(ProcessReportDataType.class)
//   public void evaluateReport_missingInstanceIndicesReturnsEmptyResult(
//       final ProcessReportDataType reportType) {
//     // given
//     final String reportId = deployDefinitionAndCreateReport(reportType);
//
//     // when
//     final ReportResultResponseDto<?> result =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildEvaluateSavedReportRequest(reportId)
//             .execute(new TypeReference<AuthorizedProcessReportEvaluationResponseDto<?>>() {})
//             .getResult();
//
//     // then
//     assertEmptyResult(reportType, result);
//   }
//
//   @Test
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void reportEvaluationWithTooManyBuckets_throwsTooManyBucketsException() {
//     // given a distributed by report with that exceeds the ES bucket limit
//     final Map<String, Object> variables = Collections.singletonMap("doubleVar", 1.0);
//     final ProcessInstanceEngineDto procInst1 = deployAndStartSimpleProcess(variables);
//     final ProcessInstanceEngineDto procInst2 =
//         engineIntegrationExtension.startProcessInstance(procInst1.getDefinitionId(), variables);
//     final ProcessInstanceEngineDto procInst3 =
//         engineIntegrationExtension.startProcessInstance(
//             procInst1.getDefinitionId(), Collections.singletonMap("doubleVar", 66000.0));
//
//     importAllEngineEntitiesFromScratch();
//
//     // when grouped by variable with bucket size of 1.0 (values ranges 1-66k makes 66k buckets)
//     // which exceeds the elastic limit of 65k
//     //
// https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket.html
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setReportDataType(ProcessReportDataType.PROC_INST_FREQ_GROUP_BY_VARIABLE)
//             .setProcessDefinitionKey(procInst1.getProcessDefinitionKey())
//             .setProcessDefinitionVersion(procInst1.getProcessDefinitionVersion())
//             .setVariableType(VariableType.DOUBLE)
//             .setVariableName("doubleVar")
//             .build();
//     reportData.getConfiguration().getCustomBucket().setActive(true);
//     reportData.getConfiguration().getCustomBucket().setBucketSize(1.0);
//     reportData.getConfiguration().getCustomBucket().setBaseline(0.0);
//     final Response response = reportClient.evaluateReportAndReturnResponse(reportData);
//
//     // then the response has the correct error code
//     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
//     assertThat(response.readEntity(ErrorResponseDto.class).getErrorCode())
//         .isEqualTo("tooManyBuckets");
//     assertThat(response.readEntity(ErrorResponseDto.class).getErrorMessage())
//         .isEqualTo(
//             "Could not evaluate report because the result has more than 10.000 data points.
// Please add filters or adjust "
//                 + "the bucket size to reduce the size of the report result.");
//     assertThat(response.readEntity(ErrorResponseDto.class).getReportDefinition()).isNotNull();
//   }
//
//   @Test
//   @SneakyThrows
//   @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
//   public void databaseStatusExceptionIsMapped() {
//     // given a report evaluation that throws an ElasticsearchStatusException
//     final String definitionKey = "someKey";
//     final ClientAndServer dbMockServer = useAndGetDbMockServer();
//     final HttpRequest requestMatcher =
//         request()
//             .withPath("/.*" + getProcessInstanceIndexAliasName(definitionKey) + "/_search")
//             .withMethod(POST);
//     // the request throws an exception which results in an ElasticsearchStatusException
//     dbMockServer
//         .when(requestMatcher, Times.once())
//         .respond(
//             (new HttpResponse())
//                 .withStatusCode(HttpStatusCode.BAD_REQUEST_400.code())
//                 .withReasonPhrase(HttpStatusCode.BAD_REQUEST_400.reasonPhrase()));
//
//     // when
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setReportDataType(ProcessReportDataType.RAW_DATA)
//             .setProcessDefinitionKey(definitionKey)
//             .setProcessDefinitionVersion(ReportConstants.ALL_VERSIONS)
//             .build();
//     final Response response = reportClient.evaluateReportAndReturnResponse(reportData);
//
//     // then the response has the correct error code
//     assertThat(response.getStatus())
//         .isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//     assertThat(response.readEntity(ErrorResponseDto.class).getErrorCode())
//         .isEqualTo("elasticsearchError");
//
//     // and the original exception was logged
//     final Throwable elasticsearchStatusException =
//         logCapturer.assertContains("Mapping ElasticsearchStatusException").getThrowable();
//     assertThat(elasticsearchStatusException).isInstanceOf(ElasticsearchStatusException.class);
//   }
//
//   private void assertEmptyResult(
//       final ProcessReportDataType reportType, final ReportResultResponseDto<?> result) {
//     assertThat(result.getInstanceCount()).isZero();
//     assertThat(result.getInstanceCountWithoutFilters()).isZero();
//     if (isNullResultExpected(reportType)) {
//       assertThat(result.getFirstMeasureData()).isNull();
//     } else if (result.getFirstMeasureData() instanceof List) {
//       assertThat((List<?>) result.getFirstMeasureData()).isEmpty();
//     } else if (result.getFirstMeasureData() instanceof Double) {
//       assertThat((Double) result.getFirstMeasureData()).isZero();
//     } else {
//       throw new OptimizeIntegrationTestException(
//           "Unexpected result type: " + result.getFirstMeasureData().getClass());
//     }
//   }
//
//   private SingleProcessReportDefinitionRequestDto constructSingleProcessReportWithFakePD() {
//     final SingleProcessReportDefinitionRequestDto reportDefinitionDto =
//         new SingleProcessReportDefinitionRequestDto();
//     final ProcessReportDataDto data = new ProcessReportDataDto();
//     data.setProcessDefinitionVersion("FAKE");
//     data.setProcessDefinitionKey("FAKE");
//     reportDefinitionDto.setData(data);
//     return reportDefinitionDto;
//   }
//
//   private List<ReportDefinitionDto> getAllPrivateReports() {
//     return embeddedOptimizeExtension
//         .getRequestExecutor()
//         .buildGetAllPrivateReportsRequest()
//         .executeAndReturnList(ReportDefinitionDto.class, Response.Status.OK.getStatusCode());
//   }
//
//   private ProcessInstanceEngineDto deployAndStartSimpleProcess(
//       final Map<String, Object> variables) {
//     final ProcessDefinitionEngineDto processDefinition =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             getSingleServiceTaskProcess());
//     final ProcessInstanceEngineDto processInstanceEngineDto =
//         engineIntegrationExtension.startProcessInstance(processDefinition.getId(), variables);
//     processInstanceEngineDto.setProcessDefinitionKey(processDefinition.getKey());
//     processInstanceEngineDto.setProcessDefinitionVersion(
//         String.valueOf(processDefinition.getVersion()));
//     return processInstanceEngineDto;
//   }
//
//   private String deployDefinitionAndCreateReport(final ProcessReportDataType reportType) {
//     final ProcessDefinitionEngineDto processDefinition =
//         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//             getSingleServiceTaskProcess(
//                 "TestProcess_evaluateReport_missingInstanceIndicesReturnsEmptyResult"));
//
//     final ProcessReportDataDto expectedReportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setProcessDefinitionKey(processDefinition.getKey())
//             .setProcessDefinitionVersion("1")
//             .setReportDataType(reportType)
//             .setVariableName("variableName")
//             .setVariableType(VariableType.INTEGER)
//             .setGroupByDateVariableUnit(AggregateByDateUnit.AUTOMATIC)
//             .setGroupByDateInterval(AggregateByDateUnit.AUTOMATIC)
//             .setDistributeByDateInterval(AggregateByDateUnit.AUTOMATIC)
//             .build();
//     final SingleProcessReportDefinitionRequestDto report =
//         new SingleProcessReportDefinitionRequestDto();
//     report.setData(expectedReportData);
//     return reportClient.createSingleProcessReport(report);
//   }
//
//   private boolean isNullResultExpected(final ProcessReportDataType reportDataType) {
//     return PROC_INST_DUR_GROUP_BY_NONE.equals(reportDataType)
//         || PROC_INST_DUR_GROUP_BY_NONE_WITH_PART.equals(reportDataType)
//         || INCIDENT_DUR_GROUP_BY_NONE.equals(reportDataType)
//         || VARIABLE_AGGREGATION_GROUP_BY_NONE.equals(reportDataType)
//         || INCIDENT_FREQ_GROUP_BY_NONE.equals(reportDataType);
//   }
// }
