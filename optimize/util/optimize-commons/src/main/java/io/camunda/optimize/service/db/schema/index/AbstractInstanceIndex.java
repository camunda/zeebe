/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index;

import static io.camunda.optimize.service.db.DatabaseConstants.FORMAT_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.IGNORE_ABOVE_CHAR_LIMIT;
import static io.camunda.optimize.service.db.DatabaseConstants.IGNORE_ABOVE_SETTING;
import static io.camunda.optimize.service.db.DatabaseConstants.IGNORE_MALFORMED;
import static io.camunda.optimize.service.db.DatabaseConstants.LOWERCASE_NGRAM;
import static io.camunda.optimize.service.db.DatabaseConstants.LOWERCASE_NORMALIZER;
import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_PROPERTY_TYPE;
import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_DATE;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_DOUBLE;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_KEYWORD;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_LONG;
import static io.camunda.optimize.service.db.DatabaseConstants.TYPE_TEXT;

import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class AbstractInstanceIndex<TBuilder> extends DefaultIndexMappingCreator<TBuilder> {

  public static final String MULTIVALUE_FIELD_DATE = "date";
  public static final String MULTIVALUE_FIELD_LONG = "long";
  public static final String MULTIVALUE_FIELD_DOUBLE = "double";
  public static final String N_GRAM_FIELD = "nGramField";
  public static final String LOWERCASE_FIELD = "lowercaseField";

  public abstract String getDefinitionKeyFieldName();

  public abstract String getDefinitionVersionFieldName();

  public abstract String getTenantIdFieldName();

  protected XContentBuilder addValueMultifields(XContentBuilder builder) throws IOException {
    // @formatter:off
    return builder
        // search relevant fields
        .startObject(N_GRAM_FIELD)
        .field(MAPPING_PROPERTY_TYPE, TYPE_TEXT)
        .field("analyzer", LOWERCASE_NGRAM)
        .endObject()
        .startObject(LOWERCASE_FIELD)
        .field(MAPPING_PROPERTY_TYPE, TYPE_KEYWORD)
        .field("normalizer", LOWERCASE_NORMALIZER)
        .field(IGNORE_ABOVE_SETTING, IGNORE_ABOVE_CHAR_LIMIT)
        .endObject()
        // multi type fields
        .startObject(MULTIVALUE_FIELD_DATE)
        .field(MAPPING_PROPERTY_TYPE, TYPE_DATE)
        .field(FORMAT_PROPERTY_TYPE, OPTIMIZE_DATE_FORMAT)
        .field(IGNORE_MALFORMED, true)
        .endObject()
        .startObject(MULTIVALUE_FIELD_LONG)
        .field(MAPPING_PROPERTY_TYPE, TYPE_LONG)
        .field(IGNORE_MALFORMED, true)
        .endObject()
        .startObject(MULTIVALUE_FIELD_DOUBLE)
        .field(MAPPING_PROPERTY_TYPE, TYPE_DOUBLE)
        .field(IGNORE_MALFORMED, true)
        .endObject()
    // boolean is not supported to be ignored if malformed, see
    // https://github.com/elastic/elasticsearch/pull/29522
    // it is enough tough to just filter on the default string value with true/false at query time
    ;
    // @formatter:on
  }
}
