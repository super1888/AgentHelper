import { apiClient, unwrapResponse } from '@/api/client'
import type {
  CreateTenantPayload,
  TenantOption,
  TenantPageResult,
  TenantProfile,
  TenantQueryPayload,
  TenantStatistics,
  UpdateTenantPayload,
} from '@/types/tenant'

export async function queryTenants(payload: TenantQueryPayload) {
  const response = await apiClient.post('/tenants/pageQuery', payload)
  return unwrapResponse<TenantPageResult>(response)
}

export async function fetchTenantStats() {
  const response = await apiClient.post('/tenants/statistics')
  return unwrapResponse<TenantStatistics>(response)
}

export async function fetchTenantOptions() {
  const response = await apiClient.get('/tenants/options')
  return unwrapResponse<TenantOption[]>(response)
}

export async function fetchTenantDetail(tenantId: string | number) {
  const response = await apiClient.get(`/tenants/select/${tenantId}`)
  return unwrapResponse<TenantProfile>(response)
}

export async function createTenant(payload: CreateTenantPayload) {
  const response = await apiClient.post('/tenants/add', payload)
  return unwrapResponse<void>(response)
}

export async function updateTenant(tenantId: string | number, payload: UpdateTenantPayload) {
  const response = await apiClient.put(`/tenants/update/${tenantId}`, payload)
  return unwrapResponse<void>(response)
}

export async function removeTenant(tenantId: string | number) {
  const response = await apiClient.delete(`/tenants/delete/${tenantId}`)
  return unwrapResponse<void>(response)
}
