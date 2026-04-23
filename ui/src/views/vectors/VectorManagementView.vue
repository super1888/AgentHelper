<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  FileSearch,
  FolderOpen,
  RefreshCw,
  Search,
  Trash2,
  Upload,
} from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  deleteAllVectorFiles,
  deleteVectorFile,
  fetchVectorDocuments,
  fetchVectorFiles,
  fetchVectorStatistics,
  searchVectorDocuments,
  uploadVectorFile,
} from '@/api/vector'
import type {
  VectorStoreDocumentItem,
  VectorStoreFileItem,
  VectorStoreSearchResult,
  VectorStoreStatistics,
} from '@/types/vector'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

const loading = ref(false)
const statsLoading = ref(false)
const documentsLoading = ref(false)
const searchLoading = ref(false)
const uploadPending = ref(false)
const deletePending = ref(false)
const selectedFile = ref<File | null>(null)
const selectedFileName = ref('')
const uploadFileInput = ref<HTMLInputElement | null>(null)
const feedback = ref<FeedbackState>({ tone: 'info', message: '' })
let feedbackTimer: ReturnType<typeof setTimeout> | null = null
const files = ref<VectorStoreFileItem[]>([])
const documents = ref<VectorStoreDocumentItem[]>([])
const searchResult = ref<VectorStoreSearchResult | null>(null)
const statistics = ref<VectorStoreStatistics>({
  totalFiles: 0,
  activeFiles: 0,
  deletedFiles: 0,
  totalChunks: 0,
  totalFileSize: 0,
})

const searchForm = reactive({
  query: '',
  fileName: '',
  topK: 5,
  similarityThreshold: 0.4,
})

const activeFile = computed(
  () => files.value.find((item) => item.fileName === selectedFileName.value) ?? null,
)

const activeFileSizeLabel = computed(() => formatBytes(activeFile.value?.fileSize ?? 0))

watch(selectedFileName, (fileName) => {
  documents.value = []
  if (fileName) {
    void loadDocuments(fileName)
  }
})

function showFeedback(tone: FeedbackTone, message: string) {
  if (feedbackTimer) {
    clearTimeout(feedbackTimer)
    feedbackTimer = null
  }
  feedback.value = { tone, message }
}

function clearFeedback() {
  if (feedbackTimer) {
    clearTimeout(feedbackTimer)
    feedbackTimer = null
  }
  feedback.value = { tone: 'info', message: '' }
}

function formatBytes(value: number | null) {
  if (!value || value <= 0) {
    return '0 B'
  }

  const units = ['B', 'KB', 'MB', 'GB']
  let size = value
  let unitIndex = 0

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex += 1
  }

  return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

function formatTime(value: string | null) {
  if (!value) {
    return '未记录'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function normalizeText(value: string) {
  const normalized = value.trim()
  return normalized ? normalized : undefined
}

async function loadFiles() {
  loading.value = true

  try {
    const result = await fetchVectorFiles()
    files.value = result.items

    if (!result.items.some((item) => item.fileName === selectedFileName.value)) {
      selectedFileName.value = result.items[0]?.fileName ?? ''
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '向量文件列表加载失败。'))
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  statsLoading.value = true

  try {
    statistics.value = await fetchVectorStatistics()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '向量统计加载失败。'))
  } finally {
    statsLoading.value = false
  }
}

async function loadDocuments(fileName: string) {
  documentsLoading.value = true

  try {
    const result = await fetchVectorDocuments(fileName)
    documents.value = result.items
  } catch (error) {
    documents.value = []
    showFeedback('error', getErrorMessage(error, '文件切片加载失败。'))
  } finally {
    documentsLoading.value = false
  }
}

async function refreshAll(successMessage?: string) {
  await Promise.all([loadFiles(), loadStatistics()])
  if (successMessage) {
    showFeedback('success', successMessage)
  }
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  selectedFile.value = target.files?.[0] ?? null
}

function clearSelectedUploadFile() {
  selectedFile.value = null
  if (uploadFileInput.value) {
    uploadFileInput.value.value = ''
  }
}

