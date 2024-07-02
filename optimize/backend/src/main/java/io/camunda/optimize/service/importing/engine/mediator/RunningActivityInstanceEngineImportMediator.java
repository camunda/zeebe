/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.mediator;

import io.camunda.optimize.dto.engine.HistoricActivityInstanceEngineDto;
import io.camunda.optimize.service.importing.TimestampBasedImportMediator;
import io.camunda.optimize.service.importing.engine.fetcher.instance.RunningActivityInstanceFetcher;
import io.camunda.optimize.service.importing.engine.handler.RunningActivityInstanceImportIndexHandler;
import io.camunda.optimize.service.importing.engine.service.RunningActivityInstanceImportService;
import io.camunda.optimize.service.util.BackoffCalculator;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RunningActivityInstanceEngineImportMediator
    extends TimestampBasedImportMediator<
        RunningActivityInstanceImportIndexHandler, HistoricActivityInstanceEngineDto> {

  private final RunningActivityInstanceFetcher engineEntityFetcher;

  public RunningActivityInstanceEngineImportMediator(
      final RunningActivityInstanceImportIndexHandler importIndexHandler,
      final RunningActivityInstanceFetcher engineEntityFetcher,
      final RunningActivityInstanceImportService importService,
      final ConfigurationService configurationService,
      final BackoffCalculator idleBackoffCalculator) {
    super(configurationService, idleBackoffCalculator, importIndexHandler, importService);
    this.engineEntityFetcher = engineEntityFetcher;
  }

  @Override
  protected OffsetDateTime getTimestamp(
      final HistoricActivityInstanceEngineDto historicActivityInstanceEngineDto) {
    return historicActivityInstanceEngineDto.getStartTime();
  }

  @Override
  protected List<HistoricActivityInstanceEngineDto> getEntitiesNextPage() {
    return engineEntityFetcher.fetchRunningActivityInstances(importIndexHandler.getNextPage());
  }

  @Override
  protected List<HistoricActivityInstanceEngineDto> getEntitiesLastTimestamp() {
    return engineEntityFetcher.fetchRunningActivityInstancesForTimestamp(
        importIndexHandler.getTimestampOfLastEntity());
  }

  @Override
  protected int getMaxPageSize() {
    return configurationService.getEngineImportActivityInstanceMaxPageSize();
  }

  @Override
  public MediatorRank getRank() {
    return MediatorRank.INSTANCE_SUB_ENTITIES;
  }
}
