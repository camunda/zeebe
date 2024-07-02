/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Component, createRef} from 'react';
import {Redirect} from 'react-router-dom';
import {parseISO} from 'date-fns';
import {
  Button,
  Column,
  Grid,
  OverflowMenu,
  OverflowMenuItem,
  Stack,
  Tag,
  TagSkeleton,
  SkeletonText,
  SkeletonIcon,
} from '@carbon/react';
import {DocumentProtected, CopyFile, Edit, Folder, Save, TrashCan} from '@carbon/icons-react';

import {format} from 'dates';
import {t} from 'translation';
import {withErrorHandling} from 'HOC';
import {
  Deleter,
  BulkDeleter,
  Tooltip,
  ReportTemplateModal,
  DashboardTemplateModal,
  PageTitle,
  Tabs,
  KpiCreationModal,
  EntityList,
  EmptyState,
  Icon,
} from 'components';
import {formatters, loadEntity, updateEntity, checkDeleteConflict, getEntityIcon} from 'services';
import {showError, addNotification} from 'notifications';
import {isUserSearchAvailable} from 'config';

import {loadCollectionEntities, importEntity, removeEntities, checkConflicts} from './service';
import {refreshBreadcrumbs} from 'components/navigation';
import Copier from './Copier';
import CreateNewButton from './CreateNewButton';

import UserList from './UserList';
import AlertList from './AlertList';
import SourcesList from './SourcesList';
import CollectionModal from './modals/CollectionModal';

import {formatLink, formatType, formatSubEntities, formatRole} from './formatters';

import './Collection.scss';

export class Collection extends Component {
  state = {
    collection: null,
    editingCollection: false,
    creating: null,
    deleting: false,
    redirect: '',
    copying: null,
    entities: null,
    sorting: null,
    isLoading: true,
    userSearchAvailable: false,
  };

  fileInput = createRef();

  async componentDidMount() {
    this.loadCollection();
    this.setState({
      userSearchAvailable: await isUserSearchAvailable(),
    });
  }

  componentDidUpdate(prevProps) {
    if (prevProps.match.params.id !== this.props.match.params.id) {
      this.setState({redirect: null});
      this.loadCollection();
    }
  }

  loadCollection = () => {
    this.props.mightFail(
      loadEntity('collection', this.props.match.params.id),
      (collection) => this.setState({collection}),
      (error) => {
        showError(error);
        this.setState({collection: null});
      }
    );
    this.loadEntities();
  };

  loadEntities = (sortBy, sortOrder) => {
    this.setState({isLoading: true, sorting: {key: sortBy, order: sortOrder}});
    this.props.mightFail(
      loadCollectionEntities(this.props.match.params.id, sortBy, sortOrder),
      (entities) => this.setState({entities}),
      (error) => {
        showError(error);
        this.setState({entities: null});
      },
      () => this.setState({isLoading: false})
    );
  };

  startEditingCollection = () => {
    this.setState({editingCollection: true});
  };
  stopEditingCollection = () => {
    this.setState({editingCollection: false});
  };

  createUploadedEntity = () => {
    const reader = new FileReader();

    reader.addEventListener('load', () => {
      this.props.mightFail(
        importEntity(reader.result, this.props.match.params.id),
        this.loadEntities,
        showError
      );
      this.fileInput.current.value = null;
    });
    reader.readAsText(this.fileInput.current.files[0]);
  };

