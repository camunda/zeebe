/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index;

import static io.camunda.optimize.service.db.DatabaseConstants.DYNAMIC_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.PROPERTIES_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_BOOLEAN;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_KEYWORD;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_OBJECT;

import io.camunda.optimize.dto.optimize.query.processoverview.ProcessDigestDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessDigestResponseDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessOverviewDto;
import io.camunda.optimize.service.db.DatabaseConstants;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class ProcessOverviewIndex<TBuilder> extends DefaultIndexMappingCreator<TBuilder> {
  public static final int VERSION = 2;

  public static final String PROCESS_DEFINITION_KEY =
      ProcessOverviewDto.Fields.processDefinitionKey;
  public static final String OWNER = ProcessOverviewDto.Fields.owner;
  public static final String DIGEST = ProcessOverviewDto.Fields.digest;
  public static final String LAST_KPI_EVALUATION =
      ProcessOverviewDto.Fields.lastKpiEvaluationResults;
  public static final String ENABLED = ProcessDigestResponseDto.Fields.enabled;
  public static final String KPI_REPORT_RESULTS = ProcessDigestDto.Fields.kpiReportResults;

  @Override
  public String getIndexName() {
    return DatabaseConstants.PROCESS_OVERVIEW_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    return xContentBuilder
        .startObject(PROCESS_DEFINITION_KEY)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject()
        .startObject(OWNER)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .endObject()
        .startObject(LAST_KPI_EVALUATION)
        .field(MAPPING_PROPERTY_TYPE, TYPE_OBJECT)
        .field(DYNAMIC_PROPERTY_TYPE, true)
        .endObject()
        .startObject(DIGEST)
        .field(MAPPING_PROPERTY_TYPE, TYPE_OBJECT)
        .startObject(PROPERTIES_PROPERTY_TYPE)
        .startObject(ENABLED)
        .field(MAPPING_PROPERTY_TYPE, TYPE_BOOLEAN)
        .endObject()
        .startObject(KPI_REPORT_RESULTS)
        .field(MAPPING_PROPERTY_TYPE, TYPE_OBJECT)
        .field(DYNAMIC_PROPERTY_TYPE, true)
        .endObject()
        .endObject()
        .endObject();
    // @formatter:on
  }
}
