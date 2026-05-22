<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { MessageSquareText, Plus, RefreshCw, Rocket, Search, ShieldBan, Trash2 } from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import { batchMigrateAgentModels, createAgent, createAgentSession, disableAgent, fetchAgentDetail, publishAgent, queryAgents, queryDocumentExpertModels, removeAgent, runDocumentExpertAgent, updateAgent } from '@/api/agent'
import { queryEnabledModels } from '@/api/core'
import { queryHooks } from '@/api/hook'
import { queryMcpServers } from '@/api/mcp'
import { queryPromptTemplates } from '@/api/prompt'
import type { AgentCreatePayload, AgentDetail, AgentPromptConfig, AgentSessionResult, AgentSummary, AgentVersion, CustomAgentDocumentExpertResult, CustomAgentStageResult } from '@/types/agent'
import type { ModelOption } from '@/types/core'
import type { HookItem } from '@/types/hook'
import type { McpItem } from '@/types/mcp'
import type { PromptTemplateItem, PromptTemplateVariable } from '@/types/prompt'
import { getErrorMessage } from '@/utils/errors'

type PromptMode = 'template' | 'custom-inline' | 'custom-file'
type FormMode = 'create' | 'edit'
type PendingAction = 'publish' | 'disable' | 'session' | 'delete' | null
type MigrationMode = 'DRAFT_ONLY' | 'PUBLISH_NEW_VERSION'

const router = useRouter()
const loading = ref(false)
const detailLoading = ref(false)
const promptTemplatesLoading = ref(false)
const hooksLoading = ref(false)
const mcpLoading = ref(false)
const modelsLoading = ref(false)
const submitting = ref(false)
const actionPending = ref<PendingAction>(null)
const feedback = ref('')
const feedbackTone = ref<'success' | 'error' | 'info'>('info')
const agents = ref<AgentSummary[]>([])
const promptTemplates = ref<PromptTemplateItem[]>([])
const hooks = ref<HookItem[]>([])
const mcpServers = ref<McpItem[]>([])
const availableModels = ref<ModelOption[]>([])
const customAgentModels = ref<ModelOption[]>([])
const selectedAgentId = ref('')
const selectedAgentDetail = ref<AgentDetail | null>(null)
const createdSession = ref<AgentSessionResult | null>(null)
const customAgentRunning = ref(false)
const customAgentResult = ref<CustomAgentDocumentExpertResult | null>(null)
const formMode = ref<FormMode>('create')
const editingAgentId = ref<string | null>(null)
const formPanelRef = ref<HTMLElement | null>(null)

const filters = reactive({ keyword: '', status: 'ALL', modelCode: '' })
const selectedAgentIds = ref<string[]>([])
const batchMigrating = ref(false)
const migrationMode = ref<MigrationMode>('DRAFT_ONLY')
const form = reactive({
  agentName: '',
  description: '',
  selectedCapabilitiesText: 'knowledge_search, session_management, failover_recovery',
  selectedHookCodes: [] as string[],
  selectedMcpServerIds: [] as string[],
  modelConfigCode: '',
  promptMode: 'template' as PromptMode,
  selectedPromptTemplateId: '',
  customPromptContent: '',
  customPromptPath: '',
  promptVariableValues: {} as Record<string, string>,
})
const customDocumentForm = reactive({
  modelCode: '',
  routeModelCode: '',
  enhancementModelCode: '',
  generationAModelCode: '',
  generationBModelCode: '',
  auditModelCode: '',
  fusionModelCode: '',
  userPrompt: '',
  autoFillMissingInfo: true,
})

const filteredAgents = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return agents.value.filter((item) => {
    const matchesKeyword = !keyword || [item.agentName, item.description, item.ownerUserName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
    const matchesStatus = filters.status === 'ALL' || item.agentStatus === filters.status
    const matchesModel = !filters.modelCode || item.modelCode === filters.modelCode
    return matchesKeyword && matchesStatus && matchesModel
  })
})

const sortedVersions = computed<AgentVersion[]>(() => {
  if (!selectedAgentDetail.value?.versions?.length) {
    return []
  }
  return [...selectedAgentDetail.value.versions].sort((left, right) => right.versionNo - left.versionNo)
})

const latestVersion = computed<AgentVersion | null>(() => sortedVersions.value[0] ?? null)
const canEditSelectedAgent = computed(() => Boolean(selectedAgentDetail.value && latestVersion.value) && actionPending.value === null)
const canPublishLatest = computed(() => Boolean(latestVersion.value) && actionPending.value === null)
const selectedPromptTemplate = computed(() => promptTemplates.value.find((item) => String(item.id) === form.selectedPromptTemplateId) ?? null)
const selectedPromptVariables = computed<PromptTemplateVariable[]>(() => selectedPromptTemplate.value?.variableDefinitions ?? [])
const capabilityPreview = computed(() => parseCapabilities(form.selectedCapabilitiesText))
const availableHooks = computed(() =>
  hooks.value.filter((item) => item.hookStatus === 'ENABLED' && item.publishStatus === 'PUBLISHED'),
)
const availableMcpServers = computed(() =>
  mcpServers.value.filter((item) => item.serverStatus === 'ENABLED' && item.publishStatus === 'PUBLISHED'),
)
const selectedModel = computed(() => availableModels.value.find((item) => item.modelCode === form.modelConfigCode) ?? null)
const selectedCustomDefaultModel = computed(() =>
  customAgentModels.value.find((item) => item.modelCode === customDocumentForm.modelCode) ?? null,
)
const totalAgentsLabel = computed(() => `共 ${agents.value.length} 个智能体`)
const publishedCountLabel = computed(() => `已发布 ${agents.value.filter((item) => item.agentStatus === 'PUBLISHED').length} 个`)
const formTitle = computed(() => (formMode.value === 'create' ? '创建智能体' : '编辑智能体'))

watch(selectedAgentId, (value) => {
  createdSession.value = null
  if (!value) {
    selectedAgentDetail.value = null
    return
  }
  void loadAgentDetail(value)
})

watch(() => [form.promptMode, form.selectedPromptTemplateId] as const, ([mode]) => {
  if (mode === 'template') {
    syncPromptVariableValues()
  }
})

watch(() => filters.modelCode, () => {
  void loadAgents()
})

function setFeedback(tone: 'success' | 'error' | 'info', message: string) {
  feedbackTone.value = tone
  feedback.value = message
}

