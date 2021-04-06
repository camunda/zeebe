/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.security.authorization;

import org.camunda.optimize.AbstractIT;
import org.camunda.optimize.dto.optimize.SettingsResponseDto;
import org.camunda.optimize.util.SuperUserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.test.engine.AuthorizationClient.GROUP_ID;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_PASSWORD;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_USERNAME;

public class SettingsAuthorizationIT extends AbstractIT {
  @ParameterizedTest
  @EnumSource(SuperUserType.class)
  public void testSetSettings_asSuperUser(SuperUserType superUserType) {
    // given
    final SettingsResponseDto newSettings = SettingsResponseDto.builder().metadataTelemetryEnabled(true).build();
    if (superUserType == SuperUserType.USER) {
      embeddedOptimizeExtension.getConfigurationService().getSuperUserIds().add(DEFAULT_USERNAME);
    } else {
      authorizationClient.addUserAndGrantOptimizeAccess(DEFAULT_USERNAME);
      authorizationClient.createGroupAndAddUser(GROUP_ID,DEFAULT_USERNAME);
      embeddedOptimizeExtension.getConfigurationService().getSuperGroupIds().add(GROUP_ID);
    }

    // when
    final Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .withUserAuthentication(DEFAULT_USERNAME, DEFAULT_PASSWORD)
      .buildSetSettingsRequest(newSettings)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testSetSettings_asNonSuperUser_Forbidden() {
    // given
    final SettingsResponseDto newSettings = SettingsResponseDto.builder().metadataTelemetryEnabled(true).build();

    // when
    final Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .withUserAuthentication(DEFAULT_USERNAME, DEFAULT_PASSWORD)
      .buildSetSettingsRequest(newSettings)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void testSetSettings_withoutAuthentication_Unauthorized() {
    // given
    final SettingsResponseDto newSettings = SettingsResponseDto.builder().metadataTelemetryEnabled(true).build();

    // when
    final Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .withoutAuthentication()
      .buildSetSettingsRequest(newSettings)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }
}
