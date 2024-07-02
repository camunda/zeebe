/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {useEffect, useState} from 'react';
import {withRouter} from 'react-router-dom';
import classnames from 'classnames';
import useDeepCompareEffect from 'use-deep-compare-effect';
import deepEqual from 'fast-deep-equal';
import {Button} from '@carbon/react';
import {Close, Copy, Edit} from '@carbon/icons-react';

import {Popover, Tooltip} from 'components';
import {withDocs, withErrorHandling} from 'HOC';
import {getCollection, formatters} from 'services';
import {t} from 'translation';
import {showError} from 'notifications';
import {getOptimizeProfile, areTenantsAvailable, getMaxNumDataSourcesForReport} from 'config';

import {loadTenants} from './service';
import DefinitionEditor from './DefinitionEditor';

import './DefinitionList.scss';
import {useDocs, useErrorHandling} from 'hooks';

const {formatVersions, formatTenants} = formatters;

export function DefinitionList({
  location,
  definitions = [],
  type,
  onChange,
  onRemove,
  onCopy,
  filters,
}) {
  const [openPopover, setOpenPopover] = useState();
  const [tenantInfo, setTenantInfo] = useState();
  const [optimizeProfile, setOptimizeProfile] = useState();
  const [tenantsAvailable, setTenantsAvailable] = useState(false);
  const [reportDataSourceLimit, setReportDataSourceLimit] = useState(100);
  const {mightFail} = useErrorHandling();
  const {generateDocsLink} = useDocs();

  const collection = getCollection(location.pathname);
  const definitionKeysAndVersions = definitions.map(({key, versions}) => ({key, versions}));
  const isDefinitionLimitReached = definitions.length >= reportDataSourceLimit;

  useDeepCompareEffect(() => {
    mightFail(loadTenants(type, definitionKeysAndVersions, collection), setTenantInfo, showError);
  }, [definitionKeysAndVersions, collection, mightFail, type]);

  useEffect(() => {
    (async () => {
      setOptimizeProfile(await getOptimizeProfile());
      setTenantsAvailable(await areTenantsAvailable());
      setReportDataSourceLimit(await getMaxNumDataSourcesForReport());
    })();
  }, []);

  function getTenantInfoForDefinition(definition) {
    return tenantInfo?.find(
      ({key, versions}) => key === definition.key && deepEqual(versions, definition.versions)
    )?.tenants;
  }

  if (definitions.length === 0) {
    return <div className="DefinitionList empty">{t('report.noSources')}</div>;
  }

  return (
    <ul className="DefinitionList">
      {definitions.map((definition, idx) => {
        const tenantInfo = getTenantInfoForDefinition(definition);

        const showOnlyTenant =
          tenantsAvailable && tenantInfo?.length === 1 && optimizeProfile === 'ccsm';

        return (
          <li key={idx + definition.key} className={classnames({active: openPopover === idx})}>
            <h4>{definition.displayName || definition.name || definition.key}</h4>
            <div className="info">
              {t('common.definitionSelection.version.label')}: {formatVersions(definition.versions)}
            </div>
            {(tenantInfo?.length > 1 || showOnlyTenant) && (
              <div className="info">
                {t('common.tenant.label')}:{' '}
                {formatTenants(definition.tenantIds, tenantInfo, showOnlyTenant)}
              </div>
            )}
            <div className="actions">
              {!isDefinitionLimitReached && (
                <Tooltip
                  content={t('report.copyTooltip', {
                    entity: t('common.process.label'),
                    docsLink: generateDocsLink(
                      'components/userguide/additional-features/process-variants-comparison/'
                    ),
                  })}
                  position="bottom"
                >
                  <Button
                    kind="ghost"
                    size="sm"
                    className="actionBtn"
                    onClick={() => onCopy(idx)}
                    iconDescription={t('common.copy')}
                    renderIcon={Copy}
                    hasIconOnly
                  />
                </Tooltip>
              )}
              <Popover
                className="DefinitionList"
                onOpen={() => setOpenPopover(idx)}
                onClose={() => setOpenPopover()}
                floating
                trigger={
                  <Popover.Button
                    size="sm"
                    className="actionBtn"
                    iconDescription={t('common.edit')}
                    renderIcon={Edit}
                    hasIconOnly
                  />
                }
              >
                <DefinitionEditor
                  filters={filters}
                  collection={collection}
                  definition={definition}
                  tenantInfo={tenantInfo}
                  onChange={(change) => onChange(change, idx)}
                  type={type}
                />
              </Popover>
              <Button
                kind="ghost"
                size="sm"
                className="actionBtn"
                iconDescription={t('common.remove')}
                onClick={() => onRemove(idx)}
                renderIcon={Close}
                hasIconOnly
              />
            </div>
          </li>
        );
      })}
    </ul>
  );
}

export default withRouter(withErrorHandling(withDocs(DefinitionList)));
