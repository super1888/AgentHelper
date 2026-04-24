import axios, { AxiosError, type AxiosResponse } from 'axios'
import { appConfig } from '@/config/env'
import type { ApiClientError, ApiResponse } from '@/types/api'
import { clearAuthSnapshot, loadAuthSnapshot } from '@/utils/storage'

type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler | null = null

export const apiClient = axios.create({
  baseURL: appConfig.apiBaseUrl,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export function setUnauthorizedHandler(handler: UnauthorizedHandler) {
  unauthorizedHandler = handler
}

function createApiClientError(message: string, status?: number, code?: string) {
  const error = new Error(message) as ApiClientError
  error.status = status
  error.code = code
  return error
}

function normalizeApiError(error: AxiosError<ApiResponse<unknown>>) {
  const message = error.response?.data?.message || error.message || '请求失败，请稍后重试。'
  return createApiClientError(message, error.response?.status, error.response?.data?.code)
}

apiClient.interceptors.request.use((config) => {
  const snapshot = loadAuthSnapshot()
  if (!snapshot?.token?.tokenName || !snapshot.token.authorizationValue) {
    return config
  }

  const headers = config.headers ?? {}
  ;(headers as Record<string, string>)[snapshot.token.tokenName] = snapshot.token.authorizationValue
  config.headers = headers
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401) {
      clearAuthSnapshot()
      unauthorizedHandler?.()
    }

    return Promise.reject(normalizeApiError(error))
  },
)

export function unwrapResponse<T>(response: AxiosResponse<ApiResponse<T>>) {
  const payload = response.data
  if (!payload.success) {
    throw createApiClientError(payload.message, response.status, payload.code)
  }

  return payload.data
}
