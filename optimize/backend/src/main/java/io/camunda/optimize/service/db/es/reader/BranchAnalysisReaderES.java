/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.reader;

import static io.camunda.optimize.dto.optimize.DefinitionType.PROCESS;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_ID;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_INSTANCES;
import static io.camunda.optimize.service.util.DefinitionQueryUtilES.createDefinitionQuery;
import static io.camunda.optimize.service.util.InstanceIndexUtil.getProcessInstanceIndexAliasName;
import static io.camunda.optimize.service.util.InstanceIndexUtil.isInstanceIndexNotFoundException;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import com.google.common.collect.Sets;
import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.ReportConstants;
import io.camunda.optimize.dto.optimize.query.analysis.BranchAnalysisOutcomeDto;
import io.camunda.optimize.dto.optimize.query.analysis.BranchAnalysisRequestDto;
import io.camunda.optimize.dto.optimize.query.analysis.BranchAnalysisResponseDto;
import io.camunda.optimize.service.DefinitionService;
import io.camunda.optimize.service.db.es.OptimizeElasticsearchClient;
import io.camunda.optimize.service.db.es.filter.FilterContext;
import io.camunda.optimize.service.db.es.filter.ProcessQueryFilterEnhancer;
import io.camunda.optimize.service.db.es.schema.index.ProcessInstanceIndexES;
import io.camunda.optimize.service.db.reader.BranchAnalysisReader;
import io.camunda.optimize.service.db.reader.ProcessDefinitionReader;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.configuration.condition.ElasticSearchCondition;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Conditional(ElasticSearchCondition.class)
public class BranchAnalysisReaderES implements BranchAnalysisReader {

  private final OptimizeElasticsearchClient esClient;
  private final DefinitionService definitionService;
  private final ProcessQueryFilterEnhancer queryFilterEnhancer;
  private final ProcessDefinitionReader processDefinitionReader;

  @Override
  public BranchAnalysisResponseDto branchAnalysis(
      final BranchAnalysisRequestDto request, final ZoneId timezone) {
    log.debug(
        "Performing branch analysis on process definition with key [{}] and versions [{}]",
        request.getProcessDefinitionKey(),
        request.getProcessDefinitionVersions());

    final BranchAnalysisResponseDto result = new BranchAnalysisResponseDto();
    getBpmnModelInstance(
            request.getProcessDefinitionKey(),
            request.getProcessDefinitionVersions(),
            request.getTenantIds())
        .ifPresent(
            bpmnModelInstance -> {
              final List<FlowNode> gatewayOutcomes =
                  fetchGatewayOutcomes(bpmnModelInstance, request.getGateway());
              final Set<String> flowNodeIdsWithMultipleIncomingSequenceFlows =
                  extractFlowNodesWithMultipleIncomingSequenceFlows(bpmnModelInstance);
              final FlowNode gateway = bpmnModelInstance.getModelElementById(request.getGateway());
              final FlowNode end = bpmnModelInstance.getModelElementById(request.getEnd());
              final boolean canReachEndFromGateway =
                  isPathPossible(gateway, end, Sets.newHashSet());

              for (FlowNode flowNode : gatewayOutcomes) {
                final Set<String> flowNodesToExcludeFromBranchAnalysis =
                    extractActivitiesToExclude(
                        gatewayOutcomes,
                        flowNodeIdsWithMultipleIncomingSequenceFlows,
                        flowNode.getId(),
                        request.getEnd());
                BranchAnalysisOutcomeDto branchAnalysis = new BranchAnalysisOutcomeDto();
                if (canReachEndFromGateway) {
                  branchAnalysis =
                      branchAnalysis(
                          flowNode, request, flowNodesToExcludeFromBranchAnalysis, timezone);
                } else {
                  branchAnalysis.setActivityId(flowNode.getId());
                  branchAnalysis.setActivitiesReached(
                      0L); // End event cannot be reached from gateway
                  branchAnalysis.setActivityCount(
                      (calculateFlowNodeCount(
                          flowNode.getId(),
                          request,
                          flowNodesToExcludeFromBranchAnalysis,
                          timezone)));
                }
                result.getFollowingNodes().put(branchAnalysis.getActivityId(), branchAnalysis);
              }

              result.setEndEvent(request.getEnd());
              result.setTotal(
                  calculateFlowNodeCount(
                      request.getEnd(), request, Collections.emptySet(), timezone));
            });

    return result;
  }

