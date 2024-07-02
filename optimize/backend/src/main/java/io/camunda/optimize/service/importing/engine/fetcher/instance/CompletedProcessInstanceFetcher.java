/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.fetcher.instance;

import static io.camunda.optimize.service.util.importing.EngineConstants.COMPLETED_PROCESS_INSTANCE_ENDPOINT;
import static io.camunda.optimize.service.util.importing.EngineConstants.FINISHED_AFTER;
import static io.camunda.optimize.service.util.importing.EngineConstants.FINISHED_AT;
import static io.camunda.optimize.service.util.importing.EngineConstants.MAX_RESULTS_TO_RETURN;

import io.camunda.optimize.dto.engine.HistoricProcessInstanceDto;
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
public class CompletedProcessInstanceFetcher extends RetryBackoffEngineEntityFetcher {

  private DateTimeFormatter dateTimeFormatter;

  public CompletedProcessInstanceFetcher(final EngineContext engineContext) {
    super(engineContext);
  }

  @PostConstruct
  public void init() {
    dateTimeFormatter = DateTimeFormatter.ofPattern(configurationService.getEngineDateFormat());
  }

  public List<HistoricProcessInstanceDto> fetchCompletedProcessInstances(
      TimestampBasedImportPage page) {
    return fetchCompletedProcessInstances(
        page.getTimestampOfLastEntity(),
        configurationService.getEngineImportProcessInstanceMaxPageSize());
  }

  public List<HistoricProcessInstanceDto> fetchCompletedProcessInstances(
      OffsetDateTime endTimeOfLastInstance) {
    logger.debug("Fetching completed historic process instances...");
    long requestStart = System.currentTimeMillis();
    List<HistoricProcessInstanceDto> secondEntries =
        fetchWithRetry(() -> performCompletedProcessInstanceRequest(endTimeOfLastInstance));
    long requestEnd = System.currentTimeMillis();
    logger.debug(
        "Fetched [{}] completed historic process instances for set end time within [{}] ms",
        secondEntries.size(),
        requestEnd - requestStart);
    return secondEntries;
  }

  private List<HistoricProcessInstanceDto> fetchCompletedProcessInstances(
      OffsetDateTime timeStamp, long pageSize) {
    logger.debug("Fetching completed historic process instances...");
    long requestStart = System.currentTimeMillis();
    List<HistoricProcessInstanceDto> entries =
        fetchWithRetry(() -> performCompletedProcessInstanceRequest(timeStamp, pageSize));
    long requestEnd = System.currentTimeMillis();
    logger.debug(
        "Fetched [{}] completed historic process instances which ended after "
            + "set timestamp with page size [{}] within [{}] ms",
        entries.size(),
        pageSize,
        requestEnd - requestStart);
    return entries;
  }

  private List<HistoricProcessInstanceDto> performCompletedProcessInstanceRequest(
      OffsetDateTime timeStamp, long pageSize) {
    return getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(COMPLETED_PROCESS_INSTANCE_ENDPOINT)
        .queryParam(FINISHED_AFTER, dateTimeFormatter.format(timeStamp))
        .queryParam(MAX_RESULTS_TO_RETURN, pageSize)
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(new GenericType<>() {});
  }

  private List<HistoricProcessInstanceDto> performCompletedProcessInstanceRequest(
      OffsetDateTime endTimeOfLastInstance) {
    return getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(COMPLETED_PROCESS_INSTANCE_ENDPOINT)
        .queryParam(FINISHED_AT, dateTimeFormatter.format(endTimeOfLastInstance))
        .queryParam(
            MAX_RESULTS_TO_RETURN, configurationService.getEngineImportProcessInstanceMaxPageSize())
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(new GenericType<>() {});
  }
}
