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
package io.camunda.operate.it;

import static io.camunda.operate.schema.indices.IndexDescriptor.DEFAULT_TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.camunda.operate.entities.OperationEntity;
import io.camunda.operate.entities.OperationState;
import io.camunda.operate.entities.listview.ListViewJoinRelation;
import io.camunda.operate.entities.listview.ProcessInstanceForListViewEntity;
import io.camunda.operate.entities.listview.ProcessInstanceState;
import io.camunda.operate.schema.templates.ListViewTemplate;
import io.camunda.operate.schema.templates.OperationTemplate;
import io.camunda.operate.store.NotFoundException;
import io.camunda.operate.util.j5templates.OperateSearchAbstractIT;
import io.camunda.operate.webapp.elasticsearch.reader.ProcessInstanceReader;
import io.camunda.operate.webapp.rest.dto.listview.ListViewProcessInstanceDto;
import io.camunda.operate.webapp.security.UserService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProcessInstanceReaderIT extends OperateSearchAbstractIT {

  @Autowired private ListViewTemplate listViewTemplate;

  @Autowired private OperationTemplate operationTemplate;

  @Autowired private ProcessInstanceReader processInstanceReader;

  @Autowired private UserService userService;

  private ProcessInstanceForListViewEntity processInstanceData;
  private OperationEntity operationData;

  @Override
  protected void runAdditionalBeforeAllSetup() throws Exception {
    final Long processInstanceKey = 2251799813685251L;
    final String indexName = listViewTemplate.getFullQualifiedName();

    processInstanceData =
        new ProcessInstanceForListViewEntity()
            .setId("2251799813685251")
            .setKey(processInstanceKey)
            .setPartitionId(1)
            .setProcessDefinitionKey(2251799813685249L)
            .setProcessName("Demo process")
            .setProcessVersion(1)
            .setBpmnProcessId("demoProcess")
            .setStartDate(OffsetDateTime.now())
            .setState(ProcessInstanceState.ACTIVE)
            .setTreePath("PI_2251799813685251")
            .setIncident(true)
            .setTenantId(DEFAULT_TENANT_ID)
            .setProcessInstanceKey(processInstanceKey)
            .setJoinRelation(new ListViewJoinRelation("processInstance"));

    testSearchRepository.createOrUpdateDocumentFromObject(
        indexName, String.valueOf(processInstanceKey), processInstanceData);

    operationData = new OperationEntity();
    operationData.setId("operation-1");
    operationData.setProcessInstanceKey(processInstanceData.getProcessInstanceKey());
    operationData.setUsername(userService.getCurrentUser().getUsername());
    operationData.setState(OperationState.SCHEDULED);

    testSearchRepository.createOrUpdateDocumentFromObject(
        operationTemplate.getFullQualifiedName(), operationData);

    searchContainerManager.refreshIndices("*");
  }

  @Test
  public void testGetProcessInstanceWithOperationsByKeyWithCorrectKey() {
    // When
    final ListViewProcessInstanceDto processInstance =
        processInstanceReader.getProcessInstanceWithOperationsByKey(
            processInstanceData.getProcessInstanceKey());
    assertThat(processInstance.getId())
        .isEqualTo(String.valueOf(processInstanceData.getProcessInstanceKey()));
    assertThat(processInstance.getOperations().size()).isEqualTo(1);
    assertThat(processInstance.getOperations().get(0).getId()).isEqualTo(operationData.getId());
  }

  @Test
  public void testGetProcessInstanceWithCorrectKey() {
    // When
    final ProcessInstanceForListViewEntity processInstance =
        processInstanceReader.getProcessInstanceByKey(processInstanceData.getProcessInstanceKey());
    assertThat(processInstance.getId())
        .isEqualTo(String.valueOf(processInstanceData.getProcessInstanceKey()));
  }

  @Test
  public void testGetProcessInstanceWithInvalidKey() {
    assertThrows(NotFoundException.class, () -> processInstanceReader.getProcessInstanceByKey(1L));
  }

  @Test
  public void testGetProcessInstanceWithOperationsWithInvalidKey() {
    assertThrows(
        NotFoundException.class,
        () -> processInstanceReader.getProcessInstanceWithOperationsByKey(1L));
  }
}
