/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.data.generation.generators.impl.decision;

import com.google.common.collect.ImmutableList;
import io.camunda.optimize.test.util.client.SimpleEngineClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.model.dmn.DmnModelInstance;

public class ExampleDmn13DataGenerator extends DecisionDataGenerator {

  private static final String DMN_DIAGRAM = "/diagrams/decision/Example-DMN-1.3.dmn";

  private final Pair<String, String> inputVarNames = Pair.of("status", "sum");
  private final List<Pair<String, Double>> possibleInputCombinations =
      ImmutableList.of(
          Pair.of("bronze", RandomUtils.nextDouble()),
          Pair.of("silver", RandomUtils.nextDouble(0, 999.99)),
          Pair.of("silver", RandomUtils.nextDouble(1000, Double.MAX_VALUE)),
          Pair.of("gold", RandomUtils.nextDouble()));

  public ExampleDmn13DataGenerator(final SimpleEngineClient engineClient, final Integer nVersions) {
    super(engineClient, nVersions);
  }

  @Override
  protected DmnModelInstance retrieveDiagram() {
    return readDecisionDiagram(DMN_DIAGRAM);
  }

  @Override
  protected Map<String, Object> createVariables() {
    final int nextCombinationIndex = RandomUtils.nextInt(0, possibleInputCombinations.size());
    final Pair<String, Double> nextCombination =
        possibleInputCombinations.get(nextCombinationIndex);
    final Map<String, Object> variables = new HashMap<>();
    variables.put(inputVarNames.getLeft(), nextCombination.getLeft());
    variables.put(inputVarNames.getRight(), nextCombination.getRight());
    return variables;
  }
}
