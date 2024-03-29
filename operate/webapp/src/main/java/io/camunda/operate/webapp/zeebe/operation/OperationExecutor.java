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
package io.camunda.operate.webapp.zeebe.operation;

import static io.camunda.operate.util.ThreadUtil.*;

import io.camunda.operate.entities.OperationEntity;
import io.camunda.operate.entities.OperationType;
import io.camunda.operate.exceptions.PersistenceException;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.webapp.writer.BatchOperationWriter;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class OperationExecutor extends Thread {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperationExecutor.class);

  private boolean shutdown = false;

  @Autowired private List<OperationHandler> handlers;

  @Autowired private BatchOperationWriter batchOperationWriter;

  @Autowired private OperateProperties operateProperties;

  @Autowired
  @Qualifier("operationsThreadPoolExecutor")
  private ThreadPoolTaskExecutor operationsTaskExecutor;

  private List<ExecutionFinishedListener> listeners = new ArrayList<>();

  public void startExecuting() {
    if (operateProperties.getOperationExecutor().isExecutorEnabled()) {
      start();
    }
  }

  @PreDestroy
  public void shutdown() {
    LOGGER.info("Shutdown OperationExecutor");
    shutdown = true;
  }

  @Override
  public void run() {
    while (!shutdown) {
      try {
        final List<Future<?>> operations = executeOneBatch();

        // TODO backoff strategy
        if (operations.size() == 0) {

          notifyExecutionFinishedListeners();
          sleepFor(2000);
        }

      } catch (Exception ex) {
        // retry
        LOGGER.error(
            "Something went wrong, while executing operations batch. Will be retried.", ex);

        sleepFor(2000);
      }
    }
  }

  public List<Future<?>> executeOneBatch() throws PersistenceException {
    final List<Future<?>> futures = new ArrayList<>();

    // lock the operations
    final List<OperationEntity> lockedOperations = batchOperationWriter.lockBatch();

    // execute all locked operations
    for (OperationEntity operation : lockedOperations) {
      final OperationHandler handler = getOperationHandlers().get(operation.getType());
      if (handler == null) {
        LOGGER.info(
            "Operation {} on worflowInstanceId {} won't be processed, as no suitable handler was found.",
            operation.getType(),
            operation.getProcessInstanceKey());
      } else {
        final OperationCommand operationCommand = new OperationCommand(operation, handler);
        futures.add(operationsTaskExecutor.submit(operationCommand));
      }
    }
    return futures;
  }

  @Bean
  public Map<OperationType, OperationHandler> getOperationHandlers() {
    // populate handlers map
    final Map<OperationType, OperationHandler> handlerMap = new HashMap<>();
    for (OperationHandler handler : handlers) {
      handler.getTypes().forEach(t -> handlerMap.put(t, handler));
    }
    return handlerMap;
  }

  public void registerListener(ExecutionFinishedListener listener) {
    this.listeners.add(listener);
  }

  private void notifyExecutionFinishedListeners() {
    for (ExecutionFinishedListener listener : listeners) {
      listener.onExecutionFinished();
    }
  }
}
