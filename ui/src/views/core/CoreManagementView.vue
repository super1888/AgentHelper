<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Cpu, KeyRound, RefreshCw, Server, Sparkles } from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import { queryProviderCatalog, queryModelConnections, removeModelConnection, saveModelConnection, testModelConnection } from '@/api/core'
import type { ModelConnectionItem, ModelConnectionPayload, ProviderCatalogItem } from '@/types/core'
import { getErrorMessage } from '@/utils/errors'

type ConnectionMode = 'create' | 'edit'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const feedback = ref('')
const feedbackTone = ref<'success' | 'error' | 'info'>('info')
const providerCatalog = ref<ProviderCatalogItem[]>([])
const connections = ref<ModelConnectionItem[]>([])
const selectedModelCode = ref('')
const connectionMode = ref<ConnectionMode>('create')
const testPrompt = ref('请只回复：MODEL_OK')
const testResult = ref('')
const baselineTestFingerprint = ref('')
const lastPassedTestFingerprint = ref('')

const connectionForm = reactive({
  modelCode: '',
  connectionName: '',
  providerEnum: '',
  baseUrl: '',
  apiKey: '',
  organizationId: '',
  defaultHeadersJson: '',
  modelType: 'CHAT',
  modelIdentifier: '',
  temperature: 0.7,
  topP: 0.9,
  presencePenalty: 0,
  frequencyPenalty: 0,
  maxTokens: 4096,
  contextWindow: 128000,
  rpmLimit: 60,
  tpmLimit: 120000,
  timeoutMs: 60000,
  supportStreaming: true,
  supportTools: true,
  supportVision: false,
  supportJsonSchema: true,
  defaultModel: false,
  advancedConfigJson: '',
  remark: '',
  status: 'ENABLED',
})

const selectedConnection = computed(() => connections.value.find((item) => item.modelCode === selectedModelCode.value) ?? null)
const connectionStats = computed(() => ({
  total: connections.value.length,
  enabled: connections.value.filter((item) => item.status === 'ENABLED').length,
  keyReady: connections.value.filter((item) => item.apiKeyConfigured).length,
  defaultCount: connections.value.filter((item) => item.defaultModel).length,
}))

function buildTestPayload() {
  return {
    modelCode: connectionForm.modelCode || undefined,
    providerEnum: connectionForm.providerEnum,
    baseUrl: connectionForm.baseUrl.trim() || null,
    apiKey: connectionForm.apiKey.trim() || null,
    modelIdentifier: connectionForm.modelIdentifier.trim(),
    temperature: connectionForm.temperature,
    topP: connectionForm.topP,
    presencePenalty: connectionForm.presencePenalty,
    frequencyPenalty: connectionForm.frequencyPenalty,
    maxTokens: connectionForm.maxTokens,
    testPrompt: testPrompt.value.trim() || null,
  }
}

function buildConnectionFingerprint(payload: {
  modelCode?: string
  providerEnum: string
  baseUrl: string | null
  apiKey: string | null
  modelIdentifier: string
  temperature: number
  topP: number
  presencePenalty: number
  frequencyPenalty: number
  maxTokens: number
}) {
  return JSON.stringify({
    modelCode: payload.modelCode,
    providerEnum: payload.providerEnum,
    baseUrl: payload.baseUrl,
    apiKey: payload.apiKey,
    modelIdentifier: payload.modelIdentifier,
    temperature: payload.temperature,
    topP: payload.topP,
    presencePenalty: payload.presencePenalty,
    frequencyPenalty: payload.frequencyPenalty,
    maxTokens: payload.maxTokens,
  })
}

function buildStoredTestFingerprint(item: ModelConnectionItem) {
  return buildConnectionFingerprint({
    modelCode: item.modelCode,
    providerEnum: item.providerEnum,
    baseUrl: item.baseUrl ?? null,
    apiKey: null,
    modelIdentifier: item.modelIdentifier,
    temperature: item.temperature ?? 0.7,
    topP: item.topP ?? 0.9,
    presencePenalty: item.presencePenalty ?? 0,
    frequencyPenalty: item.frequencyPenalty ?? 0,
    maxTokens: item.maxTokens ?? 4096,
  })
}

