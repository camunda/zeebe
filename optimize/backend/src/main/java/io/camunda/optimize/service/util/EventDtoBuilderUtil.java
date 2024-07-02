/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util;

import static io.camunda.optimize.service.events.CamundaEventService.EVENT_SOURCE_CAMUNDA;

import io.camunda.optimize.dto.optimize.query.event.process.EventTypeDto;
import io.camunda.optimize.dto.optimize.query.event.sequence.EventCountResponseDto;
import io.camunda.optimize.dto.optimize.query.event.sequence.TracedEventDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventDtoBuilderUtil {

  private static final String START_MAPPED_SUFFIX = "start";
  private static final String END_MAPPED_SUFFIX = "end";
  public static final String PROCESS_START_TYPE = "processInstanceStart";
  public static final String PROCESS_END_TYPE = "processInstanceEnd";

  public static EventTypeDto createCamundaEventTypeDto(
      final String processId, final String eventName, final String eventLabel) {
    return EventTypeDto.builder()
        .source(EVENT_SOURCE_CAMUNDA)
        .group(processId)
        .eventName(eventName)
        .eventLabel(eventLabel)
        .build();
  }

  public static EventTypeDto createCamundaProcessStartEventTypeDto(final String definitionKey) {
    return EventTypeDto.builder()
        .source(EVENT_SOURCE_CAMUNDA)
        .group(definitionKey)
        .eventName(applyCamundaProcessInstanceStartEventSuffix(definitionKey))
        .eventLabel(PROCESS_START_TYPE)
        .build();
  }

  public static EventTypeDto createCamundaProcessEndEventTypeDto(final String definitionKey) {
    return EventTypeDto.builder()
        .source(EVENT_SOURCE_CAMUNDA)
        .group(definitionKey)
        .eventName(applyCamundaProcessInstanceEndEventSuffix(definitionKey))
        .eventLabel(PROCESS_END_TYPE)
        .build();
  }

  public static EventTypeDto fromTracedEventDto(final TracedEventDto tracedEventDto) {
    return EventTypeDto.builder()
        .source(tracedEventDto.getSource())
        .group(tracedEventDto.getGroup())
        .eventName(tracedEventDto.getEventName())
        .build();
  }

  public static EventTypeDto fromEventCountDto(final EventCountResponseDto eventCountDto) {
    return EventTypeDto.builder()
        .source(eventCountDto.getSource())
        .group(eventCountDto.getGroup())
        .eventName(eventCountDto.getEventName())
        .build();
  }

  public static String applyCamundaProcessInstanceStartEventSuffix(final String identifier) {
    return addDelimiterForStrings(identifier, PROCESS_START_TYPE);
  }

  public static String applyCamundaProcessInstanceEndEventSuffix(final String identifier) {
    return addDelimiterForStrings(identifier, PROCESS_END_TYPE);
  }

  public static String applyCamundaTaskStartEventSuffix(final String identifier) {
    return addDelimiterForStrings(identifier, START_MAPPED_SUFFIX);
  }

  public static String applyCamundaTaskEndEventSuffix(final String identifier) {
    return addDelimiterForStrings(identifier, END_MAPPED_SUFFIX);
  }

  private static String addDelimiterForStrings(final String... strings) {
    return String.join("_", strings);
  }
}
