import { apiClient, unwrapResponse } from '@/api/client'
import type {
  AgentChatPayload,
  AgentCreatePayload,
  AgentCreateResult,
  AgentDetail,
  AgentReconnectPayload,
  AgentReconnectResult,
  AgentRecoverPayload,
  AgentRecoverResult,
  AgentSessionPayload,
  AgentSessionResult,
  AgentSummary,
} from '@/types/agent'

export async function queryAgents() {
  const response = await apiClient.get('/agents/simple')
  return unwrapResponse<AgentSummary[]>(response)
}

export async function fetchAgentDetail(agentId: string) {
  const response = await apiClient.get(`/agents/simple/${agentId}`)
  return unwrapResponse<AgentDetail>(response)
}

export async function createAgent(payload: AgentCreatePayload) {
  const response = await apiClient.post('/agents/simple', payload)
  return unwrapResponse<AgentCreateResult>(response)
}

export async function publishAgent(agentId: string, versionNo?: number) {
  const response = await apiClient.post(`/agents/simple/${agentId}/publish`, null, {
    params: versionNo ? { versionNo } : undefined,
  })
  return unwrapResponse<void>(response)
}

export async function disableAgent(agentId: string) {
  const response = await apiClient.post(`/agents/simple/${agentId}/disable`)
  return unwrapResponse<void>(response)
}

export async function createAgentSession(agentId: string, payload?: AgentSessionPayload) {
  const response = await apiClient.post(`/agents/simple/${agentId}/sessions`, payload ?? {})
  return unwrapResponse<AgentSessionResult>(response)
}

export async function reconnectAgentSession(sessionId: string, payload?: AgentReconnectPayload) {
  const response = await apiClient.post(`/agents/simple/sessions/${sessionId}/reconnect`, payload ?? {})
  return unwrapResponse<AgentReconnectResult>(response)
}

export async function closeAgentSession(sessionId: string) {
  const response = await apiClient.post(`/agents/simple/sessions/${sessionId}/close`)
  return unwrapResponse<void>(response)
}

export async function recoverAgentTask(sessionId: string, payload?: AgentRecoverPayload) {
  const response = await apiClient.post(`/agents/simple/sessions/${sessionId}/recover`, payload ?? {})
  return unwrapResponse<AgentRecoverResult>(response)
}

export function buildAgentChatPayload(payload: AgentChatPayload) {
  return payload
}
