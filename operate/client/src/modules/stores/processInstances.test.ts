/*
 * Copyright Camunda Services GmbH
 *
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING, OR DISTRIBUTING THE SOFTWARE ("USE"), YOU INDICATE YOUR ACCEPTANCE TO AND ARE ENTERING INTO A CONTRACT WITH, THE LICENSOR ON THE TERMS SET OUT IN THIS AGREEMENT. IF YOU DO NOT AGREE TO THESE TERMS, YOU MUST NOT USE THE SOFTWARE. IF YOU ARE RECEIVING THE SOFTWARE ON BEHALF OF A LEGAL ENTITY, YOU REPRESENT AND WARRANT THAT YOU HAVE THE ACTUAL AUTHORITY TO AGREE TO THE TERMS AND CONDITIONS OF THIS AGREEMENT ON BEHALF OF SUCH ENTITY.
 * "Licensee" means you, an individual, or the entity on whose behalf you receive the Software.
 *
 * Permission is hereby granted, free of charge, to the Licensee obtaining a copy of this Software and associated documentation files to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject in each case to the following conditions:
 * Condition 1: If the Licensee distributes the Software or any derivative works of the Software, the Licensee must attach this Agreement.
 * Condition 2: Without limiting other conditions in this Agreement, the grant of rights is solely for non-production use as defined below.
 * "Non-production use" means any use of the Software that is not directly related to creating products, services, or systems that generate revenue or other direct or indirect economic benefits.  Examples of permitted non-production use include personal use, educational use, research, and development. Examples of prohibited production use include, without limitation, use for commercial, for-profit, or publicly accessible systems or use for commercial or revenue-generating purposes.
 *
 * If the Licensee is in breach of the Conditions, this Agreement, including the rights granted under it, will automatically terminate with immediate effect.
 *
 * SUBJECT AS SET OUT BELOW, THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * NOTHING IN THIS AGREEMENT EXCLUDES OR RESTRICTS A PARTY’S LIABILITY FOR (A) DEATH OR PERSONAL INJURY CAUSED BY THAT PARTY’S NEGLIGENCE, (B) FRAUD, OR (C) ANY OTHER LIABILITY TO THE EXTENT THAT IT CANNOT BE LAWFULLY EXCLUDED OR RESTRICTED.
 */

import {processInstancesStore} from './processInstances';
import {groupedProcessesMock, mockProcessStatistics} from 'modules/testUtils';
import {waitFor} from 'modules/testing-library';
import {createOperation} from 'modules/utils/instance';
import {mockFetchProcessInstances} from 'modules/mocks/api/processInstances/fetchProcessInstances';
import {mockFetchGroupedProcesses} from 'modules/mocks/api/processes/fetchGroupedProcesses';
import {mockFetchProcessInstancesStatistics} from 'modules/mocks/api/processInstances/fetchProcessInstancesStatistics';
import {mockFetchProcessXML} from 'modules/mocks/api/processes/fetchProcessXML';
import {mockServer} from 'modules/mock-server/node';
import {rest} from 'msw';
import {checkPollingHeader} from 'modules/mocks/api/mockRequest';

const instance: ProcessInstanceEntity = {
  id: '2251799813685625',
  processId: '2251799813685623',
  processName: 'Without Incidents Process',
  processVersion: 1,
  startDate: '2020-11-19T08:14:05.406+0000',
  endDate: null,
  state: 'ACTIVE',
  bpmnProcessId: 'withoutIncidentsProcess',
  hasActiveOperation: false,
  operations: [],
  sortValues: ['', ''],
  parentInstanceId: null,
  rootInstanceId: null,
  callHierarchy: [],
  tenantId: '',
};

