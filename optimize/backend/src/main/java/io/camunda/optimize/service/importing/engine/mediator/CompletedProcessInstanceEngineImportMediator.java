/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.mediator;

import io.camunda.optimize.dto.engine.HistoricProcessInstanceDto;
import io.camunda.optimize.service.importing.TimestampBasedImportMediator;
import io.camunda.optimize.service.importing.engine.fetcher.instance.CompletedProcessInstanceFetcher;
import io.camunda.optimize.service.importing.engine.handler.CompletedProcessInstanceImportIndexHandler;
import io.camunda.optimize.service.importing.engine.service.CompletedProcessInstanceImportService;
import io.camunda.optimize.service.util.BackoffCalculator;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CompletedProcessInstanceEngineImportMediator
    extends TimestampBasedImportMediator<
        CompletedProcessInstanceImportIndexHandler, HistoricProcessInstanceDto> {

  private final CompletedProcessInstanceFetcher engineEntityFetcher;

  public CompletedProcessInstanceEngineImportMediator(
      final CompletedProcessInstanceImportIndexHandler importIndexHandler,
      final CompletedProcessInstanceFetcher engineEntityFetcher,
      final CompletedProcessInstanceImportService importService,
      final ConfigurationService configurationService,
      final BackoffCalculator idleBackoffCalculator) {
    super(configurationService, idleBackoffCalculator, importIndexHandler, importService);
    this.engineEntityFetcher = engineEntityFetcher;
  }

  @Override
  protected OffsetDateTime getTimestamp(
      final HistoricProcessInstanceDto historicProcessInstanceDto) {
    return historicProcessInstanceDto.getEndTime();
  }

  @Override
  protected List<HistoricProcessInstanceDto> getEntitiesNextPage() {
    return engineEntityFetcher.fetchCompletedProcessInstances(importIndexHandler.getNextPage());
  }

  @Override
  protected List<HistoricProcessInstanceDto> getEntitiesLastTimestamp() {
    return engineEntityFetcher.fetchCompletedProcessInstances(
        importIndexHandler.getTimestampOfLastEntity());
  }

  @Override
  protected int getMaxPageSize() {
    return configurationService.getEngineImportProcessInstanceMaxPageSize();
  }

  @Override
  public MediatorRank getRank() {
    return MediatorRank.INSTANCE;
  }
}
