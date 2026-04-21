export interface ToolCatalogItem {
  toolKey: string
  toolName: string
  description?: string | null
  toolType: string
  toolCategory: string
  sourceType: string
  tags?: string[]
  defaultRequestSchemaJson?: string | null
  defaultRuntimeConfigJson?: string | null
  defaultTestPayloadJson?: string | null
}

export interface ToolItem {
  id: number
  toolCode: string
  toolName: string
  description?: string | null
  toolType: string
  toolCategory: string
  sourceType: string
  toolStatus: string
  publishStatus: string
  riskLevel: string
  executionMode: string
  sortWeight?: number | null
  timeoutMs?: number | null
  authRequired?: number | null
  builtinToolKey?: string | null
  endpointUrl?: string | null
  httpMethod?: string | null
  tags?: string[]
  requestSchemaJson?: string | null
  authConfigJson?: string | null
  runtimeConfigJson?: string | null
  testPayloadJson?: string | null
  tenantId?: number | null
  ownerUserId?: number | null
  ownerUserName?: string | null
  logCount?: number | null
  remark?: string | null
  createTime?: number | null
  updateTime?: number | null
}

export interface ToolPayload {
  toolCode: string
  toolName: string
  description?: string | null
  toolType: string
  toolCategory: string
  sourceType: string
  toolStatus: string
  riskLevel: string
  executionMode: string
  sortWeight: number
  timeoutMs: number
  authRequired: number
  builtinToolKey?: string | null
  endpointUrl?: string | null
  httpMethod?: string | null
  tags?: string[]
  requestSchemaJson?: string | null
  authConfigJson?: string | null
  runtimeConfigJson?: string | null
  testPayloadJson?: string | null
  remark?: string | null
}

export interface ToolStatistics {
  totalCount: number
  enabledCount: number
  publishedCount: number
  builtinCount: number
  externalCount: number
  highRiskCount: number
  totalLogCount: number
  successLogCount: number
  failureLogCount: number
}

export interface ToolDebugResult {
  toolId: number
  toolCode: string
  toolName: string
  successFlag: number
  responseText?: string | null
  failureReason?: string | null
  elapsedMs?: number | null
  requestPayloadJson?: string | null
  responsePayloadJson?: string | null
}

export interface ToolExecutionLogItem {
  id: number
  toolId?: number | null
  toolCode?: string | null
  toolName?: string | null
  sourceType?: string | null
  requestPayloadJson?: string | null
  responsePayloadJson?: string | null
  executeStatus?: string | null
  successFlag?: number | null
  elapsedMs?: number | null
  failureReason?: string | null
  operatorUserName?: string | null
  createTime?: number | null
}

export interface ToolDebugPayload {
  toolId: number | string
  requestPayloadJson?: string | null
  sourceType?: string | null
}

export interface ToolLogQueryPayload {
  toolId?: number | null
  sourceType?: string | null
  successFlag?: number | null
}
