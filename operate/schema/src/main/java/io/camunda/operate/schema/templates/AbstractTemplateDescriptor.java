/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.schema.templates;

import static io.camunda.operate.schema.indices.AbstractIndexDescriptor.SCHEMA_FOLDER_ELASTICSEARCH;
import static io.camunda.operate.schema.indices.AbstractIndexDescriptor.SCHEMA_FOLDER_OPENSEARCH;

import io.camunda.operate.conditions.DatabaseInfo;
import io.camunda.operate.property.OperateProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractTemplateDescriptor implements TemplateDescriptor {

  private static final String SCHEMA_CREATE_TEMPLATE_JSON_ELASTICSEARCH =
      SCHEMA_FOLDER_ELASTICSEARCH + "/template/operate-%s.json";
  private static final String SCHEMA_CREATE_TEMPLATE_JSON_OPENSEARCH =
      SCHEMA_FOLDER_OPENSEARCH + "/template/operate-%s.json";

  @Autowired private OperateProperties operateProperties;

  private String indexPrefix;

  @Override
  public String getFullQualifiedName() {
    return String.format("%s-%s-%s_", getIndexPrefix(), getIndexName(), getVersion());
  }

  @Override
  public String getAllVersionsIndexNameRegexPattern() {
    return String.format("%s-%s-\\d.*", getIndexPrefix(), getIndexName());
  }

  @Override
  public String getSchemaClasspathFilename() {
    if (DatabaseInfo.isElasticsearch()) {
      return String.format(SCHEMA_CREATE_TEMPLATE_JSON_ELASTICSEARCH, getIndexName());
    } else {
      return String.format(SCHEMA_CREATE_TEMPLATE_JSON_OPENSEARCH, getIndexName());
    }
  }

  // FIXME clean this up when Operate exporter is converted to using Spring
  public String getIndexPrefix() {
    if (operateProperties != null) {
      return DatabaseInfo.isOpensearch()
          ? operateProperties.getOpensearch().getIndexPrefix()
          : operateProperties.getElasticsearch().getIndexPrefix();
    } else {
      return indexPrefix;
    }
  }

  public AbstractTemplateDescriptor setIndexPrefix(final String indexPrefix) {
    this.indexPrefix = indexPrefix;
    return this;
  }
}
