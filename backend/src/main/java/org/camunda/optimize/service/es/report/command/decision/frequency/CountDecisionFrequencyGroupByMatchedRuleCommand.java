/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.decision.frequency;

import org.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.result.DecisionReportMapResultDto;
import org.camunda.optimize.service.es.report.command.decision.DecisionReportCommand;
import org.camunda.optimize.service.es.report.command.util.MapResultSortingUtility;
import org.camunda.optimize.service.es.report.result.decision.SingleDecisionMapReportResult;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.camunda.optimize.service.es.schema.OptimizeIndexNameHelper.getOptimizeIndexAliasForType;
import static org.camunda.optimize.service.es.schema.type.DecisionInstanceType.MATCHED_RULES;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.DECISION_INSTANCE_TYPE;

public class CountDecisionFrequencyGroupByMatchedRuleCommand
  extends DecisionReportCommand<SingleDecisionMapReportResult> {

  private static final String MATCHED_RULES_AGGREGATION = "matchedRules";

  @Override
  protected SingleDecisionMapReportResult evaluate() {
    final DecisionReportDataDto reportData = getReportData();
    logger.debug(
      "Evaluating count decision instance frequency grouped by matched rule report " +
        "for decision definition key [{}] and version [{}]",
      reportData.getDecisionDefinitionKey(),
      reportData.getDecisionDefinitionVersion()
    );

    BoolQueryBuilder query = setupBaseQuery(
      reportData.getDecisionDefinitionKey(),
      reportData.getDecisionDefinitionVersion()
    );
    queryFilterEnhancer.addFilterToQuery(query, reportData.getFilter());

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(query)
      .fetchSource(false)
      .aggregation(createAggregation())
      .size(0);
    SearchRequest searchRequest =
      new SearchRequest(getOptimizeIndexAliasForType(DECISION_INSTANCE_TYPE))
        .types(DECISION_INSTANCE_TYPE)
        .source(searchSourceBuilder);

    try {
      final SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
      final DecisionReportMapResultDto mapResultDto = mapToReportResult(response);
      return new SingleDecisionMapReportResult(mapResultDto, reportDefinition);
    } catch (IOException e) {
      String reason =
        String.format(
          "Could not evaluate count decision instance frequency grouped by matched rule report " +
            "for decision definition with key [%s] and version [%s]",
          reportData.getDecisionDefinitionKey(),
          reportData.getDecisionDefinitionVersion()
        );
      logger.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }
  }

  @Override
  protected void sortResultData(final SingleDecisionMapReportResult evaluationResult) {
    ((DecisionReportDataDto) getReportData()).getParameters().getSorting().ifPresent(
      sorting -> MapResultSortingUtility.sortResultData(sorting, evaluationResult)
    );
  }

  private AggregationBuilder createAggregation() {
    return AggregationBuilders
      .terms(MATCHED_RULES_AGGREGATION)
      .size(configurationService.getEsAggregationBucketLimit())
      .field(MATCHED_RULES);
  }

  private DecisionReportMapResultDto mapToReportResult(final SearchResponse response) {
    final DecisionReportMapResultDto resultDto = new DecisionReportMapResultDto();

    final Terms matchedRuleTerms = response.getAggregations().get(MATCHED_RULES_AGGREGATION);
    final Map<String, Long> resultData = new LinkedHashMap<>();
    for (Terms.Bucket b : matchedRuleTerms.getBuckets()) {
      resultData.put(b.getKeyAsString(), b.getDocCount());
    }

    resultDto.getData(resultData);
    resultDto.setComplete(matchedRuleTerms.getSumOfOtherDocCounts() == 0L);
    resultDto.setDecisionInstanceCount(response.getHits().getTotalHits());

    return resultDto;
  }

}
