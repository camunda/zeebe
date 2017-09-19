import {dispatchAction} from 'view-utils';
import {get} from 'request';
import {
  createLoadProcessDefinitionsAction, createLoadProcessDefinitionsResultAction,
  createLoadProcessDefinitionsErrorAction, createSetVersionAction,
  createSetVersionXmlAction, createResetProcessDefinitionsErrorAction
} from './reducer';
import {getRouter} from 'router';
import {addNotification} from 'notifications';

const router = getRouter();

export function loadProcessDefinitions() {
  dispatchAction(createLoadProcessDefinitionsAction());
  get('/api/process-definition/groupedByKey')
    .then(response => response.json())
    .then(result => {
      const withLastestVersions = result.map(({versions}) => {
        const latestVersion = getLatestVersion(versions);

        return {
          current: latestVersion,
          versions
        };
      });
      const engineCount = withLastestVersions
        .reduce((engines, {current: {engine}}) => {
          if (engines.find(other => other === engine)) {
            return engines;
          }

          return engines.concat(engine);
        }, [])
      .length;
      const ids = withLastestVersions
        .map(({current: {id}}) => id)
        .reduce((ids, id) => {
          if (ids.length) {
            return ids + '&ids=' + encodeURIComponent(id);
          }

          return 'ids=' + encodeURIComponent(id);
        }, '');

      return get('/api/process-definition/xml?' + ids)
        .then(response => response.json())
        .then(xmls => {
          return {
            list: withLastestVersions.map(entry => {
              const xml = xmls[entry.current.id];

              entry.current.bpmn20Xml = xml;

              return entry;
            }),
            engineCount
          };
        });
    })
    .then(result => {
      dispatchAction(createLoadProcessDefinitionsResultAction(result));
    })
    .catch(err => {
      addNotification({
        status: 'Could not load process definitions',
        isError: true
      });
      dispatchAction(createLoadProcessDefinitionsErrorAction(err));
    });
}

function getLatestVersion(versions) {
  return versions.sort(({version: versionA}, {version: versionB}) => {
    return versionB - versionA;
  })[0];
}

export function openDefinition(id) {
  router.goTo('processDisplay', {definition: id});
}

export function setVersionForProcess(previousId, {id, key, version, bpmn20Xml}) {
  if (typeof bpmn20Xml !== 'string') {
    return get(`/api/process-definition/${id}/xml`)
      .then(response => response.text())
      .then(xml => {
        dispatchAction(createSetVersionXmlAction(previousId, version, xml));
        dispatchAction(createSetVersionAction(previousId, version));
      });
  }

  dispatchAction(createSetVersionAction(previousId, version));
}

export function resetData() {
  dispatchAction(createResetProcessDefinitionsErrorAction());
}
