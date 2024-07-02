/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index.report;

import static io.camunda.optimize.service.db.DatabaseConstants.COMBINED_REPORT_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_ENABLED_SETTING;

import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class CombinedReportIndex<TBuilder> extends AbstractReportIndex<TBuilder> {

  public static final int VERSION = 5;

  public static final String VISUALIZATION = "visualization";
  public static final String CONFIGURATION = "configuration";

  public static final String REPORTS = "reports";
  public static final String REPORT_ITEM_ID = "id";
  public static final String REPORT_ITEM_COLOR = "color";

  @Override
  public String getIndexName() {
    return COMBINED_REPORT_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  protected XContentBuilder addReportTypeSpecificFields(XContentBuilder xContentBuilder)
      throws IOException {
    // @formatter:off
    return xContentBuilder
        .startObject(DATA)
        .field("type", "nested")
        .startObject("properties")
        .startObject(CONFIGURATION)
        .field(MAPPING_ENABLED_SETTING, false)
        .endObject()
        .startObject(VISUALIZATION)
        .field("type", "keyword")
        .endObject()
        .startObject(REPORTS)
        .field("type", "nested")
        .startObject("properties")
        .startObject(REPORT_ITEM_ID)
        .field("type", "keyword")
        .endObject()
        .startObject(REPORT_ITEM_COLOR)
        .field("type", "keyword")
        .endObject()
        .endObject()
        .endObject()
        .endObject()
        .endObject();
    // @formatter:on
  }
}