async function handleUpload() {
  if (!selectedFile.value) {
    showFeedback('error', '请先选择一个文件。')
    return
  }

  uploadPending.value = true

  try {
    const result = await uploadVectorFile(selectedFile.value)
    selectedFileName.value = result.fileName
    clearSelectedUploadFile()
    await refreshAll(`文件 ${result.fileName} 已上传并完成切片入库。`)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '向量文件上传失败。'))
  } finally {
    uploadPending.value = false
  }
}

async function handleDeleteFile(fileName: string) {
  deletePending.value = true

  try {
    const result = await deleteVectorFile(fileName)
    if (selectedFileName.value === fileName) {
      selectedFileName.value = ''
      documents.value = []
    }
    await refreshAll(result.message)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除文件向量数据失败。'))
  } finally {
    deletePending.value = false
  }
}

async function handleDeleteAll() {
  deletePending.value = true

  try {
    const result = await deleteAllVectorFiles()
    selectedFileName.value = ''
    documents.value = []
    await refreshAll(result.message)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '清空向量数据失败。'))
  } finally {
    deletePending.value = false
  }
}

async function handleSearch() {
  if (!searchForm.query.trim()) {
    showFeedback('error', '请输入检索词。')
    return
  }

  searchLoading.value = true

  try {
    searchResult.value = await searchVectorDocuments({
      query: searchForm.query.trim(),
      fileName: normalizeText(searchForm.fileName),
      topK: searchForm.topK,
      similarityThreshold: searchForm.similarityThreshold,
    })
  } catch (error) {
    searchResult.value = null
    showFeedback('error', getErrorMessage(error, '向量检索失败。'))
  } finally {
    searchLoading.value = false
  }
}

onMounted(() => {
  void refreshAll()
})

onBeforeUnmount(() => {
  if (feedbackTimer) {
    clearTimeout(feedbackTimer)
  }
})
</script>

