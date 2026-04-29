import { apiClient, unwrapResponse } from '@/api/client'
import type {
  McpCatalogItem,
  McpDebugPayload,
  McpDebugResult,
  McpExecutionLogItem,
  McpItem,
  McpLogQueryPayload,
  McpPayload,
  McpStatistics,
} from '@/types/mcp'

export async function queryMcpServers() {
  const response = await apiClient.get('/mcp/servers')
  return unwrapResponse<McpItem[]>(response)
}

export async function fetchMcpServerDetail(serverId: number | string) {
  const response = await apiClient.get(`/mcp/servers/${serverId}`)
  return unwrapResponse<McpItem>(response)
}

export async function fetchMcpCatalog() {
  const response = await apiClient.get('/mcp/servers/catalog')
  return unwrapResponse<McpCatalogItem[]>(response)
}

export async function fetchMcpStats() {
  const response = await apiClient.post('/mcp/servers/statistics')
  return unwrapResponse<McpStatistics>(response)
}

export async function createMcpServer(payload: McpPayload) {
  const response = await apiClient.post('/mcp/servers', payload)
  return unwrapResponse<McpItem>(response)
}

export async function updateMcpServer(serverId: number | string, payload: McpPayload) {
  const response = await apiClient.put(`/mcp/servers/${serverId}`, payload)
  return unwrapResponse<McpItem>(response)
}

export async function removeMcpServer(serverId: number | string) {
  const response = await apiClient.delete(`/mcp/servers/${serverId}`)
  return unwrapResponse<void>(response)
}

export async function publishMcpServer(serverId: number | string) {
  const response = await apiClient.post(`/mcp/servers/${serverId}/publish`)
  return unwrapResponse<McpItem>(response)
}

export async function offlineMcpServer(serverId: number | string) {
  const response = await apiClient.post(`/mcp/servers/${serverId}/offline`)
  return unwrapResponse<McpItem>(response)
}

export async function debugMcpServer(payload: McpDebugPayload) {
  const response = await apiClient.post('/mcp/servers/debug', payload)
  return unwrapResponse<McpDebugResult>(response)
}

export async function queryMcpLogs(payload: McpLogQueryPayload) {
  const response = await apiClient.post('/mcp/servers/logs/query', payload)
  return unwrapResponse<McpExecutionLogItem[]>(response)
}
