<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Eye, Pencil, Plus, RefreshCw, Search, Trash2, WandSparkles } from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MainShell from '@/components/MainShell.vue'
import PromptTemplateFormDialog from '@/components/PromptTemplateFormDialog.vue'
import {
  createPromptTemplate,
  fetchPromptTemplateStats,
  queryPromptTemplates,
  removePromptTemplate,
  renderPromptTemplate,
  updatePromptTemplate,
} from '@/api/prompt'
import type {
  PromptTemplateItem,
  PromptTemplatePayload,
  PromptTemplateRenderResult,
  PromptTemplateStatistics,
} from '@/types/prompt'
import { getErrorMessage } from '@/utils/errors'

type DialogMode = 'create' | 'edit'
type Tone = 'success' | 'error'

const loading = ref(false)
const statsLoading = ref(false)
const submitting = ref(false)
const deleting = ref(false)
const dialogOpen = ref(false)
const previewOpen = ref(false)
const feedback = ref<{ tone: Tone; message: string } | null>(null)
const dialogMode = ref<DialogMode>('create')
const selectedTemplate = ref<PromptTemplateItem | null>(null)
const previewTemplate = ref<PromptTemplateItem | null>(null)
const deleteTarget = ref<PromptTemplateItem | null>(null)
const renderLoading = ref(false)
const renderJson = ref(`{
  "user_id": "U1001",
  "order_status": "refund",
  "order_list": [
    { "name": "笔记本电脑", "price": "5999" }
  ]
}`)
const renderResult = ref<PromptTemplateRenderResult | null>(null)
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
  status: 'ALL',
  sourceType: 'ALL',
})

const filteredTemplates = computed(() => templates.value.filter((item) => {
  const keyword = filters.keyword.trim().toLowerCase()
  const matchesKeyword = !keyword || [
    item.templateCode,
    item.templateName,
    item.description,
    item.ownerUserName,
  ].filter(Boolean).some((value) => String(value).toLowerCase().includes(keyword))
  const matchesStatus = filters.status === 'ALL' || item.templateStatus === filters.status
  const matchesSource = filters.sourceType === 'ALL' || item.sourceType === filters.sourceType
  return matchesKeyword && matchesStatus && matchesSource
}))

function showFeedback(tone: Tone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function formatTime(value: number | null) {
  return value
    ? new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(value)
    : '未记录'
}

function sourceLabel(value: string) {
  return value === 'INLINE_TEXT' ? '内联文本' : '文件路径'
}

function statusLabel(value: string) {
  return value === 'ENABLED' ? '启用' : '停用'
}

function capabilityCount(item: PromptTemplateItem) {
  const config = item.enterpriseConfig
  if (!config) {
    return 0
  }
  return [
    config.rendering.dynamicVariables.length,
    config.rolePolicy.forbiddenActions.length,
    config.workflowPolicy.workflowStages.length,
    config.securityPolicy.desensitizationRules.length,
    config.integrationPolicy.externalSystems.length,
  ].filter((count) => count > 0).length
}

function configLabel(item: PromptTemplateItem) {
  return item.enterpriseConfig?.outputPolicy.outputFormat || '未定义格式'
}

function capabilitySummary(item: PromptTemplateItem) {
  return [
    `变量 ${item.variableDefinitions.length}`,
    `增强 ${capabilityCount(item)}`,
    configLabel(item),
  ]
}

async function refreshAll(message?: string) {
  loading.value = true
  statsLoading.value = true
  try {
    const [list, stats] = await Promise.all([
      queryPromptTemplates(),
      fetchPromptTemplateStats(),
    ])
    templates.value = list
    statistics.value = stats
    if (message) {
      showFeedback('success', message)
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '提示词模板加载失败。'))
  } finally {
    loading.value = false
    statsLoading.value = false
  }
}

function openCreateDialog() {
  clearFeedback()
  dialogMode.value = 'create'
  selectedTemplate.value = null
  dialogOpen.value = true
}

function openEditDialog(item: PromptTemplateItem) {
  clearFeedback()
  dialogMode.value = 'edit'
  selectedTemplate.value = item
  dialogOpen.value = true
}

function openPreview(item: PromptTemplateItem) {
  previewTemplate.value = item
  renderResult.value = null
  previewOpen.value = true
}

function askDelete(item: PromptTemplateItem) {
  deleteTarget.value = item
}

async function submitDialog(payload: PromptTemplatePayload) {
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createPromptTemplate(payload)
    } else if (selectedTemplate.value) {
      await updatePromptTemplate(selectedTemplate.value.id, payload)
    }
    dialogOpen.value = false
    await refreshAll(dialogMode.value === 'create' ? '模板已创建。' : '模板已更新。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '模板保存失败。'))
  } finally {
    submitting.value = false
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) {
    return
  }
  deleting.value = true
  try {
    const name = deleteTarget.value.templateName
    await removePromptTemplate(deleteTarget.value.id)
    deleteTarget.value = null
    await refreshAll(`模板 ${name} 已删除。`)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除模板失败。'))
  } finally {
    deleting.value = false
  }
}