function parseCapabilities(value: string) {
  return value.split(/[\n,，、]/).map((item) => item.trim()).filter(Boolean)
}

function resetForm() {
  form.agentName = ''
  form.description = ''
  form.selectedCapabilitiesText = 'knowledge_search, session_management, failover_recovery'
  form.selectedHookCodes = []
  form.selectedMcpServerIds = []
  form.modelConfigCode = availableModels.value.find((item) => item.defaultModel)?.modelCode ?? availableModels.value[0]?.modelCode ?? ''
  form.promptMode = 'template'
  form.selectedPromptTemplateId = promptTemplates.value[0] ? String(promptTemplates.value[0].id) : ''
  form.customPromptContent = ''
  form.customPromptPath = ''
  form.promptVariableValues = {}
  syncPromptVariableValues()
}

function resetCustomDocumentForm() {
  const defaultModelCode = customAgentModels.value.find((item) => item.defaultModel)?.modelCode ?? customAgentModels.value[0]?.modelCode ?? ''
  customDocumentForm.modelCode = defaultModelCode
  customDocumentForm.routeModelCode = ''
  customDocumentForm.enhancementModelCode = ''
  customDocumentForm.generationAModelCode = ''
  customDocumentForm.generationBModelCode = ''
  customDocumentForm.auditModelCode = ''
  customDocumentForm.fusionModelCode = ''
  customDocumentForm.userPrompt = ''
  customDocumentForm.autoFillMissingInfo = true
}

function syncPromptVariableValues(sourceValues?: Record<string, string> | null) {
  const values: Record<string, string> = {}
  for (const item of selectedPromptVariables.value) {
    values[item.variableName] = sourceValues?.[item.variableName] ?? item.defaultValue ?? ''
  }
  form.promptVariableValues = values
}

function enterCreateMode() {
  formMode.value = 'create'
  editingAgentId.value = null
  resetForm()
}

function enterEditMode() {
  if (!selectedAgentDetail.value || !latestVersion.value) {
    setFeedback('error', '当前智能体缺少可编辑的版本快照。')
    return
  }
  const version = latestVersion.value
  formMode.value = 'edit'
  editingAgentId.value = selectedAgentDetail.value.agentId
  form.agentName = selectedAgentDetail.value.agentName
  form.description = selectedAgentDetail.value.description ?? ''
  form.selectedCapabilitiesText = version.selectedCapabilities.join(', ')
  form.selectedHookCodes = [...(version.selectedHookCodes ?? [])]
  form.selectedMcpServerIds = [...(version.selectedMcpServerIds ?? [])]
  form.modelConfigCode = version.modelCode || ''
  requestAnimationFrame(() => {
    formPanelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
  if (version.promptBindingType === 'TEMPLATE' && version.promptTemplateId) {
    form.promptMode = 'template'
    form.selectedPromptTemplateId = String(version.promptTemplateId)
    form.customPromptContent = ''
    form.customPromptPath = ''
    syncPromptVariableValues(version.promptVariables)
    return
  }
  if (version.promptSourceType === 'FILE_PATH') {
    form.promptMode = 'custom-file'
    form.customPromptPath = version.promptTemplatePath ?? ''
    form.customPromptContent = ''
    form.promptVariableValues = {}
    return
  }
  form.promptMode = 'custom-inline'
  form.customPromptContent = version.promptTemplateContent || version.systemPrompt || ''
  form.customPromptPath = ''
  form.promptVariableValues = {}
}

function buildPromptConfig(): AgentPromptConfig | null {
  if (form.promptMode === 'template') {
    return form.selectedPromptTemplateId
      ? { promptTemplateId: form.selectedPromptTemplateId, promptBindingType: 'TEMPLATE', promptVariables: { ...form.promptVariableValues } }
      : null
  }
  if (form.promptMode === 'custom-inline') {
    return form.customPromptContent.trim()
      ? { promptBindingType: 'CUSTOM', promptSourceType: 'INLINE_TEXT', promptTemplateContent: form.customPromptContent.trim() }
      : null
  }
  return form.customPromptPath.trim()
    ? { promptBindingType: 'CUSTOM', promptSourceType: 'FILE_PATH', promptTemplatePath: form.customPromptPath.trim() }
    : null
}

function buildPayload(): AgentCreatePayload {
  return {
    agentName: form.agentName.trim(),
    description: form.description.trim() || null,
    systemPrompt: form.promptMode === 'custom-inline' ? form.customPromptContent.trim() || null : null,
    selectedCapabilities: parseCapabilities(form.selectedCapabilitiesText),
    selectedHookCodes: [...form.selectedHookCodes],
    selectedMcpServerIds: [...form.selectedMcpServerIds],
    modelConfigCode: form.modelConfigCode,
    agentType: 'REACT',
    promptConfig: buildPromptConfig(),
  }
}

function formatStatus(status: string) {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'DISABLED') return '已停用'
  return '草稿中'
}

function formatModelBinding(version: AgentVersion) {
  if (!version.modelName) return '未绑定模型'
  return `${version.modelName} / ${version.providerName || version.providerEnum || '-'}`
}

function formatPromptBinding(version: AgentVersion) {
  if (version.promptBindingType === 'TEMPLATE') return version.promptTemplateName || version.promptTemplateCode || '模板绑定'
  if (version.promptSourceType === 'FILE_PATH') return version.promptTemplatePath || '文件路径'
  return '自定义文本'
}

function formatHookStage(stage?: string | null) {
  if (stage === 'PRE_MODEL') return '模型前'
  if (stage === 'POST_MODEL') return '模型后'
  if (stage === 'PRE_TOOL_CALL') return '工具前'
  if (stage === 'POST_TOOL_CALL') return '工具后'
  return stage || '未定义阶段'
}

function formatTime(value: number | null) {
  if (!value) return '刚刚生成'
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value)
}

async function loadPromptTemplates() {
  promptTemplatesLoading.value = true
  try {
    promptTemplates.value = (await queryPromptTemplates()).filter((item) => item.templateStatus === 'ENABLED')
    if (!form.selectedPromptTemplateId && promptTemplates.value.length > 0) {
      form.selectedPromptTemplateId = String(promptTemplates.value[0].id)
      syncPromptVariableValues()
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '提示词模板加载失败。'))
  } finally {
    promptTemplatesLoading.value = false
  }
}

async function loadHooks() {
  hooksLoading.value = true
  try {
    hooks.value = await queryHooks()
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '钩子列表加载失败。'))
  } finally {
    hooksLoading.value = false
  }
}

