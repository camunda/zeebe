/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.backup;

import io.camunda.operate.webapp.management.dto.GetBackupStateResponseDto;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface BackupRepository {

  Logger LOGGER = LoggerFactory.getLogger(BackupRepository.class);

  void deleteSnapshot(String repositoryName, String snapshotName);

  void validateRepositoryExists(String repositoryName);

  void validateNoDuplicateBackupId(String repositoryName, Long backupId);

  GetBackupStateResponseDto getBackupState(String repositoryName, Long backupId);

  List<GetBackupStateResponseDto> getBackups(String repositoryName);

  void executeSnapshotting(
      BackupService.SnapshotRequest snapshotRequest, Runnable onSuccess, Runnable onFailure);

  default boolean isIncompleteCheckTimedOut(
      final long incompleteCheckTimeoutInSeconds,
      final long startTimeInMilliseconds,
      final long endTimeInMilliseconds) {
    final var incompleteCheckTimeoutInMilliseconds = incompleteCheckTimeoutInSeconds * 1000;
    try {
      final var now = Instant.now().toEpochMilli();
      // if is no end time available (endTimeInMilliseconds <= 0 ) we just check start time with now
      if ((now - startTimeInMilliseconds) > incompleteCheckTimeoutInMilliseconds
          && endTimeInMilliseconds <= 0) {
        return true;
      }
      return (endTimeInMilliseconds - startTimeInMilliseconds)
          > (incompleteCheckTimeoutInMilliseconds);
    } catch (final Exception e) {
      LOGGER.warn(
          "Couldn't check incomplete timeout for backup. Return incomplete check is timed out", e);
      return true;
    }
  }
}
