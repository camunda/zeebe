/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

export function incompatibleFilters(
  filterData: {type: string; filterLevel?: string}[],
  view?: {entity?: string}
) {
  const bothExist = (arr: string[], checkLevel?: string) =>
    arr.every((val) =>
      filterData.some(({type, filterLevel}) => type === val && sameLevel(checkLevel, filterLevel))
    );

  return (
    bothExist(['completedInstancesOnly', 'runningInstancesOnly']) ||
    bothExist(['completedInstancesOnly', 'suspendedInstancesOnly']) ||
    bothExist(['canceledInstancesOnly', 'runningInstancesOnly']) ||
    bothExist(['canceledInstancesOnly', 'nonCanceledInstancesOnly']) ||
    bothExist(['canceledInstancesOnly', 'suspendedInstancesOnly']) ||
    bothExist(['nonSuspendedInstancesOnly', 'suspendedInstancesOnly']) ||
    bothExist(['instanceEndDate', 'runningInstancesOnly']) ||
    bothExist(['instanceEndDate', 'suspendedInstancesOnly']) ||
    ((view?.entity === 'flowNode' || view?.entity === 'userTask') &&
      (bothExist(['completedFlowNodesOnly', 'runningFlowNodesOnly']) ||
        bothExist(['canceledFlowNodesOnly', 'runningFlowNodesOnly']) ||
        bothExist(['completedOrCanceledFlowNodesOnly', 'runningFlowNodesOnly']) ||
        bothExist(['completedFlowNodesOnly', 'canceledFlowNodesOnly']))) ||
    bothExist(['doesNotIncludeIncident', 'includesOpenIncident']) ||
    bothExist(['doesNotIncludeIncident', 'includesResolvedIncident']) ||
    (view?.entity === 'incident' &&
      bothExist(['includesOpenIncident', 'includesResolvedIncident'], 'view'))
  );
}

function sameLevel(checkLevel?: string, filterLevel?: string) {
  if (checkLevel) {
    return checkLevel === filterLevel;
  }
  return true;
}
