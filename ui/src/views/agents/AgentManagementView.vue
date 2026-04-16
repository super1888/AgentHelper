<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bot,
  ChevronRight,
  CircleDot,
  Layers3,
  LoaderCircle,
  Plus,
  RadioTower,
  RefreshCw,
  Rocket,
  ShieldBan,
  Sparkles,
  Waypoints,
} from 'lucide-vue-next'
import MainShell from '@/components/MainShell.vue'
import { createAgent, createAgentSession, disableAgent, fetchAgentDetail, publishAgent, queryAgents } from '@/api/agent'
import type { AgentCreatePayload, AgentDetail, AgentSessionResult, AgentSummary, AgentVersion } from '@/types/agent'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const actionPending = ref<'publish' | 'disable' | 'session' | null>(null)
const feedback = ref<FeedbackState | null>(null)
const agents = ref<AgentSummary[]>([])
const selectedAgentId = ref<string>('')
const selectedAgentDetail = ref<AgentDetail | null>(null)
const createdSession = ref<AgentSessionResult | null>(null)
const router = useRouter()

const form = reactive({
  agentName: '',
  description: '',
  systemPrompt: '',
  selectedCapabilitiesText: '知识检索, 会话管理, 失败恢复',
})

const totalAgentsLabel = computed(() => `共 ${agents.value.length} 个 Agent`)
const publishedCountLabel = computed(() =>
  `${agents.value.filter((item) => item.agentStatus === 'PUBLISHED').length} 个已发布`,
)
const latestVersion = computed<AgentVersion | null>(() => selectedAgentDetail.value?.versions?.[0] ?? null)
const capabilityPreview = computed(() => parseCapabilities(form.selectedCapabilitiesText))

watch(selectedAgentId, (agentId) => {
  if (!agentId) {
    selectedAgentDetail.value = null
    return
  }
  void loadAgentDetail(agentId)
})

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function parseCapabilities(value: string) {
  return value
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function buildCreatePayload(): AgentCreatePayload {
  return {
    agentName: form.agentName.trim(),
    description: form.description.trim() || null,
    systemPrompt: form.systemPrompt.trim() || null,
    selectedCapabilities: parseCapabilities(form.selectedCapabilitiesText),
    agentType: 'REACT',
  }
}

function formatStatusLabel(status: string) {
  if (status === 'PUBLISHED') {
    return '已发布'
  }
  if (status === 'DISABLED') {
    return '已禁用'
  }
  return '草稿中'
}

function formatVersionLabel(agent: AgentSummary) {
  const current = agent.currentVersionNo ?? '-'
  const published = agent.publishedVersionNo ?? '-'
  return `当前 v${current} / 发布 v${published}`
}

function formatTime(value: number | null) {
  if (!value) {
    return '刚刚生成'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value)
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
      showFeedback('success', options.successMessage)
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Agent 列表加载失败。'))
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
    showFeedback('error', getErrorMessage(error, 'Agent 详情加载失败。'))
  } finally {
    detailLoading.value = false
  }
}

async function handleCreateAgent() {
  if (!form.agentName.trim()) {
    showFeedback('error', '请输入 Agent 名称。')
    return
  }

  submitting.value = true

  try {
    const result = await createAgent(buildCreatePayload())
    form.agentName = ''
    form.description = ''
    form.systemPrompt = ''
    createdSession.value = null
    selectedAgentId.value = result.agentId
    await loadAgents({ keepSelection: true, successMessage: `Agent ${result.agentName} 已创建。` })
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '创建 Agent 失败。'))
  } finally {
    submitting.value = false
  }
}

async function handlePublishLatest() {
  if (!selectedAgentDetail.value || !latestVersion.value) {
    return
  }

  actionPending.value = 'publish'

  try {
    await publishAgent(selectedAgentDetail.value.agentId, latestVersion.value.versionNo)
    await loadAgents({ keepSelection: true, successMessage: `已发布 v${latestVersion.value.versionNo}。` })
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '发布 Agent 失败。'))
  } finally {
    actionPending.value = null
  }
}

