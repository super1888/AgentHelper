<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import MainShell from '@/components/MainShell.vue'
import { createAgent, createAgentSession, disableAgent, fetchAgentDetail, publishAgent, queryAgents, removeAgent, updateAgent } from '@/api/agent'
import { queryPromptTemplates } from '@/api/prompt'
import type { AgentCreatePayload, AgentDetail, AgentPromptConfig, AgentSessionResult, AgentSummary, AgentVersion } from '@/types/agent'
import type { PromptTemplateItem } from '@/types/prompt'
import { getErrorMessage } from '@/utils/errors'

type PromptMode = 'template' | 'custom-inline' | 'custom-file'
type FormMode = 'create' | 'edit'
type PendingAction = 'publish' | 'disable' | 'session' | 'delete' | null

const router = useRouter()
const loading = ref(false)
const detailLoading = ref(false)
const promptTemplatesLoading = ref(false)
const submitting = ref(false)
const actionPending = ref<PendingAction>(null)
const feedback = ref('')
const feedbackTone = ref<'success' | 'error' | 'info'>('info')
const agents = ref<AgentSummary[]>([])
const promptTemplates = ref<PromptTemplateItem[]>([])
const selectedAgentId = ref('')
const selectedAgentDetail = ref<AgentDetail | null>(null)
const createdSession = ref<AgentSessionResult | null>(null)
const formMode = ref<FormMode>('create')
const editingAgentId = ref<string | null>(null)

const filters = reactive({ keyword: '', status: 'ALL' })
const form = reactive({
  agentName: '',
  description: '',
  selectedCapabilitiesText: 'knowledge_search, session_management, failover_recovery',
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
      .filter(Boolean).some((value) => String(value).toLowerCase().includes(keyword))
    const matchesStatus = filters.status === 'ALL' || item.agentStatus === filters.status
    return matchesKeyword && matchesStatus
  })
})
const latestVersion = computed<AgentVersion | null>(() => selectedAgentDetail.value?.versions?.[0] ?? null)
const selectedPromptTemplate = computed(() => promptTemplates.value.find((item) => String(item.id) === form.selectedPromptTemplateId) ?? null)
const selectedPromptVariables = computed(() => selectedPromptTemplate.value?.variableDefinitions ?? [])
const capabilityPreview = computed(() => parseCapabilities(form.selectedCapabilitiesText))
const totalAgentsLabel = computed(() => `共 ${agents.value.length} 个 Agent`)
const publishedCountLabel = computed(() => `${agents.value.filter((item) => item.agentStatus === 'PUBLISHED').length} 个已发布`)

watch(selectedAgentId, (value) => {
  createdSession.value = null
  if (!value) {
    selectedAgentDetail.value = null
    return
  }
  void loadAgentDetail(value)
})

watch(() => [form.promptMode, form.selectedPromptTemplateId] as const, ([mode]) => {
  if (mode === 'template') syncPromptVariableValues()
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
  form.promptMode = 'template'
  form.selectedPromptTemplateId = promptTemplates.value[0] ? String(promptTemplates.value[0].id) : ''
  form.customPromptContent = ''
  form.customPromptPath = ''
  form.promptVariableValues = {}
  syncPromptVariableValues()
}

function enterCreateMode() {
  formMode.value = 'create'
  editingAgentId.value = null
  resetForm()
}

function syncPromptVariableValues(sourceValues?: Record<string, string> | null) {
  const values: Record<string, string> = {}
  for (const item of selectedPromptVariables.value) {
    values[item.variableName] = sourceValues?.[item.variableName] ?? item.defaultValue ?? ''
  }
  form.promptVariableValues = values
}

function enterEditMode() {
  if (!selectedAgentDetail.value || !latestVersion.value) return
  const version = latestVersion.value
  formMode.value = 'edit'
  editingAgentId.value = selectedAgentDetail.value.agentId
  form.agentName = selectedAgentDetail.value.agentName
  form.description = selectedAgentDetail.value.description ?? ''
  form.selectedCapabilitiesText = version.selectedCapabilities.join(', ')
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
    return form.selectedPromptTemplateId ? { promptTemplateId: Number(form.selectedPromptTemplateId), promptBindingType: 'TEMPLATE', promptVariables: { ...form.promptVariableValues } } : null
  }
  if (form.promptMode === 'custom-inline') {
    return form.customPromptContent.trim() ? { promptBindingType: 'CUSTOM', promptSourceType: 'INLINE_TEXT', promptTemplateContent: form.customPromptContent.trim() } : null
  }
  return form.customPromptPath.trim() ? { promptBindingType: 'CUSTOM', promptSourceType: 'FILE_PATH', promptTemplatePath: form.customPromptPath.trim() } : null
}

