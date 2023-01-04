/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Task} from './index';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {
  render,
  screen,
  waitFor,
  waitForElementToBeRemoved,
} from '@testing-library/react';
import {
  mockGetTaskClaimed,
  mockGetTaskCompletedWithForm,
  mockGetTaskClaimedWithForm,
  mockGetTaskCompleted,
} from 'modules/queries/get-task';
import {MockThemeProvider} from 'modules/theme/MockProvider';
import {mockGetCurrentUser} from 'modules/queries/get-current-user';
import {mockCompleteTask} from 'modules/mutations/complete-task';
import {
  mockGetAllOpenTasks,
  mockGetAllOpenTasksUnclaimed,
} from 'modules/queries/get-tasks';
import {mockClaimTask} from 'modules/mutations/claim-task';
import {mockUnclaimTask} from 'modules/mutations/unclaim-task';
import userEvent from '@testing-library/user-event';
import {mockGetForm, mockGetInvalidForm} from 'modules/queries/get-form';
import {
  mockGetTaskVariables,
  mockGetTaskEmptyVariables,
} from 'modules/queries/get-task-variables';
import {mockGetSelectedVariables} from 'modules/queries/get-selected-variables';
import {ApolloProvider} from '@apollo/client';
import {client} from 'modules/apollo-client';
import {graphql} from 'msw';
import {mockServer} from 'modules/mockServer';
import {LocationLog} from 'modules/utils/LocationLog';
import {notificationsStore} from 'modules/stores/notifications';

jest.mock('modules/stores/notifications', () => ({
  notificationsStore: {
    displayNotification: jest.fn(() => () => {}),
  },
}));

const getWrapper = (
  initialEntries: React.ComponentProps<typeof MemoryRouter>['initialEntries'],
) => {
  type Props = {
    children?: React.ReactNode;
  };

  const Wrapper: React.FC<Props> = ({children}) => {
    return (
      <ApolloProvider client={client}>
        <MockThemeProvider>
          <MemoryRouter initialEntries={initialEntries}>
            <Routes>
              <Route path="/:id" element={children} />
              <Route path="*" element={<LocationLog />} />
            </Routes>
          </MemoryRouter>
        </MockThemeProvider>
      </ApolloProvider>
    );
  };

  return Wrapper;
};

