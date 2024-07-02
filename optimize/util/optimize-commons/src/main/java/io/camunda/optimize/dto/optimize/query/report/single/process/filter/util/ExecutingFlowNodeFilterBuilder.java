/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process.filter.util;

import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ExecutingFlowNodeFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.data.ExecutingFlowNodeFilterDataDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExecutingFlowNodeFilterBuilder {

  private List<String> values = new ArrayList<>();
  private ProcessFilterBuilder filterBuilder;
  private FilterApplicationLevel filterLevel = FilterApplicationLevel.INSTANCE;

  private ExecutingFlowNodeFilterBuilder(ProcessFilterBuilder processFilterBuilder) {
    filterBuilder = processFilterBuilder;
  }

  public static ExecutingFlowNodeFilterBuilder construct(
      ProcessFilterBuilder processFilterBuilder) {
    return new ExecutingFlowNodeFilterBuilder(processFilterBuilder);
  }

  public ExecutingFlowNodeFilterBuilder id(String flowNodeId) {
    values.add(flowNodeId);
    return this;
  }

  public ExecutingFlowNodeFilterBuilder ids(String... flowNodeIds) {
    values.addAll(Arrays.asList(flowNodeIds));
    return this;
  }

  public ExecutingFlowNodeFilterBuilder filterLevel(FilterApplicationLevel filterLevel) {
    this.filterLevel = filterLevel;
    return this;
  }

  public ProcessFilterBuilder add() {
    ExecutingFlowNodeFilterDataDto dataDto = new ExecutingFlowNodeFilterDataDto();
    dataDto.setValues(new ArrayList<>(values));
    ExecutingFlowNodeFilterDto executingFlowNodeFilterDto = new ExecutingFlowNodeFilterDto();
    executingFlowNodeFilterDto.setData(dataDto);
    executingFlowNodeFilterDto.setFilterLevel(filterLevel);
    filterBuilder.addFilter(executingFlowNodeFilterDto);
    return filterBuilder;
  }
}
