/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

/* istanbul ignore file */

import {MemoryRouter} from 'react-router-dom';
import {Story} from '@storybook/react';

import {InstancesByProcess as InstancesByProcessComponent} from './index';
import {rest} from 'msw';
import styled from 'styled-components';

export default {
  title: 'Components/InstancesByProcess',
};

const Container = styled.div`
  width: 638px;
  height: 388px;
  overflow: scroll;
  background-color: #fdfdfe;
`;

const InstancesByProcess: Story = () => {
  return (
    <MemoryRouter>
      <Container>
        <InstancesByProcessComponent />
      </Container>
    </MemoryRouter>
  );
};

InstancesByProcess.parameters = {
  msw: [
    rest.get('/api/incidents/byProcess', (_, res, ctx) => {
      return res(
        ctx.json([
          {
            bpmnProcessId: 'complexProcess',
            processName: null,
            instancesWithActiveIncidentsCount: 164,
            activeInstancesCount: 28,
            processes: [
              {
                processId: '2251799813689530',
                version: 3,
                name: null,
                bpmnProcessId: 'complexProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 106,
                activeInstancesCount: 0,
              },
              {
                processId: '2251799813686021',
                version: 1,
                name: null,
                bpmnProcessId: 'complexProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 32,
                activeInstancesCount: 17,
              },
              {
                processId: '2251799813687833',
                version: 2,
                name: null,
                bpmnProcessId: 'complexProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 26,
                activeInstancesCount: 11,
              },
            ],
          },
          {
            bpmnProcessId: 'flightRegistration',
            processName: 'Flight registration',
            instancesWithActiveIncidentsCount: 147,
            activeInstancesCount: 3,
            processes: [
              {
                processId: '2251799813687829',
                version: 2,
                name: 'Flight registration',
                bpmnProcessId: 'flightRegistration',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 75,
                activeInstancesCount: 3,
              },
              {
                processId: '2251799813686017',
                version: 1,
                name: 'Flight registration',
                bpmnProcessId: 'flightRegistration',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 72,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'orderProcess',
            processName: 'Order process',
            instancesWithActiveIncidentsCount: 116,
            activeInstancesCount: 6,
            processes: [
              {
                processId: '2251799813686009',
                version: 1,
                name: 'Order process',
                bpmnProcessId: 'orderProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 60,
                activeInstancesCount: 0,
              },
              {
                processId: '2251799813687827',
                version: 2,
                name: 'Order process',
                bpmnProcessId: 'orderProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 56,
                activeInstancesCount: 6,
              },
            ],
          },
          {
            bpmnProcessId: 'loanProcess',
            processName: null,
            instancesWithActiveIncidentsCount: 79,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813686014',
                version: 1,
                name: null,
                bpmnProcessId: 'loanProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 79,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'multiInstanceProcess',
            processName: 'Multi-Instance Process',
            instancesWithActiveIncidentsCount: 76,
            activeInstancesCount: 91,
            processes: [
              {
                processId: '2251799813687831',
                version: 2,
                name: 'Multi-Instance Process',
                bpmnProcessId: 'multiInstanceProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 76,
                activeInstancesCount: 0,
              },
              {
                processId: '2251799813686019',
                version: 1,
                name: 'Sequential Multi-Instance Process',
                bpmnProcessId: 'multiInstanceProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 0,
                activeInstancesCount: 91,
              },
            ],
          },
          {
            bpmnProcessId: 'call-activity-process',
            processName: 'Call Activity Process',
            instancesWithActiveIncidentsCount: 54,
            activeInstancesCount: 37,
            processes: [
              {
                processId: '2251799813686035',
                version: 1,
                name: 'Call Activity Process',
                bpmnProcessId: 'call-activity-process',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 54,
                activeInstancesCount: 37,
              },
            ],
          },
          {
            bpmnProcessId: 'error-end-process',
            processName: null,
            instancesWithActiveIncidentsCount: 54,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813686043',
                version: 1,
                name: null,
                bpmnProcessId: 'error-end-process',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 54,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'prWithSubprocess',
            processName: 'Nested subprocesses',
            instancesWithActiveIncidentsCount: 51,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813686026',
                version: 1,
                name: 'Nested subprocesses',
                bpmnProcessId: 'prWithSubprocess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 51,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'errorProcess',
            processName: 'Error Process',
            instancesWithActiveIncidentsCount: 48,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813686041',
                version: 1,
                name: 'Error Process',
                bpmnProcessId: 'errorProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 48,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'eventBasedGatewayProcess',
            processName: 'Event based gateway with timer start',
            instancesWithActiveIncidentsCount: 26,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813687835',
                version: 2,
                name: 'Event based gateway with timer start',
                bpmnProcessId: 'eventBasedGatewayProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 20,
                activeInstancesCount: 0,
              },
              {
                processId: '2251799813686023',
                version: 1,
                name: 'Event based gateway with message start',
                bpmnProcessId: 'eventBasedGatewayProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 6,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'nonInterruptingBoundaryEvent',
            processName: null,
            instancesWithActiveIncidentsCount: 25,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813687840',
                version: 2,
                name: null,
                bpmnProcessId: 'nonInterruptingBoundaryEvent',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 22,
                activeInstancesCount: 0,
              },
              {
                processId: '2251799813686030',
                version: 1,
                name: null,
                bpmnProcessId: 'nonInterruptingBoundaryEvent',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 3,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'called-process',
            processName: 'Called Process',
            instancesWithActiveIncidentsCount: 16,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813687842',
                version: 1,
                name: 'Called Process',
                bpmnProcessId: 'called-process',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 16,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'eventSubprocessProcess',
            processName: 'Event Subprocess Process',
            instancesWithActiveIncidentsCount: 15,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813686037',
                version: 1,
                name: 'Event Subprocess Process',
                bpmnProcessId: 'eventSubprocessProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 15,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'onlyIncidentsProcess',
            processName: 'Only Incidents Process',
            instancesWithActiveIncidentsCount: 13,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813685253',
                version: 1,
                name: 'Only Incidents Process',
                bpmnProcessId: 'onlyIncidentsProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 7,
                activeInstancesCount: 0,
              },
              {
                processId: '2251799813685276',
                version: 2,
                name: 'Only Incidents Process',
                bpmnProcessId: 'onlyIncidentsProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 6,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'interruptingBoundaryEvent',
            processName: null,
            instancesWithActiveIncidentsCount: 8,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813687838',
                version: 2,
                name: null,
                bpmnProcessId: 'interruptingBoundaryEvent',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 8,
                activeInstancesCount: 0,
              },
              {
                processId: '2251799813686028',
                version: 1,
                name: null,
                bpmnProcessId: 'interruptingBoundaryEvent',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 0,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'bigProcess',
            processName: null,
            instancesWithActiveIncidentsCount: 1,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813686039',
                version: 1,
                name: null,
                bpmnProcessId: 'bigProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 1,
                activeInstancesCount: 0,
              },
            ],
          },
          {
            bpmnProcessId: 'withoutIncidentsProcess',
            processName: 'Without Incidents Process',
            instancesWithActiveIncidentsCount: 0,
            activeInstancesCount: 12,
            processes: [
              {
                processId: '2251799813685294',
                version: 1,
                name: 'Without Incidents Process',
                bpmnProcessId: 'withoutIncidentsProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 0,
                activeInstancesCount: 6,
              },
              {
                processId: '2251799813685307',
                version: 2,
                name: 'Without Incidents Process',
                bpmnProcessId: 'withoutIncidentsProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 0,
                activeInstancesCount: 6,
              },
            ],
          },
          {
            bpmnProcessId: 'bigVarProcess',
            processName: 'Big variable process',
            instancesWithActiveIncidentsCount: 0,
            activeInstancesCount: 2,
            processes: [
              {
                processId: '2251799813685329',
                version: 1,
                name: 'Big variable process',
                bpmnProcessId: 'bigVarProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 0,
                activeInstancesCount: 2,
              },
            ],
          },
          {
            bpmnProcessId: 'timerProcess',
            processName: 'Timer process',
            instancesWithActiveIncidentsCount: 0,
            activeInstancesCount: 2,
            processes: [
              {
                processId: '2251799813686032',
                version: 1,
                name: 'Timer process',
                bpmnProcessId: 'timerProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 0,
                activeInstancesCount: 2,
              },
            ],
          },
          {
            bpmnProcessId: 'noInstancesProcess',
            processName: 'Without Instances Process',
            instancesWithActiveIncidentsCount: 0,
            activeInstancesCount: 0,
            processes: [
              {
                processId: '2251799813685249',
                version: 1,
                name: 'Without Instances Process',
                bpmnProcessId: 'noInstancesProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 0,
                activeInstancesCount: 0,
              },
              {
                processId: '2251799813685251',
                version: 2,
                name: 'Without Instances Process',
                bpmnProcessId: 'noInstancesProcess',
                errorMessage: null,
                instancesWithActiveIncidentsCount: 0,
                activeInstancesCount: 0,
              },
            ],
          },
        ])
      );
    }),
  ],
};

InstancesByProcess.storyName = 'InstancesByProcess - Success';

const InstancesByProcessLoading: Story = () => {
  return (
    <MemoryRouter>
      <Container>
        <InstancesByProcessComponent />
      </Container>
    </MemoryRouter>
  );
};

InstancesByProcessLoading.storyName = 'InstancesByProcess - Loading';
InstancesByProcessLoading.parameters = {
  msw: [
    rest.get('/api/incidents/byProcess', (_, res, ctx) => {
      return res(ctx.delay('infinite'), ctx.json({}));
    }),
  ],
};
const InstancesByProcessFailure: Story = () => {
  return (
    <MemoryRouter>
      <Container>
        <InstancesByProcessComponent />
      </Container>
    </MemoryRouter>
  );
};

InstancesByProcessFailure.storyName = 'InstancesByProcess - Failure';
InstancesByProcessFailure.parameters = {
  msw: [
    rest.get('/api/incidents/byProcess', (_, res, ctx) => {
      return res(ctx.status(500), ctx.json({}));
    }),
  ],
};

export {
  InstancesByProcess,
  InstancesByProcessLoading,
  InstancesByProcessFailure,
};
