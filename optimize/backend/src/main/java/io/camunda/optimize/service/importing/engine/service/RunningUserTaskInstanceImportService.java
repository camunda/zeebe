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
import io.camunda.optimize.service.db.writer.usertask.RunningUserTaskInstanceWriter;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.DatabaseImportJobExecutor;
import io.camunda.optimize.service.importing.engine.service.definition.ProcessDefinitionResolverService;
import io.camunda.optimize.service.importing.job.RunningUserTaskDatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunningUserTaskInstanceImportService
    implements ImportService<HistoricUserTaskInstanceDto> {

  private final DatabaseImportJobExecutor databaseImportJobExecutor;
  private final EngineContext engineContext;
  private final RunningUserTaskInstanceWriter runningUserTaskInstanceWriter;
  private final ProcessDefinitionResolverService processDefinitionResolverService;
  private final ConfigurationService configurationService;
  private final DatabaseClient databaseClient;

  public RunningUserTaskInstanceImportService(
      final ConfigurationService configurationService,
      final RunningUserTaskInstanceWriter runningUserTaskInstanceWriter,
      final EngineContext engineContext,
      final ProcessDefinitionResolverService processDefinitionResolverService,
      final DatabaseClient databaseClient) {
    this.databaseImportJobExecutor =
        new DatabaseImportJobExecutor(getClass().getSimpleName(), configurationService);
    this.engineContext = engineContext;
    this.runningUserTaskInstanceWriter = runningUserTaskInstanceWriter;
    this.processDefinitionResolverService = processDefinitionResolverService;
    this.configurationService = configurationService;
    this.databaseClient = databaseClient;
  }

  @Override
  public void executeImport(
      final List<HistoricUserTaskInstanceDto> pageOfEngineEntities,
      Runnable importCompleteCallback) {
    log.trace("Importing running user task entities from engine...");

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
      final List<FlowNodeInstanceDto> userTasks, Runnable callback) {
    final RunningUserTaskDatabaseImportJob importJob =
        new RunningUserTaskDatabaseImportJob(
            runningUserTaskInstanceWriter, configurationService, callback, databaseClient);
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
        .setStartDate(engineEntity.getStartTime())
        .setDueDate(engineEntity.getDue())
        .setDeleteReason(engineEntity.getDeleteReason())
        // HistoricUserTaskInstanceDto does not have a bool canceled field. To avoid having to parse
        // the deleteReason,
        // canceled defaults to false and writers do not overwrite existing canceled states.
        // The completedActivityInstanceWriter will overwrite the correct state.
        .setCanceled(false);
  }
}
