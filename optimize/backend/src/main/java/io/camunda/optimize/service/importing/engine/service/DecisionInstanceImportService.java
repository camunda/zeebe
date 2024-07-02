/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service;

import static io.camunda.optimize.service.util.VariableHelper.isDecisionVariableTypeSupported;

import io.camunda.optimize.dto.engine.HistoricDecisionInputInstanceDto;
import io.camunda.optimize.dto.engine.HistoricDecisionInstanceDto;
import io.camunda.optimize.dto.engine.HistoricDecisionOutputInstanceDto;
import io.camunda.optimize.dto.optimize.DecisionDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.importing.DecisionInstanceDto;
import io.camunda.optimize.dto.optimize.importing.InputInstanceDto;
import io.camunda.optimize.dto.optimize.importing.OutputInstanceDto;
import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import io.camunda.optimize.plugin.DecisionInputImportAdapterProvider;
import io.camunda.optimize.plugin.DecisionOutputImportAdapterProvider;
import io.camunda.optimize.plugin.importing.variable.DecisionInputImportAdapter;
import io.camunda.optimize.plugin.importing.variable.DecisionOutputImportAdapter;
import io.camunda.optimize.plugin.importing.variable.PluginDecisionInputDto;
import io.camunda.optimize.plugin.importing.variable.PluginDecisionOutputDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.writer.DecisionInstanceWriter;
import io.camunda.optimize.service.exceptions.OptimizeDecisionDefinitionNotFoundException;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.DatabaseImportJobExecutor;
import io.camunda.optimize.service.importing.engine.service.definition.DecisionDefinitionResolverService;
import io.camunda.optimize.service.importing.job.DecisionInstanceDatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DecisionInstanceImportService implements ImportService<HistoricDecisionInstanceDto> {

  private final DatabaseImportJobExecutor databaseImportJobExecutor;
  private final EngineContext engineContext;
  private final DecisionInstanceWriter decisionInstanceWriter;
  private final DecisionDefinitionResolverService decisionDefinitionResolverService;
  private final DecisionInputImportAdapterProvider decisionInputImportAdapterProvider;
  private final DecisionOutputImportAdapterProvider decisionOutputImportAdapterProvider;
  private final DatabaseClient databaseClient;

  public DecisionInstanceImportService(
      final ConfigurationService configurationService,
      final EngineContext engineContext,
      final DecisionInstanceWriter decisionInstanceWriter,
      final DecisionDefinitionResolverService decisionDefinitionResolverService,
      final DecisionInputImportAdapterProvider decisionInputImportAdapterProvider,
      final DecisionOutputImportAdapterProvider decisionOutputImportAdapterProvider,
      final DatabaseClient databaseClient) {
    this.databaseImportJobExecutor =
        new DatabaseImportJobExecutor(getClass().getSimpleName(), configurationService);
    this.engineContext = engineContext;
    this.decisionInstanceWriter = decisionInstanceWriter;
    this.decisionDefinitionResolverService = decisionDefinitionResolverService;
    this.decisionInputImportAdapterProvider = decisionInputImportAdapterProvider;
    this.decisionOutputImportAdapterProvider = decisionOutputImportAdapterProvider;
    this.databaseClient = databaseClient;
  }

  @Override
  public void executeImport(
      List<HistoricDecisionInstanceDto> engineDtoList, Runnable importCompleteCallback) {
    log.trace("Importing entities from engine...");
    boolean newDataIsAvailable = !engineDtoList.isEmpty();

    if (newDataIsAvailable) {
      final List<DecisionInstanceDto> optimizeDtos =
          mapEngineEntitiesToOptimizeEntities(engineDtoList);

      final DatabaseImportJob<DecisionInstanceDto> databaseImportJob =
          createDatabaseImportJob(optimizeDtos, importCompleteCallback);
      addDatabaseImportJobToQueue(databaseImportJob);
    }
  }

  @Override
  public DatabaseImportJobExecutor getDatabaseImportJobExecutor() {
    return databaseImportJobExecutor;
  }

  public Optional<DecisionInstanceDto> mapEngineEntityToOptimizeEntity(
      HistoricDecisionInstanceDto engineEntity) {
    final Optional<DecisionDefinitionOptimizeDto> definition =
        resolveDecisionDefinition(engineEntity);
    if (!definition.isPresent()) {
      log.info(
          "Cannot retrieve definition for definition with ID {}. Skipping import of decision instance with ID {}",
          engineEntity.getDecisionDefinitionId(),
          engineEntity.getId());
      return Optional.empty();
    }
    final DecisionDefinitionOptimizeDto resolvedDefinition = definition.get();
    return Optional.of(
        new DecisionInstanceDto(
            engineEntity.getId(),
            engineEntity.getProcessDefinitionId(),
            engineEntity.getProcessDefinitionKey(),
            engineEntity.getDecisionDefinitionId(),
            engineEntity.getDecisionDefinitionKey(),
            resolvedDefinition.getVersion(),
            engineEntity.getEvaluationTime(),
            engineEntity.getProcessInstanceId(),
            engineEntity.getRootProcessInstanceId(),
            engineEntity.getActivityId(),
            engineEntity.getCollectResultValue(),
            engineEntity.getRootDecisionInstanceId(),
            mapDecisionInputs(engineEntity, resolvedDefinition),
            mapDecisionOutputs(engineEntity, resolvedDefinition),
            engineEntity.getOutputs().stream()
                .map(HistoricDecisionOutputInstanceDto::getRuleId)
                .collect(Collectors.toSet()),
            engineContext.getEngineAlias(),
            engineEntity
                .getTenantId()
                .orElseGet(() -> engineContext.getDefaultTenantId().orElse(null))));
  }

  private void addDatabaseImportJobToQueue(DatabaseImportJob databaseImportJob) {
    databaseImportJobExecutor.executeImportJob(databaseImportJob);
  }

  private List<DecisionInstanceDto> mapEngineEntitiesToOptimizeEntities(
      List<HistoricDecisionInstanceDto> engineEntities) {
    return engineEntities.stream()
        .map(this::mapEngineEntityToOptimizeEntity)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private DatabaseImportJob<DecisionInstanceDto> createDatabaseImportJob(
      List<DecisionInstanceDto> decisionInstanceDtos, Runnable callback) {
    final DecisionInstanceDatabaseImportJob importJob =
        new DecisionInstanceDatabaseImportJob(decisionInstanceWriter, callback, databaseClient);
    importJob.setEntitiesToImport(decisionInstanceDtos);
    return importJob;
  }

  private List<OutputInstanceDto> mapDecisionOutputs(
      HistoricDecisionInstanceDto engineEntity,
      final DecisionDefinitionOptimizeDto resolvedDefinition) {
    List<PluginDecisionOutputDto> outputInstanceDtoList =
        engineEntity.getOutputs().stream()
            .map(o -> mapEngineOutputDtoToPluginOutputDto(engineEntity, o, resolvedDefinition))
            .collect(Collectors.toList());

    for (DecisionOutputImportAdapter dmnInputImportAdapter :
        decisionOutputImportAdapterProvider.getPlugins()) {
      outputInstanceDtoList = dmnInputImportAdapter.adaptOutputs(outputInstanceDtoList);
    }

    return outputInstanceDtoList.stream()
        .filter(this::isValidOutputInstanceDto)
        .map(this::mapPluginOutputDtoToOptimizeOutputDto)
        .collect(Collectors.toList());
  }

  private List<InputInstanceDto> mapDecisionInputs(
      HistoricDecisionInstanceDto engineEntity,
      final DecisionDefinitionOptimizeDto resolvedDefinition) {
    List<PluginDecisionInputDto> inputInstanceDtoList =
        engineEntity.getInputs().stream()
            .map(i -> mapEngineInputDtoToPluginInputDto(engineEntity, i, resolvedDefinition))
            .collect(Collectors.toList());

    for (DecisionInputImportAdapter decisionInputImportAdapter :
        decisionInputImportAdapterProvider.getPlugins()) {
      inputInstanceDtoList = decisionInputImportAdapter.adaptInputs(inputInstanceDtoList);
    }

    return inputInstanceDtoList.stream()
        .filter(this::isValidInputInstanceDto)
        .map(this::mapPluginInputDtoToOptimizeInputDto)
        .collect(Collectors.toList());
  }

  private InputInstanceDto mapPluginInputDtoToOptimizeInputDto(
      PluginDecisionInputDto pluginDecisionInputDto) {
    return new InputInstanceDto(
        pluginDecisionInputDto.getId(),
        pluginDecisionInputDto.getClauseId(),
        pluginDecisionInputDto.getClauseName(),
        Optional.ofNullable(pluginDecisionInputDto.getType())
            .map(VariableType::getTypeForId)
            .orElse(null),
        pluginDecisionInputDto.getValue());
  }

  private OutputInstanceDto mapPluginOutputDtoToOptimizeOutputDto(
      PluginDecisionOutputDto pluginDecisionOutputDto) {
    return new OutputInstanceDto(
        pluginDecisionOutputDto.getId(),
        pluginDecisionOutputDto.getClauseId(),
        pluginDecisionOutputDto.getClauseName(),
        pluginDecisionOutputDto.getRuleId(),
        pluginDecisionOutputDto.getRuleOrder(),
        pluginDecisionOutputDto.getVariableName(),
        Optional.ofNullable(pluginDecisionOutputDto.getType())
            .map(VariableType::getTypeForId)
            .orElse(null),
        pluginDecisionOutputDto.getValue());
  }

  @SneakyThrows
  private PluginDecisionInputDto mapEngineInputDtoToPluginInputDto(
      final HistoricDecisionInstanceDto decisionInstanceDto,
      final HistoricDecisionInputInstanceDto engineInputDto,
      final DecisionDefinitionOptimizeDto resolvedDefinition) {
    return new PluginDecisionInputDto(
        engineInputDto.getId(),
        engineInputDto.getClauseId(),
        engineInputDto.getClauseName(),
        engineInputDto.getType(),
        Optional.ofNullable(engineInputDto.getValue()).map(String::valueOf).orElse(null),
        decisionInstanceDto.getDecisionDefinitionKey(),
        resolvedDefinition.getVersion(),
        decisionInstanceDto.getDecisionDefinitionId(),
        decisionInstanceDto.getId(),
        engineContext.getEngineAlias(),
        decisionInstanceDto
            .getTenantId()
            .orElseGet(() -> engineContext.getDefaultTenantId().orElse(null)));
  }

  @SneakyThrows
  private PluginDecisionOutputDto mapEngineOutputDtoToPluginOutputDto(
      final HistoricDecisionInstanceDto decisionInstanceDto,
      final HistoricDecisionOutputInstanceDto engineOutputDto,
      final DecisionDefinitionOptimizeDto resolvedDefinition) {
    return new PluginDecisionOutputDto(
        engineOutputDto.getId(),
        engineOutputDto.getClauseId(),
        engineOutputDto.getClauseName(),
        engineOutputDto.getRuleId(),
        engineOutputDto.getRuleOrder(),
        engineOutputDto.getVariableName(),
        engineOutputDto.getType(),
        Optional.ofNullable(engineOutputDto.getValue()).map(String::valueOf).orElse(null),
        decisionInstanceDto.getDecisionDefinitionKey(),
        resolvedDefinition.getVersion(),
        decisionInstanceDto.getDecisionDefinitionId(),
        decisionInstanceDto.getId(),
        engineContext.getEngineAlias(),
        decisionInstanceDto
            .getTenantId()
            .orElseGet(() -> engineContext.getDefaultTenantId().orElse(null)));
  }

  private boolean isValidInputInstanceDto(final PluginDecisionInputDto inputInstanceDto) {
    if (!isDecisionVariableTypeSupported(inputInstanceDto.getType())) {
      log.info(
          "Refuse to add input variable [id: {}, clauseId: {}, clauseName: {}, type: {}] "
              + "for decision instance with id [{}]. Variable has no type or type is not supported.",
          inputInstanceDto.getId(),
          inputInstanceDto.getClauseId(),
          inputInstanceDto.getClauseName(),
          inputInstanceDto.getType(),
          inputInstanceDto.getDecisionInstanceId());
      return false;
    }
    return true;
  }

  private boolean isValidOutputInstanceDto(final PluginDecisionOutputDto outputInstanceDto) {
    if (!isDecisionVariableTypeSupported(outputInstanceDto.getType())) {
      log.info(
          "Refuse to add output variable [id: {}, clauseId: {}, clauseName: {}, type: {}] "
              + "for decision instance with id [{}]. Variable has no type or type is not supported.",
          outputInstanceDto.getId(),
          outputInstanceDto.getClauseId(),
          outputInstanceDto.getClauseName(),
          outputInstanceDto.getType(),
          outputInstanceDto.getDecisionInstanceId());
      return false;
    }
    return true;
  }

  private Optional<DecisionDefinitionOptimizeDto> resolveDecisionDefinition(
      final HistoricDecisionInstanceDto engineEntity) {
    try {
      return decisionDefinitionResolverService.getDefinition(
          engineEntity.getDecisionDefinitionId(), engineContext);
    } catch (OptimizeDecisionDefinitionNotFoundException ex) {
      log.debug("Could not find the definition with ID {}", engineEntity.getDecisionDefinitionId());
      return Optional.empty();
    }
  }
}
