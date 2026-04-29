<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  createMcpServer,
  debugMcpServer,
  fetchMcpCatalog,
  fetchMcpServerDetail,
  fetchMcpStats,
  offlineMcpServer,
  publishMcpServer,
  queryMcpLogs,
  queryMcpServers,
  removeMcpServer,
  updateMcpServer,
} from '@/api/mcp'
import type {
  McpCatalogItem,
  McpDebugResult,
  McpExecutionLogItem,
  McpItem,
  McpPayload,
  McpStatistics,
} from '@/types/mcp'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

const loading = ref(false)
const saving = ref(false)
const actionLoading = ref(false)
const selectedServerId = ref<number | null>(null)
const selectedServer = ref<McpItem | null>(null)
const servers = ref<McpItem[]>([])
const catalog = ref<McpCatalogItem[]>([])
const logs = ref<McpExecutionLogItem[]>([])
const debugResult = ref<McpDebugResult | null>(null)
const feedback = ref<FeedbackState | null>(null)

const stats = ref<McpStatistics>({
  totalCount: 0,
  enabledCount: 0,
  publishedCount: 0,
  builtinCount: 0,
  remoteCount: 0,
  highRiskCount: 0,
  totalLogCount: 0,
  successLogCount: 0,
  failureLogCount: 0,
})

const filters = reactive({
  keyword: '',
  serverStatus: 'ALL',
  publishStatus: 'ALL',
  serverType: 'ALL',
})

const form = reactive({
  serverCode: '',
  serverName: '',
  description: '',
  serverType: 'BUILTIN',
  transportType: 'STDIO',
  serverStatus: 'ENABLED',
  riskLevel: 'LOW',
  sortWeight: 100,
  timeoutMs: 10000,
  authRequired: 0,
  builtinServerKey: '',
  endpointUrl: '',
  tagsText: 'builtin,default',
  runtimeConfigJson: '{\n  "timeoutSeconds": 10\n}',
  authConfigJson: '{\n  "enabled": false\n}',
  testPayloadJson: '{\n  "city": "Shanghai"\n}',
  toolPromptHint: '',
  remark: '',
})

const debugForm = reactive({
  requestPayloadJson: '{\n  "city": "Shanghai"\n}',
  sourceType: 'DEBUG',
})

const logQuery = reactive({
  sourceType: '',
  successFlag: '',
})

const filteredServers = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return servers.value.filter((item) => {
    const matchKeyword = !keyword
      || [item.serverCode, item.serverName, item.description, item.builtinServerKey]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    const matchStatus = filters.serverStatus === 'ALL' || item.serverStatus === filters.serverStatus
    const matchPublish = filters.publishStatus === 'ALL' || item.publishStatus === filters.publishStatus
    const matchType = filters.serverType === 'ALL' || item.serverType === filters.serverType
    return matchKeyword && matchStatus && matchPublish && matchType
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

function formatPublishStatus(status?: string | null) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    OFFLINE: '已下线',
  }
  return map[status || ''] || status || '-'
}

function formatServerType(type?: string | null) {
  const map: Record<string, string> = {
    BUILTIN: '内置',
    REMOTE: '远程',
  }
  return map[type || ''] || type || '-'
}

function formatTransportType(type?: string | null) {
  const map: Record<string, string> = {
    STDIO: 'STDIO',
    SSE: 'SSE',
    STREAMABLE_HTTP: '流式 HTTP',
  }
  return map[type || ''] || type || '-'
}

function formatExecuteStatus(status?: string | null) {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    FAILED: '失败',
    TIMEOUT: '超时',
    RUNNING: '执行中',
  }
  return map[status || ''] || status || '-'
}

