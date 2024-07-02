/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class AuthenticationRestServiceIT extends AbstractPlatformIT {

  @Test
  public void authenticateUser() {
    // given
    addAdminUserAndGrantAccessPermission();

    // when
    Response response = embeddedOptimizeExtension.authenticateUserRequest("admin", "admin");

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    String responseEntity = response.readEntity(String.class);
    assertThat(responseEntity).isNotNull();
  }

  @Test
  public void logout() {
    // given
    addAdminUserAndGrantAccessPermission();
    String token = authenticateAdminUser();

    // when
    Response logoutResponse =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildLogOutRequest()
            .withGivenAuthToken(token)
            .execute();

    // then
    assertThat(logoutResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    String responseEntity = logoutResponse.readEntity(String.class);
    assertThat(responseEntity).isEqualTo("OK");
  }

  @Test
  public void logoutSecure() {

    // when
    Response logoutResponse =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildLogOutRequest()
            .withGivenAuthToken("randomToken")
            .execute();

    // then
    assertThat(logoutResponse.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void testAuthenticationIfNotAuthenticated() {
    // when
    Response logoutResponse =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildAuthTestRequest()
            .withoutAuthentication()
            .execute();

    // then
    assertThat(logoutResponse.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void testIfAuthenticated() {
    // when
    Response logoutResponse =
        embeddedOptimizeExtension.getRequestExecutor().buildAuthTestRequest().execute();

    // then
    assertThat(logoutResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  private String authenticateAdminUser() {
    return embeddedOptimizeExtension.authenticateUser("admin", "admin");
  }

  private void addAdminUserAndGrantAccessPermission() {
    engineIntegrationExtension.addUser("admin", "admin");
    engineIntegrationExtension.grantUserOptimizeAccess("admin");
  }
}
