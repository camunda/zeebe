/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.engine;

import static io.camunda.optimize.dto.optimize.IdentityType.GROUP;
import static io.camunda.optimize.dto.optimize.IdentityType.USER;
import static io.camunda.optimize.service.importing.engine.fetcher.EngineEntityFetcher.UTF8;
import static io.camunda.optimize.service.util.importing.EngineConstants.ALL_RESOURCES_RESOURCE_ID;
import static io.camunda.optimize.service.util.importing.EngineConstants.AUTHORIZATION_ENDPOINT;
import static io.camunda.optimize.service.util.importing.EngineConstants.AUTHORIZATION_TYPE_GLOBAL;
import static io.camunda.optimize.service.util.importing.EngineConstants.AUTHORIZATION_TYPE_GRANT;
import static io.camunda.optimize.service.util.importing.EngineConstants.AUTHORIZATION_TYPE_REVOKE;
import static io.camunda.optimize.service.util.importing.EngineConstants.DECISION_DEFINITION_ENDPOINT_TEMPLATE;
import static io.camunda.optimize.service.util.importing.EngineConstants.GROUP_BY_ID_ENDPOINT_TEMPLATE;
import static io.camunda.optimize.service.util.importing.EngineConstants.GROUP_ENDPOINT;
import static io.camunda.optimize.service.util.importing.EngineConstants.GROUP_ID_IN;
import static io.camunda.optimize.service.util.importing.EngineConstants.INDEX_OF_FIRST_RESULT;
import static io.camunda.optimize.service.util.importing.EngineConstants.MAX_RESULTS_TO_RETURN;
import static io.camunda.optimize.service.util.importing.EngineConstants.MEMBER;
import static io.camunda.optimize.service.util.importing.EngineConstants.MEMBER_OF_GROUP;
import static io.camunda.optimize.service.util.importing.EngineConstants.OPTIMIZE_APPLICATION_RESOURCE_ID;
import static io.camunda.optimize.service.util.importing.EngineConstants.PROCESS_DEFINITION_ENDPOINT_TEMPLATE;
import static io.camunda.optimize.service.util.importing.EngineConstants.PROCESS_INSTANCE_ENDPOINT_TEMPLATE;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_APPLICATION;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_DECISION_DEFINITION;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_GROUP;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_PROCESS_DEFINITION;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_TENANT;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_USER;
import static io.camunda.optimize.service.util.importing.EngineConstants.SORT_BY;
import static io.camunda.optimize.service.util.importing.EngineConstants.SORT_ORDER;
import static io.camunda.optimize.service.util.importing.EngineConstants.SORT_ORDER_ASC;
import static io.camunda.optimize.service.util.importing.EngineConstants.USER_BY_ID_ENDPOINT_TEMPLATE;
import static io.camunda.optimize.service.util.importing.EngineConstants.USER_COUNT_ENDPOINT;
import static io.camunda.optimize.service.util.importing.EngineConstants.USER_ENDPOINT;
import static io.camunda.optimize.service.util.importing.EngineConstants.USER_ID_IN;

