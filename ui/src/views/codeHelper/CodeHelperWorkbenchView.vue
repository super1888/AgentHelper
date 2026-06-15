<template>
  <MainShell>
    <section class="code-helper page-stack">
      <section class="hero-panel panel-card">
        <div>
          <p class="section-kicker">Code Helper</p>
          <h1>Java 编程助手工作台</h1>
          <p>绑定本地工作区，基于 Spring AI 模型决策和受控工具调用完成代码阅读、搜索、修改建议、上下文压缩与审计。</p>
        </div>
        <div class="hero-actions">
          <button class="app-button app-button--ghost" type="button" @click="refreshAll">刷新</button>
          <button class="app-button" type="button" :disabled="!activeSessionId" @click="loadPrompt">查看提示词</button>
        </div>
      </section>

      <section v-if="feedback.message" class="feedback" :class="`feedback--${feedback.tone}`">
        {{ feedback.message }}
      </section>

      <section class="workspace-grid">
        <aside class="panel-card session-panel">
          <div class="panel-title-row">
            <div>
              <p class="section-kicker">Session</p>
              <h2>创建会话</h2>
            </div>
          </div>
          <form class="form-grid" @submit.prevent="createSession">
            <label>
              <span>会话名称</span>
              <input v-model="sessionForm.sessionName" class="app-input" placeholder="例如：订单模块重构" />
            </label>
            <label>
              <span>工作区路径 *</span>
              <input v-model="sessionForm.workspacePath" class="app-input" placeholder="D:\\code\\springAi" />
            </label>
            <label>
              <span>项目名称</span>
              <input v-model="sessionForm.projectName" class="app-input" placeholder="springAi" />
            </label>
            <label>
              <span>分支</span>
              <input v-model="sessionForm.branchName" class="app-input" placeholder="main" />
            </label>
            <label>
              <span>编程助手模型</span>
              <select v-model="sessionForm.modelCode" class="app-input">
                <option value="">自动选择默认模型</option>
                <option v-for="model in modelOptions" :key="model.modelCode" :value="model.modelCode">
                  {{ modelLabel(model) }}
                </option>
              </select>
            </label>
            <label>
              <span>允许命令</span>
              <input v-model="allowedCommandsText" class="app-input" placeholder="mvn,git,java,gradlew" />
            </label>
            <label class="full-row">
              <span>任务目标</span>
              <textarea v-model="sessionForm.taskDescription" class="app-textarea" rows="3" placeholder="描述本次编程助手要完成的目标" />
            </label>
            <button class="app-button full-row" type="submit" :disabled="creating">{{ creating ? '创建中...' : '创建会话' }}</button>
          </form>

          <div class="session-list">
            <div class="panel-title-row compact">
              <h3>历史会话</h3>
              <button class="link-button" type="button" @click="loadSessions">刷新</button>
            </div>
            <button
              v-for="item in sessions"
              :key="item.sessionId"
              class="session-item"
              :class="{ 'session-item--active': item.sessionId === activeSessionId }"
              type="button"
              @click="selectSession(item.sessionId)"
            >
              <strong>{{ item.sessionName || item.projectName || '未命名会话' }}</strong>
              <span>{{ item.projectName }} · {{ item.branchName }}</span>
              <small>{{ item.sessionId }}</small>
            </button>
            <p v-if="!sessions.length" class="empty-hint">暂无会话，请先创建。</p>
          </div>
        </aside>

        <main class="chat-column">
          <section class="panel-card chat-panel">
            <div class="panel-title-row">
              <div>
                <p class="section-kicker">Conversation</p>
                <h2>{{ activeSession?.sessionName || '请选择会话' }}</h2>
              </div>
              <span class="status-pill" :class="activeSession ? 'status-pill--active' : 'status-pill--draft'">
                {{ activeSession?.status || '未选择' }}
              </span>
            </div>

            <div class="summary-card" v-if="activeSession">
              <strong>上下文摘要</strong>
              <p>{{ activeSession.summary || '暂无摘要' }}</p>
            </div>

            <div class="message-list">
              <article v-for="(message, index) in activeMessages" :key="`${message.role}-${index}`" class="message-card" :class="`message-card--${message.role}`">
                <div class="message-meta">
                  <strong>{{ roleLabel(message.role) }}</strong>
                  <span>{{ message.timestamp || '-' }}</span>
                </div>
                <pre>{{ message.content }}</pre>
              </article>
              <p v-if="!activeMessages.length" class="empty-hint">会话消息会展示在这里。</p>
            </div>

            <form class="composer" @submit.prevent="sendMessage">
              <textarea v-model="messageText" class="app-textarea" rows="4" placeholder="例如：帮我查找 a2a 模块 Controller，并说明入口调用链" />
              <div class="composer-actions">
                <label class="inline-field">
                  <span>本次模型</span>
                  <select v-model="messageModelCode" class="app-input">
                    <option value="">使用会话模型</option>
                    <option v-for="model in modelOptions" :key="model.modelCode" :value="model.modelCode">
                      {{ modelLabel(model) }}
                    </option>
                  </select>
                </label>
                <button class="app-button" type="submit" :disabled="!activeSessionId || sending">{{ sending ? '发送中...' : '发送消息' }}</button>
              </div>
            </form>
          </section>

          <section class="lower-grid">
            <section class="panel-card tool-panel">
              <div class="panel-title-row compact">
                <div>
                  <p class="section-kicker">Tools</p>
                  <h3>显式工具调用</h3>
                </div>
                <button class="link-button" type="button" @click="loadTools">工具清单</button>
              </div>
              <div class="tool-form">
                <label>
                  <span>工具</span>
                  <select v-model="toolForm.toolName" class="app-input">
                    <option value="">选择工具</option>
                    <option v-for="tool in tools" :key="tool.toolName" :value="tool.toolName">{{ tool.toolName }} · {{ tool.riskLevel }}</option>
                  </select>
                </label>
                <label>
                  <span>参数 JSON</span>
                  <textarea v-model="toolArgumentsText" class="app-textarea mono" rows="6" />
                </label>
                <div class="tool-actions">
                  <button class="app-button app-button--ghost" type="button" :disabled="!toolForm.toolName" @click="checkPermission">权限检查</button>
                  <button class="app-button" type="button" :disabled="!activeSessionId || !toolForm.toolName || executingTool" @click="executeTool">{{ executingTool ? '执行中...' : '执行工具' }}</button>
                </div>
                <pre v-if="toolResult" class="result-box">{{ toolResult }}</pre>
              </div>
            </section>

            <section class="panel-card inspect-panel">
              <div class="panel-title-row compact">
                <div>
                  <p class="section-kicker">Inspect</p>
                  <h3>上下文与日志</h3>
                </div>
              </div>
              <div class="inspect-actions">
                <button class="app-button app-button--ghost" type="button" :disabled="!activeSessionId" @click="loadContext">刷新上下文</button>
                <button class="app-button app-button--ghost" type="button" :disabled="!activeSessionId" @click="compactContext">压缩上下文</button>
                <button class="app-button app-button--ghost" type="button" :disabled="!activeSessionId" @click="loadLogs">刷新日志</button>
              </div>
              <div class="log-list">
                <article v-for="log in logs" :key="log.logId" class="log-item">
                  <div>
                    <strong>{{ log.toolName }}</strong>
                    <span>{{ log.riskLevel }} · {{ log.success ? '成功' : '失败' }}</span>
                  </div>
                  <small>{{ log.createTime || '-' }} · {{ log.durationMillis || 0 }}ms</small>
                  <p>{{ log.errorMessage || compactText(log.responseText) }}</p>
                </article>
                <p v-if="!logs.length" class="empty-hint">暂无工具日志。</p>
              </div>
            </section>
          </section>
        </main>
      </section>

      <section v-if="promptText" class="panel-card prompt-panel">
        <div class="panel-title-row compact">
          <div>
            <p class="section-kicker">Prompt</p>
            <h3>当前系统提示词</h3>
          </div>
          <button class="link-button" type="button" @click="promptText = ''">关闭</button>
        </div>
        <pre>{{ promptText }}</pre>
      </section>
    </section>
  </MainShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import MainShell from '@/components/MainShell.vue'
