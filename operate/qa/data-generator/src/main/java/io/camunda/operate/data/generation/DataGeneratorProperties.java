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
package io.camunda.operate.data.generation;

import static io.camunda.operate.data.generation.DataGeneratorProperties.PROPERTIES_PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class DataGeneratorProperties {

  public static final String PROPERTIES_PREFIX = "camunda.operate.qa.data";

  private int processCount = 100;

  private int processInstanceCount = 10000;

  private int callActivityProcessInstanceCount = 100;

  private int incidentCount = 100;

  private int resolvedIncidentCount = 100;

  private String zeebeGatewayAddress = "localhost:26500";

  private String elasticsearchHost = "localhost";

  private int elasticsearchPort = 9200;

  private String zeebeElasticsearchPrefix = "zeebe-record";

  private int queueSize = 200;

  private int threadCount = 2;

  public int getProcessCount() {
    return processCount;
  }

  public void setProcessCount(int processCount) {
    this.processCount = processCount;
  }

  public int getProcessInstanceCount() {
    return processInstanceCount;
  }

  public void setProcessInstanceCount(int processInstanceCount) {
    this.processInstanceCount = processInstanceCount;
  }

  public int getCallActivityProcessInstanceCount() {
    return callActivityProcessInstanceCount;
  }

  public DataGeneratorProperties setCallActivityProcessInstanceCount(
      final int callActivityProcessInstanceCount) {
    this.callActivityProcessInstanceCount = callActivityProcessInstanceCount;
    return this;
  }

  public int getIncidentCount() {
    return incidentCount;
  }

  public void setIncidentCount(int incidentCount) {
    this.incidentCount = incidentCount;
  }

  public int getResolvedIncidentCount() {
    return resolvedIncidentCount;
  }

  public DataGeneratorProperties setResolvedIncidentCount(final int resolvedIncidentCount) {
    this.resolvedIncidentCount = resolvedIncidentCount;
    return this;
  }

  public String getZeebeGatewayAddress() {
    return zeebeGatewayAddress;
  }

  public void setZeebeGatewayAddress(String gatewayAddress) {
    this.zeebeGatewayAddress = gatewayAddress;
  }

  public String getElasticsearchHost() {
    return elasticsearchHost;
  }

  public void setElasticsearchHost(String elasticsearchHost) {
    this.elasticsearchHost = elasticsearchHost;
  }

  public int getElasticsearchPort() {
    return elasticsearchPort;
  }

  public void setElasticsearchPort(int elasticsearchPort) {
    this.elasticsearchPort = elasticsearchPort;
  }

  public String getZeebeElasticsearchPrefix() {
    return zeebeElasticsearchPrefix;
  }

  public void setZeebeElasticsearchPrefix(String zeebeElasticsearchPrefix) {
    this.zeebeElasticsearchPrefix = zeebeElasticsearchPrefix;
  }

  public int getQueueSize() {
    return queueSize;
  }

  public void setQueueSize(int queueSize) {
    this.queueSize = queueSize;
  }

  public int getThreadCount() {
    return threadCount;
  }

  public DataGeneratorProperties setThreadCount(final int threadCount) {
    this.threadCount = threadCount;
    return this;
  }
}
