/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

export function getRandomId() {
  return Math.random().toString(36).slice(2);
}

export {loadReports, loadEntities, copyEntity, createEntity, getEntityIcon} from './entityService';
export {UNAUTHORIZED_TENANT_ID} from './tenantService';
export * as formatters from './formatters';
export {
  loadProcessDefinitionXml,
  loadDecisionDefinitionXml,
  loadInputVariables,
  loadOutputVariables,
  loadVariables,
} from './dataLoaders';
export {numberParser} from './NumberParser';
export {
  TEXT_REPORT_MAX_CHARACTERS,
  isTextTileTooLong,
  isTextTileValid,
  loadRawData,
  evaluateReport,
} from './reportService';
export {addSources, getCollection} from './collectionService';
export {default as getScreenBounds} from './getScreenBounds';
export {default as ignoreFragments} from './ignoreFragments';
export {default as isReactElement} from './isReactElement';
export {incompatibleFilters} from './incompatibleFilters';
export {loadDefinitions} from './loadDefinitions';

export type {Definition} from './loadDefinitions';
export type {ReportEvaluationPayload} from './reportService';
