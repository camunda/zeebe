/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index.events;

import static io.camunda.optimize.service.db.DatabaseConstants.EVENT_PROCESS_PUBLISH_STATE_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;

import io.camunda.optimize.dto.optimize.query.event.process.EventImportSourceDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventTypeDto;
import io.camunda.optimize.dto.optimize.query.event.process.db.DbEventMappingDto;
import io.camunda.optimize.dto.optimize.query.event.process.db.DbEventProcessPublishStateDto;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class EventProcessPublishStateIndex<TBuilder>
    extends DefaultIndexMappingCreator<TBuilder> {

  public static final int VERSION = 4;

  public static final String ID = DbEventProcessPublishStateDto.Fields.id;
  public static final String PROCESS_MAPPING_ID =
      DbEventProcessPublishStateDto.Fields.processMappingId;
  public static final String NAME = DbEventProcessPublishStateDto.Fields.name;
  public static final String PUBLISH_DATE_TIME =
      DbEventProcessPublishStateDto.Fields.publishDateTime;
  public static final String STATE = DbEventProcessPublishStateDto.Fields.state;
  public static final String PUBLISH_PROGRESS =
      DbEventProcessPublishStateDto.Fields.publishProgress;
  public static final String DELETED = DbEventProcessPublishStateDto.Fields.deleted;
  public static final String XML = DbEventProcessPublishStateDto.Fields.xml;
  public static final String MAPPINGS = DbEventProcessPublishStateDto.Fields.mappings;
  public static final String EVENT_IMPORT_SOURCES =
      DbEventProcessPublishStateDto.Fields.eventImportSources;

  public static final String FLOWNODE_ID = DbEventMappingDto.Fields.flowNodeId;
  public static final String START = DbEventMappingDto.Fields.start;
  public static final String END = DbEventMappingDto.Fields.end;

  public static final String GROUP = EventTypeDto.Fields.group;
  public static final String SOURCE = EventTypeDto.Fields.source;
  public static final String EVENT_NAME = EventTypeDto.Fields.eventName;
  public static final String EVENT_LABEL = EventTypeDto.Fields.eventLabel;

  public static final String FIRST_EVENT_FOR_IMPORT_SOURCE_TIMESTAMP =
      EventImportSourceDto.Fields.firstEventForSourceAtTimeOfPublishTimestamp;
  public static final String PUBLISH_COMPLETED_TIMESTAMP =
      EventImportSourceDto.Fields.lastEventForSourceAtTimeOfPublishTimestamp;
  public static final String LAST_IMPORT_EXECUTION_TIMESTAMP =
      EventImportSourceDto.Fields.lastImportExecutionTimestamp;
  public static final String LAST_IMPORTED_EVENT_TIMESTAMP =
      EventImportSourceDto.Fields.lastImportedEventTimestamp;
  public static final String EVENT_IMPORT_SOURCE_TYPE =
      EventImportSourceDto.Fields.eventImportSourceType;
  public static final String EVENT_IMPORT_SOURCE_CONFIGS =
      EventImportSourceDto.Fields.eventSourceConfigurations;

  @Override
  public String getIndexName() {
    return EVENT_PROCESS_PUBLISH_STATE_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    final XContentBuilder newXContentBuilder =
        xContentBuilder
            .startObject(ID)
            .field("type", "keyword")
            .endObject()
            .startObject(PROCESS_MAPPING_ID)
            .field("type", "keyword")
            .endObject()
            .startObject(NAME)
            .field("type", "keyword")
            .endObject()
            .startObject(PUBLISH_DATE_TIME)
            .field("type", "date")
            .field("format", OPTIMIZE_DATE_FORMAT)
            .endObject()
            .startObject(STATE)
            .field("type", "keyword")
            .endObject()
            .startObject(PUBLISH_PROGRESS)
            .field("type", "double")
            .endObject()
            .startObject(DELETED)
            .field("type", "boolean")
            .endObject()
            .startObject(XML)
            .field("type", "text")
            .field("index", true)
            .field("analyzer", "is_present_analyzer")
            .endObject()
            .startObject(MAPPINGS)
            .field("type", "object")
            .startObject("properties");
    addMappingFields(newXContentBuilder)
        .endObject()
        .endObject()
        .startObject(EVENT_IMPORT_SOURCES)
        .field("type", "object")
        .startObject("properties");
    addEventImportSourcesField(newXContentBuilder).endObject().endObject();
    // @formatter:on
    return newXContentBuilder;
  }

  private XContentBuilder addMappingFields(final XContentBuilder xContentBuilder)
      throws IOException {
    // @formatter:off
    XContentBuilder newXContentBuilder =
        xContentBuilder
            .startObject(FLOWNODE_ID)
            .field("type", "keyword")
            .endObject()
            .startObject(START)
            .field("type", "object")
            .startObject("properties");
    addEventMappingFields(newXContentBuilder)
        .endObject()
        .endObject()
        .startObject(END)
        .field("type", "object")
        .startObject("properties");
    addEventMappingFields(newXContentBuilder).endObject().endObject();
    // @formatter:on
    return newXContentBuilder;
  }

  private XContentBuilder addEventMappingFields(final XContentBuilder xContentBuilder)
      throws IOException {
    return xContentBuilder
        // @formatter:off
        .startObject(GROUP)
        .field("type", "keyword")
        .endObject()
        .startObject(SOURCE)
        .field("type", "keyword")
        .endObject()
        .startObject(EVENT_NAME)
        .field("type", "keyword")
        .endObject()
        .startObject(EVENT_LABEL)
        .field("type", "keyword")
        .endObject();
    // @formatter:on
  }

  private XContentBuilder addEventImportSourcesField(final XContentBuilder xContentBuilder)
      throws IOException {
    // @formatter:off
    return xContentBuilder
        .startObject(FIRST_EVENT_FOR_IMPORT_SOURCE_TIMESTAMP)
        .field("type", "date")
        .field("format", OPTIMIZE_DATE_FORMAT)
        .endObject()
        .startObject(PUBLISH_COMPLETED_TIMESTAMP)
        .field("type", "date")
        .field("format", OPTIMIZE_DATE_FORMAT)
        .endObject()
        .startObject(LAST_IMPORT_EXECUTION_TIMESTAMP)
        .field("type", "date")
        .field("format", OPTIMIZE_DATE_FORMAT)
        .endObject()
        .startObject(LAST_IMPORTED_EVENT_TIMESTAMP)
        .field("type", "date")
        .field("format", OPTIMIZE_DATE_FORMAT)
        .endObject()
        .startObject(EVENT_IMPORT_SOURCE_TYPE)
        .field("type", "keyword")
        .endObject()
        .startObject(EVENT_IMPORT_SOURCE_CONFIGS)
        .field("type", "object")
        .field("dynamic", true)
        .endObject();
    // @formatter:on
  }
}
