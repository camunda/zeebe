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
package io.camunda.client;

import io.camunda.zeebe.client.ZeebeClientCloudBuilderStep1;

public interface CamundaClientCloudBuilderStep1 extends ZeebeClientCloudBuilderStep1 {

  /**
   * Sets the cluster id of the Camunda Cloud cluster. This parameter is mandatory.
   *
   * @param clusterId cluster id of the Camunda Cloud cluster.
   */
  @Override
  CamundaClientCloudBuilderStep2 withClusterId(String clusterId);

  interface CamundaClientCloudBuilderStep2 extends ZeebeClientCloudBuilderStep2 {

    /**
     * Sets the client id that will be used to authenticate against the Camunda Cloud cluster. This
     * parameter is mandatory.
     *
     * @param clientId client id that will be used in the authentication.
     */
    @Override
    CamundaClientCloudBuilderStep3 withClientId(String clientId);

    interface CamundaClientCloudBuilderStep3 extends ZeebeClientCloudBuilderStep3 {

      /**
       * Sets the client secret that will be used to authenticate against the Camunda Cloud cluster.
       * This parameter is mandatory.
       *
       * @param clientSecret client secret that will be used in the authentication.
       */
      @Override
      CamundaClientCloudBuilderStep4 withClientSecret(String clientSecret);

      interface CamundaClientCloudBuilderStep4
          extends CamundaClientBuilder, ZeebeClientCloudBuilderStep4 {

        /**
         * Sets the region of the Camunda Cloud cluster. Default is 'bru-2'.
         *
         * @param region region of the Camunda Cloud cluster
         */
        @Override
        CamundaClientCloudBuilderStep4 withRegion(String region);
      }
    }
  }
}
