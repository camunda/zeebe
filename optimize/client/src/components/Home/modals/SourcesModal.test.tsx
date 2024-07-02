/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {ReactElement} from 'react';
import {runAllEffects} from '__mocks__/react';
import {shallow} from 'enzyme';

import {areTenantsAvailable, getOptimizeProfile} from 'config';

import {getDefinitionsWithTenants, getTenantsWithDefinitions} from './service';
import SourcesModal from './SourcesModal';

jest.mock('./service', () => ({
  getDefinitionsWithTenants: jest.fn().mockReturnValue([
    {key: 'def1', name: 'def1Name', type: 'process', tenants: [{id: null}, {id: 'engineering'}]},
    {key: 'def2', name: 'def2Name', type: 'process', tenants: [{id: null}]},
  ]),
  getTenantsWithDefinitions: jest.fn().mockReturnValue([{id: null}, {id: 'engineering'}]),
}));

jest.mock('config', () => ({
  areTenantsAvailable: jest.fn().mockReturnValue(true),
  getOptimizeProfile: jest.fn().mockReturnValue('platform'),
}));

jest.mock('hooks', () => ({
  ...jest.requireActual('hooks'),
  useErrorHandling: () => ({
    mightFail: jest.fn().mockImplementation((data, cb) => cb(data)),
  }),
}));

const props = {
  open: true,
  onClose: jest.fn(),
  onConfirm: jest.fn(),
  confirmText: 'confirm',
};

beforeEach(() => {
  jest.clearAllMocks();
});

it('should load definitions and tenants on mount', async () => {
  shallow(<SourcesModal {...props} />);

  await runAllEffects();

  expect(getDefinitionsWithTenants).toHaveBeenCalled();
  expect(getTenantsWithDefinitions).toHaveBeenCalled();
});

it('should display key of definition if name is null', async () => {
  (getDefinitionsWithTenants as jest.Mock).mockReturnValueOnce([
    {key: 'testDef', name: null, type: 'process', tenants: [{id: 'sales'}]},
  ]);

  const node = shallow(<SourcesModal {...props} />);

  await runAllEffects();

  expect(node.find('Table').prop<string[][]>('body')[0]?.[1]).toBe('testDef');
});

it('should hide tenants if they are not available', async () => {
  (areTenantsAvailable as jest.Mock).mockReturnValueOnce(false);
  const node = shallow(<SourcesModal {...props} />);

  await runAllEffects();
  await flushPromises();

  expect(getTenantsWithDefinitions).not.toHaveBeenCalled();
  expect(
    node
      .find('Table')
      .prop<{id?: string}[]>('head')
      .find((col) => col?.id === 'tenants')
  ).not.toBeDefined();
  expect(node.find('.header Typeahead')).not.toExist();
});

it('should preselect all definitions if specified', async () => {
  const node = shallow(<SourcesModal {...props} preSelectAll />);

  await runAllEffects();

  expect(
    node
      .find('Table')
      .prop<JSX.Element[][]>('body')
      .every((row) => row[0]?.props.checked)
  ).toBe(true);
});

it('should select and deselect a definition', async () => {
  const node = shallow(<SourcesModal {...props} />);

  await runAllEffects();

  node
    .find('Table')
    .prop<JSX.Element[][]>('body')[0]?.[0]
    ?.props.onSelect({target: {checked: true}});

  expect(node.find('Table').prop<JSX.Element[][]>('body')[0]?.[0]?.props.checked).toBe(true);

  node
    .find('Table')
    .prop<JSX.Element[][]>('body')[0]?.[0]
    ?.props.onSelect({target: {checked: false}});

  expect(node.find('Table').prop<JSX.Element[][]>('body')[0]?.[0]?.props.checked).toBe(false);
});

it('should selected/deselect all definitions', async () => {
  const node = shallow(<SourcesModal {...props} />);

  await runAllEffects();

  node
    .find('Table')
    .prop<{label: JSX.Element}[]>('head')[0]
    ?.label.props.onSelect({target: {checked: true}});

  expect(
    node
      .find('Table')
      .prop<JSX.Element[][]>('body')
      .every((row) => row[0]?.props.checked)
  ).toBe(true);

  node
    .find('Table')
    .prop<{label: JSX.Element}[]>('head')[0]
    ?.label.props.onSelect({target: {checked: false}});

  expect(
    node
      .find('Table')
      .prop<JSX.Element[][]>('body')
      .every((row) => !row[0]?.props.checked)
  ).toBe(true);
});

it('should filter definitions by tenant', async () => {
  const node = shallow(<SourcesModal {...props} />);

  await runAllEffects();

  const toolbar = shallow(node.find('Table').prop<any>('toolbar'));
  toolbar.find('TableToolbarSearch').prop<(target: HTMLInputElement) => void>('onChange')?.({
    target: {value: 'engineering'},
  } as unknown as HTMLInputElement);

  expect(
    node
      .find('Table')
      .prop<string[][]>('body')
      .find((row) => row[1] === 'def2Name')
  ).toBe(undefined);
});

it('should only select the tenant used in filtering', async () => {
  const spy = jest.fn();
  const node = shallow(<SourcesModal {...props} onConfirm={spy} />);

  await runAllEffects();

  const toolbar = shallow<ReactElement>(node.find('Table').prop('toolbar'));
  toolbar.find('ComboBox').simulate('change', {selectedItem: {value: 'engineering'}});

  node
    .find('Table')
    .prop<{label: JSX.Element}[]>('head')[0]
    ?.label.props.onSelect({target: {checked: true}});

  node.find('.confirm').simulate('click');

  expect(spy).toHaveBeenCalledWith([
    {definitionKey: 'def1', definitionType: 'process', tenants: ['engineering']},
  ]);
});

it('should change the selected tenants based on the popover in C7', async () => {
  const spy = jest.fn();
  const node = shallow(<SourcesModal {...props} onConfirm={spy} preSelectAll />);

  await runAllEffects();
  await node.update();

  const tenantPopover = (node.find('Table').prop('body') as any)[0][3];

  tenantPopover.props.onChange([{id: 'test'}]);

  node.find('.confirm').simulate('click');

  expect(spy).toHaveBeenCalledWith([
    {
      definitionKey: 'def1',
      definitionType: 'process',
      tenants: [{id: 'test'}],
    },
    {
      definitionKey: 'def2',
      definitionType: 'process',
      tenants: [null],
    },
  ]);
});

it('should display the only tenant value as text in self managed mode', async () => {
  (getOptimizeProfile as jest.Mock).mockReturnValueOnce('ccsm');
  (getTenantsWithDefinitions as jest.Mock).mockReturnValueOnce([{id: 'engineering'}]);
  (getDefinitionsWithTenants as jest.Mock).mockReturnValueOnce([
    {key: 'testDef', name: null, type: 'process', tenants: [{id: '<default>', name: 'Default'}]},
  ]);

  const spy = jest.fn();
  const node = shallow(<SourcesModal {...props} onConfirm={spy} preSelectAll />);

  await runAllEffects();
  await node.update();

  expect((node.find('Table').prop('body') as any)[0][3]).toEqual(<>Default</>);
});