<template>
  <MainShell>
    <AppFeedbackDialog
      :model-value="Boolean(feedback.message)"
      :tone="feedback.tone"
      :message="feedback.message"
      @update:model-value="!$event && clearFeedback()"
    />
    <Transition name="toast-fade">
      <section
        v-if="false && feedback"
        class="feedback-toast"
        :class="`feedback-toast--${feedback?.tone}`"
        aria-live="polite"
      >
        <div class="feedback-toast__body">
          <strong>{{ feedback.tone === 'success' ? '操作成功' : feedback.tone === 'error' ? '操作失败' : '提示' }}</strong>
          <span>{{ feedback?.message }}</span>
        </div>
        <button type="button" class="feedback-toast__close" @click="clearFeedback">
          关闭
        </button>
      </section>
    </Transition>

    <section class="management-page vector-workspace">
      <header class="vector-workspace__hero panel-card management-hero">
        <div class="vector-workspace__headline">
          <p class="section-kicker">Vector Studio</p>
          <h2>向量管理中心</h2>
          <p class="vector-workspace__subtitle">
            统一管理文件入库、切片明细、语义检索与批量清理，支撑知识库向量资产的日常运营。
          </p>
        </div>

        <div class="vector-workspace__actions">
          <button
            type="button"
            class="app-button app-button--secondary"
            :disabled="loading || statsLoading || documentsLoading"
            @click="refreshAll()"
          >
            <RefreshCw :size="16" aria-hidden="true" />
            刷新
          </button>
          <button
            type="button"
            class="app-button app-button--danger"
            :disabled="deletePending"
            @click="handleDeleteAll"
          >
            <Trash2 :size="16" aria-hidden="true" />
            {{ deletePending ? '处理中...' : '清空全部向量' }}
          </button>
        </div>
      </header>

      <div class="vector-stats">
        <article class="vector-stat panel-card">
          <span>文件总数</span>
          <strong>{{ statsLoading ? '...' : statistics.totalFiles }}</strong>
        </article>
        <article class="vector-stat panel-card">
          <span>可用文件</span>
          <strong>{{ statsLoading ? '...' : statistics.activeFiles }}</strong>
        </article>
        <article class="vector-stat panel-card">
          <span>切片总数</span>
          <strong>{{ statsLoading ? '...' : statistics.totalChunks }}</strong>
        </article>
        <article class="vector-stat panel-card">
          <span>存储体积</span>
          <strong>{{ statsLoading ? '...' : formatBytes(statistics.totalFileSize) }}</strong>
        </article>
      </div>

      <div class="vector-grid">
        <section class="vector-panel panel-card">
          <div class="section-header">
            <div>
              <strong>文件上传入库</strong>
              <p>支持将业务文档上传到向量库，并自动完成切片与索引构建。</p>
            </div>
          </div>

          <div class="upload-box">
            <label class="field">
              <span class="field__label">选择文件</span>
              <input
                id="vector-file-input"
                ref="uploadFileInput"
                class="upload-box__input"
                type="file"
                @change="handleFileChange"
              />
            </label>
            <p class="upload-box__tip">
              {{ selectedFile ? `已选择：${selectedFile.name}` : '尚未选择文件' }}
            </p>
            <div class="upload-box__actions">
              <button
                v-if="selectedFile"
                type="button"
                class="app-button app-button--ghost upload-box__clear"
                :disabled="uploadPending"
                @click="clearSelectedUploadFile"
              >
                <Trash2 :size="16" aria-hidden="true" />
                移除已选文件
              </button>
              <button
                type="button"
                class="app-button upload-box__button"
                :disabled="uploadPending"
                @click="handleUpload"
              >
                <Upload :size="16" aria-hidden="true" />
                {{ uploadPending ? '上传中...' : '上传并切片入库' }}
              </button>
            </div>
          </div>
        </section>

        <section class="vector-panel panel-card">
          <div class="section-header">
            <div>
              <strong>语义检索</strong>
              <p>支持按文件过滤检索范围，并返回命中的切片内容与相似度分值。</p>
            </div>
          </div>

          <div class="search-form">
            <label class="field">
              <span class="field__label">检索词</span>
              <div class="input-shell">
                <span class="input-shell__icon" aria-hidden="true">
                  <Search :size="16" />
                </span>
                <input
                  v-model="searchForm.query"
                  class="app-input"
                  type="text"
                  placeholder="请输入语义检索词"
                />
              </div>
            </label>

            <label class="field">
              <span class="field__label">限定文件</span>
              <div class="input-shell">
                <span class="input-shell__icon" aria-hidden="true">
                  <FileSearch :size="16" />
                </span>
                <input
                  v-model="searchForm.fileName"
                  class="app-input"
                  type="text"
                  placeholder="可选，按文件名过滤"
                />
              </div>
            </label>

            <div class="search-form__row">
              <label class="field">
                <span class="field__label">Top K</span>
                <div class="input-shell">
                  <input
                    v-model.number="searchForm.topK"
                    class="app-input"
                    type="number"
                    min="1"
                    max="20"
                  />
                </div>
              </label>

              <label class="field">
                <span class="field__label">相似度阈值</span>
                <div class="input-shell">
                  <input
                    v-model.number="searchForm.similarityThreshold"
                    class="app-input"
                    type="number"
                    min="0"
                    max="1"
                    step="0.05"
                  />
                </div>
              </label>
            </div>

            <button type="button" class="app-button" :disabled="searchLoading" @click="handleSearch">
              <Search :size="16" aria-hidden="true" />
              {{ searchLoading ? '检索中...' : '开始检索' }}
            </button>
          </div>

          <div v-if="searchResult" class="result-list">
            <div class="result-list__summary">命中 {{ searchResult.total }} 条结果</div>
            <article v-for="item in searchResult.items" :key="item.id" class="result-card">
              <div class="result-card__meta">
                <span>{{ String(item.metadata?.file_name ?? searchResult.fileName ?? '未标记文件') }}</span>
                <strong v-if="item.score !== undefined && item.score !== null">
                  {{ item.score.toFixed(4) }}
                </strong>
              </div>
              <p>{{ item.content }}</p>
            </article>
          </div>
        </section>

        <section class="vector-panel panel-card">
          <div class="section-header">
            <div>
              <strong>文件台账</strong>
              <p>查看当前向量文件状态、切片数量、存储体积和最近处理结果。</p>
            </div>
          </div>

          <div v-if="loading" class="empty-state">正在加载文件列表...</div>
          <div v-else-if="files.length === 0" class="empty-state">当前还没有向量文件。</div>
          <div v-else class="file-list">
            <article
              v-for="item in files"
              :key="item.id"
              class="file-card"
              :class="{ 'file-card--active': item.fileName === selectedFileName }"
              tabindex="0"
              @click="selectedFileName = item.fileName"
              @keydown.enter.prevent="selectedFileName = item.fileName"
              @keydown.space.prevent="selectedFileName = item.fileName"
            >
              <div class="file-card__head">
                <div>
                  <strong>{{ item.fileName }}</strong>
                  <p>{{ formatTime(item.uploadedAt) }}</p>
                </div>
                <span
                  class="file-card__status"
                  :class="`file-card__status--${String(item.storeStatus).toLowerCase()}`"
                >
                  {{ item.storeStatus || 'UNKNOWN' }}
                </span>
              </div>
              <div class="file-card__meta">
                <span>{{ item.chunkCount ?? 0 }} 个切片</span>
                <span>{{ formatBytes(item.fileSize) }}</span>
              </div>
              <p class="file-card__message">{{ item.lastOperationMessage || '暂无操作说明' }}</p>
              <button
                type="button"
                class="app-button app-button--ghost file-card__delete"
                :disabled="deletePending"
                @click.stop="handleDeleteFile(item.fileName)"
              >
                <Trash2 :size="14" aria-hidden="true" />
                删除向量
              </button>
            </article>
          </div>
        </section>

        <section class="vector-panel panel-card vector-panel--detail">
          <div class="section-header">
            <div>
              <strong>切片详情</strong>
              <p>查看文件切片文本和元数据，便于排查入库质量与检索命中效果。</p>
            </div>
          </div>

          <div v-if="!activeFile" class="empty-state">选择一个文件后查看切片详情。</div>
          <template v-else>
            <div class="detail-summary">
              <article class="detail-metric">
                <span>文件</span>
                <strong>{{ activeFile.fileName }}</strong>
              </article>
              <article class="detail-metric">
                <span>大小</span>
                <strong>{{ activeFileSizeLabel }}</strong>
              </article>
              <article class="detail-metric">
                <span>切片数</span>
                <strong>{{ activeFile.chunkCount ?? 0 }}</strong>
              </article>
            </div>

            <div v-if="documentsLoading" class="empty-state">正在加载切片详情...</div>
            <div v-else-if="documents.length === 0" class="empty-state">当前文件没有可展示的切片。</div>
            <div v-else class="document-list">
              <article v-for="item in documents" :key="item.id" class="document-card">
                <div class="document-card__head">
                  <div class="document-card__title">
                    <FolderOpen :size="15" aria-hidden="true" />
                    <strong>{{ item.id || '未记录切片 ID' }}</strong>
                  </div>
                  <span>{{ String(item.metadata?.uploaded_at ?? '-') }}</span>
                </div>
                <p>{{ item.content }}</p>
              </article>
            </div>
          </template>
        </section>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.feedback-toast {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 1200;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: min(420px, calc(100vw - 32px));
  padding: 16px 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  background: rgba(9, 16, 28, 0.94);
  box-shadow: 0 18px 48px rgba(3, 8, 18, 0.42);
  backdrop-filter: blur(18px);
}

