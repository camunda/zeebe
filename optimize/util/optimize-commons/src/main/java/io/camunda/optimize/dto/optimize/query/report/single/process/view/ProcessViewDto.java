/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.ImmutableSet;
import io.camunda.optimize.dto.optimize.query.report.Combinable;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@NoArgsConstructor
@Data
@FieldNameConstants
public class ProcessViewDto implements Combinable {
  private static final Set<ProcessViewEntity> FLOW_NODE_ENTITIES =
      ImmutableSet.of(ProcessViewEntity.FLOW_NODE, ProcessViewEntity.USER_TASK);
  private static final String COMMAND_KEY_SEPARATOR = "-";

  protected ProcessViewEntity entity;
  protected List<ViewProperty> properties = new ArrayList<>();

  public ProcessViewDto(ViewProperty property) {
    this(null, property);
  }

  public ProcessViewDto(final ProcessViewEntity entity, final ViewProperty property) {
    this.entity = entity;
    this.properties.add(property);
  }

  public ProcessViewDto(final ProcessViewEntity entity, final List<ViewProperty> properties) {
    this.entity = entity;
    this.properties = properties;
  }

  @Override
  public boolean isCombinable(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProcessViewDto)) {
      return false;
    }
    ProcessViewDto viewDto = (ProcessViewDto) o;
    if (getProperties().size() != 1 || viewDto.getProperties().size() != 1) {
      // multiple properties are not supported for combined reports
      return false;
    }
    return isEntityCombinable(viewDto) && isPropertyCombinable(viewDto);
  }

  public List<String> createCommandKeys() {
    return properties.stream()
        .distinct()
        .map(property -> entity + COMMAND_KEY_SEPARATOR + property)
        .collect(Collectors.toList());
  }

  @JsonIgnore
  public String createCommandKey() {
    return createCommandKeys().get(0);
  }

  @JsonIgnore
  public ViewProperty getFirstProperty() {
    return this.properties != null && !this.properties.isEmpty() ? properties.get(0) : null;
  }

  @JsonSetter
  public void setProperties(final List<ViewProperty> properties) {
    this.properties = new ArrayList<>(properties);
  }

  public void setProperties(final ViewProperty... properties) {
    this.properties = Arrays.asList(properties);
  }

  private boolean isEntityCombinable(final ProcessViewDto viewDto) {
    // note: user tasks are combinable with flow nodes as they are a subset of flow nodes
    return Objects.equals(entity, viewDto.entity)
        || (FLOW_NODE_ENTITIES.contains(entity) && FLOW_NODE_ENTITIES.contains(viewDto.entity));
  }

  private boolean isPropertyCombinable(final ProcessViewDto viewDto) {
    return Combinable.isCombinable(getFirstProperty(), viewDto.getFirstProperty());
  }
}
