/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.eventprocess.service;

import io.camunda.optimize.dto.optimize.query.event.process.EventImportSourceDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessState;
import io.camunda.optimize.service.db.EventProcessInstanceIndexManager;
import io.camunda.optimize.service.db.writer.EventProcessPublishStateWriter;
import java.math.RoundingMode;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Slf4j
@Component
public class PublishStateUpdateService {

  private final EventProcessPublishStateWriter eventProcessPublishStateWriter;
  private final EventProcessInstanceIndexManager eventProcessInstanceIndexManager;

  public void updateEventProcessPublishStates() {
    eventProcessInstanceIndexManager.getPublishedInstanceStates().stream()
        .peek(
            publishState -> {
              // for each publishing process we also update progress and eventually state
              if (EventProcessState.PUBLISH_PENDING.equals(publishState.getState())) {
                final double publishProgress =
                    publishState.getEventImportSources().stream()
                        .mapToDouble(this::getProgressForImportSource)
                        .average()
                        .orElse(0.0D);
                final double roundedPublishProgress =
                    Precision.round(
                        Math.min(publishProgress, 100.0D), 1, RoundingMode.DOWN.ordinal());
                publishState.setPublishProgress(roundedPublishProgress);

                if (publishState.getPublishProgress() == 100.0D) {
                  publishState.setState(EventProcessState.PUBLISHED);
                }
              }
            })
        .forEach(eventProcessPublishStateWriter::updateEventProcessPublishState);
  }

  private Double getProgressForImportSource(EventImportSourceDto eventImportSourceDto) {
    if (eventImportSourceDto.getLastImportedEventTimestamp().toInstant().toEpochMilli() == 0) {
      return 0.0D;
    }

    final double durationBetweenFirstAndLastEventAtTimeOfPublish =
        betweenFirstAndLastEventToImport(eventImportSourceDto);
    final double durationBetweenFirstAndLastImportedEvent =
        betweenFirstAndLastCurrentlyImportedEvent(eventImportSourceDto);

    if (durationBetweenFirstAndLastEventAtTimeOfPublish == 0.0D) {
      return 100.0D;
    } else {
      return Math.min(
          durationBetweenFirstAndLastImportedEvent
              / durationBetweenFirstAndLastEventAtTimeOfPublish
              * 100.0D,
          100.0D);
    }
  }

  private double betweenFirstAndLastCurrentlyImportedEvent(
      final EventImportSourceDto eventImportSourceDto) {
    return Duration.between(
            eventImportSourceDto.getFirstEventForSourceAtTimeOfPublishTimestamp().toInstant(),
            eventImportSourceDto.getLastImportedEventTimestamp().toInstant())
        .toMillis();
  }

  private double betweenFirstAndLastEventToImport(final EventImportSourceDto eventImportSourceDto) {
    return Duration.between(
            eventImportSourceDto.getFirstEventForSourceAtTimeOfPublishTimestamp().toInstant(),
            eventImportSourceDto.getLastEventForSourceAtTimeOfPublishTimestamp().toInstant())
        .toMillis();
  }
}
