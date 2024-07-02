/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service.definition;

import static java.util.stream.Collectors.groupingBy;

import io.camunda.optimize.dto.engine.definition.DecisionDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.DecisionDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.datasource.EngineDataSourceDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.writer.DecisionDefinitionWriter;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.DatabaseImportJobExecutor;
import io.camunda.optimize.service.importing.engine.service.ImportService;
import io.camunda.optimize.service.importing.job.DecisionDefinitionDatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DecisionDefinitionImportService implements ImportService<DecisionDefinitionEngineDto> {

  private final DatabaseImportJobExecutor databaseImportJobExecutor;
  private final EngineContext engineContext;
  private final DecisionDefinitionWriter decisionDefinitionWriter;
  private final DecisionDefinitionResolverService decisionDefinitionResolverService;
  private final DatabaseClient databaseClient;

  public DecisionDefinitionImportService(
      final ConfigurationService configurationService,
      final EngineContext engineContext,
      final DecisionDefinitionWriter decisionDefinitionWriter,
      final DecisionDefinitionResolverService decisionDefinitionResolverService,
      final DatabaseClient databaseClient) {
    this.databaseImportJobExecutor =
        new DatabaseImportJobExecutor(getClass().getSimpleName(), configurationService);
    this.engineContext = engineContext;
    this.decisionDefinitionWriter = decisionDefinitionWriter;
    this.decisionDefinitionResolverService = decisionDefinitionResolverService;
    this.databaseClient = databaseClient;
  }

  @Override
  public void executeImport(
      final List<DecisionDefinitionEngineDto> pageOfEngineEntities,
      final Runnable importCompleteCallback) {
    log.trace("Importing decision definitions from engine...");
    final boolean newDataIsAvailable = !pageOfEngineEntities.isEmpty();
    if (newDataIsAvailable) {
      final List<DecisionDefinitionOptimizeDto> optimizeDtos =
          mapEngineEntitiesToOptimizeEntities(pageOfEngineEntities);
      markSavedDefinitionsAsDeleted(optimizeDtos);
      final DatabaseImportJob<DecisionDefinitionOptimizeDto> databaseImportJob =
          createDatabaseImportJob(optimizeDtos, importCompleteCallback);
      databaseImportJobExecutor.executeImportJob(databaseImportJob);
    }
  }

  private void markSavedDefinitionsAsDeleted(
      final List<DecisionDefinitionOptimizeDto> definitionsToImport) {
    final boolean definitionsDeleted =
        decisionDefinitionWriter.markRedeployedDefinitionsAsDeleted(definitionsToImport);
    // We only resync the cache if at least one existing definition has been marked as deleted
    if (definitionsDeleted) {
      decisionDefinitionResolverService.syncCache();
    }
  }

  @Override
  public DatabaseImportJobExecutor getDatabaseImportJobExecutor() {
    return databaseImportJobExecutor;
  }

  private List<DecisionDefinitionOptimizeDto> mapEngineEntitiesToOptimizeEntities(
      final List<DecisionDefinitionEngineDto> engineDtos) {
    // we mark new definitions as deleted if they are imported in the same batch as a newer
    // deployment
    final Map<String, List<DecisionDefinitionEngineDto>> groupedDefinitions =
        engineDtos.stream()
            .collect(
                groupingBy(
                    definition ->
                        definition.getKey() + definition.getTenantId() + definition.getVersion()));
    groupedDefinitions.entrySet().stream()
        .filter(entry -> entry.getValue().size() > 1)
        .forEach(
            entry -> {
              final DecisionDefinitionEngineDto newestDefinition =
                  entry.getValue().stream()
                      .max(Comparator.comparing(DecisionDefinitionEngineDto::getDeploymentTime))
                      .get();
              entry.getValue().stream()
                  .filter(definition -> !definition.equals(newestDefinition))
                  .forEach(deletedDef -> deletedDef.setDeleted(true));
            });
    return engineDtos.stream()
        .map(this::mapEngineEntityToOptimizeEntity)
        .collect(Collectors.toList());
  }

  private DatabaseImportJob<DecisionDefinitionOptimizeDto> createDatabaseImportJob(
      final List<DecisionDefinitionOptimizeDto> optimizeDtos,
      final Runnable importCompleteCallback) {
    final DecisionDefinitionDatabaseImportJob importJob =
        new DecisionDefinitionDatabaseImportJob(
            decisionDefinitionWriter, importCompleteCallback, databaseClient);
    importJob.setEntitiesToImport(optimizeDtos);
    return importJob;
  }

  private DecisionDefinitionOptimizeDto mapEngineEntityToOptimizeEntity(
      final DecisionDefinitionEngineDto engineDto) {
    return DecisionDefinitionOptimizeDto.builder()
        .id(engineDto.getId())
        .key(engineDto.getKey())
        .version(String.valueOf(engineDto.getVersion()))
        .versionTag(engineDto.getVersionTag())
        .name(engineDto.getName())
        .dataSource(new EngineDataSourceDto(engineContext.getEngineAlias()))
        .tenantId(
            engineDto
                .getTenantId()
                .orElseGet(() -> engineContext.getDefaultTenantId().orElse(null)))
        .deleted(engineDto.isDeleted())
        .build();
  }
}
