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
package io.camunda.operate.property;

import io.camunda.operate.exceptions.OperateRuntimeException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ConfigurationProperties(OperateProperties.PREFIX + ".migration")
public class MigrationProperties {

  private static final int DEFAULT_REINDEX_BATCH_SIZE = 5_000;
  private static final int DEFAULT_SCRIPT_PARAMS_COUNT = 1_000;
  private static final int DEFAULT_THREADS_COUNT = 5;
  private static final int DEFAULT_SCROLL_KEEP_ALIVE =
      20 * 60 * 1000; // 20 minutes TimeValue.timeValueMinutes(20);

  private boolean migrationEnabled = true;
  private boolean deleteSrcSchema = true;

  @Deprecated // not used
  private String sourceVersion;
  @Deprecated // nor used
  private String destinationVersion;

  private int threadsCount = DEFAULT_THREADS_COUNT;

  // Depends of the size of documents
  //   big documents => batch size lower
  private int reindexBatchSize = DEFAULT_REINDEX_BATCH_SIZE;

  private int scriptParamsCount = DEFAULT_SCRIPT_PARAMS_COUNT;
  // AUTO=0 means 1 slice per shard
  private int slices = 0;

  private int scrollKeepAlive = DEFAULT_SCROLL_KEEP_ALIVE;

  public boolean isMigrationEnabled() {
    return migrationEnabled;
  }

  public MigrationProperties setMigrationEnabled(boolean migrationEnabled) {
    this.migrationEnabled = migrationEnabled;
    return this;
  }

  public String getSourceVersion() {
    return sourceVersion;
  }

  public MigrationProperties setSourceVersion(String sourceVersion) {
    this.sourceVersion = sourceVersion;
    return this;
  }

  public String getDestinationVersion() {
    return destinationVersion;
  }

  public MigrationProperties setDestinationVersion(String destinationVersion) {
    this.destinationVersion = destinationVersion;
    return this;
  }

  public boolean isDeleteSrcSchema() {
    return deleteSrcSchema;
  }

  public MigrationProperties setDeleteSrcSchema(boolean deleteSrcSchema) {
    this.deleteSrcSchema = deleteSrcSchema;
    return this;
  }

  public int getReindexBatchSize() {
    return reindexBatchSize;
  }

  public MigrationProperties setReindexBatchSize(int reindexBatchSize) {
    if (reindexBatchSize < 1 || reindexBatchSize > 10_000) {
      throw new OperateRuntimeException(
          String.format(
              "Reindex batch size must be between 1 and 10000. Given was %d", reindexBatchSize));
    }
    this.reindexBatchSize = reindexBatchSize;
    return this;
  }

  public int getScriptParamsCount() {
    return scriptParamsCount;
  }

  public MigrationProperties setScriptParamsCount(int scriptParamsCount) {
    this.scriptParamsCount = scriptParamsCount;
    return this;
  }

  public int getSlices() {
    return slices;
  }

  public MigrationProperties setSlices(int slices) {
    if (slices < 0) {
      throw new OperateRuntimeException(
          String.format("Slices must be positive. Given was %d", slices));
    }
    this.slices = slices;
    return this;
  }

  public int getThreadsCount() {
    return threadsCount;
  }

  public void setThreadsCount(int threadsCount) {
    this.threadsCount = threadsCount;
  }

  public int getScrollKeepAlive() {
    return scrollKeepAlive;
  }

  public MigrationProperties setScrollKeepAlive(int scrollKeepAlive) {
    this.scrollKeepAlive = scrollKeepAlive;
    return this;
  }
}
