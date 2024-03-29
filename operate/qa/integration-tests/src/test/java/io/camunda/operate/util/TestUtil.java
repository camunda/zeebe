/*
 * Copyright Camunda Services GmbH
 *
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING, OR DISTRIBUTING THE SOFTWARE (“USE”), YOU INDICATE YOUR ACCEPTANCE TO AND ARE ENTERING INTO A CONTRACT WITH, THE LICENSOR ON THE TERMS SET OUT IN THIS AGREEMENT. IF YOU DO NOT AGREE TO THESE TERMS, YOU MUST NOT USE THE SOFTWARE. IF YOU ARE RECEIVING THE SOFTWARE ON BEHALF OF A LEGAL ENTITY, YOU REPRESENT AND WARRANT THAT YOU HAVE THE ACTUAL AUTHORITY TO AGREE TO THE TERMS AND CONDITIONS OF THIS AGREEMENT ON BEHALF OF SUCH ENTITY.
 * “Licensee” means you, an individual, or the entity on whose behalf you receive the Software.
 *
 * Permission is hereby granted, free of charge, to the Licensee obtaining a copy of this Software and associated documentation files to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject in each case to the following conditions:
 * Condition 1: If the Licensee distributes the Software or any derivative works of the Software, the Licensee must attach this Agreement.
 * Condition 2: Without limiting other conditions in this Agreement, the grant of rights is solely for non-production use as defined below.
 * "Non-production use" means any use of the Software that is not directly related to creating products, services, or systems that generate revenue or other direct or indirect economic benefits.  Examples of permitted non-production use include personal use, educational use, research, and development. Examples of prohibited production use include, without limitation, use for commercial, for-profit, or publicly accessible systems or use for commercial or revenue-generating purposes.
 *
 * If the Licensee is in breach of the Conditions, this Agreement, including the rights granted under it, will automatically terminate with immediate effect.
 *
 * SUBJECT AS SET OUT BELOW, THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * NOTHING IN THIS AGREEMENT EXCLUDES OR RESTRICTS A PARTY’S LIABILITY FOR (A) DEATH OR PERSONAL INJURY CAUSED BY THAT PARTY’S NEGLIGENCE, (B) FRAUD, OR (C) ANY OTHER LIABILITY TO THE EXTENT THAT IT CANNOT BE LAWFULLY EXCLUDED OR RESTRICTED.
 */
package io.camunda.operate.util;

import static io.camunda.operate.entities.ErrorType.JOB_NO_RETRIES;
import static io.camunda.operate.property.OperationExecutorProperties.LOCK_TIMEOUT_DEFAULT;
import static io.camunda.operate.schema.SchemaManager.OPERATE_DELETE_ARCHIVED_INDICES;
import static io.camunda.operate.schema.indices.IndexDescriptor.DEFAULT_TENANT_ID;
import static io.camunda.operate.util.OperateAbstractIT.DEFAULT_USER;