import {
  checkCodeHelperPermission,
  compactCodeHelperContext,
  createCodeHelperSession,
  executeCodeHelperTool,
  queryCodeHelperContext,
  queryCodeHelperModelOptions,
  queryCodeHelperPrompt,
  queryCodeHelperSessions,
  queryCodeHelperToolLogs,
  queryCodeHelperTools,
  sendCodeHelperMessage,
} from '@/api/codeHelper'
import type { CodeHelperSession, CodeHelperToolDescriptor, CodeHelperToolLogResponse } from '@/types/codeHelper'
import type { ModelOption } from '@/types/core'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

const sessions = ref<CodeHelperSession[]>([])
const tools = ref<CodeHelperToolDescriptor[]>([])
const modelOptions = ref<ModelOption[]>([])
const logs = ref<CodeHelperToolLogResponse[]>([])
const activeSessionId = ref('')
const messageText = ref('')
const messageModelCode = ref('')
const promptText = ref('')
const toolArgumentsText = ref('{\n  "path": "."\n}')
const toolResult = ref('')
const allowedCommandsText = ref('mvn,git,java,gradlew,./mvnw,mvnw')
const creating = ref(false)
const sending = ref(false)
const executingTool = ref(false)
const feedback = ref<{ tone: FeedbackTone; message: string }>({ tone: 'info', message: '' })

