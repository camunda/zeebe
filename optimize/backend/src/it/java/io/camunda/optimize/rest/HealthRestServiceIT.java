/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import io.camunda.optimize.service.AbstractMultiEngineIT;
import io.camunda.optimize.service.util.importing.EngineConstants;
import io.camunda.optimize.test.it.extension.ErrorResponseMock;
import io.camunda.optimize.test.it.extension.MockServerUtil;
import jakarta.ws.rs.core.Response;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;

@Tag(OPENSEARCH_PASSING)
public class HealthRestServiceIT extends AbstractMultiEngineIT {

  @Test
  public void getReadiness() {
    // when
    final Response response =
        embeddedOptimizeExtension.getRequestExecutor().buildGetReadinessRequest().execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @Test
  public void getReadiness_noAuthentication() {
    // when
    final Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetReadinessRequest()
            .withoutAuthentication()
            .execute();

    // then the status is still returned
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @Test
  public void getReadiness_multipleEnginesConnection() {
    // given
    addSecondEngineToConfiguration();

    // when
    final Response response =
        embeddedOptimizeExtension.getRequestExecutor().buildGetReadinessRequest().execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @ParameterizedTest
  @MethodSource("engineErrors")
  public void getReadiness_singleConfiguredEngineNotConnected(ErrorResponseMock mockedResponse) {
    // given
    final ClientAndServer engineMockServer = useAndGetEngineMockServer();
    final HttpRequest request = request().withPath(".*" + EngineConstants.VERSION_ENDPOINT);
    mockedResponse.mock(request, Times.unlimited(), engineMockServer);

    // when
    final Response response =
        embeddedOptimizeExtension.getRequestExecutor().buildGetReadinessRequest().execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
  }

  @ParameterizedTest
  @MethodSource("engineErrors")
  public void getReadiness_oneOfMultipleConfiguredEnginesNotConnected(
      ErrorResponseMock mockedResponse) {
    // given
    addSecondEngineToConfiguration();
    final ClientAndServer secondEngineMockServer =
        useAndGetMockServerForEngine(secondaryEngineIntegrationExtension.getEngineName());
    mockedResponse.mock(request(), Times.unlimited(), secondEngineMockServer);

    // when
    final Response response =
        embeddedOptimizeExtension.getRequestExecutor().buildGetReadinessRequest().execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @Test
  public void getReadiness_databaseNotConnected() {
    // given
    final ClientAndServer dbMockServer = useAndGetDbMockServer();
    dbMockServer.when(request()).error(HttpError.error().withDropConnection(true));

    // when
    final Response response =
        embeddedOptimizeExtension.getRequestExecutor().buildGetReadinessRequest().execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
  }

  private static Stream<ErrorResponseMock> engineErrors() {
    return MockServerUtil.engineMockedErrorResponses();
  }
}
