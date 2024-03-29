/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.spring.client.configuration;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientConfiguration;
import io.camunda.zeebe.client.impl.ZeebeClientImpl;
import io.camunda.zeebe.client.impl.util.ExecutorResource;
import io.camunda.zeebe.gateway.protocol.GatewayGrpc;
import io.camunda.zeebe.spring.client.testsupport.SpringZeebeTestContext;
import io.grpc.ManagedChannel;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/*
 * All configurations that will only be used in production code - meaning NO TEST cases
 */
@ConditionalOnProperty(
    prefix = "zeebe.client",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ConditionalOnMissingBean(SpringZeebeTestContext.class)
@ImportAutoConfiguration({ExecutorServiceConfiguration.class, ZeebeActuatorConfiguration.class})
@AutoConfigureBefore(ZeebeClientAllAutoConfiguration.class)
public class ZeebeClientProdAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(ZeebeClientProdAutoConfiguration.class);

  @Bean
  public ZeebeClientConfiguration zeebeClientConfiguration() {
    return new ZeebeClientConfigurationSpringImpl();
  }

  @Bean(destroyMethod = "close")
  public ZeebeClient zeebeClient(final ZeebeClientConfiguration configuration) {
    LOG.info("Creating ZeebeClient using ZeebeClientConfiguration [" + configuration + "]");
    final ScheduledExecutorService jobWorkerExecutor = configuration.jobWorkerExecutor();
    if (jobWorkerExecutor != null) {
      final ManagedChannel managedChannel = ZeebeClientImpl.buildChannel(configuration);
      final GatewayGrpc.GatewayStub gatewayStub =
          ZeebeClientImpl.buildGatewayStub(managedChannel, configuration);
      final ExecutorResource executorResource =
          new ExecutorResource(jobWorkerExecutor, configuration.ownsJobWorkerExecutor());
      return new ZeebeClientImpl(configuration, managedChannel, gatewayStub, executorResource);
    } else {
      return new ZeebeClientImpl(configuration);
    }
  }
}
