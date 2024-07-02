/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React, {useState, useEffect} from 'react';
import update from 'immutability-helper';

import {loadVariables} from 'services';
import {withErrorHandling} from 'HOC';
import {showError} from 'notifications';

import RawDataTable from './RawDataTable';
import DefaultTable from './DefaultTable';

import './Table.scss';

export function Table(props) {
  const {report, mightFail, loadReport, context} = props;
  const {
    reportType,
    data: {view, groupBy, definitions, distributedBy, filter},
    result,
  } = report;

  const isRawDataReport = view?.properties?.[0] === 'rawData';
  const processVariableReport =
    reportType === 'process' && (isRawDataReport || groupBy?.type === 'variable');

  const [isSorting, setIsSorting] = useState(false);
  const [processVariables, setProcessVariables] = useState();

  useEffect(() => {
    if (processVariableReport) {
      const payload = {
        processesToQuery: definitions.map(({key, versions, tenantIds}) => ({
          processDefinitionKey: key,
          processDefinitionVersions: versions,
          tenantIds: tenantIds,
        })),
        filter,
      };
      mightFail(loadVariables(payload), setProcessVariables, showError);
    }
  }, [definitions, processVariableReport, mightFail, reportType, filter]);

  const isDisrtibutedByProcess = distributedBy?.type === 'process';
  const updateSorting = async (by, order) => {
    setIsSorting(true);
    await loadReport(result.pagination, {
      ...report,
      data: update(report.data, {configuration: {sorting: {$set: {by, order}}}}),
    });
    setIsSorting(false);
  };

  const isLoadingVariables = processVariableReport && !processVariables;
  const Component = isRawDataReport ? RawDataTable : DefaultTable;

  return (
    <Component
      {...props}
      updateSorting={
        !isDisrtibutedByProcess && context !== 'shared' && context !== 'dashboard' && updateSorting
      }
      processVariables={processVariables}
      loading={props.loading || isLoadingVariables || isSorting}
    />
  );
}

export default withErrorHandling(Table);
