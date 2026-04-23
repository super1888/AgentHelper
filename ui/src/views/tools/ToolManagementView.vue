<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  createTool,
  debugTool,
  fetchToolCatalog,
  fetchToolDetail,
  fetchToolStats,
  offlineTool,
  publishTool,
  queryToolLogs,
  queryTools,
  removeTool,
  updateTool,
} from '@/api/tool'
import type {
  ToolCatalogItem,
  ToolDebugResult,
  ToolExecutionLogItem,
  ToolItem,
  ToolPayload,
  ToolStatistics,
} from '@/types/tool'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

const loading = ref(false)
const saving = ref(false)
const actionLoading = ref(false)
const selectedToolId = ref<number | null>(null)
const selectedTool = ref<ToolItem | null>(null)
const tools = ref<ToolItem[]>([])
const catalog = ref<ToolCatalogItem[]>([])
const logs = ref<ToolExecutionLogItem[]>([])
const debugResult = ref<ToolDebugResult | null>(null)
const feedback = ref<FeedbackState | null>(null)

const stats = ref<ToolStatistics>({
  totalCount: 0,
  enabledCount: 0,
  publishedCount: 0,
  builtinCount: 0,
  externalCount: 0,
  highRiskCount: 0,
  totalLogCount: 0,
  successLogCount: 0,
  failureLogCount: 0,
})

const filters = reactive({
  keyword: '',
  toolStatus: 'ALL',
  publishStatus: 'ALL',
  sourceType: 'ALL',
})

const form = reactive({
  toolCode: '',
  toolName: '',
  description: '',
  toolType: 'API',
  toolCategory: 'KNOWLEDGE',
  sourceType: 'BUILTIN',
  toolStatus: 'ENABLED',
  riskLevel: 'LOW',
  executionMode: 'SYNC',
  sortWeight: 100,
  timeoutMs: 15000,
  authRequired: 0,
  builtinToolKey: '',
  endpointUrl: '',
  httpMethod: 'POST',
  tagsText: 'builtin,default',
  requestSchemaJson: '{\n  "type": "object",\n  "properties": {}\n}',
  authConfigJson: '{\n  "enabled": false\n}',
  runtimeConfigJson: '{\n  "timeoutSeconds": 15\n}',
  testPayloadJson: '{\n  "query": "hello"\n}',
  remark: '',
})

const debugForm = reactive({
  requestPayloadJson: '{\n  "query": "hello"\n}',
  sourceType: 'DEBUG',
})

const logQuery = reactive({
  sourceType: '',
  successFlag: '',
})

const filteredTools = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return tools.value.filter((item) => {
    const matchKeyword = !keyword
      || [item.toolCode, item.toolName, item.description, item.toolCategory]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    const matchStatus = filters.toolStatus === 'ALL' || item.toolStatus === filters.toolStatus
    const matchPublish = filters.publishStatus === 'ALL' || item.publishStatus === filters.publishStatus
    const matchSource = filters.sourceType === 'ALL' || item.sourceType === filters.sourceType
    return matchKeyword && matchStatus && matchPublish && matchSource
  })
})

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function formatTime(value?: number | null) {
  if (!value) return '未记录'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value)
}

function resetForm() {
  selectedToolId.value = null
  selectedTool.value = null
  debugResult.value = null
  logs.value = []
  form.toolCode = ''
  form.toolName = ''
  form.description = ''
  form.toolType = 'API'
  form.toolCategory = 'KNOWLEDGE'
  form.sourceType = 'BUILTIN'
  form.toolStatus = 'ENABLED'
  form.riskLevel = 'LOW'
  form.executionMode = 'SYNC'
  form.sortWeight = 100
  form.timeoutMs = 15000
  form.authRequired = 0
  form.builtinToolKey = ''
  form.endpointUrl = ''
  form.httpMethod = 'POST'
  form.tagsText = 'builtin,default'
  form.requestSchemaJson = '{\n  "type": "object",\n  "properties": {}\n}'
  form.authConfigJson = '{\n  "enabled": false\n}'
  form.runtimeConfigJson = '{\n  "timeoutSeconds": 15\n}'
  form.testPayloadJson = '{\n  "query": "hello"\n}'
  form.remark = ''
  debugForm.requestPayloadJson = '{\n  "query": "hello"\n}'
}

