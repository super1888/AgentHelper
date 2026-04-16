<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Copy,
  Eye,
  FileCode2,
  FileText,
  FolderOpen,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
  UserRoundPen,
} from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MainShell from '@/components/MainShell.vue'
import PromptTemplateFormDialog from '@/components/PromptTemplateFormDialog.vue'
import {
  createPromptTemplate,
  fetchPromptTemplateStats,
  queryPromptTemplates,
  removePromptTemplate,
  updatePromptTemplate,
} from '@/api/prompt'
import type { PromptTemplateItem, PromptTemplatePayload, PromptTemplateStatistics } from '@/types/prompt'
import { getErrorMessage } from '@/utils/errors'

type DialogMode = 'create' | 'edit'
type FeedbackTone = 'success' | 'error' | 'info'
type TemplateStatusFilter = 'ALL' | 'ENABLED' | 'DISABLED'
type TemplateSourceFilter = 'ALL' | 'INLINE_TEXT' | 'FILE_PATH'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

const loading = ref(false)
const statsLoading = ref(false)
const submitting = ref(false)
const deletePending = ref(false)
const dialogOpen = ref(false)
const previewOpen = ref(false)
const previewVariablesText = ref('{\n  "userName": "张卓奇",\n  "ticketId": "TK-20260416-01",\n  "knowledge_summary": "已匹配到 3 条知识库答案"\n}')
const dialogMode = ref<DialogMode>('create')
const selectedTemplate = ref<PromptTemplateItem | null>(null)
const previewTemplate = ref<PromptTemplateItem | null>(null)
const deleteTarget = ref<PromptTemplateItem | null>(null)
const feedback = ref<FeedbackState | null>(null)
const templates = ref<PromptTemplateItem[]>([])
const statistics = ref<PromptTemplateStatistics>({
  totalCount: 0,
  enabledCount: 0,
  disabledCount: 0,
  inlineCount: 0,
  fileCount: 0,
})
const filters = reactive({
  keyword: '',
  templateStatus: 'ALL' as TemplateStatusFilter,
  sourceType: 'ALL' as TemplateSourceFilter,
})

const filteredTemplates = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()

  return templates.value.filter((item) => {
    const matchesKeyword = !keyword
      || [item.templateName, item.templateCode, item.description, item.ownerUserName]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    const matchesStatus = filters.templateStatus === 'ALL' || item.templateStatus === filters.templateStatus
    const matchesSource = filters.sourceType === 'ALL' || item.sourceType === filters.sourceType
    return matchesKeyword && matchesStatus && matchesSource
  })
})

const resultsSummary = computed(() => {
  if (loading.value) {
    return '正在加载模板列表...'
  }
  return `共 ${filteredTemplates.value.length} 个模板，启用 ${templates.value.filter((item) => item.templateStatus === 'ENABLED').length} 个`
})

const enabledRate = computed(() => {
  if (!statistics.value.totalCount) {
    return '0%'
  }
  return `${Math.round((statistics.value.enabledCount / statistics.value.totalCount) * 100)}%`
})

const deleteDescription = computed(() =>
  deleteTarget.value ? `确认删除模板「${deleteTarget.value.templateName}」吗？该操作不可撤销。` : '',
)
const previewVariableDefinitions = computed(() => {
  const definitions = previewTemplate.value?.variableDefinitions ?? []
  if (definitions.length > 0) {
    return definitions
  }
  return extractVariables(previewTemplate.value?.templateContent ?? '').map((item) => ({
    variableName: item,
    required: true,
    defaultValue: null,
    description: null,
  }))
})
const previewVariableMap = computed<Record<string, string>>(() => {
  try {
    const parsed = JSON.parse(previewVariablesText.value)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return {}
    }
    return Object.entries(parsed).reduce<Record<string, string>>((result, [key, value]) => {
      result[key] = value == null ? '' : String(value)
      return result
    }, {})
  } catch {
    return {}
  }
})
const renderedPreviewContent = computed(() => {
  const content = previewTemplate.value?.templateContent ?? ''
  return content.replace(/\{\{\s*([a-zA-Z][a-zA-Z0-9_]*)\s*}}/g, (_, key: string) => {
    return Object.prototype.hasOwnProperty.call(previewVariableMap.value, key)
      ? previewVariableMap.value[key]
      : `{{${key}}}`
  })
})
const previewJsonValid = computed(() => {
  try {
    JSON.parse(previewVariablesText.value)
    return true
  } catch {
    return false
  }
})

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function formatTime(value: number | null) {
  if (!value) {
    return '未记录'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value)
}

