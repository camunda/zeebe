/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {SerializedEditorState} from 'lexical';

export type GenericEntity<D extends object = Record<string, unknown>> = {
  id: string | null;
  name: string;
  lastModified: string;
  created: string;
  owner: string;
  lastModifier: string;
  currentUserRole: string;
  data: D;
};

export type EntityListEntity<D extends object = Record<string, unknown>> = GenericEntity<D> & {
  entityType: string;
};

type FilterFilterApplicationLevel = 'instance' | 'view';

export type InstanceStateFilterType =
  | 'runningInstancesOnly'
  | 'completedInstancesOnly'
  | 'canceledInstancesOnly'
  | 'nonCanceledInstancesOnly'
  | 'suspendedInstancesOnly'
  | 'nonSuspendedInstancesOnly';

export type FlowNodeStateFilterType =
  | 'runningFlowNodesOnly'
  | 'completedFlowNodesOnly'
  | 'canceledFlowNodesOnly'
  | 'completedOrCanceledFlowNodesOnly';

export type IncidentFilterType =
  | 'includesOpenIncident'
  | 'includesResolvedIncident'
  | 'includesClosedIncident'
  | 'doesNotIncludeIncident';

export type FilterDataProps = {
  flowNodeDuration: Record<string, FilterData>;
  runningInstancesOnly: boolean | null;
  processInstanceDuration: FilterData;
} & {
  [key in 'instanceStartDate' | 'instanceEndDate']: Partial<DateFilterType>;
} & {
  [key in 'flowNodeStartDate' | 'flowNodeEndDate']: Partial<
    DateFilterType & {flowNodeIds: string[] | null}
  >;
} & {
  [key in 'assignee' | 'candidateGroup']: {
    values?: (string | null)[];
    operator?: string;
  };
} & {
  [key in InstanceStateFilterType | FlowNodeStateFilterType | IncidentFilterType]:
    | FilterData
    | undefined;
} & {
  [key in 'executedFlowNodes' | 'executingFlowNodes' | 'canceledFlowNodes']: {
    values?: string[];
    operator?: string;
  };
};

export type FilterType = keyof FilterDataProps;

export interface ProcessFilter<T extends FilterType = FilterType> {
  type: T;
  data: FilterDataProps[T];
  filterLevel: FilterFilterApplicationLevel;
  appliedTo: string[];
}

export interface FilterData {
  value: string | number;
  unit: string;
  operator?: string;
}

interface DecisionFilter<Data = FilterData> {
  type: 'evaluationDateTime' | 'inputVariable' | 'outputVariable';
  data: Data;
  appliedTo: string[];
}

type VariableType =
  | 'String'
  | 'Short'
  | 'Long'
  | 'Double'
  | 'Integer'
  | 'Boolean'
  | 'Date'
  | 'Object'
  | 'Json';

type ViewProcessViewEntity = 'flowNode' | 'userTask' | 'processInstance' | 'variable' | 'incident';

interface ProcessViewProperty {
  name: string;
  type: VariableType;
}

interface ProcessView {
  entity: ViewProcessViewEntity;
  properties: (ProcessViewProperty | string)[];
}

interface DecisionView {
  properties: (ProcessViewProperty | string)[];
}

interface ProcessGroupBy<Value = unknown> {
  type:
    | 'assignee'
    | 'candidateGroup'
    | 'duration'
    | 'endDate'
    | 'flowNodes'
    | 'none'
    | 'runningDate'
    | 'startDate'
    | 'userTasks'
    | 'variable';
  value: Value;
}

interface DecisionGroupBy<Value = unknown> {
  type: 'evaluationDateTime' | 'inputVariable' | 'matchedRule' | 'none' | 'outputVariable';
  value: Value;
}

type DistributedByType =
  | 'assignee'
  | 'candidateGroup'
  | 'endDate'
  | 'flowNode'
  | 'none'
  | 'process'
  | 'startDate'
  | 'userTask'
  | 'variable';

interface DistributedBy<Value = unknown> {
  type: DistributedByType;
  value: Value;
}

type ProcessReportVisualization = 'number' | 'table' | 'bar' | 'barLine' | 'line' | 'pie' | 'heat';

type DecisionReportVisualization = 'number' | 'table' | 'bar' | 'line' | 'pie' | 'heat';

type AggregationTypeType = 'avg' | 'min' | 'max' | 'sum' | 'percentile';

interface AggregationType<Type extends AggregationTypeType = AggregationTypeType> {
  type: Type;
  value: number | null;
}

