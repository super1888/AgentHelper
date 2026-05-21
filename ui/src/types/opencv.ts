export interface ImageDetectRequestPayload {
  imageBase64: string
  imageFormat: string
  businessScene?: string
}

export interface OpenCvDetectionItem {
  label: string
  classCode: string
  confidence: number
  x: number
  y: number
  width: number
  height: number
  areaRatio?: number | null
  estimatedCount?: number | null
  ingredientCategory?: string | null
}

export interface OpenCvDetectResult {
  imageWidth: number
  imageHeight: number
  detectCount: number
  modelName: string
  modelVersion: string
  costTimeMs: number
  detections: OpenCvDetectionItem[]
}
