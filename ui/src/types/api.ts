export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
}

export interface ApiClientError extends Error {
  status?: number
  code?: string
}
