export interface SkillCategoryItem {
  categoryCode: string
  categoryName: string
  parentCode?: string | null
  categoryLevel?: number | null
}

export interface SkillTagItem {
  tagCode: string
  tagName: string
  tagType?: string | null
  color?: string | null
}


export interface SkillObservabilityConfig {
  debugEnabled?: number | null
  debugScript?: string | null
  realtimeLogStreaming?: number | null
  testCaseSummary?: string | null
  logEnabled?: number | null
  satisfactionEnabled?: number | null
  metricsPolicy?: string | null
}

export interface SkillReleaseConfig {
  hotUpdateEnabled?: number | null
  releaseStage?: string | null
  approvalRequired?: number | null
  releaseChannel?: string | null
  grayPolicy?: string | null
  publishStrategy?: string | null
  rollbackPolicy?: string | null
}

export interface SkillBatchConfig {
  batchEnabled?: number | null
  importEnabled?: number | null
  exportEnabled?: number | null
  importFormats?: string[]
  exportFormats?: string[]
  tagBatchSupported?: number | null
  categoryBatchSupported?: number | null
  logicalDeleteEnabled?: number | null
  recycleEnabled?: number | null
  copyEnabled?: number | null
}

export interface SkillWorkflowConfig {
  workflowEnabled?: number | null
  workflowSteps?: string[]
  branchRules?: Record<string, unknown>[]
  loopEnabled?: number | null
  childSkillCodes?: string[]
  channelAdapters?: string[]
  orchestrationStrategy?: string | null
}

export interface SkillChannelAdaptation {
  channelCode?: string | null
  locale?: string | null
  successTemplate?: string | null
  failureTemplate?: string | null
  voiceTemplate?: string | null
  styleConfig?: Record<string, unknown> | null
}

export interface SkillMarketplaceConfig {
  marketplaceEnabled?: number | null
  thirdPartyUploadEnabled?: number | null
  reviewRequired?: number | null
  storeVisible?: number | null
  subscriptionMode?: string | null
}

export interface SkillVersionItem {
  id: number
  versionNo: number
  versionCode?: string | null
  versionDescription?: string | null
  versionStatus: string
  publishStatus: string
  releaseStage?: string | null
  createTime: number | null
}

export interface SkillTestCaseItem {
  id: number
  skillId: number
  skillCode: string
  caseName: string
  inputText: string
  slotPayloadJson?: string | null
  expectedIntent?: string | null
  expectedSuccess?: number | null
  expectedResponseContains?: string | null
  channelCode?: string | null
  locale?: string | null
  enabled?: number | null
  lastRunStatus?: string | null
  lastRunDurationMs?: number | null
  lastRunAt?: number | null
  lastResultJson?: string | null
}

export interface SkillDebugTraceStep {
  stepName: string
  stepStatus: string
  detail: string
}

export interface SkillDebugResult {
  skillId?: number | null
  skillCode?: string | null
  matchedIntent?: string | null
  confidenceScore?: number | null
  successFlag: number
  responseText?: string | null
  failureReason?: string | null
  elapsedMs?: number | null
  resolvedSlots?: Record<string, unknown>
  contextPayload?: Record<string, unknown>
  traceSteps?: SkillDebugTraceStep[]
}

export interface SkillExecutionLogItem {
  id: number
  skillId?: number | null
  skillCode?: string | null
  skillName?: string | null
  sourceType?: string | null
  sourceId?: number | null
  traceId?: string | null
  sessionCode?: string | null
  channelCode?: string | null
  locale?: string | null
  inputText?: string | null
  matchedIntent?: string | null
  confidenceScore?: number | null
  requestPayloadJson?: string | null
  responsePayloadJson?: string | null
  tracePayloadJson?: string | null
  executeStatus?: string | null
  successFlag?: number | null
  elapsedMs?: number | null
  failureReason?: string | null
  satisfactionLevel?: number | null
  operatorUserName?: string | null
  createTime?: number | null
}

export interface SkillVersionCompareResult {
  sourceVersionNo: number
  targetVersionNo: number
  sourceSnapshotJson: string
  targetSnapshotJson: string
  diffSummary: string
}

export interface SkillItem {
  id: number
  skillCode: string
  skillName: string
  description?: string | null
  skillType: string
  skillCategory: string
  categoryChain?: SkillCategoryItem[]
  tags?: SkillTagItem[]
  skillStatus: string
  publishStatus: string
  sortWeight?: number | null
  versionCode?: string | null
  versionDescription?: string | null
  versionMode: string
  currentVersionNo?: number | null
  latestVersionNo?: number | null
  publishedVersionNo?: number | null
  hotUpdateEnabled: number
  tenantId?: number | null
  ownerUserId?: number | null
  ownerUserName?: string | null
  observabilityConfig?: SkillObservabilityConfig | null
  releaseConfig?: SkillReleaseConfig | null
  batchConfig?: SkillBatchConfig | null
  workflowConfig?: SkillWorkflowConfig | null
  channelAdaptations?: SkillChannelAdaptation[]
  marketplaceConfig?: SkillMarketplaceConfig | null
  versions?: SkillVersionItem[]
  testCaseCount?: number | null
  logCount?: number | null
  remark?: string | null
  createTime?: number | null
  updateTime?: number | null
}

export interface SkillPayload {
  skillCode?: string
  skillName: string
  description?: string | null
  skillType: string
  skillCategory: string
  categoryChain?: SkillCategoryItem[]
  tags?: SkillTagItem[]
  skillStatus: string
  sortWeight?: number | null
  versionCode?: string | null
  versionDescription?: string | null
  versionMode: string
  hotUpdateEnabled: number
  observabilityConfig: SkillObservabilityConfig
  releaseConfig: SkillReleaseConfig
  batchConfig: SkillBatchConfig
  workflowConfig: SkillWorkflowConfig
  channelAdaptations?: SkillChannelAdaptation[]
  marketplaceConfig?: SkillMarketplaceConfig
  remark?: string | null
}

export interface SkillStatistics {
  totalCount: number
  enabledCount: number
  publishedCount: number
  hotUpdateEnabledCount: number
  draftCount?: number
  deletedCount?: number
  totalTestCaseCount?: number
  totalLogCount?: number
  successLogCount?: number
  failureLogCount?: number
}

export interface SkillExportResult {
  skillCode: string
  skillName: string
  exportFormat: string
  exportPayload: string
}

export interface SkillBatchActionPayload {
  skillIds: number[]
  skillStatus?: string
  targetCategoryCode?: string
  tagNames?: string[]
  versionDescription?: string
}

export interface SkillCopyPayload {
  newSkillCode: string
  newSkillName: string
  includeTestCases?: number
}

export interface SkillVersionRollbackPayload {
  targetVersionNo: number
  versionDescription?: string
}

export interface SkillVersionComparePayload {
  sourceVersionNo: number
  targetVersionNo: number
}

export interface SkillImportPayload {
  importPayload: string
  importFormat?: string
  publishAfterImport?: number
}

export interface SkillTestCasePayload {
  caseName: string
  inputText: string
  slotPayload?: Record<string, unknown>
  expectedIntent?: string | null
  expectedSuccess?: number | null
  expectedResponseContains?: string | null
  channelCode?: string | null
  locale?: string | null
  enabled?: number | null
}

export interface SkillLogQueryPayload {
  skillId?: number | null
  sourceType?: string | null
  successFlag?: number | null
}
