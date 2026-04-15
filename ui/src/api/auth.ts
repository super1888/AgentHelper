import { apiClient, unwrapResponse } from '@/api/client'
import type { LoginPayload, RegisterPayload, UserAuthLoginResult, UserProfile } from '@/types/user'

export async function login(payload: LoginPayload) {
  const response = await apiClient.post('/auth/login', payload)
  return unwrapResponse<UserAuthLoginResult>(response)
}

export async function logout() {
  const response = await apiClient.post('/auth/logout')
  return unwrapResponse<void>(response)
}

export async function fetchCurrentUser() {
  const response = await apiClient.get('/auth/currentUser')
  return unwrapResponse<UserProfile>(response)
}

export async function registerAccount(payload: RegisterPayload) {
  const response = await apiClient.post('/users/register', payload)
  return unwrapResponse<void>(response)
}