function fillForm(tool: ToolItem) {
  selectedToolId.value = tool.id
  selectedTool.value = tool
  form.toolCode = tool.toolCode
  form.toolName = tool.toolName
  form.description = tool.description ?? ''
  form.toolType = tool.toolType
  form.toolCategory = tool.toolCategory
  form.sourceType = tool.sourceType
  form.toolStatus = tool.toolStatus
  form.riskLevel = tool.riskLevel
  form.executionMode = tool.executionMode
  form.sortWeight = tool.sortWeight ?? 100
  form.timeoutMs = tool.timeoutMs ?? 15000
  form.authRequired = tool.authRequired ?? 0
  form.builtinToolKey = tool.builtinToolKey ?? ''
  form.endpointUrl = tool.endpointUrl ?? ''
  form.httpMethod = tool.httpMethod ?? 'POST'
  form.tagsText = (tool.tags ?? []).join(',')
  form.requestSchemaJson = tool.requestSchemaJson || '{}'
  form.authConfigJson = tool.authConfigJson || '{}'
  form.runtimeConfigJson = tool.runtimeConfigJson || '{}'
  form.testPayloadJson = tool.testPayloadJson || '{}'
  form.remark = tool.remark ?? ''
  debugForm.requestPayloadJson = tool.testPayloadJson || '{}'
}

function applyCatalogItem(item: ToolCatalogItem) {
  if (selectedToolId.value) return
  form.toolCode = item.toolKey
  form.toolName = item.toolName
  form.description = item.description ?? ''
  form.toolType = item.toolType
  form.toolCategory = item.toolCategory
  form.sourceType = item.sourceType
  form.builtinToolKey = item.toolKey
  form.tagsText = (item.tags ?? []).join(',')
  form.requestSchemaJson = item.defaultRequestSchemaJson || '{}'
  form.runtimeConfigJson = item.defaultRuntimeConfigJson || '{}'
  form.testPayloadJson = item.defaultTestPayloadJson || '{}'
  debugForm.requestPayloadJson = item.defaultTestPayloadJson || '{}'
}

function buildPayload(): ToolPayload {
  return {
    toolCode: form.toolCode.trim(),
    toolName: form.toolName.trim(),
    description: form.description.trim() || null,
    toolType: form.toolType,
    toolCategory: form.toolCategory,
    sourceType: form.sourceType,
    toolStatus: form.toolStatus,
    riskLevel: form.riskLevel,
    executionMode: form.executionMode,
    sortWeight: form.sortWeight,
    timeoutMs: form.timeoutMs,
    authRequired: form.authRequired,
    builtinToolKey: form.builtinToolKey.trim() || null,
    endpointUrl: form.endpointUrl.trim() || null,
    httpMethod: form.httpMethod.trim() || null,
    tags: form.tagsText.split(',').map((item) => item.trim()).filter(Boolean),
    requestSchemaJson: form.requestSchemaJson.trim() || null,
    authConfigJson: form.authConfigJson.trim() || null,
    runtimeConfigJson: form.runtimeConfigJson.trim() || null,
    testPayloadJson: form.testPayloadJson.trim() || null,
    remark: form.remark.trim() || null,
  }
}

async function loadLogs() {
  logs.value = await queryToolLogs({
    toolId: selectedToolId.value,
    sourceType: logQuery.sourceType || null,
    successFlag: logQuery.successFlag === '' ? null : Number(logQuery.successFlag),
  })
}

async function selectTool(toolId: number) {
  const detail = await fetchToolDetail(toolId)
  fillForm(detail)
  await loadLogs()
}

async function refreshAll(keepSelection = true) {
  loading.value = true
  try {
    const currentId = keepSelection ? selectedToolId.value : null
    const [toolList, statResult, catalogResult] = await Promise.all([
      queryTools(),
      fetchToolStats(),
      fetchToolCatalog(),
    ])
    tools.value = toolList
    stats.value = statResult
    catalog.value = catalogResult
    if (currentId) {
      const exists = tools.value.find((item) => item.id === currentId)
      if (exists) {
        await selectTool(currentId)
      } else {
        resetForm()
      }
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '工具数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    if (selectedToolId.value) {
      await updateTool(selectedToolId.value, buildPayload())
      showFeedback('success', '工具已更新')
    } else {
      const created = await createTool(buildPayload())
      showFeedback('success', '工具已创建')
      await refreshAll(false)
      await selectTool(created.id)
      return
    }
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '工具保存失败'))
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!selectedToolId.value || !window.confirm('确认删除当前工具吗？')) return
  actionLoading.value = true
  try {
    await removeTool(selectedToolId.value)
    showFeedback('success', '工具已删除')
    resetForm()
    await refreshAll(false)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '工具删除失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handlePublish() {
  if (!selectedToolId.value) return
  actionLoading.value = true
  try {
    await publishTool(selectedToolId.value)
    showFeedback('success', '工具已发布')
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '工具发布失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handleOffline() {
  if (!selectedToolId.value) return
  actionLoading.value = true
  try {
    await offlineTool(selectedToolId.value)
    showFeedback('success', '工具已下线')
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '工具下线失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handleDebug() {
  if (!selectedToolId.value) {
    showFeedback('info', '请先选择或创建工具')
    return
  }
  actionLoading.value = true
  try {
    debugResult.value = await debugTool({
      toolId: selectedToolId.value,
      requestPayloadJson: debugForm.requestPayloadJson.trim(),
      sourceType: debugForm.sourceType,
    })
    await loadLogs()
    await refreshAll()
    showFeedback('success', '工具调试完成')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '工具调试失败'))
  } finally {
    actionLoading.value = false
  }
}

