<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { Copy, Download, Eraser, Image, Images, Link2, RefreshCw, Send, Sparkles, Trash2, Upload } from 'lucide-vue-next'
import MainShell from '@/components/MainShell.vue'
import { getErrorMessage } from '@/utils/errors'

type StudioMode = 'generate' | 'edit'

interface ImageResultItem {
  id: string
  src: string
  mimeType: string
  revisedPrompt: string | null
  raw: Record<string, unknown>
}

interface UploadPreviewItem {
  id: string
  name: string
  sizeLabel: string
  url: string
  error: string | null
  width: number | null
  height: number | null
  aspectRatio: string
}

interface ImageApiResponse {
  data?: Array<{
    b64_json?: string
    url?: string
    revised_prompt?: string
    [key: string]: unknown
  }>
  error?: {
    message?: string
    [key: string]: unknown
  }
  [key: string]: unknown
}

const IMAGE_STUDIO_STORAGE_KEY = 'spring-ai:image-studio-config'

const activeMode = ref<StudioMode>('generate')
const loading = ref(false)
const responseStatus = ref('')
const responseTimeMs = ref<number | null>(null)
const rawResponse = ref('')
const resultImages = ref<ImageResultItem[]>([])
const lastRequestPayload = ref<Record<string, unknown> | null>(null)
const message = ref('')
const messageTone = ref<'success' | 'error' | 'info'>('info')
const requestStartedAt = ref<number | null>(null)
const elapsedMs = ref(0)
const progressTimer = ref<number | null>(null)

const endpointForm = reactive({
  baseUrl: 'https://api-cn.hi-code.cc',
  apiKey: '',
  generationPath: '/v1/images/generations',
  editsPath: '/v1/images/edits',
  persistSecret: true,
})

const generationForm = reactive({
  model: 'gpt-image-2',
  prompt: 'Generate a tiny flat blue cat face icon, minimalist vector style',
  size: '1024x1024',
  quality: 'medium',
  background: 'auto',
  moderation: 'auto',
  outputFormat: 'png',
  outputCompression: 100,
  n: 1,
  user: '',
  extraBody: '',
})

const editForm = reactive({
  model: 'gpt-image-2',
  prompt: 'Turn this into a tiny flat blue cat face icon, minimalist vector style',
  size: '1024x1024',
  quality: 'medium',
  background: 'auto',
  moderation: 'auto',
  outputFormat: 'png',
  outputCompression: 100,
  n: 1,
  user: '',
  extraBody: '',
})

const uploadOptimizerForm = reactive({
  enabled: true,
  maxLongEdge: 1600,
  jpegQuality: 0.82,
})

const primaryImageFiles = ref<File[]>([])
const maskImageFile = ref<File | null>(null)
const referenceImageFiles = ref<File[]>([])
const primaryImagePreviews = ref<UploadPreviewItem[]>([])
const maskImagePreview = ref<UploadPreviewItem | null>(null)
const referenceImagePreviews = ref<UploadPreviewItem[]>([])
const uploadValidationMessage = ref('')
const primaryDragOver = ref(false)
const maskDragOver = ref(false)
const referenceDragOver = ref(false)
const lastSuccessfulGeneratePayload = ref<Record<string, unknown> | null>(null)
const lastSuccessfulEditPayload = ref<Record<string, unknown> | null>(null)

const generationPresets = [
  { key: 'icon', label: '图标', prompt: '生成一个极简、扁平、清晰的应用图标，几何构图，纯净背景，适合小尺寸展示', size: '1024x1024', quality: 'medium', background: 'transparent' },
  { key: 'poster', label: '海报', prompt: '生成一张具有强烈视觉中心的品牌海报，层次清晰，文字留白充分，适合营销展示', size: '1024x1536', quality: 'high', background: 'opaque' },
  { key: 'banner', label: '横幅', prompt: '生成一张横向视觉横幅，主体突出，构图稳定，适合首页头图使用', size: '1536x1024', quality: 'high', background: 'opaque' },
] as const

const editPresets = [
  { key: 'style', label: '风格改造', prompt: '保留主体结构与构图，仅替换整体视觉风格，使其更加统一、精致、可用于产品展示', quality: 'high', background: 'auto' },
  { key: 'cleanup', label: '清理背景', prompt: '清理杂乱背景，提升主体识别度，保留主体细节并让边缘更干净', quality: 'medium', background: 'transparent' },
  { key: 'variant', label: '变体扩展', prompt: '基于原图生成更一致的多版本视觉变体，保持核心元素，但在细节上做合理扩展', quality: 'high', background: 'auto' },
] as const

function setMessage(tone: 'success' | 'error' | 'info', value: string) {
  messageTone.value = tone
  message.value = value
}

