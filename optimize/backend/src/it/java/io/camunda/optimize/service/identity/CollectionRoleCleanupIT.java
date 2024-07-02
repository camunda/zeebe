/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.identity;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.service.util.importing.EngineConstants.AUTHORIZATION_ENDPOINT;
import static io.camunda.optimize.service.util.importing.EngineConstants.OPTIMIZE_APPLICATION_RESOURCE_ID;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_APPLICATION;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_EMAIL_DOMAIN;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FIRSTNAME;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_LASTNAME;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.KERMIT_GROUP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.GroupDto;
import io.camunda.optimize.dto.optimize.IdentityDto;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.IdentityWithMetadataResponseDto;
import io.camunda.optimize.dto.optimize.RoleType;
import io.camunda.optimize.dto.optimize.UserDto;
import io.camunda.optimize.dto.optimize.query.IdResponseDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionRoleRequestDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionRoleResponseDto;
import io.camunda.optimize.exception.OptimizeIntegrationTestException;
import io.camunda.optimize.service.CollectionRoleCleanupService;
import io.camunda.optimize.test.it.extension.ErrorResponseMock;
import io.camunda.optimize.test.it.extension.MockServerUtil;
import io.camunda.optimize.util.SuppressionConstants;
import io.github.netmikey.logunit.api.LogCapturer;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

@Tag(OPENSEARCH_PASSING)
public class CollectionRoleCleanupIT extends AbstractPlatformIT {

  private static final String USER_KERMIT = "kermit";
  private static final String TEST_GROUP = "testGroup";
  private static final String TEST_GROUP_B = "anotherTestGroup";

  @RegisterExtension
  @Order(5)
  protected final LogCapturer collectionRoleCleanupServiceLogCapturer =
      LogCapturer.create().captureForType(CollectionRoleCleanupService.class);

  @BeforeEach
  public void setup() {
    embeddedOptimizeExtension
        .getConfigurationService()
        .getUserIdentityCacheConfiguration()
        .setCollectionRoleCleanupEnabled(true);
  }

  @AfterEach
  public void tearDown() {
    embeddedOptimizeExtension
        .getConfigurationService()
        .getUserIdentityCacheConfiguration()
        .setCollectionRoleCleanupEnabled(false);
  }