function resetForm() {
  selectedServerId.value = null
  selectedServer.value = null
  debugResult.value = null
  logs.value = []
  form.serverCode = ''
  form.serverName = ''
  form.description = ''
  form.serverType = 'BUILTIN'
  form.transportType = 'STDIO'
  form.serverStatus = 'ENABLED'
  form.riskLevel = 'LOW'
  form.sortWeight = 100
  form.timeoutMs = 10000
  form.authRequired = 0
  form.builtinServerKey = ''
  form.endpointUrl = ''
  form.tagsText = 'builtin,default'
  form.runtimeConfigJson = '{\n  "timeoutSeconds": 10\n}'
  form.authConfigJson = '{\n  "enabled": false\n}'
  form.testPayloadJson = '{\n  "city": "Shanghai"\n}'
  form.toolPromptHint = ''
  form.remark = ''
  debugForm.requestPayloadJson = '{\n  "city": "Shanghai"\n}'
}

function fillForm(server: McpItem) {
  selectedServerId.value = server.serverId
  selectedServer.value = server
  form.serverCode = server.serverCode
  form.serverName = server.serverName
  form.description = server.description ?? ''
  form.serverType = server.serverType
  form.transportType = server.transportType
  form.serverStatus = server.serverStatus
  form.riskLevel = server.riskLevel
  form.sortWeight = server.sortWeight ?? 100
  form.timeoutMs = server.timeoutMs ?? 10000
  form.authRequired = server.authRequired ?? 0
  form.builtinServerKey = server.builtinServerKey ?? ''
  form.endpointUrl = server.endpointUrl ?? ''
  form.tagsText = (server.tags ?? []).join(',')
  form.runtimeConfigJson = server.runtimeConfigJson || '{}'
  form.authConfigJson = server.authConfigJson || '{}'
  form.testPayloadJson = server.testPayloadJson || '{}'
  form.toolPromptHint = server.toolPromptHint || ''
  form.remark = server.remark ?? ''
  debugForm.requestPayloadJson = server.testPayloadJson || '{}'
}

function applyCatalogItem(item: McpCatalogItem) {
  if (selectedServerId.value) return
  form.serverCode = item.builtinServerKey
  form.serverName = item.serverName
  form.description = item.description ?? ''
  form.serverType = item.serverType
  form.transportType = item.transportType
  form.riskLevel = item.riskLevel
  form.authRequired = item.authRequired ?? 0
  form.builtinServerKey = item.builtinServerKey
  form.tagsText = (item.exposedToolNames ?? []).join(',')
  form.runtimeConfigJson = item.defaultRuntimeConfigJson || '{}'
  form.authConfigJson = item.defaultAuthConfigJson || '{}'
  form.testPayloadJson = item.defaultTestPayloadJson || '{}'
  form.toolPromptHint = item.toolPromptHint || ''
  debugForm.requestPayloadJson = item.defaultTestPayloadJson || '{}'
}

function buildPayload(): McpPayload {
  return {
    serverCode: form.serverCode.trim(),
    serverName: form.serverName.trim(),
    description: form.description.trim() || null,
    serverType: form.serverType,
    transportType: form.transportType,
    serverStatus: form.serverStatus,
    riskLevel: form.riskLevel,
    sortWeight: form.sortWeight,
    timeoutMs: form.timeoutMs,
    authRequired: form.authRequired,
    builtinServerKey: form.builtinServerKey.trim() || null,
    endpointUrl: form.endpointUrl.trim() || null,
    tags: form.tagsText.split(',').map((item) => item.trim()).filter(Boolean),
    runtimeConfigJson: form.runtimeConfigJson.trim() || null,
    authConfigJson: form.authConfigJson.trim() || null,
    testPayloadJson: form.testPayloadJson.trim() || null,
    toolPromptHint: form.toolPromptHint.trim() || null,
    remark: form.remark.trim() || null,
  }
}

async function loadLogs() {
  logs.value = await queryMcpLogs({
    serverId: selectedServerId.value,
    sourceType: logQuery.sourceType || null,
    successFlag: logQuery.successFlag === '' ? null : Number(logQuery.successFlag),
  })
}

async function selectServer(serverId: number) {
  const detail = await fetchMcpServerDetail(serverId)
  fillForm(detail)
  await loadLogs()
}

