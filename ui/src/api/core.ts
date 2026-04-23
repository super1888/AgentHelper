import { apiClient, unwrapResponse } from '@/api/client'
import type {
  ModelDefinitionItem,
  ModelDefinitionPayload,
  ModelOption,
  ModelProviderItem,
  ModelProviderPayload,
  ModelTestPayload,
  ModelTestResult,
  ProviderCatalogItem,
} from '@/types/core'

export async function queryProviderCatalog() {
  const response = await apiClient.get('/core/provider-catalog')
  return unwrapResponse<ProviderCatalogItem[]>(response)
}

export async function queryModelProviders() {
  const response = await apiClient.get('/core/model-providers')
  return unwrapResponse<ModelProviderItem[]>(response)
}

export async function createModelProvider(payload: ModelProviderPayload) {
  const response = await apiClient.post('/core/model-providers', payload)
  return unwrapResponse<ModelProviderItem>(response)
}

export async function updateModelProvider(providerConfigCode: string, payload: ModelProviderPayload) {
  const response = await apiClient.patch(`/core/model-providers/${providerConfigCode}`, payload)
  return unwrapResponse<ModelProviderItem>(response)
}

export async function removeModelProvider(providerConfigCode: string) {
  const response = await apiClient.delete(`/core/model-providers/${providerConfigCode}`)
  return unwrapResponse<void>(response)
}

export async function testModelProvider(payload: ModelTestPayload) {
  const response = await apiClient.post('/core/model-providers/test', payload)
  return unwrapResponse<ModelTestResult>(response)
}

export async function queryModels(enabledOnly = false) {
  const response = await apiClient.get('/core/models', {
    params: enabledOnly ? { enabledOnly: true } : undefined,
  })
  return unwrapResponse<ModelDefinitionItem[]>(response)
}

export async function queryEnabledModels() {
  const response = await apiClient.get('/core/models/options')
  return unwrapResponse<ModelOption[]>(response)
}

export async function createModelDefinition(payload: ModelDefinitionPayload) {
  const response = await apiClient.post('/core/models', payload)
  return unwrapResponse<ModelDefinitionItem>(response)
}

export async function updateModelDefinition(modelCode: string, payload: ModelDefinitionPayload) {
  const response = await apiClient.patch(`/core/models/${modelCode}`, payload)
  return unwrapResponse<ModelDefinitionItem>(response)
}

export async function removeModelDefinition(modelCode: string) {
  const response = await apiClient.delete(`/core/models/${modelCode}`)
  return unwrapResponse<void>(response)
}

export async function testModelDefinition(modelCode: string, payload?: Pick<ModelTestPayload, 'testPrompt'>) {
  const response = await apiClient.post(`/core/models/${modelCode}/test`, payload ?? {})
  return unwrapResponse<ModelTestResult>(response)
}
