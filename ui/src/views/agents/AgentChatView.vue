<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Bot,
  CircleAlert,
  CornerDownLeft,
  LoaderCircle,
  RadioTower,
  RefreshCw,
  RotateCcw,
  Send,
  UserRound,
  Waves,
} from 'lucide-vue-next'
import MainShell from '@/components/MainShell.vue'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import { closeAgentSession, fetchAgentDetail, reconnectAgentSession, recoverAgentTask } from '@/api/agent'
import { AgentChatSocket } from '@/services/agentChatSocket'
import type { AgentChatEvent, AgentDetail } from '@/types/agent'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

interface ChatBubble {
  id: string
  role: 'user' | 'assistant' | 'system'
  title: string
  content: string
  meta: string
  taskId: string | null
  tone?: 'error' | 'info'
  streaming?: boolean
}

const route = useRoute()
const router = useRouter()

const agentDetail = ref<AgentDetail | null>(null)
const sessionId = ref<string>('')
const connected = ref(false)
const loading = ref(false)
const sending = ref(false)
const recovering = ref(false)
const closing = ref(false)
const inputMessage = ref('')
const feedback = ref<FeedbackState | null>(null)
const lastTaskId = ref<string | null>(null)
const lastFailedTaskId = ref<string | null>(null)
const lastReceivedEventSequence = ref<string>('0')
const bubbles = ref<ChatBubble[]>([])
const transcriptRef = ref<HTMLElement | null>(null)

let socket: AgentChatSocket | null = null
let scrollFrame: number | null = null

const streamQueues = new Map<string, string>()
const streamTimers = new Map<string, ReturnType<typeof setTimeout>>()

const agentId = computed(() => String(route.params.agentId || ''))
const routeSessionId = computed(() => String(route.query.sessionId || ''))
const routeVersionNo = computed(() => Number(route.query.versionNo || 0) || 0)
const headerTitle = computed(() => agentDetail.value?.agentName || 'Agent Chat')
const connectionLabel = computed(() => (connected.value ? '实时连接中' : '连接已断开'))
const canSend = computed(() => connected.value && inputMessage.value.trim().length > 0 && !sending.value)
const canRecover = computed(() => Boolean(sessionId.value && lastFailedTaskId.value) && !recovering.value)
const currentVersion = computed(() => {
  if (!agentDetail.value?.versions?.length) {
    return null
  }
  return agentDetail.value.versions.find((item) => item.versionNo === routeVersionNo.value)
    ?? agentDetail.value.versions.find((item) => item.versionNo === agentDetail.value?.publishedVersionNo)
    ?? agentDetail.value.versions[0]
})
const currentModelSummary = computed(() => {
  if (!currentVersion.value?.modelName) {
    return '未绑定模型'
  }
  return `${currentVersion.value.modelName} / ${currentVersion.value.providerName || currentVersion.value.providerEnum || '-'}`
})

watch(
  () => bubbles.value.length,
  async () => {
    await nextTick()
    scrollTranscriptToBottom(true)
  },
)

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function pushBubble(bubble: ChatBubble) {
  bubbles.value = [...bubbles.value, bubble]
  scrollTranscriptToBottom()
}

function upsertAssistantBubble(
  taskId: string | null,
  patch: {
    title?: string
    content?: string
    meta?: string
    tone?: 'error' | 'info'
    streaming?: boolean
  },
) {
  const index = bubbles.value.findIndex((item) => item.role === 'assistant' && item.taskId === taskId)
  if (index === -1) {
    pushBubble({
      id: `assistant-${taskId ?? Date.now()}`,
      role: 'assistant',
      title: patch.title || 'Agent',
      content: patch.content || '',
      meta: patch.meta || '刚刚',
      taskId,
      tone: patch.tone,
      streaming: patch.streaming ?? false,
    })
    return
  }

  const next = [...bubbles.value]
  next[index] = {
    ...next[index],
    ...patch,
    content: patch.content ?? next[index].content,
    meta: patch.meta ?? next[index].meta,
    streaming: patch.streaming ?? next[index].streaming ?? false,
  }
  bubbles.value = next
  scrollTranscriptToBottom()
}