async function loadMcpServers() {
  mcpLoading.value = true
  try {
    mcpServers.value = await queryMcpServers()
  } catch (error) {
    setFeedback('error', getErrorMessage(error, 'MCP 服务列表加载失败。'))
  } finally {
    mcpLoading.value = false
  }
}

async function loadModels() {
  modelsLoading.value = true
  try {
    availableModels.value = await queryEnabledModels()
    if (!form.modelConfigCode) {
      form.modelConfigCode = availableModels.value.find((item) => item.defaultModel)?.modelCode ?? availableModels.value[0]?.modelCode ?? ''
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '模型列表加载失败。'))
  } finally {
    modelsLoading.value = false
  }
}

async function loadCustomAgentModels() {
  try {
    customAgentModels.value = await queryDocumentExpertModels()
    if (!customDocumentForm.modelCode) {
      resetCustomDocumentForm()
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '自定义智能体模型列表加载失败。'))
  }
}

async function loadAgents(options?: { keepSelection?: boolean; successMessage?: string }) {
  loading.value = true
  try {
    const result = await queryAgents(filters.modelCode || undefined)
    agents.value = result
    selectedAgentIds.value = selectedAgentIds.value.filter((item) => result.some((agent) => agent.agentId === item))
    const keepSelection = options?.keepSelection && result.some((item) => item.agentId === selectedAgentId.value)
    if (!keepSelection) {
      selectedAgentId.value = result[0]?.agentId ?? ''
    } else if (selectedAgentId.value) {
      await loadAgentDetail(selectedAgentId.value)
    }
    if (options?.successMessage) {
      setFeedback('success', options.successMessage)
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '智能体列表加载失败。'))
  } finally {
    loading.value = false
  }
}

async function loadAgentDetail(agentId: string) {
  detailLoading.value = true
  try {
    const detail = await fetchAgentDetail(agentId)
    selectedAgentDetail.value = {
      ...detail,
      versions: [...detail.versions].sort((left, right) => right.versionNo - left.versionNo),
    }
  } catch (error) {
    selectedAgentDetail.value = null
    setFeedback('error', getErrorMessage(error, '智能体详情加载失败。'))
  } finally {
    detailLoading.value = false
  }
}

async function handleSubmit() {
  if (!form.agentName.trim()) {
    setFeedback('error', '请输入智能体名称。')
    return
  }
  if (!buildPromptConfig()) {
    setFeedback('error', '请选择提示词模板，或补全自定义提示词配置。')
    return
  }
  if (!form.modelConfigCode) {
    setFeedback('error', '请选择模型配置。')
    return
  }
  submitting.value = true
  try {
    if (formMode.value === 'create') {
      const result = await createAgent(buildPayload())
      selectedAgentId.value = result.agentId
      enterCreateMode()
      await loadAgents({ keepSelection: true, successMessage: `智能体 ${result.agentName} 已创建。` })
    } else {
      if (!editingAgentId.value) {
        throw new Error('缺少待编辑的智能体编码。')
      }
      const result = await updateAgent(editingAgentId.value, buildPayload())
      selectedAgentId.value = result.agentId
      await loadAgents({ keepSelection: true, successMessage: `智能体 ${result.agentName} 已更新并生成新版本。` })
      enterCreateMode()
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, formMode.value === 'create' ? '创建智能体失败。' : '更新智能体失败。'))
  } finally {
    submitting.value = false
  }
}

async function handlePublishLatest() {
  if (!selectedAgentDetail.value || !latestVersion.value) {
    setFeedback('error', '当前智能体没有可发布的版本。')
    return
  }
  actionPending.value = 'publish'
  try {
    await publishAgent(selectedAgentDetail.value.agentId, latestVersion.value.versionNo)
    await Promise.all([
      loadAgents({ keepSelection: true, successMessage: `已发布 v${latestVersion.value.versionNo}。` }),
      loadAgentDetail(selectedAgentDetail.value.agentId),
    ])
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '发布智能体失败。'))
  } finally {
    actionPending.value = null
  }
}

async function handleDisable() {
  if (!selectedAgentDetail.value) return
  actionPending.value = 'disable'
  try {
    await disableAgent(selectedAgentDetail.value.agentId)
    await loadAgents({ keepSelection: true, successMessage: '智能体已停用。' })
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '停用智能体失败。'))
  } finally {
    actionPending.value = null
  }
}

async function handleDelete() {
  if (!selectedAgentDetail.value) return
  actionPending.value = 'delete'
  try {
    const { agentId, agentName } = selectedAgentDetail.value
    await removeAgent(agentId)
    selectedAgentDetail.value = null
    await loadAgents({ successMessage: `智能体 ${agentName} 已删除。` })
    if (editingAgentId.value === agentId) {
      enterCreateMode()
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '删除智能体失败。'))
  } finally {
    actionPending.value = null
  }
}

async function handleCreateSession(versionNo?: number) {
  if (!selectedAgentDetail.value) return
  actionPending.value = 'session'
  try {
    createdSession.value = await createAgentSession(selectedAgentDetail.value.agentId, versionNo ? { versionNo } : {})
    setFeedback('success', `会话 ${createdSession.value.sessionId} 已创建。`)
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '创建会话失败。'))
  } finally {
    actionPending.value = null
  }
}

async function handleOpenChat(versionNo?: number) {
  if (!selectedAgentDetail.value) return
  actionPending.value = 'session'
  try {
    const targetVersionNo = versionNo ?? (selectedAgentDetail.value.agentStatus !== 'PUBLISHED' ? latestVersion.value?.versionNo : undefined)
    const session = await createAgentSession(selectedAgentDetail.value.agentId, targetVersionNo ? { versionNo: targetVersionNo } : {})
    createdSession.value = session
    await router.push({
      name: 'agent-chat',
      params: { agentId: selectedAgentDetail.value.agentId },
      query: { sessionId: session.sessionId, versionNo: String(session.agentVersionNo) },
    })
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '创建聊天会话失败。'))
  } finally {
    actionPending.value = null
  }
}

