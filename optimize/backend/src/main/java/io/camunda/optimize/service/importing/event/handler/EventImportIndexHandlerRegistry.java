/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.event.handler;

import io.camunda.optimize.service.importing.EngineImportIndexHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EventImportIndexHandlerRegistry {

  private final BeanFactory beanFactory;

  private final Map<String, CamundaEventTraceImportIndexHandler> camundaEventTraceHandlers =
      new HashMap<>();

  public List<EngineImportIndexHandler<?, ?>> getAllHandlers() {
    final List<EngineImportIndexHandler<?, ?>> result = new ArrayList<>();
    result.add(getExternalEventTraceImportIndexHandler());
    result.addAll(camundaEventTraceHandlers.values());
    return result;
  }

  public ExternalEventTraceImportIndexHandler getExternalEventTraceImportIndexHandler() {
    return beanFactory.getBean(ExternalEventTraceImportIndexHandler.class);
  }

  public synchronized CamundaEventTraceImportIndexHandler getCamundaEventTraceImportIndexHandler(
      final String definitionKey) {
    return camundaEventTraceHandlers.computeIfAbsent(
        definitionKey, key -> beanFactory.getBean(CamundaEventTraceImportIndexHandler.class, key));
  }
}
