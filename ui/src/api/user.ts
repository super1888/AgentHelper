import { apiClient, unwrapResponse } from '@/api/client'
import type {
  CreateUserPayload,
  UpdateUserPayload,
  UserPageResult,
  UserProfile,
  UserQueryPayload,
  UserStatistics,
} from '@/types/user'

export async function queryUsers(payload: UserQueryPayload) {
  const response = await apiClient.post('/users/pageQuery', payload)
  return unwrapResponse<UserPageResult>(response)
}

export async function fetchUserStats() {
  const response = await apiClient.post('/users/statistics')
  return unwrapResponse<UserStatistics>(response)
}

export async function fetchUserDetail(userId: number) {
  const response = await apiClient.get(`/users/select/${userId}`)
  return unwrapResponse<UserProfile>(response)
}

export async function createUser(payload: CreateUserPayload) {
  const response = await apiClient.post('/users/add', payload)
  return unwrapResponse<void>(response)
}

export async function updateUser(userId: number, payload: UpdateUserPayload) {
  const response = await apiClient.put(`/users/update/${userId}`, payload)
  return unwrapResponse<void>(response)
}

export async function removeUser(userId: number) {
  const response = await apiClient.delete(`/users/delete/${userId}`)
  return unwrapResponse<void>(response)
}
