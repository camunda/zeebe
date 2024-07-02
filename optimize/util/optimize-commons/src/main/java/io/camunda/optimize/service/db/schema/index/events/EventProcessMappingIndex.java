/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index.events;

import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;

import io.camunda.optimize.dto.optimize.IdentityDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventProcessRoleRequestDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventTypeDto;
import io.camunda.optimize.dto.optimize.query.event.process.db.DbEventMappingDto;
import io.camunda.optimize.dto.optimize.query.event.process.db.DbEventProcessMappingDto;
import io.camunda.optimize.dto.optimize.query.event.process.source.EventSourceEntryDto;
import io.camunda.optimize.service.db.DatabaseConstants;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class EventProcessMappingIndex<TBuilder>
    extends DefaultIndexMappingCreator<TBuilder> {

  public static final int VERSION = 4;

  public static final String ID = DbEventProcessMappingDto.Fields.id;
  public static final String NAME = DbEventProcessMappingDto.Fields.name;
  public static final String XML = DbEventProcessMappingDto.Fields.xml;
  public static final String LAST_MODIFIED = DbEventProcessMappingDto.Fields.lastModified;
  public static final String LAST_MODIFIER = DbEventProcessMappingDto.Fields.lastModifier;
  public static final String MAPPINGS = DbEventProcessMappingDto.Fields.mappings;
  public static final String EVENT_SOURCES = DbEventProcessMappingDto.Fields.eventSources;

  public static final String FLOWNODE_ID = DbEventMappingDto.Fields.flowNodeId;
  public static final String START = DbEventMappingDto.Fields.start;
  public static final String END = DbEventMappingDto.Fields.end;

  public static final String GROUP = EventTypeDto.Fields.group;
  public static final String SOURCE = EventTypeDto.Fields.source;
  public static final String EVENT_NAME = EventTypeDto.Fields.eventName;
  public static final String EVENT_LABEL = EventTypeDto.Fields.eventLabel;

  public static final String EVENT_SOURCE_ID = EventSourceEntryDto.Fields.id;
  public static final String EVENT_SOURCE_TYPE = EventSourceEntryDto.TYPE;
  public static final String EVENT_SOURCE_CONFIG = EventSourceEntryDto.Fields.configuration;

  public static final String ROLES = DbEventProcessMappingDto.Fields.roles;
  public static final String ROLE_ID = EventProcessRoleRequestDto.Fields.id;
  public static final String ROLE_IDENTITY = EventProcessRoleRequestDto.Fields.identity;
  public static final String ROLE_IDENTITY_ID = IdentityDto.Fields.id;
  public static final String ROLE_IDENTITY_TYPE = IdentityDto.Fields.type;

  @Override
  public String getIndexName() {
    return DatabaseConstants.EVENT_PROCESS_MAPPING_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(final XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    XContentBuilder newXContentBuilder =
        xContentBuilder
            .startObject(ID)
            .field("type", "keyword")
            .endObject()
            .startObject(NAME)
            .field("type", "keyword")
            .endObject()
            .startObject(LAST_MODIFIER)
            .field("type", "keyword")
            .endObject()
            .startObject(LAST_MODIFIED)
            .field("type", "date")
            .field("format", OPTIMIZE_DATE_FORMAT)
            .endObject()
            .startObject(XML)
            .field("type", "text")
            .field("index", true)
            .field("analyzer", "is_present_analyzer")
            .endObject()
            .startObject(MAPPINGS)
            .field("type", "object")
            .startObject("properties");
    addNestedFlowNodeMappingsFields(newXContentBuilder)
        .endObject()
        .endObject()
        .startObject(EVENT_SOURCES)
        .field("type", "object")
        .field("dynamic", true)
        .startObject("properties");
    addEventSourcesField(newXContentBuilder)
        .endObject()
        .endObject()
        .startObject(ROLES)
        .field("type", "object")
        .startObject("properties");
    addNestedRolesFields(newXContentBuilder).endObject().endObject();
    // @formatter:on
    return newXContentBuilder;
  }

  private XContentBuilder addNestedFlowNodeMappingsFields(final XContentBuilder xContentBuilder)
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

  private XContentBuilder addEventSourcesField(final XContentBuilder xContentBuilder)
      throws IOException {
    // @formatter:off
    return xContentBuilder
        .startObject(EVENT_SOURCE_ID)
        .field("type", "keyword")
        .endObject()
        .startObject(EVENT_SOURCE_TYPE)
        .field("type", "keyword")
        .endObject()
        .startObject(EVENT_SOURCE_CONFIG)
        .field("type", "object")
        .field("dynamic", true)
        .endObject();
    // @formatter:on
  }

  private XContentBuilder addNestedRolesFields(final XContentBuilder xContentBuilder)
      throws IOException {
    return xContentBuilder
        // @formatter:off
        .startObject(ROLE_ID)
        .field("type", "keyword")
        .endObject()
        .startObject(ROLE_IDENTITY)
        .field("type", "object")
        .startObject("properties")
        .startObject(ROLE_IDENTITY_ID)
        .field("type", "keyword")
        .endObject()
        .startObject(ROLE_IDENTITY_TYPE)
        .field("type", "keyword")
        .endObject()
        .endObject()
        .endObject();
    // @formatter:on
  }
}
