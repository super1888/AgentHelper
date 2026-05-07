<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { Camera, LoaderCircle, ScanFace, Upload } from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'

type FaceMode = 'login' | 'bind'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    mode: FaceMode
    title: string
    description?: string
    submitting?: boolean
    forceReplace?: boolean
    errorMessage?: string
  }>(),
  {
    description: '',
    submitting: false,
    forceReplace: false,
    errorMessage: '',
  },
)

const emit = defineEmits<{
  'update:modelValue': [boolean]
  submit: [payload: {
    imageBase64: string
    imageFormat: string
    deviceId?: string | null
    clientIp?: string | null
    forceReplace?: boolean | null
    silentLogin?: boolean | null
  }]
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const stream = ref<MediaStream | null>(null)
const previewUrl = ref('')
const previewMeta = reactive({
  width: 0,
  height: 0,
  source: '',
})
const cameraReady = ref(false)
const cameraBusy = ref(false)
const cameraError = ref('')
const deviceId = ref('')
const clientIp = ref('')
const activeErrorMessage = computed(() => props.errorMessage || cameraError.value)

const modeLabel = computed(() => (props.mode === 'bind' ? '绑定人脸' : '人脸登录'))
const submitLabel = computed(() => (props.mode === 'bind' ? '确认绑定' : '立即登录'))

function revokePreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

async function stopCamera() {
  const currentStream = stream.value
  stream.value = null
  cameraReady.value = false
  if (currentStream) {
    currentStream.getTracks().forEach((track) => track.stop())
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
}

async function startCamera() {
  cameraError.value = ''
  cameraBusy.value = true
  try {
    await stopCamera()
    const mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'user' },
      audio: false,
    })
    stream.value = mediaStream
    if (videoRef.value) {
      videoRef.value.srcObject = mediaStream
      await videoRef.value.play()
      cameraReady.value = true
    }
  } catch (error) {
    cameraError.value = error instanceof Error ? error.message : '摄像头无法打开'
  } finally {
    cameraBusy.value = false
  }
}

function captureFrame() {
  const video = videoRef.value
  const canvas = canvasRef.value
  if (!video || !canvas || !cameraReady.value) {
    cameraError.value = '请先打开摄像头'
    return
  }

  const width = video.videoWidth || 1280
  const height = video.videoHeight || 720
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) {
    cameraError.value = '无法创建画布'
    return
  }

  context.drawImage(video, 0, 0, width, height)
  const dataUrl = canvas.toDataURL('image/jpeg', 0.92)
  setPreview(dataUrl, 'camera')
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] ?? null
  if (!file) return
  const dataUrl = await fileToDataUrl(file)
  setPreview(dataUrl, file.type.split('/').pop() || 'jpg')
}

function fileToDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsDataURL(file)
  })
}

function setPreview(dataUrl: string, source: string) {
  revokePreview()
  previewUrl.value = dataUrl
  const image = new Image()
  image.onload = () => {
    previewMeta.width = image.naturalWidth || 0
    previewMeta.height = image.naturalHeight || 0
    previewMeta.source = source
  }
  image.src = dataUrl
}

