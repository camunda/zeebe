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
// import static
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.ComparisonOperator.GREATER_THAN_EQUALS;
// import static
// io.camunda.optimize.service.util.ProcessReportDataType.PROC_INST_DUR_GROUP_BY_START_DATE;
// import static io.camunda.optimize.test.util.DateModificationHelper.truncateToStartOfUnit;
// import static
// io.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurationsDefaultAggr;
// import static java.time.temporal.ChronoUnit.MILLIS;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.google.common.collect.Lists;
// import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit;
// import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DurationUnit;
// import
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RollingDateFilterStartDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.instance.RollingDateFilterDataDto;
// import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.InstanceStartDateFilterDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
// import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
// import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
// import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
// import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
// import io.camunda.optimize.service.security.util.LocalDateUtil;
// import io.camunda.optimize.service.util.ProcessReportDataType;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import java.time.OffsetDateTime;
// import java.time.ZonedDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.List;
// import org.junit.jupiter.api.Test;
//
// public class ProcessInstanceDurationByInstanceStartDateReportEvaluationIT
//     extends AbstractProcessInstanceDurationByInstanceDateReportEvaluationIT {
//
//   @Override
//   protected ProcessReportDataType getTestReportDataType() {
//     return ProcessReportDataType.PROC_INST_DUR_GROUP_BY_START_DATE;
//   }
//
//   @Override
//   protected ProcessGroupByType getGroupByType() {
//     return ProcessGroupByType.START_DATE;
//   }
//
//   @Test
//   public void processInstancesStartedAtSameIntervalAreGroupedTogether() {
//     // given
//     OffsetDateTime startDate = OffsetDateTime.now();
//     ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
//     String processDefinitionKey = processInstanceDto.getProcessDefinitionKey();
//     String processDefinitionVersion = processInstanceDto.getProcessDefinitionVersion();
//
//     adjustProcessInstanceDates(processInstanceDto.getId(), startDate, 0L, 1L);
//     processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
//     adjustProcessInstanceDates(processInstanceDto.getId(), startDate, 0L, 9L);
//     processInstanceDto =
//         engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
//     adjustProcessInstanceDates(processInstanceDto.getId(), startDate, 0L, 2L);
//     ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processInstanceDto.getDefinitionId());
//     adjustProcessInstanceDates(processInstanceDto2.getId(), startDate, -1L, 1L);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReportDataSortedDesc(
//             processDefinitionKey,
//             processDefinitionVersion,
//             PROC_INST_DUR_GROUP_BY_START_DATE,
//             AggregateByDateUnit.DAY);
//     ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//     ZonedDateTime startOfToday = truncateToStartOfUnit(startDate, ChronoUnit.DAYS);
//     assertThat(resultData.get(0).getKey()).isEqualTo(localDateTimeToString(startOfToday));
//     assertThat(resultData.get(0).getValue())
//         .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(1000., 9000., 2000.));
//     assertThat(resultData.get(1).getKey())
//         .isEqualTo(localDateTimeToString(startOfToday.minusDays(1)));
//     assertThat(resultData.get(1).getValue())
//         .isEqualTo(calculateExpectedValueGivenDurationsDefaultAggr(1000.));
//   }
//
//   @Test
//   public void testEmptyBucketsAreReturnedForStartDateFilterPeriod() {
//     // given
//     final OffsetDateTime startDate = OffsetDateTime.now();
//     final ProcessInstanceEngineDto processInstanceDto1 =
// deployAndStartSimpleServiceTaskProcess();
//     final String processDefinitionId = processInstanceDto1.getDefinitionId();
//     final String processDefinitionKey = processInstanceDto1.getProcessDefinitionKey();
//     final String processDefinitionVersion = processInstanceDto1.getProcessDefinitionVersion();
//     adjustProcessInstanceDates(processInstanceDto1.getId(), startDate, 0L, 1L);
//
//     final ProcessInstanceEngineDto processInstanceDto2 =
//         engineIntegrationExtension.startProcessInstance(processDefinitionId);
//     adjustProcessInstanceDates(processInstanceDto2.getId(), startDate, -2L, 2L);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final RollingDateFilterDataDto dateFilterDataDto =
//         new RollingDateFilterDataDto(new RollingDateFilterStartDto(4L, DateUnit.DAYS));
//     final InstanceStartDateFilterDto startDateFilterDto = new InstanceStartDateFilterDto();
//     startDateFilterDto.setData(dateFilterDataDto);
//     startDateFilterDto.setFilterLevel(FilterApplicationLevel.INSTANCE);
//
//     final ProcessReportDataDto reportData =
//         createReportDataSortedDesc(
//             processDefinitionKey,
//             processDefinitionVersion,
//             PROC_INST_DUR_GROUP_BY_START_DATE,
//             AggregateByDateUnit.DAY);
//     reportData.setFilter(Lists.newArrayList(startDateFilterDto));
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//     assertThat(resultData).hasSize(5);
//
//     assertThat(resultData.get(0).getKey())
//         .isEqualTo(
//             embeddedOptimizeExtension.formatToHistogramBucketKey(startDate, ChronoUnit.DAYS));
//     assertThat(resultData.get(0).getValue()).isEqualTo(1000.);
//
//     assertThat(resultData.get(1).getKey())
//         .isEqualTo(
//             embeddedOptimizeExtension.formatToHistogramBucketKey(
//                 startDate.minusDays(1), ChronoUnit.DAYS));
//     assertThat(resultData.get(1).getValue()).isNull();
//
//     assertThat(resultData.get(2).getKey())
//         .isEqualTo(
//             embeddedOptimizeExtension.formatToHistogramBucketKey(
//                 startDate.minusDays(2), ChronoUnit.DAYS));
//     assertThat(resultData.get(2).getValue()).isEqualTo(2000.);
//
//     assertThat(resultData.get(3).getKey())
//         .isEqualTo(
//             embeddedOptimizeExtension.formatToHistogramBucketKey(
//                 startDate.minusDays(3), ChronoUnit.DAYS));
//     assertThat(resultData.get(3).getValue()).isNull();
//
//     assertThat(resultData.get(4).getKey())
//         .isEqualTo(
//             embeddedOptimizeExtension.formatToHistogramBucketKey(
//                 startDate.minusDays(4), ChronoUnit.DAYS));
//     assertThat(resultData.get(4).getValue()).isNull();
//   }
//
//   @Test
//   public void evaluateReportWithSeveralRunningAndCompletedProcessInstances() {
//     // given 1 completed + 2 running process instances
//     final OffsetDateTime now = OffsetDateTime.now();
//     LocalDateUtil.setCurrentTime(now);
//
//     final ProcessDefinitionEngineDto processDefinition =
//         deployTwoRunningAndOneCompletedUserTaskProcesses(now);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     ProcessReportDataDto reportData =
//         createReportDataSortedDesc(
//             processDefinition.getKey(),
//             processDefinition.getVersionAsString(),
//             getTestReportDataType(),
//             AggregateByDateUnit.DAY);
//     final ReportResultResponseDto<List<MapResultEntryDto>> result =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     assertThat(result.getInstanceCount()).isEqualTo(3L);
//
//     final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
//
//     assertThat(resultData).isNotNull().hasSize(3);
//
//     assertThat(resultData.get(0).getKey())
//         .isEqualTo(localDateTimeToString(truncateToStartOfUnit(now, ChronoUnit.DAYS)));
//     assertThat(resultData.get(0).getValue()).isEqualTo(1000.);
//
//     assertThat(resultData.get(1).getKey())
//         .isEqualTo(localDateTimeToString(truncateToStartOfUnit(now.minusDays(1),
// ChronoUnit.DAYS)));
//     assertThat(resultData.get(1).getValue())
//         .isEqualTo((double) now.minusDays(1).until(now, MILLIS));
//
//     assertThat(resultData.get(2).getKey())
//         .isEqualTo(localDateTimeToString(truncateToStartOfUnit(now.minusDays(2),
// ChronoUnit.DAYS)));
//     assertThat(resultData.get(2).getValue())
//         .isEqualTo((double) now.minusDays(2).until(now, MILLIS));
//   }
//
//   @Test
//   public void calculateDurationForRunningProcessInstances() {
//     // given
//     OffsetDateTime now = OffsetDateTime.now();
//     LocalDateUtil.setCurrentTime(now);
//
//     // 1 completed proc inst
//     ProcessInstanceEngineDto completeProcessInstanceDto = deployAndStartSimpleUserTaskProcess();
//     engineIntegrationExtension.finishAllRunningUserTasks(completeProcessInstanceDto.getId());
//
//     OffsetDateTime completedProcInstStartDate = now.minusDays(2);
//     OffsetDateTime completedProcInstEndDate = completedProcInstStartDate.plus(1000, MILLIS);
//     engineDatabaseExtension.changeProcessInstanceStartDate(
//         completeProcessInstanceDto.getId(), completedProcInstStartDate);
//     engineDatabaseExtension.changeProcessInstanceEndDate(
//         completeProcessInstanceDto.getId(), completedProcInstEndDate);
//
//     // 1 running proc inst
//     final ProcessInstanceEngineDto newRunningProcessInstance =
//         engineIntegrationExtension.startProcessInstance(
//             completeProcessInstanceDto.getDefinitionId());
//     OffsetDateTime runningProcInstStartDate = now.minusDays(1);
//     engineDatabaseExtension.changeProcessInstanceStartDate(
//         newRunningProcessInstance.getId(), runningProcInstStartDate);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final List<ProcessFilterDto<?>> runningInstanceFilter =
//         ProcessFilterBuilder.filter().runningInstancesOnly().add().buildList();
//
//     ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setReportDataType(getTestReportDataType())
//
// .setProcessDefinitionVersion(completeProcessInstanceDto.getProcessDefinitionVersion())
//             .setProcessDefinitionKey(completeProcessInstanceDto.getProcessDefinitionKey())
//             .setGroupByDateInterval(AggregateByDateUnit.DAY)
//             .setFilter(runningInstanceFilter)
//             .build();
//     ReportResultResponseDto<List<MapResultEntryDto>> resultDto =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     final List<MapResultEntryDto> resultData = resultDto.getFirstMeasureData();
//     assertThat(resultData).isNotNull().hasSize(1);
//     assertThat(resultData.get(0).getValue())
//         .isEqualTo((double) runningProcInstStartDate.until(now, MILLIS));
//   }
//
//   @Test
//   public void durationFilterWorksForRunningProcessInstances() {
//     // given
//     OffsetDateTime now = OffsetDateTime.now();
//     LocalDateUtil.setCurrentTime(now);
//
//     // 1 completed proc inst
//     ProcessInstanceEngineDto completeProcessInstanceDto = deployAndStartSimpleUserTaskProcess();
//     engineIntegrationExtension.finishAllRunningUserTasks(completeProcessInstanceDto.getId());
//
//     OffsetDateTime completedProcInstStartDate = now.minusDays(2);
//     OffsetDateTime completedProcInstEndDate = completedProcInstStartDate.plus(1000, MILLIS);
//     engineDatabaseExtension.changeProcessInstanceStartDate(
//         completeProcessInstanceDto.getId(), completedProcInstStartDate);
//     engineDatabaseExtension.changeProcessInstanceEndDate(
//         completeProcessInstanceDto.getId(), completedProcInstEndDate);
//
//     // 1 running proc inst
//     final ProcessInstanceEngineDto newRunningProcessInstance =
//         engineIntegrationExtension.startProcessInstance(
//             completeProcessInstanceDto.getDefinitionId());
//     OffsetDateTime runningProcInstStartDate = now.minusDays(1);
//     engineDatabaseExtension.changeProcessInstanceStartDate(
//         newRunningProcessInstance.getId(), runningProcInstStartDate);
//
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     final List<ProcessFilterDto<?>> durationFilter =
//         ProcessFilterBuilder.filter()
//             .duration()
//             .operator(GREATER_THAN_EQUALS)
//             .unit(DurationUnit.HOURS)
//             .value(1L)
//             .add()
//             .buildList();
//
//     ProcessReportDataDto reportData =
//         TemplatedProcessReportDataBuilder.createReportData()
//             .setReportDataType(getTestReportDataType())
//
// .setProcessDefinitionVersion(completeProcessInstanceDto.getProcessDefinitionVersion())
//             .setProcessDefinitionKey(completeProcessInstanceDto.getProcessDefinitionKey())
//             .setGroupByDateInterval(AggregateByDateUnit.DAY)
//             .setFilter(durationFilter)
//             .build();
//     ReportResultResponseDto<List<MapResultEntryDto>> resultDto =
//         reportClient.evaluateMapReport(reportData).getResult();
//
//     // then
//     final List<MapResultEntryDto> resultData = resultDto.getFirstMeasureData();
//     assertThat(resultData).isNotNull().hasSize(1);
//     assertThat(resultData.get(0).getValue())
//         .isEqualTo((double) runningProcInstStartDate.until(now, MILLIS));
//   }
// }
