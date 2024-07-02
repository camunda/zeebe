/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.service.db.DatabaseConstants.TENANT_INDEX_NAME;
import static io.camunda.optimize.service.util.importing.EngineConstants.ALL_PERMISSION;
import static io.camunda.optimize.service.util.importing.EngineConstants.AUTHORIZATION_TYPE_GRANT;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.engine.AuthorizationDto;
import io.camunda.optimize.dto.optimize.TenantDto;
import java.util.Collections;

public abstract class AbstractDefinitionRestServiceIT extends AbstractPlatformIT {

  protected static final String VERSION_TAG = "aVersionTag";
  protected static final String EXPECTED_DEFINITION_NOT_FOUND_MESSAGE = "Could not find xml for";

  protected abstract int getDefinitionResourceType();

  protected void grantSingleDefinitionAuthorizationsForUser(
      final String userId, final String definitionKey) {
    AuthorizationDto authorizationDto = new AuthorizationDto();
    authorizationDto.setResourceType(getDefinitionResourceType());
    authorizationDto.setPermissions(Collections.singletonList(ALL_PERMISSION));
    authorizationDto.setResourceId(definitionKey);
    authorizationDto.setType(AUTHORIZATION_TYPE_GRANT);
    authorizationDto.setUserId(userId);
    engineIntegrationExtension.createAuthorization(authorizationDto);
  }

  protected void createTenant(final TenantDto tenantDto) {
    databaseIntegrationTestExtension.addEntryToDatabase(
        TENANT_INDEX_NAME, tenantDto.getId(), tenantDto);
  }
}
