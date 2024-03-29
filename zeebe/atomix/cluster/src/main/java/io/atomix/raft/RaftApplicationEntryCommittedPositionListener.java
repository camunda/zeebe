/*
 * Copyright 2016-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
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
package io.atomix.raft;

/**
 * This listener will only be called by the Leader, when it commits an application entry.
 *
 * <p>If RAFT is currently running in a follower role, it will not call this listener.
 */
@FunctionalInterface
public interface RaftApplicationEntryCommittedPositionListener {

  /**
   * @param committedPosition the new committed position which is related to the application entries
   */
  void onCommit(long committedPosition);
}
