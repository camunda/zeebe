/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.plugin.importing.variable;

import java.util.List;

public interface DecisionInputImportAdapter {

  /**
   * Adapts the list of Decision inputs to be imported by adding new entities or filter variables
   * that should not be analyzed by Optimize.
   *
   * @param inputs The decision input instances that would be imported by Optimize, which represent
   *     instances from the engine.
   * @return An adapted list that is imported to Optimize.
   */
  List<PluginDecisionInputDto> adaptInputs(List<PluginDecisionInputDto> inputs);
}
