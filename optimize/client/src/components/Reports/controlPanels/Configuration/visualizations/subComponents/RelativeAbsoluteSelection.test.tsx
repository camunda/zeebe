/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {shallow} from 'enzyme';

import RelativeAbsoluteSelection from './RelativeAbsoluteSelection';

const props = {
  absolute: true,
  relative: true,
  reportType: 'process',
  onChange: jest.fn(),
};

it('should render toggles', () => {
  const node = shallow(<RelativeAbsoluteSelection {...props} />);

  expect(node.find('Toggle').length).toBe(2);
  expect(node.find('Toggle').at(0).props()).toEqual({
    id: 'showAbsoluteValueToggle',
    labelText: 'Show absolute value',
    hideLabel: true,
    onToggle: expect.any(Function),
    size: 'sm',
    toggled: true,
  });
  expect(node.find('Toggle').at(1).props()).toEqual({
    id: 'showRelativeValueToggle',
    labelText: 'Show relative value based on process instance count',
    hideLabel: true,
    onToggle: expect.any(Function),
    size: 'sm',
    toggled: true,
  });
});

it('should call the onChange method with the correct prop and value', () => {
  const spy = jest.fn();
  const node = shallow(<RelativeAbsoluteSelection {...props} onChange={spy} />);

  node.find('Toggle').at(0).simulate('toggle', false);

  expect(spy).toHaveBeenCalledWith('absolute', false);
});

it('hide the relative selection when hideRelative is true', () => {
  const node = shallow(<RelativeAbsoluteSelection {...props} hideRelative />);

  expect(node.find('Toggle').length).toBe(1);
  expect(node.find('Toggle').prop('id')).toBe('showAbsoluteValueToggle');
});
