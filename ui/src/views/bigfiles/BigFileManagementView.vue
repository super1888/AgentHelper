<template>
  <MainShell>
    <section class="management-page bigfile-page">
      <section class="hero panel-card">
      <div>
        <p class="section-kicker">Big File Pipeline</p>
        <h1>大文件分片上传</h1>
        <p class="hero__desc">按“选择文件 → 校验 → 生成唯一标识 → 分片上传 → 合并校验”的流程管理文件，完成后向量模块可读取合并文件路径。</p>
      </div>
      <div class="hero__stats">
        <div><strong>{{ stats.totalFiles }}</strong><span>文件总数</span></div>
        <div><strong>{{ stats.completedFiles }}</strong><span>已完成</span></div>
        <div><strong>{{ formatBytes(stats.totalFileSize) }}</strong><span>完成容量</span></div>
      </div>
    </section>

      <section v-if="feedback.message" class="feedback" :class="`feedback--${feedback.tone}`">
      {{ feedback.message }}
    </section>

      <section class="upload-grid">
      <div class="panel-card upload-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker">Upload Console</p>
            <h2>上传控制台</h2>
          </div>
          <button class="app-button app-button--secondary" type="button" @click="refreshAll">刷新列表</button>
        </div>

        <label class="drop-zone">
          <input ref="fileInput" type="file" @change="handleFileChange" />
          <span class="drop-zone__icon">⇧</span>
          <strong>{{ selectedFile?.name || '选择一个大文件' }}</strong>
          <em>支持断点续传、缺片查询、合并后路径回传</em>
        </label>

        <div class="file-inspector" v-if="selectedFile">
          <div><span>文件大小</span><strong>{{ formatBytes(selectedFile.size) }}</strong></div>
          <div><span>分片大小</span><strong>{{ formatBytes(chunkSize) }}</strong></div>
          <div><span>分片数量</span><strong>{{ totalChunks }}</strong></div>
          <div><span>业务模块</span><strong>{{ businessModule }}</strong></div>
        </div>

        <div class="form-row">
          <label>
            <span>分片大小 MB</span>
            <input v-model.number="chunkSizeMb" min="1" max="100" type="number" />
          </label>
          <label>
            <span>业务模块</span>
            <input v-model="businessModule" placeholder="vectorStore" />
          </label>
        </div>

        <button class="app-button upload-action" type="button" :disabled="uploading || !selectedFile" @click="startUpload">
          {{ uploading ? `上传中 ${progressPercent}%` : '开始分片上传' }}
        </button>

        <div class="progress-shell">
          <div class="progress-bar"><span :style="{ width: `${progressPercent}%` }"></span></div>
          <div class="progress-meta">
            <span>{{ uploadedChunks }}/{{ totalChunks }} 个分片</span>
            <span>{{ currentStage }}</span>
          </div>
        </div>
      </div>

      <div class="panel-card flow-card">
        <p class="section-kicker">Process</p>
        <h2>上传流程</h2>
        <ol class="flow-list">
          <li v-for="step in flowSteps" :key="step.title" :class="{ active: step.active }">
            <strong>{{ step.title }}</strong>
            <span>{{ step.desc }}</span>
          </li>
        </ol>
      </div>
    </section>

      <section class="panel-card records-card">
      <div class="panel-heading">
        <div>
          <p class="section-kicker">File Registry</p>
          <h2>文件管理</h2>
        </div>
        <div class="filters">
          <input v-model="filters.keyword" placeholder="搜索文件名 / 标识" @keyup.enter="loadFiles" />
          <select v-model="filters.status" @change="loadFiles">
            <option value="">全部状态</option>
            <option value="UPLOADING">上传中</option>
            <option value="COMPLETED">已完成</option>
            <option value="FAILED">失败</option>
          </select>
          <button class="app-button app-button--secondary" type="button" @click="loadFiles">查询</button>
        </div>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>文件</th>
              <th>大小</th>
              <th>分片</th>
              <th>状态</th>
              <th>业务模块</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in files" :key="item.fileId">
              <td>
                <strong>{{ item.fileName }}</strong>
                <small>{{ item.fileId }}</small>
              </td>
              <td>{{ formatBytes(item.fileSize) }}</td>
              <td>{{ item.uploadedCount }}/{{ item.totalChunks }}</td>
              <td><span class="status-pill" :class="`status-pill--${item.status.toLowerCase()}`">{{ statusLabel(item.status) }}</span></td>
              <td>{{ item.businessModule }}</td>
              <td>{{ formatTime(item.updatedAt) }}</td>
              <td>
                <button class="app-button app-button--ghost table-action" type="button" @click="copyPath(item)">复制路径</button>
                <button class="app-button table-action" type="button" :disabled="item.status !== 'COMPLETED' || importingFileId === item.fileId" @click="importToVector(item)">{{ importingFileId === item.fileId ? '入库中' : '导入向量' }}</button>
                <button class="app-button app-button--danger table-action" type="button" @click="removeFile(item.fileId)">删除</button>
              </td>
            </tr>
            <tr v-if="!files.length && !loading">
              <td colspan="7" class="empty-cell">暂无文件记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
    </section>
  </MainShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deleteBigFile,
  fetchBigFileStatistics,
  fetchBigFiles,
  fetchMissingBigFileChunks,
  initBigFileUpload,
  mergeBigFile,
  uploadBigFileChunk,
} from '@/api/bigfile'
import type { BigFileRecord, BigFileStatistics } from '@/types/bigfile'
import MainShell from '@/components/MainShell.vue'
import { importBigFileToVectorStore } from '@/api/vector'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const chunkSizeMb = ref(5)
const businessModule = ref('vectorStore')
const uploading = ref(false)
const importingFileId = ref('')
const loading = ref(false)
const currentStage = ref('等待选择文件')
const uploadedChunks = ref(0)
const files = ref<BigFileRecord[]>([])
const stats = ref<BigFileStatistics>({
  totalFiles: 0,
  completedFiles: 0,
  uploadingFiles: 0,
  failedFiles: 0,
  totalFileSize: 0,
  maxFileSize: 0,
  defaultChunkSize: DEFAULT_CHUNK_SIZE,
})
const filters = reactive({ keyword: '', status: '' })
const feedback = ref<{ tone: FeedbackTone; message: string }>({ tone: 'info', message: '' })

