/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.report.single.process.group.value;

import org.camunda.optimize.dto.optimize.query.report.single.group.GroupByDateUnit;

import java.util.Objects;

public class StartDateGroupByValueDto implements ProcessGroupByValueDto {

  protected GroupByDateUnit unit;

  public GroupByDateUnit getUnit() {
    return unit;
  }

  public void setUnit(GroupByDateUnit unit) {
    this.unit = unit;
  }

  @Override
  public boolean isCombinable(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StartDateGroupByValueDto)) {
      return false;
    }
    StartDateGroupByValueDto that = (StartDateGroupByValueDto) o;
    return Objects.equals(unit, that.unit);
  }
}
