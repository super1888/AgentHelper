import type { PageInfoResponse } from '@/types/api'

export type TenantStatus = 0 | 1

export interface TenantProfile {
  id: string
  tenantCode: string
  tenantName: string
  status: TenantStatus
  isDefault: 0 | 1
  ownerUserId: string | null
  ownerUserName: string | null
  contactName: string | null
  contactPhone: string | null
  description: string | null
  memberCount: number
}

export interface TenantOption {
  id: string
  tenantCode: string
  tenantName: string
  status: TenantStatus
}

export interface TenantQueryPayload {
  pageNum?: number
  pageSize?: number
  tenantCode?: string
  tenantName?: string
  status?: TenantStatus | null
}

export interface CreateTenantPayload {
  tenantCode: string
  tenantName: string
  contactName: string | null
  contactPhone: string | null
  description: string | null
  status: TenantStatus
}

export interface UpdateTenantPayload extends CreateTenantPayload {}

export interface TenantStatistics {
  totalCount: number
  enabledCount: number
  disabledCount: number
}

export type TenantPageResult = PageInfoResponse<TenantProfile>
