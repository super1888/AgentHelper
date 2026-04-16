export interface VectorStoreFileItem {
  id: number
  fileName: string
  fileExtension: string | null
  contentType: string | null
  fileSize: number | null
  sourceDocumentCount: number | null
  chunkCount: number | null
  uploadedAt: string | null
  storeStatus: string | null
  lastOperationMessage: string | null
}

export interface VectorStoreFileListResult {
  total: number
  items: VectorStoreFileItem[]
}

export interface VectorStoreDocumentItem {
  id: string
  content: string
  score?: number | null
  metadata: Record<string, unknown>
}

export interface VectorStoreDocumentListResult {
  fileName: string
  total: number
  items: VectorStoreDocumentItem[]
}

export interface VectorStoreSearchResult {
  query: string
  fileName?: string | null
  topK: number
  similarityThreshold?: number | null
  total: number
  items: VectorStoreDocumentItem[]
}

export interface VectorStoreStatistics {
  totalFiles: number
  activeFiles: number
  deletedFiles: number
  totalChunks: number
  totalFileSize: number
}

export interface VectorStoreUploadResult {
  fileName: string
  fileExtension: string
  sourceDocumentCount: number
  chunkCount: number
  fileSize: number
  uploadedAt: string
  message: string
}

export interface VectorStoreDeleteResult {
  action: string
  fileName?: string | null
  message: string
}
