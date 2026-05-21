import { apiClient, unwrapResponse } from '@/api/client'
import type { ImageDetectRequestPayload, OpenCvDetectResult } from '@/types/opencv'

export async function detectImage(payload: ImageDetectRequestPayload) {
  const response = await apiClient.post('/image/detect', payload)
  return unwrapResponse<OpenCvDetectResult>(response)
}
