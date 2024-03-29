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

import {observer} from 'mobx-react';
import {useLocation} from 'react-router-dom';
import {Link, OrderedList, Stack, TableBatchAction} from '@carbon/react';
import {MigrateAlt} from '@carbon/react/icons';
import {Restricted} from 'modules/components/Restricted';
import {processInstancesSelectionStore} from 'modules/stores/processInstancesSelection';
import {processInstancesStore} from 'modules/stores/processInstances';
import {getProcessInstanceFilters} from 'modules/utils/filter/getProcessInstanceFilters';
import {processInstanceMigrationStore} from 'modules/stores/processInstanceMigration';
import {processXmlStore as processXmlMigrationSourceStore} from 'modules/stores/processXml/processXml.migration.source';
import {processXmlStore} from 'modules/stores/processXml/processXml.list';
import {processesStore} from 'modules/stores/processes/processes.list';
import {ModalStateManager} from 'modules/components/ModalStateManager';
import {processStatisticsStore as processStatisticsMigrationSourceStore} from 'modules/stores/processStatistics/processStatistics.migration.source';
import {getProcessInstancesRequestFilters} from 'modules/utils/filter';
import {ListItem, Modal} from './styled';
import isNil from 'lodash/isNil';
import {tracking} from 'modules/tracking';
import {batchModificationStore} from 'modules/stores/batchModification';

const MigrateAction: React.FC = observer(() => {
  const location = useLocation();
  const {version, process, tenant} = getProcessInstanceFilters(location.search);
  const {
    selectedProcessInstanceIds,
    hasSelectedRunningInstances,
    state: {isAllChecked},
  } = processInstancesSelectionStore;

  const isVersionSelected = version !== undefined && version !== 'all';

  const isChildProcess = (() => {
    if (processInstancesSelectionStore.state.isAllChecked) {
      return processInstancesStore.state.processInstances.some(
        ({parentInstanceId}) => parentInstanceId !== null,
      );
    }

    return processInstancesSelectionStore.state.selectedProcessInstanceIds.some(
      (processInstanceId) => {
        const instance = processInstancesStore.state.processInstances.find(
          ({id}) => {
            return id === processInstanceId;
          },
        );
        return !isNil(instance?.parentInstanceId);
      },
    );
  })();

  const hasXmlError = processXmlStore.state.status === 'error';

  const isDisabled =
    batchModificationStore.state.isEnabled ||
    !isVersionSelected ||
    !hasSelectedRunningInstances ||
    isChildProcess ||
    hasXmlError;

  const getTooltipText = () => {
    if (!isVersionSelected) {
      return 'To start the migration process, choose a process and version first.';
    }

    if (hasXmlError) {
      return 'Issue fetching diagram, contact admin if problem persists.';
    }

    if (!hasSelectedRunningInstances) {
      return 'You can only migrate instances in active or incident state.';
    }

    if (isChildProcess) {
      return 'You can only migrate instances which are not called by a parent process';
    }
  };

  return (
    <Restricted
      scopes={['write']}
      resourceBasedRestrictions={{
        scopes: ['UPDATE_PROCESS_INSTANCE'],
        permissions: processesStore.getPermissions(process, tenant),
      }}
    >
      <ModalStateManager
        renderLauncher={({setOpen}) => (
          <TableBatchAction
            renderIcon={MigrateAlt}
            onClick={() => {
              setOpen(true);
              tracking.track({
                eventName: 'process-instance-migration-button-clicked',
              });
            }}
            disabled={isDisabled}
            title={
              batchModificationStore.state.isEnabled
                ? 'Not available in batch modification mode'
                : getTooltipText()
            }
          >
            Migrate
          </TableBatchAction>
        )}
      >
        {({open, setOpen}) => (
          <Modal
            open={open}
            preventCloseOnClickOutside
            modalHeading="Migrate process instance versions"
            primaryButtonText="Continue"
            secondaryButtonText="Cancel"
            onRequestSubmit={() => {
              processXmlMigrationSourceStore.setProcessXml(
                processXmlStore.state.xml,
              );

              const requestFilterParameters = {
                ...getProcessInstancesRequestFilters(),
                ids: isAllChecked ? [] : selectedProcessInstanceIds,
                excludeIds: isAllChecked ? selectedProcessInstanceIds : [],
              };

              processStatisticsMigrationSourceStore.fetchProcessStatistics(
                requestFilterParameters,
              );
              processInstanceMigrationStore.setSelectedInstancesCount(
                processInstancesSelectionStore.selectedProcessInstanceCount,
              );
              processInstanceMigrationStore.setBatchOperationQuery(
                requestFilterParameters,
              );
              processInstanceMigrationStore.enable();

              tracking.track({
                eventName: 'process-instance-migration-mode-entered',
              });
            }}
            onRequestClose={() => setOpen(false)}
            onSecondarySubmit={() => setOpen(false)}
            size="md"
          >
            <Stack as={OrderedList} nested gap={5}>
              <ListItem>
                Migrate is used to move a process to a newer (or older) version
                of the process.
              </ListItem>
              <ListItem>
                When the migration steps are executed, all process instances are
                affected. This can lead to interruptions, delays, or changes.
              </ListItem>
              <ListItem>
                To minimize interruptions or delays, plan the migration at times
                when the system load is low.
              </ListItem>
            </Stack>
            <p>
              Questions or concerns? Check our{' '}
              <Link
                href="https://docs.camunda.io/docs/components/operate/userguide/process-instance-migration/"
                target="_blank"
                inline
              >
                migration documentation
              </Link>{' '}
              for guidance and best practices.
            </p>
          </Modal>
        )}
      </ModalStateManager>
    </Restricted>
  );
});

export {MigrateAction};
