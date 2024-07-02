/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

export {
  getFlowNodeNames,
  checkDeleteConflict,
  loadDecisionDefinitionXml,
  loadInputVariables,
  loadOutputVariables,
  loadProcessDefinitionXml,
  loadVariables,
} from './dataLoaders';

export {numberParser} from './NumberParser';
export {incompatibleFilters} from './incompatibleFilters.ts';
export {default as reportConfig, createReportUpdate, getDefaultSorting} from './reportConfig';
export {getDiagramElementsBetween} from './diagramServices';
export {default as getTooltipText} from './getTooltipText';
export {default as getScreenBounds} from './getScreenBounds';
export {loadEntity, loadReports, updateEntity, deleteEntity, copyReport} from './entityService';
export {UNAUTHORIZED_TENANT_ID} from './tenantService.ts';
export {loadEntities, copyEntity, createEntity, getEntityIcon} from './entityService.tsx';

export {
  evaluateReport,
  isDurationReport,
  loadRawData,
  getReportResult,
  processResult,
  isAlertCompatibleReport,
  isCategoricalBar,
  isCategorical,
  TEXT_REPORT_MAX_CHARACTERS,
  isTextTileTooLong,
  isTextTileValid,
} from './reportService';

export {addSources, getCollection} from './collectionService';

export * as formatters from './formatters';

export function getRandomId() {
  return Math.random().toString(36).slice(2);
}

export function capitalize(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

export {loadAlerts, addAlert, removeAlert, editAlert} from './alertService';
export {default as ignoreFragments} from './ignoreFragments';
export {default as isReactElement} from './isReactElement';
export {loadDefinitions} from './loadDefinitions';