async function refreshAll(keepSelection = true) {
  loading.value = true
  try {
    const currentId = keepSelection ? selectedServerId.value : null
    const [serverList, statResult, catalogResult] = await Promise.all([
      queryMcpServers(),
      fetchMcpStats(),
      fetchMcpCatalog(),
    ])
    servers.value = serverList
    stats.value = statResult
    catalog.value = catalogResult
    if (currentId) {
      const exists = servers.value.find((item) => item.serverId === currentId)
      if (exists) {
        await selectServer(currentId)
      } else {
        resetForm()
      }
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '加载 MCP 数据失败。'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    if (selectedServerId.value) {
      await updateMcpServer(selectedServerId.value, buildPayload())
      showFeedback('success', 'MCP 服务已更新。')
    } else {
      const created = await createMcpServer(buildPayload())
      showFeedback('success', 'MCP 服务已创建。')
      await refreshAll(false)
      await selectServer(created.serverId)
      return
    }
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '保存 MCP 服务失败。'))
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!selectedServerId.value || !window.confirm('确认删除当前 MCP 服务吗？')) return
  actionLoading.value = true
  try {
    await removeMcpServer(selectedServerId.value)
    showFeedback('success', 'MCP 服务已删除。')
    resetForm()
    await refreshAll(false)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除 MCP 服务失败。'))
  } finally {
    actionLoading.value = false
  }
}

async function handlePublish() {
  if (!selectedServerId.value) return
  actionLoading.value = true
  try {
    await publishMcpServer(selectedServerId.value)
    showFeedback('success', 'MCP 服务已发布。')
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '发布 MCP 服务失败。'))
  } finally {
    actionLoading.value = false
  }
}

async function handleOffline() {
  if (!selectedServerId.value) return
  actionLoading.value = true
  try {
    await offlineMcpServer(selectedServerId.value)
    showFeedback('success', 'MCP 服务已下线。')
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '下线 MCP 服务失败。'))
  } finally {
    actionLoading.value = false
  }
}

