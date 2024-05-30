/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.query.impl;

import io.camunda.zeebe.broker.client.api.dto.BrokerRequest;
import io.camunda.zeebe.broker.client.api.dto.BrokerResponse;
import io.camunda.zeebe.protocol.impl.encoding.ExecuteGetRequest;
import io.camunda.zeebe.protocol.impl.encoding.ExecuteGetResponse;
import io.camunda.zeebe.protocol.record.ExecuteQueryResponseDecoder;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.transport.RequestType;
import io.camunda.zeebe.util.buffer.BufferWriter;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public abstract class BrokerExecuteGetRequest<T> extends BrokerRequest<T> {
  private final ExecuteGetRequest request = new ExecuteGetRequest();
  private final ExecuteGetResponse response = new ExecuteGetResponse();

  public BrokerExecuteGetRequest() {
    super(ExecuteQueryResponseDecoder.SCHEMA_ID, ExecuteQueryResponseDecoder.TEMPLATE_ID);
  }

  public void setKey(final long key) {
    request.setKey(key);
  }

  public void setValueType(final ValueType valueType) {
    request.setValueType(valueType);
  }

  @Override
  public int getPartitionId() {
    return request.getPartitionId();
  }

  @Override
  public void setPartitionId(final int partitionId) {
    request.setPartitionId(partitionId);
  }

  @Override
  public boolean addressesSpecificPartition() {
    return true;
  }

  @Override
  public boolean requiresPartitionId() {
    return true;
  }

  /**
   * @return null to avoid writing any serialized value
   */
  @Override
  public BufferWriter getRequestWriter() {
    return null;
  }

  @Override
  protected void setSerializedValue(final DirectBuffer buffer) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void wrapResponse(final DirectBuffer buffer) {
    response.wrap(buffer, 0, buffer.capacity());
  }

  @Override
  protected BrokerResponse<T> readResponse() {
    final T responseDto = toResponseDto(response.getValue());
    return new BrokerResponse<>(responseDto, request.getPartitionId(), request.getKey());
  }

  @Override
  public String getType() {
    return "Query#" + request.getValueType();
  }

  @Override
  public RequestType getRequestType() {
    return RequestType.QUERY;
  }

  @Override
  public int getLength() {
    return request.getLength();
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    request.write(buffer, offset);
  }
}
