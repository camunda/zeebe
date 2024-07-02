/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db;

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */

import io.camunda.optimize.dto.optimize.query.event.process.EventProcessPublishStateDto;
import io.camunda.optimize.service.db.reader.EventProcessPublishStateReader;
import io.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import io.camunda.optimize.service.util.configuration.ConfigurationReloadable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;

@AllArgsConstructor
public abstract class EventProcessInstanceIndexManager implements ConfigurationReloadable {

  protected final EventProcessPublishStateReader eventProcessPublishStateReader;
  protected final OptimizeIndexNameService indexNameService;

  protected final Map<String, EventProcessPublishStateDto> publishedInstanceIndices =
      new HashMap<>();
  protected final Map<String, AtomicInteger> usageCountPerIndex = new HashMap<>();

  public synchronized Map<String, EventProcessPublishStateDto> getPublishedInstanceStatesMap() {
    return publishedInstanceIndices;
  }

  public synchronized Collection<EventProcessPublishStateDto> getPublishedInstanceStates() {
    return publishedInstanceIndices.values();
  }

  public abstract void syncAvailableIndices();

  public synchronized CompletableFuture<Void> registerIndexUsageAndReturnFinishedHandler(
      final String eventProcessPublishStateId) {
    final AtomicInteger indexUsageCounter =
        usageCountPerIndex.compute(
            eventProcessPublishStateId,
            (id, usageCounter) -> {
              if (usageCounter != null) {
                usageCounter.incrementAndGet();
                return usageCounter;
              } else {
                return new AtomicInteger(1);
              }
            });
    final CompletableFuture<Void> importCompleted = new CompletableFuture<>();
    importCompleted.whenComplete((aVoid, throwable) -> indexUsageCounter.decrementAndGet());
    return importCompleted;
  }

  @Override
  public synchronized void reloadConfiguration(final ApplicationContext context) {
    publishedInstanceIndices.clear();
    usageCountPerIndex.clear();
  }
}
