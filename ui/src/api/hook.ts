import { apiClient, unwrapResponse } from '@/api/client'
import type {
  HookBindingItem,
  HookCatalogItem,
  HookDebugPayload,
  HookDebugResult,
  HookExecutionLogItem,
  HookItem,
  HookPayload,
  HookStatistics,
  HookTestCaseItem,
} from '@/types/hook'

export async function queryHooks() {
  const response = await apiClient.get('/hooks')
  return unwrapResponse<HookItem[]>(response)
}

export async function queryDeletedHooks() {
  const response = await apiClient.get('/hooks/deleted')
  return unwrapResponse<HookItem[]>(response)
}

export async function fetchHookDetail(hookId: number | string) {
  const response = await apiClient.get(`/hooks/${hookId}`)
  return unwrapResponse<HookItem>(response)
}

export async function fetchHookCatalog() {
  const response = await apiClient.get('/hooks/catalog')
  return unwrapResponse<HookCatalogItem[]>(response)
}

export async function fetchHookStats() {
  const response = await apiClient.post('/hooks/statistics')
  return unwrapResponse<HookStatistics>(response)
}

export async function createHook(payload: HookPayload) {
  const response = await apiClient.post('/hooks', payload)
  return unwrapResponse<HookItem>(response)
}

export async function updateHook(hookId: number | string, payload: HookPayload) {
  const response = await apiClient.put(`/hooks/${hookId}`, payload)
  return unwrapResponse<HookItem>(response)
}

export async function removeHook(hookId: number | string) {
  const response = await apiClient.delete(`/hooks/${hookId}`)
  return unwrapResponse<void>(response)
}

export async function publishHook(hookId: number | string) {
  const response = await apiClient.post(`/hooks/${hookId}/publish`)
  return unwrapResponse<HookItem>(response)
}

export async function offlineHook(hookId: number | string) {
  const response = await apiClient.post(`/hooks/${hookId}/offline`)
  return unwrapResponse<HookItem>(response)
}

export async function hotUpdateHook(hookId: number | string) {
  const response = await apiClient.post(`/hooks/${hookId}/hot-update`)
  return unwrapResponse<HookItem>(response)
}

export async function debugHook(payload: HookDebugPayload) {
  const response = await apiClient.post('/hooks/debug', payload)
  return unwrapResponse<HookDebugResult>(response)
}

export async function queryHookLogs(payload: { hookId?: number | null; sourceType?: string | null; successFlag?: number | null }) {
  const response = await apiClient.post('/hooks/logs/query', payload)
  return unwrapResponse<HookExecutionLogItem[]>(response)
}

export async function queryHookTestCases(hookId: number | string) {
  const response = await apiClient.get(`/hooks/${hookId}/test-cases`)
  return unwrapResponse<HookTestCaseItem[]>(response)
}

export async function createHookTestCase(hookId: number | string, payload: Record<string, unknown>) {
  const response = await apiClient.post(`/hooks/${hookId}/test-cases`, payload)
  return unwrapResponse<HookTestCaseItem>(response)
}

export async function runHookTestCase(testCaseId: number | string) {
  const response = await apiClient.post(`/hooks/test-cases/${testCaseId}/run`)
  return unwrapResponse<HookDebugResult>(response)
}

export async function queryHookBindings(hookId: number | string) {
  const response = await apiClient.get(`/hooks/${hookId}/bindings`)
  return unwrapResponse<HookBindingItem[]>(response)
}

export async function createHookBinding(hookId: number | string, payload: Record<string, unknown>) {
  const response = await apiClient.post(`/hooks/${hookId}/bindings`, payload)
  return unwrapResponse<HookBindingItem>(response)
}
