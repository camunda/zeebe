/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.process.processinstance.duration.groupby.date;

import com.google.common.collect.ImmutableList;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.parameters.ProcessPartDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.duration.AggregationResultDto;
import org.camunda.optimize.service.es.report.command.process.processinstance.duration.ProcessPartQueryUtil;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;

import java.util.List;

import static org.camunda.optimize.service.es.report.command.process.processinstance.duration.ProcessPartQueryUtil.processProcessPartAggregationOperations;


public class ProcessInstanceDurationGroupByStartDateWithProcessPartCommand
  extends AbstractProcessInstanceDurationGroupByStartDateCommand {

  @Override
  public BoolQueryBuilder setupBaseQuery(ProcessReportDataDto processReportData) {
    BoolQueryBuilder boolQueryBuilder = super.setupBaseQuery(processReportData);
    ProcessPartDto processPart = processReportData.getParameters().getProcessPart();
    return ProcessPartQueryUtil.addProcessPartQuery(boolQueryBuilder, processPart.getStart(), processPart.getEnd());
  }

  @Override
  protected AggregationResultDto processAggregationOperation(Aggregations aggs) {
    return processProcessPartAggregationOperations(aggs);
  }

  @Override
  protected List<AggregationBuilder> createOperationsAggregations() {
    ProcessPartDto processPart = ((ProcessReportDataDto) getReportData()).getParameters().getProcessPart();
    return ImmutableList.of(
      ProcessPartQueryUtil.createProcessPartAggregation(processPart.getStart(), processPart.getEnd())
    );
  }
}
