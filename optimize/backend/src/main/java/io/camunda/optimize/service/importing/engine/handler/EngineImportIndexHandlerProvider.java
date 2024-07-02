/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.handler;

import static io.camunda.optimize.util.SuppressionConstants.UNCHECKED_CAST;

import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.importing.AllEntitiesBasedImportIndexHandler;
import io.camunda.optimize.service.importing.EngineImportIndexHandler;
import io.camunda.optimize.service.importing.TimestampBasedEngineImportIndexHandler;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EngineImportIndexHandlerProvider {

  private static final List<Class<?>> TIMESTAMP_BASED_HANDLER_CLASSES;
  private static final List<Class<?>> SCROLL_BASED_HANDLER_CLASSES;
  private static final List<Class<?>> ALL_ENTITIES_HANDLER_CLASSES;

  static {
    try (final ScanResult scanResult =
        new ClassGraph()
            .enableClassInfo()
            .acceptPackages(EngineImportIndexHandlerProvider.class.getPackage().getName())
            .scan()) {
      TIMESTAMP_BASED_HANDLER_CLASSES =
          scanResult
              .getSubclasses(TimestampBasedEngineImportIndexHandler.class.getName())
              .loadClasses();
      SCROLL_BASED_HANDLER_CLASSES =
          scanResult.getSubclasses(DefinitionXmlImportIndexHandler.class.getName()).loadClasses();
      ALL_ENTITIES_HANDLER_CLASSES =
          scanResult
              .getSubclasses(AllEntitiesBasedImportIndexHandler.class.getName())
              .loadClasses();
    }
  }

  private final EngineContext engineContext;

  @Autowired private BeanFactory beanFactory;
  private List<AllEntitiesBasedImportIndexHandler> allEntitiesBasedHandlers;
  private List<TimestampBasedEngineImportIndexHandler> timestampBasedEngineHandlers;
  private Map<String, EngineImportIndexHandler<?, ?>> allHandlers;

  public EngineImportIndexHandlerProvider(final EngineContext engineContext) {
    this.engineContext = engineContext;
  }

  @PostConstruct
  public void init() {
    allHandlers = new HashMap<>();

    allEntitiesBasedHandlers = new ArrayList<>();
    timestampBasedEngineHandlers = new ArrayList<>();

    TIMESTAMP_BASED_HANDLER_CLASSES.forEach(
        clazz -> {
          final TimestampBasedEngineImportIndexHandler importIndexHandlerInstance =
              (TimestampBasedEngineImportIndexHandler)
                  getImportIndexHandlerInstance(engineContext, clazz);
          timestampBasedEngineHandlers.add(importIndexHandlerInstance);
          allHandlers.put(clazz.getSimpleName(), importIndexHandlerInstance);
        });

    SCROLL_BASED_HANDLER_CLASSES.forEach(
        clazz -> {
          final EngineImportIndexHandler<?, ?> engineImportIndexHandlerInstance =
              (EngineImportIndexHandler) getImportIndexHandlerInstance(engineContext, clazz);
          allHandlers.put(clazz.getSimpleName(), engineImportIndexHandlerInstance);
        });

    ALL_ENTITIES_HANDLER_CLASSES.forEach(
        clazz -> {
          final EngineImportIndexHandler<?, ?> engineImportIndexHandlerInstance =
              (EngineImportIndexHandler) getImportIndexHandlerInstance(engineContext, clazz);
          allEntitiesBasedHandlers.add(
              (AllEntitiesBasedImportIndexHandler) engineImportIndexHandlerInstance);
          allHandlers.put(clazz.getSimpleName(), engineImportIndexHandlerInstance);
        });
  }

  public List<AllEntitiesBasedImportIndexHandler> getAllEntitiesBasedHandlers() {
    return allEntitiesBasedHandlers;
  }

  public List<TimestampBasedEngineImportIndexHandler> getTimestampBasedEngineHandlers() {
    return timestampBasedEngineHandlers;
  }

  @SuppressWarnings(UNCHECKED_CAST)
  public <C extends EngineImportIndexHandler<?, ?>> C getImportIndexHandler(final Class<C> clazz) {
    return (C) allHandlers.get(clazz.getSimpleName());
  }

  public List<EngineImportIndexHandler<?, ?>> getAllHandlers() {
    return new ArrayList<>(allHandlers.values());
  }

  /**
   * Instantiate index handler for given engine if it has not been instantiated yet. otherwise
   * return already existing instance.
   *
   * @param engineContext - engine alias for instantiation
   * @param requiredType - type of index handler
   * @param <R> - Index handler instance
   * @param <C> - Class signature of required index handler
   */
  private <R, C extends Class<R>> R getImportIndexHandlerInstance(
      final EngineContext engineContext, final C requiredType) {
    final R result;
    if (isInstantiated(requiredType)) {
      result = requiredType.cast(allHandlers.get(requiredType.getSimpleName()));
    } else {
      result = beanFactory.getBean(requiredType, engineContext);
    }
    return result;
  }

  private boolean isInstantiated(final Class<?> handlerClass) {
    return allHandlers.get(handlerClass.getSimpleName()) != null;
  }
}
