/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.fetcher.definition;

import static io.camunda.optimize.service.util.importing.EngineConstants.DEPLOYED_AFTER;
import static io.camunda.optimize.service.util.importing.EngineConstants.DEPLOYED_AT;
import static io.camunda.optimize.service.util.importing.EngineConstants.DEPLOYMENT_ENDPOINT_TEMPLATE;
import static io.camunda.optimize.service.util.importing.EngineConstants.MAX_RESULTS_TO_RETURN;
import static io.camunda.optimize.service.util.importing.EngineConstants.SORT_BY;
import static io.camunda.optimize.service.util.importing.EngineConstants.SORT_ORDER;
import static io.camunda.optimize.service.util.importing.EngineConstants.SORT_ORDER_ASC;

import io.camunda.optimize.dto.engine.DeploymentEngineDto;
import io.camunda.optimize.dto.engine.definition.DefinitionEngineDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.importing.engine.fetcher.instance.RetryBackoffEngineEntityFetcher;
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
public abstract class DefinitionFetcher<DEF extends DefinitionEngineDto>
    extends RetryBackoffEngineEntityFetcher {

  private DateTimeFormatter dateTimeFormatter;

  public DefinitionFetcher(final EngineContext engineContext) {
    super(engineContext);
  }

  @PostConstruct
  public void init() {
    dateTimeFormatter = DateTimeFormatter.ofPattern(configurationService.getEngineDateFormat());
  }

  public List<DEF> fetchDefinitions(final TimestampBasedImportPage nextPage) {
    return fetchDefinitions(nextPage.getTimestampOfLastEntity(), getMaxPageSize());
  }

  public List<DEF> fetchDefinitionsForTimestamp(
      final OffsetDateTime deploymentTimeOfLastDefinition) {
    logger.debug("Fetching definitions ...");
    long requestStart = System.currentTimeMillis();
    List<DEF> definitions =
        fetchWithRetry(() -> performDefinitionRequest(deploymentTimeOfLastDefinition));
    long requestEnd = System.currentTimeMillis();
    logger.debug(
        "Fetched [{}] definitions for set deployment time within [{}] ms",
        definitions.size(),
        requestEnd - requestStart);
    return definitions;
  }

  /**
   * We need to explicitly create the response type during runtime else java won't be to extract the
   * specific type and stick to DefinitionEngineDto which would not contain all the necessary
   * fields.
   */
  protected abstract GenericType<List<DEF>> getResponseType();

  protected abstract String getDefinitionEndpoint();

  protected abstract int getMaxPageSize();

  private List<DEF> fetchDefinitions(final OffsetDateTime timeStamp, final int pageSize) {
    logger.debug("Fetching definitions ...");
    long requestStart = System.currentTimeMillis();
    List<DEF> entries = fetchWithRetry(() -> performDefinitionRequest(timeStamp, pageSize));
    long requestEnd = System.currentTimeMillis();
    logger.debug(
        "Fetched [{}] definitions which were deployed after set timestamp with page size [{}] within [{}] ms",
        entries.size(),
        pageSize,
        requestEnd - requestStart);

    addDeploymentTimeToDefinitionDtos(entries);
    return entries;
  }

  private void addDeploymentTimeToDefinitionDtos(final List<DEF> entries) {
    if (!entries.isEmpty()) {
      final DEF lastDefinitionEntry = entries.get(entries.size() - 1);
      final DeploymentEngineDto deploymentEngineDto =
          fetchWithRetry(() -> performDeploymentRequest(lastDefinitionEntry.getDeploymentId()));
      entries.forEach(entry -> entry.setDeploymentTime(deploymentEngineDto.getDeploymentTime()));
    }
  }

  private DeploymentEngineDto performDeploymentRequest(final String deploymentId) {
    return getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(DEPLOYMENT_ENDPOINT_TEMPLATE)
        .resolveTemplate("id", deploymentId)
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(DeploymentEngineDto.class);
  }

  private List<DEF> performDefinitionRequest(final OffsetDateTime timeStamp, final long pageSize) {
    return getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(getDefinitionEndpoint())
        .queryParam(DEPLOYED_AFTER, dateTimeFormatter.format(timeStamp))
        .queryParam(MAX_RESULTS_TO_RETURN, pageSize)
        .queryParam(SORT_BY, "deployTime")
        .queryParam(SORT_ORDER, SORT_ORDER_ASC)
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(getResponseType());
  }

  private List<DEF> performDefinitionRequest(OffsetDateTime deploymentTimeOfLastDefinition) {
    return getEngineClient()
        .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
        .path(getDefinitionEndpoint())
        .queryParam(DEPLOYED_AT, dateTimeFormatter.format(deploymentTimeOfLastDefinition))
        .queryParam(MAX_RESULTS_TO_RETURN, getMaxPageSize())
        .request(MediaType.APPLICATION_JSON)
        .acceptEncoding(UTF8)
        .get(getResponseType());
  }
}
