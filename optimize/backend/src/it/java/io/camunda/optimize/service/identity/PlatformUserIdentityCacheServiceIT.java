/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.identity;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.util.importing.EngineConstants.AUTHORIZATION_ENDPOINT;
import static io.camunda.optimize.service.util.importing.EngineConstants.OPTIMIZE_APPLICATION_RESOURCE_ID;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_APPLICATION;
import static io.camunda.optimize.test.engine.AuthorizationClient.GROUP_ID;
import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_EMAIL_DOMAIN;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FIRSTNAME;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_LASTNAME;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.KERMIT_GROUP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.GroupDto;
import io.camunda.optimize.dto.optimize.UserDto;
import io.camunda.optimize.service.exceptions.MaxEntryLimitHitException;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.engine.UserIdentityCacheConfiguration;
import io.camunda.optimize.test.it.extension.ErrorResponseMock;
import io.camunda.optimize.test.it.extension.MockServerUtil;
import io.camunda.optimize.util.SuppressionConstants;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;

@Tag(OPENSEARCH_PASSING)
public class PlatformUserIdentityCacheServiceIT extends AbstractPlatformIT {

  @Test
  public void verifySyncEnabledByDefault() {
    assertThat(getUserIdentityCacheService().isScheduledToRun()).isTrue();
  }

  @Test
  public void testSyncStoppedSuccessfully() {
    try {
      getUserIdentityCacheService().stopScheduledSync();
      assertThat(getUserIdentityCacheService().isScheduledToRun()).isFalse();
    } finally {
      getUserIdentityCacheService().startScheduledSync();
    }
  }

  @Test
  public void testCacheReplacedOnNewSync() {
    try {
      // given
      getUserIdentityCacheService().stopScheduledSync();
      authorizationClient.addKermitUserAndGrantAccessToOptimize();
      getUserIdentityCacheService().synchronizeIdentities();

      // when
      final String userIdJohn = "john";
      authorizationClient.addUserAndGrantOptimizeAccess(userIdJohn);
      getUserIdentityCacheService().synchronizeIdentities();

      // then
      assertThat(getUserIdentityCacheService().getUserIdentityById(userIdJohn)).isPresent();
    } finally {
      getUserIdentityCacheService().startScheduledSync();
    }
  }

  @Test
  public void testCacheNotReplacedOnLimitHit() {
    try {
      // given
      getUserIdentityCacheService().stopScheduledSync();
      authorizationClient.addKermitUserAndGrantAccessToOptimize();
      getUserIdentityCacheService().synchronizeIdentities();

      // when
      final String userIdJohn = "john";
      authorizationClient.addUserAndGrantOptimizeAccess(userIdJohn);
      // we have at least two users, but limit is now 1
      getIdentitySyncConfiguration().setMaxEntryLimit(1L);

      // then
      final PlatformUserIdentityCache userIdentityCacheService = getUserIdentityCacheService();
      assertThatThrownBy(userIdentityCacheService::synchronizeIdentities)
          .isInstanceOf(MaxEntryLimitHitException.class);
      assertThat(getUserIdentityCacheService().getUserIdentityById(KERMIT_USER)).isPresent();
      assertThat(getUserIdentityCacheService().getUserIdentityById(userIdJohn)).isNotPresent();
    } finally {
      getUserIdentityCacheService().startScheduledSync();
    }
  }

  @Test
  public void testGrantedUserIsImportedMetaDataAvailable() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    final Optional<UserDto> userIdentityById =
        getUserIdentityCacheService().getUserIdentityById(KERMIT_USER);
    assertThat(userIdentityById).isPresent();
    assertThat(userIdentityById.get().getName())
        .isEqualTo(DEFAULT_FIRSTNAME + " " + DEFAULT_LASTNAME);
    assertThat(userIdentityById.get().getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
    assertThat(userIdentityById.get().getLastName()).isEqualTo(DEFAULT_LASTNAME);
    assertThat(userIdentityById.get().getEmail()).contains(DEFAULT_EMAIL_DOMAIN);
  }

