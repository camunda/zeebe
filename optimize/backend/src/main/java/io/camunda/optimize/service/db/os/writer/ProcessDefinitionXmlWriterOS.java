/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.writer;

import static io.camunda.optimize.service.db.DatabaseConstants.NUMBER_OF_RETRIES_ON_CONFLICT;
import static io.camunda.optimize.service.db.DatabaseConstants.PROCESS_DEFINITION_INDEX_NAME;
import static io.camunda.optimize.service.db.schema.index.ProcessDefinitionIndex.FLOW_NODE_DATA;
import static io.camunda.optimize.service.db.schema.index.ProcessDefinitionIndex.PROCESS_DEFINITION_XML;
import static io.camunda.optimize.service.db.schema.index.ProcessDefinitionIndex.USER_TASK_NAMES;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.service.db.os.OptimizeOpenSearchClient;
import io.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import io.camunda.optimize.service.db.writer.ProcessDefinitionXmlWriter;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.condition.OpenSearchCondition;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.UpdateOperation;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
@Conditional(OpenSearchCondition.class)
public class ProcessDefinitionXmlWriterOS implements ProcessDefinitionXmlWriter {
  private final OptimizeOpenSearchClient osClient;
  private final ConfigurationService configurationService;
  private final OptimizeIndexNameService indexNameService;
  private final ObjectMapper objectMapper;

  @Override
  public void importProcessDefinitionXmls(
      List<ProcessDefinitionOptimizeDto> processDefinitionOptimizeDtos) {
    String importItemName = "process definition information";
    log.debug("Writing [{}] {} to OS.", processDefinitionOptimizeDtos.size(), importItemName);
    osClient.doImportBulkRequestWithList(
        importItemName,
        processDefinitionOptimizeDtos,
        this::importProcessDefinitionBulkOperation,
        configurationService.getSkipDataAfterNestedDocLimitReached());
  }

  private BulkOperation importProcessDefinitionBulkOperation(
      final ProcessDefinitionOptimizeDto processDefinitionDto) {
    final Script script =
        OpenSearchWriterUtil.createFieldUpdateScript(
            Set.of(FLOW_NODE_DATA, USER_TASK_NAMES, PROCESS_DEFINITION_XML),
            processDefinitionDto,
            objectMapper);
    return new BulkOperation.Builder()
        .update(
            new UpdateOperation.Builder<ProcessDefinitionOptimizeDto>()
                .index(
                    indexNameService.getOptimizeIndexAliasForIndex(PROCESS_DEFINITION_INDEX_NAME))
                .id(processDefinitionDto.getId())
                .script(script)
                .upsert(processDefinitionDto)
                .retryOnConflict(NUMBER_OF_RETRIES_ON_CONFLICT)
                .build())
        .build();
  }
}