describe('<Task />', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('should render created task', async () => {
    mockServer.use(
      graphql.query('GetTask', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskClaimed().result.data));
      }),
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res.once(ctx.data(mockGetCurrentUser.result.data));
      }),
      graphql.query('GetTaskVariables', (_, res, ctx) => {
        return res(ctx.data(mockGetTaskVariables().result.data));
      }),
    );

    render(<Task />, {
      wrapper: getWrapper(['/0']),
    });

    expect(await screen.findByTestId('details-table')).toBeInTheDocument();
    expect(await screen.findByTestId('variables-table')).toBeInTheDocument();
    expect(screen.getByText('Complete Task')).toBeInTheDocument();
  });

  it('should render created task with embedded form', async () => {
    mockServer.use(
      graphql.query('GetTask', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskClaimedWithForm().result.data));
      }),
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res.once(ctx.data(mockGetCurrentUser.result.data));
      }),
      graphql.query('GetForm', (_, res, ctx) => {
        return res.once(ctx.data(mockGetForm.result.data));
      }),
      graphql.query('GetSelectedVariables', (_, res, ctx) => {
        return res.once(ctx.data(mockGetSelectedVariables().result.data));
      }),
    );

    render(<Task />, {
      wrapper: getWrapper(['/0']),
    });

    await waitForElementToBeRemoved(() =>
      screen.getByTestId('details-skeleton'),
    );

    expect(screen.getByTestId('details-table')).toBeInTheDocument();
    expect(screen.getByTestId('embedded-form')).toBeInTheDocument();
    expect(
      await screen.findByRole('button', {name: /complete task/i}),
    ).toBeInTheDocument();
  });

  it('should render completed task', async () => {
    mockServer.use(
      graphql.query('GetTask', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskCompleted().result.data));
      }),
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res.once(ctx.data(mockGetCurrentUser.result.data));
      }),
      graphql.query('GetTaskVariables', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskVariables().result.data));
      }),
    );

    render(<Task />, {
      wrapper: getWrapper(['/0']),
    });

    expect(await screen.findByTestId('details-table')).toBeInTheDocument();
    expect(await screen.findByTestId('variables-table')).toBeInTheDocument();
    expect(screen.queryByText('Complete Task')).not.toBeInTheDocument();
  });

  it('should render completed task with embedded form', async () => {
    mockServer.use(
      graphql.query('GetTask', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskCompletedWithForm().result.data));
      }),
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res.once(ctx.data(mockGetCurrentUser.result.data));
      }),
      graphql.query('GetForm', (_, res, ctx) => {
        return res.once(ctx.data(mockGetForm.result.data));
      }),
      graphql.query('GetTaskVariables', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskVariables().result.data));
      }),
    );

    render(<Task />, {
      wrapper: getWrapper(['/0']),
    });

    expect(await screen.findByTestId('details-table')).toBeInTheDocument();
    expect(await screen.findByTestId('embedded-form')).toBeInTheDocument();
    expect(screen.queryByText('Complete Task')).not.toBeInTheDocument();
  });

  it('should complete task without variables', async () => {
    mockServer.use(
      graphql.query('GetTask', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskClaimed().result.data));
      }),
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res.once(ctx.data(mockGetCurrentUser.result.data));
      }),
      graphql.mutation('CompleteTask', (_, res, ctx) => {
        return res.once(ctx.data(mockCompleteTask().result.data));
      }),
      graphql.query('GetTasks', (req, res, ctx) => {
        const {state, assigned} = req.variables;

        if (state === 'CREATED' && assigned === undefined) {
          return res.once(ctx.data(mockGetAllOpenTasks(true).result.data));
        }

        return res.once(
          ctx.errors([
            {
              message: 'Invalid query',
            },
          ]),
        );
      }),
      graphql.query('GetTaskVariables', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskEmptyVariables().result.data));
      }),
    );

    render(<Task />, {
      wrapper: getWrapper(['/0']),
    });

    userEvent.click(
      await screen.findByRole('button', {name: /complete task/i}),
    );

    await waitFor(() => {
      expect(screen.getByTestId('pathname')).toHaveTextContent('/');
    });

    expect(notificationsStore.displayNotification).toHaveBeenCalledWith({
      kind: 'success',
      title: 'Task completed',
      isDismissable: true,
    });
  });

  it('should get error on complete task', async () => {
    mockServer.use(
      graphql.query('GetTask', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskClaimed().result.data));
      }),
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res.once(ctx.data(mockGetCurrentUser.result.data));
      }),
      graphql.mutation('CompleteTask', (_, res) => {
        return res.networkError('Network error');
      }),
      graphql.query('GetTaskVariables', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskVariables().result.data));
      }),
    );

    render(<Task />, {
      wrapper: getWrapper(['/0']),
    });

    userEvent.click(
      await screen.findByRole('button', {name: /complete task/i}),
    );

    expect(screen.getByText('Completing task...')).toBeInTheDocument();
    expect(await screen.findByText('Completion failed')).toBeInTheDocument();

    await waitFor(() => {
      expect(notificationsStore.displayNotification).toHaveBeenCalledWith({
        kind: 'error',
        title: 'Task could not be completed',
        subtitle: 'Service is not reachable',
        isDismissable: true,
      });
    });

    expect(
      await screen.findByRole('button', {name: /complete task/i}),
    ).toBeInTheDocument();
  });

  it('should show a skeleton while loading', async () => {
    mockServer.use(
      graphql.query('GetTask', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskClaimed().result.data));
      }),
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res.once(ctx.data(mockGetCurrentUser.result.data));
      }),
      graphql.query('GetTaskVariables', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskVariables().result.data));
      }),
    );

    render(<Task />, {
      wrapper: getWrapper(['/0']),
    });

    await waitForElementToBeRemoved(() =>
      screen.getByTestId('details-skeleton'),
    );

    expect(screen.getByTestId('details-table')).toBeInTheDocument();
  });

  it('should reset variables', async () => {
    mockServer.use(
      graphql.query('GetTask', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskClaimed().result.data));
      }),
      graphql.query('GetCurrentUser', (_, res, ctx) => {
        return res.once(ctx.data(mockGetCurrentUser.result.data));
      }),
      graphql.query('GetTaskVariables', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskEmptyVariables().result.data));
      }),
      graphql.mutation('UnclaimTask', (_, res, ctx) => {
        return res.once(ctx.data(mockUnclaimTask.result.data));
      }),
      graphql.query('GetTasks', (_, res, ctx) => {
        return res.once(
          ctx.data(mockGetAllOpenTasksUnclaimed(true).result.data),
        );
      }),
      graphql.mutation('ClaimTask', (_, res, ctx) => {
        return res.once(ctx.data(mockClaimTask.result.data));
      }),
      graphql.query('GetTasks', (_, res, ctx) => {
        return res.once(ctx.data(mockGetAllOpenTasks(true).result.data));
      }),
      graphql.query('GetTask', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskClaimed().result.data));
      }),
      graphql.query('GetTaskVariables', (_, res, ctx) => {
        return res.once(ctx.data(mockGetTaskEmptyVariables().result.data));
      }),
    );

    render(<Task />, {
      wrapper: getWrapper(['/0']),
    });

    userEvent.click(await screen.findByRole('button', {name: /add variable/i}));
    userEvent.type(screen.getByLabelText(/1st variable name/i), 'valid_name');
    userEvent.type(
      screen.getByLabelText(/1st variable value/i),
      '"valid_value"',
    );
    userEvent.click(screen.getByRole('button', {name: /^unclaim$/i}));
    userEvent.click(await screen.findByRole('button', {name: /^claim$/i}));

    expect(
      await screen.findByRole('button', {name: /^unclaim$/i}),
    ).toBeInTheDocument();
    expect(screen.getByText(/task has no variables/i)).toBeInTheDocument();
  });

  it('should render created task with variables form', async () => {
    mockServer.use(
      graphql.query('GetCurrentUser', (_, res, ctx) =>
        res.once(ctx.data(mockGetCurrentUser.result.data)),
      ),
      graphql.query('GetTask', (_, res, ctx) =>
        res.once(ctx.data(mockGetTaskClaimedWithForm().result.data)),
      ),
      graphql.query('GetForm', (_, res, ctx) =>
        res.once(ctx.data(mockGetInvalidForm.result.data)),
      ),
      graphql.query('GetTaskVariables', (_, res, ctx) =>
        res.once(ctx.data(mockGetTaskVariables().result.data)),
      ),
    );

    render(<Task />, {
      wrapper: getWrapper(['/0']),
    });

    expect(await screen.findByTestId('details-table')).toBeInTheDocument();
    expect(await screen.findByTestId('variables-table')).toBeInTheDocument();
    expect(notificationsStore.displayNotification).toHaveBeenCalledWith({
      kind: 'error',
      title: 'Invalid Form schema',
      isDismissable: true,
    });
  });
});
