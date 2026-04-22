import { apiClient, unwrapResponse } from '@/api/client'
import type {
  InterceptorBindingItem,
  InterceptorCatalogItem,
  InterceptorDebugPayload,
  InterceptorDebugResult,
  InterceptorExecutionLogItem,
  InterceptorItem,
  InterceptorPayload,
  InterceptorStatistics,
  InterceptorTestCaseItem,
} from '@/types/interceptor'

export async function queryInterceptors() {
  const response = await apiClient.get('/interceptors')
  return unwrapResponse<InterceptorItem[]>(response)
}

export async function queryDeletedInterceptors() {
  const response = await apiClient.get('/interceptors/deleted')
  return unwrapResponse<InterceptorItem[]>(response)
}

export async function fetchInterceptorDetail(interceptorId: number | string) {
  const response = await apiClient.get(`/interceptors/${interceptorId}`)
  return unwrapResponse<InterceptorItem>(response)
}

export async function fetchInterceptorCatalog() {
  const response = await apiClient.get('/interceptors/catalog')
  return unwrapResponse<InterceptorCatalogItem[]>(response)
}

export async function fetchInterceptorStats() {
  const response = await apiClient.post('/interceptors/statistics')
  return unwrapResponse<InterceptorStatistics>(response)
}

export async function createInterceptor(payload: InterceptorPayload) {
  const response = await apiClient.post('/interceptors', payload)
  return unwrapResponse<InterceptorItem>(response)
}

export async function updateInterceptor(interceptorId: number | string, payload: InterceptorPayload) {
  const response = await apiClient.put(`/interceptors/${interceptorId}`, payload)
  return unwrapResponse<InterceptorItem>(response)
}

export async function removeInterceptor(interceptorId: number | string) {
  const response = await apiClient.delete(`/interceptors/${interceptorId}`)
  return unwrapResponse<void>(response)
}

export async function restoreInterceptor(interceptorId: number | string) {
  const response = await apiClient.post(`/interceptors/${interceptorId}/restore`)
  return unwrapResponse<InterceptorItem>(response)
}

export async function publishInterceptor(interceptorId: number | string) {
  const response = await apiClient.post(`/interceptors/${interceptorId}/publish`)
  return unwrapResponse<InterceptorItem>(response)
}

export async function offlineInterceptor(interceptorId: number | string) {
  const response = await apiClient.post(`/interceptors/${interceptorId}/offline`)
  return unwrapResponse<InterceptorItem>(response)
}

export async function hotUpdateInterceptor(interceptorId: number | string) {
  const response = await apiClient.post(`/interceptors/${interceptorId}/hot-update`)
  return unwrapResponse<InterceptorItem>(response)
}

export async function debugInterceptor(payload: InterceptorDebugPayload) {
  const response = await apiClient.post('/interceptors/debug', payload)
  return unwrapResponse<InterceptorDebugResult>(response)
}

export async function queryInterceptorLogs(payload: { interceptorId?: number | null; sourceType?: string | null; successFlag?: number | null }) {
  const response = await apiClient.post('/interceptors/logs/query', payload)
  return unwrapResponse<InterceptorExecutionLogItem[]>(response)
}

export async function queryInterceptorTestCases(interceptorId: number | string) {
  const response = await apiClient.get(`/interceptors/${interceptorId}/test-cases`)
  return unwrapResponse<InterceptorTestCaseItem[]>(response)
}

export async function createInterceptorTestCase(interceptorId: number | string, payload: Record<string, unknown>) {
  const response = await apiClient.post(`/interceptors/${interceptorId}/test-cases`, payload)
  return unwrapResponse<InterceptorTestCaseItem>(response)
}

export async function runInterceptorTestCase(testCaseId: number | string) {
  const response = await apiClient.post(`/interceptors/test-cases/${testCaseId}/run`)
  return unwrapResponse<InterceptorDebugResult>(response)
}

export async function queryInterceptorBindings(interceptorId: number | string) {
  const response = await apiClient.get(`/interceptors/${interceptorId}/bindings`)
  return unwrapResponse<InterceptorBindingItem[]>(response)
}

export async function createInterceptorBinding(interceptorId: number | string, payload: Record<string, unknown>) {
  const response = await apiClient.post(`/interceptors/${interceptorId}/bindings`, payload)
  return unwrapResponse<InterceptorBindingItem>(response)
}
