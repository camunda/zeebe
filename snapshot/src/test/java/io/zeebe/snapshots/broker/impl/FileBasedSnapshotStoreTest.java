/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.snapshots.broker.impl;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileBasedSnapshotStoreTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private Path snapshotsDir;
  private Path pendingSnapshotsDir;
  private FileBasedSnapshotStoreFactory factory;
  private File root;
  private String partitionName;

  @Before
  public void before() {
    factory = new FileBasedSnapshotStoreFactory();
    partitionName = "1";
    root = temporaryFolder.getRoot();

    factory.createReceivableSnapshotStore(root.toPath(), partitionName);

    snapshotsDir =
        temporaryFolder
            .getRoot()
            .toPath()
            .resolve(FileBasedSnapshotStoreFactory.SNAPSHOTS_DIRECTORY);
    pendingSnapshotsDir =
        temporaryFolder.getRoot().toPath().resolve(FileBasedSnapshotStoreFactory.PENDING_DIRECTORY);
  }

  @Test
  public void shouldCreateSubFoldersOnCreatingDirBasedStore() {
    // given

    // when + then
    assertThat(snapshotsDir).exists();
    Assertions.assertThat(pendingSnapshotsDir).exists();
  }

  @Test
  public void shouldDeleteStore() {
    // given

    // when
    factory.getPersistedSnapshotStore(partitionName).delete();

    // then
    assertThat(pendingSnapshotsDir).doesNotExist();
    assertThat(snapshotsDir).doesNotExist();
  }

  @Test
  public void shouldLoadExistingSnapshot() {
    // given
    final var index = 1L;
    final var term = 0L;
    final var transientSnapshot =
        factory
            .getConstructableSnapshotStore(partitionName)
            .newTransientSnapshot(index, term, 1, 0)
            .orElseThrow();
    transientSnapshot.take(this::createSnapshotDir);
    final var persistedSnapshot = transientSnapshot.persist();

    // when
    final var snapshotStore =
        new FileBasedSnapshotStoreFactory()
            .createReceivableSnapshotStore(root.toPath(), partitionName);

    // then
    final var currentSnapshotIndex = snapshotStore.getCurrentSnapshotIndex();
    assertThat(currentSnapshotIndex).isEqualTo(1L);
    assertThat(snapshotStore.getLatestSnapshot()).get().isEqualTo(persistedSnapshot);
  }

  private boolean createSnapshotDir(final Path path) {
    try {
      FileUtil.ensureDirectoryExists(path);
      Files.write(
          path.resolve("file1.txt"),
          "This is the content".getBytes(),
          CREATE_NEW,
          StandardOpenOption.WRITE);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return true;
  }
}
