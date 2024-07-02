/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.command.util;

import io.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedByType;
import io.camunda.optimize.dto.optimize.query.report.single.decision.filter.DecisionFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import io.camunda.optimize.service.db.es.filter.DecisionQueryFilterEnhancer;
import io.camunda.optimize.service.db.es.filter.FilterContext;
import io.camunda.optimize.service.db.es.filter.ProcessQueryFilterEnhancer;
import io.camunda.optimize.service.db.es.report.MinMaxStatDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.elasticsearch.search.aggregations.AggregationBuilder;

@Builder
@Getter
public class DateAggregationContext {

  @NonNull @Setter private AggregateByDateUnit aggregateByDateUnit;
  @NonNull private final String dateField;
  private final String
      runningDateReportEndDateField; // used for range filter aggregation in running date reports
  // only

  @NonNull private final MinMaxStatDto minMaxStats;
  // extendBoundsToMinMaxStats true is used for distrBy date reports which require extended bounds
  // even when no date
  // filters are applied. If date filters are applied, extendedBounds may be overwritten by the
  // filter bounds.
  // This serves a similar purpose as <allDistributedByKeys> in the ExecutionContext for
  // non-histogram aggregations.
  @Builder.Default private final boolean extendBoundsToMinMaxStats = false;

  @NonNull private final ZoneId timezone;
  @NonNull private final List<AggregationBuilder> subAggregations;

  private final String dateAggregationName;

  private final List<DecisionFilterDto<?>> decisionFilters;
  private final DecisionQueryFilterEnhancer decisionQueryFilterEnhancer;

  private final ProcessGroupByType processGroupByType;
  private final DistributedByType distributedByType;
  private final List<ProcessFilterDto<?>> processFilters;
  private final ProcessQueryFilterEnhancer processQueryFilterEnhancer;
  @NonNull private final FilterContext filterContext;

  public ZonedDateTime getEarliestDate() {
    return ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(Math.round(minMaxStats.getMin())), timezone);
  }

  public ZonedDateTime getLatestDate() {
    return ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(Math.round(minMaxStats.getMax())), timezone);
  }

  public Optional<String> getDateAggregationName() {
    return Optional.ofNullable(dateAggregationName);
  }

  public boolean isStartDateAggregation() {
    if (processGroupByType != null) {
      return ProcessGroupByType.START_DATE.equals(processGroupByType);
    } else {
      return DistributedByType.START_DATE.equals(distributedByType);
    }
  }
}
