import { apiClient, unwrapResponse } from '@/api/client'
import type { StatisticsOverview, StatisticsTrackRequest, StatisticsTrackResult } from '@/types/statistics'

export async function trackStatistics(payload: StatisticsTrackRequest) {
  const response = await apiClient.post('/statistics/track', payload)
  return unwrapResponse<StatisticsTrackResult>(response)
}

export async function fetchStatisticsOverview(params?: { startDate?: string; endDate?: string }) {
  const response = await apiClient.get('/statistics/overview', { params })
  return unwrapResponse<StatisticsOverview>(response)
}