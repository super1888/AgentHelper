import { apiClient, unwrapResponse } from '@/api/client'
import type {
  A2aAgentCardItem,
  A2aAgentCardPayload,
  A2aDispatchPayload,
  A2aLogItem,
  A2aRouteItem,
  A2aRoutePayload,
  A2aStatistics,
  A2aTaskItem,
} from '@/types/a2a'

export async function queryA2aAgents() {
  const response = await apiClient.get('/a2a/agents')
  return unwrapResponse<A2aAgentCardItem[]>(response)
}

export async function queryDeletedA2aAgents() {
  const response = await apiClient.get('/a2a/agents/deleted')
  return unwrapResponse<A2aAgentCardItem[]>(response)
}

export async function saveA2aAgent(payload: A2aAgentCardPayload) {
  const response = await apiClient.post('/a2a/agents', payload)
  return unwrapResponse<A2aAgentCardItem>(response)
}

export async function publishA2aAgent(agentId: number | string) {
  const response = await apiClient.post(`/a2a/agents/${agentId}/publish`)
  return unwrapResponse<A2aAgentCardItem>(response)
}

export async function removeA2aAgent(agentId: number | string) {
  const response = await apiClient.delete(`/a2a/agents/${agentId}`)
  return unwrapResponse<void>(response)
}

export async function restoreA2aAgent(agentId: number | string) {
  const response = await apiClient.post(`/a2a/agents/${agentId}/restore`)
  return unwrapResponse<A2aAgentCardItem>(response)
}

export async function queryA2aRoutes() {
  const response = await apiClient.get('/a2a/routes')
  return unwrapResponse<A2aRouteItem[]>(response)
}

export async function saveA2aRoute(payload: A2aRoutePayload) {
  const response = await apiClient.post('/a2a/routes', payload)
  return unwrapResponse<A2aRouteItem>(response)
}

export async function dispatchA2aTask(payload: A2aDispatchPayload) {
  const response = await apiClient.post('/a2a/dispatch', payload)
  return unwrapResponse<A2aTaskItem>(response)
}

export async function queryA2aTasks() {
  const response = await apiClient.get('/a2a/tasks')
  return unwrapResponse<A2aTaskItem[]>(response)
}

export async function queryA2aLogs(taskCode?: string | null) {
  const response = await apiClient.get('/a2a/logs', {
    params: taskCode ? { taskCode } : undefined,
  })
  return unwrapResponse<A2aLogItem[]>(response)
}

export async function fetchA2aStats() {
  const response = await apiClient.post('/a2a/statistics')
  return unwrapResponse<A2aStatistics>(response)
}