async function handleBatchMigrateModel() {
  if (!form.modelConfigCode) {
    setFeedback('error', '请先在左侧选择一个目标模型。')
    return
  }
  if (selectedAgentIds.value.length === 0) {
    setFeedback('error', '请至少勾选一个智能体。')
    return
  }
  batchMigrating.value = true
  try {
    await batchMigrateAgentModels({
      agentIds: [...selectedAgentIds.value],
      targetModelConfigCode: form.modelConfigCode,
      migrationMode: migrationMode.value,
    })
    await loadAgents({
      keepSelection: true,
      successMessage: migrationMode.value === 'PUBLISH_NEW_VERSION'
        ? `已批量迁移并发布 ${selectedAgentIds.value.length} 个智能体的新版本。`
        : `已批量迁移 ${selectedAgentIds.value.length} 个智能体的草稿版本模型绑定。`,
    })
    if (selectedAgentId.value) {
      await loadAgentDetail(selectedAgentId.value)
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '批量迁移模型失败。'))
  } finally {
    batchMigrating.value = false
  }
}

async function handleRunDocumentExpertAgent() {
  if (!customDocumentForm.modelCode) {
    setFeedback('error', '请选择文档专家默认模型。')
    return
  }
  if (!customDocumentForm.userPrompt.trim()) {
    setFeedback('error', '请输入文档专家需求。')
    return
  }
  customAgentRunning.value = true
  customAgentResult.value = null
  try {
    customAgentResult.value = await runDocumentExpertAgent({
      modelCode: customDocumentForm.modelCode,
      routeModelCode: customDocumentForm.routeModelCode || null,
      enhancementModelCode: customDocumentForm.enhancementModelCode || null,
      generationAModelCode: customDocumentForm.generationAModelCode || null,
      generationBModelCode: customDocumentForm.generationBModelCode || null,
      auditModelCode: customDocumentForm.auditModelCode || null,
      fusionModelCode: customDocumentForm.fusionModelCode || null,
      userPrompt: customDocumentForm.userPrompt.trim(),
      autoFillMissingInfo: customDocumentForm.autoFillMissingInfo,
    })
    setFeedback('success', customAgentResult.value.clarificationNeeded ? '文档专家已返回补充问题。' : '文档专家执行完成。')
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '执行文档专家智能体失败。'))
  } finally {
    customAgentRunning.value = false
  }
}

function formatStageStatus(status?: string | null) {
  if (status === 'PASSED') return '通过'
  if (status === 'REJECTED') return '拒绝'
  if (status === 'NEED_CLARIFICATION') return '待补充'
  if (status === 'WARNING') return '警告'
  if (status === 'COMPLETED') return '完成'
  return status || '-'
}

function customStageList(result: CustomAgentDocumentExpertResult | null): CustomAgentStageResult[] {
  if (!result) return []
  return [
    result.routeStage,
    result.enhancementStage,
    result.generationStageA,
    result.generationStageB,
    result.auditStage,
    result.fusionStage,
  ].filter(Boolean) as CustomAgentStageResult[]
}

onMounted(() => {
  void Promise.all([loadAgents(), loadPromptTemplates(), loadHooks(), loadMcpServers(), loadModels(), loadCustomAgentModels()])
})
</script>

