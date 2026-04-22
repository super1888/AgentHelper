export interface InterceptorCatalogItem {
  interceptorKey: string
  interceptorName: string
  description?: string | null
  interceptorType: string
  interceptorStage: string
  riskLevel: string
  failStrategy: string
  defaultConfigJson?: string | null
  defaultTestPayloadJson?: string | null
  tags?: string[]
}

export interface InterceptorVersionItem {
  id: number
  versionNo: number
  versionCode?: string | null
  versionDescription?: string | null
  versionStatus: string
  publishStatus: string
  snapshotJson?: string | null
  createTime?: number | null
}

export interface InterceptorItem {
  id: number
  interceptorCode: string
  interceptorName: string
  description?: string | null
  interceptorType: string
  interceptorStage: string
  interceptorStatus: string
  publishStatus: string
  riskLevel: string
  triggerMode: string
  failStrategy: string
  sortWeight?: number | null
  timeoutMs?: number | null
  hotUpdateEnabled?: number | null
  currentVersionNo?: number | null
  latestVersionNo?: number | null
  publishedVersionNo?: number | null
  versionCode?: string | null
  versionDescription?: string | null
  builtinInterceptorKey?: string | null
  scriptLanguage?: string | null
  tags?: string[]
  targetChannels?: string[]
  targetEnvironments?: string[]
  targetAgentCodes?: string[]
  targetModelCodes?: string[]
  conditionConfig?: Record<string, unknown> | null
  runtimeConfig?: Record<string, unknown> | null
  securityConfig?: Record<string, unknown> | null
  observabilityConfig?: Record<string, unknown> | null
  degradationConfig?: Record<string, unknown> | null
  interceptorConfig?: Record<string, unknown> | null
  scriptContent?: string | null
  testPayloadJson?: string | null
  bindingCount?: number | null
  testCaseCount?: number | null
  logCount?: number | null
  remark?: string | null
  createTime?: number | null
  updateTime?: number | null
  versions?: InterceptorVersionItem[]
}

export interface InterceptorPayload {
  interceptorCode: string
  interceptorName: string
  description?: string | null
  interceptorType: string
  interceptorStage: string
  interceptorStatus: string
  riskLevel: string
  triggerMode: string
  failStrategy: string
  sortWeight: number
  timeoutMs: number
  hotUpdateEnabled: number
  versionCode?: string | null
  versionDescription?: string | null
  builtinInterceptorKey?: string | null
  scriptLanguage: string
  tags?: string[]
  targetChannels?: string[]
  targetEnvironments?: string[]
  targetAgentCodes?: string[]
  targetModelCodes?: string[]
  conditionConfig?: Record<string, unknown>
  runtimeConfig?: Record<string, unknown>
  securityConfig?: Record<string, unknown>
  observabilityConfig?: Record<string, unknown>
  degradationConfig?: Record<string, unknown>
  interceptorConfig?: Record<string, unknown>
  scriptContent?: string | null
  testPayloadJson?: string | null
  remark?: string | null
}

export interface InterceptorStatistics {
  totalCount: number
  enabledCount: number
  publishedCount: number
  hotUpdateEnabledCount: number
  deletedCount: number
  highRiskCount: number
  totalBindingCount: number
  totalTestCaseCount: number
  totalLogCount: number
  successLogCount: number
  failureLogCount: number
}

export interface InterceptorDebugPayload {
  interceptorId: number | string
  requestPayloadJson?: string | null
  contextPayload?: Record<string, unknown>
  sourceType?: string | null
  agentCode?: string | null
  sessionCode?: string | null
}

export interface InterceptorDebugResult {
  interceptorId: number
  interceptorCode: string
  interceptorName: string
  successFlag: number
  executeStatus: string
  responseText?: string | null
  failureReason?: string | null
  elapsedMs?: number | null
  requestPayloadJson?: string | null
  responsePayloadJson?: string | null
  tracePayload?: Record<string, unknown>
}

export interface InterceptorExecutionLogItem {
  id: number
  interceptorId?: number | null
  interceptorCode?: string | null
  interceptorName?: string | null
  sourceType?: string | null
  traceId?: string | null
  agentCode?: string | null
  sessionCode?: string | null
  requestPayloadJson?: string | null
  responsePayloadJson?: string | null
  executeStatus?: string | null
  successFlag?: number | null
  elapsedMs?: number | null
  failureReason?: string | null
  operatorUserName?: string | null
  createTime?: number | null
}

export interface InterceptorTestCaseItem {
  id: number
  interceptorId: number
  interceptorCode: string
  caseName: string
  inputPayloadJson?: string | null
  contextPayloadJson?: string | null
  expectedSuccess?: number | null
  expectedResponseContains?: string | null
  enabled?: number | null
  lastRunStatus?: string | null
  lastRunDurationMs?: number | null
  lastRunAt?: number | null
  lastResultJson?: string | null
}

export interface InterceptorBindingItem {
  id: number
  interceptorId: number
  interceptorCode: string
  bindingName: string
  bindingScope: string
  targetAgentCode?: string | null
  targetModelCode?: string | null
  environmentCode?: string | null
  priorityNo?: number | null
  enabled?: number | null
  remark?: string | null
  createTime?: number | null
}
