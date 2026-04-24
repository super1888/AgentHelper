import { apiClient, unwrapResponse } from '@/api/client'
import type {
  ModelConnectionItem,
  ModelConnectionPayload,
  ModelConnectionTestPayload,
  ModelOption,
  ModelTestResult,
  ProviderCatalogItem,
} from '@/types/core'

export async function queryProviderCatalog() {
  const response = await apiClient.get('/core/provider-catalog')
  return unwrapResponse<ProviderCatalogItem[]>(response)
}

export async function queryModelConnections() {
  const response = await apiClient.get('/core/model-connections')
  return unwrapResponse<ModelConnectionItem[]>(response)
}

export async function saveModelConnection(payload: ModelConnectionPayload) {
  const response = await apiClient.post('/core/model-connections', payload)
  return unwrapResponse<ModelConnectionItem>(response)
}

export async function removeModelConnection(modelCode: string) {
  const response = await apiClient.delete(`/core/model-connections/${modelCode}`)
  return unwrapResponse<void>(response)
}

export async function testModelConnection(payload: ModelConnectionTestPayload) {
  const response = await apiClient.post('/core/model-connections/test', payload)
  return unwrapResponse<ModelTestResult>(response)
}

export async function queryEnabledModels() {
  const response = await apiClient.get('/core/models/options')
  return unwrapResponse<ModelOption[]>(response)
}
