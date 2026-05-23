import { apiClient, unwrapResponse } from '@/api/client'
import type {
  BigFileChunkUploadResult,
  BigFileInitRequest,
  BigFileInitResult,
  BigFileListResult,
  BigFileMergeResult,
  BigFileMissingChunksResult,
  BigFileStatistics,
} from '@/types/bigfile'

export async function initBigFileUpload(payload: BigFileInitRequest) {
  const response = await apiClient.post('/big-files/init', payload)
  return unwrapResponse<BigFileInitResult>(response)
}

export async function uploadBigFileChunk(fileId: string, chunkIndex: number, chunk: Blob, chunkMd5?: string) {
  const formData = new FormData()
  formData.append('chunkIndex', String(chunkIndex))
  if (chunkMd5) {
    formData.append('chunkMd5', chunkMd5)
  }
  formData.append('chunk', chunk)
  const response = await apiClient.post(`/big-files/${fileId}/chunks`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
  return unwrapResponse<BigFileChunkUploadResult>(response)
}

export async function fetchMissingBigFileChunks(fileId: string) {
  const response = await apiClient.get(`/big-files/${fileId}/missing-chunks`)
  return unwrapResponse<BigFileMissingChunksResult>(response)
}

export async function mergeBigFile(fileId: string) {
  const response = await apiClient.post(`/big-files/${fileId}/merge`, null, { timeout: 120000 })
  return unwrapResponse<BigFileMergeResult>(response)
}

export async function fetchBigFiles(params?: { keyword?: string; status?: string; businessModule?: string }) {
  const response = await apiClient.get('/big-files', { params })
  return unwrapResponse<BigFileListResult>(response)
}

export async function fetchBigFileStatistics() {
  const response = await apiClient.get('/big-files/statistics')
  return unwrapResponse<BigFileStatistics>(response)
}

export async function deleteBigFile(fileId: string) {
  const response = await apiClient.delete(`/big-files/${fileId}`)
  return unwrapResponse<void>(response)
}
