/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter.process.date.modelelement;

import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import java.time.OffsetDateTime;
import java.util.List;

public class RelativeFlowNodeEndDateFilterIT extends AbstractRelativeFlowNodeDateFilterIT {

  @Override
  protected ProcessGroupByType getDateReportGroupByType() {
    return ProcessGroupByType.END_DATE;
  }

  @Override
  protected void updateFlowNodeDate(
      final String instanceId, final String flowNodeId, final OffsetDateTime newDate) {
    engineDatabaseExtension.changeFlowNodeEndDate(instanceId, flowNodeId, newDate);
  }

  @Override
  protected List<ProcessFilterDto<?>> createRelativeDateViewFilter(
      final Long value, final DateUnit unit) {
    return ProcessFilterBuilder.filter()
        .relativeFlowNodeEndDate()
        .filterLevel(FilterApplicationLevel.VIEW)
        .start(value, unit)
        .add()
        .buildList();
  }

  @Override
  protected List<ProcessFilterDto<?>> createRelativeDateInstanceFilter(
      final List<String> flowNodeIds, final Long value, final DateUnit unit) {
    return ProcessFilterBuilder.filter()
        .relativeFlowNodeEndDate()
        .filterLevel(FilterApplicationLevel.INSTANCE)
        .flowNodeIds(flowNodeIds)
        .start(value, unit)
        .add()
        .buildList();
  }
}