type TargetValueUnit =
  | 'millis'
  | 'seconds'
  | 'minutes'
  | 'hours'
  | 'days'
  | 'weeks'
  | 'months'
  | 'years';

type GroupAggregateByDateUnit = 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'automatic';

interface TableColumns {
  includeNewVariables: boolean;
  excludedColumns: string[];
  includedColumns: string[];
  columnOrder: string[];
}

interface BaseLine {
  unit: TargetValueUnit;
  value: string;
}

interface TargetValue {
  unit: TargetValueUnit;
  value: string;
  isBelow: boolean;
}

interface DurationProgressTargetValue extends TargetValue {
  baseline: BaseLine;
  target: TargetValue;
}

interface CountProgressTargetValue extends TargetValue {
  baseline: string;
  target: string;
}

interface SingleReportTargetValue {
  countChart: TargetValue;
  durationProgress: DurationProgressTargetValue;
  active: boolean;
  countProgress: CountProgressTargetValue;
  durationChart: TargetValue;
  isKpi: boolean;
}

interface HeatmapTargetValueValue {
  unit: TargetValueUnit;
  value: string;
}

interface HeatmapTargetValue {
  active: boolean;
  values: Record<string, HeatmapTargetValueValue>;
}

type CustomBucketUnit =
  | 'year'
  | 'month'
  | 'week'
  | 'day'
  | 'hour'
  | 'minute'
  | 'second'
  | 'millisecond';

interface CustomBucket {
  active: boolean;
  bucketSize: string;
  bucketSizeUnit: CustomBucketUnit;
  baseline: string;
  baselineUnit: CustomBucketUnit;
}

type SortingOrder = 'asc' | 'desc';

interface ReportSorting {
  by?: string | null;
  order?: SortingOrder | null;
}

interface ProcessPart {
  start: string;
  end: string;
}

interface MeasureVisualizations {
  frequency: string;
  duration: string;
}

type UserTaskDurationTimes = 'idle' | 'work' | 'total';

export interface SingleReportConfiguration {
  color: string;
  aggregationTypes: AggregationType[];
  userTaskDurationTimes: UserTaskDurationTimes[];
  showInstanceCount: boolean;
  pointMarkers: boolean;
  precision: number | null;
  hideRelativeValue: boolean;
  hideAbsoluteValue: boolean;
  alwaysShowRelative: boolean;
  alwaysShowAbsolute: boolean;
  showGradientBars: boolean;
  xml: string | null;
  tableColumns: TableColumns;
  targetValue: SingleReportTargetValue;
  heatmapTargetValue: HeatmapTargetValue;
  groupByDateVariableUnit: GroupAggregateByDateUnit;
  distributeByDateVariableUnit: GroupAggregateByDateUnit;
  customBucket: CustomBucket;
  distributeByCustomBucket: CustomBucket;
  sorting?: ReportSorting | null;
  processPart?: ProcessPart | null;
  measureVisualizations: MeasureVisualizations;
  stackedBar: boolean;
  horizontalBar: boolean;
  logScale: boolean;
  yLabel: string;
  xLabel: string;
}

interface SingleReportData {
  configuration: SingleReportConfiguration;
  definitions: Definition[];
}

export interface SingleProcessReportData<GroupByValue = unknown, DistributedByValue = unknown>
  extends SingleReportData {
  filter: ProcessFilter[];
  view: ProcessView | null;
  groupBy: ProcessGroupBy<GroupByValue> | null;
  distributedBy: DistributedBy<DistributedByValue>;
  visualization: ProcessReportVisualization | null;
  managementReport: boolean;
  instantPreviewReport: boolean;
  userTaskReport: boolean;
}

interface SingleDecisionReportData<GroupByValue = unknown, DistributedByValue = unknown>
  extends SingleReportData {
  filter: DecisionFilter[];
  view: DecisionView | null;
  groupBy: DecisionGroupBy<GroupByValue> | null;
  distributedBy: DistributedBy<DistributedByValue>;
  visualization: DecisionReportVisualization | null;
}

interface CombinedReportTargetValue {
  countChart: TargetValue;
  active: boolean;
  durationChart: TargetValue;
}

interface CombinedReportConfiguration {
  pointMarkers: boolean;
  hideRelativeValue: boolean;
  hideAbsoluteValue: boolean;
  alwaysShowRelative: boolean;
  alwaysShowAbsolute: boolean;
  targetValue: CombinedReportTargetValue;
  yLabel: string;
  xLabel: string;
}