async function runRender() {
  if (!previewTemplate.value) {
    return
  }
  let variables: Record<string, unknown>
  try {
    variables = JSON.parse(renderJson.value)
  } catch {
    showFeedback('error', '试运行 JSON 格式不正确。')
    return
  }
  renderLoading.value = true
  try {
    renderResult.value = await renderPromptTemplate(previewTemplate.value.id, { variables })
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '模板试运行失败。'))
  } finally {
    renderLoading.value = false
  }
}

onMounted(() => {
  void refreshAll()
})
</script>

<template>
  <MainShell>
    <AppFeedbackDialog
      :model-value="Boolean(feedback)"
      :tone="feedback?.tone ?? 'info'"
      :message="feedback?.message ?? ''"
      @update:model-value="!$event && clearFeedback()"
    />

    <section class="management-page">
      <header class="management-hero panel-card">
        <div>
          <p class="section-kicker">Prompt Center</p>
          <h2>提示词模板管理</h2>
          <p class="management-hero__meta">统一管理模板资产、变量定义、规则增强和试运行能力。</p>
        </div>
        <div class="management-hero__actions">
          <button type="button" class="app-button app-button--secondary" :disabled="loading || statsLoading" @click="refreshAll()">
            <RefreshCw :size="16" />
            刷新
          </button>
          <button type="button" class="app-button" @click="openCreateDialog()">
            <Plus :size="16" />
            新建模板
          </button>
        </div>
      </header>

      <section class="management-stats prompt-stats">
        <article class="panel-card management-stat">
          <span>模板总数</span>
          <strong>{{ statsLoading ? '...' : statistics.totalCount }}</strong>
        </article>
        <article class="panel-card management-stat">
          <span>启用模板</span>
          <strong>{{ statsLoading ? '...' : statistics.enabledCount }}</strong>
        </article>
        <article class="panel-card management-stat">
          <span>文件模板</span>
          <strong>{{ statsLoading ? '...' : statistics.fileCount }}</strong>
        </article>
      </section>

      <section class="panel-card management-panel">
        <div class="management-filter-grid prompt-filter-grid">
          <label class="field">
            <span class="field__label">搜索</span>
            <div class="input-shell">
              <span class="input-shell__icon"><Search :size="16" /></span>
              <input v-model="filters.keyword" class="app-input" placeholder="按编码、名称、说明搜索" />
            </div>
          </label>
          <label class="field">
            <span class="field__label">状态</span>
            <select v-model="filters.status" class="app-select">
              <option value="ALL">全部</option>
              <option value="ENABLED">启用</option>
              <option value="DISABLED">停用</option>
            </select>
          </label>
          <label class="field">
            <span class="field__label">来源</span>
            <select v-model="filters.sourceType" class="app-select">
              <option value="ALL">全部</option>
              <option value="INLINE_TEXT">内联文本</option>
              <option value="FILE_PATH">文件路径</option>
            </select>
          </label>
        </div>
      </section>

      <section class="panel-card management-panel">
        <div class="table-head">
          <strong>模板列表</strong>
          <span class="management-pager__summary">共 {{ filteredTemplates.length }} 条</span>
        </div>
        <div v-if="loading" class="management-empty">正在加载模板列表...</div>
        <div v-else-if="filteredTemplates.length === 0" class="management-empty">当前筛选条件下没有模板。</div>
        <div v-else class="row-list">
          <article v-for="item in filteredTemplates" :key="item.id" class="row-card">
            <div class="row-main">
              <div class="row-title">
                <strong>{{ item.templateName }}</strong>
                <span class="row-code">{{ item.templateCode }}</span>
              </div>
              <p class="row-desc">{{ item.description || '未填写模板说明' }}</p>
              <div class="row-meta">
                <span>{{ statusLabel(item.templateStatus) }}</span>
                <span>{{ sourceLabel(item.sourceType) }}</span>
                <span>{{ item.ownerUserName || '当前用户' }}</span>
                <span>{{ formatTime(item.updateTime) }}</span>
              </div>
              <div class="row-tags">
                <span v-for="tag in capabilitySummary(item)" :key="tag">{{ tag }}</span>
              </div>
            </div>
            <div class="row-actions">
              <button type="button" class="app-button app-button--ghost" @click="openPreview(item)">
                <Eye :size="15" />
                预览
              </button>
              <button type="button" class="app-button app-button--ghost" @click="openEditDialog(item)">
                <Pencil :size="15" />
                编辑
              </button>
              <button type="button" class="app-button app-button--ghost app-button--danger-ghost" @click="askDelete(item)">
                <Trash2 :size="15" />
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
      @submit="submitDialog"
    />

    <AppDialog
      :model-value="previewOpen"
      title="模板预览与试运行"
      description="查看模板能力摘要，并使用 JSON 变量进行一次渲染验证。"
      width="wide"
      @update:model-value="previewOpen = $event"
    >
      <div v-if="previewTemplate" class="preview-grid">
        <article class="preview-card">
          <span>模板编码</span>
          <strong>{{ previewTemplate.templateCode }}</strong>
        </article>
        <article class="preview-card">
          <span>输出格式</span>
          <strong>{{ previewTemplate.enterpriseConfig?.outputPolicy.outputFormat || '未设置' }}</strong>
        </article>
        <article class="preview-card">
          <span>增强能力</span>
          <strong>{{ capabilityCount(previewTemplate) }}</strong>
        </article>
        <article class="preview-block">
          <strong>模板摘要</strong>
          <div class="row-tags">
            <span v-for="tag in capabilitySummary(previewTemplate)" :key="tag">{{ tag }}</span>
          </div>
          <pre>{{ previewTemplate.sourceType === 'INLINE_TEXT' ? previewTemplate.templateContent : previewTemplate.sourcePath }}</pre>
        </article>
        <article class="preview-block">
          <div class="section-head">
            <strong>试运行变量</strong>
            <button type="button" class="app-button app-button--secondary" :disabled="renderLoading" @click="runRender">
              <WandSparkles :size="16" />
              {{ renderLoading ? '渲染中...' : '立即试运行' }}
            </button>
          </div>
          <div class="input-shell input-shell--textarea">
            <textarea v-model="renderJson" class="app-textarea code-textarea" rows="10" spellcheck="false" />
          </div>
        </article>
        <article class="preview-block">
          <strong>渲染结果</strong>
          <template v-if="renderResult">
            <div class="row-tags">
              <span v-for="item in renderResult.missingVariables" :key="item">缺失 {{ item }}</span>
              <span v-for="item in renderResult.appliedConditions" :key="item">条件 {{ item }}</span>
              <span v-for="item in renderResult.appliedLoops" :key="item">循环 {{ item }}</span>
            </div>
            <pre>{{ renderResult.renderedContent }}</pre>
          </template>
          <p v-else class="hint">输入变量 JSON 后点击试运行。</p>
        </article>
      </div>
    </AppDialog>

    <ConfirmDialog
      :model-value="deleteTarget !== null"
      title="删除模板"
      :description="deleteTarget ? `确认删除模板“${deleteTarget.templateName}”吗？` : ''"
      confirm-text="确认删除"
      :loading="deleting"
      @update:model-value="(visible) => { if (!visible) deleteTarget = null }"
      @confirm="confirmDelete"
    />
  </MainShell>
