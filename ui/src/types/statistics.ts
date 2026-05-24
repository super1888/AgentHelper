export interface StatisticsTrackRequest {
  path: string
  title?: string
  visitorId: string
  visitId: string
}

export interface StatisticsTrackResult {
  date: string
  path: string
  visitorId: string
  visitId: string
  message: string
}

export interface StatisticsMetricPoint {
  date: string
  pv: number
  vv: number
  uv: number
  ip: number
}

export interface StatisticsOverview {
  startDate: string
  endDate: string
  totalPv: number
  totalVv: number
  totalUv: number
  totalIp: number
  todayPv: number
  todayVv: number
  todayUv: number
  todayIp: number
  trends: StatisticsMetricPoint[]
}