/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.test.performance;

import static io.camunda.optimize.service.db.DatabaseConstants.BUSINESS_KEY_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.CAMUNDA_ACTIVITY_EVENT_INDEX_PREFIX;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_INSTANCE_MULTI_ALIAS;
import static io.camunda.optimize.service.db.DatabaseConstants.VARIABLE_UPDATE_INSTANCE_INDEX_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.service.db.schema.index.BusinessKeyIndex;
import io.camunda.optimize.service.db.schema.index.VariableUpdateInstanceIndex;
import io.camunda.optimize.service.db.schema.index.events.CamundaActivityEventIndex;
import io.camunda.optimize.service.util.configuration.cleanup.CleanupMode;
import java.time.Period;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.core.TimeValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Slf4j
@Tag("engine-cleanup")
@Tag("event-cleanup")
public class ProcessCleanupPerformanceStaticDataTest extends AbstractDataCleanupTest {

  public static final TimeValue SCROLL_KEEP_ALIVE = new TimeValue(5, TimeUnit.MINUTES);

  @BeforeAll
  public static void setUp() {
    embeddedOptimizeExtension.setupOptimize();
    // given
    // Note that when these tests run as a part of a GHA, data is usually imported already during
    // the "import" stage of the job
    importEngineData();
  }

  @Test
  @Order(1)
  public void cleanupModeProcessVariablesPerformanceTest() throws Exception {
    // given TTL of 0
    getCleanupConfiguration().getProcessDataCleanupConfiguration().setEnabled(true);
    getCleanupConfiguration().setTtl(Period.parse("P0D"));
    getCleanupConfiguration()
        .getProcessDataCleanupConfiguration()
        .setCleanupMode(CleanupMode.VARIABLES);
    embeddedOptimizeExtension.reloadConfiguration();
    final int processInstanceCount = getCamundaProcessInstanceCount();
    // we assert there is some data as a precondition as data is expected to be provided by the
    // environment
    assertThat(processInstanceCount).isPositive();
    assertThat(getFinishedProcessInstanceVariableCount()).isPositive();
    // and run the cleanup
    runCleanupAndAssertFinishedWithinTimeout();
    // and refresh es
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // then no variables are left on finished process instances
    assertThat(getFinishedProcessInstanceVariableCount()).isZero();
    // and only variable updates related to running process instances are there
    verifyThatAllCamundaVariableUpdatesAreRelatedToRunningInstancesOnly();
    // but instances are untouched
    assertThat(getCamundaProcessInstanceCount()).isEqualTo(processInstanceCount);
  }

  @Test
  @Order(2)
  public void cleanupModeAllPerformanceTest() throws Exception {
    // given ttl of 0
    getCleanupConfiguration().getProcessDataCleanupConfiguration().setEnabled(true);
    getCleanupConfiguration().setTtl(Period.parse("P0D"));
    getCleanupConfiguration().getProcessDataCleanupConfiguration().setCleanupMode(CleanupMode.ALL);
    // we assert there is some data as a precondition as data is expected to be provided by the
    // environment
    assertThat(getCamundaProcessInstanceCount()).isPositive();
    // and run the cleanup
    runCleanupAndAssertFinishedWithinTimeout();
    // and refresh es
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();

    // then no finished process instances should be left in optimize
    assertThat(getFinishedProcessInstanceCount()).isZero();
    // and only camunda activity events related to running process instances are there
    verifyThatAllCamundaActivityEventsAreRelatedToRunningInstancesOnly();
    // and only variable updates related to running process instances are there
    verifyThatAllCamundaVariableUpdatesAreRelatedToRunningInstancesOnly();
    // and only businessKey entries related to running process instances are there
    verifyThatAllBusinessKeyEntriesAreRelatedToRunningInstancesOnly();
  }

  @SneakyThrows
  private void verifyThatAllCamundaVariableUpdatesAreRelatedToRunningInstancesOnly() {
    verifyThatAllDocumentsOfIndexAreRelatedToRunningInstancesOnly(
        VARIABLE_UPDATE_INSTANCE_INDEX_NAME + "*", VariableUpdateInstanceIndex.PROCESS_INSTANCE_ID);
  }

  @SneakyThrows
  private void verifyThatAllCamundaActivityEventsAreRelatedToRunningInstancesOnly() {
    verifyThatAllDocumentsOfIndexAreRelatedToRunningInstancesOnly(
        CAMUNDA_ACTIVITY_EVENT_INDEX_PREFIX + "*", CamundaActivityEventIndex.PROCESS_INSTANCE_ID);
  }

  @SneakyThrows
  private void verifyThatAllBusinessKeyEntriesAreRelatedToRunningInstancesOnly() {
    verifyThatAllDocumentsOfIndexAreRelatedToRunningInstancesOnly(
        BUSINESS_KEY_INDEX_NAME + "*", BusinessKeyIndex.PROCESS_INSTANCE_ID);
  }

  @SneakyThrows
  private void verifyThatAllDocumentsOfIndexAreRelatedToRunningInstancesOnly(
      final String entityIndex, final String processInstanceField) {
    databaseIntegrationTestExtension.verifyThatAllDocumentsOfIndexAreRelatedToRunningInstancesOnly(
        entityIndex, processInstanceField, SCROLL_KEEP_ALIVE);
  }

  private Integer getCamundaProcessInstanceCount() {
    return databaseIntegrationTestExtension.getDocumentCountOf(PROCESS_INSTANCE_MULTI_ALIAS);
  }

  private Integer getFinishedProcessInstanceCount() {
    return databaseIntegrationTestExtension.getCountOfCompletedInstances();
  }

  private Integer getFinishedProcessInstanceVariableCount() {
    return databaseIntegrationTestExtension
        .getVariableInstanceCountForAllCompletedProcessInstances();
  }
}
