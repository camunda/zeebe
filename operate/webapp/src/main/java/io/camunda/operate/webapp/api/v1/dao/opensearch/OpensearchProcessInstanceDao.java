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
package io.camunda.operate.webapp.api.v1.dao.opensearch;

import io.camunda.operate.conditions.OpensearchCondition;
import io.camunda.operate.connect.OperateDateTimeFormatter;
import io.camunda.operate.schema.templates.ListViewTemplate;
import io.camunda.operate.store.opensearch.client.sync.RichOpenSearchClient;
import io.camunda.operate.webapp.api.v1.dao.ProcessInstanceDao;
import io.camunda.operate.webapp.api.v1.entities.ChangeStatus;
import io.camunda.operate.webapp.api.v1.entities.ProcessInstance;
import io.camunda.operate.webapp.api.v1.entities.Query;
import io.camunda.operate.webapp.api.v1.exceptions.APIException;
import io.camunda.operate.webapp.api.v1.exceptions.ClientException;
import io.camunda.operate.webapp.api.v1.exceptions.ServerException;
import io.camunda.operate.webapp.opensearch.OpensearchQueryDSLWrapper;
import io.camunda.operate.webapp.opensearch.OpensearchRequestDSLWrapper;
import io.camunda.operate.webapp.writer.ProcessInstanceWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Conditional(OpensearchCondition.class)
@Component
public class OpensearchProcessInstanceDao
    extends OpensearchKeyFilteringDao<ProcessInstance, ProcessInstance>
    implements ProcessInstanceDao {

  private final ListViewTemplate processInstanceIndex;

  private final ProcessInstanceWriter processInstanceWriter;

  private final OperateDateTimeFormatter dateTimeFormatter;

  public OpensearchProcessInstanceDao(
      final OpensearchQueryDSLWrapper queryDSLWrapper,
      final OpensearchRequestDSLWrapper requestDSLWrapper,
      final RichOpenSearchClient richOpenSearchClient,
      final ListViewTemplate processInstanceIndex,
      final ProcessInstanceWriter processInstanceWriter,
      final OperateDateTimeFormatter dateTimeFormatter) {
    super(queryDSLWrapper, requestDSLWrapper, richOpenSearchClient);
    this.processInstanceIndex = processInstanceIndex;
    this.processInstanceWriter = processInstanceWriter;
    this.dateTimeFormatter = dateTimeFormatter;
  }

  @Override
  protected String getUniqueSortKey() {
    // This probably should be ProcessInstance.KEY to be consistent with how we always pull sort
    // keys
    // from the constants in the index. Since the ElasticsearchDao uses this field too, leaving it
    // as-is.

    // While the `processInstanceKey` field is not in the ProcessInstance model object, it can still
    // be
    // used as a sort key (and has the same value as `key` which is what is referred to here with
    // ListViewTemplate.KEY.
    return ListViewTemplate.KEY;
  }

  @Override
  protected Class<ProcessInstance> getInternalDocumentModelClass() {
    return ProcessInstance.class;
  }

  @Override
  protected String getIndexName() {
    return processInstanceIndex.getAlias();
  }

  @Override
  protected void buildFiltering(
      final Query<ProcessInstance> query, final SearchRequest.Builder request) {
    final List<org.opensearch.client.opensearch._types.query_dsl.Query> queryTerms =
        new LinkedList<>();
    queryTerms.add(
        queryDSLWrapper.term(
            ListViewTemplate.JOIN_RELATION, ListViewTemplate.PROCESS_INSTANCE_JOIN_RELATION));

    final ProcessInstance filter = query.getFilter();

    if (filter != null) {
      queryTerms.add(queryDSLWrapper.term(ProcessInstance.KEY, filter.getKey()));
      queryTerms.add(
          queryDSLWrapper.term(
              ProcessInstance.PROCESS_DEFINITION_KEY, filter.getProcessDefinitionKey()));
      queryTerms.add(queryDSLWrapper.term(ProcessInstance.PARENT_KEY, filter.getParentKey()));
      queryTerms.add(
          queryDSLWrapper.term(
              ProcessInstance.PARENT_FLOW_NODE_INSTANCE_KEY,
              filter.getParentFlowNodeInstanceKey()));
      queryTerms.add(queryDSLWrapper.term(ProcessInstance.VERSION, filter.getProcessVersion()));
      queryTerms.add(
          queryDSLWrapper.term(ProcessInstance.BPMN_PROCESS_ID, filter.getBpmnProcessId()));
      queryTerms.add(queryDSLWrapper.term(ProcessInstance.STATE, filter.getState()));
      queryTerms.add(queryDSLWrapper.term(ProcessInstance.TENANT_ID, filter.getTenantId()));
      queryTerms.add(
          queryDSLWrapper.matchDateQuery(
              ProcessInstance.START_DATE,
              filter.getStartDate(),
              dateTimeFormatter.getApiDateTimeFormatString()));
      queryTerms.add(
          queryDSLWrapper.matchDateQuery(
              ProcessInstance.END_DATE,
              filter.getEndDate(),
              dateTimeFormatter.getApiDateTimeFormatString()));
    }

    final var nonNullQueryTerms = queryTerms.stream().filter(Objects::nonNull).toList();

    request.query(queryDSLWrapper.and(nonNullQueryTerms));
  }

  @Override
  protected ProcessInstance convertInternalToApiResult(final ProcessInstance internalResult) {
    if (internalResult != null) {
      if (StringUtils.isNotEmpty(internalResult.getEndDate())) {
        internalResult.setEndDate(
            dateTimeFormatter.convertGeneralToApiDateTime(internalResult.getEndDate()));
      }

      if (StringUtils.isNotEmpty(internalResult.getStartDate())) {
        internalResult.setStartDate(
            dateTimeFormatter.convertGeneralToApiDateTime(internalResult.getStartDate()));
      }
    }
    return internalResult;
  }

  @Override
  @PreAuthorize("hasPermission('write')")
  public ChangeStatus delete(final Long key) throws APIException {
    // Check for not exists
    byKey(key);
    try {
      processInstanceWriter.deleteInstanceById(key);
      return new ChangeStatus()
          .setDeleted(1)
          .setMessage(
              String.format("Process instance and dependant data deleted for key '%s'", key));
    } catch (final IllegalArgumentException iae) {
      throw new ClientException(iae.getMessage(), iae);
    } catch (final Exception e) {
      throw new ServerException(
          String.format("Error in deleting process instance and dependant data for key '%s'", key),
          e);
    }
  }

  @Override
  protected List<ProcessInstance> searchByKey(final Long key) {
    final List<org.opensearch.client.opensearch._types.query_dsl.Query> queryTerms =
        new LinkedList<>();
    queryTerms.add(
        queryDSLWrapper.term(
            ListViewTemplate.JOIN_RELATION, ListViewTemplate.PROCESS_INSTANCE_JOIN_RELATION));
    queryTerms.add(queryDSLWrapper.term(getKeyFieldName(), key));

    final SearchRequest.Builder request =
        requestDSLWrapper
            .searchRequestBuilder(getIndexName())
            .query(queryDSLWrapper.withTenantCheck(queryDSLWrapper.and(queryTerms)));

    return richOpenSearchClient.doc().searchValues(request, getInternalDocumentModelClass());
  }

  @Override
  protected String getKeyFieldName() {
    return ProcessInstance.KEY;
  }

  @Override
  protected String getByKeyServerReadErrorMessage(final Long key) {
    return String.format("Error in reading process instance for key %s", key);
  }

  @Override
  protected String getByKeyNoResultsErrorMessage(final Long key) {
    return String.format("No process instances found for key %s", key);
  }

  @Override
  protected String getByKeyTooManyResultsErrorMessage(final Long key) {
    return String.format("Found more than one process instances for key %s", key);
  }
}