function formatStatusLabel(status: PromptTemplateItem['templateStatus']) {
  return status === 'ENABLED' ? '启用' : '禁用'
}

function formatSourceLabel(sourceType: PromptTemplateItem['sourceType']) {
  return sourceType === 'INLINE_TEXT' ? '内联文本' : '文件路径'
}

function formatContentPreview(item: PromptTemplateItem) {
  const raw = item.sourceType === 'INLINE_TEXT' ? item.templateContent || '' : item.sourcePath || ''
  return raw.length > 220 ? `${raw.slice(0, 220)}...` : raw
}

function extractVariables(content: string) {
  const matches = content.match(/\{\{\s*[a-zA-Z][a-zA-Z0-9_]*\s*}}/g) ?? []
  return [...new Set(matches.map((item) => item.replace(/[{}\s]/g, '')))]
}

function estimateAssetValue(item: PromptTemplateItem) {
  const size = (item.templateContent || item.sourcePath || '').length
  if (size >= 1200) {
    return '复杂流程型'
  }
  if (size >= 300) {
    return '标准运营型'
  }
  return '轻量规则型'
}

async function loadTemplates(successMessage?: string) {
  loading.value = true

  try {
    templates.value = await queryPromptTemplates()
    if (successMessage) {
      showFeedback('success', successMessage)
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '提示词模板列表加载失败。'))
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  statsLoading.value = true

  try {
    statistics.value = await fetchPromptTemplateStats()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '提示词模板统计加载失败。'))
  } finally {
    statsLoading.value = false
  }
}

async function refreshAll(successMessage?: string) {
  await Promise.all([loadTemplates(successMessage), loadStatistics()])
}

function openCreateDialog(initialTemplate?: PromptTemplateItem) {
  clearFeedback()
  dialogMode.value = 'create'
  selectedTemplate.value = initialTemplate
    ? {
      ...initialTemplate,
      id: 0,
      templateCode: `${initialTemplate.templateCode}_COPY`,
      templateName: `${initialTemplate.templateName} - 副本`,
      templateStatus: 'ENABLED',
    }
    : null
  dialogOpen.value = true
}

function openEditDialog(template: PromptTemplateItem) {
  clearFeedback()
  dialogMode.value = 'edit'
  selectedTemplate.value = template
  dialogOpen.value = true
}

function openPreview(template: PromptTemplateItem) {
  previewTemplate.value = template
  const variables = template.variableDefinitions?.length
    ? template.variableDefinitions
    : extractVariables(template.templateContent ?? '').map((item) => ({
      variableName: item,
      defaultValue: null,
    }))
  if (variables.length > 0) {
    const nextValue = variables.reduce<Record<string, string>>((result, item) => {
      result[item.variableName] = previewVariableMap.value[item.variableName] ?? item.defaultValue ?? ''
      return result
    }, {})
    previewVariablesText.value = JSON.stringify(nextValue, null, 2)
  }
  previewOpen.value = true
}