const sessionForm = reactive({
  sessionName: '',
  workspacePath: '',
  projectName: '',
  branchName: 'main',
  taskDescription: '',
  modelCode: '',
})

const toolForm = reactive({
  toolName: '',
})

const activeSession = computed(() => sessions.value.find((item) => item.sessionId === activeSessionId.value) ?? null)
const activeMessages = computed(() => activeSession.value?.messages ?? [])

onMounted(async () => {
  await Promise.all([loadSessions(), loadTools(), loadModelOptions()])
})

async function refreshAll() {
  await Promise.all([loadSessions(), loadTools(), loadModelOptions(), activeSessionId.value ? loadLogs() : Promise.resolve()])
}

async function loadSessions() {
  try {
    sessions.value = await queryCodeHelperSessions()
    if (!activeSessionId.value && sessions.value.length) {
      activeSessionId.value = sessions.value[0].sessionId
      await loadLogs()
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '会话加载失败'))
  }
}

async function createSession() {
  if (!sessionForm.workspacePath.trim()) {
    showFeedback('error', '工作区路径不能为空')
    return
  }
  creating.value = true
  try {
    const session = await createCodeHelperSession({
      sessionName: sessionForm.sessionName.trim() || undefined,
      workspacePath: sessionForm.workspacePath.trim(),
      projectName: sessionForm.projectName.trim() || undefined,
      branchName: sessionForm.branchName.trim() || undefined,
      taskDescription: sessionForm.taskDescription.trim() || undefined,
      modelCode: sessionForm.modelCode.trim() || undefined,
      allowedCommands: parseCsv(allowedCommandsText.value),
    })
    upsertSession(session)
    activeSessionId.value = session.sessionId
    showFeedback('success', '会话创建成功')
    await loadLogs()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '会话创建失败'))
  } finally {
    creating.value = false
  }
}

async function selectSession(sessionId: string) {
  activeSessionId.value = sessionId
  promptText.value = ''
  await loadLogs()
}

