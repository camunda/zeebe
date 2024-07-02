/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.security.authorization;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_PROCESS_DEFINITION;
import static io.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static io.camunda.optimize.test.optimize.CollectionClient.DEFAULT_TENANTS;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractPlatformIT;
import io.camunda.optimize.dto.optimize.DefinitionType;
import io.camunda.optimize.dto.optimize.IdentityDto;
import io.camunda.optimize.dto.optimize.IdentityType;
import io.camunda.optimize.dto.optimize.RoleType;
import io.camunda.optimize.dto.optimize.query.collection.CollectionRoleRequestDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryDto;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.service.util.ProcessReportDataType;
import io.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import io.camunda.optimize.util.BpmnModels;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(OPENSEARCH_PASSING)
public class MultiDefinitionProcessReportAuthorizationIT extends AbstractPlatformIT {
  private static final String DEFINITION_KEY_1 = "key1";
  private static final String DEFINITION_KEY_2 = "key2";

  @Test
  public void getStoredMultiDefinitionReportWhenUserHasNoAuthForOneDefinition() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();

    engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(DEFINITION_KEY_1));
    authorizationClient.grantSingleResourceAuthorizationForKermit(
        DEFINITION_KEY_1, RESOURCE_TYPE_PROCESS_DEFINITION);
    engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(DEFINITION_KEY_2));

    // in order to share access to a report, it must reside in a collection accessible to kermit
    final String collectionId = createCollectionWithFullScopeAndAccessForKermit();
    final ProcessReportDataDto rawDataReportWithTwoDefinitions = createReportWithTwoDefinitions();
    final String reportId =
        reportClient.createSingleProcessReport(rawDataReportWithTwoDefinitions, collectionId);

    // when
    final Response response =
        reportClient.getReportByIdAsUserRawResponse(reportId, KERMIT_USER, KERMIT_USER);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void updateMultiDefinitionReportWhenUserHasNoAuthForOneDefinition() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();

    engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(DEFINITION_KEY_1));
    authorizationClient.grantSingleResourceAuthorizationForKermit(
        DEFINITION_KEY_1, RESOURCE_TYPE_PROCESS_DEFINITION);
    engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(DEFINITION_KEY_2));

    // in order to share access to a report, it must reside in a collection accessible to kermit
    final String collectionId = createCollectionWithFullScopeAndAccessForKermit();
    final ProcessReportDataDto rawDataReportWithTwoDefinitions = createReportWithTwoDefinitions();
    final String reportId =
        reportClient.createSingleProcessReport(rawDataReportWithTwoDefinitions, collectionId);
    final ReportDefinitionDto<?> storedReportDefinition = reportClient.getReportById(reportId);

    // when
    final Response response =
        reportClient.updateSingleProcessReport(
            reportId, storedReportDefinition, false, KERMIT_USER, KERMIT_USER);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void evaluateStoredMultiDefinitionReportWhenUserHasNoAuthForOneDefinition() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();

    engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(DEFINITION_KEY_1));
    authorizationClient.grantSingleResourceAuthorizationForKermit(
        DEFINITION_KEY_1, RESOURCE_TYPE_PROCESS_DEFINITION);
    engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(DEFINITION_KEY_2));

    // in order to share access to a report, it must reside in a collection accessible to kermit
    final String collectionId = createCollectionWithFullScopeAndAccessForKermit();
    final ProcessReportDataDto rawDataReportWithTwoDefinitions = createReportWithTwoDefinitions();
    final String reportId =
        reportClient.createSingleProcessReport(rawDataReportWithTwoDefinitions, collectionId);

    // when
    final Response response =
        reportClient.evaluateReportAsUserRawResponse(reportId, KERMIT_USER, KERMIT_USER);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void evaluateUnsavedMultiDefinitionReportWhenUserHasNoAuthForOneDefinition() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();

    engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(DEFINITION_KEY_1));
    authorizationClient.grantSingleResourceAuthorizationForKermit(
        DEFINITION_KEY_1, RESOURCE_TYPE_PROCESS_DEFINITION);
    engineIntegrationExtension.deployAndStartProcess(
        BpmnModels.getSingleUserTaskDiagram(DEFINITION_KEY_2));

    final ProcessReportDataDto rawDataReportWithTwoDefinitions = createReportWithTwoDefinitions();

    // when
    final Response response =
        reportClient.evaluateReportAsUserAndReturnResponse(
            rawDataReportWithTwoDefinitions, KERMIT_USER, KERMIT_USER);

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  private ProcessReportDataDto createReportWithTwoDefinitions() {
    return TemplatedProcessReportDataBuilder.createReportData()
        .setReportDataType(ProcessReportDataType.RAW_DATA)
        .definitions(
            List.of(
                new ReportDataDefinitionDto(DEFINITION_KEY_1),
                new ReportDataDefinitionDto(DEFINITION_KEY_2)))
        .build();
  }

  private String createCollectionWithFullScopeAndAccessForKermit() {
    final String collectionId = collectionClient.createNewCollection();
    collectionClient.addScopeEntriesToCollection(
        collectionId,
        List.of(
            new CollectionScopeEntryDto(DefinitionType.PROCESS, DEFINITION_KEY_1, DEFAULT_TENANTS),
            new CollectionScopeEntryDto(
                DefinitionType.PROCESS, DEFINITION_KEY_2, DEFAULT_TENANTS)));
    collectionClient.addRolesToCollection(
        collectionId,
        new CollectionRoleRequestDto(
            new IdentityDto(KERMIT_USER, IdentityType.USER), RoleType.MANAGER));
    return collectionId;
  }
}