const currentTestFingerprint = computed(() => buildConnectionFingerprint({
  modelCode: connectionForm.modelCode || undefined,
  providerEnum: connectionForm.providerEnum,
  baseUrl: connectionForm.baseUrl.trim() || null,
  apiKey: connectionForm.apiKey.trim() || null,
  modelIdentifier: connectionForm.modelIdentifier.trim(),
  temperature: connectionForm.temperature,
  topP: connectionForm.topP,
  presencePenalty: connectionForm.presencePenalty,
  frequencyPenalty: connectionForm.frequencyPenalty,
  maxTokens: connectionForm.maxTokens,
}))
const requiresRetestBeforeSave = computed(() => {
  if (connectionMode.value === 'create') {
    return true
  }
  return currentTestFingerprint.value !== baselineTestFingerprint.value
})
const canSaveCurrentConnection = computed(() => {
  if (!requiresRetestBeforeSave.value) {
    return true
  }
  return currentTestFingerprint.value === lastPassedTestFingerprint.value
})

function setFeedback(tone: 'success' | 'error' | 'info', message: string) {
  feedbackTone.value = tone
  feedback.value = message
}

function resetConnectionForm() {
  connectionMode.value = 'create'
  connectionForm.modelCode = ''
  connectionForm.connectionName = ''
  connectionForm.providerEnum = providerCatalog.value[0]?.providerEnum ?? ''
  connectionForm.baseUrl = ''
  connectionForm.apiKey = ''
  connectionForm.organizationId = ''
  connectionForm.defaultHeadersJson = ''
  connectionForm.modelType = 'CHAT'
  connectionForm.modelIdentifier = ''
  connectionForm.temperature = 0.7
  connectionForm.topP = 0.9
  connectionForm.presencePenalty = 0
  connectionForm.frequencyPenalty = 0
  connectionForm.maxTokens = 4096
  connectionForm.contextWindow = 128000
  connectionForm.rpmLimit = 60
  connectionForm.tpmLimit = 120000
  connectionForm.timeoutMs = 60000
  connectionForm.supportStreaming = true
  connectionForm.supportTools = true
  connectionForm.supportVision = false
  connectionForm.supportJsonSchema = true
  connectionForm.defaultModel = false
  connectionForm.advancedConfigJson = ''
  connectionForm.remark = ''
  connectionForm.status = 'ENABLED'
  testResult.value = ''
  baselineTestFingerprint.value = ''
  lastPassedTestFingerprint.value = ''
}

function fillConnectionForm(item: ModelConnectionItem) {
  connectionMode.value = 'edit'
  selectedModelCode.value = item.modelCode
  connectionForm.modelCode = item.modelCode
  connectionForm.connectionName = item.connectionName
  connectionForm.providerEnum = item.providerEnum
  connectionForm.baseUrl = item.baseUrl ?? ''
  connectionForm.apiKey = ''
  connectionForm.organizationId = item.organizationId ?? ''
  connectionForm.defaultHeadersJson = item.defaultHeadersJson ?? ''
  connectionForm.modelType = item.modelType
  connectionForm.modelIdentifier = item.modelIdentifier
  connectionForm.temperature = item.temperature ?? 0.7
  connectionForm.topP = item.topP ?? 0.9
  connectionForm.presencePenalty = item.presencePenalty ?? 0
  connectionForm.frequencyPenalty = item.frequencyPenalty ?? 0
  connectionForm.maxTokens = item.maxTokens ?? 4096
  connectionForm.contextWindow = item.contextWindow ?? 128000
  connectionForm.rpmLimit = item.rpmLimit ?? 60
  connectionForm.tpmLimit = item.tpmLimit ?? 120000
  connectionForm.timeoutMs = item.timeoutMs ?? 60000
  connectionForm.supportStreaming = item.supportStreaming
  connectionForm.supportTools = item.supportTools
  connectionForm.supportVision = item.supportVision
  connectionForm.supportJsonSchema = item.supportJsonSchema
  connectionForm.defaultModel = item.defaultModel
  connectionForm.advancedConfigJson = item.advancedConfigJson ?? ''
  connectionForm.remark = item.remark ?? ''
  connectionForm.status = item.status
  testResult.value = ''
  baselineTestFingerprint.value = buildStoredTestFingerprint(item)
  lastPassedTestFingerprint.value = ''
}

