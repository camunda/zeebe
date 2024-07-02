/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.data.generation.generators.impl.decision;

import io.camunda.optimize.test.util.client.SimpleEngineClient;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.model.dmn.DmnModelInstance;

public class BerStatusDateInputDecisionDataGenerator extends DecisionDataGenerator {

  private static final String DMN_DIAGRAM = "/diagrams/decision/berStatusDateInputDecision.dmn";

  private final Pair<String, String> inputVarNames = Pair.of("flightDestination", "date");
  private final List<Pair<String, Date>> possibleInputCombinations;

  public BerStatusDateInputDecisionDataGenerator(
      SimpleEngineClient engineClient, Integer nVersions) {
    super(engineClient, nVersions);

    // create some date vars within the last 10 years and the last
    // 10 mins for date variable reports with different groupBy units
    possibleInputCombinations = new ArrayList<>();
    IntStream.range(0, 10)
        .forEach(
            i -> {
              final long tenMinsAgo =
                  ZonedDateTime.now().minusMinutes(10).toInstant().toEpochMilli();
              final long tenYearsAgo =
                  ZonedDateTime.now().minusYears(10).toInstant().toEpochMilli();
              final long randomWithinLastTenYears =
                  ThreadLocalRandom.current().nextLong(tenYearsAgo, Instant.now().toEpochMilli());
              final long randomWithinLastTenMins =
                  ThreadLocalRandom.current().nextLong(tenMinsAgo, Instant.now().toEpochMilli());
              possibleInputCombinations.add(Pair.of("Glasgow", new Date(randomWithinLastTenYears)));
              possibleInputCombinations.add(Pair.of("Omran", new Date(randomWithinLastTenYears)));
              possibleInputCombinations.add(Pair.of("Malaga", new Date(randomWithinLastTenMins)));
            });
  }

  @Override
  protected DmnModelInstance retrieveDiagram() {
    return readDecisionDiagram(DMN_DIAGRAM);
  }

  @Override
  protected Map<String, Object> createVariables() {
    final int nextCombinationIndex = RandomUtils.nextInt(0, possibleInputCombinations.size());
    final Pair<String, Date> nextCombination = possibleInputCombinations.get(nextCombinationIndex);
    Map<String, Object> variables = new HashMap<>();
    variables.put(inputVarNames.getLeft(), nextCombination.getLeft());
    variables.put(inputVarNames.getRight(), nextCombination.getRight());
    return variables;
  }
}