interface CombinedReportData {
  configuration: CombinedReportConfiguration;
  visualization: ProcessReportVisualization;
  reports: {
    id: string;
    color: string;
  }[];
}

type CombinedReportResult = {
  data: Record<
    string,
    {
      data: {
        view: {
          properties: string[];
        };
      };
    }
  >;
};

export type ReportType = 'process' | 'decision';

export interface Report<
  Type extends ReportType = 'process',
  Combined extends boolean = false,
  Data extends object = Combined extends true
    ? CombinedReportData
    : Type extends 'process'
      ? SingleProcessReportData
      : SingleDecisionReportData,
  Result = unknown | undefined,
> extends GenericEntity<Data> {
  id: string;
  collectionId?: string | null;
  combined: Combined;
  reportType: Type;
  description: string | null;
  result: Result;
}

export type CombinedReport = Report<'process', true, CombinedReportData, CombinedReportResult>;
export type SingleProcessReport<GroupByValue = unknown, DistributedByValue = unknown> = Report<
  'process',
  false,
  SingleProcessReportData<GroupByValue, DistributedByValue>
>;
export type SingleDecisionReport<GroupByValue = unknown, DistributedByValue = unknown> = Report<
  'decision',
  false,
  SingleDecisionReportData<GroupByValue, DistributedByValue>
>;

export type GenericReport<GroupByValue = unknown, DistributedByValue = unknown> =
  | CombinedReport
  | SingleProcessReport<GroupByValue, DistributedByValue>
  | SingleDecisionReport<GroupByValue, DistributedByValue>;

type DashboardTileCommonProps = {
  id: string;
  position: {
    x: number;
    y: number;
  };
  dimensions: {
    width: number;
    height: number;
  };
};

export interface OptimizeReportTile extends DashboardTileCommonProps {
  type: 'optimize_report';
  configuration: GenericReport['data']['configuration'];
}

export interface ExternalTile extends DashboardTileCommonProps {
  type: 'external_url';
  configuration: {external: string};
}

export interface TextTile extends DashboardTileCommonProps {
  type: 'text';
  configuration: {text: SerializedEditorState | null};
}

export type DashboardTile = OptimizeReportTile | ExternalTile | TextTile;

export interface Definition {
  identifier: string;
  displayName?: string | JSX.Element[];
  name?: string;
  key?: string;
  tenantIds?: (string | null)[];
  versions?: string[];
  flowNodeIds?: string[];
  type?: string;
}

export type Tenant = {id: string | null; name?: string};

export type Source = {
  definitionKey: string;
  definitionType?: string;
  tenants: (string | null)[];
};

export type Variable = {id?: string; name: string; type: string; label?: string | null};

type CommonFilter = {includeUndefined?: boolean; excludeUndefined?: boolean; name?: string};

type FixedFilter = {
  type: 'fixed';
  start: string | null;
  end: string | null;
};

type RollingFilter = {
  type: 'rolling' | 'custom';
  start: {value?: number | string; unit?: string} | null;
  end: string | null;
  customNum?: string;
};

type OtherFilter = {
  type: '' | 'relative' | 'today' | 'yesterday' | 'this' | 'last' | 'between' | 'after' | 'before';
  start: {value?: number | string; unit?: string} | null;
  end: string | null;
};

export type DateFilterType = CommonFilter & (FixedFilter | RollingFilter | OtherFilter);

interface CommonFilterState {
  type: string;
  unit: string;
  valid?: boolean;
  startDate: Date | null;
  endDate: Date | null;
  includeUndefined?: boolean;
  excludeUndefined?: boolean;
  applyTo?: Definition[];
  values?: (string | number | boolean | null)[];
  operator?: string;
  customNum: string;
}

export interface NoDateFilterState extends CommonFilterState {
  type: 'this' | 'last' | 'yesterday' | 'today';
  startDate: null;
  endDate: null;
}

export interface BetweenFilterState extends CommonFilterState {
  type: 'between';
  startDate: Date;
  endDate: Date;
}

export interface BeforeFilterState extends CommonFilterState {
  type: 'before';
  startDate: Date | null;
  endDate: Date;
}

export interface AfterFilterState extends CommonFilterState {
  type: 'after';
  startDate: Date;
  endDate: Date | null;
}

export interface CustomFilterState extends CommonFilterState {
  type: 'custom';
}

export type FilterState =
  | CommonFilterState
  | BetweenFilterState
  | BeforeFilterState
  | AfterFilterState
  | CustomFilterState;

export interface AnalysisDurationChartEntry {
  key: number;
  value: number;
  outlier: boolean;
}