function formatTimestamp(value: number) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(value)
}

function toSafeNumber(value: string | number | null | undefined) {
  if (typeof value === 'number') return value
  if (!value) return 0
  const parsedValue = Number(value)
  return Number.isFinite(parsedValue) ? parsedValue : 0
}

function extractText(data: unknown) {
  if (typeof data === 'string') {
    return data
  }
  if (data == null) {
    return ''
  }
  return JSON.stringify(data, null, 2)
}

function toTaskStreamKey(taskId: string | null) {
  return taskId || '__default__'
}

function isTranscriptNearBottom() {
  const element = transcriptRef.value
  if (!element) {
    return true
  }
  const distance = element.scrollHeight - element.scrollTop - element.clientHeight
  return distance < 120
}

function scrollTranscriptToBottom(force = false) {
  if (!force && !isTranscriptNearBottom()) {
    return
  }
  if (scrollFrame != null) {
    cancelAnimationFrame(scrollFrame)
  }
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = null
    if (transcriptRef.value) {
      transcriptRef.value.scrollTop = transcriptRef.value.scrollHeight
    }
  })
}

function resolveStreamSliceSize(queue: string) {
  const characters = Array.from(queue)
  if (characters.length <= 6) {
    return 1
  }
  if (characters.length <= 24) {
    return 2
  }
  if (characters.length <= 80) {
    return 4
  }
  return 8
}

function resolveStreamDelay(appendedText: string) {
  if (!appendedText) {
    return 24
  }
  if (/[，。！；：,.!?;:]\s*$/.test(appendedText)) {
    return 88
  }
  if (/\s$/.test(appendedText)) {
    return 42
  }
  return 22
}

function drainAssistantStream(taskId: string | null, agentVersionNo?: string | number | null) {
  const taskKey = toTaskStreamKey(taskId)
  const queuedText = streamQueues.get(taskKey) || ''
  if (!queuedText) {
    streamTimers.delete(taskKey)
    return
  }

  const queueCharacters = Array.from(queuedText)
  const sliceSize = resolveStreamSliceSize(queuedText)
  const appendedText = queueCharacters.slice(0, sliceSize).join('')
  const remainingText = queueCharacters.slice(sliceSize).join('')
  const previous = bubbles.value.find((item) => item.role === 'assistant' && item.taskId === taskId)?.content || ''

  upsertAssistantBubble(taskId, {
    title: 'Agent',
    content: `${previous}${appendedText}`,
    meta: agentVersionNo ? `流式生成中 · v${agentVersionNo}` : '流式生成中',
    streaming: true,
  })

  if (!remainingText) {
    streamQueues.delete(taskKey)
    streamTimers.delete(taskKey)
    return
  }

  streamQueues.set(taskKey, remainingText)
  const timer = setTimeout(() => {
    drainAssistantStream(taskId, agentVersionNo)
  }, resolveStreamDelay(appendedText))
  streamTimers.set(taskKey, timer)
}

function queueAssistantStream(taskId: string | null, text: string, agentVersionNo?: string | number | null) {
  if (!text) {
    return
  }
  const taskKey = toTaskStreamKey(taskId)
  streamQueues.set(taskKey, `${streamQueues.get(taskKey) || ''}${text}`)
  if (streamTimers.has(taskKey)) {
    return
  }
  drainAssistantStream(taskId, agentVersionNo)
}

function stopAssistantStream(taskId: string | null) {
  const taskKey = toTaskStreamKey(taskId)
  const timer = streamTimers.get(taskKey)
  if (timer) {
    clearTimeout(timer)
  }
  streamTimers.delete(taskKey)
  streamQueues.delete(taskKey)
}

