/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process.filter.util;

import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RollingDateFilterStartDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.instance.RollingDateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.InstanceEndDateFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.InstanceStartDateFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;

public class RollingInstanceDateFilterBuilder {

  private final ProcessFilterBuilder filterBuilder;
  private RollingDateFilterStartDto start;
  private String type;
  private FilterApplicationLevel filterLevel = FilterApplicationLevel.INSTANCE;

  private RollingInstanceDateFilterBuilder(ProcessFilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  static RollingInstanceDateFilterBuilder endDate(ProcessFilterBuilder filterBuilder) {
    RollingInstanceDateFilterBuilder builder = new RollingInstanceDateFilterBuilder(filterBuilder);
    builder.type = "endDate";
    return builder;
  }

  static RollingInstanceDateFilterBuilder startDate(ProcessFilterBuilder filterBuilder) {
    RollingInstanceDateFilterBuilder builder = new RollingInstanceDateFilterBuilder(filterBuilder);
    builder.type = "startDate";
    return builder;
  }

  public RollingInstanceDateFilterBuilder start(Long value, DateUnit unit) {
    this.start = new RollingDateFilterStartDto(value, unit);
    return this;
  }

  public RollingInstanceDateFilterBuilder filterLevel(final FilterApplicationLevel filterLevel) {
    this.filterLevel = filterLevel;
    return this;
  }

  public ProcessFilterBuilder add() {
    ProcessFilterDto<DateFilterDataDto<?>> filterDto =
        type.equals("endDate") ? new InstanceEndDateFilterDto() : new InstanceStartDateFilterDto();
    filterDto.setData(new RollingDateFilterDataDto(start));
    filterDto.setFilterLevel(filterLevel);
    filterBuilder.addFilter(filterDto);
    return filterBuilder;
  }
}
