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
package io.camunda.zeebe.client.impl.search;

import io.camunda.zeebe.client.api.search.response.SearchQueryResponse;
import io.camunda.zeebe.client.api.search.response.SearchResponsePage;
import io.camunda.zeebe.client.impl.search.response.SearchQueryResponseImpl;
import io.camunda.zeebe.client.impl.search.response.SearchResponsePageImpl;
import io.camunda.zeebe.client.protocol.rest.ProcessInstance;
import io.camunda.zeebe.client.protocol.rest.ProcessInstanceSearchQueryResponse;
import io.camunda.zeebe.client.protocol.rest.UserTask;
import io.camunda.zeebe.client.protocol.rest.UserTaskSearchQueryResponse;

public final class SearchResponseMapper {

  private SearchResponseMapper() {}

  public static SearchQueryResponse<ProcessInstance> toProcessInstanceSearchResponse(
      final ProcessInstanceSearchQueryResponse response) {
    final SearchResponsePage page =
        new SearchResponsePageImpl(response.getTotal(), null, response.getSortValues());
    return new SearchQueryResponseImpl<>(response.getItems(), page);
  }

  //toUserTaskSearchResponse
  public static SearchQueryResponse<UserTask> toUserTaskSearchResponse(
      final UserTaskSearchQueryResponse response) {
    final SearchResponsePage page =
        new SearchResponsePageImpl(response.getTotal(), null, response.getSortValues());
    return new SearchQueryResponseImpl<>(response.getItems(), page);
  }
}
