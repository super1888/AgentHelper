<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Cpu, KeyRound, RefreshCw, Server, Sparkles } from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  createModelDefinition,
  createModelProvider,
  queryModels,
  queryModelProviders,
  queryProviderCatalog,
  removeModelDefinition,
  removeModelProvider,
  testModelDefinition,
  testModelProvider,
  updateModelDefinition,
  updateModelProvider,
} from '@/api/core'
import type {
  ModelDefinitionItem,
  ModelDefinitionPayload,
  ModelProviderItem,
  ModelProviderPayload,
  ProviderCatalogItem,
} from '@/types/core'
import { getErrorMessage } from '@/utils/errors'

type ProviderMode = 'create' | 'edit'
type ModelMode = 'create' | 'edit'

const loading = ref(false)
const savingProvider = ref(false)
const savingModel = ref(false)
const testingProvider = ref(false)
const testingModel = ref(false)
const feedback = ref('')
const feedbackTone = ref<'success' | 'error' | 'info'>('info')
const providerCatalog = ref<ProviderCatalogItem[]>([])
const providers = ref<ModelProviderItem[]>([])
const models = ref<ModelDefinitionItem[]>([])
const selectedProviderCode = ref('')
const selectedModelCode = ref('')
const providerMode = ref<ProviderMode>('create')
const modelMode = ref<ModelMode>('create')

const providerForm = reactive({
  providerEnum: '',
  providerName: '',
  baseUrl: '',
  apiKey: '',
  organizationId: '',
  defaultHeadersJson: '',
  remark: '',
  status: 'ENABLED',
})

