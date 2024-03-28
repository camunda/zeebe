/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.instance;

import com.google.common.collect.Sets;
import io.camunda.zeebe.db.ColumnFamily;
import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.impl.DbCompositeKey;
import io.camunda.zeebe.db.impl.DbLong;
import io.camunda.zeebe.db.impl.DbNil;
import io.camunda.zeebe.db.impl.DbString;
import io.camunda.zeebe.db.impl.DbTenantAwareKey;
import io.camunda.zeebe.db.impl.DbTenantAwareKey.PlacementType;
import io.camunda.zeebe.engine.state.mutable.MutablePendingSequenceFlowState;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.util.buffer.BufferUtil;
import java.util.Set;
import org.agrona.DirectBuffer;

public class DbPendingSequenceFlowState implements MutablePendingSequenceFlowState {

  // [[tenant_id, flowScopeKey], sequenceId] => [Nil]
  private final DbString tenantIdKey;
  private final DbLong flowScopeKey;
  private final DbString sequenceFlowId;

  private final DbTenantAwareKey<DbLong> tenantAwareFlowScopeKey;
  private final DbCompositeKey<DbTenantAwareKey<DbLong>, DbString>
      tenantAwareFlowScopeKeyAndSequenceFlowId;

  private final ColumnFamily<DbCompositeKey<DbTenantAwareKey<DbLong>, DbString>, DbNil>
      columnFamily;

  public DbPendingSequenceFlowState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final TransactionContext transactionContext) {
    tenantIdKey = new DbString();
    sequenceFlowId = new DbString();
    flowScopeKey = new DbLong();

    tenantAwareFlowScopeKey =
        new DbTenantAwareKey<>(tenantIdKey, flowScopeKey, PlacementType.PREFIX);

    tenantAwareFlowScopeKeyAndSequenceFlowId =
        new DbCompositeKey<>(tenantAwareFlowScopeKey, sequenceFlowId);

    columnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.PENDING_SEQUENCE_FLOWS,
            transactionContext,
            tenantAwareFlowScopeKeyAndSequenceFlowId,
            DbNil.INSTANCE);
  }

  @Override
  public Set<DirectBuffer> getSequenceFlowIds(final String tenantId, final long flowScopeKey) {
    this.tenantIdKey.wrapString(tenantId);
    this.flowScopeKey.wrapLong(flowScopeKey);

    final Set<DirectBuffer> sequenceFlowIds = Sets.newHashSet();
    columnFamily.whileEqualPrefix(
        tenantAwareFlowScopeKey,
        (key, value) -> {
          final String sequenceFlowId = key.second().toString();
          sequenceFlowIds.add(BufferUtil.wrapString(sequenceFlowId));
        });
    return sequenceFlowIds;
  }

  @Override
  public void create(final String tenantId, final long flowScopeKey, final String sequenceFlowId) {
    this.tenantIdKey.wrapString(tenantId);
    this.flowScopeKey.wrapLong(flowScopeKey);
    this.sequenceFlowId.wrapString(sequenceFlowId);
    columnFamily.upsert(tenantAwareFlowScopeKeyAndSequenceFlowId, DbNil.INSTANCE);
  }

  @Override
  public void delete(final String tenantId, final long flowScopeKey, final String sequenceFlowId) {
    this.tenantIdKey.wrapString(tenantId);
    this.flowScopeKey.wrapLong(flowScopeKey);
    this.sequenceFlowId.wrapString(sequenceFlowId);
    columnFamily.deleteIfExists(tenantAwareFlowScopeKeyAndSequenceFlowId);
  }
}
