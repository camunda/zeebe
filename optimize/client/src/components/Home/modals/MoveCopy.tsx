/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {useEffect, useState} from 'react';
import {ComboBox, InlineNotification, Toggle} from '@carbon/react';

import {showError} from 'notifications';
import {loadEntities} from 'services';
import {t} from 'translation';
import {useErrorHandling} from 'hooks';
import {EntityListEntity} from 'types';

interface MoveCopyProps {
  parentCollection: string;
  entity: Partial<EntityListEntity<{subEntityCounts: {report: number}}>>;
  moving: boolean;
  collection?: Partial<EntityListEntity> | null;
  setMoving: (moving: boolean) => void;
  setCollection: (collection: Partial<EntityListEntity> | null) => void;
}

export default function MoveCopy({
  parentCollection,
  entity,
  moving,
  setMoving,
  setCollection,
}: MoveCopyProps) {
  const {mightFail} = useErrorHandling();
  const [availableCollections, setAvailableCollections] = useState<Partial<EntityListEntity>[]>([]);

  useEffect(() => {
    mightFail(
      loadEntities(),
      (entities) =>
        setAvailableCollections(
          [
            {id: null, entityType: 'collection', name: t('navigation.collections').toString()},
            ...entities,
          ].filter(({entityType, id}) => entityType === 'collection' && id !== parentCollection)
        ),
      showError
    );
  }, [mightFail, parentCollection]);

  const getMulticopyText = () => {
    const {entityType, data} = entity;
    const containedReports = data?.subEntityCounts.report;

    if (!containedReports) {
      return undefined;
    }

    const params = {
      entityType: entityType === 'dashboard' ? t('dashboard.label') : t('home.types.combined'),
      number: containedReports,
    };
    if (containedReports > 1) {
      return t('home.copy.subEntities', params).toString();
    }
    return t('home.copy.subEntity', params).toString();
  };

  const getPlaceholder = () =>
    (availableCollections?.length
      ? t('home.copy.pleaseSelect')
      : t('home.copy.noCollections')
    ).toString();

  const multiTextCopy = getMulticopyText();

  return (
    <>
      <Toggle
        size="sm"
        id="moveToggle"
        labelText={t('home.copy.moveLabel').toString()}
        hideLabel
        toggled={moving}
        onToggle={(checked) => setMoving(checked)}
      />
      {moving && (
        <>
          <ComboBox<Partial<EntityListEntity>>
            id="collectionSelection"
            items={availableCollections}
            itemToString={(collection) => (collection as EntityListEntity)?.name}
            onChange={({selectedItem}) => {
              const collection =
                availableCollections.find((col) => col.id === selectedItem?.id) || null;
              setCollection(collection);
            }}
            placeholder={getPlaceholder()}
            disabled={!availableCollections.length}
          />
          {multiTextCopy && (
            <InlineNotification kind="info" hideCloseButton subtitle={getMulticopyText()} />
          )}
        </>
      )}
    </>
  );
}