const modelForm = reactive({
  modelName: '',
  providerConfigCode: '',
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

const enabledProviders = computed(() => providers.value.filter((item) => item.status === 'ENABLED'))
const selectedProvider = computed(() => providers.value.find((item) => item.providerConfigCode === selectedProviderCode.value) ?? null)
const selectedModel = computed(() => models.value.find((item) => item.modelCode === selectedModelCode.value) ?? null)
const providerTestPrompt = ref('请只回复：PROVIDER_OK')
const modelTestPrompt = ref('请只回复：MODEL_OK')
const providerTestResult = ref('')
const modelTestResult = ref('')
const providerStats = computed(() => ({
  total: providers.value.length,
  enabled: providers.value.filter((item) => item.status === 'ENABLED').length,
}))
const modelStats = computed(() => ({
  total: models.value.length,
  enabled: models.value.filter((item) => item.status === 'ENABLED').length,
}))

const providerDefaultTestModels: Record<string, string> = {
  OPENAI: 'gpt-4.1',
  DEEPSEEK: 'deepseek-chat',
  DASHSCOPE: 'qwen-max',
  ANTHROPIC: 'claude-3-7-sonnet-latest',
  ZHIPU: 'glm-4-plus',
}

function setFeedback(tone: 'success' | 'error' | 'info', message: string) {
  feedbackTone.value = tone
  feedback.value = message
}

function resetProviderForm() {
  providerMode.value = 'create'
  providerForm.providerEnum = providerCatalog.value[0]?.providerEnum ?? ''
  providerForm.providerName = ''
  providerForm.baseUrl = ''
  providerForm.apiKey = ''
  providerForm.organizationId = ''
  providerForm.defaultHeadersJson = ''
  providerForm.remark = ''
  providerForm.status = 'ENABLED'
}

function resetModelForm() {
  modelMode.value = 'create'
  modelForm.modelName = ''
  modelForm.providerConfigCode = enabledProviders.value[0]?.providerConfigCode ?? ''
  modelForm.modelType = 'CHAT'
  modelForm.modelIdentifier = ''
  modelForm.temperature = 0.7
  modelForm.topP = 0.9
  modelForm.presencePenalty = 0
  modelForm.frequencyPenalty = 0
  modelForm.maxTokens = 4096
  modelForm.contextWindow = 128000
  modelForm.rpmLimit = 60
  modelForm.tpmLimit = 120000
  modelForm.timeoutMs = 60000
  modelForm.supportStreaming = true
  modelForm.supportTools = true
  modelForm.supportVision = false
  modelForm.supportJsonSchema = true
  modelForm.defaultModel = false
  modelForm.advancedConfigJson = ''
  modelForm.remark = ''
  modelForm.status = 'ENABLED'
}

function fillProviderForm(item: ModelProviderItem) {
  providerMode.value = 'edit'
  selectedProviderCode.value = item.providerConfigCode
  providerForm.providerEnum = item.providerEnum
  providerForm.providerName = item.providerName
  providerForm.baseUrl = item.baseUrl ?? ''
  providerForm.apiKey = ''
  providerForm.organizationId = item.organizationId ?? ''
  providerForm.defaultHeadersJson = item.defaultHeadersJson ?? ''
  providerForm.remark = item.remark ?? ''
  providerForm.status = item.status
}

function fillModelForm(item: ModelDefinitionItem) {
  modelMode.value = 'edit'
  selectedModelCode.value = item.modelCode
  modelForm.modelName = item.modelName
  modelForm.providerConfigCode = item.providerConfigCode
  modelForm.modelType = item.modelType
  modelForm.modelIdentifier = item.modelIdentifier
  modelForm.temperature = item.temperature ?? 0.7
  modelForm.topP = item.topP ?? 0.9
  modelForm.presencePenalty = item.presencePenalty ?? 0
  modelForm.frequencyPenalty = item.frequencyPenalty ?? 0
  modelForm.maxTokens = item.maxTokens ?? 4096
  modelForm.contextWindow = item.contextWindow ?? 128000
  modelForm.rpmLimit = item.rpmLimit ?? 60
  modelForm.tpmLimit = item.tpmLimit ?? 120000
  modelForm.timeoutMs = item.timeoutMs ?? 60000
  modelForm.supportStreaming = item.supportStreaming
  modelForm.supportTools = item.supportTools
  modelForm.supportVision = item.supportVision
  modelForm.supportJsonSchema = item.supportJsonSchema
  modelForm.defaultModel = item.defaultModel
  modelForm.advancedConfigJson = item.advancedConfigJson ?? ''
  modelForm.remark = item.remark ?? ''
  modelForm.status = item.status
}

function buildProviderPayload(): ModelProviderPayload {
  return {
    providerEnum: providerForm.providerEnum,
    providerName: providerForm.providerName.trim(),
    baseUrl: providerForm.baseUrl.trim() || null,
    apiKey: providerForm.apiKey.trim() || null,
    organizationId: providerForm.organizationId.trim() || null,
    defaultHeadersJson: providerForm.defaultHeadersJson.trim() || null,
    remark: providerForm.remark.trim() || null,
    status: providerForm.status,
  }
}

function buildModelPayload(): ModelDefinitionPayload {
  return {
    modelName: modelForm.modelName.trim(),
    providerConfigCode: modelForm.providerConfigCode,
    modelType: modelForm.modelType,
    modelIdentifier: modelForm.modelIdentifier.trim(),
    temperature: modelForm.temperature,
    topP: modelForm.topP,
    presencePenalty: modelForm.presencePenalty,
    frequencyPenalty: modelForm.frequencyPenalty,
    maxTokens: modelForm.maxTokens,
    contextWindow: modelForm.contextWindow,
    rpmLimit: modelForm.rpmLimit,
    tpmLimit: modelForm.tpmLimit,
    timeoutMs: modelForm.timeoutMs,
    supportStreaming: modelForm.supportStreaming,
    supportTools: modelForm.supportTools,
    supportVision: modelForm.supportVision,
    supportJsonSchema: modelForm.supportJsonSchema,
    defaultModel: modelForm.defaultModel,
    advancedConfigJson: modelForm.advancedConfigJson.trim() || null,
    remark: modelForm.remark.trim() || null,
    status: modelForm.status,
  }
}

async function loadData(successMessage?: string) {
  loading.value = true
  try {
    const [catalog, providerList, modelList] = await Promise.all([
      queryProviderCatalog(),
      queryModelProviders(),
      queryModels(),
    ])
    providerCatalog.value = catalog
    providers.value = providerList
    models.value = modelList
    if (!providerList.some((item) => item.providerConfigCode === selectedProviderCode.value)) {
      selectedProviderCode.value = providerList[0]?.providerConfigCode ?? ''
    }
    if (!modelList.some((item) => item.modelCode === selectedModelCode.value)) {
      selectedModelCode.value = modelList[0]?.modelCode ?? ''
    }
    if (!providerForm.providerEnum) resetProviderForm()
    if (!providerList.some((item) => item.providerConfigCode === modelForm.providerConfigCode)) resetModelForm()
    if (successMessage) setFeedback('success', successMessage)
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '核心配置加载失败。'))
  } finally {
    loading.value = false
  }
}

