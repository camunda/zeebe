/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.testplugin.adapter.variable.dmn2;

import io.camunda.optimize.plugin.importing.variable.DecisionInputImportAdapter;
import io.camunda.optimize.plugin.importing.variable.PluginDecisionInputDto;
import java.util.List;

public class ReturnInvalidInputs implements DecisionInputImportAdapter {

  @Override
  public List<PluginDecisionInputDto> adaptInputs(List<PluginDecisionInputDto> inputs) {
    for (PluginDecisionInputDto input : inputs) {
      input.setType(null);
    }
    return inputs;
  }
}
