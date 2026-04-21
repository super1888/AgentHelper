import { apiClient, unwrapResponse } from '@/api/client'
import type {
  ToolCatalogItem,
  ToolDebugPayload,
  ToolDebugResult,
  ToolExecutionLogItem,
  ToolItem,
  ToolLogQueryPayload,
  ToolPayload,
  ToolStatistics,
} from '@/types/tool'

export async function queryTools() {
  const response = await apiClient.get('/tools')
  return unwrapResponse<ToolItem[]>(response)
}

export async function fetchToolDetail(toolId: number | string) {
  const response = await apiClient.get(`/tools/${toolId}`)
  return unwrapResponse<ToolItem>(response)
}

export async function fetchToolCatalog() {
  const response = await apiClient.get('/tools/catalog')
  return unwrapResponse<ToolCatalogItem[]>(response)
}

export async function fetchToolStats() {
  const response = await apiClient.post('/tools/statistics')
  return unwrapResponse<ToolStatistics>(response)
}

export async function createTool(payload: ToolPayload) {
  const response = await apiClient.post('/tools', payload)
  return unwrapResponse<ToolItem>(response)
}

export async function updateTool(toolId: number | string, payload: ToolPayload) {
  const response = await apiClient.put(`/tools/${toolId}`, payload)
  return unwrapResponse<ToolItem>(response)
}

export async function removeTool(toolId: number | string) {
  const response = await apiClient.delete(`/tools/${toolId}`)
  return unwrapResponse<void>(response)
}

export async function publishTool(toolId: number | string) {
  const response = await apiClient.post(`/tools/${toolId}/publish`)
  return unwrapResponse<ToolItem>(response)
}

export async function offlineTool(toolId: number | string) {
  const response = await apiClient.post(`/tools/${toolId}/offline`)
  return unwrapResponse<ToolItem>(response)
}

export async function debugTool(payload: ToolDebugPayload) {
  const response = await apiClient.post('/tools/debug', payload)
  return unwrapResponse<ToolDebugResult>(response)
}

export async function queryToolLogs(payload: ToolLogQueryPayload) {
  const response = await apiClient.post('/tools/logs/query', payload)
  return unwrapResponse<ToolExecutionLogItem[]>(response)
}
