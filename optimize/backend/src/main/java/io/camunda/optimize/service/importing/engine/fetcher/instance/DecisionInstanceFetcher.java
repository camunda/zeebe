/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.fetcher.instance;

import static io.camunda.optimize.service.util.importing.EngineConstants.DECISION_INSTANCE_ENDPOINT;
import static io.camunda.optimize.service.util.importing.EngineConstants.MAX_RESULTS_TO_RETURN;

import io.camunda.optimize.dto.engine.HistoricDecisionInstanceDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.importing.page.TimestampBasedImportPage;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DecisionInstanceFetcher extends RetryBackoffEngineEntityFetcher {
  private static final String EVALUATED_AFTER = "evaluatedAfter";
  private static final String EVALUATED_AT = "evaluatedAt";

  private DateTimeFormatter dateTimeFormatter;

  public DecisionInstanceFetcher(final EngineContext engineContext) {
    super(engineContext);
  }

  @PostConstruct
  public void init() {
    dateTimeFormatter = DateTimeFormatter.ofPattern(configurationService.getEngineDateFormat());
  }

  public List<HistoricDecisionInstanceDto> fetchHistoricDecisionInstances(
      final TimestampBasedImportPage page) {
    return fetchHistoricDecisionInstances(
        page.getTimestampOfLastEntity(),
        configurationService.getEngineImportDecisionInstanceMaxPageSize());
  }

  public List<HistoricDecisionInstanceDto> fetchHistoricDecisionInstances(
      final OffsetDateTime endTimeOfLastInstance) {
    logger.debug("Fetching historic decision instances...");

    final long requestStart = System.currentTimeMillis();
    final List<HistoricDecisionInstanceDto> secondEntries =
        fetchWithRetry(() -> performGetHistoricDecisionInstancesRequest(endTimeOfLastInstance));
    final long requestEnd = System.currentTimeMillis();

    logger.debug(
        "Fetched [{}] historic decision instances for set end time within [{}] ms",
        secondEntries.size(),
        requestEnd - requestStart);
    return secondEntries;
  }

  public DateTimeFormatter getDateTimeFormatter() {
    return dateTimeFormatter;
  }

  public void setDateTimeFormatter(final DateTimeFormatter dateTimeFormatter) {
    this.dateTimeFormatter = dateTimeFormatter;
  }

  private List<HistoricDecisionInstanceDto> fetchHistoricDecisionInstances(
      final OffsetDateTime timeStamp, final long pageSize) {
    logger.debug("Fetching historic decision instances...");
    final long requestStart = System.currentTimeMillis();
    final List<HistoricDecisionInstanceDto> entries =
        fetchWithRetry(() -> performGetHistoricDecisionInstancesRequest(timeStamp, pageSize));
    final long requestEnd = System.currentTimeMillis();
    logger.debug(
        "Fetched [{}] historic decision instances which ended after set timestamp with page size [{}] within [{}] ms",
        entries.size(),
        pageSize,
        requestEnd - requestStart);
    return entries;
  }

  private List<HistoricDecisionInstanceDto> performGetHistoricDecisionInstancesRequest(
      final OffsetDateTime timeStamp, final long pageSize) {
    // @formatter:off
    return getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(DECISION_INSTANCE_ENDPOINT)
        .queryParam(EVALUATED_AFTER, dateTimeFormatter.format(timeStamp))
        .queryParam(MAX_RESULTS_TO_RETURN, pageSize)
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(new GenericType<>() {});
    // @formatter:on
  }

  private List<HistoricDecisionInstanceDto> performGetHistoricDecisionInstancesRequest(
      final OffsetDateTime endTimeOfLastInstance) {
    // @formatter:off
    return getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(DECISION_INSTANCE_ENDPOINT)
        .queryParam(EVALUATED_AT, dateTimeFormatter.format(endTimeOfLastInstance))
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(new GenericType<>() {});
    // @formatter:on
  }
}
