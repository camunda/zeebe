/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.rest;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.rest.RestTestUtil.getOffsetDiffInHours;
import static io.camunda.optimize.rest.constants.RestConstants.X_OPTIMIZE_CLIENT_TIMEZONE;
import static io.camunda.optimize.service.db.DatabaseConstants.ALERT_INDEX_NAME;
import static io.camunda.optimize.test.it.extension.EngineIntegrationExtension.DEFAULT_FULLNAME;
import static io.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.optimize.AbstractAlertIT;
import io.camunda.optimize.dto.optimize.DefinitionType;
import io.camunda.optimize.dto.optimize.query.alert.AlertDefinitionDto;
import io.camunda.optimize.dto.optimize.query.alert.AlertIntervalUnit;
import io.camunda.optimize.util.SuppressionConstants;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag(OPENSEARCH_PASSING)
public class CollectionRestServiceAlertIT extends AbstractAlertIT {

  @SuppressWarnings(SuppressionConstants.UNUSED)
  private static Stream<DefinitionType> definitionType() {
    return Stream.of(DefinitionType.PROCESS, DefinitionType.DECISION);
  }

  @ParameterizedTest
  @MethodSource("definitionType")
  public void getStoredAlerts(final DefinitionType definitionType) {
    // given
    final String collectionId1 =
        collectionClient.createNewCollectionWithDefaultScope(definitionType);
    final String reportId1 = createNumberReportForCollection(collectionId1, definitionType);
    final String reportId2 = createNumberReportForCollection(collectionId1, definitionType);
    final String alertId1 = alertClient.createAlertForReport(reportId1);
    final String alertId2 = alertClient.createAlertForReport(reportId1);
    final String alertId3 = alertClient.createAlertForReport(reportId2);

    final String collectionId2 =
        collectionClient.createNewCollectionWithDefaultScope(definitionType);
    final String reportId3 = createNumberReportForCollection(collectionId2, definitionType);
    alertClient.createAlertForReport(reportId3);

    // when
    List<AlertDefinitionDto> allAlerts =
        alertClient.getAlertsForCollectionAsDefaultUser(collectionId1);

    // then
    assertThat(allAlerts)
        .extracting(AlertDefinitionDto::getId)
        .containsExactlyInAnyOrder(alertId1, alertId2, alertId3);
    assertThat(allAlerts)
        .allMatch(alert -> alert.getOwner().equals(DEFAULT_FULLNAME))
        .allMatch(alert -> alert.getLastModifier().equals(DEFAULT_FULLNAME));
  }

  @Test
  public void getStoredAlerts_adoptTimezoneFromHeader() {
    // given
    OffsetDateTime now = dateFreezer().timezone("Europe/Berlin").freezeDateAndReturn();
    final String collectionId =
        collectionClient.createNewCollectionWithDefaultScope(DefinitionType.PROCESS);
    final String reportId = createNumberReportForCollection(collectionId, DefinitionType.PROCESS);
    alertClient.createAlertForReport(reportId);

    // when
    List<AlertDefinitionDto> allAlerts =
        embeddedOptimizeExtension
            .getRequestExecutor()
            .buildGetAlertsForCollectionRequest(collectionId)
            .addSingleHeader(X_OPTIMIZE_CLIENT_TIMEZONE, "Europe/London")
            .executeAndReturnList(AlertDefinitionDto.class, Response.Status.OK.getStatusCode());

    // then
    assertThat(allAlerts).isNotNull().hasSize(1);
    AlertDefinitionDto alertDefinitionDto = allAlerts.get(0);
    assertThat(alertDefinitionDto.getCreated()).isEqualTo(now);
    assertThat(alertDefinitionDto.getLastModified()).isEqualTo(now);
    assertThat(getOffsetDiffInHours(alertDefinitionDto.getCreated(), now)).isEqualTo(1.);
    assertThat(getOffsetDiffInHours(alertDefinitionDto.getLastModified(), now)).isEqualTo(1.);
  }

  @ParameterizedTest(
      name = "only alerts in given collection should be retrieved for definition type {0}")
  @MethodSource("definitionType")
  public void getNoneStoredAlerts(final DefinitionType definitionType) {
    // given
    final String collectionId1 =
        collectionClient.createNewCollectionWithDefaultScope(definitionType);
    final String collectionId2 =
        collectionClient.createNewCollectionWithDefaultScope(definitionType);
    final String reportId1 = createNumberReportForCollection(collectionId1, definitionType);
    createNumberReportForCollection(collectionId2, definitionType);
    alertClient.createAlertForReport(reportId1);

    // when
    List<AlertDefinitionDto> allAlerts =
        alertClient.getAlertsForCollectionAsDefaultUser(collectionId2);

    // then
    assertThat(allAlerts).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("definitionType")
  public void deleteCollectionAlsoDeletesContainingAlerts(final DefinitionType definitionType) {
    // given
    final String collectionId =
        collectionClient.createNewCollectionWithDefaultScope(definitionType);

    final String reportId1 = createNumberReportForCollection(collectionId, definitionType);
    final String reportId2 = createNumberReportForCollection(collectionId, definitionType);

    alertClient.createAlertForReport(reportId1, 1, AlertIntervalUnit.HOURS);
    alertClient.createAlertForReport(reportId1, 1, AlertIntervalUnit.HOURS);
    alertClient.createAlertForReport(reportId2, 1, AlertIntervalUnit.HOURS);

    // when
    collectionClient.deleteCollection(collectionId);

    Integer alertCount = databaseIntegrationTestExtension.getDocumentCountOf(ALERT_INDEX_NAME);

    // then
    assertThat(alertCount).isZero();
  }
}
