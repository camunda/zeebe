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
package io.camunda.operate.util.j5templates;

import static io.camunda.operate.util.OperateAbstractIT.DEFAULT_USER;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.cache.ProcessCache;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.util.TestApplication;
import io.camunda.operate.util.searchrepository.TestSearchRepository;
import io.camunda.operate.webapp.rest.dto.UserDto;
import io.camunda.operate.webapp.security.Permission;
import io.camunda.operate.webapp.security.UserService;
import io.camunda.operate.webapp.security.tenant.TenantService;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Base definition for a test that requires opensearch/elasticsearch but not zeebe. The test suite
 * automatically starts search before all the tests run, and then tears it down once all the tests
 * have finished.
 */
@SpringBootTest(
    classes = {TestApplication.class},
    properties = {
      OperateProperties.PREFIX + ".importer.startLoadingDataOnStartup = false",
      OperateProperties.PREFIX + ".archiver.rolloverEnabled = false",
      "spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER",
      OperateProperties.PREFIX + ".multiTenancy.enabled = false"
    })
@WebAppConfiguration
@WithMockUser(DEFAULT_USER)
@TestInstance(
    TestInstance.Lifecycle
        .PER_CLASS) // Lifecycle required to use BeforeAll and AfterAll in non-static fashion
public class OperateSearchAbstractIT {
  // These are mocked so we can bypass authentication issues when connecting to search
  @MockBean protected UserService userService;
  @MockBean protected TenantService tenantService;

  @Autowired protected ProcessCache processCache;

  @Autowired protected TestSearchRepository testSearchRepository;

  @Autowired protected SearchContainerManager searchContainerManager;

  @Autowired protected TestResourceManager testResourceManager;

  @Autowired protected ObjectMapper objectMapper;

  @BeforeAll
  public void beforeAllSetup() throws Exception {
    // Mocks the authentication for search
    when(userService.getCurrentUser())
        .thenReturn(
            new UserDto().setUserId(DEFAULT_USER).setPermissions(List.of(Permission.WRITE)));
    doReturn(TenantService.AuthenticatedTenants.allTenants())
        .when(tenantService)
        .getAuthenticatedTenants();

    // Start elasticsearch/opensearch
    searchContainerManager.startContainer();

    // operateTester = beanFactory.getBean(OperateJ5Tester.class, zeebeClient);

    // Required to keep search from hanging between test suites
    processCache.clearCache();

    // Implementing tests can add any additional setup needed to run once before all the tests run
    runAdditionalBeforeAllSetup();
  }

  @BeforeEach
  public void beforeEach() throws Exception {
    // Mocks are cleared between each test, reset the authentication mocks so interactions with
    // search don't fail
    when(userService.getCurrentUser())
        .thenReturn(
            new UserDto().setUserId(DEFAULT_USER).setPermissions(List.of(Permission.WRITE)));
    doReturn(TenantService.AuthenticatedTenants.allTenants())
        .when(tenantService)
        .getAuthenticatedTenants();

    // Implementing tests can add any additional setup needed to run before each test
    runAdditionalBeforeEachSetup();
  }

  protected void runAdditionalBeforeAllSetup() throws Exception {}

  protected void runAdditionalBeforeEachSetup() throws Exception {}

  @AfterAll
  public void afterAllTeardown() {
    // Stop search once all the test are finished
    searchContainerManager.stopContainer();

    // Required to keep search from hanging between test suites
    processCache.clearCache();

    // Implementing tests can add any additional teardown needed to run at the completion of the
    // test suite
    runAdditionalAfterAllTeardown();
  }

  public void runAdditionalAfterAllTeardown() {}
}
