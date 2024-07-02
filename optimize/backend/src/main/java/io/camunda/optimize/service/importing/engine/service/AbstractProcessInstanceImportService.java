/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service;

import io.camunda.optimize.dto.engine.HistoricProcessInstanceDto;
import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
import io.camunda.optimize.plugin.BusinessKeyImportAdapterProvider;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.DatabaseImportJobExecutor;
import io.camunda.optimize.service.importing.engine.service.definition.ProcessDefinitionResolverService;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProcessInstanceImportService
    implements ImportService<HistoricProcessInstanceDto> {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final DatabaseImportJobExecutor databaseImportJobExecutor;
  protected final EngineContext engineContext;
  protected final BusinessKeyImportAdapterProvider businessKeyImportAdapterProvider;
  private final ProcessDefinitionResolverService processDefinitionResolverService;
  protected final ConfigurationService configurationService;
  protected final DatabaseClient databaseClient;

  public AbstractProcessInstanceImportService(
      final ConfigurationService configurationService,
      final EngineContext engineContext,
      final BusinessKeyImportAdapterProvider businessKeyImportAdapterProvider,
      final ProcessDefinitionResolverService processDefinitionResolverService,
      final DatabaseClient databaseClient) {
    this.databaseImportJobExecutor =
        new DatabaseImportJobExecutor(getClass().getSimpleName(), configurationService);
    this.engineContext = engineContext;
    this.businessKeyImportAdapterProvider = businessKeyImportAdapterProvider;
    this.processDefinitionResolverService = processDefinitionResolverService;
    this.configurationService = configurationService;
    this.databaseClient = databaseClient;
  }

  protected abstract DatabaseImportJob<ProcessInstanceDto> createDatabaseImportJob(
      List<ProcessInstanceDto> processInstances, Runnable callback);

  protected abstract ProcessInstanceDto mapEngineEntityToOptimizeEntity(
      HistoricProcessInstanceDto engineEntity);

  @Override
  public void executeImport(
      final List<HistoricProcessInstanceDto> pageOfEngineEntities,
      final Runnable importCompleteCallback) {
    log.trace("Importing entities from engine...");

    boolean newDataIsAvailable = !pageOfEngineEntities.isEmpty();
    if (newDataIsAvailable) {
      List<ProcessInstanceDto> newOptimizeEntities =
          mapEngineEntitiesToOptimizeEntitiesAndApplyPlugins(pageOfEngineEntities);
      DatabaseImportJob<ProcessInstanceDto> databaseImportJob =
          createDatabaseImportJob(newOptimizeEntities, importCompleteCallback);
      addDatabaseImportJobToQueue(databaseImportJob);
    }
  }

  @Override
  public DatabaseImportJobExecutor getDatabaseImportJobExecutor() {
    return databaseImportJobExecutor;
  }

  private void addDatabaseImportJobToQueue(
      final DatabaseImportJob<ProcessInstanceDto> databaseImportJob) {
    databaseImportJobExecutor.executeImportJob(databaseImportJob);
  }

  private List<ProcessInstanceDto> mapEngineEntitiesToOptimizeEntitiesAndApplyPlugins(
      final List<HistoricProcessInstanceDto> engineEntities) {
    return engineEntities.stream()
        .map(
            instance ->
                processDefinitionResolverService.enrichEngineDtoWithDefinitionKey(
                    engineContext,
                    instance,
                    HistoricProcessInstanceDto::getProcessDefinitionKey,
                    HistoricProcessInstanceDto::getProcessDefinitionId,
                    HistoricProcessInstanceDto::setProcessDefinitionKey))
        .filter(instance -> instance.getProcessDefinitionKey() != null)
        .map(this::mapEngineEntityToOptimizeEntity)
        .map(this::applyPlugins)
        .toList();
  }

  private ProcessInstanceDto applyPlugins(final ProcessInstanceDto processInstanceDto) {
    businessKeyImportAdapterProvider
        .getPlugins()
        .forEach(
            businessKeyImportAdapter ->
                processInstanceDto.setBusinessKey(
                    businessKeyImportAdapter.adaptBusinessKey(
                        processInstanceDto.getBusinessKey())));
    return processInstanceDto;
  }
}
