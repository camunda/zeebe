/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest.eventprocess;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_USER;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FIRSTNAME;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_LASTNAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.camunda.optimize.dto.optimize.GroupDto;
import io.camunda.optimize.dto.optimize.IdentityDto;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.IdentityWithMetadataResponseDto;
import io.camunda.optimize.dto.optimize.UserDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessMappingDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessRoleRequestDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessState;
import io.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
import io.camunda.optimize.dto.optimize.rest.EventProcessRoleResponseDto;
import io.camunda.optimize.dto.optimize.rest.event.EventProcessMappingResponseDto;
import io.camunda.optimize.service.exceptions.OptimizeValidationException;
import io.camunda.optimize.service.importing.eventprocess.AbstractEventProcessIT;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class EventBasedProcessRolesRestServiceIT extends AbstractEventProcessIT {

  private static final String USER_KERMIT = "kermit";
  private static final String TEST_GROUP = "testGroup";

  @Test
  public void createdEventBasedProcessContainsDefaultRole() {
    // given
    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String expectedId = eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    // when
    final List<EventProcessRoleResponseDto> roles =
        eventProcessClient.getEventProcessMappingRoles(expectedId);

    // then
    assertThat(roles)
        .hasSize(1)
        .extracting(EventProcessRoleResponseDto::getIdentity)
        .extracting(IdentityDto::getId)
        .containsExactly(DEFAULT_USERNAME);
  }

  @Test
  public void getRolesContainsUserMetadata_retrieveFromCache() {
    // given
    final UserDto expectedUserDtoWithData =
        new UserDto(DEFAULT_USERNAME, DEFAULT_FIRSTNAME, DEFAULT_LASTNAME, "me@camunda.com");
    embeddedOptimizeExtension.getIdentityService().addIdentity(expectedUserDtoWithData);

    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String expectedId = eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    // when
    final List<EventProcessRoleResponseDto> roles =
        eventProcessClient.getEventProcessMappingRoles(expectedId);

    // then
    assertThat(roles).hasSize(1);
    final IdentityWithMetadataResponseDto identityRestDto = roles.get(0).getIdentity();
    assertThat(identityRestDto).isInstanceOf(UserDto.class);
    final UserDto userDto = (UserDto) identityRestDto;
    assertThat(userDto.getFirstName()).isEqualTo(expectedUserDtoWithData.getFirstName());
    assertThat(userDto.getLastName()).isEqualTo(expectedUserDtoWithData.getLastName());
    assertThat(userDto.getName())
        .isEqualTo(
            expectedUserDtoWithData.getFirstName() + " " + expectedUserDtoWithData.getLastName());
    assertThat(userDto.getEmail()).isEqualTo(expectedUserDtoWithData.getEmail());
  }

  @Test
  public void getRolesIsFilteredByAuthorizations() {
    // given
    final UserDto userIdentity1 = new UserDto("testUser1", "Test User 1");
    final UserDto userIdentity2 = new UserDto("testUser2", "Test User 2");

    embeddedOptimizeExtension.getIdentityService().addIdentity(userIdentity1);
    embeddedOptimizeExtension.getIdentityService().addIdentity(userIdentity2);
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    embeddedOptimizeExtension
        .getConfigurationService()
        .getEventBasedProcessConfiguration()
        .setAuthorizedUserIds(Lists.newArrayList(USER_KERMIT, DEFAULT_USERNAME));
    authorizationClient.grantSingleResourceAuthorizationForKermit(
        userIdentity1.getId(), RESOURCE_TYPE_USER);

    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String expectedId = eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    eventProcessClient.updateEventProcessMappingRoles(
        expectedId,
        Arrays.asList(
            new EventProcessRoleRequestDto<>(userIdentity1),
            new EventProcessRoleRequestDto<>(userIdentity2)));

    // when
    final List<EventProcessRoleResponseDto> roles =
        eventProcessClient
            .createGetEventProcessMappingRolesRequest(expectedId)
            .withUserAuthentication(USER_KERMIT, USER_KERMIT)
            .execute(new TypeReference<>() {});

    // then
    assertThat(roles).hasSize(1);
    assertThat(roles.get(0).getIdentity().getId()).isEqualTo(userIdentity1.getId());
  }

  @Test
  public void updateEventBasedProcessRoles_singleEntry() {
    // given
    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String eventProcessMappingId =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    engineIntegrationExtension.addUser(USER_KERMIT, USER_KERMIT);
    engineIntegrationExtension.grantUserOptimizeAccess(USER_KERMIT);

    // when
    eventProcessClient.updateEventProcessMappingRoles(
        eventProcessMappingId,
        Collections.singletonList(new EventProcessRoleRequestDto<>(new UserDto(USER_KERMIT))));

    // then
    final List<EventProcessRoleResponseDto> roles =
        eventProcessClient.getEventProcessMappingRoles(eventProcessMappingId);
    assertThat(roles)
        .hasSize(1)
        .extracting(EventProcessRoleResponseDto::getIdentity)
        .extracting(IdentityDto::getId)
        .containsExactly(USER_KERMIT);
  }

  @Test
  public void updateEventBasedProcessRoles_failsForUnauthorizedEntries() {
    // given
    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String eventProcessMappingId =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
    final UserDto userIdentity1 = new UserDto("testUser1", "Test User 1");
    final UserDto userIdentity2 = new UserDto("testUser2", "Test User 2");

    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    embeddedOptimizeExtension
        .getConfigurationService()
        .getEventBasedProcessConfiguration()
        .setAuthorizedUserIds(Lists.newArrayList(USER_KERMIT, DEFAULT_USERNAME));
    authorizationClient.grantSingleResourceAuthorizationForKermit(
        userIdentity1.getId(), RESOURCE_TYPE_USER);

    // when
    final Response response =
        eventProcessClient
            .createUpdateEventProcessMappingRolesRequest(
                eventProcessMappingId,
                Arrays.asList(
                    new EventProcessRoleRequestDto<>(userIdentity1),
                    new EventProcessRoleRequestDto<>(userIdentity2)))
            .withUserAuthentication(USER_KERMIT, USER_KERMIT)
            .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void updateEventBasedProcessRoles_multipleEntries() {
    // given
    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String eventProcessMappingId =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    engineIntegrationExtension.addUser(USER_KERMIT, USER_KERMIT);
    engineIntegrationExtension.grantUserOptimizeAccess(USER_KERMIT);
    engineIntegrationExtension.createGroup(TEST_GROUP);
    engineIntegrationExtension.grantGroupOptimizeAccess(TEST_GROUP);

    final ImmutableList<EventProcessRoleRequestDto<IdentityDto>> roleEntries =
        ImmutableList.of(
            new EventProcessRoleRequestDto<>(new UserDto(USER_KERMIT)),
            new EventProcessRoleRequestDto<>(new GroupDto(TEST_GROUP)));
    // when
    eventProcessClient.updateEventProcessMappingRoles(eventProcessMappingId, roleEntries);

    // then
    final List<EventProcessRoleResponseDto> roles =
        eventProcessClient.getEventProcessMappingRoles(eventProcessMappingId);
    assertThat(roles)
        .extracting(EventProcessRoleResponseDto::getIdentity)
        .extracting(IdentityDto::getId, IdentityDto::getType)
        .containsExactly(
            Tuple.tuple(USER_KERMIT, IdentityType.USER),
            Tuple.tuple(TEST_GROUP, IdentityType.GROUP));
  }

  @Test
  public void updateEventBasedProcessRoles_multipleEntriesMissingTypeResolved() {
    // given
    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String eventProcessMappingId =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    engineIntegrationExtension.addUser(USER_KERMIT, USER_KERMIT);
    engineIntegrationExtension.grantUserOptimizeAccess(USER_KERMIT);
    engineIntegrationExtension.createGroup(TEST_GROUP);
    engineIntegrationExtension.grantGroupOptimizeAccess(TEST_GROUP);

    final ImmutableList<EventProcessRoleRequestDto<IdentityDto>> roleEntries =
        ImmutableList.of(
            new EventProcessRoleRequestDto<>(new IdentityDto(USER_KERMIT, null)),
            new EventProcessRoleRequestDto<>(new IdentityDto(TEST_GROUP, null)));
    // when
    eventProcessClient.updateEventProcessMappingRoles(eventProcessMappingId, roleEntries);

    // then
    final List<EventProcessRoleResponseDto> roles =
        eventProcessClient.getEventProcessMappingRoles(eventProcessMappingId);
    assertThat(roles)
        .extracting(EventProcessRoleResponseDto::getIdentity)
        .extracting(IdentityDto::getId, IdentityDto::getType)
        .containsExactly(
            Tuple.tuple(USER_KERMIT, IdentityType.USER),
            Tuple.tuple(TEST_GROUP, IdentityType.GROUP));
  }

  @Test
  public void updateEventBasedProcessRoles_emptyListFails() {
    // given
    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String eventProcessMappingId =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    // when
    final ErrorResponseDto updateResponse =
        eventProcessClient
            .createUpdateEventProcessMappingRolesRequest(
                eventProcessMappingId, Collections.emptyList())
            .execute(ErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());

    // then
    assertThat(updateResponse.getErrorCode()).isEqualTo(OptimizeValidationException.ERROR_CODE);
  }

  @Test
  public void updateEventBasedProcessRoles_onInvalidIdentityFail() {
    // given
    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String eventProcessMappingId =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    // when
    final ErrorResponseDto updateResponse =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildUpdateEventProcessRolesRequest(
                eventProcessMappingId,
                Collections.singletonList(new EventProcessRoleRequestDto<>(new UserDto("invalid"))))
            .execute(ErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());

    // then
    assertThat(updateResponse.getErrorCode()).isEqualTo(OptimizeValidationException.ERROR_CODE);
  }

  @Test
  public void updateEventBasedProcessRoles_onInvalidIdentityAmongValidOnesFail() {
    // given
    final EventProcessMappingDto eventProcessMappingDto =
        createEventProcessMappingDtoWithSimpleMappings();
    final String eventProcessMappingId =
        eventProcessClient.createEventProcessMapping(eventProcessMappingDto);

    // when
    final ErrorResponseDto updateResponse =
        eventProcessClient
            .createUpdateEventProcessMappingRolesRequest(
                eventProcessMappingId,
                ImmutableList.of(
                    new EventProcessRoleRequestDto<>(new UserDto("invalid")),
                    new EventProcessRoleRequestDto<>(new UserDto(DEFAULT_USERNAME))))
            .execute(ErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());

    // then
    assertThat(updateResponse.getErrorCode()).isEqualTo(OptimizeValidationException.ERROR_CODE);
  }

  @Test
  public void updateEventBasedProcessRoles_afterPublishLastModifiedAndStateUnchanged() {
    // given
    ingestTestEvent(STARTED_EVENT, OffsetDateTime.now());
    ingestTestEvent(FINISHED_EVENT, OffsetDateTime.now());
    databaseIntegrationTestExtension.refreshAllOptimizeIndices();
    final String eventProcessMappingId =
        createSimpleEventProcessMapping(STARTED_EVENT, FINISHED_EVENT);

    // when
    eventProcessClient.publishEventProcessMapping(eventProcessMappingId);
    executeImportCycle();
    executeImportCycle();
    final EventProcessMappingResponseDto eventProcessMapping =
        eventProcessClient.getEventProcessMapping(eventProcessMappingId);

    // then
    assertThat(eventProcessMapping.getState()).isEqualTo(EventProcessState.PUBLISHED);

    // when
    engineIntegrationExtension.addUser(USER_KERMIT, USER_KERMIT);
    engineIntegrationExtension.grantUserOptimizeAccess(USER_KERMIT);
    eventProcessClient.updateEventProcessMappingRoles(
        eventProcessMappingId,
        Collections.singletonList(new EventProcessRoleRequestDto<>(new UserDto(USER_KERMIT))));

    // then
    final List<EventProcessRoleResponseDto> roles =
        eventProcessClient.getEventProcessMappingRoles(eventProcessMappingId);
    assertThat(roles)
        .hasSize(1)
        .extracting(EventProcessRoleResponseDto::getIdentity)
        .extracting(IdentityDto::getId)
        .containsExactly(USER_KERMIT);
    final EventProcessMappingResponseDto updatedMapping =
        eventProcessClient.getEventProcessMapping(eventProcessMappingId);
    assertThat(updatedMapping.getLastModified()).isEqualTo(eventProcessMapping.getLastModified());
    assertThat(updatedMapping.getLastModifier()).isEqualTo(eventProcessMapping.getLastModifier());
    assertThat(updatedMapping.getState()).isEqualTo(eventProcessMapping.getState());
  }

  private EventProcessMappingDto createEventProcessMappingDtoWithSimpleMappings() {
    return eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
        Collections.emptyMap(), "process name", createSimpleProcessDefinitionXml());
  }
}
