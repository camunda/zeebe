/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.test.util;

public enum ProcessReportDataType {

  RAW_DATA,

  PROC_INST_DUR_GROUP_BY_NONE,
  PROC_INST_DUR_GROUP_BY_NONE_WITH_PART,
  PROC_INST_DUR_GROUP_BY_START_DATE,
  PROC_INST_DUR_GROUP_BY_START_DATE_WITH_PART,
  PROC_INST_DUR_GROUP_BY_VARIABLE,
  PROC_INST_DUR_GROUP_BY_VARIABLE_WITH_PART,

  COUNT_PROC_INST_FREQ_GROUP_BY_NONE,
  COUNT_PROC_INST_FREQ_GROUP_BY_START_DATE,
  COUNT_PROC_INST_FREQ_GROUP_BY_VARIABLE,

  COUNT_FLOW_NODE_FREQ_GROUP_BY_FLOW_NODE,

  FLOW_NODE_DUR_GROUP_BY_FLOW_NODE,
}