  @Test
  public void testGrantedUserIsImportedMetaNotAvailableIfDisabled() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    getIdentitySyncConfiguration().setIncludeUserMetaData(false);

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    final Optional<UserDto> userIdentityById =
        getUserIdentityCacheService().getUserIdentityById(KERMIT_USER);
    assertThat(userIdentityById).isPresent();
    assertThat(userIdentityById.get().getName()).isEqualTo(KERMIT_USER);
    assertThat(userIdentityById.get().getFirstName()).isNull();
    assertThat(userIdentityById.get().getLastName()).isNull();
    assertThat(userIdentityById.get().getEmail()).isNull();
  }

  @Test
  public void testNotGrantedUserIsNotImported() {
    // given
    engineIntegrationExtension.addUser(KERMIT_USER, KERMIT_USER);

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getUserIdentityById(KERMIT_USER)).isNotPresent();
  }

  @Test
  public void testGrantedGroupIsImported() {
    // given
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    authorizationClient.grantKermitGroupOptimizeAccess();

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    final Optional<GroupDto> groupIdentityById =
        getUserIdentityCacheService().getGroupIdentityById(GROUP_ID);
    assertThat(groupIdentityById).isPresent();
    assertThat(groupIdentityById.get().getName()).isEqualTo(KERMIT_GROUP_NAME);
    assertThat(groupIdentityById.get().getMemberCount()).isEqualTo(1L);
  }

  @Test
  public void testNotGrantedGroupIsNotImported() {
    // given
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getGroupIdentityById(GROUP_ID)).isNotPresent();
  }

  @Test
  public void testGrantedGroupMemberIsImported() {
    // given
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    authorizationClient.grantKermitGroupOptimizeAccess();

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getUserIdentityById(KERMIT_USER)).isPresent();
  }

  @Test
  public void testNotGrantedGroupMemberIsNotImported() {
    // given
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getUserIdentityById(KERMIT_USER)).isNotPresent();
  }

  @Test
  public void testGrantedGroupMemberIsImportedAlthoughAlsoMemberOfNotGrantedGroup() {
    // given
    // https://docs.camunda.org/manual/7.11/user-guide/process-engine/authorization-service/#authorization-precedence
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    authorizationClient.grantKermitGroupOptimizeAccess();
    final String revokedGroupId = "revokedGroup";
    authorizationClient.createGroupAndAddUser(revokedGroupId, KERMIT_USER);
    authorizationClient.revokeSingleResourceAuthorizationsForGroup(
        revokedGroupId, OPTIMIZE_APPLICATION_RESOURCE_ID, RESOURCE_TYPE_APPLICATION);

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getUserIdentityById(KERMIT_USER)).isPresent();
  }

  @Test
  public void testRevokedUserIsNotImportedAlthoughMemberOfGrantedGroup() {
    // given
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    authorizationClient.grantKermitGroupOptimizeAccess();
    authorizationClient.revokeSingleResourceAuthorizationsForKermit(
        OPTIMIZE_APPLICATION_RESOURCE_ID, RESOURCE_TYPE_APPLICATION);

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getUserIdentityById(KERMIT_USER)).isNotPresent();
  }

  @Test
  public void testGlobalAuthNoExplicitGrantUserIsImported() {
    // given
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_APPLICATION);
    authorizationClient.addKermitUserWithoutAuthorizations();

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getUserIdentityById(KERMIT_USER)).isPresent();
  }

  @Test
  public void testGlobalAuthRevokedAuthUserIsNotImported() {
    // given
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_APPLICATION);
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.revokeSingleResourceAuthorizationsForKermit(
        OPTIMIZE_APPLICATION_RESOURCE_ID, RESOURCE_TYPE_APPLICATION);

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getUserIdentityById(KERMIT_USER)).isNotPresent();
  }

  @Test
  public void testGlobalAuthNoExplicitGrantGroupIsImported() {
    // given
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_APPLICATION);
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getGroupIdentityById(GROUP_ID)).isPresent();
  }

  @Test
  public void testGlobalAuthRevokedAuthGroupIsNotImported() {
    // given
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_APPLICATION);
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    authorizationClient.revokeSingleResourceAuthorizationsForKermitGroup(
        OPTIMIZE_APPLICATION_RESOURCE_ID, RESOURCE_TYPE_APPLICATION);

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getGroupIdentityById(GROUP_ID)).isNotPresent();
  }

  @Test
  public void testGlobalAuthRevokedAuthGroupMemberIsNotImported() {
    // given
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_APPLICATION);
    authorizationClient.addKermitUserWithoutAuthorizations();
    authorizationClient.createKermitGroupAndAddKermitToThatGroup();
    authorizationClient.revokeSingleResourceAuthorizationsForKermitGroup(
        OPTIMIZE_APPLICATION_RESOURCE_ID, RESOURCE_TYPE_APPLICATION);

    // when
    getUserIdentityCacheService().synchronizeIdentities();

    // then
    assertThat(getUserIdentityCacheService().getUserIdentityById(KERMIT_USER)).isNotPresent();
  }

  @ParameterizedTest
  @MethodSource("engineErrors")
  public void syncRetryBackoff(final ErrorResponseMock mockedResp) throws InterruptedException {
    // given
    ConfigurationService configurationService = embeddedOptimizeExtension.getConfigurationService();

    // any date, which is not sunday 00:00, so the cron is not triggered
    LocalDateUtil.setCurrentTime(OffsetDateTime.parse("1997-01-27T18:00:00+01:00"));
    configurationService.getUserIdentityCacheConfiguration().setCronTrigger("* 0 * * 0");
    embeddedOptimizeExtension.reloadConfiguration();

    final HttpRequest engineAuthorizationsRequest =
        request().withPath(engineIntegrationExtension.getEnginePath() + AUTHORIZATION_ENDPOINT);

    ClientAndServer engineMockServer = useAndGetEngineMockServer();

    mockedResp.mock(engineAuthorizationsRequest, Times.unlimited(), engineMockServer);

    final ScheduledExecutorService identitySyncThread =
        Executors.newSingleThreadScheduledExecutor();

    // when
    try {
      identitySyncThread.execute(getUserIdentityCacheService()::syncIdentitiesWithRetry);
      Thread.sleep(1000);
      engineMockServer.verify(engineAuthorizationsRequest);

      engineMockServer.clear(engineAuthorizationsRequest);
    } finally {
      identitySyncThread.shutdown();
    }

    // then
    boolean termination = identitySyncThread.awaitTermination(30, TimeUnit.SECONDS);
    assertThat(termination).isTrue();
    engineMockServer.verify(engineAuthorizationsRequest);
  }

  private PlatformUserIdentityCache getUserIdentityCacheService() {
    return embeddedOptimizeExtension.getUserIdentityCache();
  }

  private UserIdentityCacheConfiguration getIdentitySyncConfiguration() {
    return embeddedOptimizeExtension.getConfigurationService().getUserIdentityCacheConfiguration();
  }

  @SuppressWarnings(SuppressionConstants.UNUSED)
  private static Stream<ErrorResponseMock> engineErrors() {
    return MockServerUtil.engineMockedErrorResponses();
  }
}
