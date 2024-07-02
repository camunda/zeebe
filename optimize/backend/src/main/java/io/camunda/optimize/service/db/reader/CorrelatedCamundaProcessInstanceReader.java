/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.reader;

import io.camunda.optimize.dto.optimize.query.event.autogeneration.CorrelatableProcessInstanceDto;
import io.camunda.optimize.dto.optimize.query.event.process.source.CamundaEventSourceEntryDto;
import io.camunda.optimize.dto.optimize.query.event.process.source.EventSourceType;
import io.camunda.optimize.service.db.repository.EventRepository;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class CorrelatedCamundaProcessInstanceReader {
  private final EventRepository eventRepository;

  public List<String> getCorrelationValueSampleForEventSources(
      final List<CamundaEventSourceEntryDto> eventSources) {
    final List<CamundaEventSourceEntryDto> camundaSources =
        eventSources.stream()
            .filter(eventSource -> EventSourceType.CAMUNDA.equals(eventSource.getSourceType()))
            .collect(Collectors.toList());
    if (camundaSources.isEmpty()) {
      log.debug("There are no Camunda sources to fetch sample correlation values for");
      return Collections.emptyList();
    }

    log.debug("Fetching sample of correlation values for {} event sources", camundaSources.size());

    return eventRepository.getCorrelationValueSampleForEventSources(camundaSources);
  }

  public List<CorrelatableProcessInstanceDto> getCorrelatableInstancesForSources(
      final List<CamundaEventSourceEntryDto> camundaSources, final List<String> correlationValues) {
    if (camundaSources.isEmpty()) {
      log.debug("There are no Camunda sources to fetch correlated process instances for");
      return Collections.emptyList();
    }

    log.debug(
        "Fetching correlated process instances for correlation value sample size {} for {} event sources",
        correlationValues.size(),
        camundaSources.size());

    return eventRepository.getCorrelatableInstancesForSources(camundaSources, correlationValues);
  }
}