async function handleDialogSubmit(payload: PromptTemplatePayload) {
  submitting.value = true

  try {
    if (dialogMode.value === 'create') {
      await createPromptTemplate(payload)
      dialogOpen.value = false
      await refreshAll('提示词模板已创建。')
      return
    }

    if (!selectedTemplate.value) {
      throw new Error('缺少待编辑的模板信息。')
    }

    await updatePromptTemplate(selectedTemplate.value.id, payload)
    dialogOpen.value = false
    await refreshAll('提示词模板已更新。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '保存提示词模板失败。'))
  } finally {
    submitting.value = false
  }
}

function requestDelete(template: PromptTemplateItem) {
  deleteTarget.value = template
}

function handleDeleteDialogVisibility(visible: boolean) {
  if (!visible) {
    deleteTarget.value = null
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) {
    return
  }

  deletePending.value = true

  try {
    const deletingName = deleteTarget.value.templateName
    await removePromptTemplate(deleteTarget.value.id)
    deleteTarget.value = null
    await refreshAll(`提示词模板 ${deletingName} 已删除。`)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除提示词模板失败。'))
  } finally {
    deletePending.value = false
  }
}

onMounted(() => {
  void refreshAll()
})
</script>

<template>
  <MainShell>
    <section
      v-if="feedback"
      class="feedback-banner"
      :class="`feedback-banner--${feedback.tone}`"
      aria-live="polite"
    >
      <span>{{ feedback.message }}</span>
      <button type="button" class="app-button app-button--ghost" @click="clearFeedback">
        关闭
      </button>
    </section>

    <section class="workspace panel-card">
      <header class="workspace__hero">
        <div class="workspace__headline">
          <p class="section-kicker">Prompt Center</p>
          <h2>提示词模板资产中心</h2>
          <p class="workspace__subtitle">
            沉淀面向商业化 Agent 的系统提示词资产。模板统一管理后，可以在 Agent 创建和迭代时稳定复用，降低提示词漂移风险。
          </p>
        </div>

        <div class="workspace__actions">
          <button type="button" class="app-button app-button--secondary" :disabled="loading || statsLoading" @click="refreshAll()">
            <RefreshCw :size="16" aria-hidden="true" />
            刷新
          </button>
          <button type="button" class="app-button" @click="openCreateDialog()">
            <Plus :size="16" aria-hidden="true" />
            新建模板
          </button>
        </div>
      </header>

      <div class="stats-grid">
        <article class="stats-card panel-card">
          <span>模板总数</span>
          <strong>{{ statsLoading ? '...' : statistics.totalCount }}</strong>
          <p>覆盖当前租户的全部提示词资产。</p>
        </article>
        <article class="stats-card panel-card">
          <span>启用模板</span>
          <strong>{{ statsLoading ? '...' : statistics.enabledCount }}</strong>
          <p>当前 Agent 创建页默认可见的可用资产。</p>
        </article>
        <article class="stats-card panel-card">
          <span>启用率</span>
          <strong>{{ statsLoading ? '...' : enabledRate }}</strong>
          <p>衡量模板池可投产比例。</p>
        </article>
        <article class="stats-card panel-card">
          <span>文件模板</span>
          <strong>{{ statsLoading ? '...' : statistics.fileCount }}</strong>
          <p>适合长 Prompt 与外部文件托管。</p>
        </article>
      </div>

      <section class="workspace__toolbar panel-card">
        <div class="filter-grid">
          <label class="field">
            <span class="field__label">搜索模板</span>
            <div class="input-shell">
              <span class="input-shell__icon" aria-hidden="true">
                <Search :size="16" />
              </span>
              <input
                v-model="filters.keyword"
                class="app-input"
                type="text"
                placeholder="按名称、编码、描述或维护人搜索"
              />
            </div>
          </label>

          <label class="field">
            <span class="field__label">状态</span>
            <select v-model="filters.templateStatus" class="app-select">
              <option value="ALL">全部状态</option>
              <option value="ENABLED">仅启用</option>
              <option value="DISABLED">仅禁用</option>
            </select>
          </label>

          <label class="field">
            <span class="field__label">来源</span>
            <select v-model="filters.sourceType" class="app-select">
              <option value="ALL">全部来源</option>
              <option value="INLINE_TEXT">内联文本</option>
              <option value="FILE_PATH">文件路径</option>
            </select>
          </label>
        </div>
      </section>

      <section class="workspace__table panel-card">
        <div class="workspace__table-head">
          <div>
            <strong>模板列表</strong>
            <span>{{ resultsSummary }}</span>
          </div>
          <div class="workspace__legend">
            <span><ShieldCheck :size="14" aria-hidden="true" />规范资产化</span>
            <span><FileCode2 :size="14" aria-hidden="true" />支持文件路径</span>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在加载模板列表...</div>
        <div v-else-if="filteredTemplates.length === 0" class="empty-state">
          当前筛选条件下没有模板，调整筛选条件或创建新的提示词模板。
        </div>
        <div v-else class="template-list">
          <article v-for="item in filteredTemplates" :key="item.id" class="template-card">
            <div class="template-card__head">
              <div>
                <strong>{{ item.templateName }}</strong>
                <p>{{ item.templateCode }}</p>
              </div>
              <span class="template-status" :class="`template-status--${item.templateStatus.toLowerCase()}`">
                {{ formatStatusLabel(item.templateStatus) }}
              </span>
            </div>

            <p class="template-card__description">
              {{ item.description || '该模板暂未补充说明，建议补齐适用场景和输出边界。' }}
            </p>

            <div class="template-card__meta">
              <span>
                <FileText v-if="item.sourceType === 'INLINE_TEXT'" :size="14" aria-hidden="true" />
                <FolderOpen v-else :size="14" aria-hidden="true" />
                {{ formatSourceLabel(item.sourceType) }}
              </span>
              <span>
                <ShieldCheck :size="14" aria-hidden="true" />
                {{ estimateAssetValue(item) }}
              </span>
              <span>{{ item.ownerUserName || '当前用户' }}</span>
              <span>{{ formatTime(item.updateTime) }}</span>
            </div>

            <div class="template-card__content">
              <pre>{{ formatContentPreview(item) }}</pre>
            </div>

            <div class="template-card__actions">
              <button type="button" class="app-button app-button--ghost" @click="openPreview(item)">
                <Eye :size="15" aria-hidden="true" />
                预览
              </button>
              <button type="button" class="app-button app-button--ghost" @click="openCreateDialog(item)">
                <Copy :size="15" aria-hidden="true" />
                复制
              </button>
              <button type="button" class="app-button app-button--ghost" @click="openEditDialog(item)">
                <UserRoundPen :size="15" aria-hidden="true" />
                编辑
              </button>
              <button type="button" class="app-button app-button--ghost app-button--danger-ghost" @click="requestDelete(item)">
                <Trash2 :size="15" aria-hidden="true" />
                删除
              </button>
            </div>
          </article>
        </div>
      </section>
    </section>

    <PromptTemplateFormDialog
      v-model="dialogOpen"
      :mode="dialogMode"
      :template="selectedTemplate"
      :submitting="submitting"
      @submit="handleDialogSubmit"
    />

    <AppDialog
      :model-value="previewOpen"
      title="提示词模板预览"
      description="查看模板元信息与实际内容，确认是否适合绑定到 Agent。"
      width="wide"
      @update:model-value="previewOpen = $event"
    >
      <div v-if="previewTemplate" class="preview-dialog">
        <div class="preview-dialog__meta">
          <article class="preview-stat">
            <span>模板编码</span>
            <strong>{{ previewTemplate.templateCode }}</strong>
          </article>
          <article class="preview-stat">
            <span>来源类型</span>
            <strong>{{ formatSourceLabel(previewTemplate.sourceType) }}</strong>
          </article>
          <article class="preview-stat">
            <span>状态</span>
            <strong>{{ formatStatusLabel(previewTemplate.templateStatus) }}</strong>
          </article>
        </div>

        <div class="preview-dialog__block">
          <strong>模板描述</strong>
          <p>{{ previewTemplate.description || '暂无模板描述。' }}</p>
        </div>

        <div v-if="previewTemplate.sourceType === 'INLINE_TEXT'" class="preview-dialog__block">
          <strong>占位符规范</strong>
          <p>模板变量统一使用 <code>{{variableName}}</code>，变量名仅支持英文字母开头，后接字母、数字或下划线。</p>
          <div v-if="previewVariableDefinitions.length > 0" class="preview-variable-list">
            <article
              v-for="item in previewVariableDefinitions"
              :key="item.variableName"
              class="preview-variable-card"
            >
              <div class="preview-variable-card__head">
                <span class="preview-variable-chip">{{ item.variableName }}</span>
                <span class="preview-variable-badge">{{ item.required ? '必填' : '可选' }}</span>
              </div>
              <p>{{ item.description || '未配置变量说明。' }}</p>
              <small>默认值：{{ item.defaultValue || '-' }}</small>
            </article>
          </div>
          <p v-else>当前模板未声明动态变量，可以直接作为静态系统提示词使用。</p>
        </div>

        <div v-if="previewTemplate.sourceType === 'INLINE_TEXT'" class="preview-dialog__block">
          <strong>渲染预览</strong>
          <p>输入 JSON 变量值后，可以预览模板渲染结果，便于联调 Agent 上下文注入。</p>
          <textarea
            v-model="previewVariablesText"
            class="preview-json-textarea"
            rows="8"
            spellcheck="false"
            placeholder='{
  "userName": "张三"
}'
          />
          <p :class="previewJsonValid ? 'preview-json-state' : 'preview-json-state preview-json-state--error'">
            {{ previewJsonValid ? 'JSON 格式有效，可用于占位符渲染。' : 'JSON 格式无效，当前仅展示原始占位符。' }}
          </p>
          <pre>{{ renderedPreviewContent }}</pre>
        </div>

        <div class="preview-dialog__block">
          <strong>{{ previewTemplate.sourceType === 'INLINE_TEXT' ? '模板正文' : '模板文件路径' }}</strong>
          <pre>{{ previewTemplate.sourceType === 'INLINE_TEXT' ? previewTemplate.templateContent : previewTemplate.sourcePath }}</pre>
        </div>
      </div>
    </AppDialog>

    <ConfirmDialog
      :model-value="deleteTarget !== null"
      title="删除提示词模板"
      :description="deleteDescription"
      confirm-text="确认删除"
      :loading="deletePending"
      @update:model-value="handleDeleteDialogVisibility"
      @confirm="confirmDelete"
    />
  </MainShell>
