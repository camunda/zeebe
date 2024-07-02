/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.fetcher.instance;

import static io.camunda.optimize.service.util.importing.EngineConstants.MAX_RESULTS_TO_RETURN;
import static io.camunda.optimize.service.util.importing.EngineConstants.OCCURRED_AFTER;
import static io.camunda.optimize.service.util.importing.EngineConstants.OCCURRED_AT;
import static io.camunda.optimize.service.util.importing.EngineConstants.USER_OPERATION_LOG_ENDPOINT;

import io.camunda.optimize.dto.engine.HistoricUserOperationLogDto;
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
public class UserOperationLogFetcher extends RetryBackoffEngineEntityFetcher {
  private DateTimeFormatter dateTimeFormatter;

  public UserOperationLogFetcher(final EngineContext engineContext) {
    super(engineContext);
  }

  @PostConstruct
  public void init() {
    dateTimeFormatter = DateTimeFormatter.ofPattern(configurationService.getEngineDateFormat());
  }

  public List<HistoricUserOperationLogDto> fetchUserOperationLogs(
      final TimestampBasedImportPage page) {
    return fetchUserOperationLogs(
        page.getTimestampOfLastEntity(),
        configurationService.getEngineImportIdentityLinkLogsMaxPageSize());
  }

  public List<HistoricUserOperationLogDto> fetchUserOperationLogsForTimestamp(
      final OffsetDateTime endTimeOfLastInstance) {
    logger.debug("Fetching user operations logs...");

    final long requestStart = System.currentTimeMillis();
    final List<HistoricUserOperationLogDto> secondEntries =
        fetchWithRetry(() -> performUserOperationLogsRequest(endTimeOfLastInstance));
    final long requestEnd = System.currentTimeMillis();

    logger.debug(
        "Fetched [{}] user operation logs within [{}] ms",
        secondEntries.size(),
        requestEnd - requestStart);
    return secondEntries;
  }

  private List<HistoricUserOperationLogDto> fetchUserOperationLogs(
      final OffsetDateTime timeStamp, final long pageSize) {
    logger.debug("Fetching user operations logs...");

    final long requestStart = System.currentTimeMillis();
    final List<HistoricUserOperationLogDto> entries =
        fetchWithRetry(() -> performUserOperationLogsRequest(timeStamp, pageSize));
    final long requestEnd = System.currentTimeMillis();

    logger.debug(
        "Fetched [{}] user operations which occurred after set timestamp with page size [{}] within [{}] ms",
        entries.size(),
        pageSize,
        requestEnd - requestStart);

    return entries;
  }

  private List<HistoricUserOperationLogDto> performUserOperationLogsRequest(
      final OffsetDateTime timeStamp, final long pageSize) {
    // @formatter:off
    return createUserOperationLogWebTarget()
        .queryParam(OCCURRED_AFTER, dateTimeFormatter.format(timeStamp))
        .queryParam(MAX_RESULTS_TO_RETURN, pageSize)
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(new GenericType<>() {});
    // @formatter:on
  }

  private List<HistoricUserOperationLogDto> performUserOperationLogsRequest(
      final OffsetDateTime endTimeOfLastInstance) {
    // @formatter:off
    return createUserOperationLogWebTarget()
        .queryParam(OCCURRED_AT, dateTimeFormatter.format(endTimeOfLastInstance))
        .queryParam(
            MAX_RESULTS_TO_RETURN,
            configurationService.getEngineImportUserOperationLogsMaxPageSize())
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(new GenericType<>() {});
    // @formatter:on
  }

  private WebTarget createUserOperationLogWebTarget() {
    return getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(USER_OPERATION_LOG_ENDPOINT);
  }
}
