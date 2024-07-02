/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.ApplicationContextProvider;
import io.camunda.optimize.service.importing.ImportSchedulerManagerService;
import io.camunda.optimize.service.status.StatusCheckingService;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@NoArgsConstructor
@Slf4j
@WebSocket
public class StatusWebSocket {
  private static final String ERROR_MESSAGE = "Web socket connection terminated prematurely!";
  private final StatusCheckingService statusCheckingService =
      ApplicationContextProvider.getBean(StatusCheckingService.class);
  private final ObjectMapper objectMapper = ApplicationContextProvider.getBean(ObjectMapper.class);
  private final ConfigurationService configurationService =
      ApplicationContextProvider.getBean(ConfigurationService.class);
  private final ImportSchedulerManagerService importSchedulerManagerService =
      ApplicationContextProvider.getBean(ImportSchedulerManagerService.class);

  private final Map<String, StatusNotifier> statusReportJobs = new ConcurrentHashMap<>();

  @OnWebSocketOpen
  public void onOpen(final Session session) {
    if (statusReportJobs.size() < configurationService.getMaxStatusConnections()) {
      final StatusNotifier job = new StatusNotifier(statusCheckingService, objectMapper, session);
      statusReportJobs.put(session.toString(), job);
      importSchedulerManagerService.subscribeImportObserver(job);
      log.debug("starting to report status for session [{}]", session);
    } else {
      log.debug("cannot create status report job for [{}], max connections exceeded", session);
      session.close();
    }
  }

  @OnWebSocketClose
  public void onClose(final Session session, final int statusCode, final String reason) {
    log.debug("stopping status reporting for session");
    removeSession(session);
  }

  private void removeSession(final Session session) {
    if (statusReportJobs.containsKey(session.toString())) {
      final StatusNotifier job = statusReportJobs.remove(session.toString());
      importSchedulerManagerService.unsubscribeImportObserver(job);
    }
  }

  @OnWebSocketError
  public void onError(final Session session, final Throwable t) {
    if (log.isWarnEnabled()) {
      log.warn(ERROR_MESSAGE);
    } else if (log.isDebugEnabled()) {
      log.debug(ERROR_MESSAGE, t);
    }
    removeSession(session);
  }
}
