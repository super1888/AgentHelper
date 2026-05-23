export interface BigFileInitRequest {
  fileName: string
  fileSize: number
  chunkSize: number
  totalChunks: number
  fileMd5: string
  contentType: string
  businessModule: string
}

export interface BigFileInitResult {
  fileId: string
  status: string
  chunkSize: number
  totalChunks: number
  uploadedChunks: number[]
  message: string
}

export interface BigFileChunkUploadResult {
  fileId: string
  chunkIndex: number
  chunkMd5: string
  status: string
  uploadedCount: number
  totalChunks: number
  completed: boolean
}

export interface BigFileMissingChunksResult {
  fileId: string
  uploadedChunks: number[]
  missingChunks: number[]
  uploadedCount: number
  totalChunks: number
  status: string
}

export interface BigFileMergeResult {
  fileId: string
  fileName: string
  fileSize: number
  fileMd5: string
  storagePath: string
  status: string
  message: string
}

export interface BigFileRecord {
  fileId: string
  fileName: string
  fileMd5: string
  contentType: string
  businessModule: string
  fileSize: number
  chunkSize: number
  totalChunks: number
  uploadedCount: number
  status: string
  storagePath: string | null
  createdAt: string
  updatedAt: string
  lastMessage: string
}

export interface BigFileListResult {
  items: BigFileRecord[]
}

export interface BigFileStatistics {
  totalFiles: number
  completedFiles: number
  uploadingFiles: number
  failedFiles: number
  totalFileSize: number
  maxFileSize: number
  defaultChunkSize: number
}