<template>
  <MainShell>
    <AppFeedbackDialog
      :model-value="Boolean(feedback)"
      :tone="feedbackTone"
      :message="feedback"
      @update:model-value="!$event && (feedback = '')"
    />

    <section class="management-page agent-page">
      <header class="page__hero panel-card management-hero">
        <div>
          <p class="section-kicker">智能体工作台</p>
          <h2>智能体配置台</h2>
          <p class="page__meta">{{ totalAgentsLabel }}，{{ publishedCountLabel }}</p>
        </div>
        <button class="app-button app-button--secondary" :disabled="loading || detailLoading" @click="loadAgents({ keepSelection: true })">
          <RefreshCw :size="16" />
          刷新
        </button>
      </header>

      <div class="page__grid">
        <article ref="formPanelRef" class="card-section panel-card">
          <div class="section-head">
            <div>
              <strong>{{ formTitle }}</strong>
              <p class="muted">支持模板提示词、内联提示词和文件路径三种绑定方式。</p>
            </div>
            <button v-if="formMode === 'edit'" class="app-button app-button--ghost" @click="enterCreateMode">
              切回创建
            </button>
          </div>

          <div class="section-grid">
            <label class="field">
              <span class="field__label">智能体名称</span>
              <div class="input-shell">
                <input v-model="form.agentName" class="app-input" type="text" placeholder="例如：工单助手" />
              </div>
            </label>

            <label class="field section-grid__full">
              <span class="field__label">描述</span>
              <div class="input-shell input-shell--textarea">
                <textarea v-model="form.description" class="app-textarea" rows="3" placeholder="描述智能体的职责边界、目标用户和输出风格。" />
              </div>
            </label>

            <div class="field section-grid__full">
              <span class="field__label">提示词来源</span>
              <div class="mode-grid">
                <label class="mode-pill" :class="{ 'mode-pill--active': form.promptMode === 'template' }">
                  <input v-model="form.promptMode" type="radio" value="template" />模板
                </label>
                <label class="mode-pill" :class="{ 'mode-pill--active': form.promptMode === 'custom-inline' }">
                  <input v-model="form.promptMode" type="radio" value="custom-inline" />自定义文本
                </label>
                <label class="mode-pill" :class="{ 'mode-pill--active': form.promptMode === 'custom-file' }">
                  <input v-model="form.promptMode" type="radio" value="custom-file" />文件路径
                </label>
              </div>
            </div>

            <label v-if="form.promptMode === 'template'" class="field section-grid__full">
              <span class="field__label">提示词模板</span>
              <select v-model="form.selectedPromptTemplateId" class="app-select" :disabled="promptTemplatesLoading">
                <option value="">{{ promptTemplatesLoading ? '正在加载模板...' : '请选择模板' }}</option>
                <option v-for="template in promptTemplates" :key="template.id" :value="String(template.id)">
                  {{ template.templateName }} / {{ template.templateCode }}
                </option>
              </select>
            </label>

            <label v-else-if="form.promptMode === 'custom-inline'" class="field section-grid__full">
              <span class="field__label">系统提示词文本</span>
              <div class="input-shell input-shell--textarea">
                <textarea v-model="form.customPromptContent" class="app-textarea" rows="8" placeholder="直接填写系统提示词内容。" />
              </div>
            </label>

            <label v-else class="field section-grid__full">
              <span class="field__label">提示词文件路径</span>
              <div class="input-shell">
                <input v-model="form.customPromptPath" class="app-input" type="text" placeholder="例如 D:/code/springAi/prompts/customer-service.md" />
              </div>
            </label>

            <section v-if="form.promptMode === 'template' && selectedPromptVariables.length > 0" class="section-grid__full variable-panel">
              <div class="section-head">
                <div>
                  <strong>模板变量</strong>
                  <p class="muted">根据模板变量元数据动态生成输入表单。</p>
                </div>
              </div>
              <div class="variable-grid">
                <label v-for="variable in selectedPromptVariables" :key="variable.variableName" class="field variable-card">
                  <div class="variable-card__head">
                    <strong>{{ variable.variableName }}</strong>
                    <span class="variable-badge">{{ variable.required ? '必填' : '可选' }}</span>
                  </div>
                  <div class="input-shell">
                    <input
                      v-model="form.promptVariableValues[variable.variableName]"
                      class="app-input"
                      type="text"
                      :placeholder="variable.defaultValue || '请输入变量值'"
                    />
                  </div>
                  <small class="muted">
                    {{ variable.description || '未配置业务说明' }}
                    <template v-if="variable.defaultValue"> · 默认值：{{ variable.defaultValue }}</template>
                  </small>
                </label>
              </div>
            </section>

            <label class="field section-grid__full">
              <span class="field__label">能力标签</span>
              <div class="input-shell input-shell--textarea">
                <textarea v-model="form.selectedCapabilitiesText" class="app-textarea" rows="4" placeholder="使用逗号或换行分隔多个能力标签。" />
              </div>
            </label>

            <section class="section-grid__full hook-panel">
              <div class="section-head">
                <div>
                  <strong>钩子绑定</strong>
                  <p class="muted">仅展示已发布且启用的钩子，版本快照会固化这份选择。</p>
                </div>
                <small class="muted">{{ hooksLoading ? '正在加载钩子...' : `可选 ${availableHooks.length} 个` }}</small>
              </div>
              <div v-if="availableHooks.length === 0" class="empty empty--compact">暂无可绑定的钩子。</div>
              <div v-else class="hook-grid">
                <label
                  v-for="hook in availableHooks"
                  :key="hook.hookCode"
                  class="hook-card"
                  :class="{ 'hook-card--active': form.selectedHookCodes.includes(hook.hookCode) }"
                >
                  <input v-model="form.selectedHookCodes" type="checkbox" :value="hook.hookCode" />
                  <div class="hook-card__head">
                    <strong>{{ hook.hookName }}</strong>
                    <span class="version-pill">{{ formatHookStage(hook.hookStage) }}</span>
                  </div>
                  <p class="muted">{{ hook.description || hook.hookCode }}</p>
                  <small class="muted">风险：{{ hook.riskLevel }} · 失败策略：{{ hook.failStrategy }}</small>
                </label>
              </div>
            </section>

            <section class="section-grid__full hook-panel">
              <div class="section-head">
                <div>
                  <strong>MCP 绑定</strong>
                  <p class="muted">仅展示已发布且启用的 MCP 服务，智能体运行时按版本快照挂载这些服务。</p>
                </div>
                <small class="muted">{{ mcpLoading ? '正在加载 MCP...' : `可选 ${availableMcpServers.length} 个` }}</small>
              </div>
              <div v-if="availableMcpServers.length === 0" class="empty empty--compact">暂无可绑定的 MCP 服务。</div>
              <div v-else class="hook-grid">
                <label
                  v-for="server in availableMcpServers"
                  :key="server.serverId"
                  class="hook-card"
                  :class="{ 'hook-card--active': form.selectedMcpServerIds.includes(String(server.serverId)) }"
                >
                  <input v-model="form.selectedMcpServerIds" type="checkbox" :value="String(server.serverId)" />
                  <div class="hook-card__head">
                    <strong>{{ server.serverName }}</strong>
                    <span class="version-pill">{{ server.transportType }}</span>
                  </div>
                  <p class="muted">{{ server.description || server.serverCode }}</p>
                  <small class="muted">类型：{{ server.serverType }} · 风险：{{ server.riskLevel }}</small>
                </label>
              </div>
            </section>

            <label class="field section-grid__full">
              <span class="field__label">绑定模型</span>
              <select v-model="form.modelConfigCode" class="app-select" :disabled="modelsLoading">
                <option value="">{{ modelsLoading ? '正在加载模型...' : '请选择模型配置' }}</option>
                <option v-for="model in availableModels" :key="model.modelCode" :value="model.modelCode">
                  {{ model.modelName }} / {{ model.providerName || model.providerEnum }} / {{ model.modelIdentifier }}
                </option>
              </select>
              <small v-if="selectedModel" class="muted">
                当前选择：{{ selectedModel.modelType }} · {{ selectedModel.providerName || selectedModel.providerEnum }}
                <template v-if="selectedModel.defaultModel"> · 默认模型</template>
              </small>
            </label>
          </div>

          <div class="chip-row">
            <span v-for="capability in capabilityPreview" :key="capability" class="chip">{{ capability }}</span>
          </div>

          <div v-if="form.selectedHookCodes.length > 0" class="chip-row">
            <span v-for="hookCode in form.selectedHookCodes" :key="hookCode" class="chip chip--hook">{{ hookCode }}</span>
          </div>

          <div v-if="form.selectedMcpServerIds.length > 0" class="chip-row">
            <span v-for="serverId in form.selectedMcpServerIds" :key="serverId" class="chip chip--mcp">
              {{ mcpServers.find((item) => String(item.serverId) === serverId)?.serverName || `MCP#${serverId}` }}
            </span>
          </div>

          <button class="app-button full-width" :disabled="submitting" @click="handleSubmit">
            <Plus v-if="!submitting && formMode === 'create'" :size="16" />
            {{ submitting ? '提交中...' : formMode === 'create' ? '创建智能体' : '保存并生成新版本' }}
          </button>
        </article>

        <article class="card-section panel-card">
          <div class="section-head">
            <div>
              <strong>智能体列表</strong>
              <p class="muted">按名称、描述和状态搜索。</p>
            </div>
          </div>

          <div class="toolbar">
            <label class="field">
              <span class="field__label">搜索</span>
              <div class="input-shell">
                <span class="input-shell__icon"><Search :size="16" /></span>
                <input v-model="filters.keyword" class="app-input" type="text" placeholder="搜索智能体名称或描述" />
              </div>
            </label>
            <label class="field">
              <span class="field__label">状态</span>
              <select v-model="filters.status" class="app-select">
                <option value="ALL">全部</option>
                <option value="DRAFT">草稿中</option>
                <option value="PUBLISHED">已发布</option>
                <option value="DISABLED">已停用</option>
              </select>
            </label>
            <label class="field">
              <span class="field__label">模型筛选</span>
              <select v-model="filters.modelCode" class="app-select">
                <option value="">全部模型</option>
                <option v-for="model in availableModels" :key="model.modelCode" :value="model.modelCode">
                  {{ model.modelName }}
                </option>
              </select>
            </label>
          </div>

          <div class="action-row action-row--stacked list-card-actions">
            <label class="field field--compact">
              <span class="field__label">迁移模式</span>
              <select v-model="migrationMode" class="app-select">
                <option value="DRAFT_ONLY">仅迁移草稿版本</option>
                <option value="PUBLISH_NEW_VERSION">同时生成并发布新版本</option>
              </select>
            </label>
            <button class="app-button app-button--secondary" :disabled="batchMigrating" @click="handleBatchMigrateModel">
              {{ batchMigrating ? '迁移中...' : `批量迁移到当前模型 (${selectedAgentIds.length})` }}
            </button>
          </div>

          <div v-if="loading" class="empty">正在加载智能体列表...</div>
          <div v-else-if="filteredAgents.length === 0" class="empty">当前筛选条件下没有智能体。</div>
          <div v-else class="stack">
            <button
              v-for="agent in filteredAgents"
              :key="agent.agentId"
              class="list-item"
              :class="{ 'list-item--active': selectedAgentId === agent.agentId }"
              @click="selectedAgentId = agent.agentId"
            >
              <label class="checkbox-line" @click.stop>
                <input v-model="selectedAgentIds" type="checkbox" :value="agent.agentId" />
                <span>加入批量迁移</span>
              </label>
              <div class="list-item__head">
                <strong>{{ agent.agentName }}</strong>
                <span class="status-pill" :class="`status-pill--${agent.agentStatus.toLowerCase()}`">{{ formatStatus(agent.agentStatus) }}</span>
              </div>
              <p class="muted">{{ agent.description || '暂无描述' }}</p>
              <small class="muted">模型：{{ agent.modelName || '未绑定模型' }} {{ agent.providerName ? ` / ${agent.providerName}` : '' }}</small>
              <small class="muted">当前 v{{ agent.currentVersionNo ?? '-' }} / 发布 v{{ agent.publishedVersionNo ?? '-' }}</small>
            </button>
          </div>
        </article>
      </div>

      <div class="page__grid page__grid--bottom">
        <article class="card-section panel-card">
          <div class="section-head">
            <div>
              <strong>智能体详情</strong>
              <p class="muted">查看版本快照、提示词绑定方式与变量值。</p>
            </div>
            <div v-if="selectedAgentDetail" class="action-row">
              <button class="app-button app-button--secondary" :disabled="!canEditSelectedAgent || detailLoading" @click="enterEditMode">
                编辑
              </button>
              <button class="app-button" :disabled="!canPublishLatest" @click="handlePublishLatest">
                <Rocket :size="15" />发布
              </button>
              <button class="app-button app-button--ghost" :disabled="selectedAgentDetail.agentStatus === 'DISABLED' || actionPending !== null" @click="handleDisable">
                <ShieldBan :size="15" />停用
              </button>
              <button class="app-button app-button--ghost app-button--danger-ghost" :disabled="actionPending !== null" @click="handleDelete">
                <Trash2 :size="15" />删除
              </button>
            </div>
          </div>

          <div v-if="detailLoading" class="empty">正在加载智能体详情...</div>
          <div v-else-if="!selectedAgentDetail" class="empty">请选择一个智能体查看详情。</div>
          <div v-else class="stack">
            <article class="summary-card">
              <div class="list-item__head">
                <strong>{{ selectedAgentDetail.agentName }}</strong>
                <span class="status-pill" :class="`status-pill--${selectedAgentDetail.agentStatus.toLowerCase()}`">
                  {{ formatStatus(selectedAgentDetail.agentStatus) }}
                </span>
              </div>
              <p class="muted">{{ selectedAgentDetail.description || '暂无描述' }}</p>
            </article>

            <article v-for="version in sortedVersions" :key="version.versionId" class="version-card">
              <div class="list-item__head">
                <div>
                  <strong>版本 v{{ version.versionNo }}</strong>
                  <p class="muted">{{ formatTime(version.createTime) }}</p>
                </div>
                <span class="version-pill">{{ version.published ? '已发布' : '草稿快照' }}</span>
              </div>
              <p class="muted">提示词绑定：{{ formatPromptBinding(version) }}</p>
              <div class="chip-row">
                <span v-for="capability in version.selectedCapabilities" :key="`${version.versionId}-${capability}`" class="chip">
                  {{ capability }}
                </span>
              </div>
              <div v-if="version.selectedHookCodes?.length" class="chip-row">
                <span v-for="hookCode in version.selectedHookCodes" :key="`${version.versionId}-${hookCode}`" class="chip chip--hook">
                  {{ hookCode }}
                </span>
              </div>
              <div v-if="version.selectedMcpServerIds?.length" class="chip-row">
                <span v-for="serverId in version.selectedMcpServerIds" :key="`${version.versionId}-${serverId}`" class="chip chip--mcp">
                  {{ mcpServers.find((item) => String(item.serverId) === serverId)?.serverName || `MCP#${serverId}` }}
                </span>
              </div>
              <p class="muted">模型绑定：{{ formatModelBinding(version) }}</p>
              <div v-if="version.promptVariableDefinitions?.length" class="variable-grid variable-grid--compact">
                <article v-for="variable in version.promptVariableDefinitions" :key="`${version.versionId}-${variable.variableName}`" class="variable-card">
                  <div class="variable-card__head">
                    <strong>{{ variable.variableName }}</strong>
                    <span class="variable-badge">{{ variable.required ? '必填' : '可选' }}</span>
                  </div>
                  <small class="muted">{{ variable.description || '未配置说明' }}</small>
                  <code class="code-line">{{ version.promptVariables?.[variable.variableName] || variable.defaultValue || '-' }}</code>
                </article>
              </div>
              <pre class="prompt-preview">{{ version.systemPrompt || '暂无系统提示词' }}</pre>
              <div class="action-row">
                <button class="app-button app-button--ghost" :disabled="actionPending !== null" @click="handleCreateSession(version.versionNo)">
                  创建会话
                </button>
                <button class="app-button app-button--secondary" :disabled="actionPending !== null" @click="handleOpenChat(version.versionNo)">
                  进入聊天
                </button>
              </div>
            </article>
          </div>
        </article>

        <aside class="card-section panel-card">
          <div class="section-head">
            <div>
              <strong>会话入口</strong>
              <p class="muted">从当前智能体直接进入联调聊天流程。</p>
            </div>
          </div>

          <div v-if="!selectedAgentDetail" class="empty">选择智能体后即可创建会话。</div>
          <div v-else class="stack">
            <button class="app-button full-width" :disabled="actionPending !== null" @click="handleCreateSession()">
              <MessageSquareText :size="16" />
              创建默认会话
            </button>
            <button class="app-button app-button--secondary full-width" :disabled="actionPending !== null" @click="handleOpenChat()">
              直接进入聊天页
            </button>
            <article v-if="createdSession" class="summary-card">
              <strong>{{ createdSession.sessionId }}</strong>
              <p class="muted">绑定智能体：{{ createdSession.agentId }}</p>
              <p class="muted">版本号：v{{ createdSession.agentVersionNo }}</p>
              <p class="muted">连接状态：{{ createdSession.connectionStatus }}</p>
            </article>
          </div>
        </aside>
      </div>

      <div class="page__grid page__grid--bottom">
        <article class="card-section panel-card">
          <div class="section-head">
            <div>
              <strong>自定义智能体</strong>
              <p class="muted">内置场景化能力，无需先创建智能体即可直接调用。</p>
            </div>
          </div>

          <section class="summary-card">
            <div class="section-head">
              <div>
                <strong>文档专家 Agent</strong>
                <p class="muted">支持路由校验、提示词增强、双文档并行生成、审核与融合分阶段独立选模型。</p>
              </div>
              <button class="app-button app-button--ghost" :disabled="customAgentRunning" @click="resetCustomDocumentForm">
                重置
              </button>
            </div>

            <div class="section-grid">
              <label class="field">
                <span class="field__label">默认模型</span>
                <select v-model="customDocumentForm.modelCode" class="app-select">
                  <option value="">请选择默认模型</option>
                  <option v-for="model in customAgentModels" :key="model.modelCode" :value="model.modelCode">
                    {{ model.modelName }} / {{ model.providerName || model.providerEnum }}
                  </option>
                </select>
              </label>

              <label class="field">
                <span class="field__label">路由校验模型</span>
                <select v-model="customDocumentForm.routeModelCode" class="app-select">
                  <option value="">跟随默认模型</option>
                  <option v-for="model in customAgentModels" :key="`route-${model.modelCode}`" :value="model.modelCode">
                    {{ model.modelName }} / {{ model.providerName || model.providerEnum }}
                  </option>
                </select>
              </label>

              <label class="field">
                <span class="field__label">提示增强模型</span>
                <select v-model="customDocumentForm.enhancementModelCode" class="app-select">
                  <option value="">跟随默认模型</option>
                  <option v-for="model in customAgentModels" :key="`enhance-${model.modelCode}`" :value="model.modelCode">
                    {{ model.modelName }} / {{ model.providerName || model.providerEnum }}
                  </option>
                </select>
              </label>

              <label class="field">
                <span class="field__label">生成 A 模型</span>
                <select v-model="customDocumentForm.generationAModelCode" class="app-select">
                  <option value="">跟随默认模型</option>
                  <option v-for="model in customAgentModels" :key="`ga-${model.modelCode}`" :value="model.modelCode">
                    {{ model.modelName }} / {{ model.providerName || model.providerEnum }}
                  </option>
                </select>
              </label>

              <label class="field">
                <span class="field__label">生成 B 模型</span>
                <select v-model="customDocumentForm.generationBModelCode" class="app-select">
                  <option value="">跟随默认模型</option>
                  <option v-for="model in customAgentModels" :key="`gb-${model.modelCode}`" :value="model.modelCode">
                    {{ model.modelName }} / {{ model.providerName || model.providerEnum }}
                  </option>
                </select>
              </label>

              <label class="field">
                <span class="field__label">审核模型</span>
                <select v-model="customDocumentForm.auditModelCode" class="app-select">
                  <option value="">跟随默认模型</option>
                  <option v-for="model in customAgentModels" :key="`audit-${model.modelCode}`" :value="model.modelCode">
                    {{ model.modelName }} / {{ model.providerName || model.providerEnum }}
                  </option>
                </select>
              </label>

              <label class="field">
                <span class="field__label">融合模型</span>
                <select v-model="customDocumentForm.fusionModelCode" class="app-select">
                  <option value="">跟随默认模型</option>
                  <option v-for="model in customAgentModels" :key="`fusion-${model.modelCode}`" :value="model.modelCode">
                    {{ model.modelName }} / {{ model.providerName || model.providerEnum }}
                  </option>
                </select>
              </label>

              <label class="field field--inline">
                <span class="field__label">自动补全缺失信息</span>
                <input v-model="customDocumentForm.autoFillMissingInfo" type="checkbox" />
              </label>

              <label class="field section-grid__full">
                <span class="field__label">文档需求</span>
                <div class="input-shell input-shell--textarea">
                  <textarea
                    v-model="customDocumentForm.userPrompt"
                    class="app-textarea"
                    rows="6"
                    placeholder="例如：为管理层写一份数据中台建设实施方案，突出目标、路径、风险与资源安排。"
                  />
                </div>
              </label>
            </div>

            <small v-if="selectedCustomDefaultModel" class="muted">
              默认模型：{{ selectedCustomDefaultModel.modelName }} / {{ selectedCustomDefaultModel.providerName || selectedCustomDefaultModel.providerEnum }}
            </small>

            <button class="app-button full-width" :disabled="customAgentRunning" @click="handleRunDocumentExpertAgent">
              {{ customAgentRunning ? '执行中...' : '运行文档专家 Agent' }}
            </button>
          </section>
        </article>

        <aside class="card-section panel-card">
          <div class="section-head">
            <div>
              <strong>执行结果</strong>
              <p class="muted">展示自定义智能体各阶段输出与最终成稿。</p>
            </div>
          </div>

          <div v-if="!customAgentResult" class="empty">运行文档专家 Agent 后，这里会展示阶段结果。</div>
          <div v-else class="stack">
            <article class="summary-card">
              <strong>默认模型：{{ customAgentResult.modelCode }}</strong>
              <p v-if="customAgentResult.clarificationNeeded" class="muted">
                需要补充信息：{{ customAgentResult.clarificationQuestion || '请补充必要信息。' }}
              </p>
              <p v-else class="muted">已完成文档专家链路处理。</p>
            </article>

            <article v-for="stage in customStageList(customAgentResult)" :key="stage.stageName" class="version-card">
              <div class="list-item__head">
                <strong>{{ stage.stageName }}</strong>
                <span class="version-pill">{{ formatStageStatus(stage.status) }}</span>
              </div>
              <p class="muted">模型：{{ stage.modelCode || customAgentResult.modelCode }}</p>
              <p class="muted">{{ stage.summary }}</p>
              <div v-if="stage.issues?.length" class="chip-row">
                <span v-for="issue in stage.issues" :key="`${stage.stageName}-${issue}`" class="chip chip--hook">{{ issue }}</span>
              </div>
              <pre class="prompt-preview">{{ stage.content || '暂无阶段内容' }}</pre>
            </article>

            <article v-if="customAgentResult.finalDocument" class="summary-card">
              <div class="section-head">
                <div>
                  <strong>最终文档</strong>
                  <p class="muted">已融合汇总后的最终成稿。</p>
                </div>
              </div>
              <pre class="prompt-preview">{{ customAgentResult.finalDocument }}</pre>
            </article>
          </div>
        </aside>
      </div>
    </section>
  </MainShell>
