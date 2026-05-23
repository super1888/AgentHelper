import { apiClient, unwrapResponse } from '@/api/client'
import type {
  VectorStoreDeleteResult,
  VectorStoreDocumentListResult,
  VectorStoreFileListResult,
  VectorStoreSearchResult,
  VectorStoreStatistics,
  VectorStoreUploadResult,
} from '@/types/vector'

export async function fetchVectorFiles() {
  const response = await apiClient.get('/vectorStore/files')
  return unwrapResponse<VectorStoreFileListResult>(response)
}

export async function fetchVectorStatistics() {
  const response = await apiClient.get('/vectorStore/statistics')
  return unwrapResponse<VectorStoreStatistics>(response)
}

export async function fetchVectorDocuments(fileName: string) {
  const response = await apiClient.get('/vectorStore/documents', {
    params: { fileName },
  })
  return unwrapResponse<VectorStoreDocumentListResult>(response)
}

export async function searchVectorDocuments(params: {
  query: string
  fileName?: string
  topK?: number
  similarityThreshold?: number
}) {
  const response = await apiClient.get('/vectorStore/search', {
    params,
  })
  return unwrapResponse<VectorStoreSearchResult>(response)
}

export async function uploadVectorFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post('/vectorStore/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  return unwrapResponse<VectorStoreUploadResult>(response)
}

export async function deleteVectorFile(fileName: string) {
  const response = await apiClient.post('/vectorStore/deleteByFileName', null, {
    params: { fileName },
  })
  return unwrapResponse<VectorStoreDeleteResult>(response)
}

export async function deleteAllVectorFiles() {
  const response = await apiClient.post('/vectorStore/deleteAll')
  return unwrapResponse<VectorStoreDeleteResult>(response)
}

export async function importBigFileToVectorStore(fileId: string) {
  const response = await apiClient.post('/vectorStore/importBigFile', null, {
    params: { fileId },
    timeout: 120000,
  })
  return unwrapResponse<VectorStoreUploadResult>(response)
}