async function submitProvider() {
  if (!providerForm.providerName.trim()) return setFeedback('error', '请输入提供商配置名称。')
  if (!providerForm.providerEnum) return setFeedback('error', '请选择提供商枚举。')
  savingProvider.value = true
  try {
    if (providerMode.value === 'create') {
      const result = await createModelProvider(buildProviderPayload())
      selectedProviderCode.value = result.providerConfigCode
      resetProviderForm()
      await loadData(`提供商配置 ${result.providerName} 已创建。`)
    } else {
      const result = await updateModelProvider(selectedProviderCode.value, buildProviderPayload())
      await loadData(`提供商配置 ${result.providerName} 已更新。`)
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '保存提供商配置失败。'))
  } finally {
    savingProvider.value = false
  }
}

async function handleDeleteProvider() {
  if (!selectedProviderCode.value) return
  try {
    const target = providers.value.find((item) => item.providerConfigCode === selectedProviderCode.value)
    await removeModelProvider(selectedProviderCode.value)
    selectedProviderCode.value = ''
    resetProviderForm()
    await loadData(`提供商配置 ${target?.providerName || ''} 已删除。`)
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '删除提供商配置失败。'))
  }
}

async function handleTestProvider() {
  if (!providerForm.providerEnum) return setFeedback('error', '请选择提供商枚举。')
  if (!providerForm.apiKey.trim() && !selectedProvider.value?.apiKeyConfigured) {
    return setFeedback('error', '请输入 API Key，或编辑一个已配置密钥的提供商。')
  }
  testingProvider.value = true
  try {
    const result = await testModelProvider({
      providerConfigCode: selectedProviderCode.value || undefined,
      providerEnum: providerForm.providerEnum,
      baseUrl: providerForm.baseUrl.trim() || null,
      apiKey: providerForm.apiKey.trim() || null,
      testModelIdentifier: modelForm.modelIdentifier.trim() || providerDefaultTestModels[providerForm.providerEnum] || 'gpt-4.1',
      testPrompt: providerTestPrompt.value.trim(),
    })
    providerTestResult.value = `[${result.elapsedMs}ms] ${result.responseContent}`
    setFeedback('success', '提供商连接测试成功。')
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '提供商连接测试失败。'))
  } finally {
    testingProvider.value = false
  }
}

async function submitModel() {
  if (!modelForm.modelName.trim()) return setFeedback('error', '请输入模型名称。')
  if (!modelForm.providerConfigCode) return setFeedback('error', '请选择模型提供商配置。')
  if (!modelForm.modelIdentifier.trim()) return setFeedback('error', '请输入模型标识。')
  savingModel.value = true
  try {
    if (modelMode.value === 'create') {
      const result = await createModelDefinition(buildModelPayload())
      selectedModelCode.value = result.modelCode
      resetModelForm()
      await loadData(`模型 ${result.modelName} 已创建。`)
    } else {
      const result = await updateModelDefinition(selectedModelCode.value, buildModelPayload())
      await loadData(`模型 ${result.modelName} 已更新。`)
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '保存模型失败。'))
  } finally {
    savingModel.value = false
  }
}

async function handleDeleteModel() {
  if (!selectedModelCode.value) return
  try {
    const target = models.value.find((item) => item.modelCode === selectedModelCode.value)
    await removeModelDefinition(selectedModelCode.value)
    selectedModelCode.value = ''
    resetModelForm()
    await loadData(`模型 ${target?.modelName || ''} 已删除。`)
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '删除模型失败。'))
  }
}

