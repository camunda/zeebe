/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util;

import static io.camunda.optimize.service.metadata.Version.getMajorVersionFrom;
import static io.camunda.optimize.service.metadata.Version.getMinorVersionFrom;
import static io.camunda.optimize.service.metadata.Version.getPatchVersionFrom;
import static io.camunda.optimize.service.util.DatabaseVersionChecker.getLatestSupportedESVersion;
import static io.camunda.optimize.service.util.DatabaseVersionChecker.getLatestSupportedOSVersion;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DatabaseVersionCheckerTest {
  public static final List<String> SUPPORTED_VERSIONS_ES =
      supportedDatabaseVersionsMap().get(DatabaseVersionChecker.Database.ELASTICSEARCH);
  public static final List<String> SUPPORTED_VERSIONS_OS =
      supportedDatabaseVersionsMap().get(DatabaseVersionChecker.Database.OPENSEARCH);

  @AfterEach
  public void resetSupportedVersions() {
    setSupportedDatabaseVersions(
        DatabaseVersionChecker.Database.ELASTICSEARCH, SUPPORTED_VERSIONS_ES);
    setSupportedDatabaseVersions(DatabaseVersionChecker.Database.OPENSEARCH, SUPPORTED_VERSIONS_OS);
  }

  @ParameterizedTest
  @MethodSource("validESVersions")
  public void testValidESVersions(final String version) {
    final boolean isSupported =
        DatabaseVersionChecker.isCurrentElasticsearchVersionSupported(version);

    assertThat(isSupported).isTrue();
  }

  @ParameterizedTest
  @MethodSource("validOSVersions")
  public void testValidOSVersions(final String version) {
    final boolean isSupported = DatabaseVersionChecker.isCurrentOpenSearchVersionSupported(version);
    assertThat(isSupported).isTrue();
  }

  @ParameterizedTest
  @MethodSource("invalidESVersions")
  public void testInvalidESVersions(final String version) {
    final boolean isSupported =
        DatabaseVersionChecker.isCurrentElasticsearchVersionSupported(version);
    assertThat(isSupported).isFalse();
  }

  @ParameterizedTest
  @MethodSource("invalidOSVersions")
  public void testInvalidOSVersions(final String version) {
    final boolean isSupported = DatabaseVersionChecker.isCurrentOpenSearchVersionSupported(version);
    assertThat(isSupported).isFalse();
  }

  @Test
  public void testWarningESVersions() {
    // given
    String version = constructWarningVersionHigherMinor(getLatestSupportedESVersion());

    // then
    assertThat(
            DatabaseVersionChecker.doesVersionNeedWarning(version, getLatestSupportedESVersion()))
        .isTrue();
  }

  @Test
  public void testWarningOSVersions() {
    // given
    String version = constructWarningVersionHigherMinor(getLatestSupportedOSVersion());

    // then
    assertThat(
            DatabaseVersionChecker.doesVersionNeedWarning(version, getLatestSupportedOSVersion()))
        .isTrue();
  }

  @Test
  public void testGetLatestSupportedVersion() {
    // given
    final String expectedLatestVersion = "7.11.5";

    List<String> versionsToTest =
        Arrays.asList("0.0.1", "7.2.0", expectedLatestVersion, "7.11.4", "7.10.6");
    setSupportedDatabaseVersions(DatabaseVersionChecker.Database.ELASTICSEARCH, versionsToTest);
    setSupportedDatabaseVersions(DatabaseVersionChecker.Database.OPENSEARCH, versionsToTest);

    // then
    assertThat(getLatestSupportedOSVersion()).isEqualTo(expectedLatestVersion);
    assertThat(getLatestSupportedESVersion()).isEqualTo(expectedLatestVersion);
  }

  private static void setSupportedDatabaseVersions(
      DatabaseVersionChecker.Database database, final List<String> versionsToTest) {
    supportedDatabaseVersionsMap().put(database, versionsToTest);
  }

  private static Stream<String> validVersions(List<String> supportedVersions) {
    List<String> validVersionsToTest = new ArrayList<>();
    for (String supportedVersion : supportedVersions) {
      validVersionsToTest.add(supportedVersion);
      validVersionsToTest.add(constructValidVersionHigherPatch(supportedVersion));
    }
    return validVersionsToTest.stream();
  }

  private static Stream<String> validESVersions() {
    return validVersions(
        supportedDatabaseVersionsMap().get(DatabaseVersionChecker.Database.ELASTICSEARCH));
  }

  private static Stream<String> validOSVersions() {
    return validVersions(
        supportedDatabaseVersionsMap().get(DatabaseVersionChecker.Database.OPENSEARCH));
  }

  private static EnumMap<DatabaseVersionChecker.Database, List<String>>
      supportedDatabaseVersionsMap() {
    return DatabaseVersionChecker.getDatabaseSupportedVersionsMap();
  }

  private static String constructValidVersionHigherPatch(String supportedVersion) {
    final String major = getMajorVersionFrom(supportedVersion);
    final String minor = getMinorVersionFrom(supportedVersion);
    final String patch = getPatchVersionFrom(supportedVersion);
    return buildVersionFromParts(major, minor, incrementVersionPart(patch));
  }

  private static String constructInvalidVersionLowerMajor(String leastSupportedVersion) {
    final String major = getMajorVersionFrom(leastSupportedVersion);
    final String minor = getMinorVersionFrom(leastSupportedVersion);
    final String patch = getPatchVersionFrom(leastSupportedVersion);
    return buildVersionFromParts(decrementVersionPart(major), minor, patch);
  }

  private static String constructInvalidVersionHigherMajor(String latestSupportedVersion) {
    final String major = getMajorVersionFrom(latestSupportedVersion);
    final String minor = getMinorVersionFrom(latestSupportedVersion);
    final String patch = getPatchVersionFrom(latestSupportedVersion);
    return buildVersionFromParts(incrementVersionPart(major), minor, patch);
  }

  private static String constructWarningVersionHigherMinor(String latestSupportedVersion) {
    final String major = getMajorVersionFrom(latestSupportedVersion);
    final String minor = getMinorVersionFrom(latestSupportedVersion);
    final String patch = getPatchVersionFrom(latestSupportedVersion);
    return buildVersionFromParts(major, incrementVersionPart(minor), patch);
  }

  private static String constructInvalidVersionLowerPatch(String patchedVersion) {
    final String major = getMajorVersionFrom(patchedVersion);
    final String minor = getMinorVersionFrom(patchedVersion);
    final String patch = getPatchVersionFrom(patchedVersion);
    return buildVersionFromParts(major, minor, decrementVersionPart(patch));
  }

  private static Stream<String> invalidESVersions() {
    return invalidVersions(
        supportedDatabaseVersionsMap().get(DatabaseVersionChecker.Database.ELASTICSEARCH),
        getLatestSupportedESVersion());
  }

  private static Stream<String> invalidOSVersions() {
    return invalidVersions(
        supportedDatabaseVersionsMap().get(DatabaseVersionChecker.Database.OPENSEARCH),
        getLatestSupportedOSVersion());
  }

  private static Stream<String> invalidVersions(
      List<String> supportedVersions, String latestSupportedVersion) {
    List<String> invalidVersions = new ArrayList<>();

    if (findPatchedVersionIfPresent(supportedVersions).isPresent()) {
      invalidVersions.add(
          constructInvalidVersionLowerPatch(findPatchedVersionIfPresent(supportedVersions).get()));
    }
    invalidVersions.add(constructInvalidVersionHigherMajor(latestSupportedVersion));
    invalidVersions.add(
        constructInvalidVersionLowerMajor(getLeastSupportedVersion(supportedVersions)));

    return invalidVersions.stream();
  }

  private static String buildVersionFromParts(
      final String major, final String minor, final String patch) {
    return String.join(".", major, minor, patch);
  }

  private static String decrementVersionPart(final String versionPart) {
    return String.valueOf(Long.parseLong(versionPart) - 1);
  }

  private static String incrementVersionPart(final String versionPart) {
    return String.valueOf(Long.parseLong(versionPart) + 1);
  }

  private static Optional<String> findPatchedVersionIfPresent(List<String> supportedVersions) {
    return supportedVersions.stream()
        .filter(v -> Integer.parseInt(getPatchVersionFrom(v)) > 0)
        .findFirst();
  }

  private static String getLeastSupportedVersion(List<String> supportedVersions) {
    return supportedVersions.stream().min(Comparator.naturalOrder()).get();
  }
}
