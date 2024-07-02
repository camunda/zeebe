/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {FormGroup} from '@carbon/react';

import {t} from 'translation';

import RelativeAbsoluteSelection from './subComponents/RelativeAbsoluteSelection';

export default function HeatmapConfig(props) {
  const {
    report: {reportType, data},
    onChange,
  } = props;
  return (
    <FormGroup legendText={t('report.config.tooltips.legend')}>
      <RelativeAbsoluteSelection
        reportType={reportType}
        hideRelative={data.view.properties[0] !== 'frequency'}
        absolute={data.configuration.alwaysShowAbsolute}
        relative={data.configuration.alwaysShowRelative}
        onChange={(type, value) => {
          if (type === 'absolute') {
            onChange({alwaysShowAbsolute: {$set: value}});
          } else {
            onChange({alwaysShowRelative: {$set: value}});
          }
        }}
      />
    </FormGroup>
  );
}
