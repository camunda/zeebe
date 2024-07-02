/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.fetcher.instance;

import static io.camunda.optimize.service.util.importing.EngineConstants.IDENTITY_LINK_LOG_ENDPOINT;
import static io.camunda.optimize.service.util.importing.EngineConstants.MAX_RESULTS_TO_RETURN;
import static io.camunda.optimize.service.util.importing.EngineConstants.OCCURRED_AFTER;
import static io.camunda.optimize.service.util.importing.EngineConstants.OCCURRED_AT;

import io.camunda.optimize.dto.engine.HistoricIdentityLinkLogDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.importing.page.TimestampBasedImportPage;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.client.WebTarget;
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
public class IdentityLinkLogInstanceFetcher extends RetryBackoffEngineEntityFetcher {

  private DateTimeFormatter dateTimeFormatter;

  public IdentityLinkLogInstanceFetcher(final EngineContext engineContext) {
    super(engineContext);
  }

  @PostConstruct
  public void init() {
    dateTimeFormatter = DateTimeFormatter.ofPattern(configurationService.getEngineDateFormat());
  }

  public List<HistoricIdentityLinkLogDto> fetchIdentityLinkLogs(
      final TimestampBasedImportPage page) {
    return fetchIdentityLinkLogs(
        page.getTimestampOfLastEntity(),
        configurationService.getEngineImportIdentityLinkLogsMaxPageSize());
  }

  public List<HistoricIdentityLinkLogDto> fetchIdentityLinkLogsForTimestamp(
      final OffsetDateTime endTimeOfLastInstance) {
    logger.debug("Fetching identity link logs ...");

    final long requestStart = System.currentTimeMillis();
    final List<HistoricIdentityLinkLogDto> secondEntries =
        fetchWithRetry(() -> performIdentityLinkLogRequest(endTimeOfLastInstance));
    final long requestEnd = System.currentTimeMillis();

    logger.debug(
        "Fetched [{}] identity link logs within [{}] ms",
        secondEntries.size(),
        requestEnd - requestStart);
    return secondEntries;
  }

  private List<HistoricIdentityLinkLogDto> fetchIdentityLinkLogs(
      final OffsetDateTime timeStamp, final long pageSize) {
    logger.debug("Fetching identity link logs ...");

    final long requestStart = System.currentTimeMillis();
    final List<HistoricIdentityLinkLogDto> entries =
        fetchWithRetry(() -> performIdentityLinkLogRequest(timeStamp, pageSize));
    final long requestEnd = System.currentTimeMillis();

    logger.debug(
        "Fetched [{}] identity links which occurred after set timestamp with page size [{}] within [{}] ms",
        entries.size(),
        pageSize,
        requestEnd - requestStart);

    return entries;
  }

  private List<HistoricIdentityLinkLogDto> performIdentityLinkLogRequest(
      final OffsetDateTime timeStamp, final long pageSize) {
    // @formatter:off
    return createIdentityLinkLogWebTarget()
        .queryParam(OCCURRED_AFTER, dateTimeFormatter.format(timeStamp))
        .queryParam(MAX_RESULTS_TO_RETURN, pageSize)
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(new GenericType<>() {});
    // @formatter:on
  }

  private List<HistoricIdentityLinkLogDto> performIdentityLinkLogRequest(
      final OffsetDateTime endTimeOfLastInstance) {
    // @formatter:off
    return createIdentityLinkLogWebTarget()
        .queryParam(OCCURRED_AT, dateTimeFormatter.format(endTimeOfLastInstance))
        .queryParam(
            MAX_RESULTS_TO_RETURN,
            configurationService.getEngineImportIdentityLinkLogsMaxPageSize())
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(new GenericType<>() {});
    // @formatter:on
  }

  private WebTarget createIdentityLinkLogWebTarget() {
    return getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(IDENTITY_LINK_LOG_ENDPOINT);
  }
}