async function handleDisableAgent() {
  if (!selectedAgentDetail.value) {
    return
  }

  actionPending.value = 'disable'

  try {
    await disableAgent(selectedAgentDetail.value.agentId)
    await loadAgents({ keepSelection: true, successMessage: 'Agent 已禁用。' })
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '禁用 Agent 失败。'))
  } finally {
    actionPending.value = null
  }
}

async function handleCreateSession(versionNo?: number) {
  if (!selectedAgentDetail.value) {
    return
  }

  actionPending.value = 'session'

  try {
    createdSession.value = await createAgentSession(selectedAgentDetail.value.agentId, versionNo ? { versionNo } : {})
    showFeedback('success', `会话 ${createdSession.value.sessionId} 已创建。`)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '创建会话失败。'))
  } finally {
    actionPending.value = null
  }
}

async function handleOpenChat(versionNo?: number) {
  if (!selectedAgentDetail.value) {
    return
  }

  actionPending.value = 'session'

  try {
    const session = await createAgentSession(selectedAgentDetail.value.agentId, versionNo ? { versionNo } : {})
    createdSession.value = session
    await router.push({
      name: 'agent-chat',
      params: { agentId: selectedAgentDetail.value.agentId },
      query: {
        sessionId: session.sessionId,
        versionNo: String(session.agentVersionNo),
      },
    })
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '创建聊天会话失败。'))
  } finally {
    actionPending.value = null
  }
}

