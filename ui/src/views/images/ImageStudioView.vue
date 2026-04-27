<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { Download, Eraser, Image, Images, RefreshCw, Send, Sparkles, Upload } from 'lucide-vue-next'
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

const endpointForm = reactive({
  baseUrl: 'https://api.hi-code.cc',
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

const primaryImageFile = ref<File | null>(null)
const maskImageFile = ref<File | null>(null)
const referenceImageFiles = ref<File[]>([])
const primaryImagePreview = ref<UploadPreviewItem | null>(null)
const maskImagePreview = ref<UploadPreviewItem | null>(null)
const referenceImagePreviews = ref<UploadPreviewItem[]>([])

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

function toPreviewItem(file: File, suffix: string): UploadPreviewItem {
  return {
    id: `${suffix}-${file.name}-${file.size}-${file.lastModified}`,
    name: file.name,
    sizeLabel: formatBytes(file.size),
    url: URL.createObjectURL(file),
  }
}

function setPrimaryFile(file: File | null) {
  revokePreview(primaryImagePreview.value)
  primaryImageFile.value = file
  primaryImagePreview.value = file ? toPreviewItem(file, 'primary') : null
}

function setMaskFile(file: File | null) {
  revokePreview(maskImagePreview.value)
  maskImageFile.value = file
  maskImagePreview.value = file ? toPreviewItem(file, 'mask') : null
}

function setReferenceFiles(files: File[]) {
  revokePreviewList(referenceImagePreviews.value)
  referenceImageFiles.value = files
  referenceImagePreviews.value = files.map((file, index) => toPreviewItem(file, `reference-${index}`))
}

onBeforeUnmount(() => {
  revokePreview(primaryImagePreview.value)
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
    }
    if (parsed.activeMode) activeMode.value = parsed.activeMode
    Object.assign(endpointForm, parsed.endpointForm ?? {})
    Object.assign(generationForm, parsed.generationForm ?? {})
    Object.assign(editForm, parsed.editForm ?? {})
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
  }))
}

loadLocalConfig()

watch([activeMode, endpointForm, generationForm, editForm], () => {
  persistLocalConfig()
}, { deep: true })

const effectivePath = computed(() => activeMode.value === 'generate' ? endpointForm.generationPath : endpointForm.editsPath)

const fullEndpointUrl = computed(() => {
  const baseUrl = endpointForm.baseUrl.trim().replace(/\/+$/, '')
  return `${baseUrl}${normalizePath(effectivePath.value)}`
})

const stats = computed(() => ({
  modeLabel: activeMode.value === 'generate' ? 'Generation' : 'Edit',
  imageCount: resultImages.value.length,
  responseTime: responseTimeMs.value ? `${responseTimeMs.value} ms` : '--',
}))

const transparentHint = computed(() => {
  const form = activeMode.value === 'generate' ? generationForm : editForm
  return form.background === 'transparent'
    ? 'Transparent background support depends on the upstream model mapping. If SUB2API rejects it, switch to opaque or auto.'
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
    throw new Error(getErrorMessage(error, 'Failed to parse extra body JSON.'))
  }
}

function buildGenerationPayload() {
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

  const images = [primaryImageFile.value, ...referenceImageFiles.value].filter((item): item is File => Boolean(item))
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
      throw new Error(`The upstream returned a non-JSON error body. HTTP ${response.status}.`)
    }
  }

  if (!response.ok) {
    throw new Error(parsed.error?.message || `Image request failed. HTTP ${response.status}.`)
  }

  return parsed
}

function resetResultState() {
  responseStatus.value = ''
  responseTimeMs.value = null
  rawResponse.value = ''
  resultImages.value = []
  lastRequestPayload.value = null
}

