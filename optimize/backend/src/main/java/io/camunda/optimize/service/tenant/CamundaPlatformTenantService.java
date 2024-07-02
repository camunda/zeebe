/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.tenant;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.TenantDto;
import io.camunda.optimize.service.db.reader.TenantReader;
import io.camunda.optimize.service.security.util.tenant.DataSourceTenantAuthorizationService;
import io.camunda.optimize.service.util.configuration.CacheConfiguration;
import io.camunda.optimize.service.util.configuration.ConfigurationReloadable;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.condition.CamundaPlatformCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(CamundaPlatformCondition.class)
public class CamundaPlatformTenantService implements TenantService, ConfigurationReloadable {

  public static final TenantDto TENANT_NOT_DEFINED = new TenantDto(null, "Not defined", null);

  private final TenantReader tenantReader;
  private final DataSourceTenantAuthorizationService tenantAuthorizationService;
  private final ConfigurationService configurationService;
  private final LoadingCache<String, List<TenantDto>> tenantsReadCache;
  private List<TenantDto> configuredDefaultTenants;

  public CamundaPlatformTenantService(
      final TenantReader tenantReader,
      final DataSourceTenantAuthorizationService tenantAuthorizationService,
      final ConfigurationService configurationService) {
    this.tenantReader = tenantReader;
    this.tenantAuthorizationService = tenantAuthorizationService;
    this.configurationService = configurationService;

    initDefaultTenants();

    // this cache serves the purpose to reduce the frequency an actual read is triggered
    // as the tenant data is not changing very frequently the caching is a tradeoff to
    // reduce the latency of processing requests where multiple authorization checks are done in a
    // short amount of time
    // (mostly listing endpoints for reports and process/decision definitions)
    final CacheConfiguration tenantCacheConfiguration =
        configurationService.getCaches().getTenants();
    tenantsReadCache =
        Caffeine.newBuilder()
            // as the cache holds only one entry being the global list of all tenants
            .maximumSize(1)
            .expireAfterWrite(tenantCacheConfiguration.getDefaultTtlMillis(), TimeUnit.MILLISECONDS)
            .build(key -> fetchTenants());
  }

  @Override
  public boolean isAuthorizedToSeeTenant(final String userId, final String tenantId) {
    return tenantAuthorizationService.isAuthorizedToSeeTenant(userId, IdentityType.USER, tenantId);
  }

  @Override
  public boolean isMultiTenantEnvironment() {
    return getAvailableTenants().size() > 1;
  }

  @Override
  public List<TenantDto> getTenantsForUser(final String userId) {
    return getAvailableTenants().stream()
        .filter(
            tenantDto ->
                tenantAuthorizationService.isAuthorizedToSeeTenant(
                    userId, IdentityType.USER, tenantDto.getId(), tenantDto.getEngine()))
        .toList();
  }

  public List<TenantDto> getTenantsByEngine(final String engineAlias) {
    return getAvailableTenants().stream()
        .filter(
            tenantDto ->
                tenantDto.equals(TENANT_NOT_DEFINED) || tenantDto.getEngine().equals(engineAlias))
        .toList();
  }

  public List<TenantDto> getAvailableTenants() {
    return tenantsReadCache.get("getTenants");
  }

  private List<TenantDto> fetchTenants() {
    final List<TenantDto> tenants = new ArrayList<>(configuredDefaultTenants);
    tenants.addAll(tenantReader.getTenants());
    return tenants;
  }

  private void initDefaultTenants() {
    this.configuredDefaultTenants =
        Stream.concat(
                Stream.of(TENANT_NOT_DEFINED),
                configurationService.getConfiguredEngines().entrySet().stream()
                    .filter(entry -> entry.getValue().getDefaultTenantId().isPresent())
                    .map(
                        entry -> {
                          final String tenantId = entry.getValue().getDefaultTenantId().get();
                          return new TenantDto(
                              tenantId,
                              entry.getValue().getDefaultTenantName().orElse(tenantId),
                              entry.getKey());
                        }))
            .collect(ImmutableList.toImmutableList());
  }

  @Override
  public void reloadConfiguration(final ApplicationContext context) {
    initDefaultTenants();
    tenantsReadCache.invalidateAll();
  }
}
