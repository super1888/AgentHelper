import { apiClient, unwrapResponse } from '@/api/client'
import type { SkillExportResult, SkillItem, SkillPayload, SkillStatistics } from '@/types/skill'

export async function querySkills() {
  const response = await apiClient.get('/skills')
  return unwrapResponse<SkillItem[]>(response)
}

export async function fetchSkillDetail(skillId: number | string) {
  const response = await apiClient.get(`/skills/${skillId}`)
  return unwrapResponse<SkillItem>(response)
}

export async function fetchSkillStats() {
  const response = await apiClient.post('/skills/statistics')
  return unwrapResponse<SkillStatistics>(response)
}

export async function createSkill(payload: SkillPayload) {
  const response = await apiClient.post('/skills', payload)
  return unwrapResponse<SkillItem>(response)
}

export async function updateSkill(skillId: number | string, payload: SkillPayload) {
  const response = await apiClient.put(`/skills/${skillId}`, payload)
  return unwrapResponse<SkillItem>(response)
}

export async function removeSkill(skillId: number | string) {
  const response = await apiClient.delete(`/skills/${skillId}`)
  return unwrapResponse<void>(response)
}

export async function publishSkill(skillId: number | string) {
  const response = await apiClient.post(`/skills/${skillId}/publish`)
  return unwrapResponse<SkillItem>(response)
}

export async function hotUpdateSkill(skillId: number | string) {
  const response = await apiClient.post(`/skills/${skillId}/hot-update`)
  return unwrapResponse<SkillItem>(response)
}

export async function batchDeleteSkills(skillIds: number[]) {
  const response = await apiClient.post('/skills/batch/delete', { skillIds })
  return unwrapResponse<void>(response)
}

export async function batchUpdateSkillStatus(skillIds: number[], skillStatus: string) {
  const response = await apiClient.post('/skills/batch/status', { skillIds, skillStatus })
  return unwrapResponse<SkillItem[]>(response)
}

export async function importSkill(importPayload: string) {
  const response = await apiClient.post('/skills/import', { importPayload })
  return unwrapResponse<SkillItem>(response)
}

export async function exportSkill(skillId: number | string) {
  const response = await apiClient.get(`/skills/${skillId}/export`)
  return unwrapResponse<SkillExportResult>(response)
}