const instanceWithActiveOperation: ProcessInstanceEntity = {
  id: '2251799813685627',
  processId: '2251799813685623',
  processName: 'Without Incidents Process',
  processVersion: 1,
  startDate: '2020-11-19T08:14:05.490+0000',
  endDate: null,
  state: 'ACTIVE',
  bpmnProcessId: 'withoutIncidentsProcess',
  hasActiveOperation: true,
  operations: [],
  sortValues: ['', ''],
  parentInstanceId: null,
  rootInstanceId: null,
  callHierarchy: [],
  tenantId: '',
};

const mockInstances = [instance, instanceWithActiveOperation];

const mockProcessInstances = {
  processInstances: mockInstances,
  totalCount: 100,
};

describe('stores/processInstances', () => {
  mockFetchGroupedProcesses().withSuccess(groupedProcessesMock);
  mockFetchProcessInstancesStatistics().withSuccess(mockProcessStatistics);

  beforeEach(async () => {
    mockFetchProcessXML().withSuccess('');
  });
  afterEach(() => {
    processInstancesStore.reset();
  });

  it('should fetch initial instances', async () => {
    mockFetchProcessInstances().withSuccess(mockProcessInstances);

    expect(processInstancesStore.state.status).toBe('initial');

    processInstancesStore.fetchProcessInstancesFromFilters();

    expect(processInstancesStore.state.status).toBe('first-fetch');

    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );

    expect(processInstancesStore.state.filteredProcessInstancesCount).toBe(100);
    expect(processInstancesStore.state.processInstances).toEqual(
      mockProcessInstances.processInstances,
    );
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['2251799813685627']);
  });

  it('should fetch next instances', async () => {
    mockFetchProcessInstances().withSuccess(mockProcessInstances);

    expect(processInstancesStore.state.status).toBe('initial');

    processInstancesStore.fetchProcessInstancesFromFilters();

    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );

    mockFetchProcessInstances().withSuccess({
      ...mockProcessInstances,
      processInstances: [
        {...instance, id: '100'},
        {...instance, hasActiveOperation: true, id: '101'},
      ],
    });

    processInstancesStore.fetchNextInstances();

    expect(processInstancesStore.state.status).toBe('fetching-next');
    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );

    expect(processInstancesStore.state.processInstances.length).toBe(4);
    expect(processInstancesStore.state.processInstances[2]?.id).toBe('100');
    expect(processInstancesStore.state.processInstances[3]?.id).toBe('101');
    expect(processInstancesStore.state.latestFetch).toEqual({
      fetchType: 'next',
      processInstancesCount: 2,
    });

    mockFetchProcessInstances().withSuccess({
      ...mockProcessInstances,
      processInstances: [{...instance, id: '200'}],
    });

    processInstancesStore.fetchNextInstances();

    expect(processInstancesStore.state.status).toBe('fetching-next');
    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );

    expect(processInstancesStore.state.processInstances.length).toBe(5);
    expect(processInstancesStore.state.processInstances[4]!.id).toBe('200');
    expect(processInstancesStore.state.latestFetch).toEqual({
      fetchType: 'next',
      processInstancesCount: 1,
    });

    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['2251799813685627', '101']);
  });

  it('should fetch previous instances', async () => {
    mockFetchProcessInstances().withSuccess(mockProcessInstances);

    expect(processInstancesStore.state.status).toBe('initial');

    processInstancesStore.fetchProcessInstancesFromFilters();

    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );

    mockFetchProcessInstances().withSuccess({
      ...mockProcessInstances,
      processInstances: [{...instance, id: '100'}],
    });

    processInstancesStore.fetchPreviousInstances();

    expect(processInstancesStore.state.status).toBe('fetching-prev');
    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );

    expect(processInstancesStore.state.processInstances.length).toBe(3);
    expect(processInstancesStore.state.processInstances[0]?.id).toBe('100');
    expect(processInstancesStore.state.latestFetch).toEqual({
      fetchType: 'prev',
      processInstancesCount: 1,
    });

    mockFetchProcessInstances().withSuccess({
      ...mockProcessInstances,
      processInstances: [{...instance, id: '200'}],
    });

    processInstancesStore.fetchPreviousInstances();

    expect(processInstancesStore.state.status).toBe('fetching-prev');
    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );

    expect(processInstancesStore.state.processInstances.length).toBe(4);
    expect(processInstancesStore.state.processInstances[0]?.id).toBe('200');
    expect(processInstancesStore.state.processInstances[1]?.id).toBe('100');
    expect(processInstancesStore.state.latestFetch).toEqual({
      fetchType: 'prev',
      processInstancesCount: 1,
    });

    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['2251799813685627']);
  });

  it('should refresh all instances', async () => {
    mockFetchProcessInstances().withSuccess(mockProcessInstances);

    processInstancesStore.fetchProcessInstancesFromFilters();

    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['2251799813685627']);

    // mock for refresh all instances
    mockFetchProcessInstances().withSuccess({
      processInstances: [
        instance,
        {...instanceWithActiveOperation, hasActiveOperation: false},
      ],
      totalCount: 100,
    });

    // mock for refresh running process instances count
    mockFetchProcessInstances().withSuccess({
      processInstances: [
        instance,
        {...instanceWithActiveOperation, hasActiveOperation: false},
      ],
      totalCount: 100,
    });

    processInstancesStore.refreshAllInstances();
    await waitFor(() =>
      expect(
        processInstancesStore.processInstanceIdsWithActiveOperations,
      ).toEqual([]),
    );

    // mock for refresh all instances
    mockFetchProcessInstances().withSuccess({
      processInstances: [
        {...instance, hasActiveOperation: true},
        {...instanceWithActiveOperation},
      ],
      totalCount: 100,
    });

    // mock for refresh running process instances count
    mockFetchProcessInstances().withSuccess({
      processInstances: [
        {...instance, hasActiveOperation: true},
        {...instanceWithActiveOperation},
      ],
      totalCount: 100,
    });

    processInstancesStore.refreshAllInstances();
    await waitFor(() =>
      expect(
        processInstancesStore.processInstanceIdsWithActiveOperations,
      ).toEqual(['2251799813685625', '2251799813685627']),
    );
  });

  it('should reset store', async () => {
    mockFetchProcessInstances().withSuccess(mockProcessInstances);

    await processInstancesStore.fetchInstances({
      fetchType: 'initial',
      payload: {query: {}},
    });

    expect(processInstancesStore.state.processInstances).toEqual(
      mockProcessInstances.processInstances,
    );

    processInstancesStore.reset();
    expect(processInstancesStore.state).toEqual({
      filteredProcessInstancesCount: 0,
      processInstances: [],
      status: 'initial',
      runningInstancesCount: -1,
      latestFetch: null,
    });
  });

  it('should get visible ids in list panel', async () => {
    mockFetchProcessInstances().withSuccess(mockProcessInstances);

    await processInstancesStore.fetchInstances({
      fetchType: 'initial',
      payload: {query: {}},
    });

    expect(processInstancesStore.visibleIdsInListPanel).toEqual(
      mockInstances.map(({id}) => id),
    );
  });

  it('should get areProcessInstancesEmpty', async () => {
    mockFetchProcessInstances().withSuccess(mockProcessInstances);

    await processInstancesStore.fetchInstances({
      fetchType: 'initial',
      payload: {query: {}},
    });

    expect(processInstancesStore.areProcessInstancesEmpty).toBe(false);

    mockFetchProcessInstances().withSuccess({
      processInstances: [],
      totalCount: 0,
    });

    await processInstancesStore.fetchInstances({
      fetchType: 'initial',
      payload: {query: {}},
    });

    expect(processInstancesStore.areProcessInstancesEmpty).toBe(true);
  });

  it('should mark instances with active operations', async () => {
    const cancelOperation = createOperation('CANCEL_PROCESS_INSTANCE');
    mockFetchProcessInstances().withSuccess({
      totalCount: 100,
      processInstances: [
        {...instance, id: '1'},
        {...instance, id: '2'},
        {...instance, id: '3'},
      ],
    });

    await processInstancesStore.fetchInstances({
      fetchType: 'initial',
      payload: {query: {}},
    });

    processInstancesStore.markProcessInstancesWithActiveOperations({
      ids: ['1', '2'],
      operationType: 'CANCEL_PROCESS_INSTANCE',
    });

    expect(processInstancesStore.state.processInstances).toEqual([
      {
        ...instance,
        id: '1',
        hasActiveOperation: true,
        operations: [cancelOperation],
      },
      {
        ...instance,
        id: '2',
        hasActiveOperation: true,
        operations: [cancelOperation],
      },
      {...instance, id: '3'},
    ]);
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1', '2']);

    processInstancesStore.markProcessInstancesWithActiveOperations({
      ids: ['non_existing_instance_id'],
      operationType: 'CANCEL_PROCESS_INSTANCE',
    });
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1', '2']);
    expect(processInstancesStore.state.processInstances).toEqual([
      {
        ...instance,
        id: '1',
        hasActiveOperation: true,
        operations: [cancelOperation],
      },
      {
        ...instance,
        id: '2',
        hasActiveOperation: true,
        operations: [cancelOperation],
      },
      {...instance, id: '3'},
    ]);

    processInstancesStore.markProcessInstancesWithActiveOperations({
      ids: [],
      shouldPollAllVisibleIds: true,
      operationType: 'CANCEL_PROCESS_INSTANCE',
    });
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1', '2', '3']);

    expect(processInstancesStore.state.processInstances).toEqual([
      {
        ...instance,
        id: '1',
        hasActiveOperation: true,
        operations: [cancelOperation, cancelOperation],
      },
      {
        ...instance,
        id: '2',
        hasActiveOperation: true,
        operations: [cancelOperation, cancelOperation],
      },
      {
        ...instance,
        id: '3',
        hasActiveOperation: true,
        operations: [cancelOperation],
      },
    ]);

    mockFetchProcessInstances().withSuccess({
      totalCount: 100,
      processInstances: [
        {...instance, id: '1'},
        {...instance, id: '2'},
        {...instance, id: '3'},
      ],
    });

    await processInstancesStore.fetchInstances({
      fetchType: 'initial',
      payload: {query: {}},
    });
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual([]);

    expect(processInstancesStore.state.processInstances).toEqual([
      {...instance, id: '1'},
      {...instance, id: '2'},
      {...instance, id: '3'},
    ]);

    processInstancesStore.markProcessInstancesWithActiveOperations({
      ids: ['2'],
      shouldPollAllVisibleIds: true,
      operationType: 'CANCEL_PROCESS_INSTANCE',
    });
    expect(processInstancesStore.state.processInstances).toEqual([
      {
        ...instance,
        id: '1',
        hasActiveOperation: true,
        operations: [cancelOperation],
      },
      {
        ...instance,
        id: '2',
      },
      {
        ...instance,
        id: '3',
        hasActiveOperation: true,
        operations: [cancelOperation],
      },
    ]);
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1', '3']);

    processInstancesStore.markProcessInstancesWithActiveOperations({
      ids: ['non_existing_instance_id'],
      shouldPollAllVisibleIds: true,
      operationType: 'CANCEL_PROCESS_INSTANCE',
    });
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1', '2', '3']);

    expect(processInstancesStore.state.processInstances).toEqual([
      {
        ...instance,
        id: '1',
        hasActiveOperation: true,
        operations: [cancelOperation, cancelOperation],
      },
      {
        ...instance,
        id: '2',
        hasActiveOperation: true,
        operations: [cancelOperation],
      },
      {
        ...instance,
        id: '3',
        hasActiveOperation: true,
        operations: [cancelOperation, cancelOperation],
      },
    ]);
  });

  it('should unmark instances with active operations', async () => {
    // when polling all visible instances
    mockFetchProcessInstances().withSuccess({
      totalCount: 100,
      processInstances: [
        {...instanceWithActiveOperation, id: '1'},
        {...instanceWithActiveOperation, id: '2'},
        {...instanceWithActiveOperation, id: '3'},
      ],
    });

    processInstancesStore.fetchProcessInstancesFromFilters();

    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1', '2', '3']);

    // mock for refresh all instances
    mockFetchProcessInstances().withSuccess({
      totalCount: 100,
      processInstances: [
        {...instance, id: '1'},
        {...instance, id: '2'},
        {...instance, id: '3'},
      ],
    });
    // mock for refresh running process instances count
    mockFetchProcessInstances().withSuccess({
      totalCount: 100,
      processInstances: [
        {...instance, id: '1'},
        {...instance, id: '2'},
        {...instance, id: '3'},
      ],
    });

    processInstancesStore.unmarkProcessInstancesWithActiveOperations({
      instanceIds: ['1', '2', '3'],
      shouldPollAllVisibleIds: true,
      operationType: 'CANCEL_PROCESS_INSTANCE',
    });

    await waitFor(() =>
      expect(
        processInstancesStore.processInstanceIdsWithActiveOperations,
      ).toEqual([]),
    );

    // when not polling all visible instances
    mockFetchProcessInstances().withSuccess({
      totalCount: 100,
      processInstances: [
        {...instanceWithActiveOperation, id: '1'},
        {...instanceWithActiveOperation, id: '2'},
        {...instanceWithActiveOperation, id: '3'},
      ],
    });

    processInstancesStore.fetchProcessInstancesFromFilters();
    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1', '2', '3']);

    processInstancesStore.unmarkProcessInstancesWithActiveOperations({
      instanceIds: ['3'],
      shouldPollAllVisibleIds: false,
      operationType: 'CANCEL_PROCESS_INSTANCE',
    });
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1', '2']);
  });

  it('should not set active operation state to false if there are still running operations', async () => {
    mockFetchProcessInstances().withSuccess({
      totalCount: 100,
      processInstances: [
        {
          ...instanceWithActiveOperation,
          id: '1',
          operations: [
            {
              errorMessage: 'string',
              state: 'SENT',
              type: 'RESOLVE_INCIDENT',
            },
            {
              errorMessage: 'string',
              state: 'SENT',
              type: 'CANCEL_PROCESS_INSTANCE',
            },
          ],
        },
      ],
    });

    processInstancesStore.fetchProcessInstancesFromFilters();

    await waitFor(() =>
      expect(processInstancesStore.state.status).toBe('fetched'),
    );
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1']);

    processInstancesStore.unmarkProcessInstancesWithActiveOperations({
      instanceIds: ['1'],
      shouldPollAllVisibleIds: false,
      operationType: 'RESOLVE_INCIDENT',
    });

    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['1']);
    expect(
      processInstancesStore.state.processInstances[0]?.hasActiveOperation,
    ).toBe(true);
  });

  it('should refresh instances and and call handlers every time there is an instance with completed operation', async () => {
    jest.useFakeTimers();
    const handlerMock = jest.fn();
    processInstancesStore.addCompletedOperationsHandler(handlerMock);

    mockFetchProcessInstances().withSuccess(mockProcessInstances);

    processInstancesStore.init();
    processInstancesStore.fetchProcessInstancesFromFilters();

    await waitFor(() =>
      expect(processInstancesStore.state.processInstances).toHaveLength(2),
    );
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['2251799813685627']);

    mockServer.use(
      rest.post('/api/process-instances', (_, res, ctx) =>
        res.once(
          ctx.json({
            processInstances: [
              instance,
              {...instanceWithActiveOperation, hasActiveOperation: false},
            ],
            totalCount: 2,
          }),
        ),
      ),
      // mock for refresh all instances
      rest.post('/api/process-instances', (_, res, ctx) =>
        res.once(
          ctx.json({
            processInstances: [
              instance,
              {...instanceWithActiveOperation, hasActiveOperation: false},
            ],
            totalCount: 3,
          }),
        ),
      ),
      // mock for refresh running process instances count
      rest.post('/api/process-instances', (_, res, ctx) =>
        res.once(
          ctx.json({
            processInstances: [
              instance,
              {...instanceWithActiveOperation, hasActiveOperation: false},
            ],
            totalCount: 3,
          }),
        ),
      ),
    );

    jest.runOnlyPendingTimers();

    await waitFor(() =>
      expect(processInstancesStore.state.filteredProcessInstancesCount).toBe(3),
    );

    expect(handlerMock).toHaveBeenCalledTimes(1);

    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('should poll instances by id when there are instances with active operations', async () => {
    mockFetchProcessInstances().withSuccess(
      {
        totalCount: 100,
        processInstances: [
          {...instance, id: '1'},
          {...instanceWithActiveOperation, id: '2'},
        ],
      },
      {expectPolling: false},
    );

    jest.useFakeTimers();

    processInstancesStore.init();
    processInstancesStore.fetchProcessInstancesFromFilters();

    await waitFor(() =>
      expect(processInstancesStore.state.processInstances).toHaveLength(2),
    );
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual(['2']);

    mockServer.use(
      rest.post('/api/process-instances', (req, res, ctx) => {
        checkPollingHeader({req, expectPolling: true});
        return res.once(
          ctx.json({
            processInstances: [
              {
                ...instanceWithActiveOperation,
                hasActiveOperation: false,
                id: '2',
              },
            ],
            totalCount: 100,
          }),
        );
      }),
      // mock for refreshing instances when an instance operation is complete
      rest.post('/api/process-instances', (req, res, ctx) => {
        checkPollingHeader({req, expectPolling: true});
        return res.once(
          ctx.json({
            processInstances: [
              {...instance, id: '1'},
              {
                ...instanceWithActiveOperation,
                id: '2',
                hasActiveOperation: false,
              },
            ],
            totalCount: 2,
          }),
        );
      }),
      // mock for refresh running process instances count
      rest.post('/api/process-instances', (req, res, ctx) => {
        checkPollingHeader({req, expectPolling: true});
        return res.once(
          ctx.json({
            processInstances: [
              {...instance, id: '1'},
              {
                ...instanceWithActiveOperation,
                id: '2',
                hasActiveOperation: false,
              },
            ],
            totalCount: 2,
          }),
        );
      }),
    );

    jest.runOnlyPendingTimers();

    await waitFor(() => {
      expect(processInstancesStore.state.filteredProcessInstancesCount).toEqual(
        2,
      );
    });
    expect(
      processInstancesStore.processInstanceIdsWithActiveOperations,
    ).toEqual([]);

    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('should retry fetch on network reconnection', async () => {
    const eventListeners: any = {};
    const originalEventListener = window.addEventListener;
    window.addEventListener = jest.fn((event: string, cb: any) => {
      eventListeners[event] = cb;
    });

    const mockedProcessInstances = {
      processInstances: [instance],
      totalCount: 1,
    };

    mockFetchProcessInstances().withSuccess(mockedProcessInstances, {
      expectPolling: false,
    });

    processInstancesStore.init();
    processInstancesStore.fetchProcessInstancesFromFilters();

    await waitFor(() =>
      expect(processInstancesStore.state.processInstances).toEqual(
        mockedProcessInstances.processInstances,
      ),
    );

    const newProcessInstancesResponse = {
      processInstances: [instance, {...instance, id: '123'}],
      totalCount: 2,
    };

    mockFetchProcessInstances().withSuccess(newProcessInstancesResponse, {
      expectPolling: false,
    });

    eventListeners.online();

    await waitFor(() =>
      expect(processInstancesStore.state.processInstances).toEqual(
        newProcessInstancesResponse.processInstances,
      ),
    );

    window.addEventListener = originalEventListener;
  });
});
