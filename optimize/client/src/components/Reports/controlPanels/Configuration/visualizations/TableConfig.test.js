/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {shallow} from 'enzyme';

import TableConfig from './TableConfig';

it('should render ColumnSelection for raw data views', () => {
  const node = shallow(
    <TableConfig
      report={{
        combined: false,
        data: {view: {properties: ['rawData']}, groupBy: {type: 'none'}, configuration: {}},
      }}
    />
  );

  expect(node.find('ColumnSelection')).toExist();
});

it('should render relative abolute selection for count views', () => {
  const node = shallow(
    <TableConfig
      report={{
        combined: false,
        data: {view: {properties: ['frequency']}, groupBy: {type: 'startDate'}, configuration: {}},
      }}
    />
  );

  expect(node.find('RelativeAbsoluteSelection')).toExist();
});

it('should render GradientBarsSwitch for group by rules', () => {
  const node = shallow(
    <TableConfig
      report={{
        combined: false,
        data: {
          view: {properties: ['frequency']},
          groupBy: {type: 'matchedRule'},
          configuration: {},
        },
      }}
    />
  );

  expect(node.find('GradientBarsSwitch')).toExist();
});

it('should disable the column selection component when automatic preview is off', () => {
  const node = shallow(
    <TableConfig
      report={{
        data: {
          configuration: {tableColumns: {columnOrder: ['test']}},
          groupBy: {type: 'matchedRule'},
          view: {properties: ['rawData']},
        },
      }}
      autoPreviewDisabled
    />
  );

  expect(node.find('ColumnSelection').prop('disabled')).toBe(true);
});