async function sendMessage() {
  if (!activeSessionId.value || !messageText.value.trim()) {
    showFeedback('error', '请选择会话并输入消息')
    return
  }
  sending.value = true
  try {
    const session = await sendCodeHelperMessage(activeSessionId.value, {
      content: messageText.value.trim(),
      modelCode: messageModelCode.value.trim() || undefined,
      autoToolCall: true,
    })
    upsertSession(session)
    messageText.value = ''
    showFeedback('success', '消息发送成功')
    await loadLogs()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '消息发送失败'))
  } finally {
    sending.value = false
  }
}

async function loadContext() {
  if (!activeSessionId.value) {
    return
  }
  try {
    const context = await queryCodeHelperContext(activeSessionId.value)
    showFeedback('info', `上下文已刷新：${context.recentMessages.length} 条消息，${context.tasks.length} 个任务`)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '上下文加载失败'))
  }
}

async function compactContext() {
  if (!activeSessionId.value) {
    return
  }
  try {
    await compactCodeHelperContext(activeSessionId.value, '前端手动触发上下文压缩')
    await loadSessions()
    showFeedback('success', '上下文压缩完成')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '上下文压缩失败'))
  }
}

async function loadPrompt() {
  if (!activeSessionId.value) {
    showFeedback('error', '请先选择会话')
    return
  }
  try {
    promptText.value = await queryCodeHelperPrompt(activeSessionId.value)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '提示词加载失败'))
  }
}

async function loadTools() {
  try {
    tools.value = await queryCodeHelperTools()
    if (!toolForm.toolName && tools.value.length) {
      toolForm.toolName = tools.value[0].toolName
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '工具清单加载失败'))
  }
}

async function loadModelOptions() {
  try {
    modelOptions.value = await queryCodeHelperModelOptions()
    if (!sessionForm.modelCode) {
      sessionForm.modelCode = modelOptions.value.find((model) => model.defaultModel)?.modelCode ?? ''
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '模型选项加载失败'))
  }
}

async function checkPermission() {
  if (!toolForm.toolName) {
    return
  }
  try {
    const args = parseToolArguments()
    const decision = await checkCodeHelperPermission({
      toolName: toolForm.toolName,
      command: typeof args.command === 'string' ? args.command : undefined,
      allowedCommands: parseCsv(allowedCommandsText.value),
    })
    toolResult.value = JSON.stringify(decision, null, 2)
    showFeedback(decision.allowed ? 'success' : 'error', decision.reason)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '权限检查失败'))
  }
}

async function executeTool() {
  if (!activeSessionId.value || !toolForm.toolName) {
    return
  }
  executingTool.value = true
  try {
    const result = await executeCodeHelperTool({
      sessionId: activeSessionId.value,
      toolName: toolForm.toolName,
      arguments: parseToolArguments(),
      allowedCommands: parseCsv(allowedCommandsText.value),
    })
    toolResult.value = JSON.stringify(result, null, 2)
    showFeedback(result.success ? 'success' : 'error', result.message)
    await loadLogs()
    await loadSessions()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '工具执行失败'))
  } finally {
    executingTool.value = false
  }
}

async function loadLogs() {
  if (!activeSessionId.value) {
    logs.value = []
    return
  }
  try {
    logs.value = await queryCodeHelperToolLogs(activeSessionId.value)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '工具日志加载失败'))
  }
}

function parseToolArguments() {
  try {
    return JSON.parse(toolArgumentsText.value || '{}') as Record<string, unknown>
  } catch (error) {
    throw new Error('工具参数不是合法 JSON')
  }
}

