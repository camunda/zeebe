/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.rest.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.optimize.dto.optimize.query.report.single.result.ResultType;
import io.camunda.optimize.dto.optimize.rest.pagination.PaginationDto;
import io.camunda.optimize.dto.optimize.rest.report.measure.MeasureResponseDto;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResultResponseDto<T> {
  private long instanceCount;
  private long instanceCountWithoutFilters;
  private List<MeasureResponseDto<T>> measures = new ArrayList<>();
  private PaginationDto pagination;

  public void addMeasure(MeasureResponseDto<T> measure) {
    this.measures.add(measure);
  }

  @JsonIgnore
  public T getFirstMeasureData() {
    return getMeasures().get(0).getData();
  }

  @JsonIgnore
  public T getData() {
    return getFirstMeasureData();
  }

  // here for API compatibility as the frontend currently makes use of this property
  public ResultType getType() {
    return getMeasures().stream().findFirst().map(MeasureResponseDto::getType).orElse(null);
  }
}
