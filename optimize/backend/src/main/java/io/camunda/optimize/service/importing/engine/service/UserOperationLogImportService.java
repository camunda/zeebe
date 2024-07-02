/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service;

import static io.camunda.optimize.dto.optimize.importing.UserOperationType.NOT_SUSPENSION_RELATED_OPERATION;
import static io.camunda.optimize.dto.optimize.importing.UserOperationType.isSuspensionViaBatchOperation;

import io.camunda.optimize.dto.engine.HistoricUserOperationLogDto;
import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.importing.UserOperationLogEntryDto;
import io.camunda.optimize.dto.optimize.importing.UserOperationType;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.writer.RunningProcessInstanceWriter;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.DatabaseImportJobExecutor;
import io.camunda.optimize.service.importing.engine.handler.RunningProcessInstanceImportIndexHandler;
import io.camunda.optimize.service.importing.engine.service.definition.ProcessDefinitionResolverService;
import io.camunda.optimize.service.importing.job.UserOperationLogDatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserOperationLogImportService implements ImportService<HistoricUserOperationLogDto> {

  private final RunningProcessInstanceWriter runningProcessInstanceWriter;
  private final DatabaseImportJobExecutor databaseImportJobExecutor;
  private final RunningProcessInstanceImportIndexHandler runningProcessInstanceImportIndexHandler;
  private final ProcessDefinitionResolverService processDefinitionResolverService;
  private final ProcessInstanceResolverService processInstanceResolverService;
  private final DatabaseClient databaseClient;

  public UserOperationLogImportService(
      final ConfigurationService configurationService,
      final RunningProcessInstanceWriter runningProcessInstanceWriter,
      final RunningProcessInstanceImportIndexHandler runningProcessInstanceImportIndexHandler,
      final ProcessDefinitionResolverService processDefinitionResolverService,
      final ProcessInstanceResolverService processInstanceResolverService,
      final DatabaseClient databaseClient) {
    this.databaseImportJobExecutor =
        new DatabaseImportJobExecutor(getClass().getSimpleName(), configurationService);
    this.runningProcessInstanceWriter = runningProcessInstanceWriter;
    this.runningProcessInstanceImportIndexHandler = runningProcessInstanceImportIndexHandler;
    this.processDefinitionResolverService = processDefinitionResolverService;
    this.processInstanceResolverService = processInstanceResolverService;
    this.databaseClient = databaseClient;
  }

  /**
   * Triggers a reimport of relevant process instances for suspension of instances and/or suspension
   * of definitions. Batch suspension operations are handled by restarting the running process
   * instance import from scratch as it is not possible to determine the affected instances
   * otherwise.
   */
  @Override
  public void executeImport(
      final List<HistoricUserOperationLogDto> pageOfEngineEntities,
      Runnable importCompleteCallback) {
    log.trace("Importing suspension related user operation logs from engine...");

    final boolean newDataIsAvailable = !pageOfEngineEntities.isEmpty();
    if (newDataIsAvailable) {
      final List<UserOperationLogEntryDto> newOptimizeEntities =
          filterSuspensionOperationsAndMapToOptimizeEntities(pageOfEngineEntities);
      if (containsBatchOperation(newOptimizeEntities)) {
        // since we do not know which instances were suspended, restart entire running process
        // instance import
        log.info(
            "Batch suspension operation occurred. Restarting running process instance import.");
        runningProcessInstanceImportIndexHandler.resetImportIndex();
        importCompleteCallback.run();
      } else {
        final DatabaseImportJob<UserOperationLogEntryDto> databaseImportJob =
            createDatabaseImportJob(newOptimizeEntities, importCompleteCallback);
        addDatabaseImportJobToQueue(databaseImportJob);
      }
    }
  }

  @Override
  public DatabaseImportJobExecutor getDatabaseImportJobExecutor() {
    return databaseImportJobExecutor;
  }

  private DatabaseImportJob<UserOperationLogEntryDto> createDatabaseImportJob(
      final List<UserOperationLogEntryDto> userOperationLogs, Runnable callback) {
    final UserOperationLogDatabaseImportJob importJob =
        new UserOperationLogDatabaseImportJob(
            runningProcessInstanceWriter, callback, databaseClient);
    importJob.setEntitiesToImport(userOperationLogs);
    return importJob;
  }

  private void addDatabaseImportJobToQueue(final DatabaseImportJob databaseImportJob) {
    databaseImportJobExecutor.executeImportJob(databaseImportJob);
  }

  private List<UserOperationLogEntryDto> filterSuspensionOperationsAndMapToOptimizeEntities(
      final List<HistoricUserOperationLogDto> engineEntities) {
    return engineEntities.stream()
        .filter(
            historicUserOpLog ->
                !UserOperationType.fromHistoricUserOperationLog(historicUserOpLog)
                    .equals(NOT_SUSPENSION_RELATED_OPERATION))
        .map(this::mapEngineEntityToOptimizeEntity)
        .filter(this::filterUnknownDefinitionOperations)
        .distinct()
        .collect(Collectors.toList());
  }

  private UserOperationLogEntryDto mapEngineEntityToOptimizeEntity(
      final HistoricUserOperationLogDto engineEntity) {
    if (engineEntity.getProcessDefinitionKey() == null) {
      // To update instance data, we need to know the definition key (for the index name).
      // Depending on the user operation, the key may not be present in the userOpLog so we need to
      // retrieve it
      // before importing.
      enrichOperationLogDtoWithDefinitionKey(engineEntity);
    }
    return UserOperationLogEntryDto.builder()
        .id(engineEntity.getId())
        .processInstanceId(engineEntity.getProcessInstanceId())
        .processDefinitionId(engineEntity.getProcessDefinitionId())
        .processDefinitionKey(engineEntity.getProcessDefinitionKey())
        .operationType(UserOperationType.fromHistoricUserOperationLog(engineEntity))
        .build();
  }

  private boolean containsBatchOperation(List<UserOperationLogEntryDto> userOperationLogEntryDtos) {
    return userOperationLogEntryDtos.stream()
        .anyMatch(userOpLog -> isSuspensionViaBatchOperation(userOpLog.getOperationType()));
  }

  private void enrichOperationLogDtoWithDefinitionKey(
      final HistoricUserOperationLogDto engineEntity) {
    Optional<String> definitionKey = Optional.empty();
    if (engineEntity.getProcessDefinitionId() != null) {
      definitionKey =
          processDefinitionResolverService
              .getDefinition(
                  engineEntity.getProcessDefinitionId(),
                  runningProcessInstanceImportIndexHandler.getEngineContext())
              .map(ProcessDefinitionOptimizeDto::getKey);
    } else if (engineEntity.getProcessInstanceId() != null) {
      definitionKey =
          processInstanceResolverService.getProcessInstanceDefinitionKey(
              engineEntity.getProcessInstanceId(),
              runningProcessInstanceImportIndexHandler.getEngineContext());
    }
    definitionKey.ifPresent(engineEntity::setProcessDefinitionKey);
  }

  private boolean filterUnknownDefinitionOperations(
      final UserOperationLogEntryDto userOpLogEntryDto) {
    // If the operation is not a batch operation we need to know the definition key to update the
    // specific instance
    // index. If the defKey is not present, the relevant definition or instance has not yet been
    // imported, so we do
    // not import the suspension operation. Note that this might cause a race conditions in rare
    // edge cases.
    return isSuspensionViaBatchOperation(userOpLogEntryDto.getOperationType())
        || userOpLogEntryDto.getProcessDefinitionKey() != null;
  }
}