</template>
<style scoped>
.agent-page {
  display: grid;
  gap: var(--layout-gap);
  min-width: 0;
  min-height: 100%;
}
.page__hero,
.section-head,
.list-item__head,
.action-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.page__hero {
  padding: var(--panel-padding);
  min-width: 0;
}
.page__meta,
.muted {
  color: var(--color-ink-soft);
  line-height: 1.6;
}
.page__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(360px, 0.92fr);
  gap: var(--layout-gap);
  align-items: start;
  min-width: 0;
}
.page__grid--bottom {
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(360px, 0.92fr);
  gap: var(--layout-gap);
  align-items: start;
  min-width: 0;
}
.card-section,
.list-item,
.summary-card,
.version-card,
.hook-card,
.variable-panel,
.variable-card,
.empty {
  display: grid;
  gap: 14px;
  align-content: start;
  min-width: 0;
  padding: var(--compact-panel-padding);
  border-radius: var(--sub-panel-radius);
  background: rgba(255, 255, 255, 0.04);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}
.card-section {
  min-height: 0;
  overflow: visible;
}
.section-grid,
.toolbar,
.variable-grid,
.mode-grid,
.hook-grid {
  display: grid;
  gap: 16px;
}
.section-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.section-grid__full {
  grid-column: 1 / -1;
}
.toolbar {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: end;
}
.mode-grid,
.variable-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.hook-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.variable-grid--compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.mode-pill {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 54px;
  padding: 10px 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
  line-height: 1.4;
  text-align: center;
}
.mode-pill--active {
  color: var(--color-ink-strong);
  border-color: rgba(77, 179, 255, 0.28);
  background: rgba(77, 179, 255, 0.16);
}
.mode-pill input {
  position: absolute;
  opacity: 0;
}
.stack,
.chip-row {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}
.chip-row {
  flex-direction: row;
  flex-wrap: wrap;
}
.stack {
  min-height: 0;
}
.stack > * {
  min-width: 0;
}
.chip,
.status-pill,
.variable-badge,
.version-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1.4;
}
.chip {
  color: #d8f2ff;
  background: rgba(77, 179, 255, 0.16);
}
.chip--hook {
  color: #ffe6b8;
  background: rgba(255, 176, 86, 0.16);
}
.chip--mcp {
  color: #d8fff6;
  background: rgba(72, 201, 176, 0.18);
}
.status-pill--published,
.version-pill {
  color: #d7ffef;
  background: rgba(100, 216, 190, 0.18);
}
.status-pill--draft {
  color: #dce8ff;
  background: rgba(114, 151, 255, 0.18);
}
.status-pill--disabled {
  color: #ffd2d6;
  background: rgba(255, 144, 151, 0.18);
}
.variable-badge {
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.08);
}
.variable-card__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}
.hook-card {
  position: relative;
  cursor: pointer;
}
.hook-card input {
  position: absolute;
  inset: 0;
  opacity: 0;
}
.hook-card--active {
  box-shadow:
    inset 0 0 0 1px rgba(255, 176, 86, 0.38),
    0 16px 28px rgba(255, 176, 86, 0.12);
}
.hook-card__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}
.checkbox-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--color-ink-soft);
  font-size: 0.8rem;
  line-height: 1.45;
}
.checkbox-line span {
  line-height: 1.45;
}
.empty--compact {
  padding: 14px 16px;
}

