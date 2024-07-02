/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {get} from 'request';

export async function loadEntitiesNames(entitiesIds) {
  const res = await get('api/entities/names', entitiesIds);

  return await res.json();
}
