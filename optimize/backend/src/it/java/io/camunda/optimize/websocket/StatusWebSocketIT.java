/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.websocket;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.JettyConfig;
import io.camunda.optimize.service.util.configuration.EnvironmentPropertiesConstants;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

@Tag(OPENSEARCH_PASSING)
public class StatusWebSocketIT extends AbstractPlatformIT {
  // use single web socket container instance, as every get call is by default creating new
  // instances otherwise
  private static final WebSocketContainer WEB_SOCKET_CONTAINER =
      ContainerProvider.getWebSocketContainer();

  @Test
  public void getImportStatus() throws Exception {
    // given
    final StatusClientSocket socket = new StatusClientSocket();
    try (final Session ignored = connectStatusClientSocket(socket)) {
      final boolean initialStatusCorrectlyReceived =
          socket.getInitialStatusReceivedLatch().await(1, TimeUnit.SECONDS);
      assertThat(initialStatusCorrectlyReceived).isTrue();

      // when
      deployProcessAndTriggerImport();

      // then
      boolean statusCorrectlyReceived =
          socket.getImportingStatusReceivedLatch().await(1, TimeUnit.SECONDS);
      assertThat(statusCorrectlyReceived).isTrue();
    }
  }

  @Test
  public void getImportStatus_zeroMaxStatusConnectionsConfigured() throws Exception {
    // given
    embeddedOptimizeExtension.getConfigurationService().setMaxStatusConnections(0);
    final StatusClientSocket socket = new StatusClientSocket();
    try (final Session ignored = connectStatusClientSocket(socket)) {
      final boolean initialStatusCorrectlyReceived =
          socket.getInitialStatusReceivedLatch().await(1, TimeUnit.SECONDS);
      assertThat(initialStatusCorrectlyReceived).isFalse();

      // when
      deployProcessAndTriggerImport();

      // then
      boolean statusCorrectlyReceived =
          socket.getImportingStatusReceivedLatch().await(1, TimeUnit.SECONDS);
      assertThat(statusCorrectlyReceived).isFalse();
    }
  }

  @Test
  public void importStatusHasChanged() throws Exception {
    // given
    final AssertHasChangedStatusClientSocket socket = new AssertHasChangedStatusClientSocket();

    try (final Session ignored = connectStatusClientSocket(socket)) {
      final boolean initialStatusCorrectlyReceived =
          socket.getInitialStatusReceivedLatch().await(1, TimeUnit.SECONDS);
      assertThat(initialStatusCorrectlyReceived).isTrue();

      // when
      deployProcessAndTriggerImport();

      // then
      assertThat(socket.getReceivedTwoUpdatesLatch().await(1, TimeUnit.SECONDS)).isTrue();
      assertThat(socket.isImportStatusChanged()).isTrue();
    }
  }

  @Test
  public void importNotInProgressStatusOnlyUpdatedOnValueChange() throws Exception {
    // given
    embeddedOptimizeExtension.stopImportScheduling();
    final AssertHasChangedStatusClientSocket socket = new AssertHasChangedStatusClientSocket();

    // when status socket connects
    try (final Session ignored = connectStatusClientSocket(socket)) {
      // then the initial status is received
      final boolean initialStatusCorrectlyReceived =
          socket.getInitialStatusReceivedLatch().await(1, TimeUnit.SECONDS);
      assertThat(initialStatusCorrectlyReceived).isTrue();
      assertThat(socket.getImportStatus()).isFalse();

      // then no update received as no import is running so no status change
      assertThat(socket.getReceivedTwoUpdatesLatch().await(1, TimeUnit.SECONDS)).isFalse();
      assertThat(socket.getImportStatus()).isFalse();
    }
  }