function buildPayload(): AgentCreatePayload {
  return {
    agentName: form.agentName.trim(),
    description: form.description.trim() || null,
    systemPrompt: form.promptMode === 'custom-inline' ? form.customPromptContent.trim() || null : null,
    selectedCapabilities: parseCapabilities(form.selectedCapabilitiesText),
    agentType: 'REACT',
    promptConfig: buildPromptConfig(),
  }
}

function formatStatus(status: string) {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'DISABLED') return '已禁用'
  return '草稿中'
}

function formatPromptBinding(version: AgentVersion) {
  if (version.promptBindingType === 'TEMPLATE') return version.promptTemplateName || version.promptTemplateCode || '模板绑定'
  if (version.promptSourceType === 'FILE_PATH') return version.promptTemplatePath || '文件路径'
  return '自定义文本'
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

async function loadAgents(options?: { keepSelection?: boolean; successMessage?: string }) {
  loading.value = true
  try {
    const result = await queryAgents()
    agents.value = result
    const keepSelection = options?.keepSelection && result.some((item) => item.agentId === selectedAgentId.value)
    if (!keepSelection) selectedAgentId.value = result[0]?.agentId ?? ''
    else if (selectedAgentId.value) await loadAgentDetail(selectedAgentId.value)
    if (options?.successMessage) setFeedback('success', options.successMessage)
  } catch (error) {
    setFeedback('error', getErrorMessage(error, 'Agent 列表加载失败。'))
  } finally {
    loading.value = false
  }
}

async function loadAgentDetail(agentId: string) {
  detailLoading.value = true
  try {
    selectedAgentDetail.value = await fetchAgentDetail(agentId)
  } catch (error) {
    selectedAgentDetail.value = null
    setFeedback('error', getErrorMessage(error, 'Agent 详情加载失败。'))
  } finally {
    detailLoading.value = false
  }
}

async function handleSubmit() {
  if (!form.agentName.trim()) return setFeedback('error', '请输入 Agent 名称。')
  if (!buildPromptConfig()) return setFeedback('error', '请选择提示词模板，或者补全自定义提示词配置。')
  submitting.value = true
  try {
    if (formMode.value === 'create') {
      const result = await createAgent(buildPayload())
      selectedAgentId.value = result.agentId
      enterCreateMode()
      await loadAgents({ keepSelection: true, successMessage: `Agent ${result.agentName} 已创建。` })
    } else {
      if (!editingAgentId.value) throw new Error('缺少待编辑的 Agent 编码。')
      const result = await updateAgent(editingAgentId.value, buildPayload())
      selectedAgentId.value = result.agentId
      await loadAgents({ keepSelection: true, successMessage: `Agent ${result.agentName} 已更新并生成新版本。` })
      enterCreateMode()
    }
  } catch (error) {
    setFeedback('error', getErrorMessage(error, formMode.value === 'create' ? '创建 Agent 失败。' : '更新 Agent 失败。'))
  } finally {
    submitting.value = false
  }
}

async function handlePublishLatest() {
  if (!selectedAgentDetail.value || !latestVersion.value) return
  actionPending.value = 'publish'
  try {
    await publishAgent(selectedAgentDetail.value.agentId, latestVersion.value.versionNo)
    await loadAgents({ keepSelection: true, successMessage: `已发布 v${latestVersion.value.versionNo}。` })
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '发布 Agent 失败。'))
  } finally {
    actionPending.value = null
  }
}

