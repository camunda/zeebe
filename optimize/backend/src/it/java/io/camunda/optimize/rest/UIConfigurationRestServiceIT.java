/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.db.DatabaseConstants.TENANT_INDEX_NAME;
import static io.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension.DEFAULT_ENGINE_ALIAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.TenantDto;
import io.camunda.optimize.dto.optimize.query.ui_configuration.UIConfigurationResponseDto;
import io.camunda.optimize.dto.optimize.query.ui_configuration.WebappsEndpointDto;
import io.camunda.optimize.service.metadata.Version;
import io.camunda.optimize.service.util.configuration.OnboardingConfiguration;
import io.camunda.optimize.service.util.configuration.OptimizeProfile;
import io.camunda.optimize.service.util.configuration.WebhookConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class UIConfigurationRestServiceIT extends AbstractPlatformIT {

  @Test
  public void logoutHidden() {
    // given
    embeddedOptimizeExtension.getConfigurationService().getUiConfiguration().setLogoutHidden(true);

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.isLogoutHidden()).isTrue();
  }

  @Test
  public void logoutVisible() {
    // given
    embeddedOptimizeExtension.getConfigurationService().getUiConfiguration().setLogoutHidden(false);

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.isLogoutHidden()).isFalse();
  }

  @Test
  public void sharingEnabled() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setSharingEnabled(true);

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.isSharingEnabled()).isTrue();
  }

  @Test
  public void sharingDisabled() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setSharingEnabled(false);

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.isSharingEnabled()).isFalse();
  }

  @Test
  public void getDefaultCamundaWebappsEndpoint() {
    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    final Map<String, WebappsEndpointDto> webappsEndpoints = response.getWebappsEndpoints();
    assertThat(webappsEndpoints).isNotEmpty();
    final WebappsEndpointDto defaultEndpoint = webappsEndpoints.get(DEFAULT_ENGINE_ALIAS);
    assertThat(defaultEndpoint).isNotNull();
    assertThat(defaultEndpoint.getEndpoint()).isEqualTo("http://localhost:8080/camunda");
    assertThat(defaultEndpoint.getEngineName())
        .isEqualTo(engineIntegrationExtension.getEngineName());
  }

  @Test
  public void getCustomCamundaWebappsEndpoint() {
    // given
    setWebappsEndpoint("foo");

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    final Map<String, WebappsEndpointDto> webappsEndpoints = response.getWebappsEndpoints();
    assertThat(webappsEndpoints).isNotEmpty();
    final WebappsEndpointDto defaultEndpoint = webappsEndpoints.get(DEFAULT_ENGINE_ALIAS);
    assertThat(defaultEndpoint).isNotNull();
    assertThat(defaultEndpoint.getEndpoint()).isEqualTo("foo");
    assertThat(defaultEndpoint.getEngineName())
        .isEqualTo(engineIntegrationExtension.getEngineName());
  }

  @Test
  public void disableWebappsEndpointReturnsEmptyEndpoint() {
    // given
    setWebappsEnabled(false);

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    final Map<String, WebappsEndpointDto> webappsEndpoints = response.getWebappsEndpoints();
    assertThat(webappsEndpoints).isNotEmpty();
    final WebappsEndpointDto defaultEndpoint = webappsEndpoints.get(DEFAULT_ENGINE_ALIAS);
    assertThat(defaultEndpoint).isNotNull();
    assertThat(defaultEndpoint.getEndpoint()).isEmpty();
  }

  @Test
  public void emailNotificationIsEnabled() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setEmailEnabled(true);

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.isEmailEnabled()).isTrue();
  }

  @Test
  public void emailNotificationIsDisabled() {
    // given
    embeddedOptimizeExtension.getConfigurationService().setEmailEnabled(false);

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.isEmailEnabled()).isFalse();
  }

  @Test
  public void getOptimizeVersion() {
    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.getOptimizeVersion()).isEqualTo(Version.RAW_VERSION);
    assertThat(response.getOptimizeDocsVersion()).isEqualTo(Version.VERSION);
  }

  @Test
  public void getWebhooks() {
    // given
    final String webhook1Name = "webhook1";
    final String webhook2Name = "webhook2";
    final Map<String, WebhookConfiguration> webhookMap =
        uiConfigurationClient.createSimpleWebhookConfigurationMap(
            Sets.newHashSet(webhook2Name, webhook1Name));
    embeddedOptimizeExtension.getConfigurationService().setConfiguredWebhooks(webhookMap);

    // when
    final List<String> allWebhooks = uiConfigurationClient.getUIConfiguration().getWebhooks();

    // then
    assertThat(allWebhooks).containsExactly(webhook1Name, webhook2Name);
  }

  @Test
  public void tenantsAvailable_oneTenant() {
    // given
    createTenant("tenant1");

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.isTenantsAvailable()).isTrue();
  }

  @Test
  public void tenantsAvailable_noTenants() {
    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.isTenantsAvailable()).isFalse();
  }

  @Test
  public void getDefaultOptimizeProfile() {
    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.getOptimizeProfile()).isEqualTo(OptimizeProfile.PLATFORM);
  }

  @Test
  public void getIsEnterpriseMode() {
    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.isEnterpriseMode()).isTrue();
  }

  @Test
  public void getMixpanelConfiguration() {
    // given
    embeddedOptimizeExtension.getConfigurationService().getAnalytics().setEnabled(true);
    final String testToken = "testToken";
    final String apiHost = "apiHost";
    final String organizationId = "orgId";
    final String scriptUrl = "test";
    final String stage = "IT";
    final String clusterId = "IT-cluster";
    embeddedOptimizeExtension
        .getConfigurationService()
        .getAnalytics()
        .getMixpanel()
        .setApiHost(apiHost);
    embeddedOptimizeExtension
        .getConfigurationService()
        .getAnalytics()
        .getMixpanel()
        .setToken(testToken);
    embeddedOptimizeExtension
        .getConfigurationService()
        .getAnalytics()
        .getMixpanel()
        .getProperties()
        .setOrganizationId(organizationId);
    embeddedOptimizeExtension
        .getConfigurationService()
        .getAnalytics()
        .getMixpanel()
        .getProperties()
        .setStage(stage);
    embeddedOptimizeExtension
        .getConfigurationService()
        .getAnalytics()
        .getMixpanel()
        .getProperties()
        .setClusterId(clusterId);
    embeddedOptimizeExtension
        .getConfigurationService()
        .getAnalytics()
        .getOsano()
        .setScriptUrl(scriptUrl);

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.getMixpanel().isEnabled()).isTrue();
    assertThat(response.getMixpanel().getApiHost()).isEqualTo(apiHost);
    assertThat(response.getMixpanel().getToken()).isEqualTo(testToken);
    assertThat(response.getMixpanel().getOrganizationId()).isEqualTo(organizationId);
    assertThat(response.getMixpanel().getOsanoScriptUrl()).isEqualTo(scriptUrl);
    assertThat(response.getMixpanel().getStage()).isEqualTo(stage);
    assertThat(response.getMixpanel().getClusterId()).isEqualTo(clusterId);
  }

  @Test
  public void getOnboardingConfiguration() {
    // given
    embeddedOptimizeExtension.getConfigurationService().getOnboarding().setEnabled(true);
    final String scriptUrl = "test";
    embeddedOptimizeExtension
        .getConfigurationService()
        .getOnboarding()
        .setAppCuesScriptUrl(scriptUrl);
    final String clusterId = "clusterId1";
    final String orgId = "orgId1";
    embeddedOptimizeExtension
        .getConfigurationService()
        .getOnboarding()
        .setProperties(new OnboardingConfiguration.Properties(orgId, clusterId));

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.getOnboarding().isEnabled()).isEqualTo(true);
    assertThat(response.getOnboarding().getAppCuesScriptUrl()).isEqualTo(scriptUrl);
    assertThat(response.getOnboarding().getOrgId()).isEqualTo(orgId);
    assertThat(response.getOnboarding().getClusterId()).isEqualTo(clusterId);
  }

  @Test
  public void databaseModeIsReturnedCorrectly() {
    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.getOptimizeDatabase())
        .isEqualTo(databaseIntegrationTestExtension.getDatabaseVendor());
  }

  @Test
  public void maxNumDataSourceIsReturnedCorrectly() {
    // given
    embeddedOptimizeExtension
        .getConfigurationService()
        .getUiConfiguration()
        .setMaxNumDataSourcesForReport(50);

    // when
    final UIConfigurationResponseDto response = uiConfigurationClient.getUIConfiguration();

    // then
    assertThat(response.getMaxNumDataSourcesForReport()).isEqualTo(50);
  }

  protected void createTenant(final String tenantId) {
    final TenantDto tenantDto = new TenantDto(tenantId, tenantId, DEFAULT_ENGINE_ALIAS);
    databaseIntegrationTestExtension.addEntryToDatabase(TENANT_INDEX_NAME, tenantId, tenantDto);
  }

  private void setWebappsEndpoint(final String webappsEndpoint) {
    embeddedOptimizeExtension
        .getConfigurationService()
        .getConfiguredEngines()
        .get(DEFAULT_ENGINE_ALIAS)
        .getWebapps()
        .setEndpoint(webappsEndpoint);
  }

  private void setWebappsEnabled(final boolean enabled) {
    embeddedOptimizeExtension
        .getConfigurationService()
        .getConfiguredEngines()
        .get(DEFAULT_ENGINE_ALIAS)
        .getWebapps()
        .setEnabled(enabled);
  }
}