function buildConnectionPayload(): ModelConnectionPayload {
  return {
    modelCode: connectionForm.modelCode || undefined,
    connectionName: connectionForm.connectionName.trim(),
    providerEnum: connectionForm.providerEnum,
    baseUrl: connectionForm.baseUrl.trim() || null,
    apiKey: connectionForm.apiKey.trim() || null,
    organizationId: connectionForm.organizationId.trim() || null,
    defaultHeadersJson: connectionForm.defaultHeadersJson.trim() || null,
    modelType: connectionForm.modelType,
    modelIdentifier: connectionForm.modelIdentifier.trim(),
    temperature: connectionForm.temperature,
    topP: connectionForm.topP,
    presencePenalty: connectionForm.presencePenalty,
    frequencyPenalty: connectionForm.frequencyPenalty,
    maxTokens: connectionForm.maxTokens,
    contextWindow: connectionForm.contextWindow,
    rpmLimit: connectionForm.rpmLimit,
    tpmLimit: connectionForm.tpmLimit,
    timeoutMs: connectionForm.timeoutMs,
    supportStreaming: connectionForm.supportStreaming,
    supportTools: connectionForm.supportTools,
    supportVision: connectionForm.supportVision,
    supportJsonSchema: connectionForm.supportJsonSchema,
    defaultModel: connectionForm.defaultModel,
    advancedConfigJson: connectionForm.advancedConfigJson.trim() || null,
    status: connectionForm.status,
    remark: connectionForm.remark.trim() || null,
  }
}

async function loadData(successMessage?: string) {
  loading.value = true
  try {
    const [catalog, connectionList] = await Promise.all([queryProviderCatalog(), queryModelConnections()])
    providerCatalog.value = catalog
    connections.value = connectionList
    if (!connectionList.some((item) => item.modelCode === selectedModelCode.value)) {
      selectedModelCode.value = connectionList[0]?.modelCode ?? ''
    }
    if (!connectionForm.providerEnum) {
      resetConnectionForm()
    }
    if (successMessage) setFeedback('success', successMessage)
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '核心配置加载失败。'))
  } finally {
    loading.value = false
  }
}

async function submitConnection() {
  if (!connectionForm.connectionName.trim()) return setFeedback('error', '请输入连接配置名称。')
  if (!connectionForm.providerEnum) return setFeedback('error', '请选择模型提供商。')
  if (!connectionForm.modelIdentifier.trim()) return setFeedback('error', '请输入模型标识。')
  if (!canSaveCurrentConnection.value) return setFeedback('error', '请先用当前参数完成测试，再保存连接配置。')
  saving.value = true
  try {
    const result = await saveModelConnection(buildConnectionPayload())
    selectedModelCode.value = result.modelCode
    baselineTestFingerprint.value = currentTestFingerprint.value
    lastPassedTestFingerprint.value = currentTestFingerprint.value
    if (connectionMode.value === 'create') {
      resetConnectionForm()
    }
    await loadData(`连接配置 ${result.connectionName} 已保存。`)
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '保存连接配置失败。'))
  } finally {
    saving.value = false
  }
}

async function handleDeleteConnection() {
  if (!selectedModelCode.value) return
  try {
    const target = connections.value.find((item) => item.modelCode === selectedModelCode.value)
    await removeModelConnection(selectedModelCode.value)
    selectedModelCode.value = ''
    resetConnectionForm()
    await loadData(`连接配置 ${target?.connectionName || ''} 已删除。`)
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '删除连接配置失败。'))
  }
}

