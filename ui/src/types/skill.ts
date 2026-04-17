export interface SkillParameterConfig {
  parameterName: string
  parameterType: string
  required: number
  defaultValue: string | null
  description: string | null
}

export interface SkillIntentConfig {
  intentName: string
  keywords: string[]
  parameterConfigs: SkillParameterConfig[]
}

export interface SkillExecutionConfig {
  executionType: string
  apiEndpoint: string | null
  httpMethod: string | null
  functionName: string | null
  timeoutMs: string | null
  requestTemplate: string | null
  responseMapping: string | null
}

export interface SkillRoutingConfig {
  routePolicy: string | null
  routeTags: string[]
  contextWindowStrategy: string | null
  memoryPolicy: string | null
  fallbackSkillCode: string | null
}

export interface SkillPermissionConfig {
  allowedRoles: string[]
  dataScopes: string[]
  approvalPolicy: string | null
  riskLevel: string | null
  riskControlPolicy: string | null
}

export interface SkillObservabilityConfig {
  debugEnabled: number
  debugScript: string | null
  testCaseSummary: string | null
  logEnabled: number
  metricsPolicy: string | null
}

export interface SkillReleaseConfig {
  hotUpdateEnabled: number
  releaseChannel: string | null
  grayPolicy: string | null
  rollbackPolicy: string | null
}

export interface SkillBatchConfig {
  batchEnabled: number
  importEnabled: number
  exportEnabled: number
  importTemplate: string | null
  exportTemplate: string | null
}

export interface SkillWorkflowConfig {
  workflowSteps: string[]
  channelAdapters: string[]
  orchestrationStrategy: string | null
}

export interface SkillVersionItem {
  id: number
  versionNo: number
  versionStatus: string
  publishStatus: string
  createTime: number | null
}

export interface SkillItem {
  id: number
  skillCode: string
  skillName: string
  description: string | null
  skillCategory: string
  skillStatus: string
  publishStatus: string
  versionMode: string
  currentVersionNo: number | null
  latestVersionNo: number | null
  publishedVersionNo: number | null
  hotUpdateEnabled: number
  tenantId: number | null
  ownerUserId: number | null
  ownerUserName: string | null
  intentConfigs: SkillIntentConfig[]
  executionConfig: SkillExecutionConfig | null
  routingConfig: SkillRoutingConfig | null
  permissionConfig: SkillPermissionConfig | null
  observabilityConfig: SkillObservabilityConfig | null
  releaseConfig: SkillReleaseConfig | null
  batchConfig: SkillBatchConfig | null
  workflowConfig: SkillWorkflowConfig | null
  versions: SkillVersionItem[]
  remark: string | null
  createTime: number | null
  updateTime: number | null
}

export interface SkillPayload {
  skillCode?: string
  skillName: string
  description: string | null
  skillCategory: string
  skillStatus: string
  versionMode: string
  hotUpdateEnabled: number
  intentConfigs: SkillIntentConfig[]
  executionConfig: SkillExecutionConfig
  routingConfig: SkillRoutingConfig
  permissionConfig: SkillPermissionConfig
  observabilityConfig: SkillObservabilityConfig
  releaseConfig: SkillReleaseConfig
  batchConfig: SkillBatchConfig
  workflowConfig: SkillWorkflowConfig
  remark: string | null
}

export interface SkillStatistics {
  totalCount: number
  enabledCount: number
  publishedCount: number
  hotUpdateEnabledCount: number
}

export interface SkillExportResult {
  skillCode: string
  skillName: string
  exportPayload: string
}
