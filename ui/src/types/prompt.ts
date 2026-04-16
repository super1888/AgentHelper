export type PromptTemplateSourceType = 'INLINE_TEXT' | 'FILE_PATH'
export type PromptTemplateStatus = 'ENABLED' | 'DISABLED'

export interface PromptTemplateVariable {
  variableName: string
  required: boolean
  defaultValue: string | null
  description: string | null
}

export interface PromptTemplateItem {
  id: number
  templateCode: string
  templateName: string
  description: string | null
  templateType: string
  sourceType: PromptTemplateSourceType
  templateContent: string | null
  sourcePath: string | null
  templateStatus: PromptTemplateStatus
  ownerUserId: number | null
  ownerUserName: string | null
  variableDefinitions: PromptTemplateVariable[]
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
}
