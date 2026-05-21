<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import {
  RefreshCw,
  ScanSearch,
  Trash2,
  Upload,
} from 'lucide-vue-next'
import MainShell from '@/components/MainShell.vue'
import { detectImage } from '@/api/opencv'
import { getErrorMessage } from '@/utils/errors'
import type { OpenCvDetectResult, OpenCvDetectionItem } from '@/types/opencv'

interface PreviewState {
  url: string
  width: number
  height: number
  aspectRatio: string
}

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const preview = ref<PreviewState | null>(null)
const detecting = ref(false)
const feedback = ref('')
const feedbackTone = ref<'info' | 'success' | 'error'>('info')
const result = ref<OpenCvDetectResult | null>(null)
const rawResponse = ref('')

const form = reactive({
  businessScene: 'KITCHEN_ASSISTANT',
})

function setFeedback(tone: 'info' | 'success' | 'error', message: string) {
  feedbackTone.value = tone
  feedback.value = message
}

function revokePreview() {
  if (preview.value?.url) {
    URL.revokeObjectURL(preview.value.url)
  }
}

function formatBytes(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

async function readImageMeta(url: string) {
  return await new Promise<{ width: number; height: number; aspectRatio: string }>((resolve) => {
    const image = new Image()
    image.onload = () => {
      const width = image.naturalWidth || image.width
      const height = image.naturalHeight || image.height
      resolve({
        width,
        height,
        aspectRatio: width && height ? `${width}:${height}` : '--',
      })
    }
    image.onerror = () => resolve({ width: 0, height: 0, aspectRatio: '--' })
    image.src = url
  })
}

async function loadFile(file: File) {
  revokePreview()
  selectedFile.value = file
  result.value = null
  rawResponse.value = ''
  const url = URL.createObjectURL(file)
  const meta = await readImageMeta(url)
  preview.value = { url, ...meta }
}

function clearFile() {
  revokePreview()
  selectedFile.value = null
  preview.value = null
  result.value = null
  rawResponse.value = ''
  feedback.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

function toBase64(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const value = String(reader.result || '')
      resolve(value.includes(',') ? value.split(',')[1] : value)
    }
    reader.onerror = () => reject(new Error('读取图片失败'))
    reader.readAsDataURL(file)
  })
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] ?? null
  if (file) await loadFile(file)
}

async function handleDrop(event: DragEvent) {
  event.preventDefault()
  const file = event.dataTransfer?.files?.[0] ?? null
  if (file) await loadFile(file)
}

async function submitDetect() {
  if (!selectedFile.value) {
    setFeedback('error', '请先上传图片')
    return
  }

  detecting.value = true
  try {
    const payload = {
      imageBase64: await toBase64(selectedFile.value),
      imageFormat: selectedFile.value.type.split('/').pop() || 'jpg',
      businessScene: form.businessScene,
    }
    const data = await detectImage(payload)
    result.value = data
    rawResponse.value = JSON.stringify(data, null, 2)
    setFeedback('success', '识别完成')
  } catch (error) {
    setFeedback('error', getErrorMessage(error))
  } finally {
    detecting.value = false
  }
}

const detectionBoxes = computed(() => result.value?.detections ?? [])

const displayScale = computed(() => {
  if (!preview.value || !result.value) return 1
  const maxWidth = 720
  return Math.min(1, maxWidth / Math.max(preview.value.width || 1, 1))
})

const displayImageStyle = computed(() => {
  if (!preview.value) return {}
  return {
    width: `${preview.value.width * displayScale.value}px`,
    height: `${preview.value.height * displayScale.value}px`,
  }
})

function boxStyle(item: OpenCvDetectionItem) {
  const scale = displayScale.value
  return {
    left: `${item.x * scale}px`,
    top: `${item.y * scale}px`,
    width: `${item.width * scale}px`,
    height: `${item.height * scale}px`,
  }
}

onBeforeUnmount(() => {
  revokePreview()
})
</script>

