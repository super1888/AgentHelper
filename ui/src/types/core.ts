export interface ProviderCatalogItem {
  providerEnum: string
  providerLabel: string
}

export interface ModelProviderItem {
  providerConfigCode: string
  providerEnum: string
  providerName: string
  baseUrl: string | null
  organizationId: string | null
  defaultHeadersJson: string | null
  status: string
  apiKeyMasked: string | null
  apiKeyConfigured: boolean
  ownerUserName: string | null
  updateTime: number | null
  remark: string | null
}

export interface ModelProviderPayload {
  providerEnum: string
  providerName: string
  baseUrl?: string | null
  apiKey?: string | null
  organizationId?: string | null
  defaultHeadersJson?: string | null
  remark?: string | null
  status?: string | null
}

export interface ModelDefinitionItem {
  modelCode: string
  modelName: string
  providerConfigCode: string
  providerEnum: string
  providerName: string | null
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
  status: string
  advancedConfigJson: string | null
  remark: string | null
  updateTime: number | null
}

export interface ModelDefinitionPayload {
  modelName: string
  providerConfigCode: string
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
  remark?: string | null
  status?: string | null
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

export interface ModelTestPayload {
  providerConfigCode?: string
  providerEnum?: string
  baseUrl?: string | null
  apiKey?: string | null
  testModelIdentifier?: string
  testPrompt?: string | null
}

export interface ModelTestResult {
  success: boolean
  providerEnum: string
  modelIdentifier: string
  responseContent: string
  elapsedMs: number
}