async function handleDisable() {
  if (!selectedAgentDetail.value) return
  actionPending.value = 'disable'
  try {
    await disableAgent(selectedAgentDetail.value.agentId)
    await loadAgents({ keepSelection: true, successMessage: 'Agent 已禁用。' })
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '禁用 Agent 失败。'))
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
    await loadAgents({ successMessage: `Agent ${agentName} 已删除。` })
    if (editingAgentId.value === agentId) enterCreateMode()
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '删除 Agent 失败。'))
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
    await router.push({ name: 'agent-chat', params: { agentId: selectedAgentDetail.value.agentId }, query: { sessionId: session.sessionId, versionNo: String(session.agentVersionNo) } })
  } catch (error) {
    setFeedback('error', getErrorMessage(error, '创建聊天会话失败。'))
  } finally {
    actionPending.value = null
  }
}

onMounted(() => {
  void Promise.all([loadAgents(), loadPromptTemplates()])
})
</script>

<template>
  <MainShell>
    <section v-if="feedback" class="feedback" :class="`feedback--${feedbackTone}`">{{ feedback }}</section>
    <section class="workspace">
      <header class="hero">
        <div>
          <p class="section-kicker">Agent Studio</p>
          <h2>Agent 配置工作台</h2>
          <p class="muted">{{ totalAgentsLabel }}，{{ publishedCountLabel }}</p>
        </div>
        <button class="app-button app-button--secondary" :disabled="loading || detailLoading" @click="loadAgents({ keepSelection: true })">刷新数据</button>
      </header>

      <div class="grid">
        <section class="panel-card panel">
          <div class="panel-head">
            <div>
              <strong>{{ formMode === 'create' ? '创建新 Agent' : '编辑 Agent 配置' }}</strong>
              <p class="muted">{{ formMode === 'create' ? '支持模板、自定义文本和文件路径三种提示词来源。' : '编辑会生成新的版本快照。' }}</p>
            </div>
            <button v-if="formMode === 'edit'" class="app-button app-button--ghost" @click="enterCreateMode">切回创建</button>
          </div>

          <div class="form-grid">
            <label class="field">
              <span>Agent 名称</span>
              <input v-model="form.agentName" class="app-input" type="text" maxlength="64" />
            </label>
            <label class="field">
              <span>描述</span>
              <textarea v-model="form.description" class="app-input textarea" rows="3" />
            </label>
            <div class="field full">
              <span>提示词来源</span>
              <div class="mode-grid">
                <label class="mode" :class="{ 'mode--active': form.promptMode === 'template' }"><input v-model="form.promptMode" type="radio" value="template" />模板</label>
                <label class="mode" :class="{ 'mode--active': form.promptMode === 'custom-inline' }"><input v-model="form.promptMode" type="radio" value="custom-inline" />自定义文本</label>
                <label class="mode" :class="{ 'mode--active': form.promptMode === 'custom-file' }"><input v-model="form.promptMode" type="radio" value="custom-file" />文件路径</label>
              </div>
            </div>

            <label v-if="form.promptMode === 'template'" class="field">
              <span>提示词模板</span>
              <select v-model="form.selectedPromptTemplateId" class="app-select" :disabled="promptTemplatesLoading">
                <option value="">{{ promptTemplatesLoading ? '正在加载模板...' : '请选择模板' }}</option>
                <option v-for="item in promptTemplates" :key="item.id" :value="String(item.id)">{{ item.templateName }} / {{ item.templateCode }}</option>
              </select>
            </label>
            <label v-else-if="form.promptMode === 'custom-inline'" class="field">
              <span>系统提示词文本</span>
              <textarea v-model="form.customPromptContent" class="app-input textarea large" rows="8" />
            </label>
            <label v-else class="field">
              <span>提示词文件路径</span>
              <input v-model="form.customPromptPath" class="app-input" type="text" />
            </label>

            <section v-if="form.promptMode === 'template' && selectedPromptVariables.length > 0" class="field full variables">
              <span>模板变量</span>
              <div class="vars-grid">
                <label v-for="item in selectedPromptVariables" :key="item.variableName" class="field card">
                  <div class="row"><strong>{{ item.variableName }}</strong><small>{{ item.required ? '必填' : '可选' }}</small></div>
                  <input v-model="form.promptVariableValues[item.variableName]" class="app-input" type="text" :placeholder="item.defaultValue || '请输入变量值'" />
                  <small class="muted">{{ item.description || '未配置业务说明' }}<template v-if="item.defaultValue"> · 默认值：{{ item.defaultValue }}</template></small>
                </label>
              </div>
            </section>

            <label class="field">
              <span>能力标签</span>
              <textarea v-model="form.selectedCapabilitiesText" class="app-input textarea" rows="4" />
            </label>
          </div>

          <div class="chips"><span v-for="item in capabilityPreview" :key="item" class="chip">{{ item }}</span></div>
          <button class="app-button submit" :disabled="submitting" @click="handleSubmit">{{ submitting ? '提交中...' : formMode === 'create' ? '创建 Agent' : '保存并生成新版本' }}</button>
        </section>

        <section class="panel-card panel">
          <div class="panel-head"><strong>Agent 列表</strong></div>
          <div class="form-grid">
            <label class="field"><span>搜索</span><input v-model="filters.keyword" class="app-input" type="text" /></label>
            <label class="field"><span>状态</span><select v-model="filters.status" class="app-select"><option value="ALL">全部</option><option value="DRAFT">草稿中</option><option value="PUBLISHED">已发布</option><option value="DISABLED">已禁用</option></select></label>
          </div>
          <div v-if="loading" class="empty">正在加载 Agent 列表...</div>
          <div v-else-if="filteredAgents.length === 0" class="empty">当前筛选条件下没有 Agent。</div>
          <div v-else class="list">
            <button v-for="item in filteredAgents" :key="item.agentId" class="list-item" :class="{ 'list-item--active': selectedAgentId === item.agentId }" @click="selectedAgentId = item.agentId">
              <div class="row"><strong>{{ item.agentName }}</strong><span>{{ formatStatus(item.agentStatus) }}</span></div>
              <p class="muted">{{ item.description || '暂无描述' }}</p>
              <small class="muted">当前 v{{ item.currentVersionNo ?? '-' }} / 发布 v{{ item.publishedVersionNo ?? '-' }}</small>
            </button>
          </div>
        </section>
      </div>

      <div class="grid detail-grid">
        <section class="panel-card panel">
          <div class="panel-head">
            <strong>Agent 详情</strong>
            <div v-if="selectedAgentDetail" class="row wrap">
              <button class="app-button app-button--ghost" :disabled="detailLoading || actionPending !== null" @click="enterEditMode">编辑</button>
              <button class="app-button app-button--ghost" :disabled="!latestVersion || actionPending !== null" @click="handlePublishLatest">发布最新版本</button>
              <button class="app-button app-button--ghost" :disabled="isSelectedAgentDisabled || actionPending !== null" @click="handleDisable">禁用</button>
              <button class="app-button app-button--ghost app-button--danger-ghost" :disabled="actionPending !== null" @click="handleDelete">删除</button>
            </div>
          </div>
          <div v-if="detailLoading" class="empty">正在加载 Agent 详情...</div>
          <div v-else-if="!selectedAgentDetail" class="empty">请选择一个 Agent 查看详情。</div>
          <div v-else class="detail">
            <article class="card">
              <div class="row"><h3>{{ selectedAgentDetail.agentName }}</h3><span>{{ formatStatus(selectedAgentDetail.agentStatus) }}</span></div>
              <p class="muted">{{ selectedAgentDetail.description || '暂无描述' }}</p>
            </article>
            <article v-for="version in selectedAgentDetail.versions" :key="version.versionId" class="card">
              <div class="row"><strong>版本 v{{ version.versionNo }}</strong><span>{{ version.published ? '已发布' : '草稿快照' }}</span></div>
              <p class="muted">{{ formatTime(version.createTime) }} · {{ formatPromptBinding(version) }}</p>
              <div class="chips"><span v-for="item in version.selectedCapabilities" :key="`${version.versionId}-${item}`" class="chip">{{ item }}</span></div>
              <div v-if="version.promptVariableDefinitions?.length" class="vars-grid">
                <article v-for="item in version.promptVariableDefinitions" :key="`${version.versionId}-${item.variableName}`" class="card">
                  <div class="row"><strong>{{ item.variableName }}</strong><small>{{ item.required ? '必填' : '可选' }}</small></div>
                  <small class="muted">{{ item.description || '未配置说明' }}</small>
                  <code>{{ version.promptVariables?.[item.variableName] || item.defaultValue || '-' }}</code>
                </article>
              </div>
              <pre class="prompt">{{ version.systemPrompt || '暂无系统提示词' }}</pre>
              <div class="row wrap">
                <button class="app-button app-button--ghost" :disabled="actionPending !== null" @click="handleCreateSession(version.versionNo)">创建会话</button>
                <button class="app-button app-button--secondary" :disabled="actionPending !== null" @click="handleOpenChat(version.versionNo)">进入聊天</button>
              </div>
            </article>
          </div>
        </section>

        <section class="panel-card panel">
          <div class="panel-head"><strong>会话入口</strong></div>
          <div v-if="!selectedAgentDetail" class="empty">选择 Agent 后即可创建会话。</div>
          <div v-else class="detail">
            <button class="app-button" :disabled="actionPending !== null" @click="handleCreateSession()">创建默认会话</button>
            <button class="app-button app-button--secondary" :disabled="actionPending !== null" @click="handleOpenChat()">直接进入聊天页</button>
            <article v-if="createdSession" class="card">
              <strong>{{ createdSession.sessionId }}</strong>
              <p class="muted">版本号：v{{ createdSession.agentVersionNo }}</p>
              <p class="muted">连接状态：{{ createdSession.connectionStatus }}</p>
            </article>
          </div>
        </section>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.workspace,.panel{padding:24px}.hero,.row,.panel-head{display:flex;gap:12px}.hero,.panel-head,.row{justify-content:space-between}.hero{align-items:flex-start;margin-bottom:20px}.grid,.form-grid,.mode-grid,.vars-grid{display:grid;gap:16px}.grid{grid-template-columns:minmax(0,1.3fr) minmax(320px,.9fr)}.detail-grid{margin-top:20px;grid-template-columns:minmax(0,1.4fr) minmax(260px,.6fr)}.form-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.mode-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.vars-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.field{display:flex;flex-direction:column;gap:8px}.full{grid-column:1/-1}.textarea{min-height:110px;padding:14px;resize:vertical}.large{min-height:220px}.mode,.card,.list-item,.empty,.variables{padding:14px;border-radius:16px;background:rgba(255,255,255,.04);box-shadow:inset 0 0 0 1px rgba(255,255,255,.06)}.mode{cursor:pointer;text-align:center}.mode--active{background:rgba(83,184,255,.14)}.mode input{display:none}.chips{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px}.chip{padding:6px 10px;border-radius:999px;background:rgba(83,184,255,.14);color:#d8f2ff}.submit{margin-top:16px}.list,.detail{display:flex;flex-direction:column;gap:12px}.list-item{text-align:left}.list-item--active{box-shadow:inset 0 0 0 1px rgba(83,184,255,.28)}.prompt,.empty{white-space:pre-wrap;word-break:break-word}.prompt{margin:10px 0 0;padding:12px;border-radius:12px;background:rgba(4,17,29,.58)}.feedback{margin-bottom:16px;padding:12px 14px;border-radius:14px}.feedback--success{background:rgba(55,178,109,.14);color:#d6ffe9}.feedback--error{background:rgba(180,57,68,.18);color:#ffd8d8}.feedback--info{background:rgba(83,184,255,.14);color:#d8f2ff}.muted{margin:0;color:var(--color-ink-soft)}.wrap{flex-wrap:wrap}
@media (max-width:960px){.grid,.detail-grid,.form-grid,.mode-grid,.vars-grid{grid-template-columns:1fr}}
</style>
