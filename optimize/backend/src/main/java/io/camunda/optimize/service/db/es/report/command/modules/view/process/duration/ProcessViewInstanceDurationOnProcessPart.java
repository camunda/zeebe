/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.command.modules.view.process.duration;

import static io.camunda.optimize.service.db.es.report.command.process.processinstance.duration.ProcessPartQueryUtil.addProcessPartQuery;
import static io.camunda.optimize.service.db.es.report.command.process.processinstance.duration.ProcessPartQueryUtil.createProcessPartAggregation;
import static io.camunda.optimize.service.db.es.report.command.process.processinstance.duration.ProcessPartQueryUtil.getProcessPartAggregationResult;

import io.camunda.optimize.dto.optimize.query.report.single.configuration.process_part.ProcessPartDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.service.db.es.report.command.aggregations.AggregationStrategy;
import io.camunda.optimize.service.db.es.report.command.exec.ExecutionContext;
import io.camunda.optimize.service.db.es.report.command.modules.result.CompositeCommandResult;
import io.camunda.optimize.service.db.es.report.command.modules.result.CompositeCommandResult.ViewResult;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.math3.util.Precision;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// we accept the hierarchy debt here, thus suppressing the warning
@SuppressWarnings("java:S110")
public class ProcessViewInstanceDurationOnProcessPart extends ProcessViewInstanceDuration {

  @Override
  public List<AggregationBuilder> createAggregations(
      final ExecutionContext<ProcessReportDataDto> context) {
    ProcessPartDto processPart =
        context
            .getReportConfiguration()
            .getProcessPart()
            .orElseThrow(() -> new OptimizeRuntimeException("Missing ProcessPart"));
    return Collections.singletonList(
        createProcessPartAggregation(
            processPart.getStart(),
            processPart.getEnd(),
            getAggregationStrategies(context.getReportData()).stream()
                .map(AggregationStrategy::getAggregationType)
                .collect(Collectors.toList())));
  }

  @Override
  public ViewResult retrieveResult(
      final SearchResponse response,
      final Aggregations aggs,
      final ExecutionContext<ProcessReportDataDto> context) {
    final ViewResult.ViewResultBuilder viewResultBuilder = ViewResult.builder();
    getAggregationStrategies(context.getReportData())
        .forEach(
            aggregationStrategy -> {
              Double measureResult =
                  getProcessPartAggregationResult(aggs, aggregationStrategy.getAggregationType());
              if (measureResult != null) {
                // rounding to the closest integer since the lowest precision
                // for duration in the data is milliseconds anyway for data types.
                measureResult = Precision.round(measureResult, 0);
              }
              viewResultBuilder.viewMeasure(
                  CompositeCommandResult.ViewMeasure.builder()
                      .aggregationType(aggregationStrategy.getAggregationType())
                      .value(measureResult)
                      .build());
            });
    return viewResultBuilder.build();
  }

  @Override
  public void adjustSearchRequest(
      final SearchRequest searchRequest,
      final BoolQueryBuilder baseQuery,
      final ExecutionContext<ProcessReportDataDto> context) {
    super.adjustSearchRequest(searchRequest, baseQuery, context);
    ProcessPartDto processPart =
        context
            .getReportConfiguration()
            .getProcessPart()
            .orElseThrow(() -> new OptimizeRuntimeException("Missing ProcessPart"));
    addProcessPartQuery(baseQuery, processPart.getStart(), processPart.getEnd());
  }

  @Override
  public void addViewAdjustmentsForCommandKeyGeneration(
      final ProcessReportDataDto dataForCommandKey) {
    super.addViewAdjustmentsForCommandKeyGeneration(dataForCommandKey);
    dataForCommandKey.getConfiguration().setProcessPart(new ProcessPartDto());
  }
}