function resetPreview() {
  revokePreview()
  previewMeta.width = 0
  previewMeta.height = 0
  previewMeta.source = ''
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function extractPayload(dataUrl: string) {
  const [, payload = ''] = dataUrl.split(',')
  const mime = dataUrl.match(/^data:(.*?);base64,/)?.[1] || 'image/jpeg'
  return {
    imageBase64: payload,
    imageFormat: mime.split('/').pop() || 'jpg',
  }
}

function submit() {
  if (!previewUrl.value) {
    cameraError.value = '请先拍照或上传图片'
    return
  }
  const payload = extractPayload(previewUrl.value)
  emit('submit', {
    ...payload,
    deviceId: deviceId.value.trim() || null,
    clientIp: clientIp.value.trim() || null,
    forceReplace: props.mode === 'bind' ? props.forceReplace : null,
    silentLogin: props.mode === 'login' ? true : null,
  })
}

function closeDialog() {
  emit('update:modelValue', false)
}

watch(
  () => props.modelValue,
  async (visible) => {
    cameraError.value = ''
    if (visible) {
      resetPreview()
      await startCamera()
      return
    }
    await stopCamera()
    resetPreview()
  },
)

onBeforeUnmount(() => {
  void stopCamera()
  revokePreview()
})
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    :title="title"
    :description="description || modeLabel"
    width="wide"
    @update:model-value="closeDialog"
  >
    <div class="face-dialog">
      <section class="face-dialog__stage">
        <div class="face-dialog__camera" :class="{ 'face-dialog__camera--ready': cameraReady }">
          <video ref="videoRef" class="face-dialog__video" autoplay playsinline muted></video>
          <div class="face-dialog__reticle" aria-hidden="true"></div>
        </div>

        <div class="face-dialog__actions">
          <button type="button" class="app-button app-button--secondary" :disabled="cameraBusy" @click="startCamera">
            <LoaderCircle v-if="cameraBusy" :size="16" class="spin" />
            <Camera v-else :size="16" />
            重新开启
          </button>
          <button type="button" class="app-button app-button--secondary" @click="captureFrame">
            <ScanFace :size="16" />
            拍摄人脸
          </button>
          <label class="app-button app-button--secondary face-dialog__upload">
            <Upload :size="16" />
            上传图片
            <input ref="fileInputRef" type="file" accept="image/*" class="face-dialog__file" @change="onFileChange" />
          </label>
        </div>

        <div class="face-dialog__inputs">
          <label class="field">
            <span class="field__label">设备标识</span>
            <div class="input-shell">
              <input v-model="deviceId" class="app-input" type="text" placeholder="可选" />
            </div>
          </label>
          <label class="field">
            <span class="field__label">客户端 IP</span>
            <div class="input-shell">
              <input v-model="clientIp" class="app-input" type="text" placeholder="可选" />
            </div>
          </label>
        </div>

        <p v-if="activeErrorMessage" class="face-dialog__error" role="alert">{{ activeErrorMessage }}</p>
      </section>

      <aside class="face-dialog__preview">
        <div class="face-dialog__preview-box">
          <img v-if="previewUrl" :src="previewUrl" alt="人脸预览" class="face-dialog__preview-image" />
          <div v-else class="face-dialog__preview-empty">
            <strong>等待采集</strong>
            <span>拍照或上传图片后会在这里预览</span>
          </div>
        </div>

        <div class="face-dialog__meta">
          <span>来源 {{ previewMeta.source || '--' }}</span>
          <span>分辨率 {{ previewMeta.width || '--' }} x {{ previewMeta.height || '--' }}</span>
        </div>

        <button type="button" class="app-button face-dialog__submit" :disabled="submitting" @click="submit">
          <LoaderCircle v-if="submitting" :size="16" class="spin" />
          <span v-else>{{ submitLabel }}</span>
        </button>
      </aside>
    </div>

    <canvas ref="canvasRef" class="face-dialog__canvas" aria-hidden="true"></canvas>
  </AppDialog>
</template>

<style scoped>
.face-dialog {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.9fr);
  gap: 20px;
}

.face-dialog__stage,
.face-dialog__preview {
  display: grid;
  gap: 14px;
}

.face-dialog__camera {
  position: relative;
  min-height: 340px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  overflow: hidden;
  background:
    radial-gradient(circle at top, rgba(77, 179, 255, 0.16), transparent 45%),
    rgba(255, 255, 255, 0.04);
}

.face-dialog__video {
  width: 100%;
  height: 100%;
  min-height: 340px;
  object-fit: cover;
  transform: scaleX(-1);
}

.face-dialog__reticle {
  position: absolute;
  inset: 14%;
  border: 1px solid rgba(154, 234, 255, 0.5);
  border-radius: 32px;
  box-shadow:
    0 0 0 1px rgba(77, 179, 255, 0.16) inset,
    0 0 0 999px rgba(4, 9, 18, 0.16);
}

.face-dialog__camera--ready .face-dialog__reticle {
  border-color: rgba(154, 234, 255, 0.78);
}

.face-dialog__actions,
.face-dialog__meta,
.face-dialog__inputs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.face-dialog__upload {
  position: relative;
  overflow: hidden;
}

.face-dialog__file,
.face-dialog__canvas {
  display: none;
}

.face-dialog__preview-box {
  min-height: 260px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.03);
}

.face-dialog__preview-empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  min-height: 260px;
  padding: 24px;
  color: var(--color-ink-soft);
  text-align: center;
}

.face-dialog__preview-empty strong {
  color: var(--color-ink-strong);
}

.face-dialog__preview-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.face-dialog__meta {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.face-dialog__submit {
  justify-content: center;
}

.face-dialog__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 16px;
  color: #ffd2d6;
  background: rgba(255, 144, 151, 0.12);
}

@media (max-width: 960px) {
  .face-dialog {
    grid-template-columns: 1fr;
  }

  .face-dialog__actions,
  .face-dialog__meta,
  .face-dialog__inputs {
    grid-template-columns: 1fr;
  }
}
</style>
