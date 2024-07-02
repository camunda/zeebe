/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Selector} from 'testcafe';

import {listItem} from './Common.elements';

export const list = Selector('.AlertList');
export const newAlertButton = Selector('.AlertList button.createAlert');
export const inputWithLabel = (label) =>
  Selector('.AlertModal label').withText(label).parent().find('input');
export const copyNameInput = Selector('.Modal.is-visible input');
export const editButton = Selector('[title="Edit Alert"]');
export const cancelButton = Selector('.Modal.is-visible .cds--modal-footer .cds--btn:nth-child(1)');
export const deleteButton = Selector('[title="Delete Alert"]');
export const webhookDropdown = Selector('#webhooks');
export const alertListItem = listItem('alert');
