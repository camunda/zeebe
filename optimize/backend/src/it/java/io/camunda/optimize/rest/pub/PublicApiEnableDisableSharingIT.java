/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.pub;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.SettingsDto;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag(OPENSEARCH_PASSING)
public class PublicApiEnableDisableSharingIT extends AbstractPlatformIT {

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void toggleSharingState(boolean enableSharing) {
    // given
    // initialise sharing setting to assert toggled state later
    SettingsDto settings = embeddedOptimizeExtension.getSettingsService().getSettings();
    settings.setSharingEnabled(!enableSharing);
    embeddedOptimizeExtension.getSettingsService().setSettings(settings);

    // then
    // making sure the setting was set correctly
    assertThat(embeddedOptimizeExtension.getSettingsService().getSettings().getSharingEnabled())
        .contains(!enableSharing);
    // making sure the setting was also propagated to the configuration service
    assertThat(embeddedOptimizeExtension.getConfigurationService().getSharingEnabled())
        .isEqualTo(!enableSharing);

    // when
    Response response = publicApiClient.toggleSharing(enableSharing, getAccessToken());

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    // Check if the setting was set correctly
    assertThat(embeddedOptimizeExtension.getSettingsService().getSettings().getSharingEnabled())
        .contains(enableSharing);
    // Check if the setting was also propagated to the configuration service
    assertThat(embeddedOptimizeExtension.getConfigurationService().getSharingEnabled())
        .isEqualTo(enableSharing);
  }

  private String getAccessToken() {
    return Optional.ofNullable(
            embeddedOptimizeExtension
                .getConfigurationService()
                .getOptimizeApiConfiguration()
                .getAccessToken())
        .orElseGet(
            () -> {
              String randomToken = "1_2_Polizei";
              embeddedOptimizeExtension
                  .getConfigurationService()
                  .getOptimizeApiConfiguration()
                  .setAccessToken(randomToken);
              return randomToken;
            });
  }
}
