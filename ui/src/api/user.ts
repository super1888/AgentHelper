import { apiClient, unwrapResponse } from '@/api/client'
import type { CreateUserPayload, UpdateUserPayload, UserProfile } from '@/types/user'

export async function fetchUsers() {
  const response = await apiClient.get('/users/getAllUser')
  return unwrapResponse<UserProfile[]>(response)
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