async function handleTestModel() {
  if (!selectedModelCode.value) return setFeedback('error', '请选择一个模型配置。')
  testingModel.value = true
  try {
    const result = await testModelDefinition(selectedModelCode.value, { testPrompt: modelTestPrompt.value.trim() })
    modelTestResult.value = `[${result.elapsedMs}ms] ${result.responseContent}`
    setFeedback('success', '模型测试成功。')
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '模型测试失败。'))
  } finally {
    testingModel.value = false
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
          <h2>模型中心与供应商配置</h2>
          <p class="page-meta">提供商 {{ providerStats.enabled }}/{{ providerStats.total }} 启用，模型 {{ modelStats.enabled }}/{{ modelStats.total }} 启用。</p>
        </div>
        <button class="app-button app-button--secondary" :disabled="loading" @click="loadData()"><RefreshCw :size="16" />刷新</button>
      </header>

      <div class="stats-grid">
        <article class="stat-card panel-card"><span class="stat-icon"><Server :size="18" /></span><strong>{{ providerStats.enabled }}</strong><p>启用中的提供商配置</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Cpu :size="18" /></span><strong>{{ modelStats.enabled }}</strong><p>可绑定到 Agent 的模型</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><KeyRound :size="18" /></span><strong>{{ providers.filter((item) => item.apiKeyConfigured).length }}</strong><p>已完成 API Key 安全托管</p></article>
        <article class="stat-card panel-card"><span class="stat-icon"><Sparkles :size="18" /></span><strong>{{ models.filter((item) => item.defaultModel).length }}</strong><p>默认模型配置条目</p></article>
      </div>

      <div class="page-grid">
        <article class="panel-card section-card">
          <div class="section-head"><div><strong>{{ providerMode === 'create' ? '创建提供商配置' : '编辑提供商配置' }}</strong><p class="muted">按租户管理 API Key、Base URL、组织标识和默认请求头。</p></div><div class="action-row"><button v-if="providerMode === 'edit'" class="app-button app-button--ghost" @click="resetProviderForm">切回创建</button><button v-if="providerMode === 'edit'" class="app-button app-button--ghost app-button--danger-ghost" @click="handleDeleteProvider">删除</button></div></div>
          <div class="form-grid">
            <label class="field"><span class="field__label">供应商枚举</span><select v-model="providerForm.providerEnum" class="app-select"><option v-for="item in providerCatalog" :key="item.providerEnum" :value="item.providerEnum">{{ item.providerLabel }}</option></select></label>
            <label class="field"><span class="field__label">配置名称</span><div class="input-shell"><input v-model="providerForm.providerName" class="app-input" type="text" placeholder="例如：OpenAI 生产账号" /></div></label>
            <label class="field"><span class="field__label">Base URL</span><div class="input-shell"><input v-model="providerForm.baseUrl" class="app-input" type="text" placeholder="例如：https://api.openai.com" /></div></label>
            <label class="field"><span class="field__label">状态</span><select v-model="providerForm.status" class="app-select"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label>
            <label class="field form-grid__full"><span class="field__label">API Key</span><div class="input-shell"><input v-model="providerForm.apiKey" class="app-input" type="password" :placeholder="providerMode === 'create' ? '请输入 API Key' : '留空则保持原密钥不变'" /></div></label>
            <label class="field"><span class="field__label">组织标识</span><div class="input-shell"><input v-model="providerForm.organizationId" class="app-input" type="text" placeholder="可选" /></div></label>
            <label class="field"><span class="field__label">备注</span><div class="input-shell"><input v-model="providerForm.remark" class="app-input" type="text" placeholder="例如：面向线上租户" /></div></label>
            <label class="field form-grid__full"><span class="field__label">默认请求头 JSON</span><div class="input-shell input-shell--textarea"><textarea v-model="providerForm.defaultHeadersJson" class="app-textarea code-area" rows="5" placeholder='例如：{"x-tenant":"prod"}' /></div></label>
          </div>
          <div class="action-row">
            <button class="app-button full-width" :disabled="savingProvider" @click="submitProvider">{{ savingProvider ? '提交中...' : providerMode === 'create' ? '创建提供商配置' : '保存提供商配置' }}</button>
            <button class="app-button app-button--secondary full-width" :disabled="testingProvider" @click="handleTestProvider">{{ testingProvider ? '测试中...' : '测试连接' }}</button>
          </div>
          <pre v-if="providerTestResult" class="code-panel">{{ providerTestResult }}</pre>
        </article>

        <article class="panel-card section-card">
          <div class="section-head"><div><strong>提供商配置列表</strong><p class="muted">API Key 仅脱敏展示，编辑时可覆盖更新。</p></div></div>
          <div v-if="loading" class="empty">正在加载提供商配置...</div>
          <div v-else-if="providers.length === 0" class="empty">当前租户还没有提供商配置。</div>
          <div v-else class="stack">
            <button v-for="item in providers" :key="item.providerConfigCode" class="list-item" :class="{ 'list-item--active': selectedProviderCode === item.providerConfigCode }" @click="fillProviderForm(item)">
              <div class="list-item__head"><strong>{{ item.providerName }}</strong><span class="status-pill" :class="item.status === 'ENABLED' ? 'status-pill--enabled' : 'status-pill--disabled'">{{ item.status === 'ENABLED' ? '启用' : '停用' }}</span></div>
              <p class="muted">{{ item.providerEnum }} · {{ item.baseUrl || '使用默认 Base URL' }}</p>
              <small class="muted">API Key：{{ item.apiKeyMasked || '未配置' }}</small>
              <small class="muted">更新于 {{ formatTime(item.updateTime) }}</small>
            </button>
          </div>
        </article>
      </div>

      <div class="page-grid">
        <article class="panel-card section-card">
          <div class="section-head"><div><strong>{{ modelMode === 'create' ? '创建模型配置' : '编辑模型配置' }}</strong><p class="muted">支持温度、Top P、惩罚项、吞吐限制、工具调用和 JSON 输出等参数。</p></div><div class="action-row"><button v-if="modelMode === 'edit'" class="app-button app-button--ghost" @click="resetModelForm">切回创建</button><button v-if="modelMode === 'edit'" class="app-button app-button--ghost app-button--danger-ghost" @click="handleDeleteModel">删除</button></div></div>
          <div class="form-grid">
            <label class="field"><span class="field__label">模型名称</span><div class="input-shell"><input v-model="modelForm.modelName" class="app-input" type="text" placeholder="例如：GPT-4.1 主模型" /></div></label>
            <label class="field"><span class="field__label">提供商配置</span><select v-model="modelForm.providerConfigCode" class="app-select"><option value="">请选择提供商配置</option><option v-for="item in enabledProviders" :key="item.providerConfigCode" :value="item.providerConfigCode">{{ item.providerName }} / {{ item.providerEnum }}</option></select></label>
            <label class="field"><span class="field__label">模型类型</span><select v-model="modelForm.modelType" class="app-select"><option value="CHAT">CHAT</option><option value="EMBEDDING">EMBEDDING</option><option value="RERANK">RERANK</option></select></label>
            <label class="field"><span class="field__label">模型标识</span><div class="input-shell"><input v-model="modelForm.modelIdentifier" class="app-input" type="text" placeholder="例如：gpt-4.1" /></div></label>
            <label class="field"><span class="field__label">Temperature</span><div class="input-shell"><input v-model.number="modelForm.temperature" class="app-input" type="number" step="0.1" min="0" max="2" /></div></label>
            <label class="field"><span class="field__label">Top P</span><div class="input-shell"><input v-model.number="modelForm.topP" class="app-input" type="number" step="0.1" min="0" max="1" /></div></label>
            <label class="field"><span class="field__label">Presence Penalty</span><div class="input-shell"><input v-model.number="modelForm.presencePenalty" class="app-input" type="number" step="0.1" min="-2" max="2" /></div></label>
            <label class="field"><span class="field__label">Frequency Penalty</span><div class="input-shell"><input v-model.number="modelForm.frequencyPenalty" class="app-input" type="number" step="0.1" min="-2" max="2" /></div></label>
            <label class="field"><span class="field__label">Max Tokens</span><div class="input-shell"><input v-model.number="modelForm.maxTokens" class="app-input" type="number" min="1" /></div></label>
            <label class="field"><span class="field__label">Context Window</span><div class="input-shell"><input v-model.number="modelForm.contextWindow" class="app-input" type="number" min="1" /></div></label>
            <label class="field"><span class="field__label">RPM 限流</span><div class="input-shell"><input v-model.number="modelForm.rpmLimit" class="app-input" type="number" min="1" /></div></label>
            <label class="field"><span class="field__label">TPM 限流</span><div class="input-shell"><input v-model.number="modelForm.tpmLimit" class="app-input" type="number" min="1" /></div></label>
            <label class="field"><span class="field__label">超时毫秒</span><div class="input-shell"><input v-model.number="modelForm.timeoutMs" class="app-input" type="number" min="1000" /></div></label>
            <label class="field"><span class="field__label">状态</span><select v-model="modelForm.status" class="app-select"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label>
            <div class="toggle-grid form-grid__full">
              <label class="toggle-card"><input v-model="modelForm.supportStreaming" type="checkbox" />支持流式输出</label>
              <label class="toggle-card"><input v-model="modelForm.supportTools" type="checkbox" />支持工具调用</label>
              <label class="toggle-card"><input v-model="modelForm.supportVision" type="checkbox" />支持视觉输入</label>
              <label class="toggle-card"><input v-model="modelForm.supportJsonSchema" type="checkbox" />支持 JSON Schema</label>
              <label class="toggle-card"><input v-model="modelForm.defaultModel" type="checkbox" />设为默认模型</label>
            </div>
            <label class="field form-grid__full"><span class="field__label">高级参数 JSON</span><div class="input-shell input-shell--textarea"><textarea v-model="modelForm.advancedConfigJson" class="app-textarea code-area" rows="5" placeholder='例如：{"stop":["Observation:"]}' /></div></label>
            <label class="field form-grid__full"><span class="field__label">备注</span><div class="input-shell"><input v-model="modelForm.remark" class="app-input" type="text" placeholder="例如：客服主路由使用" /></div></label>
          </div>
          <div class="action-row">
            <button class="app-button full-width" :disabled="savingModel" @click="submitModel">{{ savingModel ? '提交中...' : modelMode === 'create' ? '创建模型配置' : '保存模型配置' }}</button>
            <button class="app-button app-button--secondary full-width" :disabled="testingModel || !selectedModelCode" @click="handleTestModel">{{ testingModel ? '测试中...' : '测试模型' }}</button>
          </div>
          <pre v-if="modelTestResult" class="code-panel">{{ modelTestResult }}</pre>
        </article>

        <article class="panel-card section-card">
          <div class="section-head"><div><strong>模型配置列表</strong><p class="muted">Agent 页面会直接绑定这里的启用模型。</p></div></div>
          <div v-if="loading" class="empty">正在加载模型配置...</div>
          <div v-else-if="models.length === 0" class="empty">当前租户还没有模型配置。</div>
          <div v-else class="stack">
            <button v-for="item in models" :key="item.modelCode" class="list-item" :class="{ 'list-item--active': selectedModelCode === item.modelCode }" @click="fillModelForm(item)">
              <div class="list-item__head"><strong>{{ item.modelName }}</strong><span class="status-pill" :class="item.status === 'ENABLED' ? 'status-pill--enabled' : 'status-pill--disabled'">{{ item.status === 'ENABLED' ? '启用' : '停用' }}</span></div>
              <p class="muted">{{ item.providerName || item.providerEnum }} · {{ item.modelIdentifier }}</p>
              <div class="chip-row"><span class="chip">{{ item.modelType }}</span><span v-if="item.defaultModel" class="chip chip--accent">默认</span><span v-if="item.supportTools" class="chip">Tools</span><span v-if="item.supportStreaming" class="chip">Stream</span><span v-if="item.supportJsonSchema" class="chip">JSON</span></div>
              <small class="muted">更新于 {{ formatTime(item.updateTime) }}</small>
            </button>
          </div>
        </article>
      </div>

      <div class="page-grid">
        <article class="panel-card section-card">
          <div class="section-head"><div><strong>当前提供商详情</strong><p class="muted">用于核对该账户的安全托管状态与接入地址。</p></div></div>
          <div v-if="!selectedProvider" class="empty">选择一个提供商配置后查看详情。</div>
          <div v-else class="detail-block">
            <strong>{{ selectedProvider.providerName }}</strong>
            <p class="muted">枚举：{{ selectedProvider.providerEnum }}</p>
            <p class="muted">Base URL：{{ selectedProvider.baseUrl || '使用供应商默认地址' }}</p>
            <p class="muted">组织标识：{{ selectedProvider.organizationId || '未设置' }}</p>
            <p class="muted">API Key：{{ selectedProvider.apiKeyMasked || '未配置' }}</p>
            <pre class="code-panel">{{ selectedProvider.defaultHeadersJson || '{}' }}</pre>
          </div>
        </article>

        <article class="panel-card section-card">
          <div class="section-head"><div><strong>当前模型详情</strong><p class="muted">用于 Agent 绑定前确认运行参数和商业化能力开关。</p></div></div>
          <div v-if="!selectedModel" class="empty">选择一个模型配置后查看详情。</div>
          <div v-else class="detail-block">
            <strong>{{ selectedModel.modelName }}</strong>
            <p class="muted">供应商：{{ selectedModel.providerName || selectedModel.providerEnum }}</p>
            <p class="muted">模型标识：{{ selectedModel.modelIdentifier }}</p>
            <p class="muted">温度 / Top P：{{ selectedModel.temperature ?? '-' }} / {{ selectedModel.topP ?? '-' }}</p>
            <p class="muted">超时 / 上下文：{{ selectedModel.timeoutMs ?? '-' }} ms / {{ selectedModel.contextWindow ?? '-' }}</p>
            <div class="chip-row"><span class="chip">{{ selectedModel.modelType }}</span><span v-if="selectedModel.supportStreaming" class="chip">Streaming</span><span v-if="selectedModel.supportTools" class="chip">Tool Calling</span><span v-if="selectedModel.supportVision" class="chip">Vision</span><span v-if="selectedModel.supportJsonSchema" class="chip">JSON Schema</span></div>
            <pre class="code-panel">{{ selectedModel.advancedConfigJson || '{}' }}</pre>
          </div>
        </article>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.core-page { display: grid; gap: var(--layout-gap); min-width: 0; }
.page-hero, .section-head, .list-item__head, .action-row { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
.page-hero { align-items: flex-start; padding: var(--panel-padding); min-width: 0; }
.page-meta, .muted { color: var(--color-ink-soft); line-height: 1.6; }
.stats-grid, .page-grid, .form-grid, .toggle-grid { display: grid; gap: 18px; min-width: 0; }
.stats-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.page-grid { grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.92fr); }
.section-card, .stat-card, .list-item, .empty, .detail-block { display: grid; gap: 14px; align-content: start; padding: var(--compact-panel-padding); border-radius: var(--sub-panel-radius); background: rgba(255, 255, 255, 0.04); box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06); }
.section-card { min-width: 0; overflow: visible; }
.stat-card strong { font-size: 1.8rem; color: var(--color-ink-strong); line-height: 1.2; }
.stat-card p { margin: 0; color: var(--color-ink-soft); line-height: 1.6; }
.stat-icon { display: inline-flex; width: 42px; height: 42px; align-items: center; justify-content: center; border-radius: 14px; color: #d8f2ff; background: rgba(77, 179, 255, 0.14); }
.form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.form-grid__full { grid-column: 1 / -1; }
.toggle-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.toggle-card { display: flex; align-items: center; gap: 10px; min-height: 52px; padding: 0 14px; border-radius: 18px; color: var(--color-ink-soft); background: rgba(255, 255, 255, 0.04); }
.stack, .chip-row { display: flex; flex-direction: column; gap: 14px; min-width: 0; }
.chip-row { flex-direction: row; flex-wrap: wrap; }
.list-item { cursor: pointer; min-width: 0; text-align: left; }
.list-item--active { box-shadow: inset 0 0 0 1px rgba(77, 179, 255, 0.32), 0 16px 28px rgba(77, 179, 255, 0.1); }
.chip, .status-pill { display: inline-flex; align-items: center; justify-content: center; min-height: 28px; padding: 0 10px; border-radius: 999px; font-size: 0.78rem; font-weight: 700; line-height: 1.4; }
.chip { color: #d8f2ff; background: rgba(77, 179, 255, 0.16); }
.chip--accent, .status-pill--enabled { color: #d7ffef; background: rgba(100, 216, 190, 0.18); }
.status-pill--disabled { color: #ffd2d6; background: rgba(255, 144, 151, 0.18); }
.code-panel { margin: 0; padding: 12px 14px; border-radius: 18px; color: var(--color-ink-soft); background: rgba(4, 17, 29, 0.58); white-space: pre-wrap; word-break: break-word; }
.code-area { font-family: 'JetBrains Mono', monospace; }
.full-width { width: 100%; }
.section-head strong,
.list-item__head strong { display: inline-block; line-height: 1.35; padding-block: 2px; }
.action-row > * { flex: 1 1 220px; }
@media (max-width: 1320px) { .stats-grid, .page-grid, .form-grid, .toggle-grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .page-hero, .section-head, .list-item__head, .action-row { flex-direction: column; align-items: stretch; } }
</style>