function normalizePath(path: string) {
  const trimmed = path.trim()
  if (!trimmed) return ''
  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

function formatBytes(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

function revokePreview(item: UploadPreviewItem | null) {
  if (item?.url) URL.revokeObjectURL(item.url)
}

function revokePreviewList(items: UploadPreviewItem[]) {
  items.forEach((item) => URL.revokeObjectURL(item.url))
}

function dedupeFiles(files: File[]) {
  const seen = new Set<string>()
  return files.filter((file) => {
    const key = `${file.name}-${file.size}-${file.lastModified}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function replaceFileExtension(fileName: string, extension: string) {
  const normalizedExtension = extension.startsWith('.') ? extension : `.${extension}`
  const index = fileName.lastIndexOf('.')
  if (index <= 0) {
    return `${fileName}${normalizedExtension}`
  }
  return `${fileName.slice(0, index)}${normalizedExtension}`
}

async function fileToDataUrl(file: File) {
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('读取图片失败。'))
    reader.readAsDataURL(file)
  })
}

async function dataUrlToImage(dataUrl: string) {
  return await new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new window.Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('解析图片失败。'))
    image.src = dataUrl
  })
}

async function canvasToBlob(canvas: HTMLCanvasElement, mimeType: string, quality?: number) {
  return await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('导出优化图片失败。'))
        return
      }
      resolve(blob)
    }, mimeType, quality)
  })
}

async function optimizeEditableImage(file: File) {
  if (!uploadOptimizerForm.enabled) return file
  if (!file.type.startsWith('image/')) return file

  const shouldReencode = file.type === 'image/jpeg' || file.type === 'image/webp'
  const shouldResize = file.size > 1.5 * 1024 * 1024

  if (!shouldReencode && !shouldResize) {
    return file
  }

  const dataUrl = await fileToDataUrl(file)
  const image = await dataUrlToImage(dataUrl)
  const originalWidth = image.naturalWidth || image.width
  const originalHeight = image.naturalHeight || image.height
  const scale = Math.min(1, uploadOptimizerForm.maxLongEdge / Math.max(originalWidth, originalHeight))
  const targetWidth = Math.max(1, Math.round(originalWidth * scale))
  const targetHeight = Math.max(1, Math.round(originalHeight * scale))

  if (scale >= 0.999 && !shouldReencode) {
    return file
  }

  const canvas = document.createElement('canvas')
  canvas.width = targetWidth
  canvas.height = targetHeight
  const context = canvas.getContext('2d')
  if (!context) {
    return file
  }
  context.drawImage(image, 0, 0, targetWidth, targetHeight)

  const outputType = shouldReencode ? 'image/jpeg' : file.type
  const outputBlob = await canvasToBlob(canvas, outputType, uploadOptimizerForm.jpegQuality)

  if (outputBlob.size >= file.size * 0.96) {
    return file
  }

  const outputName = outputType === 'image/jpeg'
    ? replaceFileExtension(file.name, '.jpg')
    : replaceFileExtension(file.name, file.name.split('.').pop() || 'png')

  return new File([outputBlob], outputName, {
    type: outputType,
    lastModified: file.lastModified,
  })
}

async function readImageMeta(url: string) {
  return await new Promise<{ width: number | null, height: number | null, aspectRatio: string }>((resolve) => {
    const image = new window.Image()
    image.onload = () => {
      const width = image.naturalWidth || null
      const height = image.naturalHeight || null
      resolve({
        width,
        height,
        aspectRatio: width && height ? `${width}:${height}` : '--',
      })
    }
    image.onerror = () => {
      resolve({
        width: null,
        height: null,
        aspectRatio: '--',
      })
    }
    image.src = url
  })
}

async function toPreviewItem(file: File, suffix: string): Promise<UploadPreviewItem> {
  const url = URL.createObjectURL(file)
  const meta = await readImageMeta(url)
  return {
    id: `${suffix}-${file.name}-${file.size}-${file.lastModified}`,
    name: file.name,
    sizeLabel: formatBytes(file.size),
    url,
    error: null,
    width: meta.width,
    height: meta.height,
    aspectRatio: meta.aspectRatio,
  }
}

function validateImageFile(file: File, label: string) {
  if (!file.type.startsWith('image/')) {
    return `${label} 不是图片文件。`
  }
  if (file.size > 20 * 1024 * 1024) {
    return `${label} 超过 20MB 限制。`
  }
  return null
}

async function setPrimaryFiles(files: File[], mode: 'replace' | 'append' = 'replace') {
  revokePreviewList(primaryImagePreviews.value)
  uploadValidationMessage.value = ''

  const nextFiles = mode === 'append'
    ? dedupeFiles([...primaryImageFiles.value, ...files])
    : dedupeFiles(files)

  const errors: string[] = []
  const acceptedFiles: File[] = []
  const previews: UploadPreviewItem[] = []

  for (const [index, file] of nextFiles.entries()) {
    const error = validateImageFile(file, `主图 ${index + 1}`)
    if (error) {
      errors.push(error)
      continue
    }
    acceptedFiles.push(await optimizeEditableImage(file))
  }

  if (acceptedFiles.length > 16) {
    uploadValidationMessage.value = '图片编辑模式最多支持 16 张输入图，请删减后再试。'
    setMessage('error', uploadValidationMessage.value)
    return
  }

  for (const [index, file] of acceptedFiles.entries()) {
    previews.push(await toPreviewItem(file, `primary-${index}`))
  }

  primaryImageFiles.value = acceptedFiles
  primaryImagePreviews.value = previews

  if (errors.length) {
    uploadValidationMessage.value = errors.join('；')
    setMessage('error', uploadValidationMessage.value)
  }
}

async function setMaskFile(file: File | null) {
  revokePreview(maskImagePreview.value)
  uploadValidationMessage.value = ''
  if (file) {
    const error = validateImageFile(file, 'Mask 图')
    if (error) {
      maskImageFile.value = null
      maskImagePreview.value = null
      uploadValidationMessage.value = error
      setMessage('error', error)
      return
    }
  }
  maskImageFile.value = file
  maskImagePreview.value = file ? await toPreviewItem(file, 'mask') : null
}

async function setReferenceFiles(files: File[], mode: 'replace' | 'append' = 'replace') {
  revokePreviewList(referenceImagePreviews.value)
  uploadValidationMessage.value = ''

  const nextFiles = mode === 'append'
    ? dedupeFiles([...referenceImageFiles.value, ...files])
    : dedupeFiles(files)

  const acceptedFiles: File[] = []
  const previews: UploadPreviewItem[] = []
  const errors: string[] = []

  for (const [index, file] of nextFiles.entries()) {
    const error = validateImageFile(file, `参考图 ${index + 1}`)
    if (error) {
      errors.push(error)
      continue
    }
    const optimizedFile = await optimizeEditableImage(file)
    acceptedFiles.push(optimizedFile)
    previews.push(await toPreviewItem(optimizedFile, `reference-${index}`))
  }

  if (acceptedFiles.length + primaryImageFiles.value.length > 16) {
    uploadValidationMessage.value = '主图和参考图合计最多支持 16 张，请删减后再试。'
    setMessage('error', uploadValidationMessage.value)
    return
  }

  referenceImageFiles.value = acceptedFiles
  referenceImagePreviews.value = previews

  if (errors.length) {
    uploadValidationMessage.value = errors.join('；')
    setMessage('error', uploadValidationMessage.value)
  }
}

onBeforeUnmount(() => {
  stopProgressTracking()
  revokePreviewList(primaryImagePreviews.value)
  revokePreview(maskImagePreview.value)
  revokePreviewList(referenceImagePreviews.value)
})

function loadLocalConfig() {
  if (typeof window === 'undefined') return
  const rawValue = window.localStorage.getItem(IMAGE_STUDIO_STORAGE_KEY)
  if (!rawValue) return

  try {
    const parsed = JSON.parse(rawValue) as {
      activeMode?: StudioMode
      endpointForm?: Partial<typeof endpointForm>
      generationForm?: Partial<typeof generationForm>
      editForm?: Partial<typeof editForm>
      uploadOptimizerForm?: Partial<typeof uploadOptimizerForm>
    }
    if (parsed.activeMode) activeMode.value = parsed.activeMode
    Object.assign(endpointForm, parsed.endpointForm ?? {})
    Object.assign(generationForm, parsed.generationForm ?? {})
    Object.assign(editForm, parsed.editForm ?? {})
    Object.assign(uploadOptimizerForm, parsed.uploadOptimizerForm ?? {})
  } catch {
    window.localStorage.removeItem(IMAGE_STUDIO_STORAGE_KEY)
  }
}

function persistLocalConfig() {
  if (typeof window === 'undefined') return

  window.localStorage.setItem(IMAGE_STUDIO_STORAGE_KEY, JSON.stringify({
    activeMode: activeMode.value,
    endpointForm: {
      ...endpointForm,
      apiKey: endpointForm.persistSecret ? endpointForm.apiKey : '',
    },
    generationForm: { ...generationForm },
    editForm: { ...editForm },
    uploadOptimizerForm: { ...uploadOptimizerForm },
  }))
}

loadLocalConfig()

watch([activeMode, endpointForm, generationForm, editForm, uploadOptimizerForm], () => {
  persistLocalConfig()
}, { deep: true })

const effectivePath = computed(() => activeMode.value === 'generate' ? endpointForm.generationPath : endpointForm.editsPath)
const proxyRequestUrl = computed(() => activeMode.value === 'generate'
  ? '/agentHelper/core/image-proxy/generations'
  : '/agentHelper/core/image-proxy/edits')

const fullEndpointUrl = computed(() => {
  const baseUrl = endpointForm.baseUrl.trim().replace(/\/+$/, '')
  return `${baseUrl}${normalizePath(effectivePath.value)}`
})

const stats = computed(() => ({
  modeLabel: activeMode.value === 'generate' ? '生成' : '编辑',
  imageCount: resultImages.value.length,
  responseTime: responseTimeMs.value ? `${responseTimeMs.value} ms` : '--',
}))

const activeForm = computed(() => activeMode.value === 'generate' ? generationForm : editForm)

const estimatedDurationSeconds = computed(() => {
  const form = activeForm.value
  let seconds = activeMode.value === 'generate' ? 18 : 42

  if (form.quality === 'low') seconds -= 6
  else if (form.quality === 'high') seconds += 24
  else if (form.quality === 'auto') seconds += 8

  if (form.size === '1024x1536' || form.size === '1536x1024') seconds += 10
  else if (form.size === 'auto') seconds += 6

  if (form.outputFormat === 'jpeg') seconds -= 3
  else if (form.outputFormat === 'webp') seconds -= 1

  seconds += Math.max(0, Number(form.n || 1) - 1) * 6

  if (activeMode.value === 'edit') {
    seconds += referenceImageFiles.value.length * 6
    if (maskImageFile.value) seconds += 4
    if (primaryImageFiles.value.length > 0) {
      seconds += 3
    }
  }

  if (String(form.prompt || '').trim().length > 180) {
    seconds += 5
  }

  if (!endpointForm.baseUrl.includes('api-cn')) {
    seconds += 5
  }

  return Math.min(180, Math.max(10, seconds))
})

const progressPercent = computed(() => {
  if (!loading.value) {
    return responseTimeMs.value ? 100 : 0
  }
  const estimateMs = estimatedDurationSeconds.value * 1000
  const ratio = estimateMs > 0 ? elapsedMs.value / estimateMs : 0
  return Math.max(4, Math.min(96, Math.round(ratio * 100)))
})

const progressPhase = computed(() => {
  if (!loading.value) {
    return responseTimeMs.value ? '已完成，结果已返回' : '等待发起请求'
  }
  if (elapsedMs.value < 1500) return '请求已提交，正在建立上游连接'
  if (progressPercent.value < 36) return '上游已接收，正在解析参数'
  if (progressPercent.value < 88) return activeMode.value === 'generate' ? '上游正在生成图片' : '上游正在编辑图片'
  return '生成接近完成，正在等待最终图片返回'
})

const elapsedSecondsLabel = computed(() => `${Math.floor(elapsedMs.value / 1000)} s`)
const estimatedSecondsLabel = computed(() => `${estimatedDurationSeconds.value} s`)

const performanceTips = computed(() => {
  const tips = [
    '1024x1024 通常是最稳也最快的尺寸。',
    'quality 设为 low 或 medium，返回会明显快于 high。',
    '如果更关注速度，输出格式优先用 jpeg，其次 webp，再是 png。',
    '编辑模式会额外消耗输入图像 token，多图、mask、长提示词都会拉长等待时间。',
  ]

  if (activeMode.value === 'edit' && uploadOptimizerForm.enabled) {
    tips.push(`当前已启用上传前优化，主图和参考图会先压到最长边 ${uploadOptimizerForm.maxLongEdge}px。`)
  }

  if (!endpointForm.baseUrl.includes('api-cn')) {
    tips.push('当前不是 api-cn 域名，中转链路可能更慢或更不稳定。')
  } else {
    tips.push('当前使用 api-cn 域名，通常比默认国际中转链路更稳。')
  }

  return tips
})

const responseUsageSummary = computed(() => {
  if (!rawResponse.value) return ''
  try {
    const parsed = JSON.parse(rawResponse.value) as ImageApiResponse & {
      usage?: {
        input_tokens?: number
        output_tokens?: number
        total_tokens?: number
        input_tokens_details?: { image_tokens?: number, text_tokens?: number }
      }
    }
    const usage = parsed.usage
    if (!usage) return ''
    const imageInputTokens = usage.input_tokens_details?.image_tokens ?? 0
    const textInputTokens = usage.input_tokens_details?.text_tokens ?? 0
    return `输入 ${usage.input_tokens ?? 0}（图像 ${imageInputTokens} / 文本 ${textInputTokens}），输出 ${usage.output_tokens ?? 0}，总计 ${usage.total_tokens ?? 0} tokens`
  } catch {
    return ''
  }
})

const transparentHint = computed(() => {
  const form = activeMode.value === 'generate' ? generationForm : editForm
  const modelName = String(form.model || '').trim().toLowerCase()
  return form.background === 'transparent'
    ? modelName === 'gpt-image-2'
      ? '当前官方文档显示 gpt-image-2 不支持 transparent 背景，请改用 opaque 或 auto。'
      : '透明背景是否可用取决于上游模型映射。如果 SUB2API 返回不支持，请改用 opaque 或 auto。'
    : ''
})

function parseExtraBody(rawValue: string) {
  const value = rawValue.trim()
  if (!value) return {}

  try {
    const parsed = JSON.parse(value) as Record<string, unknown>
    if (Array.isArray(parsed) || parsed === null) {
      throw new Error('Extra body JSON must be an object.')
    }
    return parsed
  } catch (error) {
    throw new Error(getErrorMessage(error, '扩展参数 JSON 解析失败。'))
  }
}

function buildGenerationPayload() {
  if (generationForm.model.trim().toLowerCase() === 'gpt-image-2' && generationForm.background === 'transparent') {
    throw new Error('gpt-image-2 当前不支持 transparent 背景，请改用 opaque 或 auto。')
  }

  const payload: Record<string, unknown> = {
    model: generationForm.model.trim(),
    prompt: generationForm.prompt.trim(),
    size: generationForm.size,
    quality: generationForm.quality,
    background: generationForm.background,
    moderation: generationForm.moderation,
    output_format: generationForm.outputFormat,
    n: generationForm.n,
    ...parseExtraBody(generationForm.extraBody),
  }

  if (generationForm.outputFormat === 'jpeg' || generationForm.outputFormat === 'webp') {
    payload.output_compression = generationForm.outputCompression
  }

  if (generationForm.user.trim()) {
    payload.user = generationForm.user.trim()
  }

  return payload
}

function buildEditRequest() {
  if (editForm.model.trim().toLowerCase() === 'gpt-image-2' && editForm.background === 'transparent') {
    throw new Error('gpt-image-2 当前不支持 transparent 背景，请改用 opaque 或 auto。')
  }

  const extraBody = parseExtraBody(editForm.extraBody)
  const formData = new FormData()
  const summary: Record<string, unknown> = {
    model: editForm.model.trim(),
    prompt: editForm.prompt.trim(),
    size: editForm.size,
    quality: editForm.quality,
    background: editForm.background,
    moderation: editForm.moderation,
    output_format: editForm.outputFormat,
    n: editForm.n,
    image: [],
    mask: null,
    ...extraBody,
  }

  formData.append('model', editForm.model.trim())
  formData.append('prompt', editForm.prompt.trim())
  formData.append('size', editForm.size)
  formData.append('quality', editForm.quality)
  formData.append('background', editForm.background)
  formData.append('moderation', editForm.moderation)
  formData.append('output_format', editForm.outputFormat)
  formData.append('n', String(editForm.n))

  if (editForm.outputFormat === 'jpeg' || editForm.outputFormat === 'webp') {
    formData.append('output_compression', String(editForm.outputCompression))
    summary.output_compression = editForm.outputCompression
  }

  if (editForm.user.trim()) {
    formData.append('user', editForm.user.trim())
    summary.user = editForm.user.trim()
  }

  const images = [...primaryImageFiles.value, ...referenceImageFiles.value]
  images.forEach((file) => formData.append('image[]', file))
  summary.image = images.map((file) => file.name)

  if (maskImageFile.value) {
    formData.append('mask', maskImageFile.value)
    summary.mask = maskImageFile.value.name
  }

  Object.entries(extraBody).forEach(([key, value]) => {
    formData.append(key, typeof value === 'string' ? value : JSON.stringify(value))
  })

  return { formData, summary }
}

function normalizeMimeType(format: string) {
  if (format === 'jpg' || format === 'jpeg') return 'image/jpeg'
  if (format === 'webp') return 'image/webp'
  return 'image/png'
}

function normalizeResults(response: ImageApiResponse, payload: Record<string, unknown>) {
  const outputFormat = String(payload.output_format || 'png').toLowerCase()
  const mimeType = normalizeMimeType(outputFormat)

  return (response.data ?? [])
    .map((item, index) => {
      const src = item.b64_json ? `data:${mimeType};base64,${item.b64_json}` : String(item.url || '')
      return {
        id: `image-${Date.now()}-${index}`,
        src,
        mimeType,
        revisedPrompt: typeof item.revised_prompt === 'string' ? item.revised_prompt : null,
        raw: item,
      }
    })
    .filter((item) => Boolean(item.src))
}

async function readResponse(response: Response) {
  responseStatus.value = `${response.status} ${response.statusText}`
  const responseText = await response.text()
  rawResponse.value = responseText

  let parsed: ImageApiResponse = {}
  try {
    parsed = responseText ? JSON.parse(responseText) as ImageApiResponse : {}
  } catch {
    if (!response.ok) {
      throw new Error(`上游返回了非 JSON 错误内容，HTTP ${response.status}。`)
    }
  }

  if (!response.ok) {
    throw new Error(parsed.error?.message || `图片请求失败，HTTP ${response.status}。`)
  }

  return parsed
}

function resetResultState() {
  stopProgressTracking()
  responseStatus.value = ''
  responseTimeMs.value = null
  rawResponse.value = ''
  resultImages.value = []
  lastRequestPayload.value = null
}

function startProgressTracking() {
  stopProgressTracking()
  requestStartedAt.value = Date.now()
  elapsedMs.value = 0
  progressTimer.value = window.setInterval(() => {
    if (requestStartedAt.value) {
      elapsedMs.value = Date.now() - requestStartedAt.value
    }
  }, 250)
}

function stopProgressTracking() {
  if (progressTimer.value !== null) {
    window.clearInterval(progressTimer.value)
    progressTimer.value = null
  }
  if (requestStartedAt.value) {
    elapsedMs.value = Date.now() - requestStartedAt.value
  }
  requestStartedAt.value = null
}

async function handleGenerate() {
  if (!endpointForm.baseUrl.trim()) return setMessage('error', '请先填写 SUB2API_BASE。')
  if (!endpointForm.apiKey.trim()) return setMessage('error', '请先填写 SUB2API_KEY。')
  if (!generationForm.model.trim()) return setMessage('error', '请先填写图片模型名称。')
  if (!generationForm.prompt.trim()) return setMessage('error', '请先填写生成提示词。')

  loading.value = true
  resetResultState()
  startProgressTracking()

  try {
    const payload = buildGenerationPayload()
    const startedAt = Date.now()
    lastRequestPayload.value = payload

    const response = await fetch(proxyRequestUrl.value, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        baseUrl: endpointForm.baseUrl.trim(),
        apiKey: endpointForm.apiKey.trim(),
        endpointPath: endpointForm.generationPath,
        payload,
      }),
    })

    const parsed = await readResponse(response)
    responseTimeMs.value = Date.now() - startedAt
    resultImages.value = normalizeResults(parsed, payload)
    lastSuccessfulGeneratePayload.value = JSON.parse(JSON.stringify(payload))

    if (!resultImages.value.length) {
      return setMessage('info', '请求已成功，但响应中没有可预览图片。')
    }

    setMessage('success', `图片生成成功，共返回 ${resultImages.value.length} 张结果。`)
  } catch (error) {
    setMessage('error', getErrorMessage(error, '图片生成失败。'))
  } finally {
    stopProgressTracking()
    loading.value = false
  }
}

async function handleEdit() {
  if (!endpointForm.baseUrl.trim()) return setMessage('error', '请先填写 SUB2API_BASE。')
  if (!endpointForm.apiKey.trim()) return setMessage('error', '请先填写 SUB2API_KEY。')
  if (!editForm.model.trim()) return setMessage('error', '请先填写图片模型名称。')
  if (!editForm.prompt.trim()) return setMessage('error', '请先填写编辑提示词。')
  if (!primaryImageFiles.value.length) return setMessage('error', '请至少上传一张主图。')

  loading.value = true
  resetResultState()
  startProgressTracking()

  try {
    const { formData, summary } = buildEditRequest()
    const startedAt = Date.now()
    lastRequestPayload.value = summary

    formData.append('baseUrl', endpointForm.baseUrl.trim())
    formData.append('apiKey', endpointForm.apiKey.trim())
    formData.append('endpointPath', endpointForm.editsPath)

    const response = await fetch(proxyRequestUrl.value, {
      method: 'POST',
      body: formData,
    })

    const parsed = await readResponse(response)
    responseTimeMs.value = Date.now() - startedAt
    resultImages.value = normalizeResults(parsed, summary)
    lastSuccessfulEditPayload.value = JSON.parse(JSON.stringify(summary))

    if (!resultImages.value.length) {
      return setMessage('info', '编辑请求已成功，但响应中没有可预览图片。')
    }

    setMessage('success', `图片编辑成功，共返回 ${resultImages.value.length} 张结果。`)
  } catch (error) {
    setMessage('error', getErrorMessage(error, '图片编辑失败。'))
  } finally {
    stopProgressTracking()
    loading.value = false
  }
}

function handleSubmit() {
  if (activeMode.value === 'generate') {
    void handleGenerate()
    return
  }
  void handleEdit()
}

function resetForms() {
  generationForm.model = 'gpt-image-2'
  generationForm.prompt = 'Generate a tiny flat blue cat face icon, minimalist vector style'
  generationForm.size = '1024x1024'
  generationForm.quality = 'medium'
  generationForm.background = 'auto'
  generationForm.moderation = 'auto'
  generationForm.outputFormat = 'png'
  generationForm.outputCompression = 100
  generationForm.n = 1
  generationForm.user = ''
  generationForm.extraBody = ''

  editForm.model = 'gpt-image-2'
  editForm.prompt = 'Turn this into a tiny flat blue cat face icon, minimalist vector style'
  editForm.size = '1024x1024'
  editForm.quality = 'medium'
  editForm.background = 'auto'
  editForm.moderation = 'auto'
  editForm.outputFormat = 'png'
  editForm.outputCompression = 100
  editForm.n = 1
  editForm.user = ''
  editForm.extraBody = ''

  void setPrimaryFiles([])
  void setMaskFile(null)
  void setReferenceFiles([])
  resetResultState()
  setMessage('info', '生成与编辑参数已重置。')
}

function onPrimaryImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  void setPrimaryFiles(Array.from(input.files ?? []), 'append')
  input.value = ''
}

function onMaskImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  void setMaskFile(input.files?.[0] ?? null)
}

function onReferenceImagesChange(event: Event) {
  const input = event.target as HTMLInputElement
  void setReferenceFiles(Array.from(input.files ?? []), 'append')
  input.value = ''
}

function handleDragOver(event: DragEvent, target: 'primary' | 'mask' | 'reference') {
  event.preventDefault()
  if (target === 'primary') primaryDragOver.value = true
  if (target === 'mask') maskDragOver.value = true
  if (target === 'reference') referenceDragOver.value = true
}

function handleDragLeave(target: 'primary' | 'mask' | 'reference') {
  if (target === 'primary') primaryDragOver.value = false
  if (target === 'mask') maskDragOver.value = false
  if (target === 'reference') referenceDragOver.value = false
}

function handleDrop(event: DragEvent, target: 'primary' | 'mask' | 'reference') {
  event.preventDefault()
  handleDragLeave(target)
  const files = Array.from(event.dataTransfer?.files ?? []).filter((file) => file.type.startsWith('image/'))
  if (!files.length) {
    setMessage('error', '拖拽内容里没有可用图片文件。')
    return
  }

  if (target === 'primary') {
    void setPrimaryFiles(files, 'append')
    return
  }
  if (target === 'mask') {
    void setMaskFile(files[0] ?? null)
    return
  }

  void setReferenceFiles(files)
}

function clearPrimaryFile() {
  void setPrimaryFiles([])
}

function removePrimaryFile(index: number) {
  const nextFiles = primaryImageFiles.value.filter((_, currentIndex) => currentIndex !== index)
  void setPrimaryFiles(nextFiles)
}

function clearMaskFile() {
  void setMaskFile(null)
}

function removeReferenceFile(index: number) {
  const nextFiles = referenceImageFiles.value.filter((_, currentIndex) => currentIndex !== index)
  void setReferenceFiles(nextFiles)
}

function buildDownloadName(index: number, mimeType: string) {
  const extension = mimeType === 'image/jpeg' ? 'jpg' : mimeType === 'image/webp' ? 'webp' : 'png'
  return `image-studio-${activeMode.value}-${index + 1}.${extension}`
}

async function copyImageSource(src: string) {
  try {
    await navigator.clipboard.writeText(src)
    setMessage('success', '图片地址已复制。')
  } catch {
    setMessage('error', '复制失败，请检查浏览器剪贴板权限。')
  }
}

function openImageSource(src: string) {
  window.open(src, '_blank', 'noopener,noreferrer')
}

async function copyImageBase64(src: string) {
  if (!src.startsWith('data:')) {
    setMessage('info', '当前图片不是 Base64 data URL，无法直接复制 Base64。')
    return
  }

  try {
    await navigator.clipboard.writeText(src)
    setMessage('success', 'Base64 data URL 已复制。')
  } catch {
    setMessage('error', '复制 Base64 失败，请检查浏览器剪贴板权限。')
  }
}

function downloadAllImages() {
  if (!resultImages.value.length) {
    setMessage('info', '当前没有可批量下载的图片。')
    return
  }

  resultImages.value.forEach((item, index) => {
    const anchor = document.createElement('a')
    anchor.href = item.src
    anchor.download = buildDownloadName(index, item.mimeType)
    anchor.click()
  })

  setMessage('success', `已触发 ${resultImages.value.length} 张图片的下载。`)
}

function applyGenerationPreset(presetKey: string) {
  const preset = generationPresets.find((item) => item.key === presetKey)
  if (!preset) return
  generationForm.prompt = preset.prompt
  generationForm.size = preset.size
  generationForm.quality = preset.quality
  generationForm.background = preset.background
  setMessage('success', `已应用“${preset.label}”预设。`)
}

function applyEditPreset(presetKey: string) {
  const preset = editPresets.find((item) => item.key === presetKey)
  if (!preset) return
  editForm.prompt = preset.prompt
  editForm.quality = preset.quality
  editForm.background = preset.background
  setMessage('success', `已应用“${preset.label}”预设。`)
}

function restoreLastSuccessfulRequest() {
  if (activeMode.value === 'generate') {
    if (!lastSuccessfulGeneratePayload.value) {
      setMessage('info', '当前还没有成功的生成请求可回填。')
      return
    }
    const payload = lastSuccessfulGeneratePayload.value
    generationForm.model = String(payload.model ?? generationForm.model)
    generationForm.prompt = String(payload.prompt ?? generationForm.prompt)
    generationForm.size = String(payload.size ?? generationForm.size)
    generationForm.quality = String(payload.quality ?? generationForm.quality)
    generationForm.background = String(payload.background ?? generationForm.background)
    generationForm.moderation = String(payload.moderation ?? generationForm.moderation)
    generationForm.outputFormat = String(payload.output_format ?? generationForm.outputFormat)
    generationForm.outputCompression = Number(payload.output_compression ?? generationForm.outputCompression)
    generationForm.n = Number(payload.n ?? generationForm.n)
    generationForm.user = String(payload.user ?? '')
    setMessage('success', '已回填最近一次成功的生成参数。')
    return
  }

  if (!lastSuccessfulEditPayload.value) {
    setMessage('info', '当前还没有成功的编辑请求可回填。')
    return
  }

  const payload = lastSuccessfulEditPayload.value
  editForm.model = String(payload.model ?? editForm.model)
  editForm.prompt = String(payload.prompt ?? editForm.prompt)
  editForm.size = String(payload.size ?? editForm.size)
  editForm.quality = String(payload.quality ?? editForm.quality)
  editForm.background = String(payload.background ?? editForm.background)
  editForm.moderation = String(payload.moderation ?? editForm.moderation)
  editForm.outputFormat = String(payload.output_format ?? editForm.outputFormat)
  editForm.outputCompression = Number(payload.output_compression ?? editForm.outputCompression)
  editForm.n = Number(payload.n ?? editForm.n)
  editForm.user = String(payload.user ?? '')
  setMessage('success', '已回填最近一次成功的编辑参数。')
}

</script>

<template>
  <MainShell>
    <section class="image-page">
      <header class="hero panel-card">
        <div class="hero__copy">
          <p class="section-kicker">Image Studio</p>
          <h2>GPT 图片生成与编辑控制台</h2>
          <p>
            单页同时支持 `POST /v1/images/generations` 与 `POST /v1/images/edits`。
            你可以直接在页面配置 `SUB2API_BASE` 和 `SUB2API_KEY`，并在文本生成与多图编辑之间自由切换。
          </p>
        </div>
        <div class="hero__actions">
          <button type="button" class="app-button app-button--secondary" :disabled="loading" @click="resetForms">
            <RefreshCw :size="16" />
            重置
          </button>
          <button type="button" class="app-button" :disabled="loading" @click="handleSubmit">
            <Send :size="16" />
            {{ loading ? (activeMode === 'generate' ? '生成中...' : '编辑中...') : (activeMode === 'generate' ? '立即生成' : '立即编辑') }}
          </button>
        </div>
      </header>

      <div class="mode-switch panel-card">
        <button type="button" class="mode-switch__item" :class="{ 'mode-switch__item--active': activeMode === 'generate' }" @click="activeMode = 'generate'">
          <Sparkles :size="16" />
          文本生成
        </button>
        <button type="button" class="mode-switch__item" :class="{ 'mode-switch__item--active': activeMode === 'edit' }" @click="activeMode = 'edit'">
          <Eraser :size="16" />
          图片编辑
        </button>
      </div>

      <div class="stats-grid">
        <article class="stat-card panel-card"><span class="stat-icon"><Sparkles :size="18" /></span><strong>{{ stats.modeLabel }}</strong><p>当前请求模式</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Image :size="18" /></span><strong>{{ stats.imageCount }}</strong><p>最近响应中的可预览图片数</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Send :size="18" /></span><strong>{{ stats.responseTime }}</strong><p>最近一次请求耗时</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Images :size="18" /></span><strong>{{ responseStatus || '--' }}</strong><p>最近一次上游 HTTP 状态</p></article>
      </div>

      <p v-if="message" class="notice" :class="`notice--${messageTone}`">{{ message }}</p>
      <p v-if="transparentHint" class="hint">{{ transparentHint }}</p>
      <p v-if="uploadValidationMessage" class="hint hint--warning">{{ uploadValidationMessage }}</p>

      <section class="progress-panel panel-card">
        <div class="progress-panel__head">
          <div>
            <p class="section-kicker">Processing</p>
            <strong>{{ loading ? '请求处理中' : '速度与等待反馈' }}</strong>
          </div>
          <div class="progress-panel__meta">
            <span>已耗时 {{ elapsedSecondsLabel }}</span>
            <span>预估 {{ estimatedSecondsLabel }}</span>
          </div>
        </div>
        <div class="progress-track" aria-hidden="true">
          <div class="progress-track__bar" :style="{ width: `${progressPercent}%` }" />
        </div>
        <div class="progress-panel__foot">
          <p>{{ progressPhase }}</p>
          <span>{{ loading ? '这是基于参数的前端估算进度，不是上游真实百分比。' : '下次请求会根据当前参数重新估算等待时间。' }}</span>
        </div>
        <div class="tip-list">
          <article v-for="tip in performanceTips" :key="tip" class="tip-chip">{{ tip }}</article>
        </div>
      </section>

      <div class="page-grid">
        <article class="panel-card section-card">
          <div class="section-head"><div><strong>接入配置</strong><p class="muted">本页面单独维护接入参数，不依赖平台内置模型配置。</p></div></div>
          <div class="form-grid">
            <label class="field form-grid__full"><span class="field__label">SUB2API_BASE</span><div class="input-shell"><input v-model="endpointForm.baseUrl" class="app-input" type="text" placeholder="https://api.hi-code.cc" /></div></label>
            <label class="field"><span class="field__label">SUB2API_KEY</span><div class="input-shell"><input v-model="endpointForm.apiKey" class="app-input" type="password" placeholder="sk-apikey" /></div></label>
            <label class="field"><span class="field__label">生成接口路径</span><div class="input-shell"><input v-model="endpointForm.generationPath" class="app-input" type="text" placeholder="/v1/images/generations" /></div></label>
            <label class="field"><span class="field__label">编辑接口路径</span><div class="input-shell"><input v-model="endpointForm.editsPath" class="app-input" type="text" placeholder="/v1/images/edits" /></div></label>
            <label class="toggle-card form-grid__full"><input v-model="endpointForm.persistSecret" type="checkbox" />在浏览器本地保存 API Key</label>
          </div>
          <div class="endpoint-card"><span class="field__label">当前生效请求地址</span><code>{{ fullEndpointUrl }}</code></div>
          <p class="muted">当前中转站更建议使用 `https://api-cn.hi-code.cc`。如果 `api.hi-code.cc` 出现超时或路由不稳定，优先切换到 `api-cn` 域名。</p>
        </article>

        <article class="panel-card section-card">
          <div class="section-head">
            <div>
              <strong>{{ activeMode === 'generate' ? 'Generation Params' : 'Edit Params' }}</strong>
              <p class="muted">
                {{ activeMode === 'generate'
                  ? '按照官方图片生成参数组织表单，同时保留可编辑模型名以兼容 SUB2API 映射。'
                  : '编辑模式使用 multipart/form-data，支持主图、可选 mask 和多张参考图。' }}
              </p>
            </div>
            <button type="button" class="app-button app-button--secondary" @click="restoreLastSuccessfulRequest">回填最近成功参数</button>
          </div>

          <div v-if="activeMode === 'generate'" class="form-grid">
            <div class="preset-row form-grid__full">
              <span class="field__label">常用预设</span>
              <div class="preset-actions">
                <button v-for="preset in generationPresets" :key="preset.key" type="button" class="mini-action" @click="applyGenerationPreset(preset.key)">{{ preset.label }}</button>
              </div>
            </div>
            <label class="field"><span class="field__label">模型</span><div class="input-shell"><input v-model="generationForm.model" class="app-input" type="text" placeholder="gpt-image-2" /></div></label>
            <label class="field"><span class="field__label">返回数量</span><div class="input-shell"><input v-model.number="generationForm.n" class="app-input" type="number" min="1" max="10" /></div></label>
            <label class="field form-grid__full"><span class="field__label">提示词</span><div class="input-shell input-shell--textarea"><textarea v-model="generationForm.prompt" class="app-textarea code-area" rows="6" placeholder="描述你想生成的图片内容" /></div></label>
            <label class="field"><span class="field__label">尺寸</span><select v-model="generationForm.size" class="app-select"><option value="1024x1024">1024x1024</option><option value="1536x1024">1536x1024</option><option value="1024x1536">1024x1536</option><option value="auto">auto</option></select></label>
            <label class="field"><span class="field__label">质量</span><select v-model="generationForm.quality" class="app-select"><option value="low">low</option><option value="medium">medium</option><option value="high">high</option><option value="auto">auto</option></select></label>
            <label class="field"><span class="field__label">背景</span><select v-model="generationForm.background" class="app-select"><option value="auto">auto</option><option value="opaque">opaque</option><option value="transparent">transparent</option></select></label>
            <label class="field"><span class="field__label">审核强度</span><select v-model="generationForm.moderation" class="app-select"><option value="auto">auto</option><option value="low">low</option></select></label>
            <label class="field"><span class="field__label">输出格式</span><select v-model="generationForm.outputFormat" class="app-select"><option value="png">png</option><option value="jpeg">jpeg</option><option value="webp">webp</option></select></label>
            <label class="field"><span class="field__label">压缩率</span><div class="input-shell"><input v-model.number="generationForm.outputCompression" class="app-input" type="number" min="0" max="100" /></div></label>
            <label class="field form-grid__full"><span class="field__label">用户标识</span><div class="input-shell"><input v-model="generationForm.user" class="app-input" type="text" placeholder="可选，终端用户标识" /></div></label>
            <label class="field form-grid__full"><span class="field__label">扩展参数 JSON</span><div class="input-shell input-shell--textarea"><textarea v-model="generationForm.extraBody" class="app-textarea code-area" rows="5" placeholder='{"style":"clean"}' /></div></label>
          </div>

          <div v-else class="stack">
            <div class="form-grid">
              <div class="preset-row form-grid__full">
                <span class="field__label">常用预设</span>
                <div class="preset-actions">
                  <button v-for="preset in editPresets" :key="preset.key" type="button" class="mini-action" @click="applyEditPreset(preset.key)">{{ preset.label }}</button>
                </div>
              </div>
              <label class="field"><span class="field__label">模型</span><div class="input-shell"><input v-model="editForm.model" class="app-input" type="text" placeholder="gpt-image-2" /></div></label>
              <label class="field"><span class="field__label">返回数量</span><div class="input-shell"><input v-model.number="editForm.n" class="app-input" type="number" min="1" max="10" /></div></label>
              <label class="field form-grid__full"><span class="field__label">编辑提示词</span><div class="input-shell input-shell--textarea"><textarea v-model="editForm.prompt" class="app-textarea code-area" rows="6" placeholder="描述你希望对图片进行的具体修改" /></div></label>
              <label class="field"><span class="field__label">尺寸</span><select v-model="editForm.size" class="app-select"><option value="1024x1024">1024x1024</option><option value="1536x1024">1536x1024</option><option value="1024x1536">1024x1536</option><option value="auto">auto</option></select></label>
              <label class="field"><span class="field__label">质量</span><select v-model="editForm.quality" class="app-select"><option value="low">low</option><option value="medium">medium</option><option value="high">high</option><option value="auto">auto</option></select></label>
              <label class="field"><span class="field__label">背景</span><select v-model="editForm.background" class="app-select"><option value="auto">auto</option><option value="opaque">opaque</option><option value="transparent">transparent</option></select></label>
              <label class="field"><span class="field__label">审核强度</span><select v-model="editForm.moderation" class="app-select"><option value="auto">auto</option><option value="low">low</option></select></label>
              <label class="field"><span class="field__label">输出格式</span><select v-model="editForm.outputFormat" class="app-select"><option value="png">png</option><option value="jpeg">jpeg</option><option value="webp">webp</option></select></label>
              <label class="field"><span class="field__label">压缩率</span><div class="input-shell"><input v-model.number="editForm.outputCompression" class="app-input" type="number" min="0" max="100" /></div></label>
              <label class="field form-grid__full"><span class="field__label">用户标识</span><div class="input-shell"><input v-model="editForm.user" class="app-input" type="text" placeholder="可选，终端用户标识" /></div></label>
              <label class="field form-grid__full"><span class="field__label">扩展表单 JSON</span><div class="input-shell input-shell--textarea"><textarea v-model="editForm.extraBody" class="app-textarea code-area" rows="5" placeholder='{"style":"clean"}' /></div></label>
            </div>

            <div class="optimizer-card">
              <div class="section-head">
                <div>
                  <strong>上传优化</strong>
                  <p class="muted">默认先在浏览器端缩边并压缩主图/参考图，减少上传体积和输入图像 token。Mask 不做处理。</p>
                </div>
              </div>
              <div class="form-grid">
                <label class="toggle-card form-grid__full"><input v-model="uploadOptimizerForm.enabled" type="checkbox" />启用上传前优化</label>
                <label class="field">
                  <span class="field__label">最长边</span>
                  <div class="input-shell"><input v-model.number="uploadOptimizerForm.maxLongEdge" class="app-input" type="number" min="1024" max="4096" step="128" /></div>
                </label>
                <label class="field">
                  <span class="field__label">JPEG 质量</span>
                  <div class="input-shell"><input v-model.number="uploadOptimizerForm.jpegQuality" class="app-input" type="number" min="0.5" max="0.95" step="0.01" /></div>
                </label>
              </div>
            </div>

            <div class="upload-grid">
              <label class="upload-card" :class="{ 'upload-card--dragover': primaryDragOver }" @dragover="handleDragOver($event, 'primary')" @dragleave="handleDragLeave('primary')" @drop="handleDrop($event, 'primary')">
                <div class="upload-card__head">
                  <span class="upload-card__label"><Upload :size="16" />主图源图</span>
                  <button v-if="primaryImagePreviews.length" type="button" class="mini-action" @click.prevent="clearPrimaryFile"><Trash2 :size="14" />清空全部</button>
                </div>
                <input class="upload-card__input" type="file" accept="image/*" multiple @change="onPrimaryImageChange" />
                <div v-if="primaryImagePreviews.length" class="reference-grid">
                  <article v-for="(item, index) in primaryImagePreviews" :key="item.id" class="reference-item">
                    <img :src="item.url" :alt="item.name" />
                    <strong>{{ item.name }}</strong>
                    <span>{{ item.sizeLabel }}</span>
                    <span>尺寸：{{ item.width ?? '--' }} x {{ item.height ?? '--' }}</span>
                    <span>比例：{{ item.aspectRatio }}</span>
                    <button type="button" class="mini-action mini-action--inline" @click.prevent="removePrimaryFile(index)"><Trash2 :size="14" />移除</button>
                  </article>
                </div>
                <p v-else class="upload-card__empty">必填。这里支持连续多次追加上传，多张图片会一起作为 `image[]` 发送给编辑接口。</p>
              </label>

              <label class="upload-card" :class="{ 'upload-card--dragover': maskDragOver }" @dragover="handleDragOver($event, 'mask')" @dragleave="handleDragLeave('mask')" @drop="handleDrop($event, 'mask')">
                <div class="upload-card__head">
                  <span class="upload-card__label"><Eraser :size="16" />Mask 图</span>
                  <button v-if="maskImagePreview" type="button" class="mini-action" @click.prevent="clearMaskFile"><Trash2 :size="14" />清空</button>
                </div>
                <input class="upload-card__input" type="file" accept="image/*" @change="onMaskImageChange" />
                <div v-if="maskImagePreview" class="upload-card__preview">
                  <img :src="maskImagePreview.url" :alt="maskImagePreview.name" />
                  <strong>{{ maskImagePreview.name }}</strong>
                  <span>{{ maskImagePreview.sizeLabel }}</span>
                  <span>尺寸：{{ maskImagePreview.width ?? '--' }} x {{ maskImagePreview.height ?? '--' }}</span>
                  <span>比例：{{ maskImagePreview.aspectRatio }}</span>
                </div>
                <p v-else class="upload-card__empty">可选。透明区域通常表示允许编辑的区域，可点击或拖拽上传。</p>
              </label>

              <label class="upload-card upload-card--wide" :class="{ 'upload-card--dragover': referenceDragOver }" @dragover="handleDragOver($event, 'reference')" @dragleave="handleDragLeave('reference')" @drop="handleDrop($event, 'reference')">
                <span class="upload-card__label"><Images :size="16" />参考图</span>
                <input class="upload-card__input" type="file" accept="image/*" multiple @change="onReferenceImagesChange" />
                <div v-if="referenceImagePreviews.length" class="reference-grid">
                  <article v-for="(item, index) in referenceImagePreviews" :key="item.id" class="reference-item">
                    <img :src="item.url" :alt="item.name" />
                    <strong>{{ item.name }}</strong>
                    <span>{{ item.sizeLabel }}</span>
                    <span>尺寸：{{ item.width ?? '--' }} x {{ item.height ?? '--' }}</span>
                    <span>比例：{{ item.aspectRatio }}</span>
                    <button type="button" class="mini-action mini-action--inline" @click.prevent="removeReferenceFile(index)"><Trash2 :size="14" />移除</button>
                  </article>
                </div>
                <p v-else class="upload-card__empty">可选。这里也是追加上传，不会覆盖前一次已选图片；主图和参考图合计最多 16 张。</p>
              </label>
            </div>
          </div>
        </article>
      </div>

      <article class="panel-card section-card results-panel">
        <div class="section-head">
          <div><strong>结果预览</strong><p class="muted">结果区直接铺开显示，优先渲染 `b64_json`，如果上游返回 `url` 也会自动兼容。</p></div>
          <button type="button" class="app-button app-button--secondary" @click="downloadAllImages"><Download :size="16" />批量下载</button>
        </div>
        <p v-if="responseUsageSummary" class="hint">{{ responseUsageSummary }}</p>
        <div v-if="!resultImages.length" class="empty-state">生成或编辑完成后，图片会显示在这里。</div>
        <div v-else class="gallery-grid gallery-grid--filled">
          <article v-for="(item, index) in resultImages" :key="item.id" class="gallery-card gallery-card--filled">
            <img :src="item.src" :alt="`Generated image ${index + 1}`" class="gallery-card__image gallery-card__image--filled" />
            <div class="gallery-card__meta">
              <p>结果 {{ index + 1 }}</p>
              <p v-if="item.revisedPrompt" class="muted">{{ item.revisedPrompt }}</p>
            </div>
            <div class="gallery-card__actions">
              <a class="app-button app-button--secondary gallery-card__download" :href="item.src" :download="buildDownloadName(index, item.mimeType)"><Download :size="16" />下载图片</a>
              <button type="button" class="app-button app-button--ghost gallery-card__download" @click="copyImageSource(item.src)"><Copy :size="16" />复制地址</button>
              <button type="button" class="app-button app-button--ghost gallery-card__download" @click="copyImageBase64(item.src)"><Copy :size="16" />复制 Base64</button>
              <button type="button" class="app-button app-button--ghost gallery-card__download" @click="openImageSource(item.src)"><Link2 :size="16" />打开原图</button>
            </div>
          </article>
        </div>
      </article>
    </section>
  </MainShell>
</template>

<style scoped>
.image-page { display: grid; gap: var(--layout-gap); min-width: 0; }
.hero, .section-head, .hero__actions { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
.hero { padding: var(--panel-padding); }
.hero__copy { display: grid; gap: 8px; max-width: 62rem; }
.section-kicker { margin: 0; color: #8fd0ff; font-size: .76rem; letter-spacing: .18em; text-transform: uppercase; }
.hero__copy h2 { margin: 0; color: var(--color-ink-strong); font-size: 1.7rem; line-height: 1.12; }
.hero__copy p:last-child, .muted { margin: 0; color: var(--color-ink-soft); line-height: 1.7; }
.progress-panel { display: grid; gap: 14px; padding: var(--panel-padding); }
.progress-panel__head, .progress-panel__meta, .progress-panel__foot { display: flex; gap: 12px; justify-content: space-between; align-items: center; flex-wrap: wrap; }
.progress-panel__head strong { color: var(--color-ink-strong); font-size: 1.05rem; }
.progress-panel__meta span, .progress-panel__foot span { color: var(--color-ink-soft); font-size: .92rem; }
.progress-panel__foot p { margin: 0; color: var(--color-ink-strong); }
.progress-track { position: relative; overflow: hidden; height: 14px; border-radius: 999px; background: rgba(255,255,255,.06); box-shadow: inset 0 0 0 1px rgba(255,255,255,.05); }
.progress-track__bar { height: 100%; border-radius: inherit; background: linear-gradient(90deg, #5aa9ff 0%, #90e6ff 52%, #c8f6ff 100%); box-shadow: 0 0 26px rgba(90,169,255,.28); transition: width 260ms ease; }
.tip-list { display: flex; flex-wrap: wrap; gap: 10px; }
.tip-chip { padding: 10px 12px; border: 1px solid rgba(255,255,255,.07); border-radius: 999px; color: var(--color-ink-soft); background: rgba(255,255,255,.03); line-height: 1.5; }
.mode-switch { display: inline-grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; padding: 10px; }
.mode-switch__item { display: inline-flex; align-items: center; justify-content: center; gap: 10px; min-height: 52px; border-radius: 18px; color: var(--color-ink-soft); background: rgba(255,255,255,.03); transition: background-color 180ms ease, color 180ms ease, transform 180ms ease, box-shadow 180ms ease; }
.mode-switch__item:hover { color: var(--color-ink-strong); background: rgba(255,255,255,.05); transform: translateY(-1px); }
.mode-switch__item--active { color: var(--color-ink-strong); background: linear-gradient(135deg, rgba(77,179,255,.16), rgba(77,179,255,.05)); box-shadow: inset 0 0 0 1px rgba(143,231,255,.16); }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; }
.stat-card { display: grid; gap: 8px; padding: 18px; }
.stat-card strong { color: var(--color-ink-strong); font-size: 1.5rem; line-height: 1.15; }
.stat-icon { display: inline-flex; align-items: center; justify-content: center; width: 38px; height: 38px; border-radius: 13px; color: #8fd0ff; background: rgba(102,186,255,.14); }
.notice, .hint { margin: 0; padding: 14px 16px; border-radius: 16px; line-height: 1.7; }
.notice { border: 1px solid rgba(255,255,255,.08); }
.notice--success { color: #9fe6be; background: rgba(56,161,105,.14); }
.notice--error { color: #ffb4b4; background: rgba(185,70,70,.16); }
.notice--info, .hint { color: #9fd4ff; background: rgba(46,110,180,.16); }
.hint--warning { color: #ffd7a3; background: rgba(186,118,32,.16); }
.page-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; align-items: start; }
.section-card { display: grid; gap: 16px; padding: var(--panel-padding); min-width: 0; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.form-grid__full { grid-column: 1 / -1; }
.field { display: grid; gap: 8px; min-width: 0; }
.field__label { color: var(--color-ink-muted); font-size: .78rem; letter-spacing: .08em; text-transform: uppercase; }
.input-shell { border: 1px solid rgba(255,255,255,.08); border-radius: 14px; background: rgba(10,18,32,.32); overflow: hidden; }
.input-shell--textarea { min-height: 110px; }
.app-input, .app-select, .app-textarea { width: 100%; padding: 12px 14px; border: 0; outline: none; font: inherit; color: var(--color-ink-strong); background: transparent; }
.app-select { border: 1px solid rgba(255,255,255,.08); border-radius: 14px; background: rgba(10,18,32,.32); }
.app-textarea { resize: vertical; min-height: 110px; }
.code-area, .endpoint-card code { font-family: var(--font-mono); }
.toggle-card { display: flex; align-items: center; gap: 10px; min-height: 50px; padding: 12px 14px; color: var(--color-ink-strong); border: 1px solid rgba(255,255,255,.08); border-radius: 14px; background: rgba(255,255,255,.03); }
.preset-row { display: grid; gap: 10px; }
.preset-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.endpoint-card { display: grid; gap: 10px; padding: 14px; border: 1px solid rgba(255,255,255,.08); border-radius: 16px; background: rgba(255,255,255,.03); }
.endpoint-card code { color: var(--color-ink-strong); word-break: break-all; }
.optimizer-card { display: grid; gap: 14px; padding: 14px; border: 1px solid rgba(255,255,255,.08); border-radius: 18px; background: rgba(255,255,255,.03); }
.upload-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.upload-card { display: grid; gap: 12px; padding: 14px; border: 1px solid rgba(255,255,255,.08); border-radius: 18px; background: rgba(255,255,255,.03); }
.upload-card--dragover { border-color: rgba(143,231,255,.4); background: rgba(77,179,255,.08); box-shadow: inset 0 0 0 1px rgba(143,231,255,.18); }
.upload-card--wide { grid-column: 1 / -1; }
.upload-card__head { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
.upload-card__label { display: inline-flex; align-items: center; gap: 8px; color: var(--color-ink-strong); font-weight: 600; }
.upload-card__input { color: var(--color-ink-soft); }
.upload-card__preview { display: grid; gap: 8px; }
.mini-action { display: inline-flex; align-items: center; justify-content: center; gap: 6px; min-height: 34px; padding: 0 12px; border-radius: 999px; color: var(--color-ink-soft); background: rgba(255,255,255,.06); }
.mini-action--inline { justify-self: start; }
.upload-card__preview img, .reference-item img { width: 100%; max-height: 220px; object-fit: cover; border-radius: 14px; background: rgba(255,255,255,.04); }
.upload-card__preview strong, .reference-item strong { color: var(--color-ink-strong); font-size: .92rem; line-height: 1.4; }
.upload-card__preview span, .reference-item span, .upload-card__empty { color: var(--color-ink-soft); line-height: 1.6; }
.reference-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
.reference-item { display: grid; gap: 8px; }
.empty-state { display: grid; place-items: center; min-height: 240px; border: 1px dashed rgba(255,255,255,.1); border-radius: 18px; color: var(--color-ink-soft); text-align: center; }
.gallery-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
.gallery-grid--filled { grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); }
.gallery-card { display: grid; gap: 14px; padding: 14px; border: 1px solid rgba(255,255,255,.08); border-radius: 18px; background: rgba(255,255,255,.03); }
.gallery-card--filled { align-content: start; }
.gallery-card__image { width: 100%; aspect-ratio: 1 / 1; object-fit: cover; border-radius: 14px; background: rgba(255,255,255,.04); }
.gallery-card__image--filled { aspect-ratio: 4 / 3; }
.gallery-card__meta { display: grid; gap: 6px; }
.gallery-card__meta p { margin: 0; word-break: break-word; }
.gallery-card__actions { display: grid; gap: 10px; }
.gallery-card__download { width: 100%; justify-content: center; }
.results-panel { display: grid; gap: 18px; }
@media (max-width: 1100px) { .page-grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .form-grid, .upload-grid, .mode-switch { grid-template-columns: 1fr; } }
</style>
