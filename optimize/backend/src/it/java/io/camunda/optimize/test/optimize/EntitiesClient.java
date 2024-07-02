/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.test.optimize;

import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.rest.constants.RestConstants.X_OPTIMIZE_CLIENT_LOCALE;

import io.camunda.optimize.OptimizeRequestExecutor;
import io.camunda.optimize.dto.optimize.query.entity.EntitiesDeleteRequestDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityNameRequestDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityNameResponseDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto;
import io.camunda.optimize.dto.optimize.rest.sorting.EntitySorter;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EntitiesClient {
  private final Supplier<OptimizeRequestExecutor> requestExecutorSupplier;

  public List<EntityResponseDto> getAllEntities() {
    return getAllEntities(null);
  }

  public List<EntityResponseDto> getAllEntities(EntitySorter sorter) {
    return getAllEntitiesAsUser(DEFAULT_USERNAME, DEFAULT_PASSWORD, sorter);
  }

  public List<EntityResponseDto> getAllEntitiesAsUser(String username, String password) {
    return getAllEntitiesAsUser(username, password, null);
  }

  private List<EntityResponseDto> getAllEntitiesAsUser(
      String username, String password, final EntitySorter sorter) {
    return getRequestExecutor()
        .buildGetAllEntitiesRequest(sorter)
        .withUserAuthentication(username, password)
        .executeAndReturnList(EntityResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public EntityNameResponseDto getEntityNames(
      String collectionId, String dashboardId, String reportId, String eventProcessId) {
    return getEntityNamesAsUser(
        collectionId, dashboardId, reportId, eventProcessId, DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }

  public EntityNameResponseDto getEntityNamesLocalized(
      final String collectionId,
      final String dashboardId,
      final String reportId,
      final String eventProcessId,
      final String locale) {
    return getEntityNamesAsUser(
        collectionId,
        dashboardId,
        reportId,
        eventProcessId,
        DEFAULT_USERNAME,
        DEFAULT_PASSWORD,
        locale);
  }

  public EntityNameResponseDto getEntityNamesAsUser(
      String collectionId,
      String dashboardId,
      String reportId,
      String eventProcessId,
      String username,
      String password) {
    return getEntityNamesAsUser(
        collectionId, dashboardId, reportId, eventProcessId, username, password, null);
  }

  private EntityNameResponseDto getEntityNamesAsUser(
      final String collectionId,
      final String dashboardId,
      final String reportId,
      final String eventProcessId,
      final String username,
      final String password,
      final String locale) {
    final OptimizeRequestExecutor requestExecutor = getRequestExecutor();
    Optional.ofNullable(locale)
        .ifPresent(loc -> requestExecutor.addSingleHeader(X_OPTIMIZE_CLIENT_LOCALE, loc));
    return requestExecutor
        .withUserAuthentication(username, password)
        .buildGetEntityNamesRequest(
            new EntityNameRequestDto(collectionId, dashboardId, reportId, eventProcessId))
        .execute(EntityNameResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public boolean entitiesHaveDeleteConflicts(EntitiesDeleteRequestDto entitiesDeleteRequestDto) {
    return getRequestExecutor()
        .buildCheckEntityDeleteConflictsRequest(entitiesDeleteRequestDto)
        .execute(Boolean.class, Response.Status.OK.getStatusCode());
  }

  private OptimizeRequestExecutor getRequestExecutor() {
    return requestExecutorSupplier.get();
  }
}