  @Test
  public void cleanupAfterIdentitySync() {
    final PlatformUserIdentityCache userIdentityCacheService = getUserIdentityCacheService();
    try {
      // given a collection with permissions for users/groups
      userIdentityCacheService.stopScheduledSync();

      authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_APPLICATION);
      authorizationClient.addKermitUserWithoutAuthorizations();
      authorizationClient.addUserAndGrantOptimizeAccess(DEFAULT_USERNAME);
      authorizationClient.createGroupAndAddUser(TEST_GROUP, USER_KERMIT);
      authorizationClient.createGroupAndAddUser(TEST_GROUP_B, DEFAULT_USERNAME);

      userIdentityCacheService.synchronizeIdentities();

      final String collectionId1 = collectionClient.createNewCollection();
      final String collectionId2 = collectionClient.createNewCollection();

      CollectionRoleRequestDto testGroupRole =
          new CollectionRoleRequestDto(
              new IdentityDto(TEST_GROUP, IdentityType.GROUP), RoleType.EDITOR);
      CollectionRoleRequestDto testGroupBRole =
          new CollectionRoleRequestDto(
              new IdentityDto(TEST_GROUP_B, IdentityType.GROUP), RoleType.EDITOR);
      CollectionRoleRequestDto userKermitRole =
          new CollectionRoleRequestDto(
              new IdentityDto(USER_KERMIT, IdentityType.USER), RoleType.EDITOR);
      CollectionRoleRequestDto userDemoRole =
          new CollectionRoleRequestDto(
              new IdentityDto(DEFAULT_USERNAME, IdentityType.USER), RoleType.MANAGER);

      collectionClient.addRolesToCollection(collectionId1, testGroupRole);
      collectionClient.addRolesToCollection(collectionId1, testGroupBRole);
      collectionClient.addRolesToCollection(collectionId1, userKermitRole);

      collectionClient.addRolesToCollection(collectionId2, testGroupRole);
      collectionClient.addRolesToCollection(collectionId2, testGroupBRole);
      collectionClient.addRolesToCollection(collectionId2, userKermitRole);

      // when users/groups are removed from identityCache
      authorizationClient.revokeSingleResourceAuthorizationsForGroup(
          TEST_GROUP, OPTIMIZE_APPLICATION_RESOURCE_ID, RESOURCE_TYPE_APPLICATION);
      authorizationClient.revokeSingleResourceAuthorizationsForUser(
          USER_KERMIT, OPTIMIZE_APPLICATION_RESOURCE_ID, RESOURCE_TYPE_APPLICATION);

      userIdentityCacheService.synchronizeIdentities();

      // then users/groups no longer existing in identityCache have been removed from the
      // collection's permissions
      List<IdResponseDto> roleIds1 = collectionClient.getCollectionRoleIdDtos(collectionId1);
      List<IdResponseDto> roleIds2 = collectionClient.getCollectionRoleIdDtos(collectionId2);
      assertThat(roleIds1).containsExactlyInAnyOrderElementsOf(roleIds2);
      assertThat(roleIds1)
          .containsExactlyInAnyOrder(
              new IdResponseDto(testGroupBRole.getId()), new IdResponseDto(userDemoRole.getId()));
    } finally {
      userIdentityCacheService.startScheduledSync();
    }
  }

  @Test
  public void cleanupAfterIdentitySyncRemovesLastManager() {
    final PlatformUserIdentityCache userIdentityCacheService = getUserIdentityCacheService();
    try {
      // given
      userIdentityCacheService.startScheduledSync();

      authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_APPLICATION);
      authorizationClient.addKermitUserWithoutAuthorizations();
      embeddedOptimizeExtension
          .getConfigurationService()
          .getAuthConfiguration()
          .getSuperUserIds()
          .add(DEFAULT_USERNAME);

      userIdentityCacheService.synchronizeIdentities();

      final String collectionId = collectionClient.createNewCollection(USER_KERMIT, USER_KERMIT);

      // when
      authorizationClient.revokeSingleResourceAuthorizationsForUser(
          USER_KERMIT, OPTIMIZE_APPLICATION_RESOURCE_ID, RESOURCE_TYPE_APPLICATION);

      userIdentityCacheService.synchronizeIdentities();

      // then
      List<CollectionRoleResponseDto> roles = collectionClient.getCollectionRoles(collectionId);
      assertThat(roles).isEmpty();
    } finally {
      userIdentityCacheService.startScheduledSync();
    }
  }

  @Test
  public void noCleanupOnOnEmptyIdentityCache() {
    // given
    PlatformUserIdentityCache userIdentityCacheService = getUserIdentityCacheService();
    final String userid = "testUser";
    authorizationClient.addUserAndGrantOptimizeAccess(userid);

    // synchronizing identities to make sure that the newly created identities are accessible in
    // optimize
    userIdentityCacheService.synchronizeIdentities();

    String collectionId = collectionClient.createNewCollection();
    CollectionRoleRequestDto roleDto =
        new CollectionRoleRequestDto(new UserDto(userid), RoleType.EDITOR);
    collectionClient.addRolesToCollection(collectionId, roleDto);

    final HttpRequest engineAuthorizationsRequest =
        request().withPath(engineIntegrationExtension.getEnginePath() + AUTHORIZATION_ENDPOINT);

    ClientAndServer engineMockServer = useAndGetEngineMockServer();

    engineMockServer
        .when(engineAuthorizationsRequest, Times.once())
        .respond(
            HttpResponse.response()
                .withStatusCode(Response.Status.OK.getStatusCode())
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody("[]"));

    // when
    userIdentityCacheService.synchronizeIdentities();

    // then
    List<CollectionRoleResponseDto> roles = collectionClient.getCollectionRoles(collectionId);
    assertThat(roles).extracting(CollectionRoleResponseDto::getId).contains(roleDto.getId());
    engineMockServer.verify(engineAuthorizationsRequest);

    collectionRoleCleanupServiceLogCapturer.assertContains(
        "Identity cache is empty, will thus not perform collection role cleanup.");
  }

  @ParameterizedTest
  @MethodSource("identitiesAndAuthorizationResponse")
  public void noCleanupOnIdentitySyncFailWithError(
      final IdentityWithMetadataResponseDto expectedIdentity, final ErrorResponseMock mockedResp) {
    // given
    PlatformUserIdentityCache userIdentityCacheService = getUserIdentityCacheService();

    switch (expectedIdentity.getType()) {
      case USER:
        authorizationClient.addUserAndGrantOptimizeAccess(expectedIdentity.getId());
        assertThat(userIdentityCacheService.getUserIdentityById(expectedIdentity.getId()))
            .isEmpty();
        break;
      case GROUP:
        authorizationClient.createGroupAndGrantOptimizeAccess(
            expectedIdentity.getId(), expectedIdentity.getId());
        assertThat(userIdentityCacheService.getGroupIdentityById(expectedIdentity.getId()))
            .isEmpty();
        break;
      default:
        throw new OptimizeIntegrationTestException(
            "Unsupported identity type: " + expectedIdentity.getType());
    }

    // synchronizing identities to make sure that the newly created identities are accessible in
    // optimize
    userIdentityCacheService.synchronizeIdentities();

    String collectionId = collectionClient.createNewCollection();
    CollectionRoleRequestDto roleDto =
        new CollectionRoleRequestDto(expectedIdentity, RoleType.EDITOR);
    collectionClient.addRolesToCollection(collectionId, roleDto);

    final HttpRequest engineAuthorizationsRequest =
        request().withPath(engineIntegrationExtension.getEnginePath() + AUTHORIZATION_ENDPOINT);

    ClientAndServer engineMockServer = useAndGetEngineMockServer();

    mockedResp.mock(engineAuthorizationsRequest, Times.once(), engineMockServer);

    // when
    assertThrows(Exception.class, userIdentityCacheService::synchronizeIdentities);

    // then
    List<CollectionRoleResponseDto> roles = collectionClient.getCollectionRoles(collectionId);

    assertThat(roles).extracting(CollectionRoleResponseDto::getId).contains(roleDto.getId());
    engineMockServer.verify(engineAuthorizationsRequest);
  }

  private PlatformUserIdentityCache getUserIdentityCacheService() {
    return embeddedOptimizeExtension.getUserIdentityCache();
  }

  private static Stream<IdentityWithMetadataResponseDto> identities() {
    return Stream.of(
        new UserDto(
            "testUser", DEFAULT_FIRSTNAME, DEFAULT_LASTNAME, "testUser" + DEFAULT_EMAIL_DOMAIN),
        new GroupDto(KERMIT_GROUP_NAME, KERMIT_GROUP_NAME, 0L));
  }

  @SuppressWarnings(SuppressionConstants.UNUSED)
  private static Stream<Arguments> identitiesAndAuthorizationResponse() {
    return identities()
        .flatMap(
            identity ->
                MockServerUtil.engineMockedErrorResponses()
                    .map(errorResponse -> Arguments.of(identity, errorResponse)));
  }
}
