export interface McpCatalogItem {
  builtinServerKey: string
  serverName: string
  description?: string | null
  serverType: string
  transportType: string
  riskLevel: string
  authRequired?: number | null
  exposedToolNames?: string[]
  defaultRuntimeConfigJson?: string | null
  defaultAuthConfigJson?: string | null
  defaultTestPayloadJson?: string | null
  toolPromptHint?: string | null
}

export interface McpItem {
  serverId: number
  serverCode: string
  serverName: string
  description?: string | null
  serverType: string
  transportType: string
  serverStatus: string
  publishStatus: string
  riskLevel: string
  sortWeight?: number | null
  timeoutMs?: number | null
  authRequired?: number | null
  builtinServerKey?: string | null
  endpointUrl?: string | null
  tags?: string[]
  runtimeConfigJson?: string | null
  authConfigJson?: string | null
  testPayloadJson?: string | null
  toolPromptHint?: string | null
  remark?: string | null
  logCount?: number | null
}

export interface McpPayload {
  serverCode: string
  serverName: string
  description?: string | null
  serverType: string
  transportType: string
  serverStatus: string
  riskLevel: string
  sortWeight: number
  timeoutMs: number
  authRequired: number
  builtinServerKey?: string | null
  endpointUrl?: string | null
  tags?: string[]
  runtimeConfigJson?: string | null
  authConfigJson?: string | null
  testPayloadJson?: string | null
  toolPromptHint?: string | null
  remark?: string | null
}

export interface McpStatistics {
  totalCount: number
  enabledCount: number
  publishedCount: number
  builtinCount: number
  remoteCount: number
  highRiskCount: number
  totalLogCount: number
  successLogCount: number
  failureLogCount: number
}

export interface McpDebugResult {
  serverId: number
  serverCode: string
  serverName: string
  successFlag: number
  requestPayloadJson?: string | null
  responsePayloadJson?: string | null
  failureReason?: string | null
  elapsedMs?: number | null
}

export interface McpExecutionLogItem {
  logId: number
  serverId?: number | null
  serverCode?: string | null
  serverName?: string | null
  toolName?: string | null
  sourceType?: string | null
  executeStatus?: string | null
  successFlag?: number | null
  requestPayloadJson?: string | null
  responsePayloadJson?: string | null
  failureReason?: string | null
  elapsedMs?: number | null
  createTime?: number | null
}

export interface McpDebugPayload {
  serverId: number | string
  requestPayloadJson?: string | null
  sourceType?: string | null
}

export interface McpLogQueryPayload {
  serverId?: number | null
  sourceType?: string | null
  successFlag?: number | null
}
