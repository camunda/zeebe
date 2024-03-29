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

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import java.util.concurrent.ThreadFactory;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class DataGeneratorConfig {

  private static final int JOB_WORKER_MAX_JOBS_ACTIVE = 5;

  @Autowired private DataGeneratorProperties dataGeneratorProperties;

  public ZeebeClient createZeebeClient() {
    final String gatewayAddress = dataGeneratorProperties.getZeebeGatewayAddress();
    final ZeebeClientBuilder builder =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(gatewayAddress)
            .defaultJobWorkerMaxJobsActive(JOB_WORKER_MAX_JOBS_ACTIVE)
            .usePlaintext();
    return builder.build();
  }

  @Bean
  public ZeebeClient getZeebeClient() {
    return createZeebeClient();
  }

  @Bean
  public RestHighLevelClient createRestHighLevelClient() {
    return new RestHighLevelClient(
        RestClient.builder(
            new HttpHost(
                dataGeneratorProperties.getElasticsearchHost(),
                dataGeneratorProperties.getElasticsearchPort(),
                "http")));
  }

  @Bean("dataGeneratorThreadPoolExecutor")
  public ThreadPoolTaskExecutor getDataGeneratorTaskExecutor(
      DataGeneratorProperties dataGeneratorProperties) {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadFactory(getThreadFactory());
    executor.setCorePoolSize(dataGeneratorProperties.getThreadCount());
    executor.setMaxPoolSize(dataGeneratorProperties.getThreadCount());
    executor.setQueueCapacity(1);
    executor.initialize();
    return executor;
  }

  @Bean
  public ThreadFactory getThreadFactory() {
    return new CustomizableThreadFactory("data_generator_") {
      @Override
      public Thread newThread(final Runnable runnable) {
        final Thread thread =
            new DataGeneratorThread(
                this.getThreadGroup(), runnable, this.nextThreadName(), createZeebeClient());
        thread.setPriority(this.getThreadPriority());
        thread.setDaemon(this.isDaemon());
        return thread;
      }
    };
  }

  public class DataGeneratorThread extends Thread {

    private ZeebeClient zeebeClient;

    public DataGeneratorThread(final ZeebeClient zeebeClient) {
      this.zeebeClient = zeebeClient;
    }

    public DataGeneratorThread(final Runnable target, final ZeebeClient zeebeClient) {
      super(target);
      this.zeebeClient = zeebeClient;
    }

    public DataGeneratorThread(
        final ThreadGroup group, final Runnable target, final ZeebeClient zeebeClient) {
      super(group, target);
      this.zeebeClient = zeebeClient;
    }

    public DataGeneratorThread(final String name, final ZeebeClient zeebeClient) {
      super(name);
      this.zeebeClient = zeebeClient;
    }

    public DataGeneratorThread(
        final ThreadGroup group, final String name, final ZeebeClient zeebeClient) {
      super(group, name);
      this.zeebeClient = zeebeClient;
    }

    public DataGeneratorThread(
        final Runnable target, final String name, final ZeebeClient zeebeClient) {
      super(target, name);
      this.zeebeClient = zeebeClient;
    }

    public DataGeneratorThread(
        final ThreadGroup group,
        final Runnable target,
        final String name,
        final ZeebeClient zeebeClient) {
      super(group, target, name);
      this.zeebeClient = zeebeClient;
    }

    public DataGeneratorThread(
        final ThreadGroup group,
        final Runnable target,
        final String name,
        final long stackSize,
        final ZeebeClient zeebeClient) {
      super(group, target, name, stackSize);
      this.zeebeClient = zeebeClient;
    }

    public DataGeneratorThread(
        final ThreadGroup group,
        final Runnable target,
        final String name,
        final long stackSize,
        final boolean inheritThreadLocals,
        final ZeebeClient zeebeClient) {
      super(group, target, name, stackSize, inheritThreadLocals);
      this.zeebeClient = zeebeClient;
    }

    public ZeebeClient getZeebeClient() {
      return zeebeClient;
    }
  }
}