function handleAgentEvent(event: AgentChatEvent) {
  lastReceivedEventSequence.value = String(Math.max(
    toSafeNumber(lastReceivedEventSequence.value),
    toSafeNumber(event.eventSequence),
  ))
  lastTaskId.value = event.taskId

  if (event.event === 'CHAT_START' || event.event === 'METHOD_START') {
    upsertAssistantBubble(event.taskId, {
      title: 'Agent',
      content: '',
      meta: '任务开始执行',
      streaming: true,
    })
    pushBubble({
      id: `system-start-${event.eventSequence}`,
      role: 'system',
      title: '任务已启动',
      content: '消息已送达，Agent 正在处理中。',
      meta: formatTimestamp(toSafeNumber(event.timestamp)),
      taskId: event.taskId,
      tone: 'info',
    })
    return
  }

  if (event.event === 'USER_MESSAGE') {
    pushBubble({
      id: `user-${event.eventSequence}`,
      role: 'user',
      title: '我',
      content: extractText(event.data),
      meta: formatTimestamp(toSafeNumber(event.timestamp)),
      taskId: event.taskId,
    })
    return
  }

  if (event.event === 'AGENT_TOKEN') {
    queueAssistantStream(event.taskId, extractText(event.data), event.agentVersionNo)
    return
  }

  if (event.event === 'AGENT_REASONING') {
    pushBubble({
      id: `reasoning-${event.eventSequence}`,
      role: 'system',
      title: '推理片段',
      content: extractText(event.data),
      meta: formatTimestamp(toSafeNumber(event.timestamp)),
      taskId: event.taskId,
      tone: 'info',
    })
    return
  }

  if (event.event === 'AGENT_TOOL') {
    pushBubble({
      id: `tool-${event.eventSequence}`,
      role: 'system',
      title: '工具执行',
      content: extractText(event.data),
      meta: formatTimestamp(toSafeNumber(event.timestamp)),
      taskId: event.taskId,
      tone: 'info',
    })
    return
  }

  if (event.event === 'AGENT_FINISH') {
    stopAssistantStream(event.taskId)
    upsertAssistantBubble(event.taskId, {
      title: 'Agent',
      content: extractText(event.data),
      meta: `完成于 ${formatTimestamp(toSafeNumber(event.timestamp))}`,
      streaming: false,
    })
    sending.value = false
    lastFailedTaskId.value = null
    return
  }

  if (event.event === 'CHAT_ERROR') {
    stopAssistantStream(event.taskId)
    lastFailedTaskId.value = event.taskId
    upsertAssistantBubble(event.taskId, {
      title: 'Agent',
      meta: '执行中断',
      streaming: false,
    })
    pushBubble({
      id: `error-${event.eventSequence}`,
      role: 'system',
      title: '执行失败',
      content: extractText(event.data),
      meta: formatTimestamp(toSafeNumber(event.timestamp)),
      taskId: event.taskId,
      tone: 'error',
    })
    sending.value = false
    return
  }

  if (event.event === 'METHOD_ERROR') {
    pushBubble({
      id: `method-error-${event.eventSequence}`,
      role: 'system',
      title: '消息处理失败',
      content: extractText(event.data) || '消息已到达后端，但处理过程中出现异常。',
      meta: formatTimestamp(toSafeNumber(event.timestamp)),
      taskId: event.taskId,
      tone: 'error',
    })
    sending.value = false
    return
  }

  if (event.event === 'TASK_RECOVER') {
    pushBubble({
      id: `recover-${event.eventSequence}`,
      role: 'system',
      title: '恢复任务',
      content: `已根据失败任务 ${extractText(event.data)} 发起恢复。`,
      meta: formatTimestamp(toSafeNumber(event.timestamp)),
      taskId: event.taskId,
      tone: 'info',
    })
  }
}

async function loadAgentDetail() {
  if (!agentId.value) {
    await router.replace({ name: 'agents' })
    return
  }

  try {
    agentDetail.value = await fetchAgentDetail(agentId.value)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Agent 详情加载失败。'))
  }
}

