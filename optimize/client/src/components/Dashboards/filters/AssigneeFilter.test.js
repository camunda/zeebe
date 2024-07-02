/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React, {runLastEffect} from 'react';
import {shallow} from 'enzyme';
import {Filter} from '@carbon/icons-react';

import {Popover, UserTypeahead} from 'components';

import AssigneeFilter from './AssigneeFilter';

import {getAssigneeNames} from './service';

jest.mock('hooks', () => ({
  useErrorHandling: jest.fn().mockImplementation(() => ({
    mightFail: jest.fn().mockImplementation((data, cb) => cb(data)),
  })),
}));

const props = {
  filter: null,
  type: 'assignee',
  config: {
    operator: 'not in',
    values: ['user1', 'user2', null],
    allowCustomValues: false,
  },
  setFilter: jest.fn(),
  reports: [{id: 'reportA'}],
};

jest.mock('./service', () => ({
  getAssigneeNames: jest.fn().mockReturnValue([
    {id: 'user1', name: 'User 1'},
    {id: 'user2', name: 'User 2'},
  ]),
}));

beforeEach(() => {
  props.setFilter.mockClear();
  getAssigneeNames.mockClear();
});

it('should show the operator when no value is selected', () => {
  const node = shallow(<AssigneeFilter {...props} />);

  const popoverButtonLabel = shallow(node.find(Popover).prop('trigger')).find(
    '.cds--list-box__label'
  );

  expect(popoverButtonLabel.text()).toContain('is not ...');
  expect(popoverButtonLabel.find(Filter)).toExist();
});

it('should allow selecting values', () => {
  const node = shallow(<AssigneeFilter {...props} />);

  runLastEffect();

  const valueSwitch = node.find('Toggle').first();

  expect(valueSwitch).toExist();
  expect(valueSwitch.prop('labelText')).toBe('User 1');

  valueSwitch.simulate('toggle', true);

  expect(props.setFilter).toHaveBeenCalledWith({operator: 'not in', values: ['user1']});
});

it('should abbreviate multiple string selections', () => {
  const node = shallow(
    <AssigneeFilter {...props} filter={{operator: 'not in', values: ['user1', null]}} />
  );

  const popoverButtonLabel = shallow(node.find(Popover).prop('trigger')).find(
    '.cds--list-box__label'
  );

  expect(popoverButtonLabel.find('AssigneeFilterPreview')).toExist();
  expect(popoverButtonLabel.find(Filter)).toExist();
});

it('should show an input field for custom values', () => {
  const node = shallow(
    <AssigneeFilter
      {...props}
      filter={{operator: 'not in', values: ['user1']}}
      config={{
        operator: 'not in',
        values: ['user1', 'user2', null],
        allowCustomValues: true,
      }}
    />
  );

  runLastEffect();

  expect(node.find(UserTypeahead)).toExist();

  node
    .find(UserTypeahead)
    .simulate('change', [
      {identity: {id: 'john', name: 'jonny'}},
      {identity: {id: 'userX', name: 'X Man'}},
    ]);

  expect(props.setFilter).toHaveBeenCalledWith({
    operator: 'not in',
    values: ['user1', 'john', 'userX'],
  });

  node.find('Toggle').last().simulate('toggle', false);

  expect(props.setFilter).toHaveBeenCalledWith({
    operator: 'not in',
    values: ['user1'],
  });
});

it('should load all names, both explicitely added ones as well as default ones', () => {
  shallow(
    <AssigneeFilter
      {...props}
      config={{
        operator: 'not in',
        values: ['user1', 'user2', null],
        defaultValues: ['user1', 'additionalUser'],
        allowCustomValues: true,
      }}
    />
  );
  getAssigneeNames.mockReturnValueOnce([
    {id: 'user1', name: 'User 1', type: 'user'},
    {id: 'user2', name: 'User 2', type: 'user'},
    {id: null, name: null, type: 'user'},
    {id: 'additionalUser', name: 'Additional User', type: 'user'},
  ]);

  runLastEffect();

  expect(getAssigneeNames).toHaveBeenCalledWith('assignee', [
    'user1',
    'user2',
    null,
    'additionalUser',
  ]);
});
