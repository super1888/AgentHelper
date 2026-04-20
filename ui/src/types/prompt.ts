export type PromptTemplateSourceType = 'INLINE_TEXT' | 'FILE_PATH'
export type PromptTemplateStatus = 'ENABLED' | 'DISABLED'

export interface PromptTemplateVariable {
  variableName: string
  required: boolean
  defaultValue: string | null
  description: string | null
}

export interface PromptConditionalRule {
  name: string | null
  conditionExpression: string | null
  trueTemplate: string | null
  falseTemplate: string | null
}

export interface PromptLoopRule {
  listVariable: string | null
  itemAlias: string | null
  emptyTemplate: string | null
  itemTemplate: string | null
}

export interface PromptToolRule {
  toolCode: string | null
  triggerCondition: string | null
  parameterSpec: string | null
  permissionScope: string | null
}

export interface PromptRenderingPolicy {
  dynamicVariables: string[]
  dataSources: string[]
  conditionalBranches: PromptConditionalRule[]
  loopRenderers: PromptLoopRule[]
}

export interface PromptRolePolicy {
  agentRole: string | null
  dutyScope: string | null
  forbiddenActions: string[]
  tone: string | null
  speechRules: string[]
}

export interface PromptWorkflowPolicy {
  workflowStages: string[]
  hardRules: string[]
  toolRules: PromptToolRule[]
}

export interface PromptSecurityPolicy {
  desensitizationRules: string[]
  antiInjectionRules: string[]
  complianceBlacklist: string[]
  permissionTiers: string[]
}

export interface PromptAssetPolicy {
  commonModules: string[]
  businessModules: string[]
  versionStrategy: string | null
  permissionStrategy: string | null
  categories: string[]
}

export interface PromptOutputPolicy {
  outputFormat: string | null
  requiredFields: string[]
  maxLength: number | null
  channelConstraints: string[]
}

export interface PromptContextPolicy {
  historyStrategy: string | null
  memoryFields: string[]
  sessionIsolation: boolean
  retentionStrategy: string | null
}

export interface PromptFallbackPolicy {
  fallbackMessages: string[]
  repeatedRules: string[]
  supportedLanguages: string[]
  resilienceStrategy: string | null
}

export interface PromptObservabilityPolicy {
  traceEnabled: boolean
  metricKeys: string[]
  logBindingFields: string[]
  grayReleaseStrategy: string | null
}

export interface PromptIntegrationPolicy {
  externalSystems: string[]
  parameterBindings: string[]
  batchScenarios: string[]
  editorMode: string | null
}

export interface PromptEnterpriseConfig {
  rendering: PromptRenderingPolicy
  rolePolicy: PromptRolePolicy
  workflowPolicy: PromptWorkflowPolicy
  securityPolicy: PromptSecurityPolicy
  assetPolicy: PromptAssetPolicy
  outputPolicy: PromptOutputPolicy
  contextPolicy: PromptContextPolicy
  fallbackPolicy: PromptFallbackPolicy
  observabilityPolicy: PromptObservabilityPolicy
  integrationPolicy: PromptIntegrationPolicy
}

export interface PromptTemplateItem {
  id: string
  templateCode: string
  templateName: string
  description: string | null
  templateType: string
  sourceType: PromptTemplateSourceType
  templateContent: string | null
  sourcePath: string | null
  templateStatus: PromptTemplateStatus
  ownerUserId: string | null
  ownerUserName: string | null
  variableDefinitions: PromptTemplateVariable[]
  enterpriseConfig: PromptEnterpriseConfig | null
  createTime: number | null
  updateTime: number | null
}

export interface PromptTemplateStatistics {
  totalCount: number
  enabledCount: number
  disabledCount: number
  inlineCount: number
  fileCount: number
}

export interface PromptTemplatePayload {
  templateCode?: string
  templateName: string
  description: string | null
  sourceType: PromptTemplateSourceType
  templateContent: string | null
  sourcePath: string | null
  templateStatus?: PromptTemplateStatus
  variableDefinitions?: PromptTemplateVariable[]
  enterpriseConfig?: PromptEnterpriseConfig | null
}

export interface PromptTemplateRenderPayload {
  variables: Record<string, unknown>
}

export interface PromptTemplateRenderResult {
  renderedContent: string
  missingVariables: string[]
  appliedConditions: string[]
  appliedLoops: string[]
}

export function createEmptyEnterpriseConfig(): PromptEnterpriseConfig {
  return {
    rendering: {
      dynamicVariables: [],
      dataSources: [],
      conditionalBranches: [],
      loopRenderers: [],
    },
    rolePolicy: {
      agentRole: null,
      dutyScope: null,
      forbiddenActions: [],
      tone: null,
      speechRules: [],
    },
    workflowPolicy: {
      workflowStages: [],
      hardRules: [],
      toolRules: [],
    },
    securityPolicy: {
      desensitizationRules: [],
      antiInjectionRules: [],
      complianceBlacklist: [],
      permissionTiers: [],
    },
    assetPolicy: {
      commonModules: [],
      businessModules: [],
      versionStrategy: null,
      permissionStrategy: null,
      categories: [],
    },
    outputPolicy: {
      outputFormat: null,
      requiredFields: [],
      maxLength: null,
      channelConstraints: [],
    },
    contextPolicy: {
      historyStrategy: null,
      memoryFields: [],
      sessionIsolation: true,
      retentionStrategy: null,
    },
    fallbackPolicy: {
      fallbackMessages: [],
      repeatedRules: [],
      supportedLanguages: [],
      resilienceStrategy: null,
    },
    observabilityPolicy: {
      traceEnabled: true,
      metricKeys: [],
      logBindingFields: [],
      grayReleaseStrategy: null,
    },
    integrationPolicy: {
      externalSystems: [],
      parameterBindings: [],
      batchScenarios: [],
      editorMode: null,
    },
  }
}
