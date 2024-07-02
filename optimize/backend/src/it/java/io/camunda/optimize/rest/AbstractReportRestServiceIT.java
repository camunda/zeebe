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
// import static io.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
// import static io.camunda.optimize.dto.optimize.ReportConstants.DEFAULT_TENANT_IDS;
//
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.optimize.ReportType;
// import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
// import io.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionRequestDto;
// import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
// import
// io.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
// import io.camunda.optimize.service.util.ProcessReportDataType;
// import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
// import io.camunda.optimize.test.util.decision.DecisionReportDataBuilder;
// import io.camunda.optimize.test.util.decision.DecisionReportDataType;
// import java.time.OffsetDateTime;
// import java.util.Collections;
// import java.util.List;
//
// public abstract class AbstractReportRestServiceIT extends AbstractPlatformIT {
//
//   protected static final String PROCESS_DEFINITION_KEY = "simple";
//   protected static final String DECISION_DEFINITION_KEY = "invoiceClassification";
//   protected static final String RANDOM_KEY = "someRandomKey";
//   protected static final String RANDOM_VERSION = "someRandomVersion";
//   protected static final String RANDOM_STRING = "something";
//
//   protected String addReportToOptimizeWithDefinitionAndRandomXml(final ReportType reportType) {
//     switch (reportType) {
//       case PROCESS:
//         ProcessReportDataDto processReportDataDto =
//             TemplatedProcessReportDataBuilder.createReportData()
//                 .setProcessDefinitionKey(RANDOM_KEY)
//                 .setProcessDefinitionVersion(RANDOM_VERSION)
//                 .setReportDataType(ProcessReportDataType.RAW_DATA)
//                 .build();
//         processReportDataDto.getConfiguration().setXml(RANDOM_STRING);
//         return addSingleProcessReportWithDefinition(processReportDataDto, null);
//       case DECISION:
//         DecisionReportDataDto decisionReportDataDto =
//             DecisionReportDataBuilder.create()
//                 .setDecisionDefinitionKey(RANDOM_KEY)
//                 .setDecisionDefinitionVersion(RANDOM_VERSION)
//                 .setReportDataType(DecisionReportDataType.RAW_DATA)
//                 .build();
//         decisionReportDataDto.getConfiguration().setXml(RANDOM_STRING);
//         return addSingleDecisionReportWithDefinition(decisionReportDataDto, null);
//     }
//     return null;
//   }
//
//   protected String addSingleProcessReportWithDefinition(
//       final ProcessReportDataDto processReportDataDto) {
//     return addSingleProcessReportWithDefinition(processReportDataDto, null);
//   }
//
//   protected String addSingleProcessReportWithDefinition(
//       final ProcessReportDataDto processReportDataDto, final String collectionId) {
//     return addSingleProcessReportWithDefinition(processReportDataDto, null, collectionId);
//   }
//
//   protected String addSingleProcessReportWithDefinition(
//       final ProcessReportDataDto processReportDataDto,
//       final String description,
//       final String collectionId) {
//     SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionDto =
//         createSingleProcessReportDefinitionRequestDto(
//             processReportDataDto, description, collectionId);
//     return reportClient.createSingleProcessReport(singleProcessReportDefinitionDto);
//   }
//
//   protected static SingleProcessReportDefinitionRequestDto
//       createSingleProcessReportDefinitionRequestDto(
//           final ProcessReportDataDto processReportDataDto,
//           final String description,
//           final String collectionId) {
//     SingleProcessReportDefinitionRequestDto singleProcessReportDefinitionDto =
//         new SingleProcessReportDefinitionRequestDto();
//     singleProcessReportDefinitionDto.setData(processReportDataDto);
//     singleProcessReportDefinitionDto.setId(RANDOM_STRING);
//     singleProcessReportDefinitionDto.setLastModifier(RANDOM_STRING);
//     singleProcessReportDefinitionDto.setName(RANDOM_STRING);
//     singleProcessReportDefinitionDto.setDescription(description);
//     OffsetDateTime someDate = OffsetDateTime.now().plusHours(1);
//     singleProcessReportDefinitionDto.setCreated(someDate);
//     singleProcessReportDefinitionDto.setLastModified(someDate);
//     singleProcessReportDefinitionDto.setOwner(RANDOM_STRING);
//     singleProcessReportDefinitionDto.setCollectionId(collectionId);
//     return singleProcessReportDefinitionDto;
//   }
//
//   protected String addSingleDecisionReportWithDefinition(
//       final DecisionReportDataDto decisionReportDataDto) {
//     return addSingleDecisionReportWithDefinition(decisionReportDataDto, null);
//   }
//
//   protected String addSingleDecisionReportWithDefinition(
//       final DecisionReportDataDto decisionReportDataDto, final String collectionId) {
//     return addSingleDecisionReportWithDefinition(decisionReportDataDto, null, collectionId);
//   }
//
//   protected String addSingleDecisionReportWithDefinition(
//       final DecisionReportDataDto decisionReportDataDto,
//       final String description,
//       final String collectionId) {
//     SingleDecisionReportDefinitionRequestDto singleDecisionReportDefinitionDto =
//         createSingleDecisionReportDefinitionRequestDto(
//             decisionReportDataDto, description, collectionId);
//     return reportClient.createSingleDecisionReport(singleDecisionReportDefinitionDto);
//   }
//
//   protected static SingleDecisionReportDefinitionRequestDto
//       createSingleDecisionReportDefinitionRequestDto(
//           final DecisionReportDataDto decisionReportDataDto,
//           final String description,
//           final String collectionId) {
//     SingleDecisionReportDefinitionRequestDto singleDecisionReportDefinitionDto =
//         new SingleDecisionReportDefinitionRequestDto();
//     singleDecisionReportDefinitionDto.setData(decisionReportDataDto);
//     singleDecisionReportDefinitionDto.setId(RANDOM_STRING);
//     singleDecisionReportDefinitionDto.setLastModifier(RANDOM_STRING);
//     singleDecisionReportDefinitionDto.setName(RANDOM_STRING);
//     singleDecisionReportDefinitionDto.setDescription(description);
//     OffsetDateTime someDate = OffsetDateTime.now().plusHours(1);
//     singleDecisionReportDefinitionDto.setCreated(someDate);
//     singleDecisionReportDefinitionDto.setLastModified(someDate);
//     singleDecisionReportDefinitionDto.setOwner(RANDOM_STRING);
//     singleDecisionReportDefinitionDto.setCollectionId(collectionId);
//     return singleDecisionReportDefinitionDto;
//   }
//
//   protected List<ReportDataDefinitionDto> createSingleDefinitionListWithIdentifier(
//       final String definitionIdentifier) {
//     return List.of(
//         new ReportDataDefinitionDto(
//             definitionIdentifier,
//             RANDOM_KEY,
//             RANDOM_STRING,
//             RANDOM_STRING,
//             Collections.singletonList(ALL_VERSIONS),
//             DEFAULT_TENANT_IDS));
//   }
// }
