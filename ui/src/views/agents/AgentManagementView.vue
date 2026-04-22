<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { MessageSquareText, Plus, RefreshCw, Rocket, Search, ShieldBan, Trash2 } from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import { createAgent, createAgentSession, disableAgent, fetchAgentDetail, publishAgent, queryAgents, removeAgent, updateAgent } from '@/api/agent'
import { queryHooks } from '@/api/hook'
import { queryPromptTemplates } from '@/api/prompt'
import type { AgentCreatePayload, AgentDetail, AgentPromptConfig, AgentSessionResult, AgentSummary, AgentVersion } from '@/types/agent'
import type { HookItem } from '@/types/hook'
import type { PromptTemplateItem, PromptTemplateVariable } from '@/types/prompt'
import { getErrorMessage } from '@/utils/errors'

type PromptMode = 'template' | 'custom-inline' | 'custom-file'
type FormMode = 'create' | 'edit'
type PendingAction = 'publish' | 'disable' | 'session' | 'delete' | null

const router = useRouter()
const loading = ref(false)
const detailLoading = ref(false)
const promptTemplatesLoading = ref(false)
const hooksLoading = ref(false)
const submitting = ref(false)
const actionPending = ref<PendingAction>(null)
const feedback = ref('')
const feedbackTone = ref<'success' | 'error' | 'info'>('info')
const agents = ref<AgentSummary[]>([])
const promptTemplates = ref<PromptTemplateItem[]>([])
const hooks = ref<HookItem[]>([])
const selectedAgentId = ref('')
const selectedAgentDetail = ref<AgentDetail | null>(null)
const createdSession = ref<AgentSessionResult | null>(null)
const formMode = ref<FormMode>('create')
const editingAgentId = ref<string | null>(null)
const formPanelRef = ref<HTMLElement | null>(null)

const filters = reactive({ keyword: '', status: 'ALL' })
const form = reactive({
  agentName: '',
  description: '',
  selectedCapabilitiesText: 'knowledge_search, session_management, failover_recovery',
  selectedHookCodes: [] as string[],
  promptMode: 'template' as PromptMode,
  selectedPromptTemplateId: '',
  customPromptContent: '',
  customPromptPath: '',
  promptVariableValues: {} as Record<string, string>,
})

const filteredAgents = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return agents.value.filter((item) => {
    const matchesKeyword = !keyword || [item.agentName, item.description, item.ownerUserName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
    const matchesStatus = filters.status === 'ALL' || item.agentStatus === filters.status
    return matchesKeyword && matchesStatus
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
  form.promptMode = 'template'
  form.selectedPromptTemplateId = promptTemplates.value[0] ? String(promptTemplates.value[0].id) : ''
  form.customPromptContent = ''
  form.customPromptPath = ''
  form.promptVariableValues = {}
  syncPromptVariableValues()
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
    agentType: 'REACT',
    promptConfig: buildPromptConfig(),
  }
}

function formatStatus(status: string) {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'DISABLED') return '已停用'
  return '草稿中'
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

async function loadAgents(options?: { keepSelection?: boolean; successMessage?: string }) {
  loading.value = true
  try {
    const result = await queryAgents()
    agents.value = result
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

onMounted(() => {
  void Promise.all([loadAgents(), loadPromptTemplates(), loadHooks()])
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
          </div>

          <div class="chip-row">
            <span v-for="capability in capabilityPreview" :key="capability" class="chip">{{ capability }}</span>
          </div>

          <div v-if="form.selectedHookCodes.length > 0" class="chip-row">
            <span v-for="hookCode in form.selectedHookCodes" :key="hookCode" class="chip chip--hook">{{ hookCode }}</span>
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
              <div class="list-item__head">
                <strong>{{ agent.agentName }}</strong>
                <span class="status-pill" :class="`status-pill--${agent.agentStatus.toLowerCase()}`">{{ formatStatus(agent.agentStatus) }}</span>
              </div>
              <p class="muted">{{ agent.description || '暂无描述' }}</p>
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
    </section>
  </MainShell>
</template>
<style scoped>
.agent-page {
  display: grid;
  gap: 22px;
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
}
.page__hero {
  align-items: flex-start;
  padding: 24px 26px;
  min-width: 0;
}
.page__meta,
.muted {
  color: var(--color-ink-soft);
}
.page__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.78fr);
  gap: 22px;
  align-items: start;
  min-width: 0;
}
.page__grid--bottom {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.78fr);
  gap: 22px;
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
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.04);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}
.card-section {
  min-height: 0;
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
  grid-template-columns: 1fr 180px;
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
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
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
}
.chip {
  color: #d8f2ff;
  background: rgba(77, 179, 255, 0.16);
}
.chip--hook {
  color: #ffe6b8;
  background: rgba(255, 176, 86, 0.16);
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
.empty--compact {
  padding: 14px 16px;
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
@media (max-width: 1080px) {
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
