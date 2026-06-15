export interface CodeHelperMessage {
  role: string
  content: string
  timestamp?: string | null
}

export interface CodeHelperTask {
  taskId: string
  title: string
  status: string
  detail?: string | null
  changedFiles: string[]
}

export interface CodeHelperSession {
  sessionId: string
  sessionName: string
  workspacePath: string
  projectName: string
  branchName: string
  taskDescription: string
  modelCode?: string | null
  status: string
  summary?: string | null
  messages: CodeHelperMessage[]
  tasks: CodeHelperTask[]
}

export interface CodeHelperSessionCreateRequest {
  sessionName?: string
  workspacePath: string
  projectName?: string
  branchName?: string
  taskDescription?: string
  modelCode?: string
  allowedCommands?: string[]
}

export interface CodeHelperMessageRequest {
  content: string
  modelCode?: string
  autoToolCall?: boolean
}

export interface CodeHelperContextResponse {
  sessionId: string
  summary?: string | null
  recentMessages: CodeHelperMessage[]
  tasks: CodeHelperTask[]
}

export interface CodeHelperToolDescriptor {
  toolName: string
  displayName: string
  description: string
  riskLevel: string
  argumentNames: string[]
}

export interface CodeHelperToolExecuteRequest {
  sessionId: string
  toolName: string
  workspacePath?: string
  arguments: Record<string, unknown>
  allowedCommands?: string[]
}

export interface CodeHelperToolExecutionResponse {
  sessionId: string
  toolName: string
  success: boolean
  riskLevel: string
  message: string
  output: string
  durationMillis: number
}

export interface CodeHelperToolLogResponse {
  logId: number
  sessionId: string
  toolName: string
  riskLevel: string
  success: boolean
  requestJson: string
  responseText?: string | null
  durationMillis?: number | null
  errorMessage?: string | null
  createTime?: string | null
}

export interface CodeHelperPermissionCheckRequest {
  toolName: string
  workspacePath?: string
  command?: string
  allowedCommands?: string[]
}

export interface CodeHelperPermissionDecisionResponse {
  toolName: string
  allowed: boolean
  reason: string
  riskLevel: string
}