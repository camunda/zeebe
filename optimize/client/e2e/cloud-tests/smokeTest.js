/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import config from '../config';
import * as u from '../utils';

import * as Common from '../sm-tests/Common.elements.js';
import * as Collection from '../sm-tests/Collection.elements.js';
import * as e from './smokeTest.elements.js';

fixture('Smoke test').page(config.collectionsEndpoint);

test('create a report from a template', async (t) => {
  if (!process.argv.includes('ci')) {
    require('dotenv').config();
  }
  await t.maximizeWindow();

  await t
    .typeText(e.usernameInput, process.env.AUTH0_USEREMAIL)
    .click(e.submitButton)
    .typeText(e.passwordInput, process.env.AUTH0_USERPASSWORD)
    .click(e.submitButton);

  await t.click(Collection.navItem);
  await t.click(Common.createNewButton);
  await t.click(Common.menuOption('Report'));
  await t.click(e.emptyTemplate);
  await t.click(Common.modalConfirmButton);
  await u.save(t);
});