const chunkSize = computed(() => Math.max(1, chunkSizeMb.value || 5) * 1024 * 1024)
const totalChunks = computed(() => selectedFile.value ? Math.ceil(selectedFile.value.size / chunkSize.value) : 0)
const progressPercent = computed(() => totalChunks.value ? Math.round((uploadedChunks.value / totalChunks.value) * 100) : 0)
const flowSteps = computed(() => [
  { title: '选择文件', desc: '读取文件名、大小、类型', active: Boolean(selectedFile.value) },
  { title: '检查文件', desc: '校验大小与分片配置', active: currentStage.value !== '等待选择文件' },
  { title: '生成标识', desc: '创建文件唯一上传任务', active: ['生成唯一标识', '上传文件分片', '合并文件分片', '上传完成'].includes(currentStage.value) },
  { title: '上传分片', desc: '只上传缺失分片', active: ['上传文件分片', '合并文件分片', '上传完成'].includes(currentStage.value) },
  { title: '合并校验', desc: '合并为完整文件', active: ['合并文件分片', '上传完成'].includes(currentStage.value) },
])

function handleFileChange(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
  uploadedChunks.value = 0
  currentStage.value = selectedFile.value ? '已选择文件' : '等待选择文件'
}

async function startUpload() {
  if (!selectedFile.value) {
    showFeedback('error', '请先选择文件')
    return
  }
  uploading.value = true
  uploadedChunks.value = 0
  try {
    const file = selectedFile.value
    currentStage.value = '生成唯一标识'
    const initResult = await initBigFileUpload({
      fileName: file.name,
      fileSize: file.size,
      chunkSize: chunkSize.value,
      totalChunks: totalChunks.value,
      fileMd5: `${file.name}-${file.size}-${file.lastModified}`,
      contentType: file.type || 'application/octet-stream',
      businessModule: businessModule.value || 'vectorStore',
    })
    if (initResult.status === 'COMPLETED') {
      uploadedChunks.value = initResult.totalChunks
      currentStage.value = '上传完成'
      showFeedback('success', '文件已存在，可直接复用')
      await refreshAll()
      return
    }

    const missing = await fetchMissingBigFileChunks(initResult.fileId)
    const missingSet = new Set(missing.missingChunks)
    uploadedChunks.value = missing.uploadedCount
    currentStage.value = '上传文件分片'

    for (let index = 0; index < initResult.totalChunks; index += 1) {
      if (!missingSet.has(index)) {
        continue
      }
      const start = index * chunkSize.value
      const chunk = file.slice(start, Math.min(file.size, start + chunkSize.value))
      const result = await uploadBigFileChunk(initResult.fileId, index, chunk)
      uploadedChunks.value = result.uploadedCount
    }

    currentStage.value = '合并文件分片'
    await mergeBigFile(initResult.fileId)
    currentStage.value = '上传完成'
    showFeedback('success', '文件上传并合并完成，向量模块可使用存储路径')
    clearSelectedFile()
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '大文件上传失败'))
  } finally {
    uploading.value = false
  }
}

