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
// // import static io.camunda.optimize.dto.optimize.DefinitionType.PROCESS;
// // import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
// // import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// // import static
// io.camunda.optimize.rest.eventprocess.EventBasedProcessRestServiceIT.createProcessDefinitionXml;
// // import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
// // import static io.camunda.optimize.test.optimize.EventProcessClient.createEventMappingsDto;
// // import static io.camunda.optimize.test.optimize.EventProcessClient.createMappedEventDto;
// // import static org.assertj.core.api.Assertions.assertThat;
// //
// // import io.camunda.optimize.dto.optimize.query.event.process.EventProcessMappingDto;
// // import io.camunda.optimize.service.importing.eventprocess.AbstractEventProcessIT;
// // import jakarta.ws.rs.core.Response;
// // import java.util.ArrayList;
// // import java.util.Arrays;
// // import java.util.Collections;
// // import org.junit.jupiter.api.BeforeAll;
// // import org.junit.jupiter.api.Tag;
// // import org.junit.jupiter.api.Test;
// //
// // @Tag(OPENSEARCH_PASSING)
// // public class EventBasedProcessConflictIT extends AbstractEventProcessIT {
// //
// //   private static String simpleDiagramXml;
// //
// //   @BeforeAll
// //   public static void setup() {
// //     simpleDiagramXml = createProcessDefinitionXml();
// //   }
// //
// //   @Test
// //   public void checkDeleteConflictsForBulkDeleteOfEventProcessMappings_withUnauthorizedUser() {
// //     // when
// //     authorizationClient.addKermitUserAndGrantAccessToOptimize();
// //     Response response =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildCheckBulkDeleteConflictsForEventProcessMappingRequest(new ArrayList<>())
// //             .withUserAuthentication(KERMIT_USER, KERMIT_USER)
// //             .execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
// //   }
// //
// //   @Test
// //   public void
// checkDeleteConflictsForBulkDeleteOfEventProcessMappings_nullEventBasedProcessList() {
// //     // when
// //     authorizationClient.addKermitUserAndGrantAccessToOptimize();
// //     Response response =
// //         embeddedOptimizeExtension
// //             .getRequestExecutor()
// //             .buildCheckBulkDeleteConflictsForEventProcessMappingRequest(null)
// //             .execute();
// //
// //     // then
// //     assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
// //   }
// //
// //   @Test
// //   public void bulkDeleteConflictsForEventProcessMapping_alertConflict() {
// //     // given
// //     EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     String eventProcessDefinitionKey1 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //     String eventProcessDefinitionKey2 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //     String eventProcessDefinitionKey3 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     eventProcessClient.publishEventProcessMapping(eventProcessDefinitionKey1);
// //
// //     String collectionId = createNewCollectionWithScope(eventProcessDefinitionKey1);
// //     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
// //
// //     String reportId1 =
// //         reportClient.createSingleReport(
// //             collectionId, PROCESS, eventProcessDefinitionKey1, Collections.emptyList());
// //     alertClient.createAlertForReport(reportId1);
// //
// //     // when
// //     boolean response =
// //         eventProcessClient.eventProcessMappingRequestBulkDeleteHasConflicts(
// //             Arrays.asList(
// //                 eventProcessDefinitionKey1,
// //                 eventProcessDefinitionKey2,
// //                 eventProcessDefinitionKey3));
// //
// //     // then
// //     assertThat(response).isTrue();
// //   }
// //
// //   @Test
// //   public void bulkDeleteConflictsForEventProcessMapping_combinedReportConflict() {
// //     // given
// //     EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     String eventProcessDefinitionKey1 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //     String eventProcessDefinitionKey2 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //     String eventProcessDefinitionKey3 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     eventProcessClient.publishEventProcessMapping(eventProcessDefinitionKey1);
// //     eventProcessClient.publishEventProcessMapping(eventProcessDefinitionKey2);
// //     eventProcessClient.publishEventProcessMapping(eventProcessDefinitionKey3);
// //
// //     String collectionId = createNewCollectionWithScope(eventProcessDefinitionKey1);
// //     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
// //
// //     String reportId1 =
// //         reportClient.createSingleReport(
// //             collectionId, PROCESS, eventProcessDefinitionKey1, Collections.emptyList());
// //     String reportId2 =
// //         reportClient.createSingleReport(
// //             collectionId, PROCESS, eventProcessDefinitionKey1, Collections.emptyList());
// //
// //     reportClient.createCombinedReport(collectionId, Arrays.asList(reportId1, reportId2));
// //
// //     // when
// //     boolean response =
// //         eventProcessClient.eventProcessMappingRequestBulkDeleteHasConflicts(
// //             Arrays.asList(
// //                 eventProcessDefinitionKey1,
// //                 eventProcessDefinitionKey2,
// //                 eventProcessDefinitionKey3));
// //
// //     // then
// //     assertThat(response).isTrue();
// //   }
// //
// //   @Test
// //   public void bulkDeleteConflictsForEventProcessMapping_dashboardConflict() {
// //     // given
// //     EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     String eventProcessDefinitionKey1 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //     String eventProcessDefinitionKey2 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //     String eventProcessDefinitionKey3 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     eventProcessClient.publishEventProcessMapping(eventProcessDefinitionKey1);
// //     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
// //     String collectionId = createNewCollectionWithScope(eventProcessDefinitionKey1);
// //
// //     String reportId1 =
// //         reportClient.createSingleReport(
// //             collectionId, PROCESS, eventProcessDefinitionKey1, Collections.emptyList());
// //     String reportId2 = reportClient.createEmptySingleProcessReport();
// //     dashboardClient.createDashboard(collectionId, Arrays.asList(reportId1, reportId2));
// //
// //     // when
// //     boolean response =
// //         eventProcessClient.eventProcessMappingRequestBulkDeleteHasConflicts(
// //             Arrays.asList(
// //                 eventProcessDefinitionKey1,
// //                 eventProcessDefinitionKey2,
// //                 eventProcessDefinitionKey3));
// //
// //     // then
// //     assertThat(response).isTrue();
// //   }
// //
// //   @Test
// //   public void bulkDeleteConflictsForEventProcessMapping_noConflicts() {
// //     // given
// //     EventProcessMappingDto eventProcessMappingDto =
// //         createEventProcessMappingDtoWithSimpleMappingsAndExternalEventSource();
// //     String eventProcessDefinitionKey1 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //     String eventProcessDefinitionKey2 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //     String eventProcessDefinitionKey3 =
// //         eventProcessClient.createEventProcessMapping(eventProcessMappingDto);
// //
// //     // when
// //     boolean response =
// //         eventProcessClient.eventProcessMappingRequestBulkDeleteHasConflicts(
// //             Arrays.asList(
// //                 eventProcessDefinitionKey1,
// //                 eventProcessDefinitionKey2,
// //                 eventProcessDefinitionKey3));
// //
// //     // then
// //     assertThat(response).isFalse();
// //   }
// //
// //   private String createNewCollectionWithScope(String eventProcessDefinitionKey) {
// //     return collectionClient.createNewCollectionWithScope(
// //         DEFAULT_USERNAME,
// //         DEFAULT_PASSWORD,
// //         PROCESS,
// //         eventProcessDefinitionKey,
// //         Collections.emptyList());
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
