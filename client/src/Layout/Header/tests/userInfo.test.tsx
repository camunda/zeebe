/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {DEFAULT_MOCK_CLIENT_CONFIG} from 'modules/mocks/window';
import {mockServer} from 'modules/mockServer';
import {mockGetCurrentUser} from 'modules/queries/get-current-user';
import {authenticationStore} from 'modules/stores/authentication';
import {graphql, rest} from 'msw';
import {Header} from '..';
import {Wrapper} from './mocks';

describe('User info', () => {
  afterEach(() => {
    window.clientConfig = DEFAULT_MOCK_CLIENT_CONFIG;
  });

  it('should render user display name', async () => {
    mockServer.use(
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res(ctx.data(mockGetCurrentUser.result.data));
      }),
    );

    render(<Header />, {
      wrapper: Wrapper,
    });

    userEvent.click(
      await screen.findByRole('button', {
        name: /settings/i,
      }),
    );

    expect(await screen.findByText('Demo User')).toBeInTheDocument();
  });

  it('should handle a SSO user', async () => {
    window.clientConfig = {...window.clientConfig, canLogout: false};

    mockServer.use(
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res(ctx.data(mockGetCurrentUser.result.data));
      }),
    );

    render(<Header />, {
      wrapper: Wrapper,
    });

    userEvent.click(
      screen.getByRole('button', {
        name: /settings/i,
      }),
    );

    expect(await screen.findByText('Demo User')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', {
        name: /log out/i,
      }),
    ).not.toBeInTheDocument();
  });

  it('should handle logout', async () => {
    const logoutSpy = jest
      .spyOn(authenticationStore, 'handleLogout')
      .mockImplementation();

    mockServer.use(
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res(ctx.data(mockGetCurrentUser.result.data));
      }),
      rest.post('/api/logout', (_, res, ctx) =>
        res.once(ctx.status(204), ctx.json('')),
      ),
    );

    render(<Header />, {
      wrapper: Wrapper,
    });

    userEvent.click(
      screen.getByRole('button', {
        name: /settings/i,
      }),
    );

    expect(await screen.findByText('Demo User')).toBeInTheDocument();

    userEvent.click(
      await screen.findByRole('button', {
        name: /log out/i,
      }),
    );

    expect(logoutSpy).toHaveBeenCalled();
    logoutSpy.mockRestore();
  });

  it('should render links', async () => {
    const originalWindowOpen = window.open;
    const mockOpenFn = jest.fn();
    window.open = mockOpenFn;

    mockServer.use(
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res(ctx.data(mockGetCurrentUser.result.data));
      }),
    );

    render(<Header />, {
      wrapper: Wrapper,
    });

    userEvent.click(
      screen.getByRole('button', {
        name: /settings/i,
      }),
    );

    expect(await screen.findByText('Demo User')).toBeInTheDocument();

    userEvent.click(screen.getByRole('button', {name: 'Terms of use'}));
    expect(mockOpenFn).toHaveBeenLastCalledWith(
      'https://camunda.com/legal/terms/camunda-platform/camunda-platform-8-saas-trial/',
      '_blank',
    );

    userEvent.click(screen.getByRole('button', {name: 'Privacy policy'}));
    expect(mockOpenFn).toHaveBeenLastCalledWith(
      'https://camunda.com/legal/privacy/',
      '_blank',
    );

    userEvent.click(screen.getByRole('button', {name: 'Imprint'}));
    expect(mockOpenFn).toHaveBeenLastCalledWith(
      'https://camunda.com/legal/imprint/',
      '_blank',
    );

    expect(
      screen.queryByRole('button', {name: 'Cookie preferences'}),
    ).not.toBeInTheDocument();

    window.open = originalWindowOpen;
  });

  it('should cookie preferences with correct link', async () => {
    const originalWindowOpen = window.open;
    const mockOpenFn = jest.fn();
    const mockShowDrawer = jest.fn();

    window.open = mockOpenFn;
    window.Osano = {
      cm: {
        analytics: false,
        showDrawer: mockShowDrawer,
        addEventListener: jest.fn(),
      },
    };

    mockServer.use(
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res(ctx.data(mockGetCurrentUser.result.data));
      }),
    );

    render(<Header />, {
      wrapper: Wrapper,
    });

    userEvent.click(
      await screen.findByRole('button', {
        name: /settings/i,
      }),
    );

    expect(await screen.findByText('Demo User')).toBeInTheDocument();

    userEvent.click(screen.getByRole('button', {name: 'Cookie preferences'}));

    expect(mockShowDrawer).toHaveBeenLastCalledWith(
      'osano-cm-dom-info-dialog-open',
    );

    window.open = originalWindowOpen;
    window.Osano = undefined;
  });
});