async function prepareSession() {
  if (!routeSessionId.value) {
    showFeedback('error', '缺少会话参数，请从 Agent 管理页重新进入聊天。')
    return
  }

  loading.value = true

  try {
    const reconnectResult = await reconnectAgentSession(routeSessionId.value, {
      lastReceivedEventSequence: lastReceivedEventSequence.value || '0',
    })
    sessionId.value = reconnectResult.session.sessionId
    reconnectResult.missedEvents.forEach(handleAgentEvent)
    connectSocket()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '会话重连失败。'))
  } finally {
    loading.value = false
  }
}

function connectSocket() {
  if (!sessionId.value) {
    return
  }

  socket?.disconnect()
  socket = new AgentChatSocket({
    sessionId: sessionId.value,
    onEvent: handleAgentEvent,
    onConnectionChange: (value) => {
      connected.value = value
      if (!value) {
        sending.value = false
      }
    },
    onError: (message) => {
      showFeedback('error', message)
    },
  })
  socket.connect()
}

async function sendMessage() {
  if (!socket || !canSend.value) {
    return
  }

  const text = inputMessage.value.trim()
  inputMessage.value = ''
  sending.value = true
  clearFeedback()

  try {
    socket.send({
      agentId: agentId.value,
      sessionId: sessionId.value,
      message: text,
      lastReceivedEventSequence: lastReceivedEventSequence.value || '0',
    })
  } catch (error) {
    sending.value = false
    showFeedback('error', getErrorMessage(error, '消息发送失败。'))
  }
}

async function recoverTask() {
  if (!sessionId.value || !lastFailedTaskId.value) {
    return
  }

  recovering.value = true

  try {
    await recoverAgentTask(sessionId.value, { taskId: lastFailedTaskId.value })
    showFeedback('success', '恢复任务请求已提交。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '恢复任务失败。'))
  } finally {
    recovering.value = false
  }
}

async function closeSession() {
  if (!sessionId.value) {
    await router.push({ name: 'agents' })
    return
  }

  closing.value = true

  try {
    await closeAgentSession(sessionId.value)
    socket?.disconnect()
    await router.push({ name: 'agents' })
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '关闭会话失败。'))
  } finally {
    closing.value = false
  }
}

async function reconnect() {
  clearFeedback()
  connected.value = false
  await prepareSession()
}

onMounted(async () => {
  await loadAgentDetail()
  await prepareSession()
})

