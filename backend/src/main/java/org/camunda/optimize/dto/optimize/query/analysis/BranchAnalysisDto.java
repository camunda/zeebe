/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.analysis;

import java.util.HashMap;
import java.util.Map;


public class BranchAnalysisDto {
  protected String endEvent;
  protected Long total;

  protected Map<String, BranchAnalysisOutcomeDto> followingNodes = new HashMap<>();

  /**
   * The end event the branch analysis is referred to.
   */
  public String getEndEvent() {
    return endEvent;
  }

  public void setEndEvent(String endEvent) {
    this.endEvent = endEvent;
  }

  /**
   * The total amount of tokens that went from the gateway to the end event.
   */
  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  /**
   * All branch analysis information of the flow nodes from the gateway to the end event.
   */
  public Map<String, BranchAnalysisOutcomeDto> getFollowingNodes() {
    return followingNodes;
  }

  public void setFollowingNodes(Map<String, BranchAnalysisOutcomeDto> followingNodes) {
    this.followingNodes = followingNodes;
  }
}
