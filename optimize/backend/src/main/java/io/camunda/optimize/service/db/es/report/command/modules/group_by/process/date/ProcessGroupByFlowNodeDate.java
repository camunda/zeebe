/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.command.modules.group_by.process.date;

import static io.camunda.optimize.service.db.es.filter.util.ModelElementFilterQueryUtil.createModelElementAggregationFilter;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_INSTANCES;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.service.DefinitionService;
import io.camunda.optimize.service.db.es.report.MinMaxStatsService;
import io.camunda.optimize.service.db.es.report.command.exec.ExecutionContext;
import io.camunda.optimize.service.db.es.report.command.service.DateAggregationService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;

@Slf4j
public abstract class ProcessGroupByFlowNodeDate extends AbstractProcessGroupByModelElementDate {

  private final DefinitionService definitionService;

  ProcessGroupByFlowNodeDate(
      final DateAggregationService dateAggregationService,
      final MinMaxStatsService minMaxStatsService,
      final DefinitionService definitionService) {
    super(dateAggregationService, minMaxStatsService);
    this.definitionService = definitionService;
  }

  @Override
  protected String getPathToElementField() {
    return FLOW_NODE_INSTANCES;
  }

  @Override
  protected QueryBuilder getFilterQuery(final ExecutionContext<ProcessReportDataDto> context) {
    return createModelElementAggregationFilter(
        context.getReportData(), context.getFilterContext(), definitionService);
  }

  @Override
  protected QueryBuilder getModelElementTypeFilterQuery() {
    return matchAllQuery();
  }
}
