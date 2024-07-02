/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React from 'react';
import {shallow} from 'enzyme';

import {BranchAnalysis} from './BranchAnalysis';
import BranchControlPanel from './BranchControlPanel';
import {loadFrequencyData} from './service';

import {incompatibleFilters, loadProcessDefinitionXml} from 'services';
import {track} from 'tracking';

jest.mock('./service', () => {
  return {
    loadFrequencyData: jest.fn(),
  };
});

jest.mock('services', () => {
  return {
    ...jest.requireActual('services'),
    loadProcessDefinitionXml: jest.fn(),
    incompatibleFilters: jest.fn(),
  };
});

jest.mock('tracking', () => ({track: jest.fn()}));

const props = {
  mightFail: jest.fn().mockImplementation((data, cb) => cb(data)),
};

it('should contain a control panel', () => {
  const node = shallow(<BranchAnalysis {...props} />);

  expect(node.find(BranchControlPanel)).toExist();
});

it('should load the process definition xml when the process definition id is updated', () => {
  const node = shallow(<BranchAnalysis {...props} />);

  loadProcessDefinitionXml.mockClear();
  node.instance().updateConfig({
    processDefinitionKey: 'someKey',
    processDefinitionVersions: ['someVersion'],
    tenantIds: ['a', 'b'],
  });

  expect(loadProcessDefinitionXml).toHaveBeenCalledWith('someKey', 'someVersion', 'a');
  expect(track).toHaveBeenCalledWith('startBranchAnalysis', {processDefinitionKey: 'someKey'});
});

it('should load frequency data when the process definition key changes', async () => {
  const node = shallow(<BranchAnalysis {...props} />);

  node
    .instance()
    .updateConfig({processDefinitionKey: 'someKey', processDefinitionVersions: ['someVersion']});
  loadFrequencyData.mockClear();
  await node.instance().updateConfig({processDefinitionKey: 'anotherKey'});

  expect(loadFrequencyData.mock.calls[0][0]).toBe('anotherKey');
});

it('should load frequency data when the process definition version changes', async () => {
  const node = shallow(<BranchAnalysis {...props} />);

  await node
    .instance()
    .updateConfig({processDefinitionKey: 'someKey', processDefinitionVersions: ['someVersion']});
  loadFrequencyData.mockClear();
  await node.instance().updateConfig({processDefinitionVersions: ['anotherVersion']});

  expect(loadFrequencyData.mock.calls[0][1]).toEqual(['anotherVersion']);
});

it('should load updated frequency data when the filter changed', async () => {
  const node = shallow(<BranchAnalysis {...props} />);

  await node.instance().updateConfig({
    processDefinitionKey: 'someKey',
    processDefinitionVersions: ['someVersion'],
    tenantIds: [null],
  });
  loadFrequencyData.mockClear();
  await node.instance().updateConfig({filters: ['someFilter']});

  expect(loadFrequencyData.mock.calls[0][4]).toEqual(['someFilter']);
});

it('should not try to load frequency data if no process definition is selected', () => {
  const node = shallow(<BranchAnalysis {...props} />);

  loadFrequencyData.mockClear();
  node.instance().updateConfig({filters: ['someFilter']});

  expect(loadFrequencyData).not.toHaveBeenCalled();
});

it('should contain a statistics section if gateway and endEvent is selected', () => {
  const node = shallow(<BranchAnalysis {...props} />);

  node.instance().setState({
    gateway: 'g',
    endEvent: 'e',
  });

  expect(node).toIncludeText('Statistics');
});

it('should clear the selection when another process definition is selected', async () => {
  const node = shallow(<BranchAnalysis {...props} />);

  await node.instance().setState({gateway: 'g', endEvent: 'e'});
  await node.instance().updateConfig({
    processDefinitionKey: 'newKey',
    processDefinitionVersions: ['latest'],
    tenantIds: [],
  });

  expect(node).toHaveState('gateway', null);
  expect(node).toHaveState('endEvent', null);
  expect(node.state().config.filters).toEqual([]);
});

it('should not clear the selection when the xml stays the same', async () => {
  const node = shallow(<BranchAnalysis {...props} />);

  loadProcessDefinitionXml.mockReturnValue('some xml');

  await node.instance().updateConfig({
    processDefinitionKey: 'firstKey',
    processDefinitionVersions: ['2'],
    tenantIds: [],
  });
  await node.instance().setState({gateway: 'g', endEvent: 'e'});
  await node.instance().updateConfig({
    processDefinitionKey: 'newKey',
    processDefinitionVersions: ['latest'],
    tenantIds: [],
  });

  expect(node).toHaveState('gateway', 'g');
  expect(node).toHaveState('endEvent', 'e');
});

it('should show a warning message when there are incompatible filters', async () => {
  incompatibleFilters.mockReturnValue(true);
  const node = await shallow(<BranchAnalysis {...props} />);
  await node.update();
  expect(node.find('InlineNotification')).toExist();
});

it('should not reset the xml when adding a filter', async () => {
  const node = shallow(<BranchAnalysis {...props} />);

  await node.instance().updateConfig({
    processDefinitionKey: 'someKey',
    processDefinitionVersions: ['someVersion'],
    tenantIds: ['a', 'b'],
  });
  await node.instance().updateConfig({
    filters: [{type: 'completedInstancesOnly', data: null}],
  });

  expect(node.state().xml).not.toBe(null);
});
