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
package io.camunda.zeebe.client.impl.command;

import io.camunda.zeebe.client.ZeebeClientConfiguration;
import io.camunda.zeebe.client.api.JsonMapper;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.StreamJobsCommandStep1;
import io.camunda.zeebe.client.api.command.StreamJobsCommandStep1.StreamJobsCommandStep2;
import io.camunda.zeebe.client.api.command.StreamJobsCommandStep1.StreamJobsCommandStep3;
import io.camunda.zeebe.client.api.response.StreamJobsResponse;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.impl.RetriableStreamingFutureImpl;
import io.camunda.zeebe.client.impl.response.ActivatedJobImpl;
import io.camunda.zeebe.client.impl.response.StreamJobsResponseImpl;
import io.camunda.zeebe.client.impl.worker.JobClientImpl;
import io.camunda.zeebe.gateway.protocol.GatewayGrpc.GatewayStub;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivatedJob;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.StreamJobsRequest;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class StreamJobsCommandImpl
    implements StreamJobsCommandStep1, StreamJobsCommandStep2, StreamJobsCommandStep3 {
  private final GatewayStub asyncStub;
  private final JsonMapper jsonMapper;
  private final Predicate<Throwable> retryPredicate;
  private final StreamJobsRequest.Builder requestBuilder;
  private final JobClient jobClient;
  private final ExecutorService executorService;

  private JobHandler jobHandler;

  public StreamJobsCommandImpl(
      final GatewayStub asyncStub,
      final ZeebeClientConfiguration config,
      final JsonMapper jsonMapper,
      final ExecutorService executorService,
      final Predicate<Throwable> retryPredicate) {
    this.asyncStub = asyncStub;
    this.jsonMapper = jsonMapper;
    this.executorService = executorService;
    this.retryPredicate = retryPredicate;
    requestBuilder = StreamJobsRequest.newBuilder();
    jobClient = new JobClientImpl(asyncStub, config, jsonMapper, retryPredicate);
  }

  @Override
  public StreamJobsCommandStep3 requestTimeout(final Duration requestTimeout) {
    // NOOP
    return this;
  }

  @Override
  public ZeebeFuture<StreamJobsResponse> send() {
    final StreamJobsRequest request = requestBuilder.build();
    final RetriableStreamingFutureImpl<StreamJobsResponse, GatewayOuterClass.ActivatedJob> future =
        new RetriableStreamingFutureImpl<>(
            new StreamJobsResponseImpl(),
            job -> executorService.execute(() -> forwardJob(job)),
            retryPredicate,
            streamObserver -> send(request, streamObserver));

    send(request, future);
    return future;
  }

  @Override
  public StreamJobsCommandStep2 jobType(final String jobType) {
    requestBuilder.setType(jobType);
    return this;
  }

  @Override
  public StreamJobsCommandStep3 timeout(final Duration timeout) {
    requestBuilder.setTimeout(timeout.toMillis());
    return this;
  }

  @Override
  public StreamJobsCommandStep3 workerName(final String workerName) {
    requestBuilder.setWorker(workerName);
    return this;
  }

  @Override
  public StreamJobsCommandStep3 fetchVariables(final List<String> fetchVariables) {
    requestBuilder.addAllFetchVariable(fetchVariables);
    return this;
  }

  private void forwardJob(final ActivatedJob job) {
    try {
      jobHandler.handle(jobClient, new ActivatedJobImpl(jsonMapper, job));
    } catch (final Exception e) {
      jobClient.newFailCommand(job.getKey()).retries(job.getRetries() - 1).send().join();
    }
  }

  @Override
  public StreamJobsCommandStep3 handler(final JobHandler jobHandler) {
    this.jobHandler = jobHandler;
    return this;
  }

  private void send(
      final StreamJobsRequest request,
      final StreamObserver<GatewayOuterClass.ActivatedJob> future) {
    asyncStub.withDeadlineAfter(Long.MAX_VALUE, TimeUnit.MILLISECONDS).streamJobs(request, future);
  }
}
