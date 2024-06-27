/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package
// io.camunda.optimize.service.db.es.report.process.single.processinstance.duration.groupby.date.distributedby.none;
//
// import static io.camunda.optimize.test.util.DateModificationHelper.truncateToStartOfUnit;
// import static
// io.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurationsDefaultAggr;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.google.common.collect.Lists;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit;
// import
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RollingDateFilterStartDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.instance.RollingDateFilterDataDto;
// import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.InstanceEndDateFilterDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.util.ProcessReportDataType;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import java.time.Duration;
// import java.time.OffsetDateTime;
// import java.time.ZonedDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.List;
// import org.junit.jupiter.api.Test;
//
// public class ProcessInstanceDurationByInstanceEndDateReportEvaluationIT
//     extends AbstractProcessInstanceDurationByInstanceDateReportEvaluationIT {
//
//   @Override
//   protected ProcessReportDataType getTestReportDataType() {
//     return ProcessReportDataType.PROC_INST_DUR_GROUP_BY_END_DATE;
//   }
//
//   @Override
//   protected ProcessGroupByType getGroupByType() {
//     return ProcessGroupByType.END_DATE;
//   }
//
//   @Test
//   public void processInstancesEndedAtSameIntervalAreGroupedTogether() {
//     // given
//     final OffsetDateTime endDate = OffsetDateTime.now();
//     final OffsetDateTime startDate = endDate.minusDays(2);
//     final Duration between = Duration.between(startDate, endDate);
//
//     ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
//     String processDefinitionKey = processInstanceDto.getProcessDefinitionKey();
//     String processDefinitionVersion = processInstanceDto.getProcessDefinitionVersion();
//
//     adjustProcessInstanceDates(processInstanceDto.getId(), endDate, 0L, between.getSeconds() +
// 1L);
//     processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
//     adjustProcessInstanceDates(processInstanceDto.getId(), endDate, 0L, between.getSeconds() +
// 9L);
//     processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
//     adjustProcessInstanceDates(processInstanceDto.getId(), endDate, 0L, between.getSeconds() +
// 2L);
//     ProcessInstanceEngineDto processInstanceDto3 =
//         engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
//     adjustProcessInstanceDates(
//         processInstanceDto3.getId(), endDate, -1L, between.getSeconds() + 1L);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReportDataSortedDesc(
//             processDefinitionKey,
//             processDefinitionVersion,
//             getTestReportDataType(),
//             AggregateByDateUnit.DAY);
//     ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//     ZonedDateTime startOfEndDate = truncateToStartOfUnit(endDate, ChronoUnit.DAYS);
//     assertThat(resultData.get(0).getKey()).isEqualTo(localDateTimeToString(startOfEndDate));
//     assertThat(resultData.get(0).getValue())
//         .isEqualTo(
//             calculateExpectedValueGivenDurationsDefaultAggr(
//                 between.toMillis() + 1000.,
//                 between.toMillis() + 9000.,
//                 between.toMillis() + 2000.));
//     assertThat(resultData.get(1).getKey())
//         .isEqualTo(localDateTimeToString(startOfEndDate.minusDays(1)));
//     assertThat(resultData.get(1).getValue()).isEqualTo(between.toMillis() + 1000.);
//   }
//
//   @Test
//   public void testEmptyBucketsAreReturnedForEndDateFilterPeriod() {
//     // given
//     final OffsetDateTime endDate = OffsetDateTime.now();
//     final ProcessInstanceEngineDto processInstanceDto1 =
// deployAndStartSimpleServiceTaskProcess();
//     final String processDefinitionId = processInstanceDto1.getDefinitionId();
//     final String processDefinitionKey = processInstanceDto1.getProcessDefinitionKey();
//     final String processDefinitionVersion = processInstanceDto1.getProcessDefinitionVersion();
//     adjustProcessInstanceDates(processInstanceDto1.getId(), endDate, 0L, 1L);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinitionId);
//     adjustProcessInstanceDates(processInstanceDto2.getId(), endDate, -2L, 2L);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final RollingDateFilterDataDto dateFilterDataDto =
//         new RollingDateFilterDataDto(new RollingDateFilterStartDto(4L, DateUnit.DAYS));
//     final InstanceEndDateFilterDto endDateFilterDto = new InstanceEndDateFilterDto();
//     endDateFilterDto.setData(dateFilterDataDto);
//     endDateFilterDto.setFilterLevel(FilterApplicationLevel.INSTANCE);
//
//     final ProcessReportDataDto reportData =
//         createReportDataSortedDesc(
//             processDefinitionKey,
//             processDefinitionVersion,
//             getTestReportDataType(),
//             AggregateByDateUnit.DAY);
//     reportData.setFilter(Lists.newArrayList(endDateFilterDto));
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//     assertThat(resultData).hasSize(5);
//
//     assertThat(resultData.get(0).getKey())
//         .isEqualTo(embeddedOptimizeExtension.formatToHistogramBucketKey(endDate,
// ChronoUnit.DAYS));
//     assertThat(resultData.get(0).getValue()).isEqualTo(1000.);
//
//     assertThat(resultData.get(1).getKey())
//         .isEqualTo(
//             embeddedOptimizeExtension.formatToHistogramBucketKey(
//                 endDate.minusDays(1), ChronoUnit.DAYS));
//     assertThat(resultData.get(1).getValue()).isNull();
//
//     assertThat(resultData.get(2).getKey())
//         .isEqualTo(
//             embeddedOptimizeExtension.formatToHistogramBucketKey(
//                 endDate.minusDays(2), ChronoUnit.DAYS));
//     assertThat(resultData.get(2).getValue()).isEqualTo(2000.);
//
//     assertThat(resultData.get(3).getKey())
//         .isEqualTo(
//             embeddedOptimizeExtension.formatToHistogramBucketKey(
//                 endDate.minusDays(3), ChronoUnit.DAYS));
//     assertThat(resultData.get(3).getValue()).isNull();
//
//     assertThat(resultData.get(4).getKey())
//         .isEqualTo(
//             embeddedOptimizeExtension.formatToHistogramBucketKey(
//                 endDate.minusDays(4), ChronoUnit.DAYS));
//     assertThat(resultData.get(4).getValue()).isNull();
//   }
//
//   @Test
//   public void runningProcessInstancesAreNotConsideredInResults() {
//     // given
//     final ProcessInstanceEngineDto processInstanceDto1 = deployAndStartSimpleUserTaskProcess();
//
//     final String processDefinitionKey = processInstanceDto1.getProcessDefinitionKey();
//     final String processDefinitionVersion = processInstanceDto1.getProcessDefinitionVersion();
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setGroupByDateInterval(AggregateByDateUnit.DAY)
//             .setProcessDefinitionKey(processDefinitionKey)
//             .setProcessDefinitionVersion(processDefinitionVersion)
//             .setReportDataType(getTestReportDataType())
//             .build();
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//     assertThat(resultData).isEmpty();
//   }
//
//   @Test
//   public void evaluateReportWithSeveralRunningAndCompletedProcessInstances() {
//     // given 1 completed + 2 running process instances
//     final OffsetDateTime now = OffsetDateTime.now();
//
//     final ProcessDefinitionEngineDto processDefinition =
//         deployTwoRunningAndOneCompletedUserTaskProcesses(now);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setGroupByDateInterval(AggregateByDateUnit.DAY)
//             .setProcessDefinitionKey(processDefinition.getKey())
//             .setProcessDefinitionVersion(processDefinition.getVersionAsString())
//             .setReportDataType(getTestReportDataType())
//             .build();
//
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(1L);
//
//     final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//
//     assertThat(resultData).isNotNull().hasSize(1);
//
//     ZonedDateTime startOfToday = truncateToStartOfUnit(now, ChronoUnit.DAYS);
//     assertThat(resultData.get(0).getKey()).isEqualTo(localDateTimeToString(startOfToday));
//     assertThat(resultData.get(0).getValue()).isEqualTo(1000.);
//   }
// }
