/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.eventprocess.handler;

import io.camunda.optimize.dto.optimize.query.event.process.EventImportSourceDto;
import io.camunda.optimize.service.db.DatabaseConstants;
import io.camunda.optimize.service.importing.TimestampBasedImportIndexHandler;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EventProcessInstanceImportSourceIndexHandler
    extends TimestampBasedImportIndexHandler<EventImportSourceDto> {

  private final EventImportSourceDto eventImportSourceDto;

  public EventProcessInstanceImportSourceIndexHandler(
      final ConfigurationService configurationService,
      final EventImportSourceDto eventImportSourceDto) {
    this.configurationService = configurationService;
    this.eventImportSourceDto = eventImportSourceDto;
    updatePendingLastEntityTimestamp(eventImportSourceDto.getLastImportedEventTimestamp());
  }

  @Override
  public EventImportSourceDto getIndexStateDto() {
    return eventImportSourceDto;
  }

  @Override
  public String getEngineAlias() {
    return DatabaseConstants.ENGINE_ALIAS_OPTIMIZE;
  }

  @Override
  public void updateTimestampOfLastEntity(final OffsetDateTime timestamp) {
    // we store the plain real last entity timestamps for event processes, so progress can be
    // calculated correctly
    updateLastPersistedEntityTimestamp(timestamp);
  }

  @Override
  public void updatePendingTimestampOfLastEntity(final OffsetDateTime timestamp) {
    // we store the plain real last entity timestamps for event processes, so progress can be
    // calculated correctly
    updatePendingLastEntityTimestamp(timestamp);
  }

  @Override
  protected void updateLastPersistedEntityTimestamp(final OffsetDateTime timestamp) {
    eventImportSourceDto.setLastImportedEventTimestamp(timestamp);
  }

  @Override
  protected void updateLastImportExecutionTimestamp(final OffsetDateTime timestamp) {
    eventImportSourceDto.setLastImportExecutionTimestamp(timestamp);
  }

  @Override
  public OffsetDateTime getTimestampOfLastEntity() {
    // current tip of time backoff is applied on read based on the relation to the last execution
    final OffsetDateTime lastImportedEventTimestamp = timestampOfLastEntity;
    final OffsetDateTime backOffWindowStart =
        reduceByCurrentTimeBackoff(
            Optional.ofNullable(eventImportSourceDto.getLastImportExecutionTimestamp())
                .orElse(LocalDateUtil.getCurrentDateTime()));
    if (lastImportedEventTimestamp.isAfter(backOffWindowStart)) {
      logger.info(
          "Timestamp is in the current time backoff window of {}ms, will return begin of backoff window as last "
              + "timestamp",
          getTipOfTimeBackoffMilliseconds());
      return reduceByCurrentTimeBackoff(lastImportedEventTimestamp);
    } else {
      return lastImportedEventTimestamp;
    }
  }
}
