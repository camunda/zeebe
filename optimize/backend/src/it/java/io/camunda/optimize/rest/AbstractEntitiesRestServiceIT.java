/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.rest;
//
// import static io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto.Fields.entityType;
// import static
// io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto.Fields.lastModified;
// import static
// io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto.Fields.lastModifier;
// import static io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto.Fields.name;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// import static io.camunda.optimize.util.SuppressionConstants.UNUSED;
//
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.optimize.ReportType;
// import io.camunda.optimize.dto.optimize.query.IdResponseDto;
// import io.camunda.optimize.dto.optimize.query.collection.PartialCollectionDefinitionRequestDto;
// import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
// import io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto;
// import io.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionRequestDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionRequestDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
// import io.camunda.optimize.dto.optimize.query.sorting.SortOrder;
// import jakarta.ws.rs.core.Response;
// import java.util.Comparator;
// import java.util.stream.Stream;
// import org.junit.jupiter.params.provider.Arguments;
//
// public abstract class AbstractEntitiesRestServiceIT extends AbstractPlatformIT {
//
//   protected String addCollection(final String collectionName) {
//     final String collectionId = collectionClient.createNewCollection();
//     collectionClient.updateCollection(
//         collectionId, new PartialCollectionDefinitionRequestDto(collectionName));
//     return collectionId;
//   }
//
//   protected String addSingleReportToOptimize(final String name, final ReportType reportType) {
//     return addSingleReportToOptimize(name, null, reportType, null, DEFAULT_USERNAME);
//   }
//
//   protected String addSingleReportToOptimize(
//       final String name,
//       final ReportType reportType,
//       final String collectionId,
//       final String user) {
//     return addSingleReportToOptimize(name, null, reportType, collectionId, user);
//   }
//
//   protected String addSingleReportToOptimize(
//       final String name,
//       final String description,
//       final ReportType reportType,
//       final String collectionId,
//       final String user) {
//     switch (reportType) {
//       case PROCESS:
//         SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionDto =
//             new SingleProcessReportDefinitionRequestDto();
//         singleProcessReportDefinitionDto.setName(name);
//         singleProcessReportDefinitionDto.setDescription(description);
//         singleProcessReportDefinitionDto.setCollectionId(collectionId);
//         return reportClient.createSingleProcessReportAsUser(
//             singleProcessReportDefinitionDto, user, user);
//       case DECISION:
//         SingleDecisionReportDefinitionRequestDto singleDecisionReportDefinitionDto =
//             new SingleDecisionReportDefinitionRequestDto();
//         singleDecisionReportDefinitionDto.setName(name);
//         singleDecisionReportDefinitionDto.setDescription(description);
//         singleDecisionReportDefinitionDto.setCollectionId(collectionId);
//         return reportClient.createNewDecisionReportAsUser(
//             singleDecisionReportDefinitionDto, user, user);
//       default:
//         throw new IllegalStateException("ReportType not allowed!");
//     }
//   }
//
//   protected String addDashboardToOptimize(String name) {
//     return addDashboardToOptimize(name, null, DEFAULT_USERNAME);
//   }
//
//   protected String addDashboardToOptimize(String name, String collectionId, String user) {
//     return addDashboardToOptimize(name, null, collectionId, user);
//   }
//
//   protected String addDashboardToOptimize(
//       final String name, final String description, final String collectionId, final String user)
// {
//     DashboardDefinitionRestDto dashboardDefinitionDto = new DashboardDefinitionRestDto();
//     dashboardDefinitionDto.setName(name);
//     dashboardDefinitionDto.setDescription(description);
//     dashboardDefinitionDto.setCollectionId(collectionId);
//     return dashboardClient.createDashboardAsUser(dashboardDefinitionDto, user, user);
//   }
//
//   protected String addCombinedReport(String name) {
//     return addCombinedReport(name, null);
//   }
//
//   protected String addCombinedReport(String name, String collectionId) {
//     CombinedReportDefinitionRequestDto combinedReportDefinitionDto =
//         new CombinedReportDefinitionRequestDto();
//     combinedReportDefinitionDto.setName(name);
//     combinedReportDefinitionDto.setCollectionId(collectionId);
//     return embeddedOptimizeExtension
//         .getRequestExecutor()
//         .buildCreateCombinedReportRequest(combinedReportDefinitionDto)
//         .withUserAuthentication(DEFAULT_USERNAME, DEFAULT_PASSWORD)
//         .execute(IdResponseDto.class, Response.Status.OK.getStatusCode())
//         .getId();
//   }
//
//   @SuppressWarnings(UNUSED)
//   protected static Stream<Arguments> sortParamsAndExpectedComparator() {
//     return Stream.of(
//         Arguments.of(name, SortOrder.ASC, Comparator.comparing(EntityResponseDto::getName)),
//         Arguments.of(
//             name, SortOrder.DESC, Comparator.comparing(EntityResponseDto::getName).reversed()),
//         Arguments.of(
//             entityType, SortOrder.ASC, Comparator.comparing(EntityResponseDto::getEntityType)),
//         Arguments.of(
//             entityType,
//             SortOrder.DESC,
//             Comparator.comparing(EntityResponseDto::getEntityType).reversed()),
//         Arguments.of(
//             lastModified, SortOrder.ASC,
// Comparator.comparing(EntityResponseDto::getLastModified)),
//         Arguments.of(
//             lastModified,
//             SortOrder.DESC,
//             Comparator.comparing(EntityResponseDto::getLastModified).reversed()),
//         Arguments.of(
//             lastModifier, SortOrder.ASC,
// Comparator.comparing(EntityResponseDto::getLastModifier)),
//         Arguments.of(
//             lastModifier,
//             SortOrder.DESC,
//             Comparator.comparing(EntityResponseDto::getLastModifier).reversed()));
//   }
// }
