export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
}

export interface PageInfoResponse<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface ApiClientError extends Error {
  status?: number
  code?: string
}