  @Test
  public void importInProgressStatusOnlyUpdatedOnValueChange() throws Exception {
    // given
    embeddedOptimizeExtension.stopImportScheduling();
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    final AssertHasChangedStatusClientSocket socket = new AssertHasChangedStatusClientSocket();

    // when status socket connects
    try (final Session ignored = connectStatusClientSocket(socket)) {
      // then the initial status is received
      final boolean initialStatusCorrectlyReceived =
          socket.getInitialStatusReceivedLatch().await(1, TimeUnit.SECONDS);
      assertThat(initialStatusCorrectlyReceived).isTrue();
      assertThat(socket.getImportStatus()).isTrue();

      // when another import cycle runs and the state doesn't change (still importing)
      embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();

      // then no update is received
      assertThat(socket.getReceivedTwoUpdatesLatch().await(1, TimeUnit.SECONDS)).isFalse();
      assertThat(socket.getImportStatus()).isTrue();
    }
  }

  @Test
  public void importStatusStaysFalseIfImportIsDeactivated() throws Exception {
    // given
    embeddedOptimizeExtension
        .getConfigurationService()
        .getConfiguredEngines()
        .values()
        .forEach(engineConfiguration -> engineConfiguration.setImportEnabled(false));
    embeddedOptimizeExtension.reloadConfiguration();

    final AssertHasChangedStatusClientSocket socket = new AssertHasChangedStatusClientSocket();
    try (final Session ignored = connectStatusClientSocket(socket)) {
      final boolean initialStatusCorrectlyReceived =
          socket.getInitialStatusReceivedLatch().await(1, TimeUnit.SECONDS);
      assertThat(initialStatusCorrectlyReceived).isTrue();

      // when
      BpmnModelInstance processModel = getSimpleBpmnDiagram();
      engineIntegrationExtension.deployAndStartProcess(processModel);

      // then
      embeddedOptimizeExtension
          .getImportSchedulerManager()
          .getEngineImportSchedulers()
          .forEach(
              engineImportScheduler ->
                  assertThat(engineImportScheduler.isScheduledToRun()).isFalse());
      assertThat(socket.getReceivedTwoUpdatesLatch().await(1, TimeUnit.SECONDS)).isFalse();
      assertThat(socket.getReceivedTwoUpdatesLatch().getCount()).isEqualTo(1L);
      assertThat(socket.getImportStatus()).isFalse();
      assertThat(socket.isImportStatusChanged()).isFalse();
    } finally {
      // cleanup
      embeddedOptimizeExtension
          .getConfigurationService()
          .getConfiguredEngines()
          .values()
          .forEach(engineConfiguration -> engineConfiguration.setImportEnabled(true));
      embeddedOptimizeExtension.reloadConfiguration();
    }
  }

  @Test
  public void engineConnectionStatusValuesReadFromCacheWhenAvailable() throws Exception {
    // given
    final HttpRequest engineVersionRequestMatcher =
        request().withPath(engineIntegrationExtension.getEnginePath() + "/version");
    final ClientAndServer engineMockServer = useAndGetEngineMockServer();
    final StatusClientSocket socket = new StatusClientSocket();
    try (final Session ignored = connectStatusClientSocket(socket)) {
      final boolean initialStatusCorrectlyReceived =
          socket.getInitialStatusReceivedLatch().await(2, TimeUnit.SECONDS);
      assertThat(initialStatusCorrectlyReceived).isTrue();

      // when
      deployProcessAndTriggerImport();

      // then
      boolean statusCorrectlyReceived =
          socket.getImportingStatusReceivedLatch().await(1, TimeUnit.SECONDS);
      assertThat(statusCorrectlyReceived).isTrue();
      // only one request to the engine was made
      engineMockServer.verify(engineVersionRequestMatcher, VerificationTimes.exactly(1));
    }
  }

  private Session connectStatusClientSocket(Object statusClientSocket)
      throws DeploymentException, IOException, URISyntaxException {
    final String dest =
        String.format(
            "ws://localhost:%d/ws/status",
            embeddedOptimizeExtension
                .getBean(JettyConfig.class)
                .getPort(EnvironmentPropertiesConstants.HTTP_PORT_KEY));
    return WEB_SOCKET_CONTAINER.connectToServer(statusClientSocket, new URI(dest));
  }

  private void deployProcessAndTriggerImport() {
    BpmnModelInstance processModel = getSimpleBpmnDiagram();
    engineIntegrationExtension.deployAndStartProcess(processModel);
    importAllEngineEntitiesFromScratch();
  }
}
