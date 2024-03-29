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
package io.camunda.operate.webapp.rest;

import static io.camunda.operate.entities.OperationType.*;
import static io.camunda.operate.webapp.rest.ProcessInstanceRestService.PROCESS_INSTANCE_URL;

import io.camunda.operate.Metrics;
import io.camunda.operate.entities.BatchOperationEntity;
import io.camunda.operate.entities.OperationType;
import io.camunda.operate.entities.SequenceFlowEntity;
import io.camunda.operate.store.SequenceFlowStore;
import io.camunda.operate.util.CollectionUtil;
import io.camunda.operate.util.rest.ValidLongId;
import io.camunda.operate.webapp.InternalAPIErrorController;
import io.camunda.operate.webapp.elasticsearch.reader.ProcessInstanceReader;
import io.camunda.operate.webapp.reader.FlowNodeInstanceReader;
import io.camunda.operate.webapp.reader.FlowNodeStatisticsReader;
import io.camunda.operate.webapp.reader.IncidentReader;
import io.camunda.operate.webapp.reader.ListViewReader;
import io.camunda.operate.webapp.reader.OperationReader;
import io.camunda.operate.webapp.reader.VariableReader;
import io.camunda.operate.webapp.rest.dto.*;
import io.camunda.operate.webapp.rest.dto.activity.FlowNodeStateDto;
import io.camunda.operate.webapp.rest.dto.incidents.IncidentResponseDto;
import io.camunda.operate.webapp.rest.dto.listview.ListViewProcessInstanceDto;
import io.camunda.operate.webapp.rest.dto.listview.ListViewQueryDto;
import io.camunda.operate.webapp.rest.dto.listview.ListViewRequestDto;
import io.camunda.operate.webapp.rest.dto.listview.ListViewResponseDto;
import io.camunda.operate.webapp.rest.dto.metadata.FlowNodeMetadataDto;
import io.camunda.operate.webapp.rest.dto.metadata.FlowNodeMetadataRequestDto;
import io.camunda.operate.webapp.rest.dto.operation.CreateBatchOperationRequestDto;
import io.camunda.operate.webapp.rest.dto.operation.CreateOperationRequestDto;
import io.camunda.operate.webapp.rest.dto.operation.MigrationPlanDto;
import io.camunda.operate.webapp.rest.dto.operation.ModifyProcessInstanceRequestDto;
import io.camunda.operate.webapp.rest.exception.InvalidRequestException;
import io.camunda.operate.webapp.rest.exception.NotAuthorizedException;
import io.camunda.operate.webapp.security.identity.IdentityPermission;
import io.camunda.operate.webapp.security.identity.PermissionsService;
import io.camunda.operate.webapp.writer.BatchOperationWriter;
import io.camunda.operate.webapp.zeebe.operation.ModifyProcessInstanceRequestValidator;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.ConstraintViolationException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = PROCESS_INSTANCE_URL)
@Validated
public class ProcessInstanceRestService extends InternalAPIErrorController {

  public static final String PROCESS_INSTANCE_URL = "/api/process-instances";

  @Autowired(required = false)
  protected PermissionsService permissionsService;

  @Autowired private BatchOperationWriter batchOperationWriter;
  @Autowired private ProcessInstanceReader processInstanceReader;
  @Autowired private ListViewReader listViewReader;
  @Autowired private FlowNodeStatisticsReader flowNodeStatisticsReader;
  @Autowired private IncidentReader incidentReader;
  @Autowired private VariableReader variableReader;
  @Autowired private FlowNodeInstanceReader flowNodeInstanceReader;
  @Autowired private SequenceFlowStore sequenceFlowStore;
  @Autowired private OperationReader operationReader;
  @Autowired private ModifyProcessInstanceRequestValidator modifyProcessInstanceRequestValidator;

  @Operation(summary = "Query process instances by different parameters")
  @PostMapping
  @Timed(
      value = Metrics.TIMER_NAME_QUERY,
      extraTags = {Metrics.TAG_KEY_NAME, Metrics.TAG_VALUE_PROCESSINSTANCES},
      description = "How long does it take to retrieve the processinstances by query.")
  public ListViewResponseDto queryProcessInstances(
      @RequestBody ListViewRequestDto processInstanceRequest) {
    if (processInstanceRequest.getQuery() == null) {
      throw new InvalidRequestException("Query must be provided.");
    }
    if (processInstanceRequest.getQuery().getProcessVersion() != null
        && processInstanceRequest.getQuery().getBpmnProcessId() == null) {
      throw new InvalidRequestException(
          "BpmnProcessId must be provided in request, when process version is not null.");
    }
    return listViewReader.queryProcessInstances(processInstanceRequest);
  }

