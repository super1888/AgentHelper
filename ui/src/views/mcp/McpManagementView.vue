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
  if (!value) return 'Not recorded'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value)
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
    showFeedback('error', getErrorMessage(error, 'Failed to load MCP data.'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    if (selectedServerId.value) {
      await updateMcpServer(selectedServerId.value, buildPayload())
      showFeedback('success', 'MCP server updated.')
    } else {
      const created = await createMcpServer(buildPayload())
      showFeedback('success', 'MCP server created.')
      await refreshAll(false)
      await selectServer(created.serverId)
      return
    }
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Failed to save MCP server.'))
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!selectedServerId.value || !window.confirm('Delete the current MCP server?')) return
  actionLoading.value = true
  try {
    await removeMcpServer(selectedServerId.value)
    showFeedback('success', 'MCP server deleted.')
    resetForm()
    await refreshAll(false)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Failed to delete MCP server.'))
  } finally {
    actionLoading.value = false
  }
}

async function handlePublish() {
  if (!selectedServerId.value) return
  actionLoading.value = true
  try {
    await publishMcpServer(selectedServerId.value)
    showFeedback('success', 'MCP server published.')
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Failed to publish MCP server.'))
  } finally {
    actionLoading.value = false
  }
}

async function handleOffline() {
  if (!selectedServerId.value) return
  actionLoading.value = true
  try {
    await offlineMcpServer(selectedServerId.value)
    showFeedback('success', 'MCP server offline.')
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Failed to offline MCP server.'))
  } finally {
    actionLoading.value = false
  }
}