</template>

<style scoped>
.workspace {
  padding: 30px;
}

.workspace__hero,
.workspace__table-head,
.workspace__legend {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.workspace__hero {
  margin-bottom: 24px;
}

.workspace__headline {
  max-width: 48rem;
}

.workspace__headline h2 {
  margin-top: 10px;
  font-size: clamp(2rem, 2.6vw, 2.8rem);
}

.workspace__subtitle,
.template-card__description,
.template-card__head p,
.preview-dialog__block p {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.workspace__actions {
  display: flex;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.stats-card,
.workspace__toolbar,
.workspace__table {
  padding: 22px;
  border-radius: 26px;
}

.stats-card span,
.preview-stat span {
  color: var(--color-ink-muted);
}

.stats-card strong,
.preview-stat strong {
  display: block;
  margin-top: 8px;
  font-size: 1.45rem;
  color: var(--color-ink-strong);
}

.stats-card p {
  margin: 10px 0 0;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) repeat(2, minmax(180px, 0.8fr));
  gap: 16px;
}

.app-select {
  min-height: 56px;
  padding: 0 18px;
  color: var(--color-ink-strong);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  outline: none;
}

.app-select option {
  color: #f0f5ff;
  background: #0a1524;
}

.workspace__table {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.016)),
    rgba(6, 12, 24, 0.72);
}