  @Operation(summary = "Perform single operation on an instance (async)")
  @PostMapping("/{id}/operation")
  @PreAuthorize("hasPermission('write')")
  public BatchOperationEntity operation(
      @PathVariable @ValidLongId String id,
      @RequestBody CreateOperationRequestDto operationRequest) {
    validate(operationRequest, id);
    if (operationRequest.getOperationType() == OperationType.DELETE_PROCESS_INSTANCE) {
      checkIdentityPermission(Long.valueOf(id), IdentityPermission.DELETE_PROCESS_INSTANCE);
    } else {
      checkIdentityPermission(Long.valueOf(id), IdentityPermission.UPDATE_PROCESS_INSTANCE);
    }
    return batchOperationWriter.scheduleSingleOperation(Long.parseLong(id), operationRequest);
  }

  @Operation(summary = "Perform modify process instance operation")
  @PostMapping("/{id}/modify")
  @PreAuthorize("hasPermission('write')")
  public BatchOperationEntity modify(
      @PathVariable @ValidLongId String id,
      @RequestBody ModifyProcessInstanceRequestDto modifyRequest) {
    modifyRequest.setProcessInstanceKey(id);
    modifyProcessInstanceRequestValidator.validate(modifyRequest);
    checkIdentityPermission(Long.valueOf(id), IdentityPermission.UPDATE_PROCESS_INSTANCE);
    return batchOperationWriter.scheduleModifyProcessInstance(modifyRequest);
  }

  private void validate(CreateBatchOperationRequestDto batchOperationRequest) {
    if (batchOperationRequest.getQuery() == null) {
      throw new InvalidRequestException("List view query must be defined.");
    }
    if (batchOperationRequest.getOperationType() == null) {
      throw new InvalidRequestException("Operation type must be defined.");
    }
    if (Set.of(UPDATE_VARIABLE, ADD_VARIABLE).contains(batchOperationRequest.getOperationType())) {
      throw new InvalidRequestException(
          "For variable update use \"Create operation for one process instance\" endpoint.");
    }
    if (batchOperationRequest.getOperationType() == MIGRATE_PROCESS_INSTANCE) {
      final MigrationPlanDto migrationPlanDto = batchOperationRequest.getMigrationPlan();
      if (migrationPlanDto == null) {
        throw new InvalidRequestException(
            String.format(
                "Migration plan is mandatory for %s operation", MIGRATE_PROCESS_INSTANCE));
      }
      migrationPlanDto.validate();
    }
  }

  private void validate(
      CreateOperationRequestDto operationRequest, @ValidLongId String processInstanceId) {
    if (operationRequest.getOperationType() == null) {
      throw new InvalidRequestException("Operation type must be defined.");
    }
    if (Set.of(UPDATE_VARIABLE, ADD_VARIABLE).contains(operationRequest.getOperationType())
        && (operationRequest.getVariableScopeId() == null
            || operationRequest.getVariableName() == null
            || operationRequest.getVariableName().isEmpty()
            || operationRequest.getVariableValue() == null)) {
      throw new InvalidRequestException(
          "ScopeId, name and value must be defined for UPDATE_VARIABLE operation.");
    }
    if (operationRequest.getOperationType().equals(ADD_VARIABLE)
        && (variableReader.getVariableByName(
                    processInstanceId,
                    operationRequest.getVariableScopeId(),
                    operationRequest.getVariableName())
                != null
            || !operationReader
                .getOperations(
                    ADD_VARIABLE,
                    processInstanceId,
                    operationRequest.getVariableScopeId(),
                    operationRequest.getVariableName())
                .isEmpty())) {
      throw new InvalidRequestException(
          String.format(
              "Variable with the name \"%s\" already exists.", operationRequest.getVariableName()));
    }
  }

  @Operation(summary = "Create batch operation based on filter")
  @PostMapping("/batch-operation")
  @PreAuthorize("hasPermission('write')")
  public BatchOperationEntity createBatchOperation(
      @RequestBody CreateBatchOperationRequestDto batchOperationRequest) {
    validate(batchOperationRequest);
    return batchOperationWriter.scheduleBatchOperation(batchOperationRequest);
  }

  @Operation(summary = "Get process instance by id")
  @GetMapping("/{id}")
  public ListViewProcessInstanceDto queryProcessInstanceById(@PathVariable @ValidLongId String id) {
    checkIdentityReadPermission(Long.parseLong(id));
    return processInstanceReader.getProcessInstanceWithOperationsByKey(Long.valueOf(id));
  }

  @Operation(summary = "Get incidents by process instance id")
  @GetMapping("/{id}/incidents")
  public IncidentResponseDto queryIncidentsByProcessInstanceId(
      @PathVariable @ValidLongId String id) {
    checkIdentityReadPermission(Long.parseLong(id));
    return incidentReader.getIncidentsByProcessInstanceId(id);
  }

  @Operation(summary = "Get sequence flows by process instance id")
  @GetMapping("/{id}/sequence-flows")
  public List<SequenceFlowDto> querySequenceFlowsByProcessInstanceId(
      @PathVariable @ValidLongId String id) {
    checkIdentityReadPermission(Long.parseLong(id));
    final List<SequenceFlowEntity> sequenceFlows =
        sequenceFlowStore.getSequenceFlowsByProcessInstanceKey(Long.valueOf(id));
    return DtoCreator.create(sequenceFlows, SequenceFlowDto.class);
  }