onBeforeUnmount(() => {
  streamTimers.forEach((timer) => clearTimeout(timer))
  streamTimers.clear()
  streamQueues.clear()
  if (scrollFrame != null) {
    cancelAnimationFrame(scrollFrame)
  }
  socket?.disconnect()
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

    <section class="management-page chat-workspace">
      <header class="chat-workspace__hero panel-card management-hero">
        <div class="chat-workspace__headline">
          <button type="button" class="back-link" @click="router.push({ name: 'agents' })">
            <ArrowLeft :size="16" aria-hidden="true" />
            返回 Agent 管理
          </button>
          <p class="section-kicker">Live Session</p>
          <div class="chat-workspace__title-row">
            <h2>{{ headerTitle }}</h2>
            <span class="status-pill" :class="{ 'status-pill--online': connected }">
              <Waves :size="14" aria-hidden="true" />
              {{ connectionLabel }}
            </span>
          </div>
          <p class="chat-workspace__subtitle">
            会话 {{ sessionId || routeSessionId }}，支持断线补发、流式输出和失败任务恢复。
          </p>
        </div>

        <div class="chat-workspace__actions">
          <button
            type="button"
            class="app-button app-button--secondary"
            :disabled="loading"
            @click="reconnect"
          >
            <RefreshCw :size="16" aria-hidden="true" />
            {{ loading ? '重连中...' : '重连补发' }}
          </button>
          <button
            type="button"
            class="app-button app-button--secondary"
            :disabled="!canRecover"
            @click="recoverTask"
          >
            <RotateCcw :size="16" aria-hidden="true" />
            {{ recovering ? '恢复中...' : '恢复失败任务' }}
          </button>
          <button
            type="button"
            class="app-button app-button--ghost app-button--danger-ghost"
            :disabled="closing"
            @click="closeSession"
          >
            <CircleAlert :size="16" aria-hidden="true" />
            {{ closing ? '关闭中...' : '关闭会话' }}
          </button>
        </div>
      </header>

      <div class="chat-grid">
        <aside class="chat-sidebar panel-card">
          <div class="sidebar-block">
            <span class="sidebar-block__label">Agent</span>
            <strong>{{ agentDetail?.agentName || agentId }}</strong>
            <p>{{ agentDetail?.description || '当前会话已绑定固定版本。' }}</p>
          </div>

          <div class="sidebar-block">
            <span class="sidebar-block__label">当前模型</span>
            <strong>{{ currentModelSummary }}</strong>
            <p>标识：{{ currentVersion?.modelIdentifier || '-' }} · 类型：{{ currentVersion?.modelType || '-' }}</p>
          </div>

          <div class="sidebar-block">
            <span class="sidebar-block__label">当前版本</span>
            <strong>v{{ Number(route.query.versionNo || 0) || '-' }}</strong>
            <p>展示当前会话实际绑定的 Agent 版本上下文。</p>
          </div>

          <div class="sidebar-block">
            <span class="sidebar-block__label">最近任务</span>
            <strong>{{ lastTaskId || '尚未开始' }}</strong>
            <p>如果任务失败，可以直接触发恢复。</p>
          </div>

          <div class="sidebar-block sidebar-block--accent">
            <span class="sidebar-block__label">投递通道</span>
            <strong>/app/agent/chat</strong>
            <p>/topic/session/{{ sessionId || routeSessionId }}</p>
          </div>
        </aside>

        <section class="chat-panel panel-card">
          <div ref="transcriptRef" class="transcript">
            <div v-if="bubbles.length === 0" class="transcript__empty">
              <Bot :size="24" aria-hidden="true" />
              <strong>会话已就绪</strong>
              <p>发送一条消息，查看 Agent 的流式输出。</p>
            </div>

            <article
              v-for="bubble in bubbles"
              :key="bubble.id"
              class="bubble"
              :class="[
                `bubble--${bubble.role}`,
                bubble.tone ? `bubble--${bubble.tone}` : '',
                bubble.streaming ? 'bubble--streaming' : '',
              ]"
            >
              <div class="bubble__header">
                <span class="bubble__title">
                  <UserRound v-if="bubble.role === 'user'" :size="14" aria-hidden="true" />
                  <Bot v-else-if="bubble.role === 'assistant'" :size="14" aria-hidden="true" />
                  <RadioTower v-else :size="14" aria-hidden="true" />
                  {{ bubble.title }}
                </span>
                <span class="bubble__meta">{{ bubble.meta }}</span>
              </div>

              <pre
                class="bubble__content"
                :class="{ 'bubble__content--streaming': bubble.streaming }"
              >{{ bubble.content }}</pre>
            </article>
          </div>

          <footer class="composer">
            <div class="input-shell input-shell--textarea composer__shell">
              <textarea
                v-model="inputMessage"
                class="app-textarea composer__textarea"
                rows="4"
                placeholder="输入问题或指令，按 Ctrl + Enter 发送"
                @keydown.ctrl.enter.prevent="sendMessage"
              />
            </div>

            <div class="composer__actions">
              <span class="composer__hint">
                <CornerDownLeft :size="14" aria-hidden="true" />
                Ctrl + Enter 发送
              </span>
              <button type="button" class="app-button" :disabled="!canSend" @click="sendMessage">
                <LoaderCircle v-if="sending" :size="16" class="is-spinning" aria-hidden="true" />
                <Send v-else :size="16" aria-hidden="true" />
                {{ sending ? '发送中...' : '发送消息' }}
              </button>
            </div>
          </footer>
        </section>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.chat-workspace {
  display: grid;
  gap: var(--layout-gap);
}

.chat-workspace__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: var(--panel-padding);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 0;
  color: #bfefff;
  background: transparent;
  cursor: pointer;
}

.chat-workspace__headline {
  max-width: 52rem;
}

