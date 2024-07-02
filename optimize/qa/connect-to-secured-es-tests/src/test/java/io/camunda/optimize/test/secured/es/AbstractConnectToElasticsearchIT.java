/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.test.secured.es;

import static io.camunda.optimize.service.util.configuration.EnvironmentPropertiesConstants.INTEGRATION_TESTS;
import static io.camunda.optimize.upgrade.util.UpgradeUtil.createUpgradeDependenciesWithAdditionalConfigLocation;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.service.metadata.PreviousVersion;
import io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension;
import io.camunda.optimize.upgrade.main.UpgradeProcedure;
import io.camunda.optimize.upgrade.main.UpgradeProcedureFactory;
import io.camunda.optimize.upgrade.plan.factories.CurrentVersionNoOperationUpgradePlanFactory;
import io.camunda.optimize.util.FileReaderUtil;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {"spring.main.allow-bean-definition-overriding=true", INTEGRATION_TESTS + "=true"})
@DirtiesContext
public abstract class AbstractConnectToElasticsearchIT {

  @RegisterExtension
  @Order(1)
  public static EmbeddedOptimizeExtension embeddedOptimizeExtension =
      new EmbeddedOptimizeExtension();

  @BeforeAll
  public static void beforeAll() {
    embeddedOptimizeExtension.getConfigurationService();
  }

  protected abstract String getCustomConfigFile();

  @Test
  public void connectToSecuredElasticsearch() {
    // given a license and a secured optimize -> es connection
    String license = FileReaderUtil.readValidTestLicense();

    // when doing a request to add the license to optimize
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildValidateAndStoreLicenseRequest(license)
            .withoutAuthentication()
            .execute();

    // then Optimize should be able to successfully perform the underlying request to elasticsearch
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @Test
  public void runUpgradeAgainstSecuredElasticSearch() {
    // given an upgrade procedure against ES with custom configuration
    final UpgradeProcedure testUpgradeProcedure =
        UpgradeProcedureFactory.create(
            createUpgradeDependenciesWithAdditionalConfigLocation(getCustomConfigFile()));
    // the metadata version needs to match the stated versionFrom for the upgrade to pass validation
    embeddedOptimizeExtension
        .getDatabaseMetadataService()
        .upsertMetadata(
            embeddedOptimizeExtension.getOptimizeDatabaseClient(),
            PreviousVersion.PREVIOUS_VERSION);

    // then
    testUpgradeProcedure.performUpgrade(
        new CurrentVersionNoOperationUpgradePlanFactory().createUpgradePlan());
  }
}
