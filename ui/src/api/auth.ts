import { apiClient, unwrapResponse } from '@/api/client'
import type {
  FaceBindPayload,
  FaceLoginPayload,
  LoginPayload,
  RegisterPayload,
  UserAuthLoginResult,
  UserFaceStatus,
  UserProfile,
} from '@/types/user'

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

export async function faceLogin(payload: FaceLoginPayload) {
  const response = await apiClient.post('/auth/face/login', payload)
  return unwrapResponse<UserAuthLoginResult>(response)
}

export async function faceBind(payload: FaceBindPayload) {
  const response = await apiClient.post('/auth/face/bind', payload)
  return unwrapResponse(response)
}

export async function fetchFaceStatus() {
  const response = await apiClient.get('/auth/face/status')
  return unwrapResponse<UserFaceStatus>(response)
}

export async function faceUnbind() {
  const response = await apiClient.delete('/auth/face/unbind')
  return unwrapResponse<void>(response)
}
