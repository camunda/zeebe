/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter.process.date.modelelement;

import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;

import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import java.util.List;

public abstract class AbstractRollingFlowNodeDateFilterIT extends AbstractFlowNodeDateFilterIT {

  protected abstract List<ProcessFilterDto<?>> createRollingDateViewFilter(
      final Long value, final DateUnit unit);

  protected abstract List<ProcessFilterDto<?>> createRollingDateInstanceFilter(
      final List<String> flowNodeIds, final Long value, final DateUnit unit);

  @Override
  protected List<ProcessFilterDto<?>> createViewLevelDateFilterForDate1() {
    dateFreezer().dateToFreeze(DATE_1).freezeDateAndReturn();
    return createRollingDateViewFilter(0L, DateUnit.DAYS);
  }

  @Override
  protected List<ProcessFilterDto<?>> createViewLevelDateFilterForDate2() {
    dateFreezer().dateToFreeze(DATE_2).freezeDateAndReturn();
    return createRollingDateViewFilter(0L, DateUnit.DAYS);
  }

  @Override
  protected List<ProcessFilterDto<?>> createInstanceLevelDateFilterForDate1(
      final List<String> flowNodeIds) {
    dateFreezer().dateToFreeze(DATE_1).freezeDateAndReturn();
    return createRollingDateInstanceFilter(flowNodeIds, 0L, DateUnit.DAYS);
  }

  @Override
  protected List<ProcessFilterDto<?>> createInvalidFilter() {
    return createRollingDateViewFilter(null, null);
  }
}