<template>
  <MainShell>
    <section class="opencv-page">
      <article class="panel-card hero-card">
        <div>
          <p class="section-kicker">OpenCV / YOLO Demo</p>
          <h2>食材识别</h2>
          <p>上传厨房图片，调用后端 `/image/detect`，直接查看识别框、置信度和模型输出。</p>
        </div>
        <div class="hero-chip">
          <ScanSearch :size="18" />
          <span>{{ result ? `${result.detectCount} 个目标` : '等待识别' }}</span>
        </div>
      </article>

      <article class="panel-card control-card">
        <div class="upload-zone" :class="{ 'upload-zone--active': !!preview }" @dragover.prevent @drop="handleDrop">
          <input ref="fileInput" class="upload-zone__input" type="file" accept="image/*" @change="onFileChange" />
          <Upload :size="22" />
          <strong>拖入图片或点击上传</strong>
          <span>支持 JPG / PNG / WEBP</span>
          <button v-if="selectedFile" type="button" class="app-button app-button--secondary" @click="clearFile">
            <Trash2 :size="16" />
            清空
          </button>
        </div>

        <div class="form-row">
          <label class="field">
            <span class="field__label">业务场景</span>
            <div class="input-shell">
              <input v-model="form.businessScene" class="app-input" type="text" />
            </div>
          </label>
          <button type="button" class="app-button" :disabled="detecting" @click="submitDetect">
            <RefreshCw v-if="detecting" :size="16" class="spin" />
            <ScanSearch v-else :size="16" />
            {{ detecting ? '识别中...' : '开始识别' }}
          </button>
        </div>

        <p v-if="selectedFile" class="meta-line">
          {{ selectedFile.name }} · {{ formatBytes(selectedFile.size) }}
          <span v-if="preview">· {{ preview.width }} x {{ preview.height }}</span>
        </p>

        <p v-if="feedback" class="notice" :class="`notice--${feedbackTone}`">{{ feedback }}</p>
      </article>

      <article class="panel-card result-card">
        <div class="section-head">
          <strong>识别结果</strong>
          <span v-if="result" class="muted">{{ result.modelName }} / {{ result.modelVersion }} / {{ result.costTimeMs }} ms</span>
        </div>

        <div v-if="preview" class="image-frame">
          <img :src="preview.url" alt="preview" class="image-frame__img" :style="displayImageStyle" />
          <div v-if="result" class="overlay" :style="displayImageStyle">
            <div v-for="item in detectionBoxes" :key="`${item.classCode}-${item.x}-${item.y}`" class="box" :style="boxStyle(item)">
              <span>{{ item.label }} {{ Math.round(item.confidence * 100) }}%</span>
            </div>
          </div>
        </div>

        <div v-if="result" class="result-grid">
          <article v-for="item in result.detections" :key="`${item.classCode}-${item.x}-${item.y}`" class="result-item">
            <strong>{{ item.label }}</strong>
            <span>{{ item.classCode }} · {{ Math.round(item.confidence * 100) }}%</span>
            <span>{{ item.ingredientCategory || '--' }} · {{ item.estimatedCount ?? '--' }}</span>
          </article>
        </div>

        <pre v-if="rawResponse" class="raw-json">{{ rawResponse }}</pre>
      </article>
    </section>
  </MainShell>
</template>

<style scoped>
.opencv-page { display: grid; gap: 16px; }
.hero-card, .control-card, .result-card { padding: var(--panel-padding); }
.hero-card { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
.hero-card h2 { margin: 0; color: var(--color-ink-strong); font-size: 1.7rem; }
.hero-card p { margin: 0; color: var(--color-ink-soft); line-height: 1.7; }
.hero-chip { display: inline-flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: 999px; color: var(--color-ink-strong); background: rgba(255,255,255,.05); }
.control-card { display: grid; gap: 14px; }
.upload-zone { display: grid; place-items: center; gap: 10px; padding: 24px; border: 1px dashed rgba(255,255,255,.14); border-radius: 20px; background: rgba(255,255,255,.03); text-align: center; }
.upload-zone--active { border-color: rgba(143,231,255,.36); background: rgba(77,179,255,.08); }
.upload-zone__input { width: 100%; color: var(--color-ink-soft); }
.form-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 12px; align-items: end; }
.field { display: grid; gap: 8px; }
.field__label { color: var(--color-ink-muted); font-size: .78rem; letter-spacing: .08em; text-transform: uppercase; }
.input-shell { border: 1px solid rgba(255,255,255,.08); border-radius: 14px; background: rgba(10,18,32,.32); overflow: hidden; }
.app-input { width: 100%; padding: 12px 14px; border: 0; outline: none; color: var(--color-ink-strong); background: transparent; }
.meta-line, .muted { color: var(--color-ink-soft); }
.notice { margin: 0; padding: 12px 14px; border-radius: 14px; }
.notice--success { color: #9fe6be; background: rgba(56,161,105,.14); }
.notice--error { color: #ffb4b4; background: rgba(185,70,70,.16); }
.notice--info { color: #9fd4ff; background: rgba(46,110,180,.16); }
.result-card { display: grid; gap: 14px; }
.section-head { display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap; align-items: center; }
.image-frame { position: relative; width: fit-content; max-width: 100%; overflow: auto; border-radius: 20px; background: rgba(255,255,255,.03); }
.image-frame__img { display: block; max-width: 100%; height: auto; }
.overlay { position: absolute; inset: 0; pointer-events: none; }
.box { position: absolute; border: 2px solid #8ee4ff; border-radius: 10px; box-shadow: 0 0 0 1px rgba(0,0,0,.2) inset; }
.box span { position: absolute; left: 0; top: -24px; padding: 4px 8px; border-radius: 999px; color: #06141e; font-size: .72rem; font-weight: 700; background: #8ee4ff; white-space: nowrap; }
.result-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.result-item { display: grid; gap: 6px; padding: 12px 14px; border: 1px solid rgba(255,255,255,.08); border-radius: 16px; background: rgba(255,255,255,.03); }
.raw-json { margin: 0; padding: 14px; overflow: auto; border-radius: 16px; color: var(--color-ink-soft); background: rgba(4,10,18,.5); }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 720px) {
  .form-row { grid-template-columns: 1fr; }
}
</style>
