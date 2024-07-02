/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service;

import io.camunda.optimize.dto.engine.HistoricIdentityLinkLogDto;
import io.camunda.optimize.dto.optimize.importing.IdentityLinkLogEntryDto;
import io.camunda.optimize.dto.optimize.importing.IdentityLinkLogType;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.writer.usertask.IdentityLinkLogWriter;
import io.camunda.optimize.service.identity.PlatformUserTaskIdentityCache;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.DatabaseImportJobExecutor;
import io.camunda.optimize.service.importing.engine.service.definition.ProcessDefinitionResolverService;
import io.camunda.optimize.service.importing.job.IdentityLinkLogImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IdentityLinkLogImportService implements ImportService<HistoricIdentityLinkLogDto> {

  private static final Set<IdentityLinkLogType> SUPPORTED_TYPES =
      Set.of(IdentityLinkLogType.ASSIGNEE, IdentityLinkLogType.CANDIDATE);

  private final DatabaseImportJobExecutor databaseImportJobExecutor;
  private final EngineContext engineContext;
  private final IdentityLinkLogWriter identityLinkLogWriter;
  private final PlatformUserTaskIdentityCache platformUserTaskIdentityCache;
  private final ProcessDefinitionResolverService processDefinitionResolverService;
  private final ConfigurationService configurationService;
  private final DatabaseClient databaseClient;

  public IdentityLinkLogImportService(
      final ConfigurationService configurationService,
      final IdentityLinkLogWriter identityLinkLogWriter,
      final PlatformUserTaskIdentityCache platformUserTaskIdentityCache,
      final EngineContext engineContext,
      final ProcessDefinitionResolverService processDefinitionResolverService,
      final DatabaseClient databaseClient) {
    this.databaseImportJobExecutor =
        new DatabaseImportJobExecutor(getClass().getSimpleName(), configurationService);
    this.identityLinkLogWriter = identityLinkLogWriter;
    this.platformUserTaskIdentityCache = platformUserTaskIdentityCache;
    this.engineContext = engineContext;
    this.processDefinitionResolverService = processDefinitionResolverService;
    this.configurationService = configurationService;
    this.databaseClient = databaseClient;
  }

  @Override
  public void executeImport(
      final List<HistoricIdentityLinkLogDto> pageOfEngineEntities,
      final Runnable importCompleteCallback) {
    log.trace("Importing identity link logs from engine...");

    final boolean newDataIsAvailable = !pageOfEngineEntities.isEmpty();
    if (newDataIsAvailable) {
      final List<IdentityLinkLogEntryDto> newOptimizeEntities =
          filterAndMapEngineEntitiesToOptimizeEntities(pageOfEngineEntities);
      final DatabaseImportJob<IdentityLinkLogEntryDto> databaseImportJob =
          createDatabaseImportJob(newOptimizeEntities, importCompleteCallback);
      addDatabaseImportJobToQueue(databaseImportJob);
    }
  }

  @Override
  public DatabaseImportJobExecutor getDatabaseImportJobExecutor() {
    return databaseImportJobExecutor;
  }

  private void addDatabaseImportJobToQueue(
      final DatabaseImportJob<IdentityLinkLogEntryDto> databaseImportJob) {
    databaseImportJobExecutor.executeImportJob(databaseImportJob);
  }

  private List<IdentityLinkLogEntryDto> filterAndMapEngineEntitiesToOptimizeEntities(
      final List<HistoricIdentityLinkLogDto> engineEntities) {
    return engineEntities.stream()
        .filter(instance -> instance.getProcessInstanceId() != null)
        .map(
            identityLinkLog ->
                processDefinitionResolverService.enrichEngineDtoWithDefinitionKey(
                    engineContext,
                    identityLinkLog,
                    HistoricIdentityLinkLogDto::getProcessDefinitionKey,
                    HistoricIdentityLinkLogDto::getProcessDefinitionId,
                    HistoricIdentityLinkLogDto::setProcessDefinitionKey))
        .filter(identityLinkLog -> identityLinkLog.getProcessDefinitionKey() != null)
        .map(this::mapEngineEntityToOptimizeEntity)
        .filter(entry -> SUPPORTED_TYPES.contains(entry.getType()))
        .collect(Collectors.toList());
  }

  private DatabaseImportJob<IdentityLinkLogEntryDto> createDatabaseImportJob(
      final List<IdentityLinkLogEntryDto> identityLinkLogs, final Runnable callback) {
    final IdentityLinkLogImportJob importJob =
        new IdentityLinkLogImportJob(
            identityLinkLogWriter,
            platformUserTaskIdentityCache,
            configurationService,
            callback,
            databaseClient);
    importJob.setEntitiesToImport(identityLinkLogs);
    return importJob;
  }

  private IdentityLinkLogEntryDto mapEngineEntityToOptimizeEntity(
      final HistoricIdentityLinkLogDto engineEntity) {
    return new IdentityLinkLogEntryDto(
        engineEntity.getId(),
        engineEntity.getProcessInstanceId(),
        engineEntity.getProcessDefinitionKey(),
        engineContext.getEngineAlias(),
        Optional.ofNullable(engineEntity.getType())
            .map(String::toUpperCase)
            .map(IdentityLinkLogType::valueOf)
            .orElse(null),
        engineEntity.getUserId(),
        engineEntity.getGroupId(),
        engineEntity.getTaskId(),
        engineEntity.getOperationType(),
        engineEntity.getAssignerId(),
        engineEntity.getTime());
  }
}