onMounted(async () => {
  resetForm()
  await refreshAll(false)
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

    <section class="tool-page">
      <article class="panel-card hero-panel">
        <div class="hero-panel__head">
          <div>
            <p class="section-kicker">工具注册中心</p>
            <h2>工具管理台</h2>
            <p class="hero-panel__summary">统一管理智能体可用工具的目录、来源、风险、调试和日志。</p>
          </div>
          <div class="toolbar-actions">
            <button class="app-button app-button--secondary" type="button" @click="resetForm">新建工具</button>
            <button class="app-button" type="button" :disabled="saving" @click="handleSave">
              {{ saving ? '保存中...' : '保存工具' }}
            </button>
          </div>
        </div>

        <div class="stats-strip">
          <div class="metric-card"><span>工具总数</span><strong>{{ stats.totalCount }}</strong></div>
          <div class="metric-card"><span>启用中</span><strong>{{ stats.enabledCount }}</strong></div>
          <div class="metric-card"><span>已发布</span><strong>{{ stats.publishedCount }}</strong></div>
          <div class="metric-card"><span>高风险</span><strong>{{ stats.highRiskCount }}</strong></div>
          <div class="metric-card"><span>日志总数</span><strong>{{ stats.totalLogCount }}</strong></div>
        </div>
      </article>

      <div class="tool-layout">
        <article class="panel-card section-panel section-panel--sidebar">
          <div class="section-panel__head">
            <div>
              <h3>工具目录</h3>
              <p>筛选、选择并快速切换当前工具。</p>
            </div>
          </div>

          <div class="filter-grid">
            <input v-model="filters.keyword" class="app-input" type="text" placeholder="搜索编码、名称或分类" />
            <select v-model="filters.toolStatus" class="app-select">
              <option value="ALL">全部状态</option>
              <option value="ENABLED">ENABLED</option>
              <option value="DISABLED">DISABLED</option>
            </select>
            <select v-model="filters.publishStatus" class="app-select">
              <option value="ALL">全部发布态</option>
              <option value="DRAFT">DRAFT</option>
              <option value="PUBLISHED">PUBLISHED</option>
              <option value="OFFLINE">OFFLINE</option>
            </select>
            <select v-model="filters.sourceType" class="app-select">
              <option value="ALL">全部来源</option>
              <option value="BUILTIN">BUILTIN</option>
              <option value="API">API</option>
              <option value="MCP">MCP</option>
              <option value="AGENT">AGENT</option>
              <option value="CUSTOM">CUSTOM</option>
            </select>
          </div>

          <div class="catalog-strip">
            <button
              v-for="item in catalog"
              :key="item.toolKey"
              class="catalog-chip"
              type="button"
              @click="applyCatalogItem(item)"
            >
              {{ item.toolName }}
            </button>
          </div>

          <div v-if="loading" class="empty-state">加载中...</div>
          <div v-else-if="filteredTools.length === 0" class="empty-state">暂无工具数据</div>
          <div v-else class="tool-list">
            <button
              v-for="item in filteredTools"
              :key="item.id"
              class="tool-list__item"
              :class="{ 'tool-list__item--active': item.id === selectedToolId }"
              type="button"
              @click="selectTool(item.id)"
            >
              <strong>{{ item.toolName }}</strong>
              <span>{{ item.toolCode }}</span>
              <small>{{ item.sourceType }} / {{ item.publishStatus }} / {{ item.riskLevel }}</small>
            </button>
          </div>
        </article>

        <article class="panel-card section-panel">
          <div class="section-panel__head">
            <div>
              <h3>工具配置</h3>
              <p>{{ selectedTool ? `${selectedTool.toolName} · ${selectedTool.toolCode}` : '新建工具配置' }}</p>
            </div>
            <div class="toolbar-actions">
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedToolId" @click="handlePublish">发布</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedToolId" @click="handleOffline">下线</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedToolId || actionLoading" @click="handleDelete">删除</button>
            </div>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">工具编码</span><input v-model="form.toolCode" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">工具名称</span><input v-model="form.toolName" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">工具类型</span><input v-model="form.toolType" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">工具分类</span><input v-model="form.toolCategory" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">来源类型</span><select v-model="form.sourceType" class="app-select"><option>BUILTIN</option><option>API</option><option>MCP</option><option>AGENT</option><option>CUSTOM</option></select></label>
            <label class="field"><span class="field__label">工具状态</span><select v-model="form.toolStatus" class="app-select"><option>ENABLED</option><option>DISABLED</option></select></label>
            <label class="field"><span class="field__label">风险等级</span><select v-model="form.riskLevel" class="app-select"><option>LOW</option><option>MEDIUM</option><option>HIGH</option></select></label>
            <label class="field"><span class="field__label">执行模式</span><select v-model="form.executionMode" class="app-select"><option>SYNC</option><option>ASYNC</option></select></label>
            <label class="field"><span class="field__label">超时毫秒</span><input v-model.number="form.timeoutMs" class="app-input" type="number" /></label>
            <label class="field"><span class="field__label">排序权重</span><input v-model.number="form.sortWeight" class="app-input" type="number" /></label>
            <label class="field"><span class="field__label">内置工具键</span><input v-model="form.builtinToolKey" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">HTTP 方法</span><input v-model="form.httpMethod" class="app-input" type="text" /></label>
          </div>

          <label class="field"><span class="field__label">工具描述</span><textarea v-model="form.description" class="app-textarea" rows="3" /></label>
          <label class="field"><span class="field__label">外部地址</span><input v-model="form.endpointUrl" class="app-input" type="text" /></label>
          <label class="field"><span class="field__label">标签（逗号分隔）</span><input v-model="form.tagsText" class="app-input" type="text" /></label>
          <label class="field field--inline"><span class="field__label">需要认证</span><input v-model="form.authRequired" type="checkbox" :true-value="1" :false-value="0" /></label>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">请求 Schema JSON</span><textarea v-model="form.requestSchemaJson" class="app-textarea code-area" rows="9" /></label>
            <label class="field"><span class="field__label">运行配置 JSON</span><textarea v-model="form.runtimeConfigJson" class="app-textarea code-area" rows="9" /></label>
            <label class="field"><span class="field__label">认证配置 JSON</span><textarea v-model="form.authConfigJson" class="app-textarea code-area" rows="8" /></label>
            <label class="field"><span class="field__label">默认测试载荷 JSON</span><textarea v-model="form.testPayloadJson" class="app-textarea code-area" rows="8" /></label>
          </div>

          <label class="field"><span class="field__label">备注</span><input v-model="form.remark" class="app-input" type="text" /></label>

          <div class="section-panel__sub">
            <div>
              <h4>在线调试</h4>
              <p>对当前工具配置做快速校验并生成日志记录。</p>
            </div>
            <button class="app-button app-button--secondary" type="button" :disabled="actionLoading" @click="handleDebug">执行调试</button>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">调试来源</span><select v-model="debugForm.sourceType" class="app-select"><option>DEBUG</option><option>RUNTIME</option></select></label>
            <div class="field field--meta">
              <span class="field__label">最近更新时间</span>
              <strong>{{ selectedTool ? formatTime(selectedTool.updateTime) : '未选择工具' }}</strong>
            </div>
          </div>
          <label class="field"><span class="field__label">调试请求 JSON</span><textarea v-model="debugForm.requestPayloadJson" class="app-textarea code-area" rows="8" /></label>
          <pre v-if="debugResult" class="result-box">{{ JSON.stringify(debugResult, null, 2) }}</pre>

          <div class="section-panel__sub">
            <div>
              <h4>执行日志</h4>
              <p>查看调试与运行阶段的请求响应记录。</p>
            </div>
            <button class="app-button app-button--secondary" type="button" @click="loadLogs">刷新日志</button>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">日志来源</span><input v-model="logQuery.sourceType" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">成功标记</span><select v-model="logQuery.successFlag" class="app-select"><option value="">全部</option><option value="1">成功</option><option value="0">失败</option></select></label>
          </div>

          <div v-if="logs.length === 0" class="empty-state empty-state--compact">暂无日志</div>
          <div v-else class="log-list">
            <article v-for="item in logs" :key="item.id" class="log-card">
              <div class="log-card__head">
                <strong>{{ item.toolName || item.toolCode }}</strong>
                <span>{{ item.executeStatus }} / {{ formatTime(item.createTime) }}</span>
              </div>
              <p>{{ item.failureReason || '执行成功' }}</p>
              <pre class="result-box">{{ item.responsePayloadJson || item.requestPayloadJson || '{}' }}</pre>
            </article>
          </div>
        </article>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.tool-page { display: grid; gap: 24px; min-width: 0; min-height: 100%; }
.hero-panel, .section-panel, .metric-card, .catalog-chip, .tool-list__item, .log-card, .empty-state, .result-box { border: 1px solid rgba(255,255,255,.08); }
.hero-panel, .section-panel {
  padding: 28px;
  border-radius: 26px;
  background: linear-gradient(180deg, rgba(255,255,255,.034), rgba(255,255,255,.012)), rgba(7,14,26,.82);
  box-shadow: 0 24px 56px rgba(0,0,0,.22);
}
.hero-panel__head, .section-panel__head, .section-panel__sub, .toolbar-actions { display: flex; justify-content: space-between; gap: 14px; align-items: flex-start; flex-wrap: wrap; }
.hero-panel__head h2, .section-panel__head h3, .section-panel__sub h4 { margin: 0; color: var(--color-ink-strong); }
.hero-panel__summary, .section-panel__head p, .section-panel__sub p { color: var(--color-ink-soft); line-height: 1.7; }
.stats-strip { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-top: 22px; }
.metric-card { padding: 18px 20px; border-radius: 18px; background: linear-gradient(180deg, rgba(255,255,255,.045), rgba(255,255,255,.02)); }
.metric-card span { color: var(--color-ink-soft); }
.metric-card strong { display: block; margin-top: 10px; color: var(--color-ink-strong); font-size: 1.6rem; }
.tool-layout { display: grid; grid-template-columns: 360px minmax(0, 1fr); gap: 20px; align-items: start; min-width: 0; }
.section-panel--sidebar { display: grid; gap: 16px; align-content: start; }
.filter-grid, .form-grid { display: grid; gap: 14px; }
.form-grid--double { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.catalog-strip { display: flex; flex-wrap: wrap; gap: 10px; }
.catalog-chip, .tool-list__item {
  border-radius: 16px;
  background: rgba(255,255,255,.03);
  color: var(--color-ink-soft);
}
.catalog-chip { padding: 10px 14px; cursor: pointer; }
.tool-list { display: grid; gap: 10px; overflow: auto; min-height: 0; padding-right: 4px; scrollbar-gutter: stable; }
.tool-list__item { display: grid; gap: 6px; padding: 14px 16px; text-align: left; cursor: pointer; }
.tool-list__item strong, .field__label, .field--meta strong, .log-card__head strong { color: var(--color-ink-strong); }
.tool-list__item--active { background: rgba(76,162,255,.08); border-color: rgba(108,201,255,.22); }
.tool-list__item span, .tool-list__item small, .field--meta, .log-card__head span, .log-card p { color: var(--color-ink-soft); }
.field { display: grid; gap: 8px; }
.field--inline { grid-template-columns: auto auto; justify-content: flex-start; align-items: center; gap: 12px; }
.field--meta { align-content: center; min-height: 52px; }
.app-input, .app-select, .app-textarea {
  width: 100%;
  border: 1px solid rgba(147,177,233,.28);
  border-radius: 14px;
  background: rgba(8,16,30,.96);
}
.app-input, .app-select { min-height: 52px; padding: 0 14px; }
.app-textarea { min-height: 120px; padding: 14px; }
.code-area, .result-box { font-family: var(--font-mono); font-size: .84rem; }
.result-box {
  margin: 0;
  padding: 16px;
  border-radius: 14px;
  background: rgba(8,16,30,.96);
  white-space: pre-wrap;
  word-break: break-word;
}
.log-list { display: grid; gap: 12px; min-width: 0; }
.log-card { padding: 16px 18px; border-radius: 18px; background: rgba(255,255,255,.018); min-width: 0; }
.log-card__head { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.empty-state {
  display: grid;
  place-items: center;
  min-height: 96px;
  padding: 18px;
  border-radius: 16px;
  color: var(--color-ink-soft);
  background: rgba(255,255,255,.014);
  text-align: center;
}
.empty-state--compact { min-height: 72px; }
@media (max-width: 1380px) {
  .stats-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 1180px) {
  .tool-layout, .form-grid--double { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .hero-panel, .section-panel { padding: 20px; border-radius: 20px; }
  .hero-panel__head, .section-panel__head, .section-panel__sub, .toolbar-actions { flex-direction: column; align-items: stretch; }
  .stats-strip { grid-template-columns: 1fr; }
}
</style>
