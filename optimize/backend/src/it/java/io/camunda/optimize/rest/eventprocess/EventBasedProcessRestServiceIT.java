/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// // TODO recreate C8 IT equivalent of this with #13337
// // package io.camunda.optimize.rest.eventprocess;
// //
// // import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// // import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// // import static io.camunda.optimize.rest.RestTestUtil.getOffsetDiffInHours;
// // import static io.camunda.optimize.rest.constants.RestConstants.X_OPTIMIZE_CLIENT_TIMEZONE;
// // import static
// io.camunda.optimize.service.db.DatabaseConstants.EVENT_PROCESS_MAPPING_INDEX_NAME;
// // import static
// io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
// // import static io.camunda.optimize.test.optimize.EventProcessClient.createEventMappingsDto;
// // import static
// io.camunda.optimize.test.optimize.EventProcessClient.createExternalEventSourceEntryForGroup;
// // import static io.camunda.optimize.test.optimize.EventProcessClient.createMappedEventDto;
// // import static
// io.camunda.optimize.test.optimize.EventProcessClient.createSimpleCamundaEventSourceEntry;
// // import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
// // import static jakarta.ws.rs.HttpMethod.GET;
// // import static jakarta.ws.rs.HttpMethod.POST;
// // import static jakarta.ws.rs.HttpMethod.PUT;
// // import static org.assertj.core.api.Assertions.assertThat;
// // import static org.assertj.core.groups.Tuple.tuple;
// // import static org.mockserver.model.HttpRequest.request;
// // import static org.mockserver.verify.VerificationTimes.exactly;
// //
// // import com.google.common.collect.ImmutableMap;
// // import io.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
// // import io.camunda.optimize.dto.optimize.UserDto;
// // import io.camunda.optimize.dto.optimize.query.IdResponseDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventImportSourceDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventMappingDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventProcessMappingDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventProcessPublishStateDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventProcessState;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventTypeDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.source.EventSourceEntryDto;
// // import
// io.camunda.optimize.dto.optimize.query.event.process.source.ExternalEventSourceEntryDto;
// // import io.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
// // import io.camunda.optimize.dto.optimize.rest.EventMappingCleanupRequestDto;
// // import io.camunda.optimize.dto.optimize.rest.EventProcessMappingRequestDto;
// // import io.camunda.optimize.dto.optimize.rest.EventProcessRoleResponseDto;
// // import io.camunda.optimize.dto.optimize.rest.event.EventProcessMappingResponseDto;
// // import io.camunda.optimize.exception.OptimizeIntegrationTestException;
// // import io.camunda.optimize.service.importing.eventprocess.AbstractEventProcessIT;
// // import io.camunda.optimize.service.security.util.LocalDateUtil;
// // import io.camunda.optimize.service.util.IdGenerator;
// // import io.camunda.optimize.service.util.configuration.EventBasedProcessConfiguration;
// // import io.camunda.optimize.util.BpmnModels;
// // import jakarta.ws.rs.core.Response;
// // import java.io.ByteArrayOutputStream;
// // import java.nio.charset.StandardCharsets;
// // import java.time.Instant;
// // import java.time.OffsetDateTime;
// // import java.time.ZoneId;
// // import java.util.Arrays;
// // import java.util.Collections;
// // import java.util.HashMap;
// // import java.util.List;
// // import java.util.Map;
// // import java.util.Optional;
// // import java.util.stream.Stream;
// // import lombok.NonNull;
// // import lombok.SneakyThrows;
// // import org.assertj.core.groups.Tuple;
// // import org.camunda.bpm.model.bpmn.Bpmn;
// // import org.camunda.bpm.model.bpmn.BpmnModelInstance;
// // import org.junit.jupiter.api.BeforeAll;
// // import org.junit.jupiter.api.Tag;
// // import org.junit.jupiter.api.Test;
// // import org.junit.jupiter.params.ParameterizedTest;
// // import org.junit.jupiter.params.provider.Arguments;
// // import org.junit.jupiter.params.provider.MethodSource;
// // import org.mockserver.integration.ClientAndServer;
// // import org.mockserver.matchers.Times;
// // import org.mockserver.model.HttpError;
// // import org.mockserver.model.HttpRequest;
// // import org.mockserver.verify.VerificationTimes;
// //
// // @Tag(OPENSEARCH_PASSING)
// // public class EventBasedProcessRestServiceIT extends AbstractEventProcessIT {
// //
// //   private static String simpleDiagramXml;
// //
// //   @BeforeAll
// //   public static void setup() {
// //     simpleDiagramXml = createProcessDefinitionXml();
// //   }
// //
// //   private static Stream<Arguments> getAllEndpointsThatNeedEventAuthorization() {
// //     return Stream.of(
// //         Arguments.of(GET, "/eventBasedProcess", null),
// //         Arguments.of(POST, "/eventBasedProcess/delete-conflicts", Collections.emptyList()),
// //         Arguments.of(POST, "/eventBasedProcess", null),
// //         Arguments.of(
// //             PUT,
// //             "/eventBasedProcess/someId",
// //             EventProcessMappingRequestDto.builder().name("someName").build()),
// //         Arguments.of(POST, "/eventBasedProcess/someId/_publish", null),
// //         Arguments.of(POST, "/eventBasedProcess/someId/_cancelPublish", null),
// //         Arguments.of(GET, "/eventBasedProcess/someId/role", null),
// //         Arguments.of(
// //             PUT,
// //             "/eventBasedProcess/someId/role",
// //             Collections.singleton(new EventProcessRoleResponseDto(new UserDto("someId")))),
// //         Arguments.of(
// //             POST,
// //             "/eventBasedProcess/_mappingCleanup",
// //             EventMappingCleanupRequestDto.builder().xml("<xml></xml>").build()));
// //   }
// //
// //   @Test
// //   public void getIsEventBasedProcessesEnabled() {
// //     // when
// //     final boolean isEnabled = eventProcessClient.getIsEventBasedProcessEnabled();
// //
// //     // then
// //     assertThat(isEnabled).isTrue();
// //   }
// //
// //   @Test
// //   public void
// //       getIsEventBasedProcessesEnabledWithUserNotGrantedEventBasedProcessAccessReturnsFalse() {
// //     // given
// //     embeddedOptimizeExtension
// //         .getConfigurationService()
// //         .getEventBasedProcessConfiguration()
// //         .getAuthorizedUserIds()
// //         .clear();
// //
// //     // when
// //     final boolean isEnabled = eventProcessClient.getIsEventBasedProcessEnabled();
// //
// //     // then
// //     assertThat(isEnabled).isFalse();
// //   }
// //
// //   @Test
// //   public void getIsEventBasedProcessEnabledWithUserNotAuthorizedButInAuthorizedGroup() {
// //     // given only group authorization exists containing user
// //     final EventBasedProcessConfiguration eventBasedProcessConfiguration =
// //
// embeddedOptimizeExtension.getConfigurationService().getEventBasedProcessConfiguration();
// //     eventBasedProcessConfiguration.getAuthorizedUserIds().clear();
// //
// //     final String authorizedGroup = "senate";
// //     authorizationClient.createGroupAndAddUser(authorizedGroup, DEFAULT_USERNAME);
// //     eventBasedProcessConfiguration.setAuthorizedGroupIds(
// //         Collections.singletonList(authorizedGroup));
// //
// //     // when
// //     final boolean isEnabled = eventProcessClient.getIsEventBasedProcessEnabled();
// //
// //     // then
// //     assertThat(isEnabled).isTrue();
// //   }
// //
// //   @Test
// //   public void getIsEventBasedProcessEnabledWithNoAuthorizedUsersOrGroups() {
// //     // given
// //     final EventBasedProcessConfiguration eventBasedProcessConfiguration =
// //
// embeddedOptimizeExtension.getConfigurationService().getEventBasedProcessConfiguration();
// //     eventBasedProcessConfiguration.getAuthorizedUserIds().clear();
// //     eventBasedProcessConfiguration.getAuthorizedGroupIds().clear();
// //
// //     // when
// //     final boolean isEnabled = eventProcessClient.getIsEventBasedProcessEnabled();
// //
// //     // then
// //     assertThat(isEnabled).isFalse();
// //   }
// //
// //   @Test
// //   public void getIsEventBasedProcessEnabledWithUserInGroupNotAuthorized() {
// //     // given user exists in group not authorized for access
// //     final EventBasedProcessConfiguration eventBasedProcessConfiguration =
// //
// embeddedOptimizeExtension.getConfigurationService().getEventBasedProcessConfiguration();
// //     eventBasedProcessConfiguration.getAuthorizedUserIds().clear();
// //
// //     final String authorizedGroup = "humans";
// //     authorizationClient.createGroupAndAddUser(authorizedGroup, DEFAULT_USERNAME);
// //
// eventBasedProcessConfiguration.setAuthorizedGroupIds(Collections.singletonList("zombies"));
// //
// //     // when
// //     final boolean isEnabled = eventProcessClient.getIsEventBasedProcessEnabled();
// //
// //     // then
// //     assertThat(isEnabled).isFalse();
// //   }
// //
// //   @Test
// //   public void getIsEventBasedProcessEnabledWithAuthorizedUserAndInAuthorizedGroup() {
// //     // given user is authorized and is in authorized group
// //     final EventBasedProcessConfiguration eventBasedProcessConfiguration =
// //
// embeddedOptimizeExtension.getConfigurationService().getEventBasedProcessConfiguration();
// //
// //     final String authorizedGroup = "humans";
// //     authorizationClient.createGroupAndAddUser(authorizedGroup, DEFAULT_USERNAME);
// //     eventBasedProcessConfiguration.setAuthorizedGroupIds(
// //         Collections.singletonList(authorizedGroup));
// //
// //     // when
// //     final boolean isEnabled = eventProcessClient.getIsEventBasedProcessEnabled();
// //
// //     // then
// //     assertThat(isEnabled).isTrue();
// //   }
// //
// //   @ParameterizedTest
// //   @MethodSource("getAllEndpointsThatNeedEventAuthorization")
// //   public void
// callingEventBasedProcessApiWithUserNotGrantedEventBasedProcessAccessReturnsForbidden(
// //       final String method, final String path, final Object payload) {
// //     // given
// //     final EventBasedProcessConfiguration eventProcessConfiguration =
// //
// embeddedOptimizeExtension.getConfigurationService().getEventBasedProcessConfiguration();
// //     eventProcessConfiguration.getAuthorizedUserIds().clear();
// //     eventProcessConfiguration.getAuthorizedGroupIds().clear();
// //
// //     // when
// //     final Response response =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGenericRequest(method, path, payload)
// //             .execute();
// //
// //     // then the status code is forbidden
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping() {
// //     // given
// //     final ClientAndServer dbMockServer = useAndGetDbMockServer();
// //
// //     // when
// //     final Response response =
// //         eventProcessClient
// //             .createCreateEventProcessMappingRequest(
// //                 eventProcessClient.buildEventProcessMappingDto(simpleDiagramXml))
// //             .execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
// //     dbMockServer.verify(
// //         request().withPath("/.*-" + EVENT_PROCESS_MAPPING_INDEX_NAME +
// "/_doc/.*").withMethod(PUT),
// //         exactly(1));
// //   }
// //
// //   @Test
// //   public void createEventProcessMappingUsingLabels_returnedByGetRequest() {
// //     final String firstLabel = "oneLabel";
// //     final String secondLabel = "anotherLabel";
// //     final Map<String, EventMappingDto> eventMappings =
// //         ImmutableMap.of(
// //             USER_TASK_ID_ONE,
// //             createEventMappingsDto(
// //                 createMappedEventDtoWithLabel(firstLabel),
// //                 createMappedEventDtoWithLabel(secondLabel)));
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             eventMappings, "process name", simpleDiagramXml);
// //     final String eventProcessMappingId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     eventProcessClient
// //         .createCreateEventProcessMappingRequest(eventProcessMappingDto)
// //         .execute(Response.Status.OK.getStatusCode());
// //     final EventProcessMappingResponseDto storedMapping =
// //         eventProcessClient.getEventProcessMapping(eventProcessMappingId);
// //
// //     // then
// //     assertThat(storedMapping.getMappings().values())
// //         .hasSize(1)
// //         .allSatisfy(
// //             mappings -> {
// //               assertThat(mappings.getStart().getEventLabel()).isEqualTo(firstLabel);
// //               assertThat(mappings.getEnd().getEventLabel()).isEqualTo(secondLabel);
// //             });
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_elasticsearchConnectionError() {
// //     // given
// //     final ClientAndServer dbMockServer = useAndGetDbMockServer();
// //     final HttpRequest requestMatcher =
// //         request().withPath("/.*-" + EVENT_PROCESS_MAPPING_INDEX_NAME +
// "/_doc/.*").withMethod(PUT);
// //     dbMockServer
// //         .when(requestMatcher, Times.once())
// //         .error(HttpError.error().withDropConnection(true));
// //
// //     // when
// //     final Response createResponse =
// //         eventProcessClient
// //             .createCreateEventProcessMappingRequest(
// //                 eventProcessClient.buildEventProcessMappingDto(simpleDiagramXml))
// //             .execute();
// //
// //     // then
// //     dbMockServer.verify(requestMatcher, VerificationTimes.once());
// //     assertThat(createResponse.getStatus())
// //         .isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_withEventMappingCombinations() {
// //     // given event mappings with IDs existing in XML
// //     final Map<String, EventMappingDto> eventMappings = new HashMap<>();
// //     eventMappings.put(
// //         USER_TASK_ID_ONE, createEventMappingsDto(createMappedEventDto(),
// createMappedEventDto()));
// //     eventMappings.put(USER_TASK_ID_TWO, createEventMappingsDto(createMappedEventDto(), null));
// //     eventMappings.put(USER_TASK_ID_THREE, createEventMappingsDto(null,
// createMappedEventDto()));
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             eventMappings, "process name", simpleDiagramXml);
// //
// //     // when
// //     final Response response =
// //
// eventProcessClient.createCreateEventProcessMappingRequest(eventProcessMappingDto).execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_withEventMappingIdNotExistInXml() {
// //     // given event mappings with ID does not exist in XML
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap(
// //                 "invalid_Id",
// //                 createEventMappingsDto(createMappedEventDto(), createMappedEventDto())),
// //             "process name",
// //             simpleDiagramXml);
// //
// //     // when
// //     final Response response =
// //
// eventProcessClient.createCreateEventProcessMappingRequest(eventProcessMappingDto).execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_multipleExternalEventSources() {
// //     // given
// //     final List<EventSourceEntryDto<?>> externalEventSources =
// //         Arrays.asList(
// //             createExternalEventSourceEntryForGroup("firstGroup"),
// //             createExternalEventSourceEntryForGroup("secondGroup"));
// //     final Map<String, EventMappingDto> eventMappings = new HashMap<>();
// //     eventMappings.put(
// //         USER_TASK_ID_ONE, createEventMappingsDto(createMappedEventDto(),
// createMappedEventDto()));
// //     eventMappings.put(USER_TASK_ID_TWO, createEventMappingsDto(createMappedEventDto(), null));
// //     eventMappings.put(USER_TASK_ID_THREE, createEventMappingsDto(null,
// createMappedEventDto()));
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsWithXmlAndEventSources(
// //             eventMappings, "process name", simpleDiagramXml, externalEventSources);
// //
// //     // when
// //     final String processId =
// //         eventProcessClient
// //             .createCreateEventProcessMappingRequest(eventProcessMappingDto)
// //             .execute(IdResponseDto.class, Response.Status.OK.getStatusCode())
// //             .getId();
// //
// //     // then
// //     assertThat(eventProcessClient.getEventProcessMapping(processId))
// //         .extracting(EventProcessMappingResponseDto::getEventSources)
// //         .isEqualTo(externalEventSources);
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_withEventMappingsAndXmlNotPresent() {
// //     // given event mappings but no XML provided
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap(
// //                 "some_task_id",
// //                 createEventMappingsDto(createMappedEventDto(), createMappedEventDto())),
// //             "process name",
// //             null);
// //
// //     // when
// //     final Response response =
// //
// eventProcessClient.createCreateEventProcessMappingRequest(eventProcessMappingDto).execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_withNullStartAndEndEventMappings() {
// //     // given event mapping entry but neither start nor end is mapped
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap("some_task_id", createEventMappingsDto(null, null)),
// //             "process name",
// //             simpleDiagramXml);
// //
// //     // when
// //     final Response response =
// //
// eventProcessClient.createCreateEventProcessMappingRequest(eventProcessMappingDto).execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_withInvalidEventMappings() {
// //     // given event mappings but mapped events have fields missing
// //     final EventTypeDto invalidEventTypeDto =
// //         EventTypeDto.builder()
// //             .group(IdGenerator.getNextId())
// //             .source(IdGenerator.getNextId())
// //             .eventName(null)
// //             .build();
// //     invalidEventTypeDto.setGroup(null);
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap(
// //                 USER_TASK_ID_ONE,
// //                 createEventMappingsDto(invalidEventTypeDto, createMappedEventDto())),
// //             "process name",
// //             simpleDiagramXml);
// //
// //     // when
// //     final Response response =
// //
// eventProcessClient.createCreateEventProcessMappingRequest(eventProcessMappingDto).execute();
// //
// //     // then a bad request exception is thrown
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_withBPMNEventWithStartAndEndMapping() {
// //     // given event mappings but BPMN event has start and end mapping
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap(
// //                 BPMN_START_EVENT_ID,
// //                 createEventMappingsDto(createMappedEventDto(), createMappedEventDto())),
// //             "process name",
// //             simpleDiagramXml);
// //
// //     // when
// //     final Response response =
// //
// eventProcessClient.createCreateEventProcessMappingRequest(eventProcessMappingDto).execute();
// //
// //     // then a bad request exception is thrown
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_invalidModelXml() {
// //     // when
// //     final Response response =
// //         eventProcessClient
// //             .createCreateEventProcessMappingRequest(
// //                 eventProcessClient.buildEventProcessMappingDto("some invalid BPMN xml"))
// //             .execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void createEventProcessMapping_camundaSourceHasNoEventsImported() {
// //     // given
// //     final ProcessDefinitionEngineDto processDefinitionEngineDto =
// //         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
// //             BpmnModels.getSimpleBpmnDiagram("someDefinition"));
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsWithXmlAndEventSources(
// //             Collections.emptyMap(),
// //             "processName",
// //             simpleDiagramXml,
// //             Collections.singletonList(
// //                 createSimpleCamundaEventSourceEntry(processDefinitionEngineDto.getKey())));
// //
// //     // when
// //     final Response response =
// //
// eventProcessClient.createCreateEventProcessMappingRequest(eventProcessMappingDto).execute();
// //
// //     // then a bad request exception is thrown
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void getEventProcessMappingWithId() {
// //     // given
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     final OffsetDateTime now = OffsetDateTime.parse("2019-11-25T10:00:00+01:00");
// //     LocalDateUtil.setCurrentTime(now);
// //     final String expectedId =
// eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     final EventProcessMappingResponseDto actual =
// //         eventProcessClient.getEventProcessMapping(expectedId);
// //
// //     // then
// //     assertThat(actual.getId()).isEqualTo(expectedId);
// //     assertThat(actual)
// //         .usingRecursiveComparison()
// //         .ignoringFields(
// //             EventProcessMappingDto.Fields.id,
// //             EventProcessMappingDto.Fields.lastModified,
// //             EventProcessMappingDto.Fields.lastModifier,
// //             EventProcessMappingDto.Fields.state,
// //             EventProcessMappingDto.Fields.roles,
// //             EventProcessMappingDto.Fields.eventSources)
// //         .isEqualTo(eventProcessMappingDto);
// //     assertThat(actual)
// //         .extracting(EventProcessMappingDto.Fields.eventSources)
// //         .asList()
// //         .hasSize(1)
// //         .containsExactly(eventProcessMappingDto.getEventSources().get(0));
// //     assertThat(actual.getLastModified()).isEqualTo(now);
// //     assertThat(actual.getState()).isEqualTo(EventProcessState.MAPPED);
// //     assertThat(actual.getLastModifier()).isEqualTo(DEFAULT_FULLNAME);
// //   }
// //
// //   @Test
// //   public void getEventProcessMappingWithId_adoptTimezoneFromHeader() {
// //     // given
// //     final OffsetDateTime now = dateFreezer().timezone("Europe/Berlin").freezeDateAndReturn();
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     final String eventProcessMappingId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     final EventProcessMappingResponseDto mapping =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGetEventProcessMappingRequest(eventProcessMappingId)
// //             .addSingleHeader(X_OPTIMIZE_CLIENT_TIMEZONE, "Europe/London")
// //             .execute(EventProcessMappingResponseDto.class,
// Response.Status.OK.getStatusCode());
// //
// //     // then
// //     assertThat(mapping.getLastModified()).isEqualTo(now);
// //     assertThat(getOffsetDiffInHours(mapping.getLastModified(), now)).isEqualTo(1.);
// //   }
// //
// //   @Test
// //   public void getEventProcessMappingWithId_unmappedState() {
// //     // given
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             null, "process name", simpleDiagramXml);
// //     final String expectedId =
// eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     final EventProcessMappingResponseDto actual =
// //         eventProcessClient.getEventProcessMapping(expectedId);
// //
// //     // then the report is returned in state unmapped
// //     assertThat(actual.getState()).isEqualTo(EventProcessState.UNMAPPED);
// //   }
// //
// //   @Test
// //   public void getEventProcessMappingWithIdNotExists() {
// //     // when
// //     final Response response =
// //
// eventProcessClient.createGetEventProcessMappingRequest(IdGenerator.getNextId()).execute();
// //
// //     // then the report is returned with expect
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
// //   }
// //
// //   @Test
// //   public void getAllEventProcessMappings() {
// //     // given
// //     final Map<String, EventMappingDto> firstProcessMappings =
// //         Collections.singletonMap(
// //             USER_TASK_ID_THREE,
// //             createEventMappingsDto(createMappedEventDto(), createMappedEventDto()));
// //     final EventProcessMappingDto firstExpectedDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             firstProcessMappings, "process name", simpleDiagramXml);
// //     final OffsetDateTime now = OffsetDateTime.parse("2019-11-25T10:00:00+01:00");
// //     LocalDateUtil.setCurrentTime(now);
// //     final String firstExpectedId =
// eventProcessClient.createEventProcessMapping(firstExpectedDto);
// //     final EventProcessMappingDto secondExpectedDto =
// //         eventProcessClient.buildEventProcessMappingDto(simpleDiagramXml);
// //     final String secondExpectedId =
// eventProcessClient.createEventProcessMapping(secondExpectedDto);
// //
// //     // when
// //     final List<EventProcessMappingDto> response =
// eventProcessClient.getAllEventProcessMappings();
// //
// //     // then the response contains expected processes with xml omitted
// //     assertThat(response)
// //         .extracting(
// //             EventProcessMappingDto.Fields.id,
// //             EventProcessMappingDto.Fields.name,
// //             EventProcessMappingDto.Fields.xml,
// //             EventProcessMappingDto.Fields.lastModified,
// //             EventProcessMappingDto.Fields.lastModifier,
// //             EventProcessMappingDto.Fields.mappings,
// //             EventProcessMappingDto.Fields.state)
// //         .containsExactlyInAnyOrder(
// //             tuple(
// //                 firstExpectedId,
// //                 firstExpectedDto.getName(),
// //                 null,
// //                 now,
// //                 DEFAULT_FULLNAME,
// //                 firstProcessMappings,
// //                 EventProcessState.MAPPED),
// //             tuple(
// //                 secondExpectedId,
// //                 secondExpectedDto.getName(),
// //                 null,
// //                 now,
// //                 DEFAULT_FULLNAME,
// //                 null,
// //                 EventProcessState.UNMAPPED));
// //   }
// //
// //   @Test
// //   public void getAllEventProcessMappings_adoptTimezoneFromHeader() {
// //     // given
// //     final OffsetDateTime now = dateFreezer().timezone("Europe/Berlin").freezeDateAndReturn();
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             null, "process name", simpleDiagramXml);
// //     eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     final List<EventProcessMappingDto> allMappings =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildGetAllEventProcessMappingsRequests()
// //             .addSingleHeader(X_OPTIMIZE_CLIENT_TIMEZONE, "Europe/London")
// //             .executeAndReturnList(EventProcessMappingDto.class,
// Response.Status.OK.getStatusCode());
// //
// //     // then
// //     assertThat(allMappings).isNotNull().hasSize(1);
// //     final EventProcessMappingDto mappingDto = allMappings.get(0);
// //     assertThat(mappingDto.getLastModified()).isEqualTo(now);
// //     assertThat(getOffsetDiffInHours(mappingDto.getLastModified(), now)).isEqualTo(1.);
// //   }
// //
// //   @Test
// //   public void updateEventProcessMapping_withMappingsAdded() {
// //     // given
// //     final OffsetDateTime createdTime = OffsetDateTime.parse("2019-11-24T18:00:00+01:00");
// //     LocalDateUtil.setCurrentTime(createdTime);
// //     final String storedEventProcessMappingId =
// //         eventProcessClient.createEventProcessMapping(
// //             eventProcessClient.buildEventProcessMappingDto(simpleDiagramXml));
// //
// //     // when
// //     final EventProcessMappingDto updateDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap(
// //                 USER_TASK_ID_THREE,
// //                 createEventMappingsDto(createMappedEventDto(), createMappedEventDto())),
// //             "new process name",
// //             simpleDiagramXml);
// //     final OffsetDateTime updatedTime = OffsetDateTime.parse("2019-11-25T10:00:00+01:00");
// //     LocalDateUtil.setCurrentTime(updatedTime);
// //     final Response response =
// //         eventProcessClient
// //             .createUpdateEventProcessMappingRequest(storedEventProcessMappingId, updateDto)
// //             .execute();
// //
// //     // then the update response code is correct
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //
// //     // then the fields have been updated
// //     final EventSourceEntryDto<?> eventSourceEntry = updateDto.getEventSources().get(0);
// //     final EventProcessMappingResponseDto storedDto =
// //         eventProcessClient.getEventProcessMapping(storedEventProcessMappingId);
// //     assertThat(storedDto)
// //         .usingRecursiveComparison()
// //         .ignoringFields(
// //             EventProcessMappingDto.Fields.id,
// //             EventProcessMappingDto.Fields.lastModified,
// //             EventProcessMappingDto.Fields.lastModifier,
// //             EventProcessMappingDto.Fields.state,
// //             EventProcessMappingDto.Fields.roles,
// //             EventProcessMappingDto.Fields.eventSources)
// //         .isEqualTo(updateDto);
// //     assertThat(storedDto).extracting("id").isEqualTo(storedEventProcessMappingId);
// //     assertThat(storedDto.getLastModified()).isEqualTo(updatedTime);
// //     assertThat(storedDto.getLastModifier()).isEqualTo(DEFAULT_FULLNAME);
// //     assertThat(storedDto.getEventSources())
// //         .hasSize(1)
// //         .extracting(
// //             EventSourceEntryDto::getId,
// //             EventSourceEntryDto::getSourceType,
// //             source -> source.getConfiguration().getEventScope())
// //         .containsExactly(
// //             Tuple.tuple(
// //                 eventSourceEntry.getId(),
// //                 eventSourceEntry.getSourceType(),
// //                 eventSourceEntry.getConfiguration().getEventScope()));
// //   }
// //
// //   @Test
// //   public void updateEventProcessMapping_addEventLabels() {
// //     final Map<String, EventMappingDto> eventMappings =
// //         ImmutableMap.of(
// //             USER_TASK_ID_ONE,
// //             createEventMappingsDto(createMappedEventDto(), createMappedEventDto()));
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             eventMappings, "process name", simpleDiagramXml);
// //     final String eventProcessMappingId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     eventProcessClient
// //         .createCreateEventProcessMappingRequest(eventProcessMappingDto)
// //         .execute(Response.Status.OK.getStatusCode());
// //     final EventProcessMappingResponseDto storedMapping =
// //         eventProcessClient.getEventProcessMapping(eventProcessMappingId);
// //
// //     // then
// //     assertThat(storedMapping.getMappings().values())
// //         .hasSize(1)
// //         .allSatisfy(
// //             mappings -> {
// //               assertThat(mappings.getStart().getEventLabel()).isNull();
// //               assertThat(mappings.getEnd().getEventLabel()).isNull();
// //             });
// //
// //     // when
// //     final String firstLabel = "oneLabel";
// //     final String secondLabel = "anotherLabel";
// //     final Map<String, EventMappingDto> updatedEventMappings =
// //         ImmutableMap.of(
// //             USER_TASK_ID_ONE,
// //             createEventMappingsDto(
// //                 createMappedEventDtoWithLabel(firstLabel),
// //                 createMappedEventDtoWithLabel(secondLabel)));
// //     final EventProcessMappingDto updateDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             updatedEventMappings, "process name", simpleDiagramXml);
// //     eventProcessClient
// //         .createUpdateEventProcessMappingRequest(eventProcessMappingId, updateDto)
// //         .execute(Response.Status.NO_CONTENT.getStatusCode());
// //
// //     // then
// //     final EventProcessMappingResponseDto updatedMapping =
// //         eventProcessClient.getEventProcessMapping(eventProcessMappingId);
// //
// //     // then
// //     assertThat(updatedMapping.getMappings().values())
// //         .hasSize(1)
// //         .allSatisfy(
// //             mappings -> {
// //               assertThat(mappings.getStart().getEventLabel()).isEqualTo(firstLabel);
// //               assertThat(mappings.getEnd().getEventLabel()).isEqualTo(secondLabel);
// //             });
// //   }
// //
// //   @Test
// //   public void updateEventProcessMapping_withIdNotExists() {
// //     // when
// //     final Response response =
// //         eventProcessClient
// //             .createUpdateEventProcessMappingRequest(
// //                 "doesNotExist",
// eventProcessClient.buildEventProcessMappingDto(simpleDiagramXml))
// //             .execute();
// //
// //     // then the report is returned with expect
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
// //   }
// //
// //   @Test
// //   public void updateEventProcessMapping_withEventMappingIdNotExistInXml() {
// //     // given
// //     final String storedEventProcessMappingId =
// //         eventProcessClient.createEventProcessMapping(
// //             eventProcessClient.buildEventProcessMappingDto(simpleDiagramXml));
// //
// //     // when update event mappings with ID does not exist in XML
// //     final EventProcessMappingDto updateDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap(
// //                 "invalid_Id",
// //                 createEventMappingsDto(createMappedEventDto(), createMappedEventDto())),
// //             "process name",
// //             simpleDiagramXml);
// //     final Response response =
// //         eventProcessClient
// //             .createUpdateEventProcessMappingRequest(storedEventProcessMappingId, updateDto)
// //             .execute();
// //
// //     // then the update response code is correct
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void updateEventProcessMapping_multipleExternalEventSources() {
// //     // given
// //     final ExternalEventSourceEntryDto firstGroupSource =
// //         createExternalEventSourceEntryForGroup("firstGroup");
// //     final Map<String, EventMappingDto> eventMappings = new HashMap<>();
// //     eventMappings.put(
// //         USER_TASK_ID_ONE, createEventMappingsDto(createMappedEventDto(),
// createMappedEventDto()));
// //     eventMappings.put(USER_TASK_ID_TWO, createEventMappingsDto(createMappedEventDto(), null));
// //     eventMappings.put(USER_TASK_ID_THREE, createEventMappingsDto(null,
// createMappedEventDto()));
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsWithXmlAndEventSources(
// //             eventMappings,
// //             "process name",
// //             simpleDiagramXml,
// //             Collections.singletonList(firstGroupSource));
// //
// //     // when
// //     final String eventMappingId =
// //         eventProcessClient
// //             .createCreateEventProcessMappingRequest(eventProcessMappingDto)
// //             .execute(IdResponseDto.class, Response.Status.OK.getStatusCode())
// //             .getId();
// //     final List<EventSourceEntryDto<?>> updatedSources =
// //         Arrays.asList(firstGroupSource,
// createExternalEventSourceEntryForGroup("secondGroup"));
// //     eventProcessMappingDto.setEventSources(updatedSources);
// //     eventProcessClient.updateEventProcessMapping(eventMappingId, eventProcessMappingDto);
// //
// //     // then
// //     assertThat(eventProcessClient.getEventProcessMapping(eventMappingId))
// //         .extracting(EventProcessMappingResponseDto::getEventSources)
// //         .isEqualTo(updatedSources);
// //   }
// //
// //   @Test
// //   public void updateEventProcessMapping_withInvalidEventMappings() {
// //     // given existing event based process
// //     final String storedEventProcessMappingId =
// //         eventProcessClient.createEventProcessMapping(
// //             eventProcessClient.buildEventProcessMappingDto(simpleDiagramXml));
// //     final EventTypeDto invalidEventTypeDto =
// //         EventTypeDto.builder()
// //             .group(IdGenerator.getNextId())
// //             .source(IdGenerator.getNextId())
// //             .eventName(null)
// //             .build();
// //
// //     // when update event mappings with a mapped event with missing fields
// //     final EventProcessMappingDto updateDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap(
// //                 USER_TASK_ID_THREE,
// //                 createEventMappingsDto(invalidEventTypeDto, createMappedEventDto())),
// //             "process name",
// //             simpleDiagramXml);
// //     final Response response =
// //         eventProcessClient
// //             .createUpdateEventProcessMappingRequest(storedEventProcessMappingId, updateDto)
// //             .execute();
// //
// //     // then a bad request exception is thrown
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void updateEventProcessMapping_invalidModelXml() {
// //     // given existing event based process
// //     final String storedEventProcessMappingId =
// //         eventProcessClient.createEventProcessMapping(
// //             eventProcessClient.buildEventProcessMappingDto(simpleDiagramXml));
// //
// //     // when
// //     final EventProcessMappingDto updateDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap(
// //                 USER_TASK_ID_THREE,
// //                 createEventMappingsDto(createMappedEventDto(), createMappedEventDto())),
// //             "new process name",
// //             "some invalid BPMN xml");
// //     final Response response =
// //         eventProcessClient
// //             .createUpdateEventProcessMappingRequest(storedEventProcessMappingId, updateDto)
// //             .execute();
// //
// //     // then a bad request exception is thrown
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void updateEventProcessMapping_withEventMappingAndNoXmlPresent() {
// //     // given
// //     final String storedEventProcessMappingId =
// //         eventProcessClient.createEventProcessMapping(
// //             eventProcessClient.buildEventProcessMappingDto(null));
// //
// //     // when update event mappings and no XML present
// //     final EventProcessMappingDto updateDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.singletonMap(
// //                 "some_task_id",
// //                 createEventMappingsDto(createMappedEventDto(), createMappedEventDto())),
// //             "process name",
// //             null);
// //     final Response response =
// //         eventProcessClient
// //             .createUpdateEventProcessMappingRequest(storedEventProcessMappingId, updateDto)
// //             .execute();
// //
// //     // then the update response code is correct
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void updateEventProcessMapping_camundaSourceHasNoEventsImported() {
// //     // given a stored process mapping
// //     final String storedEventProcessMappingId =
// //         eventProcessClient.createEventProcessMapping(
// //             eventProcessClient.buildEventProcessMappingDto(null));
// //
// //     // and an update of event sources to one with no imported events
// //     final ProcessDefinitionEngineDto processDefinitionEngineDto =
// //         engineIntegrationExtension.deployProcessAndGetProcessDefinition(
// //             BpmnModels.getSimpleBpmnDiagram("someDefinition"));
// //     final EventProcessMappingDto updateDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             Collections.emptyMap(), "process name", null);
// //     updateDto.setEventSources(
// //         Collections.singletonList(
// //             createSimpleCamundaEventSourceEntry(processDefinitionEngineDto.getKey())));
// //
// //     // when
// //     final Response response =
// //         eventProcessClient
// //             .createUpdateEventProcessMappingRequest(storedEventProcessMappingId, updateDto)
// //             .execute();
// //
// //     // then a bad request exception is thrown
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void publishMappedEventProcessMapping() {
// //     // given
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     final String eventProcessId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     LocalDateUtil.setCurrentTime(OffsetDateTime.now());
// //     publishEventProcessMappingAndRefreshIndices(eventProcessId);
// //
// //     final EventProcessMappingResponseDto storedEventProcessMapping =
// //         eventProcessClient.getEventProcessMapping(eventProcessId);
// //
// //     // then
// //
// assertThat(storedEventProcessMapping.getState()).isEqualTo(EventProcessState.PUBLISH_PENDING);
// //     assertThat(storedEventProcessMapping.getPublishingProgress()).isEqualTo(0.0D);
// //
// //     final Optional<EventProcessPublishStateDto> publishStateDto =
// //         getEventProcessPublishStateDtoFromDatabase(eventProcessId);
// //     assertThat(publishStateDto)
// //         .get()
// //         .usingRecursiveComparison()
// //         .ignoringFields(
// //             EventProcessPublishStateDto.Fields.id,
// //             EventProcessPublishStateDto.Fields.eventImportSources)
// //         .isEqualTo(
// //             EventProcessPublishStateDto.builder()
// //                 .processMappingId(storedEventProcessMapping.getId())
// //                 .name(storedEventProcessMapping.getName())
// //                 .publishDateTime(LocalDateUtil.getCurrentDateTime())
// //                 .state(EventProcessState.PUBLISH_PENDING)
// //                 .publishProgress(0.0D)
// //                 .xml(storedEventProcessMapping.getXml())
// //                 .mappings(eventProcessMappingDto.getMappings())
// //                 .deleted(false)
// //                 .build());
// //     assertThat(publishStateDto)
// //         .get()
// //         .extracting(EventProcessPublishStateDto::getEventImportSources)
// //         .asList()
// //         .hasSize(1)
// //         .containsExactly(
// //             EventImportSourceDto.builder()
// //                 .firstEventForSourceAtTimeOfPublishTimestamp(
// //                     OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L),
// ZoneId.systemDefault()))
// //                 .lastEventForSourceAtTimeOfPublishTimestamp(
// //                     OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L),
// ZoneId.systemDefault()))
// //                 .lastImportedEventTimestamp(
// //                     OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L),
// ZoneId.systemDefault()))
// //                 .eventImportSourceType(
// //                     storedEventProcessMapping.getEventSources().get(0).getSourceType())
// //                 .eventSourceConfigurations(
// //                     Collections.singletonList(
// //
// storedEventProcessMapping.getEventSources().get(0).getConfiguration()))
// //                 .build());
// //   }
// //
// //   @Test
// //   public void publishUnpublishedChangesEventProcessMapping() {
// //     // given
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     final String eventProcessId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     publishMappingAndExecuteImport(eventProcessId);
// //     final EventProcessPublishStateDto publishState =
// getEventProcessPublishStateDto(eventProcessId);
// //     assertThat(eventInstanceIndexForPublishStateExists(publishState)).isTrue();
// //
// //     final EventProcessMappingDto updateDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             eventProcessMappingDto.getMappings(), "new process name", simpleDiagramXml);
// //     eventProcessClient.updateEventProcessMapping(eventProcessId, updateDto);
// //
// //     LocalDateUtil.setCurrentTime(OffsetDateTime.now().plusSeconds(1));
// //     publishMappingAndExecuteImport(eventProcessId);
// //     assertThat(eventInstanceIndexForPublishStateExists(publishState)).isFalse();
// //
// //     final EventProcessMappingResponseDto republishedEventProcessMapping =
// //         eventProcessClient.getEventProcessMapping(eventProcessId);
// //     final EventProcessPublishStateDto republishedPublishState =
// //         getEventProcessPublishStateDto(eventProcessId);
// //     assertThat(eventInstanceIndexForPublishStateExists(republishedPublishState)).isTrue();
// //
// //     // then
// //     assertThat(republishedEventProcessMapping.getState())
// //         .isEqualTo(EventProcessState.PUBLISH_PENDING);
// //     assertThat(republishedEventProcessMapping.getPublishingProgress()).isEqualTo(0.0D);
// //
// //     assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessId))
// //         .get()
// //         .hasFieldOrPropertyWithValue(EventProcessPublishStateDto.Fields.xml,
// updateDto.getXml())
// //         .hasFieldOrPropertyWithValue(
// //             EventProcessPublishStateDto.Fields.publishDateTime,
// LocalDateUtil.getCurrentDateTime());
// //   }
// //
// //   @Test
// //   public void publishUnmappedEventProcessMapping_fails() {
// //     // given
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             null, "unmapped", simpleDiagramXml);
// //     final String eventProcessId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     final ErrorResponseDto errorResponse =
// //         eventProcessClient
// //             .createPublishEventProcessMappingRequest(eventProcessId)
// //             .execute(ErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());
// //
// //     final EventProcessMappingResponseDto actual =
// //         eventProcessClient.getEventProcessMapping(eventProcessId);
// //
// //     // then
// //     assertThat(errorResponse.getErrorCode()).isEqualTo("invalidEventProcessState");
// //
// //     assertThat(actual.getState()).isEqualTo(EventProcessState.UNMAPPED);
// //     assertThat(actual.getPublishingProgress()).isNull();
// //
// //     assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessId)).isEmpty();
// //   }
// //
// //   @Test
// //   public void publishPublishPendingEventProcessMapping_fails() {
// //     // given
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     final String eventProcessId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     publishEventProcessMappingAndRefreshIndices(eventProcessId);
// //     final OffsetDateTime firstPublishDate =
// //         getPublishedDateForEventProcessMappingOrFail(eventProcessId);
// //
// //     // when
// //     final ErrorResponseDto errorResponse =
// //         eventProcessClient
// //             .createPublishEventProcessMappingRequest(eventProcessId)
// //             .execute(ErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());
// //
// //     final EventProcessMappingResponseDto actual =
// //         eventProcessClient.getEventProcessMapping(eventProcessId);
// //
// //     // then
// //     assertThat(errorResponse.getErrorCode()).isEqualTo("invalidEventProcessState");
// //
// //     assertThat(actual.getState()).isEqualTo(EventProcessState.PUBLISH_PENDING);
// //     assertThat(actual.getPublishingProgress()).isEqualTo(0.0D);
// //
// //     assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessId))
// //         .get()
// //         .hasFieldOrPropertyWithValue(
// //             EventProcessPublishStateDto.Fields.publishDateTime, firstPublishDate);
// //   }
// //
// //   @Test
// //   public void publishedEventProcessMapping_failsIfNotExists() {
// //     // given
// //
// //     // when
// //     final ErrorResponseDto errorResponse =
// //         eventProcessClient
// //             .createPublishEventProcessMappingRequest("notExistingId")
// //             .execute(ErrorResponseDto.class, Response.Status.NOT_FOUND.getStatusCode());
// //
// //     // then
// //     assertThat(errorResponse.getErrorCode()).isEqualTo("notFoundError");
// //   }
// //
// //   @Test
// //   public void cancelPublishPendingEventProcessMapping() {
// //     // given
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     final String eventProcessId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     publishEventProcessMappingAndRefreshIndices(eventProcessId);
// //
// //     // when
// //     eventProcessClient.cancelPublishEventProcessMapping(eventProcessId);
// //
// //     final EventProcessMappingResponseDto actual =
// //         eventProcessClient.getEventProcessMapping(eventProcessId);
// //
// //     // then
// //     assertThat(actual.getState()).isEqualTo(EventProcessState.MAPPED);
// //     assertThat(actual.getPublishingProgress()).isNull();
// //
// //     assertThat(getEventProcessPublishStateDtoFromDatabase(eventProcessId)).isEmpty();
// //   }
// //
// //   @Test
// //   public void cancelPublishUnmappedEventProcessMapping_fails() {
// //     // given
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //             null, "unmapped", simpleDiagramXml);
// //     final String eventProcessId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     final ErrorResponseDto errorResponse =
// //         eventProcessClient
// //             .createPublishEventProcessMappingRequest(eventProcessId)
// //             .execute(ErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());
// //
// //     // then
// //     assertThat(errorResponse.getErrorCode()).isEqualTo("invalidEventProcessState");
// //   }
// //
// //   @Test
// //   public void cancelPublishMappedEventProcessMapping_fails() {
// //     // given
// //     final EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     final String eventProcessId =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     final ErrorResponseDto errorResponse =
// //         eventProcessClient
// //             .createCancelPublishEventProcessMappingRequest(eventProcessId)
// //             .execute(ErrorResponseDto.class, Response.Status.BAD_REQUEST.getStatusCode());
// //
// //     // then
// //     assertThat(errorResponse.getErrorCode()).isEqualTo("invalidEventProcessState");
// //   }
// //
// //   @Test
// //   public void cancelPublishedEventProcessMapping_failsIfNotExists() {
// //     // given
// //
// //     // when
// //     final ErrorResponseDto errorResponse =
// //         eventProcessClient
// //             .createCancelPublishEventProcessMappingRequest("notExistingId")
// //             .execute(ErrorResponseDto.class, Response.Status.NOT_FOUND.getStatusCode());
// //
// //     // then
// //     assertThat(errorResponse.getErrorCode()).isEqualTo("notFoundError");
// //   }
// //
// //   @NonNull
// //   private OffsetDateTime getPublishedDateForEventProcessMappingOrFail(final String
// eventProcessId) {
// //     return getEventProcessPublishStateDtoFromDatabase(eventProcessId)
// //         .orElseThrow(
// //             () -> new OptimizeIntegrationTestException("Failed reading first publish date"))
// //         .getPublishDateTime();
// //   }
// //
// //   private static EventTypeDto createMappedEventDtoWithLabel(final String eventLabel) {
// //     return EventTypeDto.builder()
// //         .group(IdGenerator.getNextId())
// //         .source(IdGenerator.getNextId())
// //         .eventName(IdGenerator.getNextId())
// //         .eventLabel(eventLabel)
// //         .build();
// //   }
// //
// //   @SneakyThrows
// //   public static String createProcessDefinitionXml() {
// //     final BpmnModelInstance bpmnModel =
// //         Bpmn.createExecutableProcess("aProcess")
// //             .camundaVersionTag("aVersionTag")
// //             .name("aProcessName")
// //             .startEvent(BPMN_START_EVENT_ID)
// //             .userTask(USER_TASK_ID_ONE)
// //             .userTask(USER_TASK_ID_TWO)
// //             .userTask(USER_TASK_ID_THREE)
// //             .endEvent("endEvent_ID")
// //             .done();
// //     final ByteArrayOutputStream xmlOutput = new ByteArrayOutputStream();
// //     Bpmn.writeModelToStream(xmlOutput, bpmnModel);
// //     return new String(xmlOutput.toByteArray(), StandardCharsets.UTF_8);
// //   }
// //
// //   private EventProcessMappingDto
// //       createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource() {
// //     return eventProcessClient.buildEventProcessMappingDtoWithMappingsAndExternalEventSource(
// //         Collections.singletonMap(
// //             USER_TASK_ID_THREE,
// //             createEventMappingsDto(createMappedEventDto(), createMappedEventDto())),
// //         "process name",
// //         simpleDiagramXml);
// //   }
// // }
