/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index;

import static io.camunda.optimize.service.db.DatabaseConstants.DASHBOARD_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_BOOLEAN;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_DATE;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_KEYWORD;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_NESTED;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_OBJECT;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_TEXT;

import io.camunda.optimize.dto.optimize.query.dashboard.BaseDashboardDefinitionDto;
import io.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionRestDto;
import io.camunda.optimize.dto.optimize.query.dashboard.filter.DashboardFilterDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.DashboardReportTileDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.DimensionDto;
import io.camunda.optimize.dto.optimize.query.dashboard.tile.PositionDto;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class DashboardIndex<TBuilder> extends DefaultIndexMappingCreator<TBuilder> {

  public static final int VERSION = 8;

  public static final String ID = BaseDashboardDefinitionDto.Fields.id;
  public static final String NAME = BaseDashboardDefinitionDto.Fields.name;
  public static final String DESCRIPTION = BaseDashboardDefinitionDto.Fields.description;
  public static final String LAST_MODIFIED = BaseDashboardDefinitionDto.Fields.lastModified;
  public static final String CREATED = BaseDashboardDefinitionDto.Fields.created;
  public static final String OWNER = BaseDashboardDefinitionDto.Fields.owner;
  public static final String LAST_MODIFIER = BaseDashboardDefinitionDto.Fields.lastModifier;
  public static final String REFRESH_RATE_SECONDS =
      BaseDashboardDefinitionDto.Fields.refreshRateSeconds;
  public static final String TILES = DashboardDefinitionRestDto.Fields.tiles;
  public static final String COLLECTION_ID = BaseDashboardDefinitionDto.Fields.collectionId;
  public static final String MANAGEMENT_DASHBOARD =
      BaseDashboardDefinitionDto.Fields.managementDashboard;
  public static final String INSTANT_PREVIEW_DASHBOARD =
      BaseDashboardDefinitionDto.Fields.instantPreviewDashboard;
  public static final String AVAILABLE_FILTERS = BaseDashboardDefinitionDto.Fields.availableFilters;

  public static final String POSITION = DashboardReportTileDto.Fields.position;
  public static final String X_POSITION = PositionDto.Fields.x;
  public static final String Y_POSITION = PositionDto.Fields.y;

  public static final String DIMENSION = DashboardReportTileDto.Fields.dimensions;
  public static final String HEIGHT = DimensionDto.Fields.height;
  public static final String WIDTH = DimensionDto.Fields.width;

  public static final String REPORT_ID = DashboardReportTileDto.Fields.id;
  public static final String REPORT_TILE_TYPE = DashboardReportTileDto.Fields.type;
  public static final String CONFIGURATION = DashboardReportTileDto.Fields.configuration;

  public static final String FILTER_TYPE = "type";
  public static final String FILTER_DATA = DashboardFilterDto.Fields.data;

  @Override
  public String getIndexName() {
    return DASHBOARD_INDEX_NAME;
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
            .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
            .endObject()
            .startObject(NAME)
            .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
            .endObject()
            .startObject(DESCRIPTION)
            .field(MAPPING_PROPERTY_TYPE, TYPE_TEXT)
            .field("index", false)
            .endObject()
            .startObject(LAST_MODIFIED)
            .field(MAPPING_PROPERTY_TYPE, TYPE_DATE)
            .field("format", OPTIMIZE_DATE_FORMAT)
            .endObject()
            .startObject(CREATED)
            .field(MAPPING_PROPERTY_TYPE, TYPE_DATE)
            .field("format", OPTIMIZE_DATE_FORMAT)
            .endObject()
            .startObject(OWNER)
            .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
            .endObject()
            .startObject(LAST_MODIFIER)
            .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
            .endObject()
            .startObject(REFRESH_RATE_SECONDS)
            .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
            .endObject()
            .startObject(TILES)
            .field(MAPPING_PROPERTY_TYPE, TYPE_NESTED)
            .startObject("properties");
    addNestedReportsField(newBuilder)
        .endObject()
        .endObject()
        .startObject(COLLECTION_ID)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject()
        .startObject(MANAGEMENT_DASHBOARD)
        .field(MAPPING_PROPERTY_TYPE, TYPE_BOOLEAN)
        .endObject()
        .startObject(INSTANT_PREVIEW_DASHBOARD)
        .field(MAPPING_PROPERTY_TYPE, TYPE_BOOLEAN)
        .endObject()
        .startObject(AVAILABLE_FILTERS)
        .field(MAPPING_PROPERTY_TYPE, TYPE_OBJECT)
        .startObject("properties")
        .startObject(FILTER_TYPE)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject()
        .startObject(FILTER_DATA)
        .field("enabled", false)
        .endObject()
        .endObject()
        .endObject();
    // @formatter:on
    return newBuilder;
  }

  private XContentBuilder addNestedReportsField(XContentBuilder builder) throws IOException {
    // @formatter:off
    XContentBuilder newBuilder =
        builder
            .startObject(REPORT_ID)
            .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
            .endObject()
            .startObject(REPORT_TILE_TYPE)
            .field(REPORT_TILE_TYPE, TYPE_KEYWORD)
            .endObject()
            .startObject(POSITION)
            .field(MAPPING_PROPERTY_TYPE, TYPE_NESTED)
            .startObject("properties");
    addNestedPositionField(newBuilder)
        .endObject()
        .endObject()
        .startObject(DIMENSION)
        .field(MAPPING_PROPERTY_TYPE, TYPE_NESTED)
        .startObject("properties");
    addNestedDimensionField(newBuilder)
        .endObject()
        .endObject()
        .startObject(CONFIGURATION)
        .field("enabled", false)
        .endObject();
    // @formatter:on
    return newBuilder;
  }

  private XContentBuilder addNestedPositionField(XContentBuilder builder) throws IOException {
    // @formatter:off
    return builder
        .startObject(X_POSITION)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject()
        .startObject(Y_POSITION)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject();
    // @formatter:on
  }

  private XContentBuilder addNestedDimensionField(XContentBuilder builder) throws IOException {
    // @formatter:off
    return builder
        .startObject(WIDTH)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject()
        .startObject(HEIGHT)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject();
    // @formatter:on
  }
}
