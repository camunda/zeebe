/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing;

import static io.camunda.optimize.service.db.DatabaseConstants.ENGINE_ALIAS_OPTIMIZE;

import io.camunda.optimize.dto.engine.TenantSpecificEngineDto;
import io.camunda.optimize.service.exceptions.OptimizeConfigurationException;
import io.camunda.optimize.service.importing.engine.service.ImportService;
import io.camunda.optimize.service.util.BackoffCalculator;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BackoffImportMediator<T extends EngineImportIndexHandler<?, ?>, DTO>
    implements ImportMediator {

  private final BackoffCalculator errorBackoffCalculator = new BackoffCalculator(10, 1000);
  protected Logger logger = LoggerFactory.getLogger(getClass());
  protected ConfigurationService configurationService;
  protected BackoffCalculator idleBackoffCalculator;
  protected T importIndexHandler;
  protected ImportService<DTO> importService;
  protected List<String> excludedTenantIds;

  protected BackoffImportMediator(
      final ConfigurationService configurationService,
      final BackoffCalculator idleBackoffCalculator,
      final T importIndexHandler,
      final ImportService<DTO> importService) {
    this.configurationService = configurationService;
    this.idleBackoffCalculator = idleBackoffCalculator;
    this.importIndexHandler = importIndexHandler;
    this.importService = importService;
    excludedTenantIds = readExcludedIdsFromConfig();
  }

  @Override
  public CompletableFuture<Void> runImport() {
    final CompletableFuture<Void> importCompleted = new CompletableFuture<>();
    final boolean pageIsPresent = importNextPageRetryOnError(importCompleted);
    if (pageIsPresent) {
      idleBackoffCalculator.resetBackoff();
    } else {
      calculateNewDateUntilIsBlocked();
    }
    return importCompleted;
  }

  /**
   * Method is invoked by scheduler once no more jobs are created by factories associated with
   * import process from specific engine
   *
   * @return time to sleep for import process of an engine in general
   */
  @Override
  public long getBackoffTimeInMs() {
    return idleBackoffCalculator.getTimeUntilNextRetry();
  }

  @Override
  public void resetBackoff() {
    idleBackoffCalculator.resetBackoff();
  }

  @Override
  public boolean canImport() {
    final boolean canImportNewPage = idleBackoffCalculator.isReadyForNextRetry();
    logger.debug("can import next page [{}]", canImportNewPage);
    return canImportNewPage;
  }

  public T getImportIndexHandler() {
    return importIndexHandler;
  }

  @Override
  public boolean hasPendingImportJobs() {
    return importService.hasPendingImportJobs();
  }

  @Override
  public void shutdown() {
    importService.shutdown();
  }

  protected abstract boolean importNextPage(Runnable importCompleteCallback);

  private boolean importNextPageRetryOnError(final CompletableFuture<Void> importCompleteCallback) {
    Boolean result = null;
    try {
      while (result == null) {
        try {
          result = importNextPage(() -> importCompleteCallback.complete(null));
        } catch (final IllegalStateException e) {
          throw e;
        } catch (final Exception e) {
          if (errorBackoffCalculator.isMaximumBackoffReached()) {
            // if max back-off is reached abort retrying and return true to indicate there is new
            // data
            logger.error(
                "Was not able to import next page and reached max backoff, aborting this run.", e);
            importCompleteCallback.complete(null);
            result = true;
          } else {
            final long timeToSleep = errorBackoffCalculator.calculateSleepTime();
            logger.error(
                "Was not able to import next page, retrying after sleeping for {}ms.",
                timeToSleep,
                e);
            Thread.sleep(timeToSleep);
          }
        }
      }
    } catch (final InterruptedException e) {
      logger.warn("Was interrupted while importing next page.", e);
      Thread.currentThread().interrupt();
      return false;
    }
    errorBackoffCalculator.resetBackoff();

    return result;
  }

  private void calculateNewDateUntilIsBlocked() {
    if (idleBackoffCalculator.isMaximumBackoffReached()) {
      logger.debug(
          "Maximum idle backoff reached, this mediator will not backoff any further than {}ms.",
          idleBackoffCalculator.getMaximumBackoffMilliseconds());
    }
    final long sleepTime = idleBackoffCalculator.calculateSleepTime();
    logger.debug("Was not able to produce a new job, sleeping for [{}] ms", sleepTime);
  }

  private List<String> readExcludedIdsFromConfig() {
    final List<String> excludedIdsFromConfig =
        // This check for configurationService being null is necessary, otherwise migration tests
        // fail
        Optional.ofNullable(configurationService)
            .map(
                config -> {
                  final List<String> idsToExclude = new ArrayList<>();
                  // If the internal Optimize alias is used, then the mediator is an internal
                  // mechanism and no tenant exclusion should
                  // be applied
                  if (!ENGINE_ALIAS_OPTIMIZE.equals(importIndexHandler.getEngineAlias())) {
                    try {
                      idsToExclude.addAll(
                          config.getExcludedTenants(importIndexHandler.getEngineAlias()));
                    } catch (final OptimizeConfigurationException e) {
                      // This happens when the Engine configured in the importIndexHandler doesn't
                      // exist in the config. That's
                      // not a problem, then we just simply have no excluded Tenants and don't do
                      // any filtering. This is only
                      // expected to happen in unit tests where the EngineConfiguration map is not
                      // mocked
                      logger.info(
                          String.format(
                              "Engine '%s' could not be found in the configuration",
                              importIndexHandler.getEngineAlias()));
                    }
                  }
                  return idsToExclude;
                })
            .orElse(new ArrayList<>());
    excludedTenantIds = excludedIdsFromConfig;
    return excludedIdsFromConfig;
  }

  protected List<DTO> filterEntitiesFromExcludedTenants(final List<DTO> allEntities) {
    if (excludedTenantIds.isEmpty()) {
      return allEntities;
    } else {
      logger.debug(
          "Import filter by tenant is active - Excluding data from these tenants: {}",
          excludedTenantIds);
      final Predicate<DTO> isTenantSpecific = TenantSpecificEngineDto.class::isInstance;
      // We only exclude entities that are tenant specific AND the tenant ID is in the exclusion
      // list
      return allEntities.stream()
          .filter(
              isTenantSpecific
                  .negate()
                  .or(
                      isTenantSpecific.and(
                          entity ->
                              !excludedTenantIds.contains(
                                  ((TenantSpecificEngineDto) entity).getTenantId().orElse("")))))
          .collect(Collectors.toList());
    }
  }
}
