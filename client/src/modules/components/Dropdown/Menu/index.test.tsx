/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {render, screen} from '@testing-library/react';
import {noop} from 'lodash';
import {ThemeProvider} from 'modules/theme/ThemeProvider';

import Menu from './index';

describe('<Menu />', () => {
  it('should render its children', () => {
    const {rerender} = render(
      <Menu onKeyDown={noop} placement="top">
        <span>I am a Dropdown.Option Component</span>
      </Menu>,
      {wrapper: ThemeProvider}
    );

    expect(screen.getByRole('listitem')).toBeInTheDocument();

    rerender(<Menu onKeyDown={noop} placement="top" />);

    expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
  });
});
