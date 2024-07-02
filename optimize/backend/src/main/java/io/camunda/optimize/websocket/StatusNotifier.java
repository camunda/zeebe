/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.query.status.EngineStatusDto;
import io.camunda.optimize.dto.optimize.query.status.StatusResponseDto;
import io.camunda.optimize.service.importing.engine.service.ImportObserver;
import io.camunda.optimize.service.status.StatusCheckingService;
import java.io.IOException;
import java.util.Map;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusNotifier implements ImportObserver {
  private static final Logger logger = LoggerFactory.getLogger(StatusNotifier.class);

  private final StatusCheckingService statusCheckingService;
  private final ObjectMapper objectMapper;
  private final Session session;

  private final Map<String, EngineStatusDto> engineStatusMap;

  public StatusNotifier(
      final StatusCheckingService statusCheckingService,
      final ObjectMapper objectMapper,
      final Session session) {
    this.statusCheckingService = statusCheckingService;
    this.objectMapper = objectMapper;
    this.session = session;
    engineStatusMap = statusCheckingService.getStatusResponse().getEngineStatus();
    sendStatus();
  }

  @Override
  public synchronized void importInProgress(final String engineAlias) {
    final boolean containsKey = engineStatusMap.containsKey(engineAlias);
    final boolean sendUpdate = containsKey && !engineStatusMap.get(engineAlias).getIsImporting();
    final EngineStatusDto engineStatus = new EngineStatusDto();
    if (containsKey) {
      engineStatus.setIsConnected(engineStatusMap.get(engineAlias).getIsConnected());
    } else {
      engineStatus.setIsConnected(false);
    }
    engineStatus.setIsImporting(true);
    engineStatusMap.put(engineAlias, engineStatus);
    if (sendUpdate) {
      sendStatus();
    }
  }

  @Override
  public synchronized void importIsIdle(final String engineAlias) {
    final boolean containsKey = engineStatusMap.containsKey(engineAlias);
    final boolean sendUpdate = containsKey && engineStatusMap.get(engineAlias).getIsImporting();
    final EngineStatusDto engineStatus = new EngineStatusDto();
    if (containsKey) {
      engineStatus.setIsConnected(engineStatusMap.get(engineAlias).getIsConnected());
    } else {
      engineStatus.setIsConnected(false);
    }
    engineStatus.setIsImporting(false);
    engineStatusMap.put(engineAlias, engineStatus);
    if (sendUpdate) {
      sendStatus();
    }
  }

  private void sendStatus() {
    final StatusResponseDto result = statusCheckingService.getCachedStatusResponse();
    try {
      if (session.isOpen()) {
        session.sendText(objectMapper.writeValueAsString(result), Callback.NOOP);
      } else {
        logger.debug(
            "Could not write to websocket session [{}], because it already seems closed.", session);
      }
    } catch (final IOException e) {
      logger.warn("can't write status to web socket");
      logger.debug("Exception when writing status", e);
    }
  }
}