.feedback-toast--success {
  border-color: rgba(86, 214, 164, 0.34);
}

.feedback-toast--error {
  border-color: rgba(255, 120, 120, 0.34);
}

.feedback-toast--info {
  border-color: rgba(105, 190, 255, 0.34);
}

.feedback-toast__body {
  display: grid;
  gap: 6px;
  flex: 1;
}

.feedback-toast__body strong {
  color: var(--color-ink-strong);
  font-size: 0.94rem;
}

.feedback-toast__body span {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.feedback-toast__close {
  border: 0;
  padding: 0;
  color: var(--color-ink-muted);
  background: transparent;
  cursor: pointer;
}

.feedback-toast__close:hover {
  color: var(--color-ink-strong);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate3d(0, -10px, 0);
}

.vector-workspace {
  display: grid;
  gap: 18px;
}

.vector-workspace__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.vector-workspace__headline {
  display: grid;
  gap: 10px;
  max-width: 42rem;
}

.vector-workspace__headline h2 {
  font-size: clamp(2rem, 2.4vw, 2.7rem);
}

.vector-workspace__subtitle {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.vector-workspace__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.vector-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.vector-stat {
  padding: 18px 20px;
  border-radius: 24px;
}

.vector-stat span {
  color: var(--color-ink-muted);
  font-size: 0.84rem;
  line-height: 1.45;
}

.vector-stat strong {
  display: block;
  margin-top: 8px;
  color: var(--color-ink-strong);
  font-size: 1.5rem;
  line-height: 1.2;
}

.vector-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.vector-panel {
  padding: 22px;
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.018)),
    rgba(6, 12, 24, 0.72);
  overflow: visible;
}

