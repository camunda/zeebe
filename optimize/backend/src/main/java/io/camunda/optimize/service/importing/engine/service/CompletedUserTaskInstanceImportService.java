/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service;

import io.camunda.optimize.dto.engine.HistoricUserTaskInstanceDto;
import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.writer.usertask.CompletedUserTaskInstanceWriter;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.DatabaseImportJobExecutor;
import io.camunda.optimize.service.importing.engine.service.definition.ProcessDefinitionResolverService;
import io.camunda.optimize.service.importing.job.CompletedUserTasksDatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompletedUserTaskInstanceImportService
    implements ImportService<HistoricUserTaskInstanceDto> {

  private final DatabaseImportJobExecutor databaseImportJobExecutor;
  private final EngineContext engineContext;
  private final CompletedUserTaskInstanceWriter completedUserTaskInstanceWriter;
  private final ProcessDefinitionResolverService processDefinitionResolverService;
  private final ConfigurationService configurationService;
  private final DatabaseClient databaseClient;

  public CompletedUserTaskInstanceImportService(
      final ConfigurationService configurationService,
      final CompletedUserTaskInstanceWriter completedUserTaskInstanceWriter,
      final EngineContext engineContext,
      final ProcessDefinitionResolverService processDefinitionResolverService,
      final DatabaseClient databaseClient) {
    this.databaseImportJobExecutor =
        new DatabaseImportJobExecutor(getClass().getSimpleName(), configurationService);
    this.engineContext = engineContext;
    this.completedUserTaskInstanceWriter = completedUserTaskInstanceWriter;
    this.processDefinitionResolverService = processDefinitionResolverService;
    this.configurationService = configurationService;
    this.databaseClient = databaseClient;
  }

  @Override
  public void executeImport(
      final List<HistoricUserTaskInstanceDto> pageOfEngineEntities,
      Runnable importCompleteCallback) {
    log.trace("Importing completed user task entities from engine...");

    final boolean newDataIsAvailable = !pageOfEngineEntities.isEmpty();
    if (newDataIsAvailable) {
      final List<FlowNodeInstanceDto> newOptimizeEntities =
          mapEngineEntitiesToOptimizeEntities(pageOfEngineEntities);
      final DatabaseImportJob<FlowNodeInstanceDto> databaseImportJob =
          createDatabaseImportJob(newOptimizeEntities, importCompleteCallback);
      addDatabaseImportJobToQueue(databaseImportJob);
    }
  }

  @Override
  public DatabaseImportJobExecutor getDatabaseImportJobExecutor() {
    return databaseImportJobExecutor;
  }

  private void addDatabaseImportJobToQueue(final DatabaseImportJob databaseImportJob) {
    databaseImportJobExecutor.executeImportJob(databaseImportJob);
  }

  private List<FlowNodeInstanceDto> mapEngineEntitiesToOptimizeEntities(
      final List<HistoricUserTaskInstanceDto> engineEntities) {
    return engineEntities.stream()
        .filter(instance -> instance.getProcessInstanceId() != null)
        .map(
            userTask ->
                processDefinitionResolverService.enrichEngineDtoWithDefinitionKey(
                    engineContext,
                    userTask,
                    HistoricUserTaskInstanceDto::getProcessDefinitionKey,
                    HistoricUserTaskInstanceDto::getProcessDefinitionId,
                    HistoricUserTaskInstanceDto::setProcessDefinitionKey))
        .filter(userTask -> userTask.getProcessDefinitionKey() != null)
        .map(this::mapEngineEntityToOptimizeEntity)
        .collect(Collectors.toList());
  }

  private DatabaseImportJob<FlowNodeInstanceDto> createDatabaseImportJob(
      final List<FlowNodeInstanceDto> userTasks, final Runnable callback) {
    final CompletedUserTasksDatabaseImportJob importJob =
        new CompletedUserTasksDatabaseImportJob(
            completedUserTaskInstanceWriter, configurationService, callback, databaseClient);
    importJob.setEntitiesToImport(userTasks);
    return importJob;
  }

  private FlowNodeInstanceDto mapEngineEntityToOptimizeEntity(
      final HistoricUserTaskInstanceDto engineEntity) {
    return new FlowNodeInstanceDto(
            engineEntity.getProcessDefinitionKey(),
            engineContext.getEngineAlias(),
            engineEntity.getProcessInstanceId(),
            engineEntity.getTaskDefinitionKey(),
            engineEntity.getActivityInstanceId(),
            engineEntity.getId())
        .setTotalDurationInMs(engineEntity.getDuration())
        .setStartDate(engineEntity.getStartTime())
        .setEndDate(engineEntity.getEndTime())
        .setDueDate(engineEntity.getDue())
        .setDeleteReason(engineEntity.getDeleteReason())
        // HistoricUserTaskInstanceDto does not have a bool canceled field. To avoid having to parse
        // the deleteReason,
        // canceled defaults to false and writers do not overwrite existing canceled states.
        // The completedActivityInstanceWriter will overwrite the correct state.
        .setCanceled(false);
  }
}