  render() {
    const {
      collection,
      deleting,
      editingCollection,
      redirect,
      copying,
      entities,
      sorting,
      isLoading,
      userSearchAvailable,
      creating,
    } = this.state;

    const {match} = this.props;

    const currentTab = match.params.viewMode || 'home';

    if (redirect) {
      return <Redirect to={redirect} />;
    }

    const collectionEntity = {...collection, entityType: 'collection'};
    const hasEditRights = collection && collection.currentUserRole !== 'viewer';

    return (
      <Grid className="Collection" fullWidth>
        <PageTitle pageName={t('common.collection.label')} resourceName={collection?.name} />
        <Column sm={4} md={8} lg={16}>
          <Stack className="layoutContainer" gap={6} orientation="vertical">
            <Stack gap={6} className="header" orientation="horizontal">
              <Folder size="24" />
              {collection && (
                <>
                  <Tooltip content={collection.name} position="bottom" overflowOnly>
                    <span className="text">{collection.name}</span>
                  </Tooltip>
                  {collection.currentUserRole === 'manager' && (
                    <OverflowMenu>
                      <OverflowMenuItem
                        itemText={t('common.edit')}
                        onClick={this.startEditingCollection}
                      />
                      <OverflowMenuItem
                        itemText={t('common.copy')}
                        onClick={() => this.setState({copying: collectionEntity})}
                      />
                      <OverflowMenuItem
                        isDelete
                        itemText={t('common.delete')}
                        onClick={() => this.setState({deleting: collectionEntity})}
                      />
                    </OverflowMenu>
                  )}
                  <Tag className="role" type="blue">
                    {formatRole(collection.currentUserRole)}
                  </Tag>
                </>
              )}
              {!collection && isLoading && (
                <>
                  <SkeletonText className="skeletonText" heading />
                  <SkeletonIcon />
                  <TagSkeleton />
                </>
              )}
            </Stack>
            <Tabs value={currentTab} isLoading={!collection && isLoading}>
              <Tabs.Tab
                key="home"
                value="home"
                title={t('home.collectionTitleWithAmpersand')}
                onClick={() => this.props.history.push('.')}
              >
                <EntityList
                  emptyStateComponent={
                    hasEditRights ? (
                      <EmptyState
                        title={t('home.emptyState.title')}
                        description={t('home.emptyState.description')}
                        icon={<Icon type="dashboard-optimize" />}
                        actions={
                          <>
                            <Button
                              size="md"
                              onClick={() => this.setState({creating: 'dashboard'})}
                            >
                              {t('dashboard.createNew')}
                            </Button>
                            <CreateNewButton
                              collection={collection.id}
                              create={(type) => this.setState({creating: type})}
                              importEntity={() => this.fileInput.current.click()}
                            />
                          </>
                        }
                      />
                    ) : (
                      <EmptyState
                        icon={<DocumentProtected />}
                        title={t('home.empty')}
                        description={t('home.contactManager')}
                      />
                    )
                  }
                  action={
                    hasEditRights && (
                      <CreateNewButton
                        kind="primary"
                        size="lg"
                        collection={collection.id}
                        create={(type) => this.setState({creating: type})}
                        importEntity={() => this.fileInput.current.click()}
                      />
                    )
                  }
                  bulkActions={
                    hasEditRights && [
                      <BulkDeleter
                        type="delete"
                        deleteEntities={async (selected) =>
                          await removeEntities(selected, collection)
                        }
                        checkConflicts={async (selected) =>
                          await checkConflicts(selected, collection)
                        }
                        conflictMessage={t('common.deleter.affectedMessage.bulk.report')}
                      />,
                    ]
                  }
                  isLoading={isLoading}
                  sorting={sorting}
                  onChange={this.loadEntities}
                  headers={[
                    {name: t('common.name'), key: 'name', defaultOrder: 'asc'},
                    t('common.description'),
                    t('home.contents'),
                    {name: t('common.entity.modifiedBy'), key: 'lastModifier', defaultOrder: 'asc'},
                    {
                      name: t('common.entity.modified'),
                      key: 'lastModified',
                      defaultOrder: 'desc',
                    },
                  ]}
                  rows={
                    entities &&
                    entities.map((entity) => {
                      const {
                        id,
                        entityType,
                        currentUserRole,
                        lastModified,
                        lastModifier,
                        name,
                        description,
                        data,
                        reportType,
                        combined,
                      } = entity;
                      const actions = [
                        {
                          icon: <CopyFile />,
                          text: t('common.copy'),
                          action: () => this.setState({copying: entity}),
                        },
                      ];
                      if (currentUserRole === 'editor') {
                        actions.unshift({
                          icon: <Edit />,
                          text: t('common.edit'),
                          action: () =>
                            this.setState({redirect: formatLink(id, entityType) + 'edit'}),
                        });
                        actions.push(
                          {
                            icon: <TrashCan />,
                            text: t('common.delete'),
                            action: () => this.setState({deleting: entity}),
                          },
                          {
                            icon: <Save />,
                            text: t('common.export'),
                            action: () => {
                              window.location.href = `api/export/${entityType}/json/${
                                entity.id
                              }/${encodeURIComponent(formatters.formatFileName(entity.name))}.json`;
                            },
                          }
                        );
                      }
                      return {
                        id,
                        entityType,
                        link: formatLink(id, entityType),
                        icon: getEntityIcon(entityType),
                        type: formatType(entityType, reportType, combined),
                        name,
                        meta: [
                          description,
                          formatSubEntities(data.subEntityCounts),
                          lastModifier,
                          format(parseISO(lastModified), 'PP'),
                        ],
                        actions,
                      };
                    })
                  }
                />
              </Tabs.Tab>
              {userSearchAvailable && collection && (
                <>
                  <Tabs.Tab
                    key="alerts"
                    value="alerts"
                    title={t('alert.label-plural')}
                    onClick={() => this.props.history.push('alerts')}
                  >
                    <AlertList
                      readOnly={collection.currentUserRole === 'viewer'}
                      collection={collection.id}
                    />
                  </Tabs.Tab>
                  <Tabs.Tab
                    key="users"
                    value="users"
                    title={t('common.user.label-plural')}
                    onClick={() => this.props.history.push('users')}
                  >
                    <UserList
                      readOnly={collection.currentUserRole !== 'manager'}
                      onChange={this.loadCollection}
                      collection={collection.id}
                    />
                  </Tabs.Tab>
                </>
              )}
              {collection && (
                <Tabs.Tab
                  key="sources"
                  value="sources"
                  title={t('home.sources.title')}
                  onClick={() => this.props.history.push('sources')}
                >
                  <SourcesList
                    onChange={this.loadEntities}
                    readOnly={collection.currentUserRole !== 'manager'}
                    collection={collection.id}
                  />
                </Tabs.Tab>
              )}
            </Tabs>
          </Stack>
          {editingCollection && (
            <CollectionModal
              title={t('common.collection.modal.title.edit')}
              initialName={collection.name}
              confirmText={t('common.collection.modal.editBtn')}
              onClose={this.stopEditingCollection}
              onConfirm={async (name) => {
                await updateEntity('collection', collection.id, {name});
                this.loadCollection();
                this.stopEditingCollection();
                refreshBreadcrumbs();
              }}
            />
          )}
        </Column>
        <Deleter
          entity={deleting}
          type={deleting && deleting.entityType}
          onDelete={() => {
            if (deleting.entityType === 'collection') {
              this.setState({redirect: '/'});
            } else {
              this.loadEntities();
            }
          }}
          checkConflicts={async () => {
            const {entityType, id} = deleting;
            if (entityType === 'report') {
              return checkDeleteConflict(id, entityType);
            }
            return {conflictedItems: []};
          }}
          onClose={() => this.setState({deleting: null})}
        />
        <Copier
          entity={copying}
          collection={collection && collection.id}
          onCopy={(name, redirect) => {
            const entity = this.state.copying;
            if (!redirect && entity.entityType === 'collection') {
              addNotification({type: 'success', text: t('common.collection.created', {name})});
            }
            if (!redirect && entity.entityType !== 'collection') {
              this.loadEntities();
            }
            this.setState({copying: null});
          }}
          onCancel={() => this.setState({copying: null})}
        />
        {creating === 'report' && (
          <ReportTemplateModal onClose={() => this.setState({creating: null})} />
        )}
        {creating === 'dashboard' && (
          <DashboardTemplateModal onClose={() => this.setState({creating: null})} />
        )}
        {creating === 'kpi' && <KpiCreationModal onClose={() => this.setState({creating: null})} />}
        <input
          className="hidden"
          onChange={this.createUploadedEntity}
          type="file"
          accept=".json"
          ref={this.fileInput}
        />
      </Grid>
    );
  }
}

export default withErrorHandling(Collection);
