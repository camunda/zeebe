/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.decision.group.value;

import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
import java.util.Objects;
import lombok.Data;

@Data
public class DecisionGroupByEvaluationDateTimeValueDto implements DecisionGroupByValueDto {

  protected AggregateByDateUnit unit;

  @Override
  public boolean isCombinable(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DecisionGroupByEvaluationDateTimeValueDto)) {
      return false;
    }
    final DecisionGroupByEvaluationDateTimeValueDto that = (DecisionGroupByEvaluationDateTimeValueDto) o;
    return Objects.equals(unit, that.unit);
  }
}
