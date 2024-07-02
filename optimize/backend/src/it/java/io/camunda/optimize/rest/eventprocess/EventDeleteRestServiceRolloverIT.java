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
// // import static org.assertj.core.api.Assertions.assertThat;
// //
// // import io.camunda.optimize.dto.optimize.ProcessInstanceDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.EventProcessInstanceDto;
// // import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceDto;
// // import io.camunda.optimize.dto.optimize.rest.CloudEventRequestDto;
// // import jakarta.ws.rs.core.Response;
// // import java.util.Arrays;
// // import java.util.Collections;
// // import java.util.List;
// // import java.util.stream.Collectors;
// // import org.junit.jupiter.api.Tag;
// // import org.junit.jupiter.api.Test;
// //
// // @Tag(OPENSEARCH_PASSING)
// // public class EventDeleteRestServiceRolloverIT extends AbstractEventRestServiceRolloverIT {
// //
// //   @Test
// //   public void deleteRolledOverEvents_deleteSingleEventUsedInSingleEventInstance() {
// //     // given
// //     ingestEventAndRolloverIndex(impostorSabotageNav);
// //     ingestEventAndRolloverIndex(impostorMurderedMedBay);
// //     ingestEventAndRolloverIndex(normieTaskNav);
// //     final List<CloudEventRequestDto> instanceEvents =
// //         Arrays.asList(impostorSabotageNav, impostorMurderedMedBay);
// //     final ProcessInstanceDto instance =
// //         createAndSaveEventInstanceContainingEvents(instanceEvents, "indexId");
// //     final List<EventDto> allSavedEventsBeforeDelete = getAllStoredEvents();
// //     final List<String> eventIdsToDelete =
// Collections.singletonList(instanceEvents.get(0).getId());
// //     assertEventInstanceContainsAllEventsOfIds(
// //         getSavedInstanceWithId(instance.getProcessInstanceId()), eventIdsToDelete);
// //
// //     // when
// //     final Response response =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildDeleteEventsRequest(eventIdsToDelete)
// //             .execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //     assertEventInstancesDoNotContainAnyEventsOfIds(getAllStoredEventInstances(),
// eventIdsToDelete);
// //     assertThatEventsHaveBeenDeleted(allSavedEventsBeforeDelete, eventIdsToDelete);
// //   }
// //
// //   @Test
// //   public void deleteRolledOverEvents_deleteMultipleEventsUsedInEventInstance() {
// //     // given
// //     ingestEventAndRolloverIndex(impostorSabotageNav);
// //     ingestEventAndRolloverIndex(impostorMurderedMedBay);
// //     ingestEventAndRolloverIndex(normieTaskNav);
// //     final List<CloudEventRequestDto> instanceEvents =
// //         Arrays.asList(impostorSabotageNav, impostorMurderedMedBay);
// //     final ProcessInstanceDto instance =
// //         createAndSaveEventInstanceContainingEvents(instanceEvents, "indexId");
// //     final List<EventDto> allSavedEventsBeforeDelete = getAllStoredEvents();
// //     final List<String> eventIdsToDelete =
// //         instanceEvents.stream().map(CloudEventRequestDto::getId).collect(Collectors.toList());
// //     assertEventInstanceContainsAllEventsOfIds(
// //         getSavedInstanceWithId(instance.getProcessInstanceId()), eventIdsToDelete);
// //
// //     // when
// //     final Response response =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildDeleteEventsRequest(eventIdsToDelete)
// //             .execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //     assertEventInstancesDoNotContainAnyEventsOfIds(getAllStoredEventInstances(),
// eventIdsToDelete);
// //     assertThatEventsHaveBeenDeleted(allSavedEventsBeforeDelete, eventIdsToDelete);
// //   }
// //
// //   @Test
// //   public void deleteRolledOverEvents_deleteSingleEventUsedInMultipleEventInstances() {
// //     // given
// //     ingestEventAndRolloverIndex(impostorSabotageNav);
// //     ingestEventAndRolloverIndex(impostorMurderedMedBay);
// //     ingestEventAndRolloverIndex(normieTaskNav);
// //     final List<CloudEventRequestDto> instanceEvents =
// //         Arrays.asList(impostorSabotageNav, impostorMurderedMedBay);
// //     final ProcessInstanceDto firstInstance =
// //         createAndSaveEventInstanceContainingEvents(instanceEvents, "indexId");
// //     final ProcessInstanceDto secondInstance =
// //         createAndSaveEventInstanceContainingEvents(instanceEvents, "indexId");
// //     final List<EventDto> allSavedEventsBeforeDelete = getAllStoredEvents();
// //     final List<String> eventIdsToDelete =
// Collections.singletonList(instanceEvents.get(0).getId());
// //     assertEventInstanceContainsAllEventsOfIds(
// //         getSavedInstanceWithId(firstInstance.getProcessInstanceId()), eventIdsToDelete);
// //     assertEventInstanceContainsAllEventsOfIds(
// //         getSavedInstanceWithId(secondInstance.getProcessInstanceId()), eventIdsToDelete);
// //
// //     // when
// //     final Response response =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildDeleteEventsRequest(eventIdsToDelete)
// //             .execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
// //     assertEventInstancesDoNotContainAnyEventsOfIds(getAllStoredEventInstances(),
// eventIdsToDelete);
// //     assertThatEventsHaveBeenDeleted(allSavedEventsBeforeDelete, eventIdsToDelete);
// //   }
// //
// //   private void assertEventInstanceContainsAllEventsOfIds(
// //       final EventProcessInstanceDto eventInstance, final List<String> eventIds) {
// //     assertThat(eventInstance)
// //         .satisfies(
// //             storedInstance -> {
// //               assertThat(storedInstance.getFlowNodeInstances())
// //                   .extracting(FlowNodeInstanceDto::getFlowNodeInstanceId)
// //                   .containsAll(eventIds);
// //             });
// //   }
// //
// //   private void assertEventInstancesDoNotContainAnyEventsOfIds(
// //       final List<EventProcessInstanceDto> eventInstances, final List<String> eventIds) {
// //     assertThat(eventInstances)
// //         .isNotEmpty()
// //         .allSatisfy(
// //             storedInstance -> {
// //               assertThat(storedInstance.getFlowNodeInstances())
// //                   .extracting(FlowNodeInstanceDto::getFlowNodeInstanceId)
// //                   .doesNotContainAnyElementsOf(eventIds);
// //             });
// //   }
// // }
