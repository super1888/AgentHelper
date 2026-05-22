import { apiClient, unwrapResponse } from '@/api/client'
import type {
  ShortLinkAccessLogItem,
  ShortLinkItem,
  ShortLinkPayload,
  ShortLinkStatistics,
} from '@/types/link'

export async function queryShortLinks(keyword?: string) {
  const response = await apiClient.get('/short-links', {
    params: {
      keyword: keyword || undefined,
    },
  })
  return unwrapResponse<ShortLinkItem[]>(response)
}

export async function createShortLink(payload: ShortLinkPayload) {
  const response = await apiClient.post('/short-links', payload)
  return unwrapResponse<ShortLinkItem>(response)
}

export async function fetchShortLinkStats() {
  const response = await apiClient.get('/short-links/statistics')
  return unwrapResponse<ShortLinkStatistics>(response)
}

export async function enableShortLink(linkId: number | string) {
  const response = await apiClient.post(`/short-links/${linkId}/enable`)
  return unwrapResponse<ShortLinkItem>(response)
}

export async function disableShortLink(linkId: number | string) {
  const response = await apiClient.post(`/short-links/${linkId}/disable`)
  return unwrapResponse<ShortLinkItem>(response)
}

export async function removeShortLink(linkId: number | string) {
  const response = await apiClient.delete(`/short-links/${linkId}`)
  return unwrapResponse<void>(response)
}

export async function queryShortLinkLogs(shortCode?: string) {
  const response = await apiClient.get('/short-links/access-logs', {
    params: {
      shortCode: shortCode || undefined,
    },
  })
  return unwrapResponse<ShortLinkAccessLogItem[]>(response)
}