  private boolean isPathPossible(
      final FlowNode currentNode, final FlowNode targetNode, final Set<FlowNode> visitedNodes) {
    visitedNodes.add(currentNode);
    final List<FlowNode> succeedingNodes = currentNode.getSucceedingNodes().list();
    boolean pathFound = false;
    for (FlowNode succeedingNode : succeedingNodes) {
      if (visitedNodes.contains(succeedingNode)) {
        continue;
      }
      pathFound =
          succeedingNode.equals(targetNode)
              || isPathPossible(succeedingNode, targetNode, visitedNodes);
      if (pathFound) {
        break;
      }
    }
    return pathFound;
  }

  private Set<String> extractActivitiesToExclude(
      final List<FlowNode> gatewayOutcomes,
      final Set<String> flowNodeIdsWithMultipleIncomingSequenceFlows,
      final String currentFlowNodeId,
      final String endEventFlowNodeId) {
    Set<String> flowNodesToExcludeFromBranchAnalysis = new HashSet<>();
    for (FlowNode gatewayOutgoingNode : gatewayOutcomes) {
      String flowNodeId = gatewayOutgoingNode.getId();
      if (!flowNodeIdsWithMultipleIncomingSequenceFlows.contains(flowNodeId)) {
        flowNodesToExcludeFromBranchAnalysis.add(gatewayOutgoingNode.getId());
      }
    }
    flowNodesToExcludeFromBranchAnalysis.remove(currentFlowNodeId);
    flowNodesToExcludeFromBranchAnalysis.remove(endEventFlowNodeId);
    return flowNodesToExcludeFromBranchAnalysis;
  }

  private BranchAnalysisOutcomeDto branchAnalysis(
      final FlowNode flowNode,
      final BranchAnalysisRequestDto request,
      final Set<String> activitiesToExclude,
      final ZoneId timezone) {

    BranchAnalysisOutcomeDto result = new BranchAnalysisOutcomeDto();
    result.setActivityId(flowNode.getId());
    result.setActivityCount(
        calculateFlowNodeCount(flowNode.getId(), request, activitiesToExclude, timezone));
    result.setActivitiesReached(
        calculateReachedEndEventFlowNodeCount(
            flowNode.getId(), request, activitiesToExclude, timezone));

    return result;
  }

  private long calculateReachedEndEventFlowNodeCount(
      final String flowNodeId,
      final BranchAnalysisRequestDto request,
      final Set<String> activitiesToExclude,
      final ZoneId timezone) {
    final BoolQueryBuilder query =
        buildBaseQuery(request, activitiesToExclude)
            .must(createMustMatchFlowNodeIdQuery(request.getGateway()))
            .must(createMustMatchFlowNodeIdQuery(flowNodeId))
            .must(createMustMatchFlowNodeIdQuery(request.getEnd()));
    return executeQuery(request, query, timezone);
  }

  private long calculateFlowNodeCount(
      final String flowNodeId,
      final BranchAnalysisRequestDto request,
      final Set<String> activitiesToExclude,
      final ZoneId timezone) {
    final BoolQueryBuilder query =
        buildBaseQuery(request, activitiesToExclude)
            .must(createMustMatchFlowNodeIdQuery(request.getGateway()))
            .must(createMustMatchFlowNodeIdQuery(flowNodeId));
    return executeQuery(request, query, timezone);
  }

  private BoolQueryBuilder buildBaseQuery(
      final BranchAnalysisRequestDto request, final Set<String> activitiesToExclude) {
    final BoolQueryBuilder query =
        createDefinitionQuery(
            request.getProcessDefinitionKey(),
            request.getProcessDefinitionVersions(),
            request.getTenantIds(),
            new ProcessInstanceIndexES(request.getProcessDefinitionKey()),
            processDefinitionReader::getLatestVersionToKey);
    excludeFlowNodes(activitiesToExclude, query);
    return query;
  }

  private void excludeFlowNodes(
      final Set<String> flowNodeIdsToExclude, final BoolQueryBuilder query) {
    for (String excludeFlowNodeId : flowNodeIdsToExclude) {
      query.mustNot(createMustMatchFlowNodeIdQuery(excludeFlowNodeId));
    }
  }

