/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index.events;

import static io.camunda.optimize.service.db.DatabaseConstants.EVENT_PROCESS_INSTANCE_INDEX_PREFIX;
import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_ENABLED_SETTING;
import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;

import io.camunda.optimize.dto.optimize.query.event.process.EventProcessInstanceDto;
import io.camunda.optimize.dto.optimize.query.event.process.FlowNodeInstanceUpdateDto;
import io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex;
import java.io.IOException;
import java.util.Locale;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class EventProcessInstanceIndex<TBuilder> extends ProcessInstanceIndex<TBuilder> {

  private static final String PENDING_FLOW_NODE_UPDATES =
      EventProcessInstanceDto.Fields.pendingFlowNodeInstanceUpdates;
  private static final String CORRELATED_EVENTS_BY_EVENT_ID =
      EventProcessInstanceDto.Fields.correlatedEventsById;
  private static final String ACTIVITY_UPDATE_ID = "id";
  private static final String ACTIVITY_UPDATE_SOURCE_EVENT_ID =
      FlowNodeInstanceUpdateDto.Fields.sourceEventId;
  private static final String ACTIVITY_UPDATE_ACTIVITY_ID =
      FlowNodeInstanceUpdateDto.Fields.flowNodeId;
  private static final String ACTIVITY_UPDATE_ACTIVITY_TYPE =
      FlowNodeInstanceUpdateDto.Fields.flowNodeType;
  private static final String ACTIVITY_UPDATE_MAPPED_AS = FlowNodeInstanceUpdateDto.Fields.mappedAs;
  private static final String ACTIVITY_UPDATE_DATE = FlowNodeInstanceUpdateDto.Fields.date;

  protected EventProcessInstanceIndex(final String eventProcessId) {
    super(eventProcessId);
  }

  @Override
  public XContentBuilder addProperties(XContentBuilder builder) throws IOException {
    // @formatter:off
    XContentBuilder newBuilder =
        super.addProperties(builder)
            .startObject(PENDING_FLOW_NODE_UPDATES)
            .field("type", "object")
            .startObject("properties");
    addPendingEventUpdateObjectFields(newBuilder)
        .endObject()
        .endObject()
        .startObject(CORRELATED_EVENTS_BY_EVENT_ID)
        .field("type", "object")
        .field(MAPPING_ENABLED_SETTING, "false")
        .endObject();
    return newBuilder;
    // @formatter:on
  }

  @Override
  protected String getIndexPrefix() {
    return EVENT_PROCESS_INSTANCE_INDEX_PREFIX;
  }

  // This needs to be done separately to the logic of the constructor, because the non-static method
  // getIndexPrefix()
  // will get overridden when a subclass such as EventProcessInstanceIndex is being instantiated
  public static String constructIndexName(final String processInstanceIndexKey) {
    return EVENT_PROCESS_INSTANCE_INDEX_PREFIX
        + processInstanceIndexKey.toLowerCase(Locale.ENGLISH);
  }

  private XContentBuilder addPendingEventUpdateObjectFields(final XContentBuilder builder)
      throws IOException {
    // @formatter:off
    return builder
        .startObject(ACTIVITY_UPDATE_ID)
        .field("type", "keyword")
        .field("index", "false")
        .endObject()
        .startObject(ACTIVITY_UPDATE_SOURCE_EVENT_ID)
        .field("type", "keyword")
        .field("index", "false")
        .endObject()
        .startObject(ACTIVITY_UPDATE_ACTIVITY_ID)
        .field("type", "keyword")
        .field("index", "false")
        .endObject()
        .startObject(ACTIVITY_UPDATE_ACTIVITY_TYPE)
        .field("type", "keyword")
        .field("index", "false")
        .endObject()
        .startObject(ACTIVITY_UPDATE_MAPPED_AS)
        .field("type", "keyword")
        .field("index", "false")
        .endObject()
        .startObject(ACTIVITY_UPDATE_DATE)
        .field("type", "date")
        .field("format", OPTIMIZE_DATE_FORMAT)
        .field("index", "false")
        .endObject();
    // @formatter:on
  }
}
