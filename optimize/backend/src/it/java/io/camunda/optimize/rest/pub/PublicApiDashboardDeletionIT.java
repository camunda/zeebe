/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.pub;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.db.DatabaseConstants.DASHBOARD_INDEX_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class PublicApiDashboardDeletionIT extends AbstractPlatformIT {
  private static final String ACCESS_TOKEN = "secret_export_token";

  @Test
  public void deleteDashboard() {
    // given
    setAccessToken();
    final String dashboardId = dashboardClient.createEmptyDashboard();

    // when
    final Response deleteResponse = publicApiClient.deleteDashboard(dashboardId, ACCESS_TOKEN);

    // then
    assertThat(deleteResponse.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertThat(databaseIntegrationTestExtension.getDocumentCountOf(DASHBOARD_INDEX_NAME))
        .isEqualTo(0);
  }

  @Test
  public void deleteDashboardNotExisting() {
    // given
    setAccessToken();

    // when
    final Response deleteResponse = publicApiClient.deleteDashboard("notExisting", ACCESS_TOKEN);

    // then
    assertThat(deleteResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  private void setAccessToken() {
    embeddedOptimizeExtension
        .getConfigurationService()
        .getOptimizeApiConfiguration()
        .setAccessToken(ACCESS_TOKEN);
  }
}
