/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.test.optimize;

import com.google.common.collect.ImmutableMap;
import io.camunda.optimize.OptimizeRequestExecutor;
import io.camunda.optimize.dto.optimize.query.variable.ExternalProcessVariableRequestDto;
import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto;
import io.camunda.optimize.service.util.IdGenerator;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

@AllArgsConstructor
public class IngestionClient {
  private static final Random RANDOM = new Random();

  private final Supplier<OptimizeRequestExecutor> requestExecutorSupplier;
  private final Supplier<String> accessTokenSupplier;

  public Response ingestVariablesAndReturnResponse(
      final List<ExternalProcessVariableRequestDto> variables) {
    return ingestVariablesAndReturnResponse(variables, getVariableIngestionToken());
  }

  public Response ingestVariablesAndReturnResponse(
      final List<ExternalProcessVariableRequestDto> variables, final String accessToken) {
    return requestExecutorSupplier
        .get()
        .buildIngestExternalVariables(variables, accessToken)
        .execute();
  }

  public void ingestVariables(final List<ExternalProcessVariableRequestDto> variables) {
    requestExecutorSupplier
        .get()
        .buildIngestExternalVariables(variables, getVariableIngestionToken())
        .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void ingestEventBatch(final List<CloudEventRequestDto> eventDtos) {
    requestExecutorSupplier
        .get()
        .buildIngestEventBatch(eventDtos, accessTokenSupplier.get())
        .execute();
  }

  public List<CloudEventRequestDto> ingestEventBatchWithTimestamp(
      final Instant timestamp, final int eventCount) {
    final List<CloudEventRequestDto> ingestedEvents =
        IntStream.range(0, eventCount)
            .mapToObj(operand -> createCloudEventDto().toBuilder().time(timestamp).build())
            .toList();
    ingestEventBatch(ingestedEvents);
    return ingestedEvents;
  }

  public ExternalProcessVariableRequestDto createPrimitiveExternalVariable() {
    return new ExternalProcessVariableRequestDto()
        .setId("anId")
        .setName("aName")
        .setValue("aValue")
        .setType(VariableType.STRING)
        .setProcessInstanceId("anInstanceId")
        .setProcessDefinitionKey("aDefinitionKey");
  }

  public ExternalProcessVariableRequestDto createObjectExternalVariable(final String value) {
    return new ExternalProcessVariableRequestDto()
        .setId("anId")
        .setName("objectVarName")
        .setValue(value)
        .setType(VariableType.OBJECT)
        .setProcessInstanceId("anInstanceId")
        .setProcessDefinitionKey("aDefinitionKey")
        .setSerializationDataFormat(MediaType.APPLICATION_JSON);
  }

  public CloudEventRequestDto createCloudEventDto() {
    return CloudEventRequestDto.builder()
        .id(IdGenerator.getNextId())
        .source(RandomStringUtils.randomAlphabetic(10))
        .specversion("1.0")
        .type(RandomStringUtils.randomAlphabetic(10))
        .time(Instant.now())
        .group(RandomStringUtils.randomAlphabetic(10))
        .data(
            ImmutableMap.of(
                RandomStringUtils.randomAlphabetic(5), RANDOM.nextInt(),
                RandomStringUtils.randomAlphabetic(5), RANDOM.nextBoolean(),
                RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
        .traceid(RandomStringUtils.randomAlphabetic(10))
        .build();
  }

  public String getVariableIngestionToken() {
    return accessTokenSupplier.get();
  }
}
