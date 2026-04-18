import { apiClient, unwrapResponse } from '@/api/client'
import type {
  SkillBatchActionPayload,
  SkillCopyPayload,
  SkillDebugResult,
  SkillExportResult,
  SkillImportPayload,
  SkillExecutionLogItem,
  SkillItem,
  SkillLogQueryPayload,
  SkillPayload,
  SkillStatistics,
  SkillTestCaseItem,
  SkillTestCasePayload,
  SkillVersionComparePayload,
  SkillVersionCompareResult,
  SkillVersionRollbackPayload,
} from '@/types/skill'

export async function querySkills() {
  const response = await apiClient.get('/skills')
  return unwrapResponse<SkillItem[]>(response)
}

export async function queryDeletedSkills() {
  const response = await apiClient.get('/skills/deleted')
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

export async function restoreSkill(skillId: number | string) {
  const response = await apiClient.post(`/skills/${skillId}/restore`)
  return unwrapResponse<SkillItem>(response)
}

export async function publishSkill(skillId: number | string) {
  const response = await apiClient.post(`/skills/${skillId}/publish`)
  return unwrapResponse<SkillItem>(response)
}

export async function offlineSkill(skillId: number | string) {
  const response = await apiClient.post(`/skills/${skillId}/offline`)
  return unwrapResponse<SkillItem>(response)
}

export async function hotUpdateSkill(skillId: number | string) {
  const response = await apiClient.post(`/skills/${skillId}/hot-update`)
  return unwrapResponse<SkillItem>(response)
}

export async function rollbackSkill(skillId: number | string, payload: SkillVersionRollbackPayload) {
  const response = await apiClient.post(`/skills/${skillId}/rollback`, payload)
  return unwrapResponse<SkillItem>(response)
}

export async function compareSkillVersions(skillId: number | string, payload: SkillVersionComparePayload) {
  const response = await apiClient.post(`/skills/${skillId}/compare`, payload)
  return unwrapResponse<SkillVersionCompareResult>(response)
}

export async function copySkill(skillId: number | string, payload: SkillCopyPayload) {
  const response = await apiClient.post(`/skills/${skillId}/copy`, payload)
  return unwrapResponse<SkillItem>(response)
}

export async function batchDeleteSkills(payload: SkillBatchActionPayload) {
  const response = await apiClient.post('/skills/batch/delete', payload)
  return unwrapResponse<void>(response)
}

export async function batchUpdateSkillStatus(payload: SkillBatchActionPayload) {
  const response = await apiClient.post('/skills/batch/status', payload)
  return unwrapResponse<SkillItem[]>(response)
}

export async function batchUpdateSkillTags(payload: SkillBatchActionPayload) {
  const response = await apiClient.post('/skills/batch/tags', payload)
  return unwrapResponse<SkillItem[]>(response)
}

export async function batchMoveSkillCategory(payload: SkillBatchActionPayload) {
  const response = await apiClient.post('/skills/batch/category', payload)
  return unwrapResponse<SkillItem[]>(response)
}

export async function batchPublishSkills(payload: SkillBatchActionPayload) {
  const response = await apiClient.post('/skills/batch/publish', payload)
  return unwrapResponse<SkillItem[]>(response)
}

export async function batchOfflineSkills(payload: SkillBatchActionPayload) {
  const response = await apiClient.post('/skills/batch/offline', payload)
  return unwrapResponse<SkillItem[]>(response)
}

export async function importSkill(payload: SkillImportPayload) {
  const response = await apiClient.post('/skills/import', payload)
  return unwrapResponse<SkillItem>(response)
}

export async function exportSkill(skillId: number | string) {
  const response = await apiClient.get(`/skills/${skillId}/export`)
  return unwrapResponse<SkillExportResult>(response)
}

export async function querySkillTestCases(skillId: number | string) {
  const response = await apiClient.get(`/skills/${skillId}/test-cases`)
  return unwrapResponse<SkillTestCaseItem[]>(response)
}

export async function createSkillTestCase(skillId: number | string, payload: SkillTestCasePayload) {
  const response = await apiClient.post(`/skills/${skillId}/test-cases`, payload)
  return unwrapResponse<SkillTestCaseItem>(response)
}

export async function updateSkillTestCase(testCaseId: number | string, payload: SkillTestCasePayload) {
  const response = await apiClient.put(`/skills/test-cases/${testCaseId}`, payload)
  return unwrapResponse<SkillTestCaseItem>(response)
}

export async function removeSkillTestCase(testCaseId: number | string) {
  const response = await apiClient.delete(`/skills/test-cases/${testCaseId}`)
  return unwrapResponse<void>(response)
}

export async function runSkillTestCase(testCaseId: number | string) {
  const response = await apiClient.post(`/skills/test-cases/${testCaseId}/run`)
  return unwrapResponse<SkillDebugResult>(response)
}

export async function debugSkill(payload: Record<string, unknown>) {
  const response = await apiClient.post('/skills/debug', payload)
  return unwrapResponse<SkillDebugResult>(response)
}

export async function querySkillLogs(payload: SkillLogQueryPayload) {
  const response = await apiClient.post('/skills/logs/query', payload)
  return unwrapResponse<SkillExecutionLogItem[]>(response)
}
