export interface AgentSummary {
  agentId: string
  agentName: string
  description: string | null
  agentType: string
  agentStatus: string
  currentVersionNo: number | null
  publishedVersionNo: number | null
  ownerUserName: string | null
}

export interface AgentVersion {
  versionId: number
  versionNo: number
  agentName: string
  description: string | null
  systemPrompt: string | null
  selectedCapabilities: string[]
  published: boolean
  createTime: number | null
}

export interface AgentDetail {
  agentId: string
  agentName: string
  description: string | null
  agentType: string
  agentStatus: string
  currentVersionNo: number | null
  publishedVersionNo: number | null
  ownerUserId: number
  ownerUserName: string | null
  versions: AgentVersion[]
}

export interface AgentCreatePayload {
  agentName: string
  description: string | null
  systemPrompt: string | null
  selectedCapabilities: string[]
  agentType?: string
}

export interface AgentCreateResult {
  agentId: string
  agentName: string
  description: string | null
  selectedCapabilities: string[]
  currentVersionNo: number | null
  publishedVersionNo: number | null
  websocketEndpoint: string
  websocketTopic: string
  websocketSendDestination: string
}

export interface AgentSessionPayload {
  versionNo?: number
}

export interface AgentSessionResult {
  sessionId: string
  agentId: string
  agentVersionNo: number
  agentVersionId: number
  sessionStatus: string
  connectionStatus: string
  lastEventSequence: number
  websocketEndpoint: string
  websocketTopic: string
  websocketSendDestination: string
}

export interface AgentReconnectPayload {
  lastReceivedEventSequence?: number
}

export interface AgentChatPayload {
  agentId: string
  sessionId: string
  message: string
  lastReceivedEventSequence?: number
}

export interface AgentChatEvent {
  agentId: string
  sessionId: string
  taskId: string | null
  agentVersionId: number
  agentVersionNo: number
  eventSequence: number
  event: string
  data: unknown
  timestamp: number
}

export interface AgentReconnectResult {
  session: AgentSessionResult
  missedEvents: AgentChatEvent[]
}

export interface AgentRecoverPayload {
  taskId?: string
}

export interface AgentRecoverResult {
  sessionId: string
  taskId: string
  taskStatus: string
  message: string
}
