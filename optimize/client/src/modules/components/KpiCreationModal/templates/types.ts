/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {FilterType, ProcessFilter, SingleProcessReportData} from 'types';

export interface DefaultProcessFilter<T extends FilterType = FilterType>
  extends Omit<ProcessFilter<T>, 'appliedTo'> {
  label: string;
  description: string;
}

export interface KpiTemplate {
  name: string;
  description: string;
  img: string;
  config: Omit<Partial<SingleProcessReportData>, 'configuration'> & {
    configuration?: Partial<SingleProcessReportData['configuration']>;
  };
  uiConfig: {
    filters: DefaultProcessFilter[];
  };
}
