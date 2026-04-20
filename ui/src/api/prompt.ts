import { apiClient, unwrapResponse } from '@/api/client'
import type {
  PromptTemplateItem,
  PromptTemplatePayload,
  PromptTemplateRenderPayload,
  PromptTemplateRenderResult,
  PromptTemplateStatistics,
} from '@/types/prompt'

export async function queryPromptTemplates() {
  const response = await apiClient.get('/promptTemplates')
  return unwrapResponse<PromptTemplateItem[]>(response)
}

export async function fetchPromptTemplateDetail(promptTemplateId: number | string) {
  const response = await apiClient.get(`/promptTemplates/${promptTemplateId}`)
  return unwrapResponse<PromptTemplateItem>(response)
}

export async function fetchPromptTemplateStats() {
  const response = await apiClient.post('/promptTemplates/statistics')
  return unwrapResponse<PromptTemplateStatistics>(response)
}

export async function createPromptTemplate(payload: PromptTemplatePayload) {
  const response = await apiClient.post('/promptTemplates', payload)
  return unwrapResponse<PromptTemplateItem>(response)
}

export async function updatePromptTemplate(promptTemplateId: number | string, payload: PromptTemplatePayload) {
  const response = await apiClient.put(`/promptTemplates/${promptTemplateId}`, payload)
  return unwrapResponse<PromptTemplateItem>(response)
}

export async function removePromptTemplate(promptTemplateId: number | string) {
  const response = await apiClient.delete(`/promptTemplates/${promptTemplateId}`)
  return unwrapResponse<void>(response)
}

export async function renderPromptTemplate(promptTemplateId: number | string, payload: PromptTemplateRenderPayload) {
  const response = await apiClient.post(`/promptTemplates/${promptTemplateId}/render`, payload)
  return unwrapResponse<PromptTemplateRenderResult>(response)
}
