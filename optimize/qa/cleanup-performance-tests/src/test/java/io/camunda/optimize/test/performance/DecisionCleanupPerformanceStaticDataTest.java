/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.test.performance;

import static io.camunda.optimize.service.db.DatabaseConstants.DECISION_INSTANCE_MULTI_ALIAS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("engine-cleanup")
public class DecisionCleanupPerformanceStaticDataTest extends AbstractDataCleanupTest {

  @BeforeAll
  public static void setUp() {
    embeddedOptimizeExtension.setupOptimize();
    // given
    // Note that when these tests run as a part of a GHA, data is usually imported
    // already during the "import" stage of the job
    importEngineData();
  }

  @Test
  public void cleanupPerformanceTest() throws Exception {
    // given ttl of 0
    getCleanupConfiguration().getDecisionCleanupConfiguration().setEnabled(true);
    getCleanupConfiguration().setTtl(Period.parse("P0D"));
    // we assert there is some data as a precondition as data is expected to be
    // provided by the environment
    assertThat(getDecisionInstanceCount()).isPositive();
    // and run the cleanup
    runCleanupAndAssertFinishedWithinTimeout();
    // and refresh es
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // then no decision instances should be left
    assertThat(getDecisionInstanceCount()).isZero();
  }

  private Integer getDecisionInstanceCount() {
    return databaseIntegrationTestExtension.getDocumentCountOf(DECISION_INSTANCE_MULTI_ALIAS);
  }
}
