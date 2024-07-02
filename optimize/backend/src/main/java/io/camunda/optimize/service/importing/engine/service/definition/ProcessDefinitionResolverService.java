/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service.definition;

import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.reader.ProcessDefinitionReader;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class ProcessDefinitionResolverService
    extends AbstractDefinitionResolverService<ProcessDefinitionOptimizeDto> {

  protected final DatabaseClient databaseClient;
  private final ProcessDefinitionReader processDefinitionReader;

  @Override
  protected ProcessDefinitionOptimizeDto fetchFromEngine(
      final String definitionId, final EngineContext engineContext) {
    return engineContext.fetchProcessDefinition(definitionId);
  }

  @Override
  protected void syncCache() {
    processDefinitionReader.getAllProcessDefinitions().forEach(this::addToCacheIfNotNull);
  }

  public <T> T enrichEngineDtoWithDefinitionKey(
      final EngineContext engineContext,
      final T engineEntity,
      final Function<T, String> definitionKeyGetter,
      final Function<T, String> definitionIdGetter,
      final BiConsumer<T, String> definitionKeySetter) {
    // Under some circumstances, eg due to very old process instance data or specific
    // userOperationLogs, the
    // definitionKey may not be present. It is required to write to the correct instanceIndex, so we
    // need to retrieve
    // it if possible
    if (definitionKeyGetter.apply(engineEntity) == null) {
      Optional<String> definitionKey = Optional.empty();
      if (definitionIdGetter.apply(engineEntity) != null) {
        definitionKey =
            getDefinition(definitionIdGetter.apply(engineEntity), engineContext)
                .map(ProcessDefinitionOptimizeDto::getKey);
      }
      definitionKey.ifPresent(key -> definitionKeySetter.accept(engineEntity, key));
    }
    return engineEntity;
  }
}