function clearSelectedFile() {
  selectedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

async function loadFiles() {
  loading.value = true
  try {
    const result = await fetchBigFiles({ keyword: filters.keyword || undefined, status: filters.status || undefined })
    files.value = result.items
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '文件列表加载失败'))
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await fetchBigFileStatistics()
    if (stats.value.defaultChunkSize) {
      chunkSizeMb.value = Math.max(1, Math.round(stats.value.defaultChunkSize / 1024 / 1024))
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '统计信息加载失败'))
  }
}

async function refreshAll() {
  await Promise.all([loadFiles(), loadStats()])
}

async function removeFile(fileId: string) {
  if (!window.confirm('确认删除此文件记录和本地分片吗？')) {
    return
  }
  try {
    await deleteBigFile(fileId)
    showFeedback('success', '文件记录已删除')
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '文件删除失败'))
  }
}

async function importToVector(item: BigFileRecord) {
  if (item.status !== 'COMPLETED') {
    showFeedback('error', '文件合并完成后才能导入向量库')
    return
  }
  importingFileId.value = item.fileId
  try {
    const result = await importBigFileToVectorStore(item.fileId)
    showFeedback('success', `已导入向量库：${result.fileName}，生成 ${result.chunkCount} 个切片`)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '导入向量库失败'))
  } finally {
    importingFileId.value = ''
  }
}
async function copyPath(item: BigFileRecord) {
  const value = item.storagePath || item.fileId
  await navigator.clipboard?.writeText(value)
  showFeedback('success', item.storagePath ? '存储路径已复制' : '文件标识已复制')
}

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function formatBytes(value: number | null | undefined) {
  if (!value || value <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = value
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex += 1
  }
  return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

function formatTime(value: string | null) {
  if (!value) return '未记录'
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function statusLabel(status: string) {
  return { UPLOADING: '上传中', COMPLETED: '已完成', FAILED: '失败' }[status] ?? status
}

onMounted(() => {
  void refreshAll()
})
</script>

<style scoped>
.bigfile-page {
  display: grid;
  gap: 20px;
  min-width: 0;
  overflow: hidden;
}

.bigfile-page * {
  box-sizing: border-box;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 360px);
  align-items: start;
  gap: 24px;
  overflow: hidden;
  position: relative;
}

.hero::after {
  content: '';
  position: absolute;
  right: -80px;
  top: -100px;
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(56, 189, 248, .32), transparent 65%);
  pointer-events: none;
}

.hero h1,
.panel-heading h2,
.flow-card h2 {
  margin: 0;
  color: #0f172a;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.hero__desc {
  max-width: 680px;
  color: #64748b;
  line-height: 1.75;
  overflow-wrap: anywhere;
}

.hero__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
  z-index: 1;
}

.hero__stats div {
  min-width: 0;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(145deg, #eff6ff, #f8fafc);
  border: 1px solid #dbeafe;
}

.hero__stats strong {
  display: block;
  color: #0369a1;
  font-size: clamp(18px, 2vw, 24px);
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.hero__stats span {
  color: #64748b;
  font-size: 12px;
}

.upload-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(260px, .75fr);
  gap: 20px;
  min-width: 0;
}

.upload-card,
.flow-card,
.records-card {
  min-width: 0;
}

.panel-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
  min-width: 0;
  margin-bottom: 18px;
}

.panel-heading > div {
  min-width: 0;
}

.drop-zone {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 190px;
  min-width: 0;
  padding: 20px;
  border: 1px dashed #38bdf8;
  border-radius: 28px;
  background: linear-gradient(160deg, rgba(240, 249, 255, .95), rgba(255, 255, 255, .82));
  cursor: pointer;
  text-align: center;
}

.drop-zone input {
  display: none;
}

.drop-zone__icon {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: #0f172a;
  color: #67e8f9;
  font-size: 30px;
}

