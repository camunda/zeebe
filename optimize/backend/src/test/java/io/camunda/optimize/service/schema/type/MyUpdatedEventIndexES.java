/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.schema.type;

import static io.camunda.optimize.service.db.DatabaseConstants.DEFAULT_SHARD_NUMBER;
import static io.camunda.optimize.service.db.DatabaseConstants.NUMBER_OF_SHARDS_SETTING;

import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.xcontent.XContentBuilder;

@Slf4j
public class MyUpdatedEventIndexES extends MyUpdatedEventIndex<XContentBuilder> {

  @Override
  public XContentBuilder getStaticSettings(
      final XContentBuilder xContentBuilder, final ConfigurationService configurationService)
      throws IOException {
    return xContentBuilder.field(NUMBER_OF_SHARDS_SETTING, DEFAULT_SHARD_NUMBER);
  }
}