async function handleTestConnection() {
  if (!connectionForm.providerEnum) return setFeedback('error', '请选择模型提供商。')
  if (!connectionForm.modelIdentifier.trim()) return setFeedback('error', '请输入模型标识。')
  if (!connectionForm.apiKey.trim() && !selectedConnection.value?.apiKeyConfigured) {
    return setFeedback('error', '请输入 API Key，或编辑一个已配置密钥的连接。')
  }
  testing.value = true
  try {
    const result = await testModelConnection(buildTestPayload())
    testResult.value = `[${result.elapsedMs}ms] ${result.responseContent}`
    lastPassedTestFingerprint.value = currentTestFingerprint.value
    setFeedback('success', '连接测试成功。')
  } catch (error) {
    lastPassedTestFingerprint.value = ''
    setFeedback('error', getErrorMessage(error, '连接测试失败。'))
  } finally {
    testing.value = false
  }
}

function formatTime(value: number | null) {
  if (!value) return '刚刚更新'
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value)
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <MainShell>
    <AppFeedbackDialog :model-value="Boolean(feedback)" :tone="feedbackTone" :message="feedback" @update:model-value="!$event && (feedback = '')" />
    <section class="core-page">
      <header class="page-hero panel-card">
        <div>
          <p class="section-kicker">Core Config</p>
          <h2>模型连接配置</h2>
          <p class="page-meta">供应商接入信息与模型运行参数合并为一套连接配置，适合先测试再保存。</p>
        </div>
        <button class="app-button app-button--secondary" :disabled="loading" @click="loadData()"><RefreshCw :size="16" />刷新</button>
      </header>

      <div class="stats-grid">
        <article class="stat-card panel-card"><span class="stat-icon"><Server :size="18" /></span><strong>{{ connectionStats.enabled }}</strong><p>启用中的连接配置</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Cpu :size="18" /></span><strong>{{ connectionStats.total }}</strong><p>租户下已保存配置总数</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><KeyRound :size="18" /></span><strong>{{ connectionStats.keyReady }}</strong><p>已完成 API Key 安全托管</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Sparkles :size="18" /></span><strong>{{ connectionStats.defaultCount }}</strong><p>默认模型条目</p></article>
      </div>

      <div class="page-grid">
        <article class="panel-card section-card">
          <div class="section-head">
            <div>
              <strong>{{ connectionMode === 'create' ? '创建模型连接配置' : '编辑模型连接配置' }}</strong>
              <p class="muted">选择供应商与模型，补充 Base URL、密钥和运行参数，测试通过后直接保存。</p>
            </div>
            <div class="action-row">
              <button v-if="connectionMode === 'edit'" class="app-button app-button--ghost" @click="resetConnectionForm">切回创建</button>
              <button v-if="connectionMode === 'edit'" class="app-button app-button--ghost app-button--danger-ghost" @click="handleDeleteConnection">删除</button>
            </div>
          </div>
          <div class="form-grid">
            <label class="field"><span class="field__label">连接名称</span><div class="input-shell"><input v-model="connectionForm.connectionName" class="app-input" type="text" placeholder="例如：DashScope 图像推理" /></div></label>
            <label class="field"><span class="field__label">供应商</span><select v-model="connectionForm.providerEnum" class="app-select"><option v-for="item in providerCatalog" :key="item.providerEnum" :value="item.providerEnum">{{ item.providerLabel }}</option></select></label>
            <label class="field"><span class="field__label">模型类型</span><select v-model="connectionForm.modelType" class="app-select"><option value="CHAT">CHAT</option><option value="EMBEDDING">EMBEDDING</option><option value="RERANK">RERANK</option></select></label>
            <label class="field"><span class="field__label">模型标识</span><div class="input-shell"><input v-model="connectionForm.modelIdentifier" class="app-input" type="text" placeholder="例如：qwen-max" /></div></label>
            <label class="field"><span class="field__label">Base URL</span><div class="input-shell"><input v-model="connectionForm.baseUrl" class="app-input" type="text" placeholder="例如：https://dashscope.aliyuncs.com" /></div></label>
            <label class="field"><span class="field__label">状态</span><select v-model="connectionForm.status" class="app-select"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label>
            <label class="field form-grid__full"><span class="field__label">API Key</span><div class="input-shell"><input v-model="connectionForm.apiKey" class="app-input" type="password" :placeholder="connectionMode === 'create' ? '请输入 API Key' : '留空则保持原密钥不变'" /></div></label>
            <label class="field"><span class="field__label">组织标识</span><div class="input-shell"><input v-model="connectionForm.organizationId" class="app-input" type="text" placeholder="可选" /></div></label>
            <label class="field"><span class="field__label">备注</span><div class="input-shell"><input v-model="connectionForm.remark" class="app-input" type="text" placeholder="例如：视觉问答主路由" /></div></label>
            <label class="field"><span class="field__label">Temperature</span><div class="input-shell"><input v-model.number="connectionForm.temperature" class="app-input" type="number" step="0.1" min="0" max="2" /></div></label>
            <label class="field"><span class="field__label">Top P</span><div class="input-shell"><input v-model.number="connectionForm.topP" class="app-input" type="number" step="0.1" min="0" max="1" /></div></label>
            <label class="field"><span class="field__label">Presence Penalty</span><div class="input-shell"><input v-model.number="connectionForm.presencePenalty" class="app-input" type="number" step="0.1" min="-2" max="2" /></div></label>
            <label class="field"><span class="field__label">Frequency Penalty</span><div class="input-shell"><input v-model.number="connectionForm.frequencyPenalty" class="app-input" type="number" step="0.1" min="-2" max="2" /></div></label>
            <label class="field"><span class="field__label">Max Tokens</span><div class="input-shell"><input v-model.number="connectionForm.maxTokens" class="app-input" type="number" min="1" /></div></label>
            <label class="field"><span class="field__label">Context Window</span><div class="input-shell"><input v-model.number="connectionForm.contextWindow" class="app-input" type="number" min="1" /></div></label>
            <label class="field"><span class="field__label">RPM 限流</span><div class="input-shell"><input v-model.number="connectionForm.rpmLimit" class="app-input" type="number" min="1" /></div></label>
            <label class="field"><span class="field__label">TPM 限流</span><div class="input-shell"><input v-model.number="connectionForm.tpmLimit" class="app-input" type="number" min="1" /></div></label>
            <label class="field"><span class="field__label">超时毫秒</span><div class="input-shell"><input v-model.number="connectionForm.timeoutMs" class="app-input" type="number" min="1000" /></div></label>
            <div class="toggle-grid form-grid__full">
              <label class="toggle-card"><input v-model="connectionForm.supportStreaming" type="checkbox" />支持流式输出</label>
              <label class="toggle-card"><input v-model="connectionForm.supportTools" type="checkbox" />支持工具调用</label>
              <label class="toggle-card"><input v-model="connectionForm.supportVision" type="checkbox" />支持视觉输入</label>
              <label class="toggle-card"><input v-model="connectionForm.supportJsonSchema" type="checkbox" />支持 JSON Schema</label>
              <label class="toggle-card"><input v-model="connectionForm.defaultModel" type="checkbox" />设为默认模型</label>
            </div>
            <label class="field form-grid__full"><span class="field__label">默认请求头 JSON</span><div class="input-shell input-shell--textarea"><textarea v-model="connectionForm.defaultHeadersJson" class="app-textarea code-area" rows="4" placeholder='例如：{"x-tenant":"prod"}' /></div></label>
            <label class="field form-grid__full"><span class="field__label">高级参数 JSON</span><div class="input-shell input-shell--textarea"><textarea v-model="connectionForm.advancedConfigJson" class="app-textarea code-area" rows="5" placeholder='例如：{"stop":["Observation:"]}' /></div></label>
          </div>
          <div class="action-row">
            <button class="app-button full-width" :disabled="saving || !canSaveCurrentConnection" @click="submitConnection">{{ saving ? '提交中...' : '保存连接配置' }}</button>
            <button class="app-button app-button--secondary full-width" :disabled="testing || !connectionForm.providerEnum || !connectionForm.modelIdentifier.trim()" @click="handleTestConnection">{{ testing ? '测试中...' : '测试连接' }}</button>
          </div>
          <p v-if="requiresRetestBeforeSave" class="muted save-hint">当前连通性参数有改动，请先测试通过再保存。</p>
          <pre v-if="testResult" class="code-panel">{{ testResult }}</pre>
        </article>

        <article class="panel-card section-card">
          <div class="section-head"><div><strong>连接配置列表</strong><p class="muted">每条记录都包含供应商接入信息和模型运行参数，适合直接测试后保存。</p></div></div>
          <div v-if="loading" class="empty">正在加载连接配置...</div>
          <div v-else-if="connections.length === 0" class="empty">当前租户还没有模型连接配置。</div>
          <div v-else class="stack">
            <button v-for="item in connections" :key="item.modelCode" class="list-item" :class="{ 'list-item--active': selectedModelCode === item.modelCode }" @click="fillConnectionForm(item)">
              <div class="list-item__head"><strong>{{ item.connectionName }}</strong><span class="status-pill" :class="item.status === 'ENABLED' ? 'status-pill--enabled' : 'status-pill--disabled'">{{ item.status === 'ENABLED' ? '启用' : '停用' }}</span></div>
              <p class="muted">{{ item.providerEnum }} · {{ item.modelIdentifier }}</p>
              <div class="chip-row"><span class="chip">{{ item.modelType }}</span><span v-if="item.defaultModel" class="chip chip--accent">默认</span><span v-if="item.supportVision" class="chip">Vision</span><span v-if="item.supportTools" class="chip">Tools</span><span v-if="item.supportStreaming" class="chip">Stream</span></div>
              <small class="muted">API Key：{{ item.apiKeyMasked || '未配置' }}</small>
              <small class="muted">更新于 {{ formatTime(item.updateTime) }}</small>
            </button>
          </div>
        </article>
      </div>

      <div class="page-grid">
        <article class="panel-card section-card">
          <div class="section-head"><div><strong>当前连接详情</strong><p class="muted">用于核对接入地址、密钥托管状态与模型能力开关。</p></div></div>
          <div v-if="!selectedConnection" class="empty">选择一个连接配置后查看详情。</div>
          <div v-else class="detail-block">
            <strong>{{ selectedConnection.connectionName }}</strong>
            <p class="muted">供应商：{{ selectedConnection.providerEnum }}</p>
            <p class="muted">模型标识：{{ selectedConnection.modelIdentifier }}</p>
            <p class="muted">Base URL：{{ selectedConnection.baseUrl || '使用供应商默认地址' }}</p>
            <p class="muted">组织标识：{{ selectedConnection.organizationId || '未设置' }}</p>
            <p class="muted">API Key：{{ selectedConnection.apiKeyMasked || '未配置' }}</p>
            <p class="muted">温度 / Top P：{{ selectedConnection.temperature ?? '-' }} / {{ selectedConnection.topP ?? '-' }}</p>
            <p class="muted">超时 / 上下文：{{ selectedConnection.timeoutMs ?? '-' }} ms / {{ selectedConnection.contextWindow ?? '-' }}</p>
            <div class="chip-row"><span class="chip">{{ selectedConnection.modelType }}</span><span v-if="selectedConnection.supportStreaming" class="chip">Streaming</span><span v-if="selectedConnection.supportTools" class="chip">Tool Calling</span><span v-if="selectedConnection.supportVision" class="chip">Vision</span><span v-if="selectedConnection.supportJsonSchema" class="chip">JSON Schema</span></div>
            <pre class="code-panel">{{ selectedConnection.defaultHeadersJson || '{}' }}</pre>
            <pre class="code-panel">{{ selectedConnection.advancedConfigJson || '{}' }}</pre>
          </div>
        </article>

        <article class="panel-card section-card">
          <div class="section-head"><div><strong>测试提示词</strong><p class="muted">连接测试会直接使用当前表单中的最新供应商与模型参数。</p></div></div>
          <label class="field">
            <span class="field__label">测试 Prompt</span>
            <div class="input-shell input-shell--textarea"><textarea v-model="testPrompt" class="app-textarea code-area" rows="8" placeholder="输入用于连通性验证的提示词" /></div>
          </label>
          <pre v-if="testResult" class="code-panel">{{ testResult }}</pre>
          <div v-else class="empty">执行测试后会在这里显示返回结果。</div>
        </article>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.core-page { display: grid; gap: var(--layout-gap); min-width: 0; }
