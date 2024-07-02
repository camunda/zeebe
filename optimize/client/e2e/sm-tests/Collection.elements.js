/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Selector} from 'testcafe';

import {overflowMenuOption, listItem} from './Common.elements';

export const navItem = Selector('.NavItem a').withText('Collections');
export const collectionTitle = Selector('.Collection .header .text');
export const collectionBreadcrumb = Selector('.cds--header__menu-bar .breadcrumb');
export const collectionContextMenu = Selector(
  '.Collection .header .cds--overflow-menu__wrapper button'
);
export const editCollectionNameButton = overflowMenuOption('Edit');
export const copyCollectionButton = overflowMenuOption('Copy');
export const deleteCollectionButton = overflowMenuOption('Delete');
export const remove = (element) => element.find('.DropdownOption').withText('Remove');
const tabButton = Selector('.Collection .cds--tabs__nav-item');
export const entityTab = tabButton.withText('Dashboards & reports');
export const entitiesTab = tabButton.withText('Dashboards');
export const userTab = tabButton.withText('Users');
export const alertTab = tabButton.withText('Alerts');
export const sourcesTab = tabButton.withText('Data sources');
export const activeTab = Selector('.Collection .cds--tab-content:not([hidden])');
export const addButton = activeTab.find('.cds--toolbar-content .cds--btn--primary');
export const emptyStateAdd = activeTab.find('.EmptyState .cds--btn--primary');
export const typeaheadInput = Selector('.Typeahead input');
export const checkbox = (text) => Selector('.Checklist tr').withText(text);
export const processItem = listItem('process');
export const decisionItem = listItem('decision table');
export const userName = (entity) => entity.find('td:nth-child(2) .cds--stack-vertical').child(0);
export const roleOption = (text) =>
  Selector('.Modal.is-visible .LabeledInput .label.after').withText(text);
export const carbonRoleOption = (text) =>
  Selector('.Modal.is-visible .cds--radio-button-wrapper').withText(text);
export const userList = Selector('.UserList');
export const logoutButton = Selector('header button').withText('Logout');
export const usernameDropdown = Selector('header button').withAttribute('aria-label', 'Open User');
export const sourceModalSearchField = Selector('.SourcesModal .cds--search-input');
export const selectAllCheckbox = Selector('.Table thead .cds--table-column-checkbox label');
export const itemCheckbox = (idx) =>
  Selector('.Table tbody tr').nth(idx).find('.cds--table-column-checkbox label');
export const bulkRemove = activeTab.find('.cds--action-list button').withText('Remove');
