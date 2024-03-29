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
package io.camunda.operate.schema.util.opensearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.conditions.OpensearchCondition;
import io.camunda.operate.exceptions.OperateRuntimeException;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.schema.IndexMapping;
import io.camunda.operate.schema.IndexMapping.IndexMappingProperty;
import io.camunda.operate.schema.SchemaManager;
import io.camunda.operate.schema.indices.AbstractIndexDescriptor;
import io.camunda.operate.schema.indices.IndexDescriptor;
import io.camunda.operate.schema.templates.TemplateDescriptor;
import io.camunda.operate.schema.util.SchemaTestHelper;
import io.camunda.operate.store.opensearch.client.sync.RichOpenSearchClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.GetIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.GetIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.get_index_template.IndexTemplate;
import org.opensearch.client.opensearch.indices.get_index_template.IndexTemplateItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;

@Conditional(OpensearchCondition.class)
public class OpenSearchSchemaTestHelper implements SchemaTestHelper {

  @Autowired private SchemaManager schemaManager;

  @Autowired private RichOpenSearchClient openSearchClient;

  @Autowired private OpenSearchClient lowLevelOpenSearchClient;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private OperateProperties properties;

  @Override
  public void dropSchema() {
    final String openSearchObjectPrefix = properties.getOpensearch().getIndexPrefix() + "-*";
    final Set<String> indexesToDelete = schemaManager.getIndexNames(openSearchObjectPrefix);
    if (!indexesToDelete.isEmpty()) {
      // fails if there are no matching indexes
      setReadOnly(openSearchObjectPrefix, false);
    }

    schemaManager.deleteIndicesFor(openSearchObjectPrefix);
    schemaManager.deleteTemplatesFor(openSearchObjectPrefix);
  }

  @Override
  public IndexMapping getTemplateMappings(final TemplateDescriptor template) {
    try {
      final String templateName = template.getTemplateName();

      final GetIndexTemplateRequest request =
          new GetIndexTemplateRequest.Builder().name(templateName).build();

      final GetIndexTemplateResponse indexTemplateResponse =
          lowLevelOpenSearchClient.indices().getIndexTemplate(request);
      final List<IndexTemplateItem> indexTemplates = indexTemplateResponse.indexTemplates();

      if (indexTemplates.isEmpty()) {
        return null;
      } else if (indexTemplates.size() > 1) {
        throw new OperateRuntimeException(
            String.format(
                "Found more than one template matching name %s. Expected one.", templateName));
      }

      final IndexTemplate indexTemplate = indexTemplates.get(0).indexTemplate();
      final Map<String, Property> properties = indexTemplate.template().mappings().properties();

      return new IndexMapping()
          .setIndexName(templateName)
          .setProperties(
              properties.entrySet().stream().map(this::mapProperty).collect(Collectors.toSet()));
    } catch (final IOException e) {
      throw new OperateRuntimeException(e);
    }
  }

  @Override
  public void createIndex(
      final IndexDescriptor indexDescriptor,
      final String indexName,
      final String indexSchemaFilename) {
    schemaManager.createIndex(
        new AbstractIndexDescriptor() {
          @Override
          public String getIndexName() {
            return indexDescriptor.getIndexName();
          }

          @Override
          public String getFullQualifiedName() {
            return indexName;
          }
        },
        indexSchemaFilename);
  }

  @Override
  public void setReadOnly(final String indexName, final boolean readOnly) {
    final PutIndicesSettingsRequest updateSettingsRequest =
        new PutIndicesSettingsRequest.Builder()
            .index(indexName)
            .settings(b -> b.blocksReadOnly(readOnly))
            .build();

    try {
      openSearchClient.index().putSettings(updateSettingsRequest);
    } catch (final IOException e) {
      throw new OperateRuntimeException(e);
    }
  }

  protected IndexMappingProperty mapProperty(final Entry<String, Property> property)
      throws OperateRuntimeException {

    final String propertyAsJson = openSearchClient.index().toJsonString(property.getValue());

    final Map<String, Object> propertyAsMap;
    try {
      propertyAsMap =
          objectMapper.readValue(propertyAsJson, new TypeReference<HashMap<String, Object>>() {});
    } catch (final JsonProcessingException e) {
      throw new OperateRuntimeException(e);
    }

    return new IndexMappingProperty().setName(property.getKey()).setTypeDefinition(propertyAsMap);
  }
}
