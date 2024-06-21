/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.mixpanel.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MixpanelEntityEventProperties extends MixpanelEventProperties {

  @JsonProperty("entityId")
  private String entityId;

  public MixpanelEntityEventProperties(
      final String entityId,
      final String stage,
      final String organizationId,
      final String clusterId) {
    super(stage, organizationId, clusterId);
    this.entityId = entityId;
  }
}
