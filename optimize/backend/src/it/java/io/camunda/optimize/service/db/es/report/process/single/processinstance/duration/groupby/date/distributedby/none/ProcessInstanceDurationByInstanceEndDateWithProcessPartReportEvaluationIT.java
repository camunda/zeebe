/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.process.single.processinstance.duration.groupby.date.distributedby.none;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RollingDateFilterStartDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.instance.RollingDateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.InstanceEndDateFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ProcessInstanceDurationByInstanceEndDateWithProcessPartReportEvaluationIT
    extends AbstractProcessInstanceDurationByInstanceDateWithProcessPartReportEvaluationIT {

  @Override
  protected ProcessReportDataType getTestReportDataType() {
    return ProcessReportDataType.PROC_INST_DUR_GROUP_BY_END_DATE_WITH_PART;
  }

  @Override
  protected ProcessGroupByType getGroupByType() {
    return ProcessGroupByType.END_DATE;
  }

  @Override
  protected void adjustProcessInstanceDates(
      String processInstanceId,
      OffsetDateTime referenceDate,
      long daysToShift,
      Long durationInSec) {
    OffsetDateTime shiftedDate = referenceDate.plusDays(daysToShift);
    if (durationInSec != null) {
      engineDatabaseExtension.changeProcessInstanceStartDate(
          processInstanceId, shiftedDate.minusSeconds(durationInSec));
    }
    engineDatabaseExtension.changeProcessInstanceEndDate(processInstanceId, shiftedDate);
  }

  @Test
  public void testEmptyBucketsAreReturnedForEndDateFilterPeriod() {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessDefinitionEngineDto procDefDto = deploySimpleServiceTaskProcess();
    startThreeProcessInstances(startDate, 0, procDefDto, Arrays.asList(1, 1, 1));
    startThreeProcessInstances(startDate, -2, procDefDto, Arrays.asList(2, 2, 2));

    importAllEngineEntitiesFromScratch();

    // when
    final RollingDateFilterDataDto dateFilterDataDto =
        new RollingDateFilterDataDto(new RollingDateFilterStartDto(4L, DateUnit.DAYS));
    final InstanceEndDateFilterDto endDateFilterDto = new InstanceEndDateFilterDto();
    endDateFilterDto.setData(dateFilterDataDto);
    endDateFilterDto.setFilterLevel(FilterApplicationLevel.INSTANCE);

    final ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(procDefDto.getKey())
            .setProcessDefinitionVersion(procDefDto.getVersionAsString())
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(getTestReportDataType())
            .setGroupByDateInterval(AggregateByDateUnit.DAY)
            .setFilter(endDateFilterDto)
            .build();
    final ReportResultResponseDto<List<MapResultEntryDto>> result =
        reportClient.evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto> resultData = result.getFirstMeasureData();
    assertThat(resultData.size()).isEqualTo(5);

    assertThat(resultData.get(0).getKey())
        .isEqualTo(
            embeddedOptimizeExtension.formatToHistogramBucketKey(
                startDate.minusDays(4), ChronoUnit.DAYS));
    assertThat(resultData.get(0).getValue()).isNull();

    assertThat(resultData.get(1).getKey())
        .isEqualTo(
            embeddedOptimizeExtension.formatToHistogramBucketKey(
                startDate.minusDays(3), ChronoUnit.DAYS));
    assertThat(resultData.get(1).getValue()).isNull();

    assertThat(resultData.get(2).getKey())
        .isEqualTo(
            embeddedOptimizeExtension.formatToHistogramBucketKey(
                startDate.minusDays(2), ChronoUnit.DAYS));
    assertThat(resultData.get(2).getValue()).isEqualTo(2000.);

    assertThat(resultData.get(3).getKey())
        .isEqualTo(
            embeddedOptimizeExtension.formatToHistogramBucketKey(
                startDate.minusDays(1), ChronoUnit.DAYS));
    assertThat(resultData.get(3).getValue()).isNull();

    assertThat(resultData.get(4).getKey())
        .isEqualTo(
            embeddedOptimizeExtension.formatToHistogramBucketKey(startDate, ChronoUnit.DAYS));
    assertThat(resultData.get(4).getValue()).isEqualTo(1000.);
  }
}