.workspace__table-head strong,
.template-card__head strong,
.preview-dialog__block strong {
  color: var(--color-ink-strong);
}

.workspace__table-head span,
.workspace__legend span {
  color: var(--color-ink-soft);
}

.workspace__legend {
  align-items: center;
}

.workspace__legend span,
.template-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
}

.template-list {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.template-card {
  padding: 20px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    radial-gradient(circle at top right, rgba(83, 184, 255, 0.12), transparent 30%),
    rgba(255, 255, 255, 0.04);
}

.template-card__head,
.template-card__actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.template-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.template-card__content {
  margin-top: 14px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(4, 10, 20, 0.55);
}

.template-card__content pre,
.preview-dialog__block pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.84rem;
  line-height: 1.7;
}

.template-card__actions {
  margin-top: 16px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.template-status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
}

.template-status--enabled {
  color: #ddfff6;
  background: rgba(100, 216, 190, 0.16);
}

.template-status--disabled {
  color: #ffe2e2;
  background: rgba(255, 144, 151, 0.14);
}

.preview-dialog {
  display: grid;
  gap: 18px;
}

.preview-dialog__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.preview-stat,
.preview-dialog__block {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.04);
}

.preview-dialog__block {
  display: grid;
  gap: 10px;
}

.preview-dialog__block code {
  color: #d9f7ff;
}

.preview-variable-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.preview-variable-card {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.preview-variable-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.preview-variable-card p,
.preview-variable-card small {
  margin: 0;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.preview-variable-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  color: #d9f7ff;
  background: rgba(83, 184, 255, 0.12);
  border: 1px solid rgba(83, 184, 255, 0.18);
  font-size: 0.8rem;
}

.preview-variable-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.08);
  font-size: 0.78rem;
}

.preview-json-textarea {
  width: 100%;
  min-height: 180px;
  padding: 14px 16px;
  color: var(--color-ink-strong);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(4, 10, 20, 0.55);
  outline: none;
  resize: vertical;
  font-family: var(--font-mono);
}

.preview-json-state {
  color: #c5f7db;
}

.preview-json-state--error {
  color: #ffb7b7;
}

.app-button--ghost {
  color: var(--color-ink-strong);
  background: rgba(255, 255, 255, 0.06);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.app-button--danger-ghost {
  color: #ffd8d8;
  background: rgba(180, 57, 68, 0.14);
  box-shadow: inset 0 0 0 1px rgba(244, 140, 140, 0.16);
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 240px;
  color: var(--color-ink-soft);
  text-align: center;
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .workspace {
    padding: 22px;
  }

  .workspace__hero,
  .workspace__actions,
  .workspace__table-head,
  .workspace__legend,
  .template-card__head,
  .template-card__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .preview-dialog__meta {
    grid-template-columns: 1fr;
  }

  .preview-variable-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
