/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

export function getExternalResourcePath(fileName: string, subDir?: string): string {
  return `${window.location.pathname}external/static/${subDir ? subDir + '/' : ''}${fileName}`;
}
