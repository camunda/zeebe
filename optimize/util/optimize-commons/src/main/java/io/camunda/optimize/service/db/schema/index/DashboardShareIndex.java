/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index;

import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_ENABLED_SETTING;

import io.camunda.optimize.dto.optimize.query.dashboard.tile.DashboardReportTileDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.DimensionDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.PositionDto;
import io.camunda.optimize.service.db.DatabaseConstants;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class DashboardShareIndex<TBuilder> extends DefaultIndexMappingCreator<TBuilder> {

  public static final int VERSION = 4;

  public static final String ID = "id";
  public static final String DASHBOARD_ID = "dashboardId";
  public static final String TILE_SHARES = "tileShares";

  public static final String POSITION = DashboardReportTileDto.Fields.position;
  public static final String X_POSITION = PositionDto.Fields.x;
  public static final String Y_POSITION = PositionDto.Fields.y;

  public static final String DIMENSION = DashboardReportTileDto.Fields.dimensions;
  public static final String HEIGHT = DimensionDto.Fields.height;
  public static final String WIDTH = DimensionDto.Fields.width;

  public static final String REPORT_ID = DashboardReportTileDto.Fields.id;
  public static final String REPORT_TILE_TYPE = DashboardReportTileDto.Fields.type;
  public static final String REPORT_NAME = "name";

  public static final String CONFIGURATION = DashboardReportTileDto.Fields.configuration;

  @Override
  public String getIndexName() {
    return DatabaseConstants.DASHBOARD_SHARE_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    XContentBuilder newBuilder =
        xContentBuilder
            .startObject(ID)
            .field("type", "keyword")
            .endObject()
            .startObject(TILE_SHARES)
            .field("type", "nested")
            .startObject("properties");
    addNestedReportsField(newBuilder)
        .endObject()
        .endObject()
        .startObject(DASHBOARD_ID)
        .field("type", "keyword")
        .endObject();
    // @formatter:on
    return newBuilder;
  }

  private XContentBuilder addNestedReportsField(XContentBuilder builder) throws IOException {
    // @formatter:off
    XContentBuilder newBuilder =
        builder
            .startObject(REPORT_ID)
            .field("type", "keyword")
            .endObject()
            .startObject(REPORT_TILE_TYPE)
            .field("type", "keyword")
            .endObject()
            .startObject(REPORT_NAME)
            .field("type", "keyword")
            .endObject()
            .startObject(POSITION)
            .field("type", "nested")
            .startObject("properties");
    addNestedPositionField(newBuilder)
        .endObject()
        .endObject()
        .startObject(DIMENSION)
        .field("type", "nested")
        .startObject("properties");
    addNestedDimensionField(newBuilder)
        .endObject()
        .endObject()
        .startObject(CONFIGURATION)
        .field(MAPPING_ENABLED_SETTING, false)
        .endObject();
    // @formatter:on
    return newBuilder;
  }

  private XContentBuilder addNestedPositionField(XContentBuilder builder) throws IOException {
    // @formatter:off
    return builder
        .startObject(X_POSITION)
        .field("type", "keyword")
        .endObject()
        .startObject(Y_POSITION)
        .field("type", "keyword")
        .endObject();
    // @formatter:on
  }

  private XContentBuilder addNestedDimensionField(XContentBuilder builder) throws IOException {
    // @formatter:off
    return builder
        .startObject(WIDTH)
        .field("type", "keyword")
        .endObject()
        .startObject(HEIGHT)
        .field("type", "keyword")
        .endObject();
    // @formatter:on
  }
}