.page-hero, .section-head, .list-item__head, .action-row { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
.page-hero { align-items: flex-start; padding: var(--panel-padding); min-width: 0; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; }
.page-grid { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr); gap: 16px; align-items: start; }
.section-card, .panel-card, .stat-card, .list-item { min-width: 0; }
.section-card { padding: var(--panel-padding); display: grid; gap: 16px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.form-grid__full { grid-column: 1 / -1; }
.field { display: grid; gap: 8px; min-width: 0; }
.field__label { font-size: 12px; color: var(--text-muted); }
.input-shell { border: 1px solid var(--panel-border); border-radius: 14px; background: rgba(15, 23, 42, 0.35); overflow: hidden; }
.input-shell--textarea { min-height: 100px; }
.app-input, .app-select, .app-textarea {
  width: 100%;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  padding: 12px 14px;
  font: inherit;
}
.app-textarea { resize: vertical; min-height: 100px; }
.code-area { font-family: 'Consolas', 'SFMono-Regular', monospace; }
.toggle-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.toggle-card { display: flex; align-items: center; gap: 8px; border: 1px solid var(--panel-border); border-radius: 14px; padding: 12px 14px; background: rgba(15, 23, 42, 0.28); }
.full-width { flex: 1 1 240px; }
.stack { display: grid; gap: 12px; }
.list-item {
  border: 1px solid var(--panel-border);
  border-radius: 16px;
  padding: 14px;
  background: rgba(15, 23, 42, 0.22);
  color: var(--text-primary);
  text-align: left;
}
.list-item--active { border-color: rgba(96, 165, 250, 0.65); box-shadow: 0 0 0 1px rgba(96, 165, 250, 0.2) inset; }
.detail-block { display: grid; gap: 10px; }
.empty { border: 1px dashed var(--panel-border); border-radius: 16px; padding: 24px; color: var(--text-muted); text-align: center; }
.muted { color: var(--text-muted); }
.save-hint { margin: -4px 0 0; }
.chip-row { display: flex; flex-wrap: wrap; gap: 8px; }
.chip, .status-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  background: rgba(148, 163, 184, 0.16);
  color: var(--text-secondary);
}
.chip--accent { background: rgba(56, 189, 248, 0.18); color: #7dd3fc; }
.status-pill--enabled { background: rgba(34, 197, 94, 0.16); color: #86efac; }
.status-pill--disabled { background: rgba(248, 113, 113, 0.16); color: #fca5a5; }
.code-panel {
  margin: 0;
  border: 1px solid var(--panel-border);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.32);
  padding: 14px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Consolas', 'SFMono-Regular', monospace;
  color: var(--text-secondary);
}
.section-kicker { margin: 0 0 6px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.16em; font-size: 12px; }
.page-meta { margin: 8px 0 0; color: var(--text-muted); }
.stat-card { padding: 18px; display: grid; gap: 8px; }
.stat-icon { width: 36px; height: 36px; border-radius: 12px; display: inline-flex; align-items: center; justify-content: center; background: rgba(96, 165, 250, 0.12); color: #93c5fd; }
@media (max-width: 1100px) {
  .page-grid { grid-template-columns: 1fr; }
}
@media (max-width: 720px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
