/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React from 'react';
import {mount} from 'enzyme';

import DashboardTile from './DashboardTile';

jest.mock('./ExternalUrlTile', () => {
  const actual = jest.requireActual('./ExternalUrlTile');
  const ExternalUrlTile = ({children}) => <span>ExternalUrlTile: {children()}</span>;
  ExternalUrlTile.isTileOfType = actual.ExternalUrlTile.isTileOfType;
  return {ExternalUrlTile};
});
jest.mock('./TextTile', () => {
  const actual = jest.requireActual('./TextTile');
  const TextTile = ({children}) => <span>TextTile: {children()}</span>;
  TextTile.isTileOfType = actual.TextTile.isTileOfType;
  return {TextTile};
});
jest.mock('./OptimizeReportTile', () => {
  const actual = jest.requireActual('./OptimizeReportTile');
  const OptimizeReportTile = ({children}) => <span>OptimizeReportTile: {children()}</span>;
  OptimizeReportTile.isTileOfType = actual.OptimizeReportTile.isTileOfType;
  return {OptimizeReportTile};
});

const props = {
  tile: {
    type: 'optimize_report',
    id: 'a',
  },
};

it('should render optional addons', () => {
  const TextRenderer = ({children}) => <p>{children}</p>;

  const node = mount(
    <DashboardTile {...props} addons={[<TextRenderer>I am an addon!</TextRenderer>]} />
  );

  expect(node).toIncludeText('I am an addon!');
});

it('should pass properties to tile addons', () => {
  const PropsRenderer = (props) => <p>{JSON.stringify(Object.keys(props))}</p>;

  const node = mount(<DashboardTile {...props} addons={[<PropsRenderer key="propsRenderer" />]} />);

  expect(node).toIncludeText('tile');
});
