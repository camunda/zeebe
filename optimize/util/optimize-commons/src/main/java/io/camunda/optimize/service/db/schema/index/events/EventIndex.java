/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index.events;

import static io.camunda.optimize.service.db.DatabaseConstants.FIELDS;
import static io.camunda.optimize.service.db.DatabaseConstants.LOWERCASE_NGRAM;
import static io.camunda.optimize.service.db.DatabaseConstants.LOWERCASE_NORMALIZER;
import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_ENABLED_SETTING;

import io.camunda.optimize.dto.optimize.query.event.process.EventDto;
import io.camunda.optimize.service.db.DatabaseConstants;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class EventIndex<TBuilder> extends DefaultIndexMappingCreator<TBuilder> {

  public static final String ID = EventDto.Fields.id;
  public static final String EVENT_NAME = EventDto.Fields.eventName;
  public static final String TRACE_ID = EventDto.Fields.traceId;
  public static final String TIMESTAMP = EventDto.Fields.timestamp;
  public static final String INGESTION_TIMESTAMP = EventDto.Fields.ingestionTimestamp;
  public static final String GROUP = EventDto.Fields.group;
  public static final String SOURCE = EventDto.Fields.source;
  public static final String DATA = EventDto.Fields.data;
  public static final String N_GRAM_FIELD = "nGramField";

  public static final int VERSION = 4;

  @Override
  public String getIndexName() {
    return DatabaseConstants.EXTERNAL_EVENTS_INDEX_NAME;
  }

  @Override
  public String getIndexNameInitialSuffix() {
    return DatabaseConstants.INDEX_SUFFIX_PRE_ROLLOVER;
  }

  @Override
  public boolean isCreateFromTemplate() {
    return true;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(final XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    return xContentBuilder
        .startObject(ID)
        .field("type", "keyword")
        .endObject()
        .startObject(EVENT_NAME)
        .field("type", "keyword")
        .startObject(FIELDS)
        .startObject(N_GRAM_FIELD)
        .field("type", "text")
        .field(ANALYZER, LOWERCASE_NGRAM)
        .endObject()
        .startObject(LOWERCASE)
        .field("type", "keyword")
        .field(NORMALIZER, LOWERCASE_NORMALIZER)
        .endObject()
        .endObject()
        .endObject()
        .startObject(TRACE_ID)
        .field("type", "keyword")
        .startObject(FIELDS)
        .startObject(N_GRAM_FIELD)
        .field("type", "text")
        .field(ANALYZER, LOWERCASE_NGRAM)
        .endObject()
        .startObject(LOWERCASE)
        .field("type", "keyword")
        .field(NORMALIZER, LOWERCASE_NORMALIZER)
        .endObject()
        .endObject()
        .endObject()
        .startObject(TIMESTAMP)
        .field("type", "date")
        .endObject()
        .startObject(INGESTION_TIMESTAMP)
        .field("type", "date")
        .endObject()
        .startObject(GROUP)
        .field("type", "keyword")
        .startObject(FIELDS)
        .startObject(N_GRAM_FIELD)
        .field("type", "text")
        .field(ANALYZER, LOWERCASE_NGRAM)
        .endObject()
        .startObject(LOWERCASE)
        .field("type", "keyword")
        .field(NORMALIZER, LOWERCASE_NORMALIZER)
        .endObject()
        .endObject()
        .endObject()
        .startObject(SOURCE)
        .field("type", "keyword")
        .startObject(FIELDS)
        .startObject(N_GRAM_FIELD)
        .field("type", "text")
        .field(ANALYZER, LOWERCASE_NGRAM)
        .endObject()
        .startObject(LOWERCASE)
        .field("type", "keyword")
        .field(NORMALIZER, LOWERCASE_NORMALIZER)
        .endObject()
        .endObject()
        .endObject()
        .startObject(DATA)
        .field(MAPPING_ENABLED_SETTING, false)
        .endObject();
    // @formatter:on
  }
}
