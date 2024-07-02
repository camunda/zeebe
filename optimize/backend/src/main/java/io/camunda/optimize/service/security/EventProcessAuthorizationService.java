/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.security;

import io.camunda.optimize.dto.optimize.IdentityDto;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessRoleRequestDto;
import io.camunda.optimize.service.EventProcessRoleService;
import io.camunda.optimize.service.identity.AbstractIdentityService;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class EventProcessAuthorizationService {

  private final ConfigurationService configurationService;
  private final EventProcessRoleService eventProcessRoleService;
  private final AbstractIdentityService identityService;

  public boolean hasEventProcessManagementAccess(@NonNull final String userId) {
    return configurationService.getEventBasedProcessAccessUserIds().contains(userId)
        || isInGroupWithEventProcessManagementAccess(userId);
  }

  /**
   * Validate whether a user is authorized to access an event process.
   *
   * @param userId to check for authorization
   * @param eventProcessMappingId the id of the event process
   * @return {@code Optional.empty} if no event process for that id exists, otherwise the
   *     authorization result.
   */
  public Optional<Boolean> isAuthorizedToEventProcess(
      @NonNull final String userId, @NonNull final String eventProcessMappingId) {
    final List<EventProcessRoleRequestDto<IdentityDto>> roles =
        eventProcessRoleService.getRoles(eventProcessMappingId);
    if (!roles.isEmpty()) {
      boolean isAuthorized = false;
      final Map<IdentityType, Set<String>> groupAndUserRoleIdentityIds =
          roles.stream()
              .map(EventProcessRoleRequestDto::getIdentity)
              .collect(
                  Collectors.groupingBy(
                      IdentityDto::getType,
                      Collectors.mapping(IdentityDto::getId, Collectors.toSet())));
      final Set<String> roleGroupIds =
          groupAndUserRoleIdentityIds.getOrDefault(IdentityType.GROUP, Collections.emptySet());
      final Set<String> roleUserIds =
          groupAndUserRoleIdentityIds.getOrDefault(IdentityType.USER, Collections.emptySet());

      if (!roleGroupIds.isEmpty()) {
        // if there are groups check if the user is member of those
        final Set<String> allGroupIdsOfUser =
            identityService.getAllGroupsOfUser(userId).stream()
                .map(IdentityDto::getId)
                .collect(Collectors.toSet());
        isAuthorized = allGroupIdsOfUser.stream().anyMatch(roleGroupIds::contains);
      }

      if (!isAuthorized) {
        // if not authorized yet check the user roles
        isAuthorized = roleUserIds.stream().anyMatch(userId::equals);
      }

      return Optional.of(isAuthorized);
    } else {
      return Optional.empty();
    }
  }

  private boolean isInGroupWithEventProcessManagementAccess(@NonNull final String userId) {
    final List<String> authorizedGroupIds =
        configurationService.getEventBasedProcessAccessGroupIds();
    return identityService.getAllGroupsOfUser(userId).stream()
        .map(IdentityDto::getId)
        .anyMatch(authorizedGroupIds::contains);
  }
}
