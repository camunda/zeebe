/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter.process.variable;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateFilterType;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RelativeDateFilterStartDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RollingDateFilterStartDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.instance.FixedDateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.instance.RelativeDateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.instance.RollingDateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.result.raw.RawDataProcessInstanceDto;
import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import io.camunda.optimize.dto.optimize.rest.report.ReportResultResponseDto;
import io.camunda.optimize.exception.OptimizeIntegrationTestException;
import io.camunda.optimize.service.db.es.filter.process.AbstractFilterIT;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.test.it.extension.EngineVariableValue;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag(OPENSEARCH_PASSING)
public class DateVariableQueryFilterIT extends AbstractFilterIT {

  private static final String VARIABLE_NAME = "var";

  @BeforeEach
  public void setup() {
    LocalDateUtil.setCurrentTime(OffsetDateTime.parse("2019-06-15T12:00:00+02:00"));
  }

  private static Stream<Arguments> nullFilterScenarios() {
    return Stream.concat(
        Arrays.stream(DateFilterType.values())
            .flatMap(
                dateFilterType ->
                    Stream.of(
                        // filter for null/undefined only, includeUndefined=true
                        Arguments.of(
                            "Include Null/Undefined for type " + dateFilterType,
                            createSupplier(
                                () ->
                                    createDateFilterDataDto(dateFilterType)
                                        .setIncludeUndefined(true)),
                            2L),
                        // filter for defined only, excludeUndefined=true
                        Arguments.of(
                            "Exclude Null/Undefined for type " + dateFilterType,
                            createSupplier(
                                () ->
                                    createDateFilterDataDto(dateFilterType)
                                        .setExcludeUndefined(true)),
                            3L))),
        Stream.of(
            // filter for particular values and includeUndefined=true
            Arguments.of(
                "Include value and Null/Undefined for type " + DateFilterType.FIXED,
                createSupplier(
                    () ->
                        new FixedDateFilterDataDto(
                                LocalDateUtil.getCurrentDateTime(),
                                LocalDateUtil.getCurrentDateTime().plusMinutes(1))
                            .setIncludeUndefined(true)),
                4L),
            Arguments.of(
                "Include value and Null/Undefined for type " + DateFilterType.ROLLING,
                createSupplier(
                    () ->
                        new RollingDateFilterDataDto(
                                new RollingDateFilterStartDto(0L, DateUnit.MINUTES))
                            .setIncludeUndefined(true)),
                3L),
            Arguments.of(
                "Include value and Null/Undefined for type " + DateFilterType.RELATIVE,
                createSupplier(
                    () ->
                        new RelativeDateFilterDataDto(
                                new RelativeDateFilterStartDto(0L, DateUnit.MINUTES))
                            .setIncludeUndefined(true)),
                3L)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("nullFilterScenarios")
  public void dateFilterSupportsNullValueInclusionAndExclusion(
      final String scenarioName,
      final Supplier<DateFilterDataDto<?>> dateFilterSupplier,
      final long expectedInstanceCount) {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
    // instance where the variable is undefined
    engineIntegrationExtension.startProcessInstance(processDefinition.getId());
    // instance where the variable has the value null
    engineIntegrationExtension.startProcessInstance(
        processDefinition.getId(),
        ImmutableMap.of(VARIABLE_NAME, new EngineVariableValue(null, VariableType.DATE.getId())));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.minusMinutes(1));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now);
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.plusMinutes(1));

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> filter =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .dateFilter(dateFilterSupplier.get())
            .add()
            .buildList();

    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluateReportWithFilter(processDefinition, filter);

    // then
    assertThat(result.getInstanceCount()).isEqualTo(expectedInstanceCount);
  }

