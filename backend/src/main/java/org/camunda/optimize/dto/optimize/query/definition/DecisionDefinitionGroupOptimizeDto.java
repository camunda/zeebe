/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.definition;

import org.camunda.optimize.dto.optimize.OptimizeDto;
import org.camunda.optimize.dto.optimize.importing.DecisionDefinitionOptimizeDto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DecisionDefinitionGroupOptimizeDto extends KeyDefinitionOptimizeDto
  implements Serializable, OptimizeDto {

  protected List<DecisionDefinitionOptimizeDto> versions = new ArrayList<>();

  public List<DecisionDefinitionOptimizeDto> getVersions() {
    return versions;
  }

  public void setVersions(List<DecisionDefinitionOptimizeDto> versions) {
    this.versions = versions;
  }

  public void sort() {
    versions.sort(
      Comparator.comparing(
        DecisionDefinitionOptimizeDto::getVersion, Comparator.comparing(Long::valueOf)
      ).reversed()
    );
  }
}