.field--compact {
  flex: 1 1 260px;
  min-width: min(100%, 260px);
}
.prompt-preview,
.code-line {
  margin: 0;
  padding: 12px 14px;
  border-radius: 18px;
  color: var(--color-ink-soft);
  background: rgba(4, 17, 29, 0.58);
  white-space: pre-wrap;
  word-break: break-word;
}
.full-width {
  width: 100%;
}
.section-head > div:first-child,
.list-item__head > div:first-child,
.action-row > div:first-child {
  min-width: 0;
}
.section-head strong {
  display: inline-block;
  line-height: 1.35;
  padding-block: 2px;
}
.card-section > .section-head:first-child {
  margin-top: 2px;
  padding-top: 2px;
}
.section-head .app-button,
.action-row .app-button {
  flex: 0 0 auto;
}
.action-row--stacked {
  flex-direction: column;
  align-items: stretch;
}
.list-card-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.list-card-actions .app-button {
  width: 100%;
}
.toolbar .field:first-child {
  grid-column: 1 / -1;
}
.toolbar .field {
  min-width: 0;
}
.list-item {
  overflow: visible;
}
.list-item__head strong,
.summary-card strong,
.version-card strong {
  line-height: 1.35;
}
.status-pill {
  flex-shrink: 0;
}
.feedback {
  padding: 14px 16px;
  border-radius: 18px;
}
.feedback--success {
  color: #d7ffef;
  background: rgba(100, 216, 190, 0.16);
}
.feedback--error {
  color: #ffd2d6;
  background: rgba(255, 144, 151, 0.16);
}
.feedback--info {
  color: #d8f2ff;
  background: rgba(77, 179, 255, 0.16);
}
@media (max-width: 1320px) {
  .page__grid,
  .page__grid--bottom,
  .section-grid,
  .toolbar,
  .mode-grid,
  .variable-grid,
  .hook-grid,
  .variable-grid--compact {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 720px) {
  .page__hero,
  .section-head,
  .list-item__head,
  .action-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
