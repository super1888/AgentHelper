import { apiClient, unwrapResponse } from '@/api/client'
import type { ModelOption } from '@/types/core'
import type {
  CodeHelperContextResponse,
  CodeHelperMessageRequest,
  CodeHelperPermissionCheckRequest,
  CodeHelperPermissionDecisionResponse,
  CodeHelperSession,
  CodeHelperSessionCreateRequest,
  CodeHelperToolDescriptor,
  CodeHelperToolExecuteRequest,
  CodeHelperToolExecutionResponse,
  CodeHelperToolLogResponse,
} from '@/types/codeHelper'

export async function createCodeHelperSession(payload: CodeHelperSessionCreateRequest) {
  const response = await apiClient.post('/code-helper/sessions', payload)
  return unwrapResponse<CodeHelperSession>(response)
}

export async function queryCodeHelperSessions() {
  const response = await apiClient.get('/code-helper/sessions')
  return unwrapResponse<CodeHelperSession[]>(response)
}

export async function sendCodeHelperMessage(sessionId: string, payload: CodeHelperMessageRequest) {
  const response = await apiClient.post('/code-helper/sessions/send', payload, { params: { sessionId } })
  return unwrapResponse<CodeHelperSession>(response)
}

export async function queryCodeHelperContext(sessionId: string) {
  const response = await apiClient.get('/code-helper/context', { params: { sessionId } })
  return unwrapResponse<CodeHelperContextResponse>(response)
}

export async function compactCodeHelperContext(sessionId: string, summaryHint?: string) {
  const response = await apiClient.post('/code-helper/context/compact', { summaryHint }, { params: { sessionId } })
  return unwrapResponse<CodeHelperContextResponse>(response)
}

export async function queryCodeHelperPrompt(sessionId: string) {
  const response = await apiClient.get('/code-helper/prompt', { params: { sessionId } })
  return unwrapResponse<string>(response)
}

export async function queryCodeHelperTools() {
  const response = await apiClient.get('/code-helper/tools')
  return unwrapResponse<CodeHelperToolDescriptor[]>(response)
}

export async function queryCodeHelperModelOptions() {
  const response = await apiClient.get('/code-helper/models/options')
  return unwrapResponse<ModelOption[]>(response)
}

export async function executeCodeHelperTool(payload: CodeHelperToolExecuteRequest) {
  const response = await apiClient.post('/code-helper/tool/execute', payload)
  return unwrapResponse<CodeHelperToolExecutionResponse>(response)
}

export async function queryCodeHelperToolLogs(sessionId?: string) {
  const response = await apiClient.get('/code-helper/tool/logs', { params: sessionId ? { sessionId } : undefined })
  return unwrapResponse<CodeHelperToolLogResponse[]>(response)
}

export async function checkCodeHelperPermission(payload: CodeHelperPermissionCheckRequest) {
  const response = await apiClient.post('/code-helper/permission/check', payload)
  return unwrapResponse<CodeHelperPermissionDecisionResponse>(response)
}