onMounted(() => {
  void loadAgents()
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

    <section class="agent-workspace panel-card">
      <header class="agent-workspace__hero">
        <div class="agent-workspace__headline">
          <p class="section-kicker">Agent Studio</p>
          <div class="agent-workspace__title-row">
            <h2>Agent 配置工作台</h2>
            <span class="agent-workspace__count-badge">
              <Bot :size="15" aria-hidden="true" />
              {{ totalAgentsLabel }}
            </span>
            <span class="agent-workspace__count-badge agent-workspace__count-badge--subtle">
              <Rocket :size="15" aria-hidden="true" />
              {{ publishedCountLabel }}
            </span>
          </div>
          <p class="agent-workspace__subtitle">
            在一个页面里完成 Agent 创建、版本发布、会话绑定和运行入口确认。
          </p>
        </div>

        <button
          type="button"
          class="app-button app-button--secondary"
          :disabled="loading || detailLoading"
          @click="loadAgents({ keepSelection: true })"
        >
          <RefreshCw :size="16" aria-hidden="true" />
          刷新数据
        </button>
      </header>

      <div class="agent-grid">
        <section class="agent-grid__composer panel-card">
          <div class="section-header">
            <div>
              <strong>创建新 Agent</strong>
              <p>保持结构简洁，只保留创建和核心版本信息。</p>
            </div>
            <div class="section-header__pulse">
              <Sparkles :size="16" aria-hidden="true" />
              REACT
            </div>
          </div>

          <div class="composer-form">
            <label class="field">
              <span class="field__label">Agent 名称</span>
              <div class="input-shell">
                <span class="input-shell__icon" aria-hidden="true">
                  <Bot :size="16" />
                </span>
                <input
                  v-model="form.agentName"
                  class="app-input"
                  type="text"
                  maxlength="64"
                  placeholder="例如：客户工单助手"
                />
              </div>
            </label>

            <label class="field">
              <span class="field__label">描述</span>
              <textarea
                v-model="form.description"
                class="composer-textarea"
                rows="3"
                placeholder="概括这个 Agent 的职责和使用场景"
              />
            </label>

            <label class="field">
              <span class="field__label">系统提示词</span>
              <textarea
                v-model="form.systemPrompt"
                class="composer-textarea composer-textarea--prompt"
                rows="7"
                placeholder="例如：你是面向运营团队的任务协调助手，回答要直接、稳定、可执行。"
              />
            </label>

            <label class="field">
              <span class="field__label">能力标签</span>
              <textarea
                v-model="form.selectedCapabilitiesText"
                class="composer-textarea"
                rows="4"
                placeholder="使用逗号或换行分隔，例如：知识检索, 会话管理, 失败恢复"
              />
            </label>
          </div>

          <div class="capability-preview">
            <span v-for="item in capabilityPreview" :key="item" class="capability-chip">
              {{ item }}
            </span>
          </div>

          <button
            type="button"
            class="app-button composer-submit"
            :disabled="submitting"
            @click="handleCreateAgent"
          >
            <LoaderCircle v-if="submitting" :size="16" class="is-spinning" aria-hidden="true" />
            <Plus v-else :size="16" aria-hidden="true" />
            {{ submitting ? '创建中...' : '创建 Agent' }}
          </button>
        </section>

        <section class="agent-grid__catalog panel-card">
          <div class="section-header">
            <div>
              <strong>我的 Agent</strong>
              <p>草稿、发布状态和版本信息一目了然。</p>
            </div>
          </div>

          <div v-if="loading" class="catalog-state">
            正在加载 Agent 列表...
          </div>

          <div v-else-if="agents.length === 0" class="catalog-state">
            还没有 Agent，先在左侧创建一个。
          </div>

          <div v-else class="catalog-list">
            <button
              v-for="agent in agents"
              :key="agent.agentId"
              type="button"
              class="agent-card"
              :class="{ 'agent-card--active': agent.agentId === selectedAgentId }"
              @click="selectedAgentId = agent.agentId"
            >
              <div class="agent-card__top">
                <strong>{{ agent.agentName }}</strong>
                <span class="agent-status" :class="`agent-status--${agent.agentStatus.toLowerCase()}`">
                  {{ formatStatusLabel(agent.agentStatus) }}
                </span>
              </div>
              <p class="agent-card__description">
                {{ agent.description || '暂未填写描述，建议补充业务定位和输出边界。' }}
              </p>
              <div class="agent-card__meta">
                <span>
                  <Layers3 :size="14" aria-hidden="true" />
                  {{ formatVersionLabel(agent) }}
                </span>
                <span>
                  <CircleDot :size="14" aria-hidden="true" />
                  {{ agent.ownerUserName || '当前用户' }}
                </span>
              </div>
            </button>
          </div>
        </section>

        <section class="agent-grid__detail panel-card">
          <div class="section-header">
            <div>
              <strong>版本与运行</strong>
              <p>查看版本快照，并直接创建当前会话。</p>
            </div>
          </div>

          <div v-if="detailLoading" class="catalog-state">正在加载详情...</div>
          <div v-else-if="!selectedAgentDetail" class="catalog-state">选择一个 Agent 查看详情。</div>
          <template v-else>
            <div class="detail-head">
              <div>
                <p class="detail-kicker">{{ selectedAgentDetail.agentType }}</p>
                <h3>{{ selectedAgentDetail.agentName }}</h3>
                <p class="detail-description">
                  {{ selectedAgentDetail.description || '当前 Agent 暂未填写描述。' }}
                </p>
              </div>

              <div class="detail-actions">
                <button
                  type="button"
                  class="app-button"
                  :disabled="actionPending !== null || !latestVersion"
                  @click="handlePublishLatest"
                >
                  <Rocket :size="16" aria-hidden="true" />
                  {{ actionPending === 'publish' ? '发布中...' : '发布最新版本' }}
                </button>

                <button
                  type="button"
                  class="app-button app-button--ghost app-button--danger-ghost"
                  :disabled="actionPending !== null"
                  @click="handleDisableAgent"
                >
                  <ShieldBan :size="16" aria-hidden="true" />
                  {{ actionPending === 'disable' ? '处理中...' : '禁用 Agent' }}
                </button>
              </div>
            </div>

            <div class="detail-strip">
              <article class="detail-metric">
                <span>当前版本</span>
                <strong>v{{ selectedAgentDetail.currentVersionNo ?? '-' }}</strong>
              </article>
              <article class="detail-metric">
                <span>已发布版本</span>
                <strong>v{{ selectedAgentDetail.publishedVersionNo ?? '-' }}</strong>
              </article>
              <article class="detail-metric">
                <span>版本数量</span>
                <strong>{{ selectedAgentDetail.versions.length }}</strong>
              </article>
            </div>

            <div class="detail-session panel-card">
              <div class="detail-session__head">
                <div class="detail-session__intro">
                  <strong>运行入口</strong>
                  <p>
                    默认使用已发布版本创建会话；如果未发布，则回退到当前最新版本。
                  </p>
                </div>
                <div class="detail-session__actions">
                  <button
                    type="button"
                    class="app-button app-button--secondary detail-session__button"
                    :disabled="actionPending !== null"
                    @click="handleCreateSession()"
                  >
                    <RadioTower :size="16" aria-hidden="true" />
                    {{ actionPending === 'session' ? '创建中...' : '创建默认会话' }}
                  </button>
                  <button
                    type="button"
                    class="app-button detail-session__button"
                    :disabled="actionPending !== null"
                    @click="handleOpenChat()"
                  >
                    <Waypoints :size="16" aria-hidden="true" />
                    {{ actionPending === 'session' ? '准备中...' : '进入聊天' }}
                  </button>
                </div>
              </div>

              <div v-if="createdSession" class="session-result">
                <div>
                  <span>会话编码</span>
                  <strong>{{ createdSession.sessionId }}</strong>
                </div>
                <div>
                  <span>绑定版本</span>
                  <strong>v{{ createdSession.agentVersionNo }}</strong>
                </div>
                <div>
                  <span>发送地址</span>
                  <strong>{{ createdSession.websocketSendDestination }}</strong>
                </div>
              </div>
            </div>

            <div class="version-list">
              <button
                v-for="version in selectedAgentDetail.versions"
                :key="version.versionId"
                type="button"
                class="version-card"
                @click="handleOpenChat(version.versionNo)"
              >
                <div class="version-card__head">
                  <div>
                    <strong>v{{ version.versionNo }}</strong>
                    <p>{{ formatTime(version.createTime) }}</p>
                  </div>
                  <span class="version-badge" :class="{ 'version-badge--published': version.published }">
                    {{ version.published ? '已发布' : '快照' }}
                  </span>
                </div>

                <p class="version-card__description">
                  {{ version.description || '该版本未填写描述。' }}
                </p>

                <div class="version-card__chips">
                  <span v-for="capability in version.selectedCapabilities" :key="capability">
                    {{ capability }}
                  </span>
                </div>

                <div class="version-card__foot">
                  <span>点击直接用该版本创建会话</span>
                  <ChevronRight :size="16" aria-hidden="true" />
                </div>
              </button>
            </div>
          </template>
        </section>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.agent-workspace {
  padding: 30px;
}

.agent-workspace__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 28px;
}

.agent-workspace__headline {
  max-width: 48rem;
}

.agent-workspace__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.agent-workspace__headline h2 {
  font-size: clamp(2rem, 2.5vw, 2.6rem);
  line-height: 1.02;
  letter-spacing: -0.03em;
}

.agent-workspace__count-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  color: #d9f7ff;
  border: 1px solid rgba(116, 210, 255, 0.22);
  background:
    linear-gradient(135deg, rgba(116, 210, 255, 0.16), rgba(116, 210, 255, 0.06)),
    rgba(255, 255, 255, 0.03);
}