.chat-workspace__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.chat-workspace__headline h2 {
  font-size: clamp(2rem, 2.5vw, 2.7rem);
  line-height: 1.02;
  letter-spacing: -0.03em;
}

.chat-workspace__subtitle {
  margin-top: 14px;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.chat-workspace__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 14px;
  color: #dce7ff;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.05);
}

.status-pill--online {
  color: #ddfff5;
  border-color: rgba(100, 216, 190, 0.2);
  background: rgba(100, 216, 190, 0.12);
}

.chat-grid {
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(0, 1fr);
  gap: var(--layout-gap);
}

.chat-sidebar,
.chat-panel {
  padding: var(--compact-panel-padding);
  border-radius: var(--sub-panel-radius);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.018)),
    rgba(6, 12, 24, 0.72);
}

.chat-sidebar {
  display: grid;
  gap: 14px;
  align-content: start;
}

.sidebar-block {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.sidebar-block--accent {
  background:
    radial-gradient(circle at top right, rgba(83, 184, 255, 0.12), transparent 36%),
    rgba(83, 184, 255, 0.08);
}

.sidebar-block__label {
  display: block;
  color: var(--color-ink-muted);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.sidebar-block strong {
  display: block;
  margin-top: 10px;
  color: var(--color-ink-strong);
}

.sidebar-block p {
  margin-top: 10px;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: min(72vh, 820px);
}

.transcript {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  padding-right: 4px;
}

.transcript__empty {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 280px;
  color: var(--color-ink-soft);
  text-align: center;
}

.transcript__empty strong {
  color: var(--color-ink-strong);
}

.bubble {
  max-width: min(78%, 680px);
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.bubble--user {
  align-self: flex-end;
  background: rgba(83, 184, 255, 0.1);
  border-color: rgba(83, 184, 255, 0.18);
}

.bubble--assistant {
  align-self: flex-start;
}

.bubble--streaming {
  border-color: rgba(126, 229, 199, 0.3);
  box-shadow: 0 0 0 1px rgba(126, 229, 199, 0.08), 0 18px 40px rgba(8, 18, 34, 0.28);
}

.bubble--system {
  align-self: center;
  max-width: min(88%, 760px);
  background: rgba(255, 255, 255, 0.04);
}

.bubble--error {
  border-color: rgba(255, 144, 151, 0.26);
  background: rgba(255, 144, 151, 0.08);
}

.bubble--info {
  border-color: rgba(83, 184, 255, 0.18);
}

.bubble__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.bubble__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--color-ink-strong);
  font-weight: 700;
}

.bubble__meta {
  color: var(--color-ink-muted);
  font-size: 0.84rem;
}

.bubble__content {
  margin: 12px 0 0;
  color: var(--color-ink);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-body);
  line-height: 1.72;
}

.bubble__content--streaming::after {
  content: '';
  display: inline-block;
  width: 0.72ch;
  height: 1.1em;
  margin-left: 0.18ch;
  vertical-align: -0.18em;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(126, 229, 199, 0.96), rgba(83, 184, 255, 0.9));
  box-shadow: 0 0 18px rgba(83, 184, 255, 0.32);
  animation: stream-caret 0.95s ease-in-out infinite;
}

.composer {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.composer__shell {
  min-height: 136px;
}

.composer__textarea {
  min-height: 128px;
}

.composer__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
}

.composer__hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--color-ink-muted);
  font-size: 0.88rem;
}

.is-spinning {
  animation: spin 0.9s linear infinite;
}

@keyframes stream-caret {
  0%,
  100% {
    opacity: 0.22;
    transform: scaleY(0.72);
  }

  50% {
    opacity: 1;
    transform: scaleY(1);
  }
}

@media (max-width: 1120px) {
  .chat-grid {
    grid-template-columns: 1fr;
  }

  .chat-panel {
    min-height: 620px;
  }
}

@media (max-width: 860px) {
  .chat-workspace__hero,
  .chat-workspace__actions,
  .composer__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .bubble {
    max-width: 100%;
  }
}
</style>
