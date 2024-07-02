/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.security.util.definition;

import io.camunda.optimize.dto.optimize.DefinitionOptimizeResponseDto;
import io.camunda.optimize.dto.optimize.DefinitionType;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.SimpleDefinitionDto;
import io.camunda.optimize.dto.optimize.TenantDto;
import java.util.List;
import java.util.Set;

public interface DataSourceDefinitionAuthorizationService {

  default boolean isAuthorizedToAccessDefinition(
      final String userId,
      final DefinitionType definitionType,
      final String definitionKey,
      final List<String> tenantIds) {
    return isAuthorizedToAccessDefinition(
        userId, IdentityType.USER, definitionKey, definitionType, tenantIds);
  }

  boolean isAuthorizedToAccessDefinition(
      final String identityId,
      final IdentityType identityType,
      final String definitionKey,
      final DefinitionType definitionType,
      final List<String> tenantIds);

  List<TenantDto> resolveAuthorizedTenantsForProcess(
      final String userId,
      final SimpleDefinitionDto definitionDto,
      final List<String> tenantIds,
      final Set<String> engines);

  boolean isAuthorizedToAccessDefinition(
      final String userId, final String tenantId, final SimpleDefinitionDto definition);

  <T extends DefinitionOptimizeResponseDto> boolean isAuthorizedToAccessDefinition(
      final String userId, final T definition);
}