</template>

<style scoped>
.prompt-stats {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.prompt-filter-grid {
  grid-template-columns: minmax(0, 1fr) 180px 180px;
}

.row-list,
.preview-grid {
  display: grid;
  gap: 16px;
}

.table-head,
.row-card,
.row-actions,
.row-meta,
.row-tags,
.section-head {
  display: flex;
  gap: 12px;
}

.table-head,
.row-card,
.section-head {
  justify-content: space-between;
}

.row-card,
.preview-card,
.preview-block {
  padding: var(--compact-panel-padding);
  border-radius: var(--sub-panel-radius);
  background: rgba(255, 255, 255, 0.04);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.row-card {
  align-items: flex-start;
}

.row-main {
  display: grid;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.row-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.row-code,
.row-meta,
.hint {
  color: var(--color-ink-soft);
}

.row-desc {
  line-height: 1.7;
  color: var(--color-ink-soft);
}

.row-meta,
.row-tags,
.row-actions {
  flex-wrap: wrap;
}

.row-meta span,
.row-tags span {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--color-ink-soft);
}

.preview-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.preview-card span {
  color: var(--color-ink-muted);
}

.preview-card strong {
  display: block;
  margin-top: 8px;
  color: var(--color-ink-strong);
}

.preview-block {
  grid-column: 1 / -1;
  display: grid;
  gap: 12px;
}

.preview-block pre {
  margin: 0;
  padding: 16px;
  border-radius: 18px;
  background: rgba(5, 11, 20, 0.7);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
  color: var(--color-ink-soft);
}

.code-textarea {
  font-family: var(--font-mono);
}

@media (max-width: 980px) {
  .prompt-stats,
  .prompt-filter-grid,
  .preview-grid {
    grid-template-columns: 1fr;
  }

  .row-card,
  .table-head,
  .section-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
