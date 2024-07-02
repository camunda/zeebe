/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.migrate313to86.indices;

import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;

import io.camunda.optimize.dto.optimize.SettingsDto;
import io.camunda.optimize.service.db.DatabaseConstants;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public class SettingsIndexV2 extends DefaultIndexMappingCreator<XContentBuilder> {

  public static final int VERSION = 2;

  public static final String SHARING_ENABLED = SettingsDto.Fields.sharingEnabled.name();
  public static final String LAST_MODIFIED = SettingsDto.Fields.lastModified.name();

  @Override
  public String getIndexName() {
    return DatabaseConstants.SETTINGS_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(final XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    return xContentBuilder
        .startObject("metadataTelemetryEnabled")
        .field("type", "boolean")
        .endObject()
        .startObject(SHARING_ENABLED)
        .field("type", "boolean")
        .endObject()
        .startObject(LAST_MODIFIED)
        .field("type", "date")
        .field("format", OPTIMIZE_DATE_FORMAT)
        .endObject()
        .startObject("lastModifier")
        .field("type", "keyword")
        .endObject();
    // @formatter:on
  }

  @Override
  public XContentBuilder addStaticSetting(String key, int value, XContentBuilder contentBuilder)
      throws IOException {
    return contentBuilder.field(key, value);
  }
}
