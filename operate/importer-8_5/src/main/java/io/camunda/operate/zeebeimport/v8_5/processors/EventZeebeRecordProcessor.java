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
package io.camunda.operate.zeebeimport.v8_5.processors;

import static io.camunda.operate.entities.EventType.ELEMENT_ACTIVATING;
import static io.camunda.operate.entities.EventType.ELEMENT_COMPLETING;
import static io.camunda.operate.schema.templates.EventTemplate.METADATA;
import static io.camunda.operate.util.LambdaExceptionUtil.rethrowConsumer;
import static io.camunda.operate.zeebeimport.util.ImportUtil.tenantOrDefault;
import static io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent.*;

import io.camunda.operate.entities.*;
import io.camunda.operate.entities.ErrorType;
import io.camunda.operate.exceptions.PersistenceException;
import io.camunda.operate.schema.templates.EventTemplate;
import io.camunda.operate.store.BatchRequest;
import io.camunda.operate.util.DateUtil;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordValue;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessMessageSubscriptionIntent;
import io.camunda.zeebe.protocol.record.value.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EventZeebeRecordProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventZeebeRecordProcessor.class);

  private static final String ID_PATTERN = "%s_%s";

  private static final Set<String> INCIDENT_EVENTS = new HashSet<>();
  private static final Set<String> JOB_EVENTS = new HashSet<>();
  private static final Set<String> PROCESS_INSTANCE_STATES = new HashSet<>();
  private static final Set<String> PROCESS_MESSAGE_SUBSCRIPTION_STATES = new HashSet<>();

  static {
    INCIDENT_EVENTS.add(IncidentIntent.CREATED.name());
    INCIDENT_EVENTS.add(IncidentIntent.RESOLVED.name());

    JOB_EVENTS.add(JobIntent.CREATED.name());
    JOB_EVENTS.add(JobIntent.COMPLETED.name());
    JOB_EVENTS.add(JobIntent.TIMED_OUT.name());
    JOB_EVENTS.add(JobIntent.FAILED.name());
    JOB_EVENTS.add(JobIntent.RETRIES_UPDATED.name());
    JOB_EVENTS.add(JobIntent.CANCELED.name());
    JOB_EVENTS.add(JobIntent.MIGRATED.name());

    PROCESS_INSTANCE_STATES.add(ELEMENT_ACTIVATING.name());
    PROCESS_INSTANCE_STATES.add(ELEMENT_ACTIVATED.name());
    PROCESS_INSTANCE_STATES.add(ELEMENT_COMPLETING.name());
    PROCESS_INSTANCE_STATES.add(ELEMENT_COMPLETED.name());
    PROCESS_INSTANCE_STATES.add(ELEMENT_TERMINATED.name());

    PROCESS_MESSAGE_SUBSCRIPTION_STATES.add(ProcessMessageSubscriptionIntent.CREATED.name());
  }

  @Autowired private EventTemplate eventTemplate;

  public void processIncidentRecords(
      final Map<Long, List<Record<IncidentRecordValue>>> records, final BatchRequest batchRequest)
      throws PersistenceException {
    for (final List<Record<IncidentRecordValue>> incidentRecords : records.values()) {
      processLastRecord(
          incidentRecords,
          INCIDENT_EVENTS,
          rethrowConsumer(
              record -> {
                final IncidentRecordValue recordValue = (IncidentRecordValue) record.getValue();
                processIncident(record, recordValue, batchRequest);
              }));
    }
  }

  public void processJobRecords(
      final Map<Long, List<Record<JobRecordValue>>> records, final BatchRequest batchRequest)
      throws PersistenceException {
    for (final List<Record<JobRecordValue>> jobRecords : records.values()) {
      processLastRecord(
          jobRecords,
          JOB_EVENTS,
          rethrowConsumer(
              record -> {
                final JobRecordValue recordValue = (JobRecordValue) record.getValue();
                processJob(record, recordValue, batchRequest);
              }));
    }
  }

  public void processProcessMessageSubscription(
      final Map<Long, List<Record<ProcessMessageSubscriptionRecordValue>>> records,
      final BatchRequest batchRequest)
      throws PersistenceException {
    for (final List<Record<ProcessMessageSubscriptionRecordValue>> pmsRecords : records.values()) {
      processLastRecord(
          pmsRecords,
          PROCESS_MESSAGE_SUBSCRIPTION_STATES,
          rethrowConsumer(
              record -> {
                final ProcessMessageSubscriptionRecordValue recordValue =
                    (ProcessMessageSubscriptionRecordValue) record.getValue();
                processMessage(record, recordValue, batchRequest);
              }));
    }
  }

  public void processProcessInstanceRecords(
      final Map<Long, List<Record<ProcessInstanceRecordValue>>> records,
      final BatchRequest batchRequest)
      throws PersistenceException {
    for (final List<Record<ProcessInstanceRecordValue>> piRecords : records.values()) {
      processLastRecord(
          piRecords,
          PROCESS_INSTANCE_STATES,
          rethrowConsumer(
              record -> {
                final ProcessInstanceRecordValue recordValue =
                    (ProcessInstanceRecordValue) record.getValue();
                processProcessInstance(record, recordValue, batchRequest);
              }));
    }
  }

  private <T extends RecordValue> void processLastRecord(
      final List<Record<T>> incidentRecords,
      final Set<String> events,
      final Consumer<Record<? extends RecordValue>> recordProcessor) {
    if (incidentRecords.size() >= 1) {
      for (int i = incidentRecords.size() - 1; i >= 0; i--) {
        final String intentStr = incidentRecords.get(i).getIntent().name();
        if (events.contains(intentStr)) {
          recordProcessor.accept(incidentRecords.get(i));
          break;
        }
      }
    }
  }

  private void processProcessInstance(
      final Record record,
      final ProcessInstanceRecordValue recordValue,
      final BatchRequest batchRequest)
      throws PersistenceException {
    if (!isProcessEvent(recordValue)) { // we do not need to store process level events
      final EventEntity eventEntity =
          new EventEntity()
              .setId(
                  String.format(ID_PATTERN, recordValue.getProcessInstanceKey(), record.getKey()));

      loadEventGeneralData(record, eventEntity);

      eventEntity
          .setProcessDefinitionKey(recordValue.getProcessDefinitionKey())
          .setProcessInstanceKey(recordValue.getProcessInstanceKey())
          .setBpmnProcessId(recordValue.getBpmnProcessId())
          .setTenantId(tenantOrDefault(recordValue.getTenantId()));

      if (recordValue.getElementId() != null) {
        eventEntity.setFlowNodeId(recordValue.getElementId());
      }

      if (record.getKey() != recordValue.getProcessInstanceKey()) {
        eventEntity.setFlowNodeInstanceKey(record.getKey());
      }

      persistEvent(eventEntity, record.getPosition(), batchRequest);
    }
  }

  private void processMessage(
      final Record record,
      final ProcessMessageSubscriptionRecordValue recordValue,
      final BatchRequest batchRequest)
      throws PersistenceException {
    final EventEntity eventEntity =
        new EventEntity()
            .setId(
                String.format(
                    ID_PATTERN,
                    recordValue.getProcessInstanceKey(),
                    recordValue.getElementInstanceKey()));

    loadEventGeneralData(record, eventEntity);

    final long processInstanceKey = recordValue.getProcessInstanceKey();
    if (processInstanceKey > 0) {
      eventEntity.setProcessInstanceKey(processInstanceKey);
    }

    eventEntity
        .setBpmnProcessId(recordValue.getBpmnProcessId())
        .setFlowNodeId(recordValue.getElementId())
        .setTenantId(tenantOrDefault(recordValue.getTenantId()));

    final long activityInstanceKey = recordValue.getElementInstanceKey();
    if (activityInstanceKey > 0) {
      eventEntity.setFlowNodeInstanceKey(activityInstanceKey);
    }

    final EventMetadataEntity eventMetadata = new EventMetadataEntity();
    eventMetadata.setMessageName(recordValue.getMessageName());
    eventMetadata.setCorrelationKey(recordValue.getCorrelationKey());

    eventEntity.setMetadata(eventMetadata);

    persistEvent(eventEntity, record.getPosition(), batchRequest);
  }

  private void processJob(
      final Record record, final JobRecordValue recordValue, final BatchRequest batchRequest)
      throws PersistenceException {
    final EventEntity eventEntity =
        new EventEntity()
            .setId(
                String.format(
                    ID_PATTERN,
                    recordValue.getProcessInstanceKey(),
                    recordValue.getElementInstanceKey()));

    loadEventGeneralData(record, eventEntity);

    final long processDefinitionKey = recordValue.getProcessDefinitionKey();
    if (processDefinitionKey > 0) {
      eventEntity.setProcessDefinitionKey(processDefinitionKey);
    }

    final long processInstanceKey = recordValue.getProcessInstanceKey();
    if (processInstanceKey > 0) {
      eventEntity.setProcessInstanceKey(processInstanceKey);
    }

    eventEntity
        .setBpmnProcessId(recordValue.getBpmnProcessId())
        .setFlowNodeId(recordValue.getElementId())
        .setTenantId(tenantOrDefault(recordValue.getTenantId()));

    final long activityInstanceKey = recordValue.getElementInstanceKey();
    if (activityInstanceKey > 0) {
      eventEntity.setFlowNodeInstanceKey(activityInstanceKey);
    }

    final EventMetadataEntity eventMetadata = new EventMetadataEntity();
    eventMetadata.setJobType(recordValue.getType());
    eventMetadata.setJobRetries(recordValue.getRetries());
    eventMetadata.setJobWorker(recordValue.getWorker());
    eventMetadata.setJobCustomHeaders(recordValue.getCustomHeaders());

    if (record.getKey() > 0) {
      eventMetadata.setJobKey(record.getKey());
    }

    final long jobDeadline = recordValue.getDeadline();
    if (jobDeadline >= 0) {
      eventMetadata.setJobDeadline(DateUtil.toOffsetDateTime(Instant.ofEpochMilli(jobDeadline)));
    }

    eventEntity.setMetadata(eventMetadata);

    persistEvent(eventEntity, record.getPosition(), batchRequest);
  }

  private void processIncident(
      final Record record, final IncidentRecordValue recordValue, final BatchRequest batchRequest)
      throws PersistenceException {
    final EventEntity eventEntity =
        new EventEntity()
            .setId(
                String.format(
                    ID_PATTERN,
                    recordValue.getProcessInstanceKey(),
                    recordValue.getElementInstanceKey()));
    loadEventGeneralData(record, eventEntity);

    if (recordValue.getProcessInstanceKey() > 0) {
      eventEntity.setProcessInstanceKey(recordValue.getProcessInstanceKey());
    }
    eventEntity
        .setBpmnProcessId(recordValue.getBpmnProcessId())
        .setFlowNodeId(recordValue.getElementId())
        .setTenantId(tenantOrDefault(recordValue.getTenantId()));
    if (recordValue.getElementInstanceKey() > 0) {
      eventEntity.setFlowNodeInstanceKey(recordValue.getElementInstanceKey());
    }

    final EventMetadataEntity eventMetadata = new EventMetadataEntity();
    eventMetadata.setIncidentErrorMessage(
        StringUtils.trimWhitespace(recordValue.getErrorMessage()));
    eventMetadata.setIncidentErrorType(
        ErrorType.fromZeebeErrorType(
            recordValue.getErrorType() == null ? null : recordValue.getErrorType().name()));
    eventEntity.setMetadata(eventMetadata);

    persistEvent(eventEntity, record.getPosition(), batchRequest);
  }

  private boolean isProcessEvent(final ProcessInstanceRecordValue recordValue) {
    return isOfType(recordValue, BpmnElementType.PROCESS);
  }

  private boolean isOfType(
      final ProcessInstanceRecordValue recordValue, final BpmnElementType type) {
    final BpmnElementType bpmnElementType = recordValue.getBpmnElementType();
    if (bpmnElementType == null) {
      return false;
    }
    return bpmnElementType.equals(type);
  }

  private void loadEventGeneralData(final Record record, final EventEntity eventEntity) {
    eventEntity.setKey(record.getKey());
    eventEntity.setPartitionId(record.getPartitionId());
    eventEntity.setEventSourceType(
        EventSourceType.fromZeebeValueType(
            record.getValueType() == null ? null : record.getValueType().name()));
    eventEntity.setDateTime(DateUtil.toOffsetDateTime(Instant.ofEpochMilli(record.getTimestamp())));
    eventEntity.setEventType(EventType.fromZeebeIntent(record.getIntent().name()));
  }

  private void persistEvent(
      final EventEntity entity, final long position, final BatchRequest batchRequest)
      throws PersistenceException {
    LOGGER.debug(
        "Event: id {}, eventSourceType {}, eventType {}, processInstanceKey {}",
        entity.getId(),
        entity.getEventSourceType(),
        entity.getEventType(),
        entity.getProcessInstanceKey());
    final Map<String, Object> jsonMap = new HashMap<>();
    jsonMap.put(EventTemplate.KEY, entity.getKey());
    jsonMap.put(EventTemplate.EVENT_SOURCE_TYPE, entity.getEventSourceType());
    jsonMap.put(EventTemplate.EVENT_TYPE, entity.getEventType());
    jsonMap.put(EventTemplate.DATE_TIME, entity.getDateTime());
    jsonMap.put(EventTemplate.PROCESS_KEY, entity.getProcessDefinitionKey());
    jsonMap.put(EventTemplate.BPMN_PROCESS_ID, entity.getBpmnProcessId());
    jsonMap.put(EventTemplate.FLOW_NODE_ID, entity.getFlowNodeId());
    if (entity.getMetadata() != null) {
      final Map<String, Object> metadataMap = new HashMap<>();
      if (entity.getMetadata().getIncidentErrorMessage() != null) {
        metadataMap.put(
            EventTemplate.INCIDENT_ERROR_MSG, entity.getMetadata().getIncidentErrorMessage());
        metadataMap.put(
            EventTemplate.INCIDENT_ERROR_TYPE, entity.getMetadata().getIncidentErrorType());
      }
      if (entity.getMetadata().getJobKey() != null) {
        metadataMap.put(EventTemplate.JOB_KEY, entity.getMetadata().getJobKey());
      }
      if (entity.getMetadata().getJobType() != null) {
        metadataMap.put(EventTemplate.JOB_TYPE, entity.getMetadata().getJobType());
        metadataMap.put(EventTemplate.JOB_RETRIES, entity.getMetadata().getJobRetries());
        metadataMap.put(EventTemplate.JOB_WORKER, entity.getMetadata().getJobWorker());
        metadataMap.put(EventTemplate.JOB_KEY, entity.getMetadata().getJobKey());
        metadataMap.put(
            EventTemplate.JOB_CUSTOM_HEADERS, entity.getMetadata().getJobCustomHeaders());
      }
      if (entity.getMetadata().getMessageName() != null) {
        metadataMap.put(EventTemplate.MESSAGE_NAME, entity.getMetadata().getMessageName());
        metadataMap.put(EventTemplate.CORRELATION_KEY, entity.getMetadata().getCorrelationKey());
      }
      if (metadataMap.size() > 0) {
        jsonMap.put(METADATA, metadataMap);
      }
    }
    // write event
    batchRequest.upsert(eventTemplate.getFullQualifiedName(), entity.getId(), entity, jsonMap);
  }
}