.vector-panel--detail {
  grid-column: 1 / -1;
}

.upload-box,
.search-form,
.file-list,
.document-list,
.result-list {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.section-header p,
.upload-box__tip,
.result-list__summary,
.empty-state {
  color: var(--color-ink-soft);
}

.upload-box__input {
  width: 100%;
  color: var(--color-ink-soft);
}

.upload-box__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.upload-box__button {
  flex: 1 1 240px;
}

.upload-box__clear {
  flex: 0 0 auto;
}

.search-form__row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.file-card,
.document-card,
.result-card,
.detail-metric {
  width: 100%;
  padding: var(--compact-panel-padding);
  text-align: left;
  border-radius: var(--sub-panel-radius);
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.035);
}

.file-card {
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, background-color 180ms ease;
}

.file-card:hover {
  transform: translateY(-1px);
  border-color: rgba(83, 184, 255, 0.22);
}

.file-card--active {
  border-color: rgba(119, 224, 255, 0.42);
  background: rgba(83, 184, 255, 0.08);
}

.file-card__head,
.document-card__head,
.result-card__meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  min-width: 0;
}

.file-card__head p,
.file-card__message,
.document-card p,
.result-card p {
  margin-top: 8px;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.file-card__meta,
.document-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.section-header,
.upload-box__actions,
.file-card,
.document-card,
.result-card {
  min-width: 0;
}

.section-header {
  min-height: 60px;
}

.section-header > div:first-child,
.file-card__head > div:first-child,
.document-card__head > div:first-child,
.result-card__meta > div:first-child {
  min-width: 0;
  flex: 1 1 320px;
}

.file-card__meta {
  margin-top: 12px;
  color: var(--color-ink-muted);
  font-size: 0.85rem;
}

.file-card__status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 700;
}

.file-card__status--active {
  color: #ddfff6;
  background: rgba(100, 216, 190, 0.16);
}

.file-card__status--deleted {
  color: #ffe2e2;
  background: rgba(255, 144, 151, 0.14);
}

.file-card__delete {
  width: 100%;
  margin-top: 14px;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
  margin-bottom: 18px;
}

.detail-metric {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-metric span {
  color: var(--color-ink-muted);
  font-size: 0.82rem;
}

.detail-metric strong {
  color: var(--color-ink-strong);
  line-height: 1.35;
}

@media (max-width: 1100px) {
  .vector-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .vector-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .feedback-toast {
    top: 16px;
    right: 16px;
    left: 16px;
    width: auto;
  }

  .vector-workspace__hero {
    flex-direction: column;
  }

  .search-form__row,
  .detail-summary,
  .vector-stats {
    grid-template-columns: 1fr;
  }

  .upload-box__actions {
    flex-direction: column;
  }

  .upload-box__button,
  .upload-box__clear {
    width: 100%;
  }
}
</style>
