/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.service.definition;

import io.camunda.optimize.dto.optimize.DefinitionOptimizeResponseDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.exceptions.OptimizeDecisionDefinitionFetchException;
import io.camunda.optimize.service.exceptions.OptimizeProcessDefinitionFetchException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public abstract class AbstractDefinitionResolverService<T extends DefinitionOptimizeResponseDto> {

  // map contains not xml
  private final Map<String, T> idToDefinitionMap = new ConcurrentHashMap<>();

  public Optional<T> getDefinition(final String definitionId, final EngineContext engineContext) {
    // #1 read value from internal cache
    T value = idToDefinitionMap.get(definitionId);

    // #2 on miss sync the cache and try again
    if (value == null) {
      log.debug("No definition for definitionId {} in cache, syncing definitions", definitionId);

      syncCache();
      value = idToDefinitionMap.get(definitionId);
    }

    // #3 on miss fetch directly from the engine
    if (value == null && engineContext != null) {
      log.info(
          "Definition with id [{}] hasn't been imported yet. "
              + "Trying to directly fetch the definition from the engine.",
          definitionId);
      try {
        value = fetchFromEngine(definitionId, engineContext);
      } catch (OptimizeDecisionDefinitionFetchException
          | OptimizeProcessDefinitionFetchException e) {
        log.info("Could not retrieve definition with ID {} from the engine.", definitionId);
        return Optional.empty();
      }
      addToCacheIfNotNull(value);
    }

    return Optional.ofNullable(value);
  }

  protected abstract T fetchFromEngine(
      final String definitionId, final EngineContext engineContext);

  protected abstract void syncCache();

  protected void addToCacheIfNotNull(final T newEntry) {
    Optional.ofNullable(newEntry)
        .ifPresent(definition -> idToDefinitionMap.put(definition.getId(), definition));
  }
}