function parseCsv(value: string) {
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

function upsertSession(session: CodeHelperSession) {
  const index = sessions.value.findIndex((item) => item.sessionId === session.sessionId)
  if (index >= 0) {
    sessions.value.splice(index, 1, session)
  } else {
    sessions.value.unshift(session)
  }
}

function roleLabel(role: string) {
  const labels: Record<string, string> = {
    user: '用户',
    assistant: '助手',
    system: '系统',
  }
  return labels[role] ?? role
}

function modelLabel(model: ModelOption) {
  const defaultTag = model.defaultModel ? '默认 · ' : ''
  const provider = model.providerName || model.providerEnum
  return `${defaultTag}${model.modelName} / ${model.modelIdentifier} / ${provider}`
}

function compactText(value?: string | null) {
  if (!value) {
    return '无输出'
  }
  return value.length > 120 ? `${value.slice(0, 120)}...` : value
}

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}
</script>

<style scoped>
.code-helper {
  gap: 18px;
}

.hero-panel,
.panel-title-row,
.hero-actions,
.composer-actions,
.tool-actions,
.inspect-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.hero-panel {
  padding: var(--panel-padding);
}

.hero-panel h1,
.panel-title-row h2,
.panel-title-row h3 {
  margin: 0;
  color: var(--color-ink-strong);
}

.hero-panel p:last-child,
.summary-card p,
.empty-hint,
.log-item p {
  margin: 0;
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.36fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.session-panel,
.chat-panel,
.tool-panel,
.inspect-panel,
.prompt-panel {
  padding: var(--panel-padding);
}

.form-grid,
.tool-form,
.session-list,
.log-list {
  display: grid;
  gap: 12px;
}

.form-grid label,
.tool-form label,
.inline-field {
  display: grid;
  gap: 8px;
  color: var(--color-ink-muted);
  font-size: 0.82rem;
}

.full-row {
  grid-column: 1 / -1;
}

.app-textarea {
  resize: vertical;
}

.session-list {
  margin-top: 18px;
}

.session-item {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 13px 14px;
  text-align: left;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.035);
  color: var(--color-ink-soft);
  cursor: pointer;
}

.session-item strong {
  color: var(--color-ink-strong);
}

.session-item small,
.log-item small,
.message-meta span {
  color: var(--color-ink-muted);
  font-size: 0.76rem;
}

.session-item--active {
  border-color: rgba(137, 228, 255, 0.45);
  background: rgba(137, 228, 255, 0.08);
}

.chat-column,
.message-list,
.lower-grid {
  display: grid;
  gap: 18px;
}

.summary-card,
.result-box,
.prompt-panel pre {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.035);
}

.message-list {
  max-height: 520px;
  overflow: auto;
  padding-right: 4px;
}

.message-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.035);
}

.message-card--user {
  border-color: rgba(137, 228, 255, 0.24);
}

.message-card--assistant {
  border-color: rgba(166, 255, 203, 0.22);
}

.message-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.message-card pre,
.result-box,
.prompt-panel pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--color-ink-soft);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  line-height: 1.6;
}

.composer {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.inline-field {
  min-width: 260px;
}

.lower-grid {
  grid-template-columns: minmax(0, 0.9fr) minmax(300px, 0.7fr);
}

.inspect-actions,
.tool-actions {
  flex-wrap: wrap;
  justify-content: flex-start;
}

.log-item {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.035);
}

.log-item div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.feedback {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.feedback--success {
  color: #a6ffcb;
  background: rgba(66, 211, 146, 0.08);
}

.feedback--error {
  color: #ff9f9f;
  background: rgba(255, 89, 89, 0.08);
}

.feedback--info {
  color: #9edfff;
  background: rgba(89, 172, 255, 0.08);
}

.link-button {
  border: 0;
  background: transparent;
  color: var(--color-primary);
  cursor: pointer;
}

.status-pill--active {
  background: rgba(66, 211, 146, 0.12);
  color: #a6ffcb;
}

.status-pill--draft {
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-ink-muted);
}

@media (max-width: 1180px) {
  .workspace-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-panel,
  .panel-title-row,
  .composer-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .inline-field {
    min-width: 0;
  }
}
</style>
