/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index.events;

import io.camunda.optimize.dto.optimize.query.event.sequence.EventTraceStateDto;
import io.camunda.optimize.dto.optimize.query.event.sequence.TracedEventDto;
import io.camunda.optimize.service.db.DatabaseConstants;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import java.util.Locale;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class EventTraceStateIndex<TBuilder> extends DefaultIndexMappingCreator<TBuilder> {

  public static final String TRACE_ID = EventTraceStateDto.Fields.traceId;
  public static final String EVENT_TRACE = EventTraceStateDto.Fields.eventTrace;

  public static final String EVENT_ID = TracedEventDto.Fields.eventId;
  public static final String GROUP = TracedEventDto.Fields.group;
  public static final String SOURCE = TracedEventDto.Fields.source;
  public static final String EVENT_NAME = TracedEventDto.Fields.eventName;
  public static final String TIMESTAMP = TracedEventDto.Fields.timestamp;
  public static final String ORDER_COUNTER = TracedEventDto.Fields.orderCounter;

  public static final int VERSION = 2;
  private final String indexName;

  protected EventTraceStateIndex(final String indexKey) {
    this.indexName = constructIndexName(indexKey);
  }

  public static String constructIndexName(final String indexKey) {
    return DatabaseConstants.EVENT_TRACE_STATE_INDEX_PREFIX + indexKey.toLowerCase(Locale.ENGLISH);
  }

  @Override
  public String getIndexName() {
    return indexName;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(final XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    return xContentBuilder
        .startObject(TRACE_ID)
        .field("type", "keyword")
        .endObject()
        .startObject(EVENT_TRACE)
        .field("type", "nested")
        .startObject("properties")
        .startObject(EVENT_ID)
        .field("type", "keyword")
        .endObject()
        .startObject(GROUP)
        .field("type", "keyword")
        .endObject()
        .startObject(SOURCE)
        .field("type", "keyword")
        .endObject()
        .startObject(EVENT_NAME)
        .field("type", "keyword")
        .endObject()
        .startObject(TIMESTAMP)
        .field("type", "date")
        .endObject()
        .startObject(ORDER_COUNTER)
        .field("type", "keyword")
        .endObject()
        .endObject()
        .endObject();
    // @formatter:on
  }
}
