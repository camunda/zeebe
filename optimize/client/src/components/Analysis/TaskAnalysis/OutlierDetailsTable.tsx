/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Table, TableBody} from 'components';
import {t} from 'translation';
import {Button} from '@carbon/react';

import {AnalysisProcessDefinitionParameters, getOutlierSummary} from './service';
import InstancesButton from './InstanceButton';

import './OutlierDetailsTable.scss';

type TaskData = {
  totalCount: number;
  higherOutlier?: {count: number; relation: number; boundValue: number};
};

type Variable = {variableName: string; variableTerm: string | number | boolean};

interface OutlierDetailsTableProps {
  loading?: boolean;
  nodeOutliers: Record<string, TaskData | undefined>;
  outlierVariables: Record<string, Variable[]>;
  flowNodeNames: Record<string, string>;
  onDetailsClick: (taskId: string, taskData: TaskData) => string;
  config: AnalysisProcessDefinitionParameters;
}

export default function OutlierDetailsTable({
  loading,
  nodeOutliers,
  outlierVariables,
  flowNodeNames,
  onDetailsClick,
  config,
}: OutlierDetailsTableProps) {
  function getVariablesList(variables?: Variable[]): string | JSX.Element {
    if (!variables?.length) {
      return '-';
    }

    return (
      <ul>
        {variables.map(({variableName, variableTerm}) => (
          <li key={variableName}>{`${variableName}=${variableTerm}`}</li>
        ))}
      </ul>
    );
  }

  function parseTableBody(): TableBody[] {
    if (!nodeOutliers) {
      return [];
    }

    return Object.entries(nodeOutliers).reduce<TableBody[]>(
      (tableRows, [nodeOutlierId, nodeOutlierData]) => {
        if (!nodeOutlierData || !nodeOutlierData.higherOutlier) {
          return tableRows;
        }

        const {
          higherOutlier: {count, relation, boundValue},
          totalCount,
        } = nodeOutlierData;
        const variables = outlierVariables[nodeOutlierId];

        tableRows.push([
          flowNodeNames[nodeOutlierId] || nodeOutlierId,
          totalCount.toString(),
          getOutlierSummary(count, relation),
          getVariablesList(variables),
          <Button
            kind="tertiary"
            size="sm"
            onClick={() => onDetailsClick(nodeOutlierId, nodeOutlierData)}
          >
            {t('common.viewDetails')}
          </Button>,
          <InstancesButton
            id={nodeOutlierId}
            name={flowNodeNames[nodeOutlierId]}
            value={boundValue}
            config={config}
            totalCount={totalCount}
          />,
        ]);

        return tableRows;
      },
      []
    );
  }

  return (
    <Table
      className="OutlierDetailsTable"
      head={[
        t('analysis.task.table.flowNodeName').toString(),
        t('analysis.task.totalInstances').toString(),
        t('analysis.task.table.outliers').toString(),
        t('report.variables.default').toString(),
        t('common.details').toString(),
        t('common.download').toString(),
      ]}
      body={parseTableBody()}
      loading={loading}
      disablePagination
    />
  );
}
