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
package io.camunda.operate.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.operate.connect.ElasticsearchConnector;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.util.apps.nobeans.TestApplicationWithNoBeans;
import java.io.File;
import java.util.Map;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.MountableFile;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      TestApplicationWithNoBeans.class,
      OperateProperties.class,
      ElasticsearchConnector.class
    })
@ContextConfiguration(initializers = {ElasticsearchConnectorSSLAuthIT.ElasticsearchStarter.class})
public class ElasticsearchConnectorSSLAuthIT {

  static String certDir = new File("src/test/resources/certs").getAbsolutePath();

  static ElasticsearchContainer elasticsearch =
      new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.16.2")
          .withCopyFileToContainer(
              MountableFile.forHostPath("src/test/resources/certs/elastic-stack-ca.p12"),
              "/usr/share/elasticsearch/config/certs/elastic-stack-ca.p12")
          // .withCopyFileToContainer(MountableFile.forClasspathResource("/certs/elastic-stack-ca.p12"),"/usr/share/elasticsearch/config/certs/elastic-stack-ca.p12")
          .withPassword("elastic")
          .withEnv(
              Map.of(
                  "xpack.security.enabled", "true",
                  "xpack.security.http.ssl.enabled", "true",
                  "xpack.security.http.ssl.keystore.path",
                      "/usr/share/elasticsearch/config/certs/elastic-stack-ca.p12"))
          .withExposedPorts(9200)
          .waitingFor(Wait.forHttps("/").withBasicCredentials("elastic", "elastic"));

  @Autowired RestHighLevelClient esClient;

  @Autowired RestHighLevelClient zeebeEsClient;

  @Ignore("Can be tested manually")
  @Test
  public void canConnect() {
    assertThat(esClient).isNotNull();
    assertThat(zeebeEsClient).isNotNull();
  }

  static class ElasticsearchStarter
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      elasticsearch.start();

      final String elsUrl =
          String.format(
              "https://%s:%d/", elasticsearch.getHost(), elasticsearch.getFirstMappedPort());
      TestPropertyValues.of(
              "camunda.operate.elasticsearch.url=" + elsUrl,
              "camunda.operate.elasticsearch.username=elastic",
              "camunda.operate.elasticsearch.password=elastic",
              "camunda.operate.elasticsearch.clusterName=docker-cluster",
              // "camunda.operate.elasticsearch.ssl.certificatePath="+certDir+"/elastic-stack-ca.p12",
              // "camunda.operate.elasticsearch.ssl.selfSigned=true",
              // "camunda.operate.elasticsearch.ssl.verifyHostname=true",
              "camunda.operate.zeebeElasticsearch.url=" + elsUrl,
              "camunda.operate.zeebeElasticsearch.username=elastic",
              "camunda.operate.zeebeElasticsearch.password=elastic",
              // "camunda.operate.zeebeElasticsearch.ssl.certificatePath="+certDir+"/elastic-stack-ca.p12",
              // "camunda.operate.zeebeElasticsearch.ssl.selfSigned=true",
              // "camunda.operate.zeebeElasticsearch.ssl.verifyHostname=true",
              "camunda.operate.zeebeElasticsearch.clusterName=docker-cluster",
              "camunda.operate.zeebeElasticsearch.prefix=zeebe-record")
          .applyTo(applicationContext.getEnvironment());
    }
  }
}
