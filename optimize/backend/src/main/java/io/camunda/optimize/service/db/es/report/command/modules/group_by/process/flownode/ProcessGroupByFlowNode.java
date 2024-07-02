/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.command.modules.group_by.process.flownode;

import static io.camunda.optimize.service.db.es.report.command.modules.result.CompositeCommandResult.GroupByResult;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_ID;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_INSTANCES;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

import io.camunda.optimize.dto.optimize.DefinitionType;
import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.FlowNodesGroupByDto;
import io.camunda.optimize.service.DefinitionService;
import io.camunda.optimize.service.db.es.report.command.exec.ExecutionContext;
import io.camunda.optimize.service.db.es.report.command.modules.result.CompositeCommandResult;
import io.camunda.optimize.service.db.es.report.command.modules.result.CompositeCommandResult.DistributedByResult;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProcessGroupByFlowNode extends AbstractGroupByFlowNode {

  private static final String NESTED_EVENTS_AGGREGATION = "nestedEvents";

  private final ConfigurationService configurationService;

  public ProcessGroupByFlowNode(
      final ConfigurationService configurationService, final DefinitionService definitionService) {
    super(definitionService);
    this.configurationService = configurationService;
  }

  @Override
  public List<AggregationBuilder> createAggregation(
      final SearchSourceBuilder searchSourceBuilder,
      final ExecutionContext<ProcessReportDataDto> context) {
    final TermsAggregationBuilder flowNodeTermsAggregation =
        terms(NESTED_EVENTS_AGGREGATION)
            .size(configurationService.getElasticSearchConfiguration().getAggregationBucketLimit())
            .field(FLOW_NODE_INSTANCES + "." + FLOW_NODE_ID);
    distributedByPart.createAggregations(context).forEach(flowNodeTermsAggregation::subAggregation);
    return Collections.singletonList(
        createFilteredFlowNodeAggregation(context, flowNodeTermsAggregation));
  }

  @Override
  public void addQueryResult(
      final CompositeCommandResult compositeCommandResult,
      final SearchResponse response,
      final ExecutionContext<ProcessReportDataDto> context) {
    getFilteredFlowNodesAggregation(response)
        .map(
            filteredFlowNodes ->
                (Terms) filteredFlowNodes.getAggregations().get(NESTED_EVENTS_AGGREGATION))
        .ifPresent(
            byFlowNodeIdAggregation -> {
              final Map<String, String> flowNodeNames = getFlowNodeNames(context.getReportData());
              final List<GroupByResult> groupedData = new ArrayList<>();
              for (Terms.Bucket flowNodeBucket : byFlowNodeIdAggregation.getBuckets()) {
                final String flowNodeKey = flowNodeBucket.getKeyAsString();
                if (flowNodeNames.containsKey(flowNodeKey)) {
                  final List<DistributedByResult> singleResult =
                      distributedByPart.retrieveResult(
                          response, flowNodeBucket.getAggregations(), context);
                  String label = flowNodeNames.get(flowNodeKey);
                  groupedData.add(
                      GroupByResult.createGroupByResult(flowNodeKey, label, singleResult));
                  flowNodeNames.remove(flowNodeKey);
                }
              }
              addMissingGroupByKeys(flowNodeNames, groupedData, context);
              compositeCommandResult.setGroups(groupedData);
            });
  }

  private void addMissingGroupByKeys(
      final Map<String, String> flowNodeNames,
      final List<GroupByResult> groupedData,
      final ExecutionContext<ProcessReportDataDto> context) {
    final boolean viewLevelFilterExists =
        context.getReportData().getFilter().stream()
            .anyMatch(filter -> FilterApplicationLevel.VIEW.equals(filter.getFilterLevel()));
    // If a view level filter exists, the data should not be enriched as the missing data could been
    // omitted by the filters
    if (!viewLevelFilterExists) {
      // If no view level filter exists, we enrich data with flow nodes that haven't been executed,
      // but should still
      // show up in the result
      flowNodeNames.forEach(
          (key, value) ->
              groupedData.add(
                  GroupByResult.createGroupByResult(
                      key, value, distributedByPart.createEmptyResult(context))));
    }
  }

  private Map<String, String> getFlowNodeNames(final ProcessReportDataDto reportData) {
    return definitionService.extractFlowNodeIdAndNames(
        reportData.getDefinitions().stream()
            .map(
                definitionDto ->
                    definitionService.getDefinition(
                        DefinitionType.PROCESS,
                        definitionDto.getKey(),
                        definitionDto.getVersions(),
                        definitionDto.getTenantIds()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ProcessDefinitionOptimizeDto.class::cast)
            .collect(Collectors.toList()));
  }

  @Override
  protected void addGroupByAdjustmentsForCommandKeyGeneration(
      final ProcessReportDataDto reportData) {
    reportData.setGroupBy(new FlowNodesGroupByDto());
  }
}