import io.camunda.optimize.dto.engine.AuthorizationDto;
import io.camunda.optimize.dto.engine.CountDto;
import io.camunda.optimize.dto.engine.EngineGroupDto;
import io.camunda.optimize.dto.engine.EngineListUserDto;
import io.camunda.optimize.dto.engine.HistoricProcessInstanceDto;
import io.camunda.optimize.dto.engine.definition.DecisionDefinitionEngineDto;
import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import io.camunda.optimize.dto.optimize.DecisionDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.GroupDto;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.UserDto;
import io.camunda.optimize.dto.optimize.datasource.EngineDataSourceDto;
import io.camunda.optimize.service.exceptions.OptimizeDecisionDefinitionFetchException;
import io.camunda.optimize.service.exceptions.OptimizeDecisionDefinitionNotFoundException;
import io.camunda.optimize.service.exceptions.OptimizeProcessDefinitionFetchException;
import io.camunda.optimize.service.exceptions.OptimizeProcessDefinitionNotFoundException;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.EngineVersionChecker;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class EngineContext {
  private static final Set<String> OPTIMIZE_APPLICATION_AUTH_RESOURCE_IDS =
      Set.of(ALL_RESOURCES_RESOURCE_ID, OPTIMIZE_APPLICATION_RESOURCE_ID);
  private final String engineAlias;
  private final Client engineClient;
  private final ConfigurationService configurationService;

  private boolean versionValidated;

  public EngineContext(
      final String engineAlias,
      final Client engineClient,
      final ConfigurationService configurationService) {
    this.engineAlias = engineAlias;
    this.engineClient = engineClient;
    this.configurationService = configurationService;
  }

  public Client getEngineClient() {
    if (!versionValidated) {
      try {
        EngineVersionChecker.checkEngineVersionSupport(
            engineClient,
            configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()));
        this.versionValidated = true;
      } catch (Exception e) {
        log.error(
            "Failed to validate engine {} version with error message: {}",
            getEngineAlias(),
            e.getMessage(),
            e);
        throw e;
      }
    }
    return engineClient;
  }

  public void close() {
    this.engineClient.close();
  }

  public String getEngineAlias() {
    return engineAlias;
  }

  public Optional<String> getDefaultTenantId() {
    return configurationService.getEngineDefaultTenantIdOfCustomEngine(engineAlias);
  }

  public AuthorizedIdentitiesResult getApplicationAuthorizedIdentities() {
    final AuthorizedIdentitiesResult authorizedIdentitiesResult = new AuthorizedIdentitiesResult();
    final List<AuthorizationDto> optimizeGrantAndRevokeAuthorizations =
        getAllApplicationAuthorizations().stream()
            .filter(
                authorizationDto ->
                    OPTIMIZE_APPLICATION_AUTH_RESOURCE_IDS.contains(
                        authorizationDto.getResourceId()))
            .peek(
                authorizationDto -> {
                  if (AUTHORIZATION_TYPE_GLOBAL == authorizationDto.getType()) {
                    authorizedIdentitiesResult.setGlobalOptimizeGrant(true);
                  }
                })
            .filter(authorizationDto -> AUTHORIZATION_TYPE_GLOBAL != authorizationDto.getType())
            .toList();

    optimizeGrantAndRevokeAuthorizations.stream()
        .sorted(
            // as users authorization win over group authorizations, we order by having group first
            Comparator.comparing(
                    AuthorizationDto::getGroupId, Comparator.nullsLast(Comparator.naturalOrder()))
                // as revokes win over grants we have them last (AUTHORIZATION_TYPE_GRANT=1 <
                // AUTHORIZATION_TYPE_REVOKE=2)
                .thenComparing(AuthorizationDto::getType))
        .forEach(
            authorizationDto -> {
              switch (authorizationDto.getType()) {
                case AUTHORIZATION_TYPE_GRANT:
                  if (StringUtils.isNotEmpty(authorizationDto.getGroupId())) {
                    authorizedIdentitiesResult
                        .getGrantedGroupIds()
                        .add(authorizationDto.getGroupId());
                  } else if (StringUtils.isNotEmpty(authorizationDto.getUserId())) {
                    authorizedIdentitiesResult
                        .getGrantedUserIds()
                        .add(authorizationDto.getUserId());
                  }
                  break;
                case AUTHORIZATION_TYPE_REVOKE:
                  if (StringUtils.isNotEmpty(authorizationDto.getGroupId())) {
                    authorizedIdentitiesResult
                        .getRevokedGroupIds()
                        .add(authorizationDto.getGroupId());
                  } else if (StringUtils.isNotEmpty(authorizationDto.getUserId())) {
                    authorizedIdentitiesResult
                        .getRevokedUserIds()
                        .add(authorizationDto.getUserId());
                  }
                  break;
                default:
                  throw new OptimizeRuntimeException(
                      "Unexpected authorization type:" + authorizationDto.getType());
              }
            });
    return authorizedIdentitiesResult;
  }

  public Optional<UserDto> getUserById(final String userId) {
    EngineListUserDto engineUserDto = null;
    try {
      Response response =
          getEngineClient()
              .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
              .path(USER_BY_ID_ENDPOINT_TEMPLATE)
              .resolveTemplate("id", userId)
              .request(MediaType.APPLICATION_JSON)
              .get();
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        engineUserDto = response.readEntity(EngineListUserDto.class);
      }
      response.close();
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch user with id [%s] from engine with alias [%s]",
              userId, getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
    return Optional.ofNullable(engineUserDto).map(this::mapEngineUser);
  }

  public DecisionDefinitionOptimizeDto fetchDecisionDefinition(final String decisionDefinitionId) {
    final Response response =
        getEngineClient()
            .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
            .path(DECISION_DEFINITION_ENDPOINT_TEMPLATE)
            .resolveTemplate("id", decisionDefinitionId)
            .request(MediaType.APPLICATION_JSON)
            .acceptEncoding(UTF8)
            .get();

    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
      final DecisionDefinitionEngineDto decisionDefinitionEngineDto =
          response.readEntity(DecisionDefinitionEngineDto.class);
      return mapToOptimizeDecisionDefinition(decisionDefinitionEngineDto);
    } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
      final String message =
          String.format(
              "Wasn't able to retrieve decision definition with id [%s] from the engine. It's likely that the definition "
                  + "has been deleted but the historic data for it is still available. Please make sure that there are no "
                  + "remnants of historic decision instances for that definition left! Response from the engine with alias %s: \n%s",
              decisionDefinitionId, getEngineAlias(), response.readEntity(String.class));
      throw new OptimizeDecisionDefinitionNotFoundException(message);
    } else {
      final String message =
          String.format(
              "Wasn't able to retrieve decision definition with id [%s] from the engine. Maybe the Optimize user utilized "
                  + "for the import is not authorized or there are some issues with the internet connection? Response from the "
                  + "engine with alias %s: \n%s",
              decisionDefinitionId, getEngineAlias(), response.readEntity(String.class));
      throw new OptimizeDecisionDefinitionFetchException(message);
    }
  }

  private DecisionDefinitionOptimizeDto mapToOptimizeDecisionDefinition(
      final DecisionDefinitionEngineDto engineDto) {
    return DecisionDefinitionOptimizeDto.builder()
        .id(engineDto.getId())
        .key(engineDto.getKey())
        .version(engineDto.getVersionAsString())
        .versionTag(engineDto.getVersionTag())
        .name(engineDto.getName())
        .dataSource(new EngineDataSourceDto(this.getEngineAlias()))
        .tenantId(engineDto.getTenantId().orElseGet(() -> this.getDefaultTenantId().orElse(null)))
        .build();
  }

  public ProcessDefinitionOptimizeDto fetchProcessDefinition(final String processDefinitionId) {
    final Response response =
        getEngineClient()
            .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
            .path(PROCESS_DEFINITION_ENDPOINT_TEMPLATE)
            .resolveTemplate("id", processDefinitionId)
            .request(MediaType.APPLICATION_JSON)
            .acceptEncoding(UTF8)
            .get();

    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
      final ProcessDefinitionEngineDto processDefinitionEngineDto =
          response.readEntity(ProcessDefinitionEngineDto.class);
      return mapToOptimizeProcessDefinition(processDefinitionEngineDto);
    } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
      final String message =
          String.format(
              "Wasn't able to retrieve process definition with id [%s] from the engine. It's likely that the definition "
                  + "has been deleted but the historic data for it is still available. Please make sure that there are no "
                  + "remnants of historic process instances for that definition left! Response from the engine with alias %s: \n%s",
              processDefinitionId, getEngineAlias(), response.readEntity(String.class));
      throw new OptimizeProcessDefinitionNotFoundException(message);
    } else {
      final String message =
          String.format(
              "Wasn't able to retrieve process definition with id [%s] from the engine. Maybe the Optimize user utilized "
                  + "for the import is not authorized or there are some issues with the internet connection? Response from the "
                  + "engine with alias %s: \n%s",
              processDefinitionId, getEngineAlias(), response.readEntity(String.class));
      throw new OptimizeProcessDefinitionFetchException(message);
    }
  }

  private ProcessDefinitionOptimizeDto mapToOptimizeProcessDefinition(
      ProcessDefinitionEngineDto engineEntity) {
    return new ProcessDefinitionOptimizeDto(
        engineEntity.getId(),
        engineEntity.getKey(),
        engineEntity.getVersionAsString(),
        engineEntity.getVersionTag(),
        engineEntity.getName(),
        true,
        new EngineDataSourceDto(this.getEngineAlias()),
        engineEntity.getTenantId().orElseGet(() -> this.getDefaultTenantId().orElse(null)));
  }

  public HistoricProcessInstanceDto fetchProcessInstance(final String processInstanceId) {
    final Response response =
        getEngineClient()
            .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
            .path(PROCESS_INSTANCE_ENDPOINT_TEMPLATE)
            .resolveTemplate("id", processInstanceId)
            .request(MediaType.APPLICATION_JSON)
            .acceptEncoding(UTF8)
            .get();

    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
      return response.readEntity(HistoricProcessInstanceDto.class);
    } else {
      return null;
    }
  }

  private UserDto mapEngineUser(EngineListUserDto engineUser) {
    if (this.configurationService.getUserIdentityCacheConfiguration().isIncludeUserMetaData()) {
      return new UserDto(
          engineUser.getId(),
          engineUser.getFirstName(),
          engineUser.getLastName(),
          engineUser.getEmail());
    } else {
      return new UserDto(engineUser.getId());
    }
  }

  public List<UserDto> getUsersById(final Collection<String> userIds) {
    return userIds.stream()
        // consider adding get multiple users by id to optimize api in engine, see OPT-2788
        .map(this::getUserById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  public List<UserDto> fetchPageOfUsers(final int pageStartIndex, final int pageLimit) {
    return fetchPageOfUsers(pageStartIndex, pageLimit, null);
  }

  public List<UserDto> fetchPageOfUsers(
      final int pageStartIndex, final int pageLimit, final String groupId) {
    Response response =
        getEngineClient()
            .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
            .queryParam(MAX_RESULTS_TO_RETURN, pageLimit)
            .queryParam("sortBy", "userId")
            .queryParam("sortOrder", "asc")
            .queryParam("memberOfGroup", groupId)
            .queryParam(INDEX_OF_FIRST_RESULT, pageStartIndex)
            .path(USER_ENDPOINT)
            .request(MediaType.APPLICATION_JSON)
            .get();
    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
      // @formatter:off
      return response
          .readEntity(new GenericType<List<EngineListUserDto>>() {})
          // @formatter:on
          .stream()
          .map(this::mapEngineUser)
          .toList();

    } else {
      final String message =
          String.format(
              "Failed querying users from engine with alias [%s], response status: [%s].",
              getEngineAlias(), response.getStatus());
      response.close();
      log.error(message);
      throw new OptimizeRuntimeException(message);
    }
  }

  public List<GroupDto> getGroupsById(final Collection<String> groupIds) {
    return groupIds.stream()
        // consider adding get multiple groups by id to optimize api in engine, see OPT-2788
        .map(this::getGroupById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  public Optional<GroupDto> getGroupById(final String groupId) {
    if (StringUtils.isEmpty(groupId)) {
      return Optional.empty();
    }
    EngineGroupDto groupDto = null;
    try {
      Response response =
          getEngineClient()
              .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
              .path(GROUP_BY_ID_ENDPOINT_TEMPLATE)
              .resolveTemplate("id", groupId)
              .request(MediaType.APPLICATION_JSON)
              .get();
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        groupDto = response.readEntity(EngineGroupDto.class);
      }
      response.close();
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch group with id [%s] from engine with alias [%s]",
              groupId, getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
    return Optional.ofNullable(groupDto)
        .map(
            group ->
                new GroupDto(
                    group.getId(),
                    group.getName(),
                    getUserCountForUserGroup(group.getId()).orElse(null)));
  }

  private Optional<Long> getUserCountForUserGroup(String userGroupId) {
    try {
      Response response =
          getEngineClient()
              .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
              .queryParam(MEMBER_OF_GROUP, userGroupId)
              .path(USER_COUNT_ENDPOINT)
              .request(MediaType.APPLICATION_JSON)
              .get();
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        return Optional.of(response.readEntity(CountDto.class).getCount());
      }
      response.close();
    } catch (Exception e) {
      String message =
          String.format(
              "Could not get user count for user group [%s] from engine with alias [%s]",
              userGroupId, getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
    return Optional.empty();
  }

  public Optional<String> getInstallationId() {
    // CAM-12294 will not be implemented, so returning empty
    return Optional.empty();
  }

  public List<GroupDto> fetchPageOfGroups(final int pageStartIndex, final int pageLimit) {
    Response response =
        getEngineClient()
            .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
            .queryParam(MAX_RESULTS_TO_RETURN, pageLimit)
            .queryParam(SORT_BY, "id")
            .queryParam(SORT_ORDER, SORT_ORDER_ASC)
            .queryParam(INDEX_OF_FIRST_RESULT, pageStartIndex)
            .path(GROUP_ENDPOINT)
            .request(MediaType.APPLICATION_JSON)
            .get();
    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
      // @formatter:off
      return response.readEntity(new GenericType<List<EngineGroupDto>>() {}).stream()
          .map(
              engineGroupDto ->
                  new GroupDto(
                      engineGroupDto.getId(),
                      engineGroupDto.getName(),
                      getUserCountForUserGroup(engineGroupDto.getId()).orElse(null)))
          .toList();
      // @formatter:on
    } else {
      final String message =
          String.format(
              "Failed querying groups from engine with alias [%s], response status: [%s].",
              getEngineAlias(), response.getStatus());
      response.close();
      log.error(message);
      throw new OptimizeRuntimeException(message);
    }
  }

  public List<GroupDto> getAllGroupsOfUser(final String userId) {
    try {
      Response response =
          getEngineClient()
              .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
              .queryParam(MEMBER, userId)
              .queryParam(
                  MAX_RESULTS_TO_RETURN, configurationService.getEngineImportGroupMaxPageSize())
              .path(GROUP_ENDPOINT)
              .request(MediaType.APPLICATION_JSON)
              .get();
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        // @formatter:off
        return response.readEntity(new GenericType<List<EngineGroupDto>>() {}).stream()
            .map(engineGroupDto -> new GroupDto(engineGroupDto.getId(), engineGroupDto.getName()))
            .toList();
        // @formatter:on
      }
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch groups for user [%s] from engine with alias [%s]",
              userId, getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
    return new ArrayList<>();
  }

  public List<AuthorizationDto> getAllApplicationAuthorizations() {
    try {
      return getAuthorizationsForType(RESOURCE_TYPE_APPLICATION);
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch application authorizations from the Engine with alias [%s] to check the access permissions.",
              getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message);
    }
  }

  public List<AuthorizationDto> getAllProcessDefinitionAuthorizations() {
    try {
      return getAuthorizationsForType(RESOURCE_TYPE_PROCESS_DEFINITION);
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch process definition authorizations from the Engine with alias [%s] to check the access "
                  + "permissions.",
              getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
  }

  public List<AuthorizationDto> getAllDecisionDefinitionAuthorizations() {
    try {
      return getAuthorizationsForType(RESOURCE_TYPE_DECISION_DEFINITION);
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch decision definition authorizations from the Engine with alias [%s] to check the access "
                  + "permissions.",
              getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
  }

  public List<AuthorizationDto> getAllTenantAuthorizations() {
    try {
      return getAuthorizationsForType(RESOURCE_TYPE_TENANT);
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch tenant authorizations from the Engine with alias [%s] to check the access "
                  + "permissions.",
              getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
  }

  public List<AuthorizationDto> getAllGroupAuthorizations() {
    try {
      return getAuthorizationsForType(RESOURCE_TYPE_GROUP);
    } catch (Exception e) {
      log.error(
          "Could not fetch group authorizations from the engine with alias {} to check the access permissions.",
          getEngineAlias(),
          e);
    }
    return new ArrayList<>();
  }

  public List<AuthorizationDto> getAllUserAuthorizations() {
    try {
      return getAuthorizationsForType(RESOURCE_TYPE_USER);
    } catch (Exception e) {
      log.error(
          "Could not fetch user authorizations from the engine with alias {} to check the access permissions.",
          getEngineAlias(),
          e);
    }
    return new ArrayList<>();
  }

  public List<AuthorizationDto> getAllApplicationAuthorizationsForUser(final String userId) {
    try {
      return getAuthorizationsForTypeForUser(RESOURCE_TYPE_APPLICATION, userId);
    } catch (Exception e) {
      final String message =
          String.format(
              "Could not fetch application authorizations for user with ID [%s] from the Engine with alias [%s] to check "
                  + "the access permissions.",
              userId, getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
  }

  public List<AuthorizationDto> getAllProcessDefinitionAuthorizationsForUser(final String userId) {
    try {
      return getAuthorizationsForTypeForUser(RESOURCE_TYPE_PROCESS_DEFINITION, userId);
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch process definition authorizations for user with ID [%s] from the Engine with alias [%s] to "
                  + "check the access permissions.",
              userId, getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
  }

  public List<AuthorizationDto> getAllDecisionDefinitionAuthorizationsForUser(final String userId) {
    try {
      return getAuthorizationsForTypeForUser(RESOURCE_TYPE_DECISION_DEFINITION, userId);
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch decision definition authorizations from the Engine with alias [%s] to check the access "
                  + "permissions.",
              getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
  }

  public List<AuthorizationDto> getAllTenantAuthorizationsForUser(final String userId) {
    try {
      return getAuthorizationsForTypeForUser(RESOURCE_TYPE_TENANT, userId);
    } catch (Exception e) {
      String message =
          String.format(
              "Could not fetch tenant authorizations from the Engine with alias [%s] to check the access "
                  + "permissions.",
              getEngineAlias());
      log.error(message, e);
      throw new OptimizeRuntimeException(message, e);
    }
  }

  public List<AuthorizationDto> getAllGroupAuthorizationsForUser(final String userId) {
    try {
      return getAuthorizationsForTypeForUser(RESOURCE_TYPE_GROUP, userId);
    } catch (Exception e) {
      log.error(
          "Could not fetch group authorizations from the engine with alias {} to check the access permissions.",
          getEngineAlias(),
          e);
    }
    return new ArrayList<>();
  }

  public List<AuthorizationDto> getAllUserAuthorizationsForUser(final String userId) {
    try {
      return getAuthorizationsForTypeForUser(RESOURCE_TYPE_USER, userId);
    } catch (Exception e) {
      log.error(
          "Could not fetch user authorizations from the engine with alias {} to check the access permissions.",
          getEngineAlias(),
          e);
    }
    return new ArrayList<>();
  }

  private List<AuthorizationDto> getAuthorizationsForTypeForUser(
      final int resourceType, final String userId) {
    final List<AuthorizationDto> allAuthorizations =
        getAuthorizationsForTypeForIdentity(resourceType, USER, Arrays.asList(userId, "*"));
    final List<String> groupIdsForUser =
        getAllGroupsOfUser(userId).stream().map(GroupDto::getId).toList();
    if (!groupIdsForUser.isEmpty()) {
      allAuthorizations.addAll(
          getAuthorizationsForTypeForIdentity(resourceType, GROUP, groupIdsForUser));
    }
    return allAuthorizations;
  }

  private List<AuthorizationDto> getAuthorizationsForType(final int resourceType) {
    int pageSize = configurationService.getEngineImportAuthorizationMaxPageSize();
    List<AuthorizationDto> totalAuthorizations = new ArrayList<>();
    List<AuthorizationDto> pageOfAuthorizations;
    do {
      final Response response =
          getEngineClient()
              .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
              .path(AUTHORIZATION_ENDPOINT)
              .queryParam(RESOURCE_TYPE, resourceType)
              .queryParam(INDEX_OF_FIRST_RESULT, totalAuthorizations.size())
              .queryParam(MAX_RESULTS_TO_RETURN, pageSize)
              .request(MediaType.APPLICATION_JSON)
              .get();
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        // @formatter:off
        pageOfAuthorizations = response.readEntity(new GenericType<>() {});
        totalAuthorizations.addAll(pageOfAuthorizations);
        // @formatter:on
      } else {
        String message =
            String.format(
                "Could not fetch authorizations from engine with alias [%s]! Error from engine: %s",
                getEngineAlias(), response.readEntity(String.class));
        log.debug(message);
        throw new OptimizeRuntimeException(message);
      }
      response.close();
    } while (pageOfAuthorizations.size() >= pageSize);
    return totalAuthorizations;
  }

  private List<AuthorizationDto> getAuthorizationsForTypeForIdentity(
      final int resourceType, final IdentityType identityType, final List<String> identityIds) {
    int pageSize = configurationService.getEngineImportAuthorizationMaxPageSize();
    List<AuthorizationDto> totalAuthorizations = new ArrayList<>();
    List<AuthorizationDto> pageOfAuthorizations;
    do {
      final Response response =
          getEngineClient()
              .target(configurationService.getEngineRestApiEndpointOfCustomEngine(getEngineAlias()))
              .path(AUTHORIZATION_ENDPOINT)
              .queryParam(
                  mapToIdentityQueryParam(identityType),
                  encodeCommaSeparatedListForUri(identityIds))
              .queryParam(RESOURCE_TYPE, resourceType)
              .queryParam(INDEX_OF_FIRST_RESULT, totalAuthorizations.size())
              .queryParam(MAX_RESULTS_TO_RETURN, pageSize)
              .request(MediaType.APPLICATION_JSON)
              .get();
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        // @formatter:off
        pageOfAuthorizations = response.readEntity(new GenericType<>() {});
        totalAuthorizations.addAll(pageOfAuthorizations);
        // @formatter:on
      } else {
        String message =
            String.format(
                "Could not fetch authorizations from engine with alias [%s] for [%s]s with IDs [%s]! Error from "
                    + "engine: %s",
                getEngineAlias(), identityType, identityIds, response.readEntity(String.class));
        log.debug(message);
        throw new OptimizeRuntimeException(message);
      }
      response.close();
    } while (pageOfAuthorizations.size() >= pageSize);
    return totalAuthorizations;
  }

  private String mapToIdentityQueryParam(final IdentityType identityType) {
    if (USER.equals(identityType)) {
      return USER_ID_IN;
    }
    return GROUP_ID_IN;
  }

  private String encodeCommaSeparatedListForUri(final List<String> stringList) {
    try {
      return URLEncoder.encode(String.join(",", stringList), StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new OptimizeRuntimeException("Error while encoding list for URI.", e);
    }
  }
}
