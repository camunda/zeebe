/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.reader;

import static io.camunda.optimize.service.db.DatabaseConstants.ALERT_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.LIST_FETCH_LIMIT;
import static org.elasticsearch.core.TimeValue.timeValueSeconds;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.query.alert.AlertDefinitionDto;
import io.camunda.optimize.service.db.es.OptimizeElasticsearchClient;
import io.camunda.optimize.service.db.reader.AlertReader;
import io.camunda.optimize.service.db.schema.index.AlertIndex;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.condition.ElasticSearchCondition;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Conditional(ElasticSearchCondition.class)
public class AlertReaderES implements AlertReader {

  private final OptimizeElasticsearchClient esClient;
  private final ConfigurationService configurationService;
  private final ObjectMapper objectMapper;

  @Override
  public long getAlertCount() {
    final CountRequest countRequest = new CountRequest(ALERT_INDEX_NAME);
    try {
      return esClient.count(countRequest).getCount();
    } catch (IOException e) {
      throw new OptimizeRuntimeException("Was not able to retrieve alert count!", e);
    }
  }

  @Override
  public List<AlertDefinitionDto> getStoredAlerts() {
    log.debug("getting all stored alerts");

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(QueryBuilders.matchAllQuery());
    searchSourceBuilder.size(LIST_FETCH_LIMIT);
    SearchRequest searchRequest =
        new SearchRequest(ALERT_INDEX_NAME)
            .source(searchSourceBuilder)
            .scroll(
                timeValueSeconds(
                    configurationService
                        .getElasticSearchConfiguration()
                        .getScrollTimeoutInSeconds()));

    SearchResponse scrollResp;
    try {
      scrollResp = esClient.search(searchRequest);
    } catch (IOException e) {
      log.error("Was not able to retrieve stored alerts!", e);
      throw new OptimizeRuntimeException("Was not able to retrieve stored alerts!", e);
    }

    return ElasticsearchReaderUtil.retrieveAllScrollResults(
        scrollResp,
        AlertDefinitionDto.class,
        objectMapper,
        esClient,
        configurationService.getElasticSearchConfiguration().getScrollTimeoutInSeconds());
  }

  @Override
  public Optional<AlertDefinitionDto> getAlert(String alertId) {
    log.debug("Fetching alert with id [{}]", alertId);
    GetRequest getRequest = new GetRequest(ALERT_INDEX_NAME).id(alertId);

    GetResponse getResponse;
    try {
      getResponse = esClient.get(getRequest);
    } catch (IOException e) {
      String reason = String.format("Could not fetch alert with id [%s]", alertId);
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }

    if (!getResponse.isExists()) {
      return Optional.empty();
    }

    String responseAsString = getResponse.getSourceAsString();

    try {
      return Optional.ofNullable(
          objectMapper.readValue(responseAsString, AlertDefinitionDto.class));
    } catch (IOException e) {
      logError(alertId);
      throw new OptimizeRuntimeException("Can't fetch alert");
    }
  }

  @Override
  public List<AlertDefinitionDto> getAlertsForReport(String reportId) {
    log.debug("Fetching first {} alerts using report with id {}", LIST_FETCH_LIMIT, reportId);

    final QueryBuilder query = QueryBuilders.termQuery(AlertIndex.REPORT_ID, reportId);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(query);
    searchSourceBuilder.size(LIST_FETCH_LIMIT);
    SearchRequest searchRequest = new SearchRequest(ALERT_INDEX_NAME).source(searchSourceBuilder);

    SearchResponse searchResponse;
    try {
      searchResponse = esClient.search(searchRequest);
    } catch (IOException e) {
      String reason =
          String.format("Was not able to fetch alerts for report with id [%s]", reportId);
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }

    return ElasticsearchReaderUtil.mapHits(
        searchResponse.getHits(), AlertDefinitionDto.class, objectMapper);
  }

  @Override
  public List<AlertDefinitionDto> getAlertsForReports(List<String> reportIds) {
    log.debug("Fetching first {} alerts using reports with ids {}", LIST_FETCH_LIMIT, reportIds);

    final QueryBuilder query = QueryBuilders.termsQuery(AlertIndex.REPORT_ID, reportIds);
    SearchSourceBuilder searchSourceBuilder =
        new SearchSourceBuilder().query(query).size(LIST_FETCH_LIMIT);
    SearchRequest searchRequest = new SearchRequest(ALERT_INDEX_NAME).source(searchSourceBuilder);

    SearchResponse searchResponse;
    try {
      searchResponse = esClient.search(searchRequest);
    } catch (IOException e) {
      String reason =
          String.format("Was not able to fetch alerts for reports with ids [%s]", reportIds);
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }

    return ElasticsearchReaderUtil.mapHits(
        searchResponse.getHits(), AlertDefinitionDto.class, objectMapper);
  }

  private void logError(String alertId) {
    log.error("Was not able to retrieve alert with id [{}] from Elasticsearch.", alertId);
  }
}
