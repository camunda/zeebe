/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.test.performance;

import static io.camunda.optimize.service.db.DatabaseConstants.DECISION_DEFINITION_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.DECISION_INSTANCE_MULTI_ALIAS;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_DEFINITION_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_INSTANCE_MULTI_ALIAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.test.it.extension.DatabaseIntegrationTestExtension;
import io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension;
import io.camunda.optimize.test.it.extension.EngineDatabaseExtension;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public abstract class AbstractImportTest {
  @RegisterExtension
  @Order(2)
  public static EmbeddedOptimizeExtension embeddedOptimizeExtension =
      new EmbeddedOptimizeExtension();

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private final Properties properties = getProperties();

  @RegisterExtension
  @Order(1)
  public DatabaseIntegrationTestExtension databaseIntegrationTestExtension =
      new DatabaseIntegrationTestExtension();

  @RegisterExtension
  @Order(3)
  public EngineDatabaseExtension engineDatabaseExtension = new EngineDatabaseExtension(properties);

  protected long maxImportDurationInMin;
  protected ConfigurationService configurationService;

  abstract Properties getProperties();

  @BeforeEach
  public void setUp() {
    maxImportDurationInMin =
        Long.parseLong(properties.getProperty("import.test.max.duration.in.min", "240"));
    databaseIntegrationTestExtension.disableCleanup();
    configurationService = embeddedOptimizeExtension.getConfigurationService();
    configurationService
        .getCleanupServiceConfiguration()
        .getProcessDataCleanupConfiguration()
        .setEnabled(false);
  }

  protected void logStats() {
    try {
      logger.info(
          "The Camunda Platform contains {} process definitions. Optimize: {}",
          (engineDatabaseExtension.countProcessDefinitions()),
          databaseIntegrationTestExtension.getDocumentCountOf(PROCESS_DEFINITION_INDEX_NAME));
      logger.info(
          "The Camunda Platform contains {} historic process instances. Optimize: {}",
          engineDatabaseExtension.countHistoricProcessInstances(),
          databaseIntegrationTestExtension.getDocumentCountOf(PROCESS_INSTANCE_MULTI_ALIAS));
      logger.info(
          "The Camunda Platform contains {} historic variable instances. Optimize: {}",
          engineDatabaseExtension.countHistoricVariableInstances(),
          databaseIntegrationTestExtension.getVariableInstanceCountForAllProcessInstances());
      logger.info(
          "The Camunda Platform contains {} historic activity instances. Optimize: {}",
          engineDatabaseExtension.countHistoricActivityInstances(),
          databaseIntegrationTestExtension.getActivityCountForAllProcessInstances());

      logger.info(
          "The Camunda Platform contains {} decision definitions. Optimize: {}",
          engineDatabaseExtension.countDecisionDefinitions(),
          databaseIntegrationTestExtension.getDocumentCountOf(DECISION_DEFINITION_INDEX_NAME));
      logger.info(
          "The Camunda Platform contains {} historic decision instances. Optimize: {}",
          engineDatabaseExtension.countHistoricDecisionInstances(),
          databaseIntegrationTestExtension.getDocumentCountOf(DECISION_INSTANCE_MULTI_ALIAS));
    } catch (final SQLException e) {
      logger.error("Failed producing stats", e);
    }
  }

  protected ScheduledExecutorService reportImportProgress() {
    final ScheduledExecutorService exec =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName()).build());
    exec.scheduleAtFixedRate(
        () -> logger.info("Progress of engine import: {}%", computeImportProgress()),
        0,
        5,
        TimeUnit.SECONDS);
    return exec;
  }

  private long computeImportProgress() {
    // assumption: we know how many process instances have been generated
    final Integer processInstancesImported =
        databaseIntegrationTestExtension.getDocumentCountOf(PROCESS_INSTANCE_MULTI_ALIAS);
    final long totalInstances;
    try {
      totalInstances = Math.max(engineDatabaseExtension.countHistoricProcessInstances(), 1L);
      return Math.round(processInstancesImported.doubleValue() / (double) totalInstances * 100);
    } catch (final SQLException e) {
      e.printStackTrace();
      return 0L;
    }
  }

  protected void assertThatEngineAndElasticDataMatch() throws SQLException {
    assertThat(databaseIntegrationTestExtension.getDocumentCountOf(PROCESS_DEFINITION_INDEX_NAME))
        .as("processDefinitionsCount")
        .isEqualTo(engineDatabaseExtension.countProcessDefinitions());
    assertThat(databaseIntegrationTestExtension.getDocumentCountOf(PROCESS_INSTANCE_MULTI_ALIAS))
        .as("processInstanceTypeCount")
        .isEqualTo(engineDatabaseExtension.countHistoricProcessInstances());
    assertThat(databaseIntegrationTestExtension.getVariableInstanceCountForAllProcessInstances())
        .as("variableInstanceCount")
        .isGreaterThanOrEqualTo(engineDatabaseExtension.countHistoricVariableInstances());
    assertThat(databaseIntegrationTestExtension.getActivityCountForAllProcessInstances())
        .as("historicActivityInstanceCount")
        .isEqualTo(engineDatabaseExtension.countHistoricActivityInstances());
    assertThat(databaseIntegrationTestExtension.getDocumentCountOf(DECISION_DEFINITION_INDEX_NAME))
        .as("decisionDefinitionsCount")
        .isEqualTo(engineDatabaseExtension.countDecisionDefinitions());
    assertThat(databaseIntegrationTestExtension.getDocumentCountOf(DECISION_INSTANCE_MULTI_ALIAS))
        .as("decisionInstancesCount")
        .isEqualTo(engineDatabaseExtension.countHistoricDecisionInstances());
  }
}
