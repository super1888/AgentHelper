import type { PromptTemplateVariable } from '@/types/prompt'

export interface AgentPromptConfig {
  promptTemplateId?: string | null
  promptTemplateCode?: string | null
  promptTemplateName?: string | null
  promptBindingType?: string | null
  promptSourceType?: string | null
  promptTemplatePath?: string | null
  promptTemplateContent?: string | null
  promptVariableDefinitions?: PromptTemplateVariable[] | null
  promptVariables?: Record<string, string> | null
}

export interface AgentSummary {
  agentId: string
  agentName: string
  description: string | null
  agentType: string
  agentStatus: string
  currentVersionNo: number | null
  publishedVersionNo: number | null
  ownerUserName: string | null
  modelCode?: string | null
  modelName?: string | null
  providerName?: string | null
}

export interface AgentVersion {
  versionId: string
  versionNo: number
  agentName: string
  description: string | null
  systemPrompt: string | null
  selectedCapabilities: string[]
  selectedHookCodes: string[]
  promptTemplateId?: string | null
  promptTemplateCode?: string | null
  promptTemplateName?: string | null
  promptBindingType?: string | null
  promptSourceType?: string | null
  promptTemplatePath?: string | null
  promptTemplateContent?: string | null
  promptVariableDefinitions?: PromptTemplateVariable[] | null
  promptVariables?: Record<string, string> | null
  modelCode?: string | null
  modelName?: string | null
  providerConfigCode?: string | null
  providerEnum?: string | null
  providerName?: string | null
  modelIdentifier?: string | null
  modelType?: string | null
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
  ownerUserId: string
  ownerUserName: string | null
  versions: AgentVersion[]
}

export interface AgentCreatePayload {
  agentName: string
  description: string | null
  systemPrompt: string | null
  selectedCapabilities: string[]
  selectedHookCodes: string[]
  agentType?: string
  promptConfig?: AgentPromptConfig | null
  modelConfigCode: string
}

export interface AgentCreateResult {
  agentId: string
  agentName: string
  description: string | null
  selectedCapabilities: string[]
  selectedHookCodes: string[]
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
  agentVersionId: string
  sessionStatus: string
  connectionStatus: string
  lastEventSequence: string
  websocketEndpoint: string
  websocketTopic: string
  websocketSendDestination: string
}

export interface AgentReconnectPayload {
  lastReceivedEventSequence?: string
}

export interface AgentChatPayload {
  agentId: string
  sessionId: string
  message: string
  lastReceivedEventSequence?: string
}

export interface AgentChatEvent {
  agentId: string
  sessionId: string
  taskId: string | null
  agentVersionId: string
  agentVersionNo: number
  eventSequence: string
  event: string
  data: unknown
  timestamp: string
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

export interface AgentBatchMigratePayload {
  agentIds: string[]
  targetModelConfigCode: string
  migrationMode?: 'DRAFT_ONLY' | 'PUBLISH_NEW_VERSION'
}
