/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {ChangeEvent, useEffect, useState} from 'react';
import {
  Button,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  TextInput,
} from '@carbon/react';

import {Modal, NoDataNotice, Table} from 'components';
import {loadVariables} from 'services';
import {t} from 'translation';
import {showError} from 'notifications';
import {useErrorHandling} from 'hooks';
import {ProcessFilter, Variable} from 'types';

import {updateVariables} from './service';

import './RenameVariablesModal.scss';

interface RenameVariablesModalProps {
  open?: boolean;
  onClose: () => void;
  onChange: () => void;
  definitionKey: string;
  availableTenants: (string | null)[];
  filters?: ProcessFilter[];
}

export default function RenameVariablesModal({
  open,
  onClose,
  onChange,
  definitionKey,
  availableTenants,
  filters = [],
}: RenameVariablesModalProps) {
  const [variables, setVariables] = useState<Variable[]>();
  const [query, setQuery] = useState('');
  const [renamedVariables, setRenamedVariables] = useState(new Map());
  const {mightFail} = useErrorHandling();

  useEffect(() => {
    mightFail(
      loadVariables({
        processesToQuery: [
          {
            processDefinitionKey: definitionKey,
            processDefinitionVersions: ['all'],
            tenantIds: availableTenants,
          },
        ],
        filter: filters,
      }),
      (variables) => {
        setVariables(variables);
        setRenamedVariables(
          new Map(
            variables
              .filter((variable) => !!variable.label)
              .map((variable) => [
                variable,
                {
                  variableName: variable.name,
                  variableType: variable.type,
                  variableLabel: variable.label,
                },
              ])
          )
        );
      },
      showError
    );
  }, [availableTenants, definitionKey, filters, mightFail]);

  function updateVariableNames() {
    mightFail(
      updateVariables(definitionKey, Array.from(renamedVariables.values())),
      () => {
        onChange();
        onClose();
      },
      showError
    );
  }

  const filteredVariables =
    variables?.filter(({name}) => name.toLowerCase().includes(query.toLowerCase())) || [];

  return (
    <Modal open={open} size="lg" onClose={onClose} className="RenameVariablesModal">
      <Modal.Header title={t('report.definition.variables.rename')} />
      <Modal.Content>
        <div className="header">
          <div className="info">
            {t('report.definition.variables.renameInfo')}
            <p>
              <b>{t('report.definition.variables.important')} </b>
              {t('report.definition.variables.followGuidelines')}
            </p>
            <ul className="guidelines">
              <li>{t('report.definition.variables.globalChanges')}</li>
              <li>{t('report.definition.variables.useSameVariable')}</li>
            </ul>
          </div>
        </div>
        <Table
          toolbar={
            <TableToolbar>
              <TableToolbarContent>
                <TableToolbarSearch
                  placeholder={t('report.groupBy.searchForVariable').toString()}
                  onChange={(evt) => {
                    setQuery((evt as ChangeEvent<HTMLInputElement>).target.value);
                  }}
                  onClear={() => {
                    setQuery('');
                  }}
                />
              </TableToolbarContent>
            </TableToolbar>
          }
          head={[
            {label: t('report.definition.variables.variableName'), id: 'name'},
            {label: t('report.definition.variables.type'), id: 'type'},
            {label: t('report.definition.variables.newName'), id: 'newName', sortable: false},
          ]}
          body={filteredVariables.map((variable) => [
            variable.name,
            variable.type,
            <TextInput
              id={`${variable.name}-${variable.type}-input`}
              className="nameInput"
              size="sm"
              labelText={t('report.definition.variables.newName')}
              hideLabel
              value={renamedVariables.get(variable)?.variableLabel || ''}
              onChange={(evt) =>
                setRenamedVariables(
                  (renamedVariables) =>
                    new Map(
                      renamedVariables.set(variable, {
                        variableName: variable.name,
                        variableType: variable.type,
                        variableLabel: evt.target.value,
                      })
                    )
                )
              }
            />,
          ])}
          loading={!variables}
          noData={
            !!variables?.length && filteredVariables.length === 0 ? (
              <NoDataNotice title={t('events.table.noResults')} />
            ) : undefined
          }
          allowLocalSorting
        />
      </Modal.Content>
      <Modal.Footer>
        <Button kind="secondary" className="cancel" onClick={onClose}>
          {t('common.close')}
        </Button>
        <Button className="confirm" onClick={updateVariableNames}>
          {t('common.update')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