async function handleGenerate() {
  if (!endpointForm.baseUrl.trim()) return setMessage('error', 'Please provide SUB2API_BASE.')
  if (!endpointForm.apiKey.trim()) return setMessage('error', 'Please provide SUB2API_KEY.')
  if (!generationForm.model.trim()) return setMessage('error', 'Please provide the image model name.')
  if (!generationForm.prompt.trim()) return setMessage('error', 'Please provide a generation prompt.')

  loading.value = true
  resetResultState()

  try {
    const payload = buildGenerationPayload()
    const startedAt = Date.now()
    lastRequestPayload.value = payload

    const response = await fetch(fullEndpointUrl.value, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${endpointForm.apiKey.trim()}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })

    const parsed = await readResponse(response)
    responseTimeMs.value = Date.now() - startedAt
    resultImages.value = normalizeResults(parsed, payload)

    if (!resultImages.value.length) {
      return setMessage('info', 'The request succeeded but no previewable image was found in the response body.')
    }

    setMessage('success', `Generation completed with ${resultImages.value.length} image result(s).`)
  } catch (error) {
    setMessage('error', getErrorMessage(error, 'Generation failed.'))
  } finally {
    loading.value = false
  }
}

async function handleEdit() {
  if (!endpointForm.baseUrl.trim()) return setMessage('error', 'Please provide SUB2API_BASE.')
  if (!endpointForm.apiKey.trim()) return setMessage('error', 'Please provide SUB2API_KEY.')
  if (!editForm.model.trim()) return setMessage('error', 'Please provide the image model name.')
  if (!editForm.prompt.trim()) return setMessage('error', 'Please provide an edit prompt.')
  if (!primaryImageFile.value) return setMessage('error', 'Please upload the primary image for editing.')

  loading.value = true
  resetResultState()

  try {
    const { formData, summary } = buildEditRequest()
    const startedAt = Date.now()
    lastRequestPayload.value = summary

    const response = await fetch(fullEndpointUrl.value, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${endpointForm.apiKey.trim()}`,
      },
      body: formData,
    })

    const parsed = await readResponse(response)
    responseTimeMs.value = Date.now() - startedAt
    resultImages.value = normalizeResults(parsed, summary)

    if (!resultImages.value.length) {
      return setMessage('info', 'The edit request succeeded but no previewable image was found in the response body.')
    }

    setMessage('success', `Edit completed with ${resultImages.value.length} image result(s).`)
  } catch (error) {
    setMessage('error', getErrorMessage(error, 'Edit request failed.'))
  } finally {
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

  setPrimaryFile(null)
  setMaskFile(null)
  setReferenceFiles([])
  resetResultState()
  setMessage('info', 'Generation and edit parameters were reset.')
}

function onPrimaryImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  setPrimaryFile(input.files?.[0] ?? null)
}

function onMaskImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  setMaskFile(input.files?.[0] ?? null)
}

function onReferenceImagesChange(event: Event) {
  const input = event.target as HTMLInputElement
  setReferenceFiles(Array.from(input.files ?? []))
}

function buildDownloadName(index: number, mimeType: string) {
  const extension = mimeType === 'image/jpeg' ? 'jpg' : mimeType === 'image/webp' ? 'webp' : 'png'
  return `image-studio-${activeMode.value}-${index + 1}.${extension}`
}

const curlPreview = computed(() => {
  if (activeMode.value === 'generate') {
    let payload: Record<string, unknown> = {}
    try {
      payload = lastRequestPayload.value ?? buildGenerationPayload()
    } catch {
      payload = {}
    }

    return [
      `curl -sS "${fullEndpointUrl.value}" \\`,
      '  -H "Authorization: Bearer ***" \\',
      '  -H "Content-Type: application/json" \\',
      `  --data '${JSON.stringify(payload, null, 2)}'`,
    ].join('\n')
  }

  const previewLines = [
    `curl -sS "${fullEndpointUrl.value}" \\`,
    '  -H "Authorization: Bearer ***" \\',
    `  -F "model=${editForm.model}" \\`,
    `  -F "prompt=${editForm.prompt}" \\`,
    `  -F "size=${editForm.size}" \\`,
    `  -F "quality=${editForm.quality}" \\`,
    `  -F "background=${editForm.background}" \\`,
    `  -F "moderation=${editForm.moderation}" \\`,
    `  -F "output_format=${editForm.outputFormat}" \\`,
    `  -F "n=${editForm.n}" \\`,
  ]

  if (editForm.outputFormat === 'jpeg' || editForm.outputFormat === 'webp') {
    previewLines.push(`  -F "output_compression=${editForm.outputCompression}" \\`)
  }
  if (editForm.user.trim()) {
    previewLines.push(`  -F "user=${editForm.user.trim()}" \\`)
  }
  if (primaryImageFile.value) {
    previewLines.push(`  -F "image[]=@${primaryImageFile.value.name}" \\`)
  }
  referenceImageFiles.value.forEach((file) => {
    previewLines.push(`  -F "image[]=@${file.name}" \\`)
  })
  if (maskImageFile.value) {
    previewLines.push(`  -F "mask=@${maskImageFile.value.name}" \\`)
  }

  return previewLines.join('\n')
})
</script>

<template>
  <MainShell>
    <section class="image-page">
      <header class="hero panel-card">
        <div class="hero__copy">
          <p class="section-kicker">Image Studio</p>
          <h2>GPT Image Generation And Edit Console</h2>
          <p>
            One standalone page for both `POST /v1/images/generations` and `POST /v1/images/edits`.
            Configure `SUB2API_BASE` and `SUB2API_KEY` directly on the page, then switch between text-only generation and multipart image editing.
          </p>
        </div>
        <div class="hero__actions">
          <button type="button" class="app-button app-button--secondary" :disabled="loading" @click="resetForms">
            <RefreshCw :size="16" />
            Reset
          </button>
          <button type="button" class="app-button" :disabled="loading" @click="handleSubmit">
            <Send :size="16" />
            {{ loading ? (activeMode === 'generate' ? 'Generating...' : 'Editing...') : (activeMode === 'generate' ? 'Run Generation' : 'Run Edit') }}
          </button>
        </div>
      </header>

      <div class="mode-switch panel-card">
        <button type="button" class="mode-switch__item" :class="{ 'mode-switch__item--active': activeMode === 'generate' }" @click="activeMode = 'generate'">
          <Sparkles :size="16" />
          Text Generation
        </button>
        <button type="button" class="mode-switch__item" :class="{ 'mode-switch__item--active': activeMode === 'edit' }" @click="activeMode = 'edit'">
          <Eraser :size="16" />
          Image Edit
        </button>
      </div>

      <div class="stats-grid">
        <article class="stat-card panel-card"><span class="stat-icon"><Sparkles :size="18" /></span><strong>{{ stats.modeLabel }}</strong><p>Current request mode</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Image :size="18" /></span><strong>{{ stats.imageCount }}</strong><p>Previewable images in the latest response</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Send :size="18" /></span><strong>{{ stats.responseTime }}</strong><p>Latest request duration</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Images :size="18" /></span><strong>{{ responseStatus || '--' }}</strong><p>Latest upstream HTTP status</p></article>
      </div>

      <p v-if="message" class="notice" :class="`notice--${messageTone}`">{{ message }}</p>
      <p v-if="transparentHint" class="hint">{{ transparentHint }}</p>

      <div class="page-grid">
        <article class="panel-card section-card">
          <div class="section-head"><div><strong>Endpoint Config</strong><p class="muted">The page keeps its own connection settings and does not depend on the platform model registry.</p></div></div>
          <div class="form-grid">
            <label class="field form-grid__full"><span class="field__label">SUB2API_BASE</span><div class="input-shell"><input v-model="endpointForm.baseUrl" class="app-input" type="text" placeholder="https://api.hi-code.cc" /></div></label>
            <label class="field"><span class="field__label">SUB2API_KEY</span><div class="input-shell"><input v-model="endpointForm.apiKey" class="app-input" type="password" placeholder="sk-apikey" /></div></label>
            <label class="field"><span class="field__label">Generation Path</span><div class="input-shell"><input v-model="endpointForm.generationPath" class="app-input" type="text" placeholder="/v1/images/generations" /></div></label>
            <label class="field"><span class="field__label">Edits Path</span><div class="input-shell"><input v-model="endpointForm.editsPath" class="app-input" type="text" placeholder="/v1/images/edits" /></div></label>
            <label class="toggle-card form-grid__full"><input v-model="endpointForm.persistSecret" type="checkbox" />Persist API key in local storage</label>
          </div>
          <div class="endpoint-card"><span class="field__label">Effective Request URL</span><code>{{ fullEndpointUrl }}</code></div>
        </article>

        <article class="panel-card section-card">
          <div class="section-head">
            <div>
              <strong>{{ activeMode === 'generate' ? 'Generation Params' : 'Edit Params' }}</strong>
              <p class="muted">
                {{ activeMode === 'generate'
                  ? 'Use the official image generation parameters with an editable model name for SUB2API compatibility.'
                  : 'Edit mode uses multipart form uploads and supports a primary image, optional mask, and extra reference images.' }}
              </p>
            </div>
          </div>

          <div v-if="activeMode === 'generate'" class="form-grid">
            <label class="field"><span class="field__label">Model</span><div class="input-shell"><input v-model="generationForm.model" class="app-input" type="text" placeholder="gpt-image-2" /></div></label>
            <label class="field"><span class="field__label">Count</span><div class="input-shell"><input v-model.number="generationForm.n" class="app-input" type="number" min="1" max="10" /></div></label>
            <label class="field form-grid__full"><span class="field__label">Prompt</span><div class="input-shell input-shell--textarea"><textarea v-model="generationForm.prompt" class="app-textarea code-area" rows="6" placeholder="Describe the image you want to generate" /></div></label>
            <label class="field"><span class="field__label">Size</span><select v-model="generationForm.size" class="app-select"><option value="1024x1024">1024x1024</option><option value="1536x1024">1536x1024</option><option value="1024x1536">1024x1536</option><option value="auto">auto</option></select></label>
            <label class="field"><span class="field__label">Quality</span><select v-model="generationForm.quality" class="app-select"><option value="low">low</option><option value="medium">medium</option><option value="high">high</option><option value="auto">auto</option></select></label>
            <label class="field"><span class="field__label">Background</span><select v-model="generationForm.background" class="app-select"><option value="auto">auto</option><option value="opaque">opaque</option><option value="transparent">transparent</option></select></label>
            <label class="field"><span class="field__label">Moderation</span><select v-model="generationForm.moderation" class="app-select"><option value="auto">auto</option><option value="low">low</option></select></label>
            <label class="field"><span class="field__label">Output Format</span><select v-model="generationForm.outputFormat" class="app-select"><option value="png">png</option><option value="jpeg">jpeg</option><option value="webp">webp</option></select></label>
            <label class="field"><span class="field__label">Compression</span><div class="input-shell"><input v-model.number="generationForm.outputCompression" class="app-input" type="number" min="0" max="100" /></div></label>
            <label class="field form-grid__full"><span class="field__label">User</span><div class="input-shell"><input v-model="generationForm.user" class="app-input" type="text" placeholder="Optional end-user identifier" /></div></label>
            <label class="field form-grid__full"><span class="field__label">Extra Body JSON</span><div class="input-shell input-shell--textarea"><textarea v-model="generationForm.extraBody" class="app-textarea code-area" rows="5" placeholder='{"style":"clean"}' /></div></label>
          </div>

          <div v-else class="stack">
            <div class="form-grid">
              <label class="field"><span class="field__label">Model</span><div class="input-shell"><input v-model="editForm.model" class="app-input" type="text" placeholder="gpt-image-2" /></div></label>
              <label class="field"><span class="field__label">Count</span><div class="input-shell"><input v-model.number="editForm.n" class="app-input" type="number" min="1" max="10" /></div></label>
              <label class="field form-grid__full"><span class="field__label">Edit Prompt</span><div class="input-shell input-shell--textarea"><textarea v-model="editForm.prompt" class="app-textarea code-area" rows="6" placeholder="Describe the exact transformation you want" /></div></label>
              <label class="field"><span class="field__label">Size</span><select v-model="editForm.size" class="app-select"><option value="1024x1024">1024x1024</option><option value="1536x1024">1536x1024</option><option value="1024x1536">1024x1536</option><option value="auto">auto</option></select></label>
              <label class="field"><span class="field__label">Quality</span><select v-model="editForm.quality" class="app-select"><option value="low">low</option><option value="medium">medium</option><option value="high">high</option><option value="auto">auto</option></select></label>
              <label class="field"><span class="field__label">Background</span><select v-model="editForm.background" class="app-select"><option value="auto">auto</option><option value="opaque">opaque</option><option value="transparent">transparent</option></select></label>
              <label class="field"><span class="field__label">Moderation</span><select v-model="editForm.moderation" class="app-select"><option value="auto">auto</option><option value="low">low</option></select></label>
              <label class="field"><span class="field__label">Output Format</span><select v-model="editForm.outputFormat" class="app-select"><option value="png">png</option><option value="jpeg">jpeg</option><option value="webp">webp</option></select></label>
              <label class="field"><span class="field__label">Compression</span><div class="input-shell"><input v-model.number="editForm.outputCompression" class="app-input" type="number" min="0" max="100" /></div></label>
              <label class="field form-grid__full"><span class="field__label">User</span><div class="input-shell"><input v-model="editForm.user" class="app-input" type="text" placeholder="Optional end-user identifier" /></div></label>
              <label class="field form-grid__full"><span class="field__label">Extra Form Fields JSON</span><div class="input-shell input-shell--textarea"><textarea v-model="editForm.extraBody" class="app-textarea code-area" rows="5" placeholder='{"style":"clean"}' /></div></label>
            </div>

            <div class="upload-grid">
              <label class="upload-card">
                <span class="upload-card__label"><Upload :size="16" />Primary Image</span>
                <input class="upload-card__input" type="file" accept="image/*" @change="onPrimaryImageChange" />
                <div v-if="primaryImagePreview" class="upload-card__preview">
                  <img :src="primaryImagePreview.url" :alt="primaryImagePreview.name" />
                  <strong>{{ primaryImagePreview.name }}</strong>
                  <span>{{ primaryImagePreview.sizeLabel }}</span>
                </div>
                <p v-else class="upload-card__empty">Required. The base image to modify.</p>
              </label>

              <label class="upload-card">
                <span class="upload-card__label"><Eraser :size="16" />Mask Image</span>
                <input class="upload-card__input" type="file" accept="image/*" @change="onMaskImageChange" />
                <div v-if="maskImagePreview" class="upload-card__preview">
                  <img :src="maskImagePreview.url" :alt="maskImagePreview.name" />
                  <strong>{{ maskImagePreview.name }}</strong>
                  <span>{{ maskImagePreview.sizeLabel }}</span>
                </div>
                <p v-else class="upload-card__empty">Optional. Transparent regions indicate editable areas.</p>
              </label>

              <label class="upload-card upload-card--wide">
                <span class="upload-card__label"><Images :size="16" />Reference Images</span>
                <input class="upload-card__input" type="file" accept="image/*" multiple @change="onReferenceImagesChange" />
                <div v-if="referenceImagePreviews.length" class="reference-grid">
                  <article v-for="item in referenceImagePreviews" :key="item.id" class="reference-item">
                    <img :src="item.url" :alt="item.name" />
                    <strong>{{ item.name }}</strong>
                    <span>{{ item.sizeLabel }}</span>
                  </article>
                </div>
                <p v-else class="upload-card__empty">Optional. Add extra images to guide composition or style.</p>
              </label>
            </div>
          </div>
        </article>
      </div>

      <div class="page-grid page-grid--results">
        <article class="panel-card section-card">
          <div class="section-head"><div><strong>Result Gallery</strong><p class="muted">The page first tries `b64_json`, and falls back to direct image URLs when the upstream returns them.</p></div></div>
          <div v-if="!resultImages.length" class="empty-state">Generated or edited images will appear here after the request completes.</div>
          <div v-else class="gallery-grid">
            <article v-for="(item, index) in resultImages" :key="item.id" class="gallery-card">
              <img :src="item.src" :alt="`Generated image ${index + 1}`" class="gallery-card__image" />
              <div class="gallery-card__meta">
                <p>Result {{ index + 1 }}</p>
                <p v-if="item.revisedPrompt" class="muted">{{ item.revisedPrompt }}</p>
              </div>
              <a class="app-button app-button--secondary gallery-card__download" :href="item.src" :download="buildDownloadName(index, item.mimeType)"><Download :size="16" />Download</a>
            </article>
          </div>
        </article>

        <article class="panel-card section-card">
          <div class="section-head"><div><strong>Request Inspector</strong><p class="muted">Use this panel to compare the page request against the official API contract and your SUB2API behavior.</p></div></div>
          <div class="stack">
            <div class="code-block"><span class="field__label">cURL Preview</span><pre class="code-panel">{{ curlPreview }}</pre></div>
            <div class="code-block"><span class="field__label">Last Payload Summary</span><pre class="code-panel">{{ lastRequestPayload ? JSON.stringify(lastRequestPayload, null, 2) : 'No request has been sent yet.' }}</pre></div>
            <div class="code-block"><span class="field__label">Raw Response</span><pre class="code-panel">{{ rawResponse || 'No response yet.' }}</pre></div>
          </div>
        </article>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.image-page { display: grid; gap: var(--layout-gap); min-width: 0; }
.hero, .section-head, .hero__actions { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
.hero { padding: var(--panel-padding); }
.hero__copy { display: grid; gap: 8px; max-width: 62rem; }
.hero__copy h2 { margin: 0; color: var(--color-ink-strong); font-size: 1.7rem; line-height: 1.12; }
.hero__copy p:last-child, .muted { margin: 0; color: var(--color-ink-soft); line-height: 1.7; }
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
.page-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; align-items: start; }
.page-grid--results { grid-template-columns: minmax(0, 1.15fr) minmax(320px, .85fr); }
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
.code-area, .code-panel, .endpoint-card code { font-family: var(--font-mono); }
.toggle-card { display: flex; align-items: center; gap: 10px; min-height: 50px; padding: 12px 14px; color: var(--color-ink-strong); border: 1px solid rgba(255,255,255,.08); border-radius: 14px; background: rgba(255,255,255,.03); }
.endpoint-card { display: grid; gap: 10px; padding: 14px; border: 1px solid rgba(255,255,255,.08); border-radius: 16px; background: rgba(255,255,255,.03); }
.endpoint-card code { color: var(--color-ink-strong); word-break: break-all; }
.upload-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.upload-card { display: grid; gap: 12px; padding: 14px; border: 1px solid rgba(255,255,255,.08); border-radius: 18px; background: rgba(255,255,255,.03); }
.upload-card--wide { grid-column: 1 / -1; }
.upload-card__label { display: inline-flex; align-items: center; gap: 8px; color: var(--color-ink-strong); font-weight: 600; }
.upload-card__input { color: var(--color-ink-soft); }
.upload-card__preview { display: grid; gap: 8px; }
.upload-card__preview img, .reference-item img { width: 100%; max-height: 220px; object-fit: cover; border-radius: 14px; background: rgba(255,255,255,.04); }
.upload-card__preview strong, .reference-item strong { color: var(--color-ink-strong); font-size: .92rem; line-height: 1.4; }
.upload-card__preview span, .reference-item span, .upload-card__empty { color: var(--color-ink-soft); line-height: 1.6; }
.reference-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
.reference-item { display: grid; gap: 8px; }
.empty-state { display: grid; place-items: center; min-height: 240px; border: 1px dashed rgba(255,255,255,.1); border-radius: 18px; color: var(--color-ink-soft); text-align: center; }
.gallery-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
.gallery-card { display: grid; gap: 14px; padding: 14px; border: 1px solid rgba(255,255,255,.08); border-radius: 18px; background: rgba(255,255,255,.03); }
.gallery-card__image { width: 100%; aspect-ratio: 1 / 1; object-fit: cover; border-radius: 14px; background: rgba(255,255,255,.04); }
.gallery-card__meta { display: grid; gap: 6px; }
.gallery-card__meta p { margin: 0; word-break: break-word; }
.gallery-card__download { width: 100%; justify-content: center; }
.stack, .code-block { display: grid; gap: 10px; }
.code-panel { margin: 0; padding: 14px; color: var(--color-ink); white-space: pre-wrap; word-break: break-word; border: 1px solid rgba(255,255,255,.08); border-radius: 16px; background: rgba(10,18,32,.32); }
@media (max-width: 1100px) { .page-grid, .page-grid--results { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .form-grid, .upload-grid, .mode-switch { grid-template-columns: 1fr; } }
</style>
