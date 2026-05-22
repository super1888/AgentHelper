export interface ShortLinkItem {
  id: number
  shortCode: string
  shortUrl: string
  longUrl: string
  title?: string | null
  description?: string | null
  domain?: string | null
  status: 'ENABLED' | 'DISABLED' | string
  expireTime?: string | null
  totalVisitCount: number
  uniqueVisitorCount: number
  uniqueIpCount: number
  lastAccessTime?: string | null
  createTime?: string | null
}

export interface ShortLinkPayload {
  longUrl: string
  title?: string | null
  description?: string | null
  customCode?: string | null
  domain?: string | null
  expireTime?: string | null
}

export interface ShortLinkStatistics {
  totalCount: number
  enabledCount: number
  expiredCount: number
  totalVisitCount: number
  uniqueVisitorCount: number
  uniqueIpCount: number
}

export interface ShortLinkAccessLogItem {
  shortCode: string
  visitorId?: string | null
  ipAddress?: string | null
  userAgent?: string | null
  referer?: string | null
  accessTime?: string | null
  successFlag: number
  failReason?: string | null
}