.drop-zone strong {
  max-width: 100%;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drop-zone em {
  max-width: 100%;
  color: #64748b;
  font-style: normal;
  overflow-wrap: anywhere;
}

.file-inspector {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  min-width: 0;
  margin: 16px 0;
}

.file-inspector div {
  min-width: 0;
  padding: 12px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.file-inspector span {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}

.file-inspector strong {
  display: block;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-row {
  display: grid;
  grid-template-columns: minmax(140px, 180px) minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
  margin-top: 14px;
}

.form-row label {
  display: grid;
  gap: 8px;
  min-width: 0;
  color: #64748b;
  font-size: 13px;
}

.form-row input,
.filters input,
.filters select {
  width: 100%;
  min-width: 0;
  border: 1px solid #dbe3ef;
  border-radius: 14px;
  padding: 11px 12px;
  outline: none;
  background: #fff;
}

.upload-action {
  width: 100%;
  margin-top: 16px;
  justify-content: center;
}

.progress-shell {
  margin-top: 16px;
  min-width: 0;
}

.progress-bar {
  height: 12px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.progress-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #06b6d4, #2563eb);
  transition: width .2s ease;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.flow-list {
  display: grid;
  gap: 12px;
  padding: 0;
  list-style: none;
}

.flow-list li {
  min-width: 0;
  padding: 14px 16px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.flow-list li.active {
  background: #ecfeff;
  border-color: #67e8f9;
  box-shadow: 0 12px 30px rgba(8, 145, 178, .12);
}

.flow-list strong {
  display: block;
  color: #0f172a;
}

.flow-list span {
  color: #64748b;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.feedback {
  min-width: 0;
  padding: 12px 16px;
  border-radius: 16px;
  border: 1px solid transparent;
  overflow-wrap: anywhere;
}

.feedback--success { background: #ecfdf5; border-color: #bbf7d0; color: #047857; }
.feedback--error { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }
.feedback--info { background: #eff6ff; border-color: #bfdbfe; color: #1d4ed8; }

.filters {
  display: flex;
  flex: 1 1 420px;
  justify-content: flex-end;
  gap: 10px;
  min-width: 0;
  flex-wrap: wrap;
}

.filters input {
  flex: 1 1 220px;
}

.filters select {
  flex: 0 1 130px;
}

.table-wrap {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  scrollbar-gutter: stable;
}

table {
  width: 100%;
  min-width: 760px;
  table-layout: fixed;
  border-collapse: collapse;
}

th {
  color: #64748b;
  font-size: 12px;
  text-align: left;
  padding: 12px 10px;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
}

th:nth-child(1) { width: 32%; }
th:nth-child(2) { width: 10%; }
th:nth-child(3) { width: 10%; }
th:nth-child(4) { width: 12%; }
th:nth-child(5) { width: 12%; }
th:nth-child(6) { width: 12%; }
th:nth-child(7) { width: 12%; }

td {
  min-width: 0;
  padding: 14px 10px;
  border-bottom: 1px solid #edf2f7;
  color: #334155;
  vertical-align: middle;
  overflow-wrap: anywhere;
}

td strong,
td small {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

td small {
  color: #94a3b8;
  margin-top: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.status-pill {
  display: inline-flex;
  max-width: 100%;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  background: #f1f5f9;
  color: #475569;
}

.status-pill--completed { background: #dcfce7; color: #15803d; }
.status-pill--uploading { background: #e0f2fe; color: #0369a1; }
.status-pill--failed { background: #fee2e2; color: #b91c1c; }

.table-action {
  min-height: 32px;
  padding: 7px 10px;
  margin: 2px 6px 2px 0;
  font-size: 12px;
  white-space: nowrap;
}

.empty-cell {
  text-align: center;
  color: #94a3b8;
  padding: 32px;
}

@media (max-width: 1180px) {
  .hero,
  .upload-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .bigfile-page {
    gap: 14px;
  }

  .hero,
  .upload-card,
  .flow-card,
  .records-card {
    padding: 18px;
  }

  .hero__stats,
  .file-inspector,
  .form-row {
    grid-template-columns: 1fr;
  }

  .panel-heading,
  .progress-meta {
    align-items: stretch;
    flex-direction: column;
  }

  .filters,
  .filters input,
  .filters select,
  .filters .app-button {
    width: 100%;
    flex-basis: auto;
  }

  table {
    min-width: 680px;
  }
}
</style>