  @Test
  public void dateLessThanOrEqualVariableFilter() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.minusDays(2));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now);
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.plusDays(10));

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> filter =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .fixedDate(null, now.plusDays(1))
            .add()
            .buildList();

    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluateReportWithFilter(processDefinition, filter);

    // then
    assertThat(result.getInstanceCount()).isEqualTo(2L);
  }

  @Test
  public void dateGreaterOrEqualThanVariableFilter() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.minusDays(2));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now);
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.plusDays(10));

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> filter =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .fixedDate(now, null)
            .add()
            .buildList();

    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluateReportWithFilter(processDefinition, filter);

    // then
    assertThat(result.getInstanceCount()).isEqualTo(2L);
  }

  @Test
  public void dateEqualVariableFilter() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.minusDays(2));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now);
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.plusDays(10));

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> filter =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .fixedDate(now, now.plusDays(1))
            .add()
            .buildList();

    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluateReportWithFilter(processDefinition, filter);

    // then
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  public void dateWithinRangeVariableFilter() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.minusDays(2));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now);

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> filter =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .fixedDate(now.minusDays(1), now.plusDays(1))
            .add()
            .buildList();

    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluateReportWithFilter(processDefinition, filter);

    // then
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  public void overlappingDateFiltersYieldConjunctInstancesVariableFilter() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.minusDays(2));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now);
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.plusDays(10));

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> filters =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .fixedDate(now.minusDays(1), now.plusDays(1))
            .add()
            .variable()
            .name(VARIABLE_NAME)
            .dateType()
            .fixedDate(now, now.plusDays(1))
            .add()
            .buildList();

    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluateReportWithFilter(
            processDefinition.getKey(), String.valueOf(processDefinition.getVersion()), filters);

    // then
    assertThat(result.getInstanceCount()).isEqualTo(1L);
  }

  @Test
  public void nonOverlappingDateFiltersYieldNoResultsVariableFilter() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.minusSeconds(2));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now);
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.plusSeconds(10));

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> filters =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .fixedDate(now.minusSeconds(2), now.minusSeconds(1))
            .add()
            .variable()
            .name(VARIABLE_NAME)
            .dateType()
            .fixedDate(now.plusSeconds(1), now.plusSeconds(2))
            .add()
            .buildList();

    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result =
        evaluateReportWithFilter(
            processDefinition.getKey(), String.valueOf(processDefinition.getVersion()), filters);

    // then
    assertThat(result.getInstanceCount()).isEqualTo(0L);
  }

  @Test
  public void rollingDateVariableFilter() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.minusDays(2));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now.minusDays(3));
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now);

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> filters1 =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .rollingDate(1L, DateUnit.DAYS)
            .add()
            .buildList();
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result1 =
        evaluateReportWithFilter(
            processDefinition.getKey(), String.valueOf(processDefinition.getVersion()), filters1);

    final List<ProcessFilterDto<?>> filters2 =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .rollingDate(3L, DateUnit.DAYS)
            .add()
            .buildList();
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result2 =
        evaluateReportWithFilter(
            processDefinition.getKey(), String.valueOf(processDefinition.getVersion()), filters2);

    // then
    assertThat(result1.getInstanceCount()).isEqualTo(1L);
    assertThat(result2.getInstanceCount()).isEqualTo(3L);
  }

  @Test
  public void relativeDateVariableFilter() {
    // given
    final OffsetDateTime now = LocalDateUtil.getCurrentDateTime();
    final ProcessDefinitionEngineDto processDefinition = deploySimpleProcessDefinition();
    startInstanceForDefinitionWithDateVar(processDefinition.getId(), now);

    importAllEngineEntitiesFromScratch();

    // when
    final List<ProcessFilterDto<?>> filterToday =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .relativeDate(0L, DateUnit.DAYS)
            .add()
            .buildList();
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result1 =
        evaluateReportWithFilter(
            processDefinition.getKey(),
            String.valueOf(processDefinition.getVersion()),
            filterToday);

    // now move the current day
    LocalDateUtil.setCurrentTime(now.plusDays(1L));
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result2 =
        evaluateReportWithFilter(
            processDefinition.getKey(),
            String.valueOf(processDefinition.getVersion()),
            filterToday);

    final List<ProcessFilterDto<?>> filterPreviousDay =
        ProcessFilterBuilder.filter()
            .variable()
            .dateType()
            .name(VARIABLE_NAME)
            .relativeDate(1L, DateUnit.DAYS)
            .add()
            .buildList();
    final ReportResultResponseDto<List<RawDataProcessInstanceDto>> result3 =
        evaluateReportWithFilter(
            processDefinition.getKey(),
            String.valueOf(processDefinition.getVersion()),
            filterPreviousDay);

    // then
    assertThat(result1.getInstanceCount()).isEqualTo(1L);
    assertThat(result2.getInstanceCount()).isEqualTo(0L);
    assertThat(result3.getInstanceCount()).isEqualTo(1L);
  }

  private void startInstanceForDefinitionWithDateVar(
      final String definitionId, final OffsetDateTime variableValue) {
    engineIntegrationExtension.startProcessInstance(
        definitionId, ImmutableMap.of(VARIABLE_NAME, variableValue));
  }

  private static Supplier<Object> createSupplier(final Supplier<Object> supplier) {
    return supplier;
  }

  private static DateFilterDataDto<?> createDateFilterDataDto(final DateFilterType dateFilterType) {
    switch (dateFilterType) {
      case FIXED:
        return new FixedDateFilterDataDto();
      case ROLLING:
        return new RollingDateFilterDataDto();
      case RELATIVE:
        return new RelativeDateFilterDataDto();
      default:
        throw new OptimizeIntegrationTestException("Unsupported dateFilter type:" + dateFilterType);
    }
  }
}
