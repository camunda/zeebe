var uuid = require('uuid/v4');
var bpmn = require('./bpmn');

// Possible states that activity can be in
var states = ['COMPLETED', 'CREATED'];
var eventsFactor = 50;
var processInstanceFactor = 4;
var isCI = process.env.NODE_ENV === 'ci';

module.exports = {
  getData: getData
};

function getData() {
  return bpmn
    .getBpmnEntries()
    .then(function(bpmnEntries) {
      var processDefinitions = createProcessDefinitions(bpmnEntries);

      return {
        event: createEvents(eventsFactor, bpmnEntries, processDefinitions),
        'process-definition': processDefinitions,
        'process-definition-xml': createXmlEntries(bpmnEntries, processDefinitions),
        users: [
          {
            username: 'admin',
            password: 'admin'
          }
        ]
      };
    });
}

function createProcessDefinitions(bpmnEntries) {
  return bpmnEntries.map(function(entry) {
    return {
      id: uuid(),
      key: entry.key,
      name: entry.key
    };
  });
}

function createXmlEntries(bpmnEntries, processDefinitions) {
  return bpmnEntries.map(function(entry, index) {
    var id = processDefinitions[index].id;

    return {
      id: id,
      bpmn20Xml: entry.xml
    };
  });
}

function createEvents(factor, bpmnEntries, definitions) {
  var events = [];
  var  i;

  for(i = 0; i < factor * processInstanceFactor; i++) {
    events.push.apply(
      events,
      getEventsForProcess(factor, bpmnEntries, definitions)
    );
  }

  return events;
}

function getEventsForProcess(factor, bpmnEntries, definitions) {
  var processInstanceId = uuid();
  var entryIndex = Math.floor(Math.random() * bpmnEntries.length);
  var entry = bpmnEntries[entryIndex];
  var definition = definitions[entryIndex];
  var events = [];

  for (i = 0; i < factor; i++) {
    events.push.apply(
      events,
      getEventsForActivity(processInstanceId, entry, definition)
    );
  }

  return events;
}

function getEventsForActivity(processInstanceId, entry, definition) {
  var seed = (Math.random() + Math.random()/2)/1.5;
  var activityIndex = Math.floor(seed * entry.activities.length);
  var activityId = entry.activities[activityIndex];
  var activityInstanceId = uuid();

  return states.map(function(state, index) {
    var startOffset = Math.round(Math.random() * 1000 * 60 * 60 * 24 * 365);
    var endOffset = Math.round(Math.random() * startOffset);

    return {
      activityId: activityId,
      activityInstanceId: activityInstanceId,
      processInstanceId: processInstanceId,
      processDefinitionId: definition.id,
      processDefinitionKey: definition.key,
      state: state,
      timestamp: new Date().getTime() + Math.round(index * -5000 * Math.random()),
      startDate: getDateString(startOffset),
      endDate: getDateString(endOffset)
    };
  });
}

function getDateString(offset) {
  return new Date(Date.now() - offset).toISOString().substr(0,19);
}
