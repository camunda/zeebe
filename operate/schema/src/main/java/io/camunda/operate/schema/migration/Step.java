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
package io.camunda.operate.schema.migration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.OffsetDateTime;
import java.util.Comparator;

/**
 * A step describes a change in one index in a specific version and in which order inside the
 * version.<br>
 * A step stores when it was created and applied.<br>
 * The change is described in content of step.<br>
 * It also provides comparators for SemanticVersion and order comparing.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ProcessorStep.class),
  @JsonSubTypes.Type(value = SetBpmnProcessIdStep.class),
  @JsonSubTypes.Type(value = FillPostImporterQueueStep.class)
})
public interface Step {

  public static final String INDEX_NAME = "indexName",
      CREATED_DATE = "createdDate",
      APPLIED = "applied",
      APPLIED_DATE = "appliedDate",
      VERSION = "version",
      ORDER = "order",
      CONTENT = "content";
  public static final Comparator<Step> SEMANTICVERSION_COMPARATOR =
      new Comparator<Step>() {
        @Override
        public int compare(Step s1, Step s2) {
          return SemanticVersion.fromVersion(s1.getVersion())
              .compareTo(SemanticVersion.fromVersion(s2.getVersion()));
        }
      };
  public static final Comparator<Step> ORDER_COMPARATOR =
      new Comparator<Step>() {
        @Override
        public int compare(Step s1, Step s2) {
          return s1.getOrder().compareTo(s2.getOrder());
        }
      };
  public static final Comparator<Step> SEMANTICVERSION_ORDER_COMPARATOR =
      new Comparator<Step>() {
        @Override
        public int compare(Step s1, Step s2) {
          int result = SEMANTICVERSION_COMPARATOR.compare(s1, s2);
          if (result == 0) {
            result = ORDER_COMPARATOR.compare(s1, s2);
          }
          return result;
        }
      };

  public OffsetDateTime getCreatedDate();

  public Step setCreatedDate(final OffsetDateTime date);

  public OffsetDateTime getAppliedDate();

  public Step setAppliedDate(final OffsetDateTime date);

  public String getVersion();

  public Integer getOrder();

  public boolean isApplied();

  public Step setApplied(final boolean isApplied);

  public String getIndexName();

  public String getContent();

  public String getDescription();
}