async function handleDebug() {
  if (!selectedServerId.value) {
    showFeedback('info', 'Select or create an MCP server first.')
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
    showFeedback('success', 'MCP debug finished.')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Failed to debug MCP server.'))
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
            <p class="section-kicker">MCP Registry</p>
            <h2>MCP Service Console</h2>
            <p class="hero-panel__summary">Manage builtin and remote MCP servers, runtime configuration, debug records, publish state, and agent-ready service bindings.</p>
          </div>
          <div class="toolbar-actions">
            <button class="app-button app-button--secondary" type="button" @click="resetForm">New Server</button>
            <button class="app-button" type="button" :disabled="saving" @click="handleSave">
              {{ saving ? 'Saving...' : 'Save Server' }}
            </button>
          </div>
        </div>

        <div class="stats-strip">
          <div class="metric-card"><span>Total</span><strong>{{ stats.totalCount }}</strong></div>
          <div class="metric-card"><span>Enabled</span><strong>{{ stats.enabledCount }}</strong></div>
          <div class="metric-card"><span>Published</span><strong>{{ stats.publishedCount }}</strong></div>
          <div class="metric-card"><span>Remote</span><strong>{{ stats.remoteCount }}</strong></div>
          <div class="metric-card"><span>Logs</span><strong>{{ stats.totalLogCount }}</strong></div>
        </div>
      </article>

      <div class="mcp-layout">
        <article class="panel-card section-panel section-panel--sidebar">
          <div class="section-panel__head">
            <div>
              <h3>Server Catalog</h3>
              <p>Filter by status, publish state, and service type.</p>
            </div>
          </div>

          <div class="filter-grid">
            <input v-model="filters.keyword" class="app-input" type="text" placeholder="Search code, name, builtin key" />
            <select v-model="filters.serverStatus" class="app-select">
              <option value="ALL">All status</option>
              <option value="ENABLED">ENABLED</option>
              <option value="DISABLED">DISABLED</option>
            </select>
            <select v-model="filters.publishStatus" class="app-select">
              <option value="ALL">All publish state</option>
              <option value="DRAFT">DRAFT</option>
              <option value="PUBLISHED">PUBLISHED</option>
              <option value="OFFLINE">OFFLINE</option>
            </select>
            <select v-model="filters.serverType" class="app-select">
              <option value="ALL">All types</option>
              <option value="BUILTIN">BUILTIN</option>
              <option value="REMOTE">REMOTE</option>
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

          <div v-if="loading" class="empty-state">Loading...</div>
          <div v-else-if="filteredServers.length === 0" class="empty-state">No MCP server data.</div>
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
              <small>{{ item.serverType }} / {{ item.publishStatus }} / {{ item.transportType }}</small>
            </button>
          </div>
        </article>

        <article class="panel-card section-panel">
          <div class="section-panel__head">
            <div>
              <h3>Server Config</h3>
              <p>{{ selectedServer ? `${selectedServer.serverName} / ${selectedServer.serverCode}` : 'Create a new MCP server definition' }}</p>
            </div>
            <div class="toolbar-actions">
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedServerId" @click="handlePublish">Publish</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedServerId" @click="handleOffline">Offline</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedServerId || actionLoading" @click="handleDelete">Delete</button>
            </div>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">Server Code</span><input v-model="form.serverCode" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">Server Name</span><input v-model="form.serverName" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">Server Type</span><select v-model="form.serverType" class="app-select"><option>BUILTIN</option><option>REMOTE</option></select></label>
            <label class="field"><span class="field__label">Transport</span><select v-model="form.transportType" class="app-select"><option>STDIO</option><option>SSE</option><option>STREAMABLE_HTTP</option></select></label>
            <label class="field"><span class="field__label">Server Status</span><select v-model="form.serverStatus" class="app-select"><option>ENABLED</option><option>DISABLED</option></select></label>
            <label class="field"><span class="field__label">Risk Level</span><select v-model="form.riskLevel" class="app-select"><option>LOW</option><option>MEDIUM</option><option>HIGH</option></select></label>
            <label class="field"><span class="field__label">Timeout Ms</span><input v-model.number="form.timeoutMs" class="app-input" type="number" /></label>
            <label class="field"><span class="field__label">Sort Weight</span><input v-model.number="form.sortWeight" class="app-input" type="number" /></label>
            <label class="field"><span class="field__label">Builtin Key</span><input v-model="form.builtinServerKey" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">Endpoint URL</span><input v-model="form.endpointUrl" class="app-input" type="text" /></label>
          </div>

          <label class="field"><span class="field__label">Description</span><textarea v-model="form.description" class="app-textarea" rows="3" /></label>
          <label class="field"><span class="field__label">Tags</span><input v-model="form.tagsText" class="app-input" type="text" placeholder="database, weather, builtin" /></label>
          <label class="field"><span class="field__label">Tool Prompt Hint</span><textarea v-model="form.toolPromptHint" class="app-textarea" rows="4" placeholder="Describe tool scope, guardrails, and intended usage." /></label>
          <label class="field field--inline"><span class="field__label">Auth Required</span><input v-model="form.authRequired" type="checkbox" :true-value="1" :false-value="0" /></label>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">Runtime Config JSON</span><textarea v-model="form.runtimeConfigJson" class="app-textarea code-area" rows="9" /></label>
            <label class="field"><span class="field__label">Auth Config JSON</span><textarea v-model="form.authConfigJson" class="app-textarea code-area" rows="9" /></label>
            <label class="field"><span class="field__label">Test Payload JSON</span><textarea v-model="form.testPayloadJson" class="app-textarea code-area" rows="9" /></label>
            <div class="field field--meta">
              <span class="field__label">Runtime Summary</span>
              <strong>{{ selectedServer ? `logs ${selectedServer.logCount ?? 0} / ${selectedServer.publishStatus}` : 'No selection' }}</strong>
            </div>
          </div>

          <label class="field"><span class="field__label">Remark</span><input v-model="form.remark" class="app-input" type="text" /></label>

          <div class="section-panel__sub">
            <div>
              <h4>Online Debug</h4>
              <p>Execute one debug call and persist request/response logs.</p>
            </div>
            <button class="app-button app-button--secondary" type="button" :disabled="actionLoading" @click="handleDebug">Run Debug</button>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">Debug Source</span><select v-model="debugForm.sourceType" class="app-select"><option>DEBUG</option><option>RUNTIME</option></select></label>
            <div class="field field--meta">
              <span class="field__label">Mounted Capability</span>
              <strong>{{ selectedServer?.builtinServerKey || selectedServer?.serverType || 'Pending selection' }}</strong>
            </div>
          </div>
          <label class="field"><span class="field__label">Debug Request JSON</span><textarea v-model="debugForm.requestPayloadJson" class="app-textarea code-area" rows="8" /></label>
          <pre v-if="debugResult" class="result-box">{{ JSON.stringify(debugResult, null, 2) }}</pre>

          <div class="section-panel__sub">
            <div>
              <h4>Execution Logs</h4>
              <p>Inspect debug and runtime invocation results.</p>
            </div>
            <button class="app-button app-button--secondary" type="button" @click="loadLogs">Refresh Logs</button>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">Source Type</span><input v-model="logQuery.sourceType" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">Success Flag</span><select v-model="logQuery.successFlag" class="app-select"><option value="">All</option><option value="1">Success</option><option value="0">Failed</option></select></label>
          </div>

          <div v-if="logs.length === 0" class="empty-state empty-state--compact">No logs yet.</div>
          <div v-else class="log-list">
            <article v-for="item in logs" :key="item.logId" class="log-card">
              <div class="log-card__head">
                <strong>{{ item.toolName || item.serverName || item.serverCode }}</strong>
                <span>{{ item.executeStatus }} / {{ formatTime(item.createTime) }}</span>
              </div>
              <p>{{ item.failureReason || 'Execution succeeded.' }}</p>
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