  private NestedQueryBuilder createMustMatchFlowNodeIdQuery(final String flowNodeId) {
    return nestedQuery(
        FLOW_NODE_INSTANCES,
        termQuery(FLOW_NODE_INSTANCES + "." + FLOW_NODE_ID, flowNodeId),
        ScoreMode.None);
  }

  private long executeQuery(
      final BranchAnalysisRequestDto request, final BoolQueryBuilder query, final ZoneId timezone) {
    queryFilterEnhancer.addFilterToQuery(
        query, request.getFilter(), FilterContext.builder().timezone(timezone).build());
    final CountRequest searchRequest =
        new CountRequest(getProcessInstanceIndexAliasName(request.getProcessDefinitionKey()))
            .query(query);
    try {
      final CountResponse countResponse = esClient.count(searchRequest);
      return countResponse.getCount();
    } catch (IOException e) {
      String reason =
          String.format(
              "Was not able to perform branch analysis on process definition with key [%s] and versions [%s}]",
              request.getProcessDefinitionKey(), request.getProcessDefinitionVersions());
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    } catch (ElasticsearchStatusException e) {
      if (isInstanceIndexNotFoundException(PROCESS, e)) {
        log.info(
            "Was not able to perform branch analysis because the required instance index {} does not "
                + "exist. Returning 0 instead.",
            getProcessInstanceIndexAliasName(request.getProcessDefinitionKey()));
        return 0L;
      }
      throw e;
    }
  }

  private List<FlowNode> fetchGatewayOutcomes(
      final BpmnModelInstance bpmnModelInstance, final String gatewayFlowNodeId) {
    List<FlowNode> result = new ArrayList<>();
    FlowNode flowNode = bpmnModelInstance.getModelElementById(gatewayFlowNodeId);
    for (SequenceFlow sequence : flowNode.getOutgoing()) {
      result.add(sequence.getTarget());
    }
    return result;
  }

  private Optional<BpmnModelInstance> getBpmnModelInstance(
      final String definitionKey,
      final List<String> definitionVersions,
      final List<String> tenantIds) {
    final Optional<String> processDefinitionXml =
        tenantIds.stream()
            .map(
                tenantId ->
                    getDefinitionXml(
                        definitionKey, definitionVersions, Collections.singletonList(tenantId)))
            .filter(Optional::isPresent)
            .findFirst()
            .orElseGet(
                () ->
                    getDefinitionXml(
                        definitionKey, definitionVersions, ReportConstants.DEFAULT_TENANT_IDS));

    return processDefinitionXml.map(
        xml -> Bpmn.readModelFromStream(new ByteArrayInputStream(xml.getBytes())));
  }

  private Optional<String> getDefinitionXml(
      final String definitionKey,
      final List<String> definitionVersions,
      final List<String> tenants) {
    final Optional<ProcessDefinitionOptimizeDto> definitionWithXmlAsService =
        definitionService.getDefinitionWithXmlAsService(
            PROCESS, definitionKey, definitionVersions, tenants);
    return definitionWithXmlAsService.map(ProcessDefinitionOptimizeDto::getBpmn20Xml);
  }

  private Set<String> extractFlowNodesWithMultipleIncomingSequenceFlows(
      final BpmnModelInstance bpmnModelInstance) {
    Collection<SequenceFlow> sequenceFlowCollection =
        bpmnModelInstance.getModelElementsByType(SequenceFlow.class);
    Set<String> flowNodesWithOneIncomingSequenceFlow = new HashSet<>();
    Set<String> flowNodeIdsWithMultipleIncomingSequenceFlows = new HashSet<>();
    for (SequenceFlow sequenceFlow : sequenceFlowCollection) {
      String targetFlowNodeId = sequenceFlow.getTarget().getId();
      if (flowNodesWithOneIncomingSequenceFlow.contains(targetFlowNodeId)) {
        flowNodeIdsWithMultipleIncomingSequenceFlows.add(targetFlowNodeId);
      } else {
        flowNodesWithOneIncomingSequenceFlow.add(targetFlowNodeId);
      }
    }
    return flowNodeIdsWithMultipleIncomingSequenceFlows;
  }
}