import io.camunda.operate.entities.BatchOperationEntity;
import io.camunda.operate.entities.FlowNodeState;
import io.camunda.operate.entities.FlowNodeType;
import io.camunda.operate.entities.IncidentEntity;
import io.camunda.operate.entities.IncidentState;
import io.camunda.operate.entities.OperationEntity;
import io.camunda.operate.entities.OperationState;
import io.camunda.operate.entities.OperationType;
import io.camunda.operate.entities.ProcessEntity;
import io.camunda.operate.entities.VariableEntity;
import io.camunda.operate.entities.dmn.DecisionInstanceEntity;
import io.camunda.operate.entities.dmn.DecisionInstanceInputEntity;
import io.camunda.operate.entities.dmn.DecisionInstanceOutputEntity;
import io.camunda.operate.entities.dmn.DecisionInstanceState;
import io.camunda.operate.entities.dmn.DecisionType;
import io.camunda.operate.entities.listview.FlowNodeInstanceForListViewEntity;
import io.camunda.operate.entities.listview.ProcessInstanceForListViewEntity;
import io.camunda.operate.entities.listview.ProcessInstanceState;
import io.camunda.operate.entities.listview.VariableForListViewEntity;
import io.camunda.operate.store.opensearch.client.sync.OpenSearchIndexOperations;
import io.camunda.operate.store.opensearch.client.sync.OpenSearchTemplateOperations;
import io.camunda.operate.store.opensearch.client.sync.RichOpenSearchClient;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indexlifecycle.DeleteLifecyclePolicyRequest;
import org.elasticsearch.client.indices.DeleteComposableIndexTemplateRequest;
import org.elasticsearch.client.indices.GetComposableIndexTemplateRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public abstract class TestUtil {

  public static final String ERROR_MSG = "No more retries left.";
  private static final Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);
  private static Random random = new Random();

  public static String createRandomString(int length) {
    return UUID.randomUUID().toString().substring(0, length);
  }

  public static ProcessInstanceForListViewEntity createProcessInstance(ProcessInstanceState state) {
    return createProcessInstance(state, null, false, null);
  }

  public static ProcessInstanceForListViewEntity createProcessInstance(
      ProcessInstanceState state, boolean incident, String tenantId) {
    return createProcessInstance(state, null, incident, tenantId);
  }

  public static ProcessInstanceForListViewEntity createProcessInstance(
      ProcessInstanceState state, Long processId, String tenantId) {
    return createProcessInstance(state, processId, null, null, false, tenantId);
  }

  public static ProcessInstanceForListViewEntity createProcessInstance(
      ProcessInstanceState state, Long processId) {
    return createProcessInstance(state, processId, null, null, false, null);
  }

  public static ProcessInstanceForListViewEntity createProcessInstance(
      ProcessInstanceState state, Long processId, boolean incident) {
    return createProcessInstance(state, processId, null, null, incident, null);
  }

  public static ProcessInstanceForListViewEntity createProcessInstance(
      ProcessInstanceState state, Long processId, boolean incident, String tenantId) {
    return createProcessInstance(state, processId, null, null, incident, tenantId);
  }

  public static ProcessInstanceForListViewEntity createProcessInstance(
      ProcessInstanceState state,
      Long processId,
      Long parentInstanceKey,
      String treePath,
      String tenantId) {
    return createProcessInstance(state, processId, parentInstanceKey, treePath, false, tenantId);
  }

  public static ProcessInstanceForListViewEntity createProcessInstance(
      ProcessInstanceState state,
      Long processId,
      Long parentInstanceKey,
      String treePath,
      boolean incident,
      String tenantId) {
    final ProcessInstanceForListViewEntity processInstance = createProcessInstanceEntityWithIds();

    processInstance.setStartDate(DateUtil.getRandomStartDate());
    if (state.equals(ProcessInstanceState.COMPLETED)
        || state.equals(ProcessInstanceState.CANCELED)) {
      final OffsetDateTime endDate = DateUtil.getRandomEndDate();
      processInstance.setEndDate(endDate);
    }
    processInstance.setState(state);
    if (processId != null) {
      processInstance.setProcessDefinitionKey(processId);
      processInstance.setBpmnProcessId("testProcess" + processId);
      // no process name to test sorting
      processInstance.setProcessVersion(random.nextInt(10));
    } else {
      final int i = random.nextInt(10);
      processInstance.setProcessDefinitionKey(Long.valueOf(i));
      processInstance.setBpmnProcessId("testProcess" + i);
      processInstance.setProcessName(UUID.randomUUID().toString());
      processInstance.setProcessVersion(i);
    }
    if (StringUtils.isEmpty(processInstance.getProcessName())) {
      processInstance.setProcessName(processInstance.getBpmnProcessId());
    }
    processInstance.setPartitionId(1);
    processInstance.setParentProcessInstanceKey(parentInstanceKey);
    if (treePath != null) {
      processInstance.setTreePath(treePath);
    } else {
      processInstance.setTreePath(new TreePath().startTreePath(processInstance.getId()).toString());
    }
    processInstance.setIncident(incident);
    processInstance.setTenantId(tenantId == null ? DEFAULT_TENANT_ID : tenantId);
    return processInstance;
  }

  public static ProcessInstanceForListViewEntity createProcessInstance(
      OffsetDateTime startDate, OffsetDateTime endDate) {
    final ProcessInstanceForListViewEntity processInstance = createProcessInstanceEntityWithIds();
    final int i = random.nextInt(10);
    processInstance.setBpmnProcessId("testProcess" + i);
    processInstance.setProcessName("Test process" + i);
    processInstance.setProcessVersion(i);
    processInstance.setStartDate(startDate);
    processInstance.setState(ProcessInstanceState.ACTIVE);
    if (endDate != null) {
      processInstance.setEndDate(endDate);
      processInstance.setState(ProcessInstanceState.COMPLETED);
    }
    processInstance.setPartitionId(1);
    return processInstance;
  }

  public static FlowNodeInstanceForListViewEntity createFlowNodeInstanceWithIncident(
      Long processInstanceKey, FlowNodeState state, String errorMsg) {
    final FlowNodeInstanceForListViewEntity activityInstanceForListViewEntity =
        createFlowNodeInstance(processInstanceKey, state);
    createIncident(activityInstanceForListViewEntity, errorMsg);
    return activityInstanceForListViewEntity;
  }

  public static void createIncident(
      FlowNodeInstanceForListViewEntity activityInstanceForListViewEntity, String errorMsg) {
    activityInstanceForListViewEntity.setIncident(true);
    if (errorMsg != null) {
      activityInstanceForListViewEntity.setErrorMessage(errorMsg);
    } else {
      activityInstanceForListViewEntity.setErrorMessage(ERROR_MSG);
    }
  }

  public static FlowNodeInstanceForListViewEntity createFlowNodeInstance(
      Long processInstanceKey, FlowNodeState state) {
    return createFlowNodeInstance(processInstanceKey, state, "start", null, null);
  }

  public static FlowNodeInstanceForListViewEntity createFlowNodeInstance(
      Long processInstanceKey, FlowNodeState state, String activityId, FlowNodeType activityType) {
    return createFlowNodeInstance(processInstanceKey, state, activityId, activityType, null);
  }

  public static FlowNodeInstanceForListViewEntity createFlowNodeInstance(
      Long processInstanceKey,
      FlowNodeState state,
      String activityId,
      FlowNodeType activityType,
      Boolean retriesLeft) {
    final FlowNodeInstanceForListViewEntity activityInstanceEntity =
        new FlowNodeInstanceForListViewEntity();
    activityInstanceEntity.setProcessInstanceKey(processInstanceKey);
    final Long activityInstanceId = random.nextLong();
    activityInstanceEntity.setId(activityInstanceId.toString());
    activityInstanceEntity.setActivityId(activityId);
    activityInstanceEntity.setActivityType(activityType);
    activityInstanceEntity.setActivityState(state);
    activityInstanceEntity.getJoinRelation().setParent(processInstanceKey);
    activityInstanceEntity.setPartitionId(1);
    if (retriesLeft != null) {
      activityInstanceEntity.setJobFailedWithRetriesLeft(retriesLeft);
    }
    return activityInstanceEntity;
  }

  public static FlowNodeInstanceForListViewEntity createFlowNodeInstance(
      Long processInstanceKey, FlowNodeState state, String activityId) {
    return createFlowNodeInstance(
        processInstanceKey, state, activityId, FlowNodeType.SERVICE_TASK, null);
  }

  public static ProcessInstanceForListViewEntity createProcessInstanceEntity(
      ProcessInstanceState state, Long processDefinitionKey, String bpmnProcessId) {
    return createProcessInstanceEntity(state, processDefinitionKey, bpmnProcessId, false);
  }

  public static ProcessInstanceForListViewEntity createProcessInstanceEntity(
      ProcessInstanceState state,
      Long processDefinitionKey,
      String bpmnProcessId,
      boolean incident) {
    final ProcessInstanceForListViewEntity processInstance = createProcessInstanceEntityWithIds();
    final int i = random.nextInt(10);
    processInstance.setBpmnProcessId(bpmnProcessId);
    processInstance.setProcessName("Test process" + i);
    processInstance.setProcessVersion(i);
    processInstance.setStartDate(DateUtil.getRandomStartDate());
    if (state.equals(ProcessInstanceState.COMPLETED)
        || state.equals(ProcessInstanceState.CANCELED)) {
      final OffsetDateTime endDate = DateUtil.getRandomEndDate();
      processInstance.setEndDate(endDate);
    }
    processInstance.setState(state);
    processInstance.setProcessDefinitionKey(processDefinitionKey);
    processInstance.setPartitionId(1);
    processInstance.setIncident(incident);
    return processInstance;
  }

  public static ProcessInstanceForListViewEntity createProcessInstanceEntityWithIds() {
    final ProcessInstanceForListViewEntity processInstance = new ProcessInstanceForListViewEntity();
    final Long processInstanceKey = Math.abs(random.nextLong());
    processInstance.setId(processInstanceKey.toString());
    processInstance.setProcessInstanceKey(processInstanceKey);
    processInstance.setKey(processInstanceKey);
    processInstance.setPartitionId(1);
    processInstance.setTreePath(
        new TreePath().startTreePath(processInstanceKey.toString()).toString());
    return processInstance;
  }

  public static ProcessInstanceForListViewEntity createProcessInstanceEntity(
      OffsetDateTime startDate, OffsetDateTime endDate) {
    final ProcessInstanceForListViewEntity processInstance = createProcessInstanceEntityWithIds();
    final int i = random.nextInt(10);
    processInstance.setBpmnProcessId("testProcess" + i);
    processInstance.setProcessName("Test process" + i);
    processInstance.setProcessVersion(i);
    processInstance.setStartDate(startDate);
    processInstance.setState(ProcessInstanceState.ACTIVE);
    if (endDate != null) {
      processInstance.setEndDate(endDate);
      processInstance.setState(ProcessInstanceState.COMPLETED);
    }
    processInstance.setPartitionId(1);
    return processInstance;
  }

  public static IncidentEntity createIncident(IncidentState state) {
    return createIncident(state, "start", random.nextLong(), null);
  }

  public static IncidentEntity createIncident(
      IncidentState state, Long incidentKey, Long processInstanceKey) {
    return createIncident(
        state, "start", random.nextLong(), null, incidentKey, processInstanceKey, null, null);
  }

  public static IncidentEntity createIncident(
      IncidentState state, Long incidentKey, Long processInstanceKey, Long processDefinitionKey) {
    return createIncident(
        state,
        "start",
        random.nextLong(),
        null,
        incidentKey,
        processInstanceKey,
        processDefinitionKey,
        null);
  }

  public static IncidentEntity createIncident(IncidentState state, String errorMsg) {
    return createIncident(state, "start", random.nextLong(), errorMsg);
  }

  public static IncidentEntity createIncident(
      IncidentState state, String activityId, Long activityInstanceId) {
    return createIncident(state, activityId, activityInstanceId, null);
  }

  public static IncidentEntity createIncident(
      IncidentState state, String activityId, Long activityInstanceId, String errorMsg) {
    return createIncident(state, activityId, activityInstanceId, errorMsg, null);
  }

  public static IncidentEntity createIncident(
      IncidentState state,
      String activityId,
      Long activityInstanceId,
      String errorMsg,
      Long incidentKey) {
    return createIncident(
        state, activityId, activityInstanceId, errorMsg, incidentKey, null, null, null);
  }

  public static IncidentEntity createIncident(
      IncidentState state,
      String activityId,
      Long activityInstanceId,
      String errorMsg,
      Long incidentKey,
      Long processInstanceKey,
      Long processDefinitionKey,
      String bpmnProcessId) {
    final IncidentEntity incidentEntity = new IncidentEntity();
    if (incidentKey == null) {
      incidentEntity.setKey(random.nextLong());
      incidentEntity.setId(String.valueOf(incidentEntity.getKey()));
    } else {
      incidentEntity.setKey(incidentKey);
      incidentEntity.setId(String.valueOf(incidentKey));
    }
    incidentEntity.setFlowNodeId(activityId);
    incidentEntity.setFlowNodeInstanceKey(activityInstanceId);
    incidentEntity.setErrorType(JOB_NO_RETRIES);
    if (errorMsg == null) {
      incidentEntity.setErrorMessage(ERROR_MSG);
    } else {
      incidentEntity.setErrorMessage(errorMsg);
    }
    incidentEntity.setState(state);
    incidentEntity.setPartitionId(1);
    incidentEntity.setProcessInstanceKey(processInstanceKey);
    incidentEntity.setTreePath(
        new TreePath()
            .startTreePath(String.valueOf(processInstanceKey))
            .appendFlowNode(activityId)
            .appendFlowNodeInstance(String.valueOf(activityInstanceId))
            .toString());
    if (processDefinitionKey != null) {
      incidentEntity.setProcessDefinitionKey(processDefinitionKey);
    }
    incidentEntity.setBpmnProcessId(bpmnProcessId);
    return incidentEntity;
  }

  public static List<ProcessEntity> createProcessVersions(
      String bpmnProcessId, String name, int versionsCount, String tenantId) {
    final List<ProcessEntity> result = new ArrayList<>();
    final Random processIdGenerator = new Random();
    for (int i = 1; i <= versionsCount; i++) {
      final ProcessEntity processEntity = new ProcessEntity();
      final Long processId = processIdGenerator.nextLong();
      processEntity.setKey(processId);
      processEntity.setId(processId.toString());
      processEntity.setBpmnProcessId(bpmnProcessId);
      processEntity.setTenantId(tenantId);
      processEntity.setName(name + i);
      processEntity.setVersion(i);
      result.add(processEntity);
    }
    return result;
  }

  public static VariableForListViewEntity createVariableForListView(
      Long processInstanceKey, Long scopeKey, String name, String value) {
    final VariableForListViewEntity variable = new VariableForListViewEntity();
    variable.setId(scopeKey + "_" + name);
    variable.setProcessInstanceKey(processInstanceKey);
    variable.setScopeKey(scopeKey);
    variable.setVarName(name);
    variable.setVarValue(value);
    variable.getJoinRelation().setParent(processInstanceKey);
    return variable;
  }

  public static VariableEntity createVariable(
      Long processInstanceKey, Long scopeKey, String name, String value) {
    return createVariable(processInstanceKey, null, null, scopeKey, name, value);
  }

  public static VariableEntity createVariable(
      Long processInstanceKey,
      Long processDefinitionKey,
      String bpmnProcessId,
      Long scopeKey,
      String name,
      String value) {
    final VariableEntity variable = new VariableEntity();
    variable.setId(scopeKey + "_" + name);
    variable.setProcessInstanceKey(processInstanceKey);
    variable.setProcessDefinitionKey(processDefinitionKey);
    variable.setBpmnProcessId(bpmnProcessId);
    variable.setScopeKey(scopeKey);
    variable.setName(name);
    variable.setName(value);
    return variable;
  }

  public static void removeAllIndices(RestHighLevelClient esClient, String prefix) {
    try {
      LOGGER.info("Removing indices");
      final var indexResponses =
          esClient.indices().get(new GetIndexRequest(prefix + "*"), RequestOptions.DEFAULT);
      for (final String index : indexResponses.getIndices()) {
        esClient.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
      }
      final var templateResponses =
          esClient
              .indices()
              .getIndexTemplate(
                  new GetComposableIndexTemplateRequest(prefix + "*"), RequestOptions.DEFAULT);
      for (final String template : templateResponses.getIndexTemplates().keySet()) {
        esClient
            .indices()
            .deleteIndexTemplate(
                new DeleteComposableIndexTemplateRequest(template), RequestOptions.DEFAULT);
      }
    } catch (ElasticsearchStatusException | IOException ex) {
      LOGGER.error(ex.getMessage(), ex);
    }
  }

  public static void removeAllIndices(
      OpenSearchIndexOperations indexOperations,
      OpenSearchTemplateOperations templateOperations,
      String prefix) {
    try {
      LOGGER.info("Removing indices");
      indexOperations.deleteIndicesWithRetries(prefix + "*");
      templateOperations.deleteTemplatesWithRetries(prefix + "*");
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
    }
  }

  public static void removeIlmPolicy(RestHighLevelClient esClient) {
    try {
      LOGGER.info("Removing ILM policy " + OPERATE_DELETE_ARCHIVED_INDICES);
      final var request = new DeleteLifecyclePolicyRequest(OPERATE_DELETE_ARCHIVED_INDICES);
      esClient.indexLifecycle().deleteLifecyclePolicy(request, RequestOptions.DEFAULT);
    } catch (ElasticsearchStatusException | IOException ex) {
      LOGGER.error(ex.getMessage(), ex);
    }
  }

  public static void removeIlmPolicy(RichOpenSearchClient richOpenSearchClient) {
    try {
      LOGGER.info("Removing ILM policy " + OPERATE_DELETE_ARCHIVED_INDICES);
      richOpenSearchClient.ism().deletePolicy(OPERATE_DELETE_ARCHIVED_INDICES);
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
    }
  }

  public static OperationEntity createOperationEntity(
      Long processInstanceKey, Long incidentKey, String varName, String username) {
    return createOperationEntity(
        processInstanceKey, incidentKey, varName, OperationState.SCHEDULED, username, false);
  }

  public static OperationEntity createOperationEntity(
      Long processInstanceKey,
      Long incidentKey,
      String varName,
      OperationState state,
      String username,
      boolean lockExpired) {
    return createOperationEntity(
        processInstanceKey, null, null, incidentKey, varName, state, username, lockExpired);
  }

  public static OperationEntity createOperationEntity(
      Long processInstanceKey,
      Long processDefinitionKey,
      String bpmnProcessId,
      Long incidentKey,
      String varName,
      OperationState state,
      String username,
      boolean lockExpired) {
    final OperationEntity oe = new OperationEntity();
    oe.generateId();
    oe.setProcessInstanceKey(processInstanceKey);
    oe.setProcessDefinitionKey(processDefinitionKey);
    oe.setBpmnProcessId(bpmnProcessId);
    oe.setIncidentKey(incidentKey);
    oe.setVariableName(varName);
    oe.setType(OperationType.RESOLVE_INCIDENT);
    if (username != null) {
      oe.setUsername(username);
    } else {
      oe.setUsername(DEFAULT_USER);
    }
    oe.setState(state);
    if (state.equals(OperationState.LOCKED)) {
      if (lockExpired) {
        oe.setLockExpirationTime(OffsetDateTime.now().minus(1, ChronoUnit.MILLIS));
      } else {
        oe.setLockExpirationTime(
            OffsetDateTime.now().plus(LOCK_TIMEOUT_DEFAULT, ChronoUnit.MILLIS));
      }
      oe.setLockOwner("otherWorkerId");
    }
    return oe;
  }

  public static OperationEntity createOperationEntity(
      Long processInstanceKey, OperationState state, boolean lockExpired) {
    return createOperationEntity(processInstanceKey, null, null, state, null, lockExpired);
  }

  public static OperationEntity createOperationEntity(
      Long processInstanceKey, OperationState state) {
    return createOperationEntity(processInstanceKey, null, null, state, null, false);
  }

  public static BatchOperationEntity createBatchOperationEntity(
      OffsetDateTime startDate, OffsetDateTime endDate, String username) {
    return new BatchOperationEntity()
        .setId(UUID.randomUUID().toString())
        .setStartDate(startDate)
        .setEndDate(endDate)
        .setUsername(username)
        .setType(OperationType.CANCEL_PROCESS_INSTANCE);
  }

  public static DecisionInstanceEntity createDecisionInstanceEntity() {
    final DecisionInstanceEntity decisionInstance = new DecisionInstanceEntity();
    final long key = Math.abs(random.nextLong());
    decisionInstance
        .setId(String.valueOf(key))
        .setKey(key)
        .setDecisionId(UUID.randomUUID().toString())
        .setDecisionDefinitionId(String.valueOf(Math.abs(random.nextLong())))
        .setDecisionId("decisionId")
        .setDecisionName("Decision Name")
        .setDecisionRequirementsId(UUID.randomUUID().toString())
        .setDecisionRequirementsKey(Math.abs(random.nextLong()))
        .setDecisionType(DecisionType.DECISION_TABLE)
        .setElementId("businessTask")
        .setElementInstanceKey(Math.abs(random.nextLong()))
        .setEvaluationDate(OffsetDateTime.now())
        .setPosition(Math.abs(random.nextLong()))
        .setProcessDefinitionKey(Math.abs(random.nextLong()))
        .setProcessInstanceKey(Math.abs(random.nextLong()))
        .setResult("someJSON")
        .setState(DecisionInstanceState.EVALUATED)
        .setEvaluatedInputs(createDecisionInstanceInputs())
        .setEvaluatedOutputs(createDecisionOutputs());
    return decisionInstance;
  }

  private static List<DecisionInstanceOutputEntity> createDecisionOutputs() {
    final List<DecisionInstanceOutputEntity> outputs = new ArrayList<>();
    outputs.add(
        new DecisionInstanceOutputEntity()
            .setId("output1")
            .setName("Output 1")
            .setValue("output1")
            .setRuleId("rule1")
            .setRuleIndex(1));
    outputs.add(
        new DecisionInstanceOutputEntity()
            .setId("output2")
            .setName("Output 2")
            .setValue("output2")
            .setRuleId("rule2")
            .setRuleIndex(2));
    return outputs;
  }

  private static List<DecisionInstanceInputEntity> createDecisionInstanceInputs() {
    final List<DecisionInstanceInputEntity> inputs = new ArrayList<>();
    inputs.add(
        new DecisionInstanceInputEntity().setId("input1").setName("Input 1").setValue("value1"));
    inputs.add(
        new DecisionInstanceInputEntity().setId("input2").setName("Input 2").setValue("value2"));
    return inputs;
  }
}
