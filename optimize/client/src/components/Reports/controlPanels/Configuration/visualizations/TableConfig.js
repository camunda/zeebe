/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {FormGroup, Stack} from '@carbon/react';

import {isDurationReport} from 'services';

import ColumnSelection from './subComponents/ColumnSelection';
import RelativeAbsoluteSelection from './subComponents/RelativeAbsoluteSelection';
import GradientBarsSwitch from './subComponents/GradientBarsSwitch';

export default function TableConfig({report, onChange, autoPreviewDisabled}) {
  let typeSpecificComponent = null;

  const property = (report.combined ? Object.values(report.result.data)[0] : report).data.view
    .properties[0];

  const groupBy = !report.combined && report.data.groupBy.type;

  switch (property) {
    case 'rawData':
      typeSpecificComponent = (
        <ColumnSelection report={report} onChange={onChange} disabled={autoPreviewDisabled} />
      );
      break;
    case 'frequency':
      typeSpecificComponent = (
        <FormGroup legendText="">
          <Stack gap={4}>
            <RelativeAbsoluteSelection
              reportType={report.reportType}
              absolute={!report.data.configuration.hideAbsoluteValue}
              relative={!report.data.configuration.hideRelativeValue}
              onChange={(type, value) => {
                if (type === 'absolute') {
                  onChange({hideAbsoluteValue: {$set: !value}});
                } else {
                  onChange({hideRelativeValue: {$set: !value}});
                }
              }}
            />
            {groupBy === 'matchedRule' && (
              <GradientBarsSwitch configuration={report.data.configuration} onChange={onChange} />
            )}
          </Stack>
        </FormGroup>
      );
      break;
    default:
      typeSpecificComponent = null;
  }

  return typeSpecificComponent;
}

// disable popover for duration tables since they currently have no configuration
TableConfig.isDisabled = (report) => {
  return (
    report.combined &&
    report.data.reports &&
    report.data.reports.length &&
    isDurationReport(Object.values(report.result.data)[0])
  );
};
