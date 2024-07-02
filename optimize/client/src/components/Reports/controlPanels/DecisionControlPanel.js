/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React from 'react';
import {Accordion, AccordionItem, Layer, Tag} from '@carbon/react';
import {Db2Database, Factor, Filter as FilterIcon} from '@carbon/icons-react';

import {DefinitionSelection} from 'components';
import {DecisionFilter} from 'filter';
import {loadInputVariables, loadOutputVariables, loadDecisionDefinitionXml} from 'services';
import {t} from 'translation';
import {withErrorHandling} from 'HOC';
import {showError} from 'notifications';

import View from './View';
import GroupBy from './GroupBy';

export class DecisionControlPanel extends React.Component {
  state = {
    variables: {
      inputVariable: null,
      outputVariable: null,
    },
  };

  componentDidMount() {
    this.loadVariables(this.props.report.data?.definitions?.[0]);
  }

  loadVariables = ({key, versions, tenantIds}) => {
    if (key && versions && tenantIds) {
      const payload = [
        {decisionDefinitionKey: key, decisionDefinitionVersions: versions, tenantIds},
      ];
      return new Promise((resolve, reject) => {
        this.props.mightFail(
          Promise.all([loadInputVariables(payload), loadOutputVariables(payload)]),
          ([inputVariable, outputVariable]) =>
            this.setState({variables: {inputVariable, outputVariable}}, resolve),
          (error) => reject(showError(error))
        );
      });
    }
  };

  loadXml = ({key, versions, tenantIds}) => {
    if (key && versions?.[0] && tenantIds) {
      return new Promise((resolve, reject) => {
        this.props.mightFail(
          loadDecisionDefinitionXml(key, versions[0], tenantIds[0]),
          resolve,
          (error) => reject(showError(error))
        );
      });
    }

    return null;
  };

  variableExists = (type, varName) =>
    this.state.variables[type].some((variable) => variable.id === varName);

  getNewVariables = (columns) => {
    const types = ['input', 'output'];
    return Object.values(this.state.variables)
      .map((variables, i) => variables.map((variable) => types[i] + ':' + variable.id))
      .flat()
      .filter((col) => !columns.includes(col));
  };

  filterNonExistingVariables = (columns) =>
    columns.filter((col) => {
      if (col.startsWith('input:') || col.startsWith('output:')) {
        const [type, name] = col.split(':');
        return this.variableExists(type + 'Variable', name);
      }

      return true;
    });

  changeDefinition = async ({key, versions, tenantIds, name, identifier}) => {
    const {groupBy, configuration} = this.props.report.data;
    const {columnOrder, includedColumns, excludedColumns} = configuration.tableColumns;
    const definitionData = {
      key,
      versions,
      tenantIds,
      name,
      displayName: name,
      identifier,
    };

    this.props.setLoading(true);
    const [xml] = await Promise.all([
      this.loadXml(definitionData),
      this.loadVariables(definitionData),
    ]);

    const change = {
      definitions: {$set: [definitionData]},
      configuration: {xml: {$set: xml}},
    };

    const variableReport = ['inputVariable', 'outputVariable'].includes(groupBy?.type);

    if (variableReport && !this.variableExists(groupBy.type, groupBy.value.id)) {
      change.groupBy = {$set: null};
      change.visualization = {$set: null};
    }

    if (columnOrder.length) {
      change.configuration.tableColumns = {
        columnOrder: {$set: this.filterNonExistingVariables(columnOrder)},
      };
    }

    if (includedColumns.length) {
      change.configuration.tableColumns = {
        ...change.configuration.tableColumns,
        includedColumns: {
          $set: includedColumns.concat(
            this.getNewVariables(includedColumns.concat(excludedColumns))
          ),
        },
      };
    }

    await this.props.updateReport(change, true);
    this.props.setLoading(false);
  };

  render() {
    const {data, result} = this.props.report;
    const {
      definitions,
      filter,
      configuration: {xml},
    } = data;

    const {key, versions, tenantIds} = definitions?.[0] ?? {};

    return (
      <div className="DecisionControlPanel ReportControlPanel">
        <Layer className="controlSections" style={{overflow: 'initial'}}>
          {/* manual style override will be removed once decision reports use multi-definition setup */}
          <Accordion>
            <AccordionItem
              title={
                <>
                  <Db2Database />
                  {t('common.dataSource')}
                </>
              }
              open
            >
              <DefinitionSelection
                type="decision"
                definitionKey={key}
                versions={versions}
                tenants={tenantIds}
                xml={xml}
                onChange={this.changeDefinition}
              />
            </AccordionItem>
            <AccordionItem
              title={
                <>
                  <Factor />
                  {t('report.reportSetup')}
                </>
              }
              open
            >
              <ul className="reportSetup">
                <li className="select">
                  <span className="label">{t(`report.view.label`)}</span>
                  <View
                    type="decision"
                    report={this.props.report.data}
                    onChange={(change) => this.props.updateReport(change, true)}
                    variables={this.state.variables}
                  />
                </li>
                <GroupBy
                  type="decision"
                  report={this.props.report.data}
                  variables={this.state.variables}
                  onChange={(change) => this.props.updateReport(change, true)}
                />
              </ul>
            </AccordionItem>
            <AccordionItem
              title={
                <>
                  <FilterIcon />
                  {t('common.filter.label')}
                  {data.filter?.length > 0 && (
                    <Tag type="high-contrast" className="filterCount">
                      {filter.length}
                    </Tag>
                  )}
                </>
              }
            >
              <DecisionFilter
                definitions={definitions}
                data={filter}
                onChange={this.props.updateReport}
                decisionDefinitionKey={key}
                decisionDefinitionVersions={versions}
                tenants={tenantIds}
                variables={this.state.variables}
              />
            </AccordionItem>
          </Accordion>
        </Layer>
        {result && typeof result.instanceCount !== 'undefined' && (
          <div className="instanceCount">
            {t(
              `report.instanceCount.decision.label${
                result.instanceCountWithoutFilters !== 1 ? '-plural' : ''
              }-withFilter`,
              {count: result.instanceCount, totalCount: result.instanceCountWithoutFilters}
            )}
          </div>
        )}
      </div>
    );
  }
}

export default withErrorHandling(DecisionControlPanel);