.agent-workspace__count-badge--subtle {
  color: var(--color-ink-soft);
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
}

.agent-workspace__subtitle {
  margin-top: 14px;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.agent-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.95fr) minmax(280px, 0.9fr) minmax(360px, 1.15fr);
  gap: 22px;
}

.agent-grid > section {
  padding: 22px;
  border-radius: 26px;
}

.agent-grid__composer,
.agent-grid__catalog,
.agent-grid__detail {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.018)),
    rgba(6, 12, 24, 0.72);
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-header strong {
  display: block;
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.section-header p {
  margin-top: 6px;
  color: var(--color-ink-soft);
  font-size: 0.9rem;
  line-height: 1.6;
}

.section-header__pulse {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  color: #bff6ff;
  background: rgba(83, 184, 255, 0.12);
  border: 1px solid rgba(83, 184, 255, 0.18);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.composer-form {
  display: grid;
  gap: 16px;
  margin-top: 22px;
}

.composer-textarea {
  min-height: 110px;
  padding: 16px 18px;
  color: var(--color-ink-strong);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  resize: vertical;
  outline: none;
  transition: border-color 180ms ease, box-shadow 180ms ease, background-color 180ms ease;
}

.composer-textarea:focus {
  border-color: rgba(77, 179, 255, 0.42);
  box-shadow: var(--shadow-focus);
  background: rgba(255, 255, 255, 0.08);
}

.composer-textarea::placeholder {
  color: rgba(166, 183, 211, 0.56);
}

.composer-textarea--prompt {
  min-height: 168px;
}

.capability-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.capability-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  color: #d7f7ff;
  background: rgba(83, 184, 255, 0.12);
  border: 1px solid rgba(83, 184, 255, 0.18);
  font-size: 0.84rem;
}

.composer-submit {
  width: 100%;
  margin-top: 22px;
}

.catalog-state {
  display: grid;
  place-items: center;
  min-height: 320px;
  color: var(--color-ink-soft);
  text-align: center;
}

.catalog-list,
.version-list {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.agent-card,
.version-card {
  width: 100%;
  padding: 18px;
  text-align: left;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, background-color 180ms ease;
}

.agent-card:hover,
.version-card:hover {
  transform: translateY(-1px);
  border-color: rgba(83, 184, 255, 0.22);
  background: rgba(83, 184, 255, 0.07);
}

.agent-card--active {
  border-color: rgba(119, 224, 255, 0.42);
  background:
    radial-gradient(circle at top right, rgba(83, 184, 255, 0.1), transparent 36%),
    rgba(83, 184, 255, 0.08);
}

.agent-card__top,
.version-card__head,
.detail-head,
.version-card__foot {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.agent-card__top strong,
.detail-head h3,
.version-card__head strong {
  color: var(--color-ink-strong);
}

.agent-status,
.version-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.agent-status--published,
.version-badge--published {
  color: #ddfff6;
  background: rgba(100, 216, 190, 0.16);
}

.agent-status--draft {
  color: #def0ff;
  background: rgba(83, 184, 255, 0.16);
}

.agent-status--disabled {
  color: #ffe2e2;
  background: rgba(255, 144, 151, 0.14);
}

.agent-card__description,
.detail-description,
.version-card__description {
  margin-top: 12px;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.agent-card__meta,
.version-card__chips,
.detail-strip,
.session-result {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.agent-card__meta span,
.version-card__chips span,
.detail-metric,
.session-result > div {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 12px;
  color: var(--color-ink-soft);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.05);
}

.detail-kicker {
  color: #b4f5ff;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 18px;
}

.detail-metric {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  min-height: 96px;
  padding: 18px;
  border-radius: 22px;
}

.detail-metric span,
.session-result span {
  color: var(--color-ink-muted);
  font-size: 0.82rem;
}

.detail-metric strong,
.session-result strong {
  color: var(--color-ink-strong);
  font-size: 1.08rem;
}

.detail-session {
  margin-top: 18px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.04);
}

.detail-session__head {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(240px, 0.9fr);
  gap: 18px;
  align-items: stretch;
}

.detail-session__intro {
  min-width: 0;
}

.detail-session__intro p {
  margin-top: 8px;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.detail-session__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-self: stretch;
}

.detail-session__button {
  justify-content: center;
  min-height: 56px;
  padding: 14px 18px;
  text-align: center;
  white-space: normal;
  line-height: 1.4;
}

.version-card__head p {
  margin-top: 6px;
  color: var(--color-ink-muted);
  font-size: 0.84rem;
}

.version-card__foot {
  margin-top: 16px;
  color: #bfefff;
  font-size: 0.9rem;
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

.is-spinning {
  animation: spin 0.9s linear infinite;
}

@media (max-width: 1280px) {
  .agent-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .agent-grid__detail {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .agent-workspace {
    padding: 22px;
  }

  .agent-workspace__hero,
  .detail-head,
  .detail-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-strip {
    grid-template-columns: 1fr;
  }

  .detail-session__head {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .detail-session__actions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .agent-grid {
    grid-template-columns: 1fr;
  }
}
</style>
