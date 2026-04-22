export interface A2aAgentCardItem {
  id: number
  agentCode: string
  agentName: string
  description?: string | null
  endpointUrl: string
  protocolVersion: string
  transportType: string
  authType: string
  agentStatus: string
  publishStatus: string
  riskLevel: string
  trustLevel: string
  ownerTeam?: string | null
  timeoutMs?: number | null
  retryTimes?: number | null
  rateLimitQps?: number | null
  successRateSlo?: number | null
  capabilities?: string[]
  inputModes?: string[]
  outputModes?: string[]
  authConfig?: Record<string, unknown> | null
  metadata?: Record<string, unknown> | null
  remark?: string | null
  createTime?: number | null
  updateTime?: number | null
}

export interface A2aAgentCardPayload {
  agentCode: string
  agentName: string
  description?: string | null
  endpointUrl: string
  protocolVersion: string
  transportType: string
  authType: string
  agentStatus: string
  riskLevel: string
  trustLevel: string
  ownerTeam?: string | null
  timeoutMs: number
  retryTimes: number
  rateLimitQps: number
  successRateSlo: number
  capabilities: string[]
  inputModes: string[]
  outputModes: string[]
  authConfig: Record<string, unknown>
  metadata: Record<string, unknown>
  remark?: string | null
}

export interface A2aRouteItem {
  id: number
  routeCode: string
  routeName: string
  sourceAgentCode?: string | null
  targetAgentCode: string
  taskType: string
  routeStatus: string
  priorityNo?: number | null
  failoverEnabled?: number | null
  fallbackAgentCodes?: string | null
  remark?: string | null
  createTime?: number | null
}

export interface A2aRoutePayload {
  routeCode: string
  routeName: string
  sourceAgentCode?: string | null
  targetAgentCode: string
  taskType: string
  routeStatus: string
  priorityNo: number
  failoverEnabled: number
  fallbackAgentCodes?: string | null
  remark?: string | null
}

export interface A2aDispatchPayload {
  sourceAgentCode?: string | null
  targetAgentCode?: string | null
  taskType: string
  payload: Record<string, unknown>
}

export interface A2aTaskItem {
  id: number
  taskCode: string
  taskType: string
  sourceAgentCode?: string | null
  targetAgentCode: string
  routeCode?: string | null
  taskStatus: string
  requestPayloadJson?: string | null
  responsePayloadJson?: string | null
  failureReason?: string | null
  elapsedMs?: number | null
  createTime?: number | null
}

export interface A2aLogItem {
  id: number
  taskCode: string
  traceId: string
  sourceAgentCode?: string | null
  targetAgentCode: string
  routeCode?: string | null
  eventType: string
  executeStatus?: string | null
  attemptNo?: number | null
  retryIndex?: number | null
  successFlag?: number | null
  elapsedMs?: number | null
  failureReason?: string | null
  createTime?: number | null
}

export interface A2aStatistics {
  agentCount: number
  publishedAgentCount: number
  routeCount: number
  taskCount: number
  successTaskCount: number
  failedTaskCount: number
  logCount: number
}
