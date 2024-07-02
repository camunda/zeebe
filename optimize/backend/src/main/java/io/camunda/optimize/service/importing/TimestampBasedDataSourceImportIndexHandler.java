/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing;

import io.camunda.optimize.dto.optimize.datasource.DataSourceDto;
import io.camunda.optimize.dto.optimize.index.TimestampBasedImportIndexDto;
import io.camunda.optimize.service.db.reader.importindex.TimestampBasedImportIndexReader;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

@RequiredArgsConstructor
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class TimestampBasedDataSourceImportIndexHandler<T extends DataSourceDto>
    extends TimestampBasedImportIndexHandler<TimestampBasedImportIndexDto> {

  @Autowired protected TimestampBasedImportIndexReader importIndexReader;

  protected OffsetDateTime lastImportExecutionTimestamp = BEGINNING_OF_TIME;
  private OffsetDateTime persistedTimestampOfLastEntity = BEGINNING_OF_TIME;

  @Override
  public TimestampBasedImportIndexDto getIndexStateDto() {
    TimestampBasedImportIndexDto indexToStore = new TimestampBasedImportIndexDto();
    indexToStore.setLastImportExecutionTimestamp(lastImportExecutionTimestamp);
    indexToStore.setTimestampOfLastEntity(persistedTimestampOfLastEntity);
    indexToStore.setDataSource(getDataSource());
    indexToStore.setEsTypeIndexRefersTo(getDatabaseDocID());
    return indexToStore;
  }

  @PostConstruct
  protected void init() {
    final Optional<TimestampBasedImportIndexDto> dto =
        importIndexReader.getImportIndex(getDatabaseDocID(), getDataSource());
    if (dto.isPresent()) {
      TimestampBasedImportIndexDto loadedImportIndex = dto.get();
      updateLastPersistedEntityTimestamp(loadedImportIndex.getTimestampOfLastEntity());
      updatePendingLastEntityTimestamp(loadedImportIndex.getTimestampOfLastEntity());
      updateLastImportExecutionTimestamp(loadedImportIndex.getLastImportExecutionTimestamp());
    }
  }

  protected abstract String getDatabaseDocID();

  protected abstract T getDataSource();

  @Override
  protected void updateLastPersistedEntityTimestamp(final OffsetDateTime timestamp) {
    this.persistedTimestampOfLastEntity = timestamp;
  }

  @Override
  protected void updateLastImportExecutionTimestamp(final OffsetDateTime timestamp) {
    this.lastImportExecutionTimestamp = timestamp;
  }
}
