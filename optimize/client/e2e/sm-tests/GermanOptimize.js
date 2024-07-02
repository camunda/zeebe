/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import config from '../config';
import {cleanEntities} from '../setup';
import {login, save} from '../utils';
import * as Common from './Common.elements.js';

fixture('German Optimize')
  .page(config.endpoint)
  .beforeEach(async (t) => {
    await login(t);
    await t.navigateTo(config.collectionsEndpoint);
  })
  .afterEach(cleanEntities)
  .clientScripts({
    content: `
      navigator.__defineGetter__('languages', function () {
        return ['de-DE'];
      });`,
  });

test('Create a report in the german version of optimize', async (t) => {
  await t.click(Common.createNewButton);
  await t.click(Common.menuOption('Bericht'));

  await t.click(Common.templateModalProcessField);
  await t.click(Common.firstOption);
  await t.click(Common.modalConfirmButton);

  await save(t);
});
