/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.security;

import io.camunda.optimize.dto.optimize.RoleType;
import io.camunda.optimize.dto.optimize.query.collection.CollectionEntity;
import io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityType;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.dto.optimize.rest.AuthorizationType;
import io.camunda.optimize.service.db.reader.EntitiesReader;
import io.camunda.optimize.service.identity.AbstractIdentityService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthorizedEntitiesService {

  private final EntitiesReader entitiesReader;
  private final AbstractIdentityService identityService;
  private final ReportAuthorizationService reportAuthorizationService;

  public List<EntityResponseDto> getAuthorizedPrivateEntities(final String userId) {

    final List<CollectionEntity> collectionEntities;
    if (identityService.isSuperUserIdentity(userId)) {
      collectionEntities = entitiesReader.getAllPrivateEntities();
    } else {
      collectionEntities = entitiesReader.getAllPrivateEntitiesForOwnerId(userId);
    }

    RoleType roleForUser =
        identityService.getUserAuthorizations(userId).contains(AuthorizationType.ENTITY_EDITOR)
            ? RoleType.EDITOR
            : RoleType.VIEWER;
    return collectionEntities.stream()
        .map(
            collectionEntity ->
                Pair.of(collectionEntity, collectionEntity.toEntityDto(roleForUser)))
        .filter(
            collectionEntityAndEntityDto -> {
              final EntityResponseDto entityDto = collectionEntityAndEntityDto.getValue();
              if (entityDto.getEntityType().equals(EntityType.REPORT)) {
                return reportAuthorizationService.isAuthorizedToAccessReportDefinition(
                    userId, (ReportDefinitionDto) collectionEntityAndEntityDto.getKey());
              } else {
                return true;
              }
            })
        .map(Pair::getValue)
        .collect(Collectors.toList());
  }

  public List<EntityResponseDto> getAuthorizedCollectionEntities(
      final String userId, final String collectionId) {
    return entitiesReader.getAllEntitiesForCollection(collectionId).stream()
        // defaults to EDITOR, any authorization specific values in a collection have to be applied
        // in responsible service layer
        .map(
            collectionEntity ->
                Pair.of(collectionEntity, collectionEntity.toEntityDto(RoleType.EDITOR)))
        .filter(
            collectionEntityAndEntityDto -> {
              final EntityResponseDto entityDto = collectionEntityAndEntityDto.getValue();
              if (entityDto.getEntityType().equals(EntityType.REPORT)) {
                return reportAuthorizationService.isAuthorizedToAccessReportDefinition(
                    userId, (ReportDefinitionDto) collectionEntityAndEntityDto.getKey());
              } else {
                return true;
              }
            })
        .map(Pair::getValue)
        .collect(Collectors.toList());
  }
}
