/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {shallow} from 'enzyme';

import Analysis from './Analysis';

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useLocation: jest.fn().mockReturnValue({pathname: '/analysis/branchAnalysis'}),
}));

it('should select the correct table', () => {
  const node = shallow(<Analysis />);

  expect(node.find('Tabs').prop('value')).toBe(1);
});
