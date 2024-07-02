/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index.index;

import static io.camunda.optimize.service.db.DatabaseConstants.DYNAMIC_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.FORMAT_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;
import static io.camunda.optimize.service.db.DatabaseConstants.POSITION_BASED_IMPORT_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_BOOLEAN;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_DATE;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_KEYWORD;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_OBJECT;

import io.camunda.optimize.dto.optimize.index.ImportIndexDto;
import io.camunda.optimize.dto.optimize.index.PositionBasedImportIndexDto;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class PositionBasedImportIndex<TBuilder>
    extends DefaultIndexMappingCreator<TBuilder> {

  public static final int VERSION = 3;

  private static final String LAST_IMPORT_EXECUTION_TIMESTAMP =
      ImportIndexDto.Fields.lastImportExecutionTimestamp;
  private static final String POSITION_OF_LAST_ENTITY =
      PositionBasedImportIndexDto.Fields.positionOfLastEntity;
  private static final String SEQUENCE_OF_LAST_ENTITY =
      PositionBasedImportIndexDto.Fields.sequenceOfLastEntity;
  private static final String HAS_SEEN_SEQUENCE_FIELD =
      PositionBasedImportIndexDto.Fields.hasSeenSequenceField;
  private static final String TIMESTAMP_OF_LAST_ENTITY =
      ImportIndexDto.Fields.timestampOfLastEntity;
  private static final String DB_TYPE_INDEX_REFERS_TO =
      PositionBasedImportIndexDto.Fields.esTypeIndexRefersTo;
  private static final String DATA_SOURCE = ImportIndexDto.Fields.dataSource;

  @Override
  public String getIndexName() {
    return POSITION_BASED_IMPORT_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public boolean isImportIndex() {
    return true;
  }

  @Override
  public XContentBuilder addProperties(XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    return xContentBuilder
        .startObject(DATA_SOURCE)
        .field(MAPPING_PROPERTY_TYPE, TYPE_OBJECT)
        .field(DYNAMIC_PROPERTY_TYPE, true)
        .endObject()
        .startObject(DB_TYPE_INDEX_REFERS_TO)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject()
        .startObject(POSITION_OF_LAST_ENTITY)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject()
        .startObject(SEQUENCE_OF_LAST_ENTITY)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject()
        .startObject(HAS_SEEN_SEQUENCE_FIELD)
        .field(MAPPING_PROPERTY_TYPE, TYPE_BOOLEAN)
        .endObject()
        .startObject(TIMESTAMP_OF_LAST_ENTITY)
        .field(MAPPING_PROPERTY_TYPE, TYPE_DATE)
        .field(FORMAT_PROPERTY_TYPE, OPTIMIZE_DATE_FORMAT)
        .endObject()
        .startObject(LAST_IMPORT_EXECUTION_TIMESTAMP)
        .field(MAPPING_PROPERTY_TYPE, TYPE_DATE)
        .field(FORMAT_PROPERTY_TYPE, OPTIMIZE_DATE_FORMAT)
        .endObject();
    // @formatter:on
  }
}
