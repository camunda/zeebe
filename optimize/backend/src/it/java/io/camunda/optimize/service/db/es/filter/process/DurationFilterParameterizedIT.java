/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter.process;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.ComparisonOperator.GREATER_THAN;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.ComparisonOperator.GREATER_THAN_EQUALS;
import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.ComparisonOperator.LESS_THAN;

import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DurationUnit;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.operator.ComparisonOperator;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessInstanceDto;
import io.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResponseDto;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag(OPENSEARCH_PASSING)
public class DurationFilterParameterizedIT extends AbstractDurationFilterIT {

  @ParameterizedTest
  @MethodSource("getArguments")
  public void testGetReportWithLtDurationCriteria(
      boolean deployWithTimeShift,
      Long daysToShift,
      Long durationInSec,
      ComparisonOperator operator,
      int duration,
      DurationUnit unit)
      throws Exception {
    // given
    ProcessInstanceEngineDto processInstance;
    if (deployWithTimeShift) {
      processInstance = deployWithTimeShift(daysToShift, durationInSec);
    } else {
      processInstance = deployAndStartSimpleProcess();
    }

    importAllEngineEntitiesFromScratch();

    // when
    ProcessReportDataDto reportData =
        TemplatedProcessReportDataBuilder.createReportData()
            .setProcessDefinitionKey(processInstance.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstance.getProcessDefinitionVersion())
            .setReportDataType(ProcessReportDataType.RAW_DATA)
            .build();
    reportData.setFilter(
        ProcessFilterBuilder.filter()
            .duration()
            .unit(unit)
            .value((long) duration)
            .operator(operator)
            .add()
            .buildList());
    AuthorizedProcessReportEvaluationResponseDto<List<RawDataProcessInstanceDto>> result =
        reportClient.evaluateRawReport(reportData);

    // then
    assertResult(processInstance, result);
  }

  private static Stream<Arguments> getArguments() {
    return Stream.of(
        Arguments.of(false, null, null, LESS_THAN, 1, DurationUnit.SECONDS),
        Arguments.of(false, null, null, LESS_THAN, 1, DurationUnit.MINUTES),
        Arguments.of(false, null, null, LESS_THAN, 1, DurationUnit.HOURS),
        Arguments.of(false, null, null, LESS_THAN, 1, DurationUnit.HALF_DAYS),
        Arguments.of(false, null, null, LESS_THAN, 1, DurationUnit.DAYS),
        Arguments.of(false, null, null, LESS_THAN, 1, DurationUnit.WEEKS),
        Arguments.of(false, null, null, LESS_THAN, 1, DurationUnit.MONTHS),
        Arguments.of(true, 0L, 2L, GREATER_THAN, 1, DurationUnit.SECONDS),
        Arguments.of(true, 0L, 2L, GREATER_THAN_EQUALS, 2, DurationUnit.SECONDS));
  }
}
