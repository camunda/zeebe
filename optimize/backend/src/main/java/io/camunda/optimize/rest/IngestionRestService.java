/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableRequestDto.toExternalProcessVariableDtos;
import static io.camunda.optimize.rest.IngestionRestService.INGESTION_PATH;
import static java.util.stream.Collectors.toList;

import io.camunda.optimize.dto.optimize.ReportConstants;
import io.camunda.optimize.dto.optimize.query.event.process.EventDto;
import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableRequestDto;
import io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto;
import io.camunda.optimize.service.events.ExternalEventService;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.service.util.VariableHelper;
import io.camunda.optimize.service.variable.ExternalVariableService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@AllArgsConstructor
@Slf4j
@Path(INGESTION_PATH)
@Component
public class IngestionRestService {
  public static final String INGESTION_PATH = "/ingestion";
  public static final String EVENT_BATCH_SUB_PATH = "/event/batch";
  public static final String VARIABLE_SUB_PATH = "/variable";
  public static final String CONTENT_TYPE_CLOUD_EVENTS_V1_JSON_BATCH =
      "application/cloudevents-batch+json";

  private final ExternalEventService externalEventService;
  private final ExternalVariableService externalVariableService;

  @POST
  @Path(EVENT_BATCH_SUB_PATH)
  @Consumes({CONTENT_TYPE_CLOUD_EVENTS_V1_JSON_BATCH, MediaType.APPLICATION_JSON})
  @Produces(MediaType.APPLICATION_JSON)
  public void ingestCloudEvents(
      final @Context ContainerRequestContext requestContext,
      final @NotNull @Valid @RequestBody ValidList<CloudEventRequestDto> cloudEventDtos) {
    externalEventService.saveEventBatch(mapToEventDto(cloudEventDtos));
  }

  @POST
  @Path(VARIABLE_SUB_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  public void ingestVariables(
      final @Context ContainerRequestContext requestContext,
      final @NotNull @Valid @RequestBody List<ExternalProcessVariableRequestDto> variableDtos) {
    validateVariableType(variableDtos);
    externalVariableService.storeExternalProcessVariables(
        toExternalProcessVariableDtos(
            LocalDateUtil.getCurrentDateTime().toInstant().toEpochMilli(), variableDtos));
  }

  private void validateVariableType(final List<ExternalProcessVariableRequestDto> variables) {
    if (variables.stream()
        .anyMatch(variable -> !VariableHelper.isProcessVariableTypeSupported(variable.getType()))) {
      throw new BadRequestException(
          String.format(
              "A given variable type is not supported. The type must always be one of: %s",
              ReportConstants.ALL_SUPPORTED_PROCESS_VARIABLE_TYPES));
    }
  }

  private static List<EventDto> mapToEventDto(final List<CloudEventRequestDto> cloudEventDtos) {
    Instant rightNow = LocalDateUtil.getCurrentDateTime().toInstant();
    return cloudEventDtos.stream()
        .map(
            cloudEventDto ->
                EventDto.builder()
                    .id(cloudEventDto.getId())
                    .eventName(cloudEventDto.getType())
                    .timestamp(
                        cloudEventDto
                            .getTime()
                            .orElse(rightNow) // In case no time was passed as a parameter, use the
                            // current time instead
                            .toEpochMilli())
                    .traceId(cloudEventDto.getTraceid())
                    .group(cloudEventDto.getGroup().orElse(null))
                    .source(cloudEventDto.getSource())
                    .data(cloudEventDto.getData())
                    .build())
        .collect(toList());
  }

  @Data
  private static class ValidList<E> implements List<E> {

    @Delegate private List<E> list = new ArrayList<>();
  }
}
