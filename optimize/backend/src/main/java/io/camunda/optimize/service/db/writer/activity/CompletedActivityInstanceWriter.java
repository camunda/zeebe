/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.writer.activity;

import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_CANCELED;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_DEFINITION_KEY;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_DEFINITION_VERSION;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_END_DATE;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_ID;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_INSTANCES;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_INSTANCE_ID;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_TENANT_ID;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_TYPE;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.USER_TASK_INSTANCE_ID;
import static io.camunda.optimize.service.util.importing.EngineConstants.FLOW_NODE_TYPE_USER_TASK;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.camunda.optimize.service.db.repository.IndexRepository;
import io.camunda.optimize.service.db.repository.script.ActivityInstanceScriptFactory;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CompletedActivityInstanceWriter extends AbstractActivityInstanceWriter {
  Set<String> USER_TASK_FIELDS_TO_UPDATE =
      Set.of(
          FLOW_NODE_ID,
          USER_TASK_INSTANCE_ID,
          FLOW_NODE_INSTANCE_ID,
          FLOW_NODE_CANCELED,
          FLOW_NODE_DEFINITION_KEY,
          FLOW_NODE_DEFINITION_VERSION,
          FLOW_NODE_TENANT_ID);

  String UPDATE_USER_TASK_FIELDS_SCRIPT =
      USER_TASK_FIELDS_TO_UPDATE.stream()
          .map(fieldKey -> String.format("existingTask.%s = newFlowNode.%s;%n", fieldKey, fieldKey))
          .collect(Collectors.joining());

  public CompletedActivityInstanceWriter(
      final IndexRepository indexRepository, final ObjectMapper objectMapper) {
    super(objectMapper, indexRepository);
  }

  @Override
  protected String createInlineUpdateScript() {
    // new import flowNodeInstances should win over already imported flowNodeInstances, since those
    // might be running
    // instances.
    final StringSubstitutor substitutor =
        new StringSubstitutor(
            ImmutableMap.<String, String>builder()
                .put("flowNodesField", FLOW_NODE_INSTANCES)
                .put("flowNodeInstanceIdField", FLOW_NODE_INSTANCE_ID)
                .put("userTaskIdField", USER_TASK_INSTANCE_ID)
                .put("flowNodeTypeField", FLOW_NODE_TYPE)
                .put("userTaskFlowNodeType", FLOW_NODE_TYPE_USER_TASK)
                .put("flowNodeCanceledField", FLOW_NODE_CANCELED)
                .put("flowNodeEndDateField", FLOW_NODE_END_DATE)
                .build());

    return substitutor.replace(
        ActivityInstanceScriptFactory.createCompletedActivityInlineUpdateScript(
            UPDATE_USER_TASK_FIELDS_SCRIPT));
  }
}
