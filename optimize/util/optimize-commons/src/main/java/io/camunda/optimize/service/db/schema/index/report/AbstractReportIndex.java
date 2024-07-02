/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index.report;

import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_BOOLEAN;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_DATE;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_KEYWORD;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_TEXT;

import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.SingleReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.SingleReportConfigurationDto;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class AbstractReportIndex<TBuilder> extends DefaultIndexMappingCreator<TBuilder> {

  public static final String ID = ReportDefinitionDto.Fields.id;
  public static final String NAME = ReportDefinitionDto.Fields.name;
  public static final String DESCRIPTION = ReportDefinitionDto.Fields.description;
  public static final String LAST_MODIFIED = ReportDefinitionDto.Fields.lastModified;
  public static final String CREATED = ReportDefinitionDto.Fields.created;
  public static final String OWNER = ReportDefinitionDto.Fields.owner;
  public static final String LAST_MODIFIER = ReportDefinitionDto.Fields.lastModifier;
  public static final String COLLECTION_ID = ReportDefinitionDto.Fields.collectionId;

  public static final String REPORT_TYPE = ReportDefinitionDto.Fields.reportType;
  public static final String COMBINED = ReportDefinitionDto.Fields.combined;
  public static final String DATA = ReportDefinitionDto.Fields.data;

  public static final String CONFIGURATION = SingleReportDataDto.Fields.configuration;
  public static final String XML = SingleReportConfigurationDto.Fields.xml;
  public static final String AGGREGATION_TYPES =
      SingleReportConfigurationDto.Fields.aggregationTypes;

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
            .startObject(COLLECTION_ID)
            .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
            .endObject()
            .startObject(REPORT_TYPE)
            .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
            .endObject()
            .startObject(COMBINED)
            .field(MAPPING_PROPERTY_TYPE, TYPE_BOOLEAN)
            .endObject();
    // @formatter:on
    newBuilder = addReportTypeSpecificFields(newBuilder);
    return newBuilder;
  }

  protected abstract XContentBuilder addReportTypeSpecificFields(XContentBuilder xContentBuilder)
      throws IOException;
}
