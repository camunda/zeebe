/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React from 'react';
import {mount} from 'enzyme';

import AutoRefreshBehavior from './AutoRefreshBehavior';

jest.useFakeTimers();
jest.spyOn(global, 'setInterval');
jest.spyOn(global, 'clearInterval');

const ReportSpy = jest.fn();
const interval = 600;

it('should register an interval with the specified interval duration and function', () => {
  mount(<AutoRefreshBehavior loadTileData={ReportSpy} interval={interval} />);

  expect(setInterval).toHaveBeenCalledTimes(1);
});

it('should clear the interval when component is unmounted', () => {
  const node = mount(<AutoRefreshBehavior loadTileData={ReportSpy} interval={interval} />);
  node.unmount();

  expect(clearInterval).toHaveBeenCalledTimes(1);
});

it('should update the interval when the interval property changes', () => {
  const node = mount(<AutoRefreshBehavior loadTileData={ReportSpy} interval={interval} />);

  clearInterval.mockClear();
  node.setProps({interval: 1000});

  expect(clearInterval).toHaveBeenCalledTimes(1);
  expect(setInterval).toHaveBeenCalled();
});

it('should invoke the loadTileData function after the interval duration ends', async () => {
  mount(<AutoRefreshBehavior loadTileData={ReportSpy} interval={interval} />);
  jest.advanceTimersByTime(700);
  expect(ReportSpy).toHaveBeenCalled();
});
