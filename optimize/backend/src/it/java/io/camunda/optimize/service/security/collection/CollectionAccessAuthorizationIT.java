/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.security.collection;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.dto.optimize.RoleType;
import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto;
import io.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.rest.AuthorizedCollectionDefinitionRestDto;
import io.camunda.optimize.service.security.CaseInsensitiveAuthenticationMockUtil;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;

@Tag(OPENSEARCH_PASSING)
public class CollectionAccessAuthorizationIT extends AbstractCollectionRoleIT {

  @Test
  public void creatorCanAccessCollection() {
    // given
    final String collectionId = collectionClient.createNewCollectionForAllDefinitionTypes();

    // when
    AuthorizedCollectionDefinitionRestDto collection =
        collectionClient.getAuthorizedCollectionById(collectionId);

    // then
    assertThat(collection.getDefinitionDto().getId()).isEqualTo(collectionId);
    assertThat(collection.getCurrentUserRole()).isEqualTo(RoleType.MANAGER);
  }

  @Test
  public void
      collectionAccessDoesNotDependOnUsernameCaseAtLoginWithCaseInsensitiveAuthenticationBackend() {
    // given
    final String allUpperCaseUserId = DEFAULT_USERNAME.toUpperCase();
    final String actualUserId = DEFAULT_USERNAME;
    final ClientAndServer engineMockServer = useAndGetEngineMockServer();

    final List<HttpRequest> mockedRequests =
        CaseInsensitiveAuthenticationMockUtil.setupCaseInsensitiveAuthentication(
            embeddedOptimizeExtension,
            engineIntegrationExtension,
            engineMockServer,
            allUpperCaseUserId,
            actualUserId);

    final String collectionId = collectionClient.createNewCollectionForAllDefinitionTypes();

    // when
    AuthorizedCollectionDefinitionRestDto collection =
        collectionClient.getAuthorizedCollectionById(
            collectionId, allUpperCaseUserId, actualUserId);

    // then
    assertThat(collection.getDefinitionDto().getId()).isEqualTo(collectionId);
    assertThat(collection.getCurrentUserRole()).isEqualTo(RoleType.MANAGER);

    mockedRequests.forEach(engineMockServer::verify);
  }

  @ParameterizedTest
  @MethodSource(ACCESS_IDENTITY_ROLES)
  public void identityIsGrantedAccessByCollectionRole(
      final IdentityAndRole accessIdentityRolePairs) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    authorizationClient.grantKermitGroupOptimizeAccess();

    final String collectionId = collectionClient.createNewCollectionForAllDefinitionTypes();
    createSimpleProcessReportInCollectionAsDefaultUser(collectionId);
    createDashboardInCollectionAsDefaultUser(collectionId);
    addRoleToCollectionAsDefaultUser(
        accessIdentityRolePairs.roleType, accessIdentityRolePairs.identityDto, collectionId);

    // when
    AuthorizedCollectionDefinitionRestDto collection =
        collectionClient.getAuthorizedCollectionById(collectionId, KERMIT_USER, KERMIT_USER);

    final List<EntityResponseDto> entities =
        collectionClient.getEntitiesForCollection(collectionId, KERMIT_USER, KERMIT_USER);
    // then
    assertThat(collection.getDefinitionDto().getId()).isEqualTo(collectionId);
    assertThat(collection.getCurrentUserRole()).isEqualTo(accessIdentityRolePairs.roleType);

    assertThat(entities).hasSize(2);
    assertThat(entities.get(0).getCurrentUserRole())
        .isEqualTo(getExpectedResourceRoleForCollectionRole(accessIdentityRolePairs));
    assertThat(entities.get(1).getCurrentUserRole())
        .isEqualTo(getExpectedResourceRoleForCollectionRole(accessIdentityRolePairs));
  }

  @Test
  public void userIsGrantedAccessAsSuperUser() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    embeddedOptimizeExtension
        .getConfigurationService()
        .getAuthConfiguration()
        .getSuperUserIds()
        .add(KERMIT_USER);

    final String collectionId = collectionClient.createNewCollectionForAllDefinitionTypes();

    // when + then
    collectionClient.getAuthorizedCollectionById(collectionId, KERMIT_USER, KERMIT_USER);
  }

  @Test
  public void userIsNotGrantedAccessDueMissingRole() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    final String collectionId = collectionClient.createNewCollectionForAllDefinitionTypes();

    // when
    Response response =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .withUserAuthentication(KERMIT_USER, KERMIT_USER)
            .buildGetCollectionRequest(collectionId)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  private void createSimpleProcessReportInCollectionAsDefaultUser(final String collectionId) {
    CombinedReportDefinitionRequestDto combinedReportDefinitionDto =
        new CombinedReportDefinitionRequestDto();
    combinedReportDefinitionDto.setCollectionId(collectionId);
    reportClient.createCombinedReport(collectionId, new ArrayList<>());
  }

  private void createDashboardInCollectionAsDefaultUser(final String collectionId) {
    DashboardDefinitionRestDto dashboardDefinitionDto = new DashboardDefinitionRestDto();
    dashboardDefinitionDto.setCollectionId(collectionId);
    dashboardClient.createDashboard(collectionId, new ArrayList<>());
  }
}
