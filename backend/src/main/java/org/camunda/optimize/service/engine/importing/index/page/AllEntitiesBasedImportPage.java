/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.engine.importing.index.page;

public class AllEntitiesBasedImportPage implements ImportPage {

  protected long indexOfFirstResult;
  protected long pageSize;

  public long getIndexOfFirstResult() {
    return indexOfFirstResult;
  }

  public void setIndexOfFirstResult(long indexOfFirstResult) {
    this.indexOfFirstResult = indexOfFirstResult;
  }

  public long getPageSize() {
    return pageSize;
  }

  public void setPageSize(long pageSize) {
    this.pageSize = pageSize;
  }
}