async function handleDebug() {
  if (!selectedServerId.value) {
    showFeedback('info', '请先选择或创建一个 MCP 服务。')
    return
  }
  actionLoading.value = true
  try {
    debugResult.value = await debugMcpServer({
      serverId: selectedServerId.value,
      requestPayloadJson: debugForm.requestPayloadJson.trim(),
      sourceType: debugForm.sourceType,
    })
    await loadLogs()
    await refreshAll()
    showFeedback('success', 'MCP 调试已完成。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '调试 MCP 服务失败。'))
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

    <section class="mcp-page">
      <article class="panel-card hero-panel">
        <div class="hero-panel__head">
          <div>
            <p class="section-kicker">MCP 注册中心</p>
            <h2>MCP 服务管理台</h2>
            <p class="hero-panel__summary">统一管理内置与远程 MCP 服务、运行配置、调试记录、发布状态以及面向智能体的服务绑定。</p>
          </div>
          <div class="toolbar-actions">
            <button class="app-button app-button--secondary" type="button" @click="resetForm">新建服务</button>
            <button class="app-button" type="button" :disabled="saving" @click="handleSave">
              {{ saving ? '保存中...' : '保存服务' }}
            </button>
          </div>
        </div>

        <div class="stats-strip">
          <div class="metric-card"><span>服务总数</span><strong>{{ stats.totalCount }}</strong></div>
          <div class="metric-card"><span>启用数量</span><strong>{{ stats.enabledCount }}</strong></div>
          <div class="metric-card"><span>已发布</span><strong>{{ stats.publishedCount }}</strong></div>
          <div class="metric-card"><span>远程服务</span><strong>{{ stats.remoteCount }}</strong></div>
          <div class="metric-card"><span>日志总数</span><strong>{{ stats.totalLogCount }}</strong></div>
        </div>
      </article>

      <div class="mcp-layout">
        <article class="panel-card section-panel section-panel--sidebar">
          <div class="section-panel__head">
            <div>
              <h3>服务目录</h3>
              <p>按状态、发布情况和服务类型筛选。</p>
            </div>
          </div>

          <div class="filter-grid">
            <input v-model="filters.keyword" class="app-input" type="text" placeholder="搜索编码、名称、内置标识" />
            <select v-model="filters.serverStatus" class="app-select">
              <option value="ALL">全部状态</option>
              <option value="ENABLED">启用</option>
              <option value="DISABLED">停用</option>
            </select>
            <select v-model="filters.publishStatus" class="app-select">
              <option value="ALL">全部发布状态</option>
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">已发布</option>
              <option value="OFFLINE">已下线</option>
            </select>
            <select v-model="filters.serverType" class="app-select">
              <option value="ALL">全部类型</option>
              <option value="BUILTIN">内置</option>
              <option value="REMOTE">远程</option>
            </select>
          </div>

          <div class="catalog-strip">
            <button
              v-for="item in catalog"
              :key="item.builtinServerKey"
              class="catalog-chip"
              type="button"
              @click="applyCatalogItem(item)"
            >
              {{ item.serverName }}
            </button>
          </div>

          <div v-if="loading" class="empty-state">加载中...</div>
          <div v-else-if="filteredServers.length === 0" class="empty-state">暂无 MCP 服务数据。</div>
          <div v-else class="server-list">
            <button
              v-for="item in filteredServers"
              :key="item.serverId"
              class="server-list__item"
              :class="{ 'server-list__item--active': item.serverId === selectedServerId }"
              type="button"
              @click="selectServer(item.serverId)"
            >
              <strong>{{ item.serverName }}</strong>
              <span>{{ item.serverCode }}</span>
              <small>{{ formatServerType(item.serverType) }} / {{ formatPublishStatus(item.publishStatus) }} / {{ formatTransportType(item.transportType) }}</small>
            </button>
          </div>
        </article>

        <article class="panel-card section-panel">
          <div class="section-panel__head">
            <div>
              <h3>服务配置</h3>
              <p>{{ selectedServer ? `${selectedServer.serverName} / ${selectedServer.serverCode}` : '创建新的 MCP 服务定义' }}</p>
            </div>
            <div class="toolbar-actions">
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedServerId" @click="handlePublish">发布</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedServerId" @click="handleOffline">下线</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedServerId || actionLoading" @click="handleDelete">删除</button>
            </div>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">服务编码</span><input v-model="form.serverCode" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">服务名称</span><input v-model="form.serverName" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">服务类型</span><select v-model="form.serverType" class="app-select"><option value="BUILTIN">内置</option><option value="REMOTE">远程</option></select></label>
            <label class="field"><span class="field__label">传输方式</span><select v-model="form.transportType" class="app-select"><option value="STDIO">STDIO</option><option value="SSE">SSE</option><option value="STREAMABLE_HTTP">流式 HTTP</option></select></label>
            <label class="field"><span class="field__label">服务状态</span><select v-model="form.serverStatus" class="app-select"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label>
            <label class="field"><span class="field__label">风险等级</span><select v-model="form.riskLevel" class="app-select"><option value="LOW">低</option><option value="MEDIUM">中</option><option value="HIGH">高</option></select></label>
            <label class="field"><span class="field__label">超时时间（毫秒）</span><input v-model.number="form.timeoutMs" class="app-input" type="number" /></label>
            <label class="field"><span class="field__label">排序权重</span><input v-model.number="form.sortWeight" class="app-input" type="number" /></label>
            <label class="field"><span class="field__label">内置标识</span><input v-model="form.builtinServerKey" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">接口地址</span><input v-model="form.endpointUrl" class="app-input" type="text" /></label>
          </div>

          <label class="field"><span class="field__label">服务描述</span><textarea v-model="form.description" class="app-textarea" rows="3" /></label>
          <label class="field"><span class="field__label">标签</span><input v-model="form.tagsText" class="app-input" type="text" placeholder="database, weather, builtin" /></label>
          <label class="field"><span class="field__label">工具提示词说明</span><textarea v-model="form.toolPromptHint" class="app-textarea" rows="4" placeholder="描述工具能力边界、限制条件和推荐使用方式。" /></label>
          <label class="field field--inline"><span class="field__label">需要鉴权</span><input v-model="form.authRequired" type="checkbox" :true-value="1" :false-value="0" /></label>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">运行配置 JSON</span><textarea v-model="form.runtimeConfigJson" class="app-textarea code-area" rows="9" /></label>
            <label class="field"><span class="field__label">鉴权配置 JSON</span><textarea v-model="form.authConfigJson" class="app-textarea code-area" rows="9" /></label>
            <label class="field"><span class="field__label">测试请求 JSON</span><textarea v-model="form.testPayloadJson" class="app-textarea code-area" rows="9" /></label>
            <div class="field field--meta">
              <span class="field__label">运行摘要</span>
              <strong>{{ selectedServer ? `日志 ${selectedServer.logCount ?? 0} 条 / ${formatPublishStatus(selectedServer.publishStatus)}` : '尚未选择服务' }}</strong>
            </div>
          </div>

          <label class="field"><span class="field__label">备注</span><input v-model="form.remark" class="app-input" type="text" /></label>

          <div class="section-panel__sub">
            <div>
              <h4>在线调试</h4>
              <p>执行一次调试调用，并保存请求与响应日志。</p>
            </div>
            <button class="app-button app-button--secondary" type="button" :disabled="actionLoading" @click="handleDebug">执行调试</button>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">调试来源</span><select v-model="debugForm.sourceType" class="app-select"><option value="DEBUG">调试</option><option value="RUNTIME">运行时</option></select></label>
            <div class="field field--meta">
              <span class="field__label">挂载能力</span>
              <strong>{{ selectedServer?.builtinServerKey || (selectedServer?.serverType ? formatServerType(selectedServer.serverType) : '待选择服务') }}</strong>
            </div>
          </div>
          <label class="field"><span class="field__label">调试请求 JSON</span><textarea v-model="debugForm.requestPayloadJson" class="app-textarea code-area" rows="8" /></label>
          <pre v-if="debugResult" class="result-box">{{ JSON.stringify(debugResult, null, 2) }}</pre>

          <div class="section-panel__sub">
            <div>
              <h4>执行日志</h4>
              <p>查看调试与运行时调用结果。</p>
            </div>
            <button class="app-button app-button--secondary" type="button" @click="loadLogs">刷新日志</button>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">来源类型</span><input v-model="logQuery.sourceType" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">执行结果</span><select v-model="logQuery.successFlag" class="app-select"><option value="">全部</option><option value="1">成功</option><option value="0">失败</option></select></label>
          </div>

          <div v-if="logs.length === 0" class="empty-state empty-state--compact">暂无日志。</div>
          <div v-else class="log-list">
            <article v-for="item in logs" :key="item.logId" class="log-card">
              <div class="log-card__head">
                <strong>{{ item.toolName || item.serverName || item.serverCode }}</strong>
                <span>{{ formatExecuteStatus(item.executeStatus) }} / {{ formatTime(item.createTime) }}</span>
              </div>
              <p>{{ item.failureReason || '执行成功。' }}</p>
              <pre class="result-box">{{ item.responsePayloadJson || item.requestPayloadJson || '{}' }}</pre>
            </article>
          </div>
        </article>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.mcp-page { display: grid; gap: var(--layout-gap); min-width: 0; min-height: 100%; }
.hero-panel, .section-panel, .metric-card, .catalog-chip, .server-list__item, .log-card, .empty-state, .result-box { border: 1px solid rgba(255,255,255,.08); }
.hero-panel, .section-panel {
  padding: var(--panel-padding);
  border-radius: var(--panel-radius);
  background: linear-gradient(180deg, rgba(255,255,255,.034), rgba(255,255,255,.012)), rgba(7,14,26,.82);
  box-shadow: 0 24px 56px rgba(0,0,0,.22);
  min-width: 0;
  overflow: visible;
}
.hero-panel__head, .section-panel__head, .section-panel__sub, .toolbar-actions { display: flex; justify-content: space-between; gap: 14px; align-items: flex-start; flex-wrap: wrap; min-width: 0; }
.hero-panel__head h2, .section-panel__head h3, .section-panel__sub h4 { margin: 0; color: var(--color-ink-strong); line-height: 1.2; padding-block: 2px; }
.hero-panel__summary, .section-panel__head p, .section-panel__sub p { color: var(--color-ink-soft); line-height: 1.7; }
.stats-strip { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-top: 22px; }
.metric-card { padding: 18px 20px; border-radius: 18px; background: linear-gradient(180deg, rgba(255,255,255,.045), rgba(255,255,255,.02)); }
.metric-card span { color: var(--color-ink-soft); line-height: 1.45; }
.metric-card strong { display: block; margin-top: 10px; color: var(--color-ink-strong); font-size: 1.6rem; line-height: 1.2; }
.mcp-layout { display: grid; grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.18fr); gap: var(--layout-gap); align-items: start; min-width: 0; }
.section-panel--sidebar { display: grid; gap: 16px; align-content: start; min-width: 0; }
.filter-grid, .form-grid { display: grid; gap: 14px; }
.form-grid--double { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.catalog-strip { display: flex; flex-wrap: wrap; gap: 10px; }
.catalog-chip, .server-list__item {
  border-radius: 16px;
  background: rgba(255,255,255,.03);
  color: var(--color-ink-soft);
}
.catalog-chip { padding: 10px 14px; cursor: pointer; }
.server-list { display: grid; gap: 10px; overflow: auto; min-height: 0; padding-right: 4px; scrollbar-gutter: stable; }
.server-list__item { display: grid; gap: 6px; padding: 14px 16px; text-align: left; cursor: pointer; }
.server-list__item strong, .field__label, .field--meta strong, .log-card__head strong { color: var(--color-ink-strong); line-height: 1.35; }
.server-list__item--active { background: rgba(76,162,255,.08); border-color: rgba(108,201,255,.22); }
.server-list__item span, .server-list__item small, .field--meta, .log-card__head span, .log-card p { color: var(--color-ink-soft); line-height: 1.5; }
.field { display: grid; gap: 8px; }
.field--inline { grid-template-columns: auto auto; justify-content: flex-start; align-items: center; gap: 12px; }
.field--meta { align-content: center; min-height: 52px; }
.app-input, .app-select, .app-textarea {
  width: 100%;
  border: 1px solid rgba(147,177,233,.28);
  border-radius: 14px;
  background: rgba(8,16,30,.96);
  line-height: 1.45;
}
.app-input, .app-select { min-height: 54px; padding: 0 14px; }
.app-textarea { min-height: 120px; padding: 14px; line-height: 1.6; }
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
.log-card { padding: var(--compact-panel-padding); border-radius: 18px; background: rgba(255,255,255,.018); min-width: 0; }
.log-card__head { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.section-panel,
.server-list__item,
.catalog-chip,
.log-card {
  overflow: visible;
}
.section-panel__head > div:first-child,
.hero-panel__head > div:first-child,
.section-panel__sub > div:first-child,
.log-card__head > strong {
  min-width: 0;
  flex: 1 1 320px;
}
.hero-panel__head,
.section-panel__head,
.section-panel__sub {
  min-height: 60px;
}
.toolbar-actions {
  flex: 1 1 320px;
  justify-content: flex-end;
  align-self: stretch;
}
.toolbar-actions > * {
  flex: 1 1 140px;
}
.field--inline input {
  width: auto;
  min-height: auto;
}
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
@media (max-width: 1320px) {
  .mcp-layout, .form-grid--double { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .hero-panel, .section-panel { padding: 20px; border-radius: 20px; }
  .hero-panel__head, .section-panel__head, .section-panel__sub, .toolbar-actions { flex-direction: column; align-items: stretch; }
  .stats-strip { grid-template-columns: 1fr; }
}
</style>
