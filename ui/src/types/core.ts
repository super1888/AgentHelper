export interface ProviderCatalogItem {
  providerEnum: string
  providerLabel: string
}

export interface ModelOption {
  modelCode: string
  modelName: string
  providerConfigCode: string
  providerEnum: string
  providerName: string | null
  modelIdentifier: string
  modelType: string
  defaultModel: boolean
}

export interface ModelConnectionItem {
  modelCode: string
  providerConfigCode: string
  connectionName: string
  providerEnum: string
  baseUrl: string | null
  organizationId: string | null
  defaultHeadersJson: string | null
  apiKeyMasked: string | null
  apiKeyConfigured: boolean
  modelType: string
  modelIdentifier: string
  temperature: number | null
  topP: number | null
  presencePenalty: number | null
  frequencyPenalty: number | null
  maxTokens: number | null
  contextWindow: number | null
  rpmLimit: number | null
  tpmLimit: number | null
  timeoutMs: number | null
  supportStreaming: boolean
  supportTools: boolean
  supportVision: boolean
  supportJsonSchema: boolean
  defaultModel: boolean
  advancedConfigJson: string | null
  status: string
  remark: string | null
  updateTime: number | null
}

export interface ModelConnectionPayload {
  modelCode?: string
  connectionName: string
  providerEnum: string
  baseUrl?: string | null
  apiKey?: string | null
  organizationId?: string | null
  defaultHeadersJson?: string | null
  modelType?: string | null
  modelIdentifier: string
  temperature?: number | null
  topP?: number | null
  presencePenalty?: number | null
  frequencyPenalty?: number | null
  maxTokens?: number | null
  contextWindow?: number | null
  rpmLimit?: number | null
  tpmLimit?: number | null
  timeoutMs?: number | null
  supportStreaming?: boolean | null
  supportTools?: boolean | null
  supportVision?: boolean | null
  supportJsonSchema?: boolean | null
  defaultModel?: boolean | null
  advancedConfigJson?: string | null
  status?: string | null
  remark?: string | null
}

export interface ModelConnectionTestPayload {
  modelCode?: string
  providerEnum?: string
  baseUrl?: string | null
  apiKey?: string | null
  modelIdentifier?: string
  temperature?: number | null
  topP?: number | null
  presencePenalty?: number | null
  frequencyPenalty?: number | null
  maxTokens?: number | null
  testPrompt?: string | null
}

export interface ModelTestResult {
  success: boolean
  providerEnum: string
  modelIdentifier: string
  responseContent: string
  elapsedMs: number
}