  @Operation(summary = "Get variables by process instance id and scope id")
  @PostMapping("/{processInstanceId}/variables")
  public List<VariableDto> getVariables(
      @PathVariable @ValidLongId String processInstanceId,
      @RequestBody VariableRequestDto variableRequest) {
    checkIdentityReadPermission(Long.parseLong(processInstanceId));
    validate(variableRequest);
    return variableReader.getVariables(processInstanceId, variableRequest);
  }

  @Operation(summary = "Get full variable by id")
  @GetMapping("/{processInstanceId}/variables/{variableId}")
  public VariableDto getVariable(
      @PathVariable @ValidLongId String processInstanceId, @PathVariable String variableId) {
    checkIdentityReadPermission(Long.parseLong(processInstanceId));
    return variableReader.getVariable(variableId);
  }

  @Operation(summary = "Get flow node states by process instance id")
  @GetMapping("/{processInstanceId}/flow-node-states")
  public Map<String, FlowNodeStateDto> getFlowNodeStates(
      @PathVariable @ValidLongId String processInstanceId) {
    checkIdentityReadPermission(Long.parseLong(processInstanceId));
    return flowNodeInstanceReader.getFlowNodeStates(processInstanceId);
  }

  @Operation(summary = "Get flow node statistic by process instance id")
  @GetMapping("/{processInstanceId}/statistics")
  public Collection<FlowNodeStatisticsDto> getStatistics(
      @PathVariable @ValidLongId String processInstanceId) {
    checkIdentityReadPermission(Long.parseLong(processInstanceId));
    return flowNodeInstanceReader.getFlowNodeStatisticsForProcessInstance(
        Long.parseLong(processInstanceId));
  }

  @Operation(summary = "Get flow node metadata.")
  @PostMapping("/{processInstanceId}/flow-node-metadata")
  public FlowNodeMetadataDto getFlowNodeMetadata(
      @PathVariable @ValidLongId String processInstanceId,
      @RequestBody FlowNodeMetadataRequestDto request) {
    checkIdentityReadPermission(Long.parseLong(processInstanceId));
    validate(request);
    return flowNodeInstanceReader.getFlowNodeMetadata(processInstanceId, request);
  }

  private void validate(final VariableRequestDto request) {
    if (request.getScopeId() == null) {
      throw new InvalidRequestException("ScopeId must be specifies in the request.");
    }
  }

  private void validate(final FlowNodeMetadataRequestDto request) {
    if (request.getFlowNodeId() == null
        && request.getFlowNodeType() == null
        && request.getFlowNodeInstanceId() == null) {
      throw new InvalidRequestException(
          "At least flowNodeId or flowNodeInstanceId must be specifies in the request.");
    }
    if (request.getFlowNodeId() != null && request.getFlowNodeInstanceId() != null) {
      throw new InvalidRequestException(
          "Only one of flowNodeId or flowNodeInstanceId must be specifies in the request.");
    }
  }

  @Operation(summary = "Get activity instance statistics")
  @PostMapping(path = "/statistics")
  public Collection<FlowNodeStatisticsDto> getStatistics(@RequestBody ListViewQueryDto query) {
    final List<Long> processDefinitionKeys =
        CollectionUtil.toSafeListOfLongs(query.getProcessIds());
    final String bpmnProcessId = query.getBpmnProcessId();
    final Integer processVersion = query.getProcessVersion();

    if ((processDefinitionKeys != null && processDefinitionKeys.size() == 1)
        == (bpmnProcessId != null && processVersion != null)) {
      throw new InvalidRequestException(
          "Exactly one process must be specified in the request (via processIds or bpmnProcessId/version).");
    }
    return flowNodeStatisticsReader.getFlowNodeStatistics(query);
  }

  @Operation(summary = "Get process instance core statistics (aggregations)")
  @GetMapping(path = "/core-statistics")
  @Timed(
      value = Metrics.TIMER_NAME_QUERY,
      extraTags = {Metrics.TAG_KEY_NAME, Metrics.TAG_VALUE_CORESTATISTICS},
      description = "How long does it take to retrieve the core statistics.")
  public ProcessInstanceCoreStatisticsDto getCoreStatistics() {
    return processInstanceReader.getCoreStatistics();
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
  }

  private void checkIdentityReadPermission(Long processInstanceKey) {
    checkIdentityPermission(processInstanceKey, IdentityPermission.READ);
  }

  private void checkIdentityPermission(Long processInstanceKey, IdentityPermission permission) {
    if (permissionsService != null
        && !permissionsService.hasPermissionForProcess(
            processInstanceReader.getProcessInstanceByKey(processInstanceKey).getBpmnProcessId(),
            permission)) {
      throw new NotAuthorizedException(
          String.format(
              "No %s permission for process instance %s", permission, processInstanceKey));
    }
  }
}
