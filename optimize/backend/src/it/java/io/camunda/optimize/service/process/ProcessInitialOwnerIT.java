/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
// TODO recreate C8 IT equivalent of this with #13337
// package io.camunda.optimize.service.process;
//
// import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
// import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
// import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_OVERVIEW_INDEX_NAME;
// import static
// io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_PROCESS_DEFINITION;
// import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_USER;
// import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
// import static io.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.camunda.optimize.AbstractPlatformIT;
// import io.camunda.optimize.dto.optimize.query.processoverview.InitialProcessOwnerDto;
// import io.camunda.optimize.dto.optimize.query.processoverview.ProcessDigestRequestDto;
// import io.camunda.optimize.dto.optimize.query.processoverview.ProcessOverviewDto;
// import io.camunda.optimize.dto.optimize.query.processoverview.ProcessOverviewResponseDto;
// import io.camunda.optimize.dto.optimize.query.processoverview.ProcessOwnerResponseDto;
// import io.camunda.optimize.dto.optimize.query.processoverview.ProcessUpdateDto;
// import io.camunda.optimize.dto.optimize.rest.sorting.ProcessOverviewSorter;
// import io.camunda.optimize.exception.OptimizeIntegrationTestException;
// import io.camunda.optimize.service.onboarding.OnboardingSchedulerService;
// import jakarta.ws.rs.core.Response;
// import java.util.Collections;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.TimeUnit;
// import java.util.stream.Collectors;
// import org.awaitility.Awaitility;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
//
// @Tag(OPENSEARCH_PASSING)
// public class ProcessInitialOwnerIT extends AbstractPlatformIT {
//
//   private static final String DEF_KEY = "def_key";
//
//   @Test
//   public void setInitialOwner_processDoesNotExistYet() {
//     // given
//     final String defKey = "unborn_process";
//
//     // when
//     // Make sure process definition is not there yet
//     assertThat(definitionClient.getAllDefinitions()).isEmpty();
//     final Response responseInitialOwner =
//         processOverviewClient.setInitialProcessOwner(defKey, DEFAULT_USERNAME);
//     // Now only we deploy the process
//     deploySimpleProcessDefinition(defKey);
//     importAllEngineEntitiesFromScratch();
//
//     final OnboardingSchedulerService onboardingSchedulerService =
//
// embeddedOptimizeExtension.getApplicationContext().getBean(OnboardingSchedulerService.class);
//     // Process the pending data
//     onboardingSchedulerService.onboardNewProcesses();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertExpectedProcessOwner(defKey, DEFAULT_USERNAME);
//   }
//
//   @Test
//   public void setInitialOwner_ownerSetToCorrectPendingProcessDefinition() {
//     // given
//     final String defKey1 = "unborn_process1";
//     final String defKey2 = "unborn_process2";
//
//     // when
//     // Make sure process definitions are not there yet
//     assertThat(definitionClient.getAllDefinitions()).isEmpty();
//     // Set owner for 1st process
//     final Response responseInitialOwner =
//         processOverviewClient.setInitialProcessOwner(defKey1, DEFAULT_USERNAME);
//     // Now only we deploy the processes
//     deploySimpleProcessDefinition(defKey1);
//     deploySimpleProcessDefinition(defKey2);
//     importAllEngineEntitiesFromScratch();
//
//     final OnboardingSchedulerService onboardingSchedulerService =
//
// embeddedOptimizeExtension.getApplicationContext().getBean(OnboardingSchedulerService.class);
//     // Process the pending data
//     onboardingSchedulerService.onboardNewProcesses();
//     databaseIntegrationTestExtension.refreshAllOptimizeIndices();
//
//     // then
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     // Only the 1st process should have an owner
//     assertExpectedProcessOwner(defKey1, DEFAULT_USERNAME);
//     assertExpectedProcessOwner(defKey2, null);
//   }
//
//   @Test
//   public void setInitialOwner_processDoesNotExistYetPendingOwnerNotAuthorizedToProcess() {
//     // given
//     String defKey = "unborn_rogue_process";
//     engineIntegrationExtension.addUser(KERMIT_USER, KERMIT_USER);
//     engineIntegrationExtension.grantUserOptimizeAccess(KERMIT_USER);
//     authorizationClient.revokeSingleResourceAuthorizationsForKermit(
//         defKey, RESOURCE_TYPE_PROCESS_DEFINITION);
//
//     // when
//     // Make sure process definition is not there yet
//     assertThat(definitionClient.getAllDefinitions()).isEmpty();
//     Response responseInitialOwner =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildSetInitialProcessOwnerRequest(new InitialProcessOwnerDto(defKey, KERMIT_USER))
//             .withUserAuthentication(KERMIT_USER, KERMIT_USER)
//             .execute();
//     // Now only we deploy the process
//     deploySimpleProcessDefinition(defKey);
//     importAllEngineEntitiesFromScratch();
//     final OnboardingSchedulerService onboardingSchedulerService =
//
// embeddedOptimizeExtension.getApplicationContext().getBean(OnboardingSchedulerService.class);
//     // Process the pending data
//     onboardingSchedulerService.onboardNewProcesses();
//
//     // then
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     // No owner was set, since kermit not allowed
//     assertExpectedProcessOwner(defKey, null);
//   }
//
//   @Test
//   public void setInitialOwner_processDoesNotExistYetPendingOwnerNotAuthorizedToProcessOwner() {
//     // given
//     String defKey = "unborn_rogue_process";
//     engineIntegrationExtension.addUser(KERMIT_USER, KERMIT_USER);
//     engineIntegrationExtension.grantUserOptimizeAccess(KERMIT_USER);
//     authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_PROCESS_DEFINITION);
//     authorizationClient.revokeSingleResourceAuthorizationsForKermit(
//         DEFAULT_USERNAME, RESOURCE_TYPE_USER);
//
//     // when
//     // Make sure process definition is not there yet
//     assertThat(definitionClient.getAllDefinitions()).isEmpty();
//     Response responseInitialOwner =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildSetInitialProcessOwnerRequest(
//                 new InitialProcessOwnerDto(defKey, DEFAULT_USERNAME))
//             .withUserAuthentication(KERMIT_USER, KERMIT_USER)
//             .execute();
//     // Now only we deploy the process
//     deploySimpleProcessDefinition(defKey);
//     importAllEngineEntitiesFromScratch();
//     final OnboardingSchedulerService onboardingSchedulerService =
//
// embeddedOptimizeExtension.getApplicationContext().getBean(OnboardingSchedulerService.class);
//     // Process the pending data
//     onboardingSchedulerService.onboardNewProcesses();
//
//     // then
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
//     // No owner was set, since kermit not allowed
//     assertExpectedProcessOwner(defKey, null);
//   }
//
//   @Test
//   public void setInitialOwner_processDoesNotExistYetPendingOwnerDoesNotExist() {
//     // given
//     String defKey = "unborn_rogue_process";
//
//     // when
//     // Make sure process definition is not there yet
//     assertThat(definitionClient.getAllDefinitions()).isEmpty();
//     final Response responseInitialOwner =
//         processOverviewClient.setInitialProcessOwner(defKey, "Rotten_Tomato_head");
//     // Now only we deploy the process
//     deploySimpleProcessDefinition(defKey);
//     importAllEngineEntitiesFromScratch();
//     final OnboardingSchedulerService onboardingSchedulerService =
//
// embeddedOptimizeExtension.getApplicationContext().getBean(OnboardingSchedulerService.class);
//     // Process the pending data
//     onboardingSchedulerService.onboardNewProcesses();
//
//     // then
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
//     // No owner was set, since user doesn't exist
//     assertExpectedProcessOwner(defKey, null);
//   }
//
//   @Test
//   public void setInitialOwner_doNotOverwriteExistingOwner() {
//     // given
//     engineIntegrationExtension.addUser(KERMIT_USER, KERMIT_USER);
//     engineIntegrationExtension.grantUserOptimizeAccess(KERMIT_USER);
//     authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_PROCESS_DEFINITION);
//     deploySimpleProcessDefinition(DEF_KEY);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     Response response =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildUpdateProcessRequest(
//                 DEF_KEY, new ProcessUpdateDto(DEFAULT_USERNAME, new ProcessDigestRequestDto()))
//             .execute();
//
//     final Response responseInitialOwner =
//         processOverviewClient.setInitialProcessOwner(DEF_KEY, KERMIT_USER);
//
//     // then
//     assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertExpectedProcessOwner(DEF_KEY, DEFAULT_USERNAME);
//   }
//
//   @Test
//   public void setInitialOwner_processExistsOwnerNotYetSetButUserNotAuthorizedToProcess() {
//     // given
//     deploySimpleProcessDefinition(DEF_KEY);
//     importAllEngineEntitiesFromScratch();
//     engineIntegrationExtension.addUser(KERMIT_USER, KERMIT_USER);
//     engineIntegrationExtension.grantUserOptimizeAccess(KERMIT_USER);
//     authorizationClient.revokeSingleResourceAuthorizationsForKermit(
//         DEF_KEY, RESOURCE_TYPE_PROCESS_DEFINITION);
//
//     // when
//     // No process owner yet
//     assertExpectedProcessOwner(DEF_KEY, null);
//     Response responseInitialOwner =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildSetInitialProcessOwnerRequest(
//                 new InitialProcessOwnerDto(DEF_KEY, DEFAULT_USERNAME))
//             .withUserAuthentication(KERMIT_USER, KERMIT_USER)
//             .execute();
//
//     // then
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
//     // No owner was set, since user not allowed
//     assertExpectedProcessOwner(DEF_KEY, null);
//   }
//
//   @Test
//   public void setInitialOwner_processExistsOwnerNotYetSetButUserNotAuthorizedToProcessOwner() {
//     // given
//     deploySimpleProcessDefinition(DEF_KEY);
//     importAllEngineEntitiesFromScratch();
//     engineIntegrationExtension.addUser(KERMIT_USER, KERMIT_USER);
//     engineIntegrationExtension.grantUserOptimizeAccess(KERMIT_USER);
//     authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_PROCESS_DEFINITION);
//     authorizationClient.revokeSingleResourceAuthorizationsForKermit(
//         DEFAULT_USERNAME, RESOURCE_TYPE_USER);
//
//     // when
//     // No process owner yet
//     assertExpectedProcessOwner(DEF_KEY, null);
//     Response responseInitialOwner =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildSetInitialProcessOwnerRequest(
//                 new InitialProcessOwnerDto(DEF_KEY, DEFAULT_USERNAME))
//             .withUserAuthentication(KERMIT_USER, KERMIT_USER)
//             .execute();
//
//     // then
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
//     // No owner was set, since user not allowed
//     assertExpectedProcessOwner(DEF_KEY, null);
//   }
//
//   @Test
//   public void setInitialOwner_processExistsButOwnerNotYetSet() {
//     // given
//     String defKey = "brandnew";
//     deploySimpleProcessDefinition(defKey);
//     importAllEngineEntitiesFromScratch();
//
//     // when
//     // No process owner yet
//     assertExpectedProcessOwner(defKey, null);
//     final Response responseInitialOwner =
//         processOverviewClient.setInitialProcessOwner(defKey, DEFAULT_USERNAME);
//
//     // then
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     assertExpectedProcessOwner(defKey, DEFAULT_USERNAME);
//   }
//
//   @Test
//   public void setInitialOwner_notPossibleForUnauthenticatedUser() {
//     // when
//     Response responseInitialOwner =
//         embeddedOptimizeExtension
//             .getRequestExecutor()
//             .buildSetInitialProcessOwnerRequest(
//                 new InitialProcessOwnerDto(DEF_KEY, DEFAULT_USERNAME))
//             .withoutAuthentication()
//             .execute();
//
//     // then
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
//   }
//
//   @Test
//   public void ifOnboardingEmailServiceIsDeactivatedPendingOwnerDataIsStillProcessed() {
//     // given
//     embeddedOptimizeExtension
//         .getConfigurationService()
//         .getOnboarding()
//         .setEnableOnboardingEmails(false);
//     final String defKey = "unborn_process";
//
//     // when
//     // Make sure process definition is not there yet
//     assertThat(definitionClient.getAllDefinitions()).isEmpty();
//     final Response responseInitialOwner =
//         processOverviewClient.setInitialProcessOwner(defKey, DEFAULT_USERNAME);
//     assertThat(responseInitialOwner.getStatus())
//         .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
//     final OnboardingSchedulerService onboardingSchedulerService =
//
// embeddedOptimizeExtension.getApplicationContext().getBean(OnboardingSchedulerService.class);
//     onboardingSchedulerService.stopOnboardingScheduling();
//     onboardingSchedulerService.setIntervalToCheckForOnboardingDataInSeconds(
//         1); // Set interval to 1s
//     onboardingSchedulerService.startOnboardingScheduling();
//     // Only now we deploy the process
//     deploySimpleProcessDefinition(defKey);
//     importAllEngineEntitiesFromScratch();
//
//     // then
//     // The new process is now there, and we're checking every second, let's check if our process
//     // definition was picked up
//     Awaitility.given()
//         .ignoreExceptions()
//         .timeout(10, TimeUnit.SECONDS)
//         .untilAsserted(() -> assertExpectedProcessOwner(defKey, DEFAULT_USERNAME));
//   }
//
//   private void deploySimpleProcessDefinition(String processDefinitionKey) {
//     engineIntegrationExtension.deployProcessAndGetProcessDefinition(
//         getSimpleBpmnDiagram(processDefinitionKey));
//   }
//
//   private void assertExpectedProcessOwner(final String defKey, final String expectedOwnerId) {
//     assertThat(getProcessOverview(null))
//         .filteredOn(def -> def.getProcessDefinitionKey().equals(defKey))
//         .extracting(ProcessOverviewResponseDto::getOwner)
//         .singleElement()
//         .satisfies(
//             processOwner ->
//                 assertThat(processOwner)
//                     .isEqualTo(
//                         expectedOwnerId == null
//                             ? new ProcessOwnerResponseDto()
//                             : new ProcessOwnerResponseDto(
//                                 expectedOwnerId,
//                                 embeddedOptimizeExtension
//                                     .getIdentityService()
//                                     .getIdentityNameById(expectedOwnerId)
//                                     .orElseThrow(
//                                         () ->
//                                             new OptimizeIntegrationTestException(
//                                                 "Could not find default user in cache")))));
//     // we also assert that the last results are initialised as empty rather to avoid potential
// null
//     // checks on overview load
//     final List<Map<String, String>> lastKpiResultsForDef =
//         databaseIntegrationTestExtension
//             .getAllDocumentsOfIndexAs(PROCESS_OVERVIEW_INDEX_NAME, ProcessOverviewDto.class)
//             .stream()
//             .filter(overview -> overview.getProcessDefinitionKey().contains(defKey))
//             .map(ProcessOverviewDto::getLastKpiEvaluationResults)
//             .collect(Collectors.toList());
//     if (!lastKpiResultsForDef.isEmpty()) {
//       assertThat(lastKpiResultsForDef).singleElement().isEqualTo(Collections.emptyMap());
//     }
//   }
//
//   private List<ProcessOverviewResponseDto> getProcessOverview(
//       final ProcessOverviewSorter processOverviewSorter) {
//     return embeddedOptimizeExtension
//         .getRequestExecutor()
//         .buildGetProcessOverviewRequest(processOverviewSorter)
//         .executeAndReturnList(ProcessOverviewResponseDto.class,
// Response.Status.OK.getStatusCode());
//   }
// }
