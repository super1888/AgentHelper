<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Play, RefreshCw, Rocket, Save, Send, Undo2 } from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  dispatchA2aTask,
  fetchA2aStats,
  publishA2aAgent,
  queryA2aAgents,
  queryA2aLogs,
  queryA2aRoutes,
  queryA2aTasks,
  queryDeletedA2aAgents,
  removeA2aAgent,
  restoreA2aAgent,
  saveA2aAgent,
  saveA2aRoute,
} from '@/api/a2a'
import type {
  A2aAgentCardItem,
  A2aAgentCardPayload,
  A2aDispatchPayload,
  A2aLogItem,
  A2aRouteItem,
  A2aRoutePayload,
  A2aStatistics,
  A2aTaskItem,
} from '@/types/a2a'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

const loading = ref(false)
const savingAgent = ref(false)
const savingRoute = ref(false)
const dispatching = ref(false)
const actioning = ref(false)
const selectedAgentId = ref<number | null>(null)
const logTaskCode = ref('')
const feedback = ref<{ tone: FeedbackTone; message: string } | null>(null)
const agents = ref<A2aAgentCardItem[]>([])
const deletedAgents = ref<A2aAgentCardItem[]>([])
const routes = ref<A2aRouteItem[]>([])
const tasks = ref<A2aTaskItem[]>([])
const logs = ref<A2aLogItem[]>([])
const lastTask = ref<A2aTaskItem | null>(null)
const stats = ref<A2aStatistics>({ agentCount: 0, publishedAgentCount: 0, routeCount: 0, taskCount: 0, successTaskCount: 0, failedTaskCount: 0, logCount: 0 })
const filters = reactive({ keyword: '', publishStatus: 'ALL' })

const agentForm = reactive({
  agentCode: '',
  agentName: '',
  description: '',
  endpointUrl: 'http://127.0.0.1:18080/a2a/task',
  protocolVersion: '1.0',
  transportType: 'HTTP',
  authType: 'NONE',
  agentStatus: 'ENABLED',
  riskLevel: 'MEDIUM',
  trustLevel: 'INTERNAL',
  ownerTeam: 'agent-platform',
  timeoutMs: 10000,
  retryTimes: 1,
  rateLimitQps: 20,
  successRateSlo: 99,
  capabilitiesText: 'chat,tool_call,streaming',
  inputModesText: 'text/json',
  outputModesText: 'text/json',
  authConfigText: '{\n  "mode": "none"\n}',
  metadataText: '{\n  "region": "cn-shanghai",\n  "tier": "production"\n}',
  remark: '',
})

const routeForm = reactive({
  routeCode: '',
  routeName: '',
  sourceAgentCode: '',
  targetAgentCode: '',
  taskType: 'chat.completion',
  routeStatus: 'ENABLED',
  priorityNo: 100,
  failoverEnabled: 1,
  fallbackAgentCodes: '',
  remark: '',
})

const dispatchForm = reactive({
  sourceAgentCode: '',
  targetAgentCode: '',
  taskType: 'chat.completion',
  payloadText: '{\n  "messages": [\n    { "role": "user", "content": "请处理企业知识库问答请求" }\n  ],\n  "sessionId": "demo-session"\n}',
})

const selectedAgent = computed(() => selectedAgentId.value == null ? null : agents.value.find((item) => item.id === selectedAgentId.value) ?? null)
const filteredAgents = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return agents.value.filter((item) => {
    const matchKeyword = !keyword || [item.agentCode, item.agentName, item.description, item.ownerTeam].filter(Boolean).some((value) => String(value).toLowerCase().includes(keyword))
    const matchStatus = filters.publishStatus === 'ALL' || item.publishStatus === filters.publishStatus
    return matchKeyword && matchStatus
  })
})

function notice(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function parseJson<T>(value: string, label: string) {
  try {
    return JSON.parse(value) as T
  } catch {
    throw new Error(`${label} 不是合法 JSON`)
  }
}

function splitText(value: string) {
  return value.split(/[,\n]/).map((item) => item.trim()).filter(Boolean)
}

function formatTime(value?: number | null) {
  return value ? new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value) : '未记录'
}

function resetAgentForm() {
  selectedAgentId.value = null
  agentForm.agentCode = ''
  agentForm.agentName = ''
  agentForm.description = ''
  agentForm.endpointUrl = 'http://127.0.0.1:18080/a2a/task'
  agentForm.protocolVersion = '1.0'
  agentForm.transportType = 'HTTP'
  agentForm.authType = 'NONE'
  agentForm.agentStatus = 'ENABLED'
  agentForm.riskLevel = 'MEDIUM'
  agentForm.trustLevel = 'INTERNAL'
  agentForm.ownerTeam = 'agent-platform'
  agentForm.timeoutMs = 10000
  agentForm.retryTimes = 1
  agentForm.rateLimitQps = 20
  agentForm.successRateSlo = 99
  agentForm.capabilitiesText = 'chat,tool_call,streaming'
  agentForm.inputModesText = 'text/json'
  agentForm.outputModesText = 'text/json'
  agentForm.authConfigText = '{\n  "mode": "none"\n}'
  agentForm.metadataText = '{\n  "region": "cn-shanghai",\n  "tier": "production"\n}'
  agentForm.remark = ''
}

function fillAgentForm(item: A2aAgentCardItem) {
  selectedAgentId.value = item.id
  agentForm.agentCode = item.agentCode
  agentForm.agentName = item.agentName
  agentForm.description = item.description ?? ''
  agentForm.endpointUrl = item.endpointUrl
  agentForm.protocolVersion = item.protocolVersion
  agentForm.transportType = item.transportType
  agentForm.authType = item.authType
  agentForm.agentStatus = item.agentStatus
  agentForm.riskLevel = item.riskLevel
  agentForm.trustLevel = item.trustLevel
  agentForm.ownerTeam = item.ownerTeam ?? ''
  agentForm.timeoutMs = item.timeoutMs ?? 10000
  agentForm.retryTimes = item.retryTimes ?? 1
  agentForm.rateLimitQps = item.rateLimitQps ?? 20
  agentForm.successRateSlo = item.successRateSlo ?? 99
  agentForm.capabilitiesText = (item.capabilities ?? []).join(',')
  agentForm.inputModesText = (item.inputModes ?? []).join(',')
  agentForm.outputModesText = (item.outputModes ?? []).join(',')
  agentForm.authConfigText = JSON.stringify(item.authConfig ?? {}, null, 2)
  agentForm.metadataText = JSON.stringify(item.metadata ?? {}, null, 2)
  agentForm.remark = item.remark ?? ''
}

function buildAgentPayload(): A2aAgentCardPayload {
  return {
    agentCode: agentForm.agentCode.trim(),
    agentName: agentForm.agentName.trim(),
    description: agentForm.description.trim() || null,
    endpointUrl: agentForm.endpointUrl.trim(),
    protocolVersion: agentForm.protocolVersion.trim(),
    transportType: agentForm.transportType,
    authType: agentForm.authType,
    agentStatus: agentForm.agentStatus,
    riskLevel: agentForm.riskLevel,
    trustLevel: agentForm.trustLevel,
    ownerTeam: agentForm.ownerTeam.trim() || null,
    timeoutMs: agentForm.timeoutMs,
    retryTimes: agentForm.retryTimes,
    rateLimitQps: agentForm.rateLimitQps,
    successRateSlo: agentForm.successRateSlo,
    capabilities: splitText(agentForm.capabilitiesText),
    inputModes: splitText(agentForm.inputModesText),
    outputModes: splitText(agentForm.outputModesText),
    authConfig: parseJson<Record<string, unknown>>(agentForm.authConfigText, '认证配置'),
    metadata: parseJson<Record<string, unknown>>(agentForm.metadataText, '扩展元数据'),
    remark: agentForm.remark.trim() || null,
  }
}

function buildRoutePayload(): A2aRoutePayload {
  return {
    routeCode: routeForm.routeCode.trim(),
    routeName: routeForm.routeName.trim(),
    sourceAgentCode: routeForm.sourceAgentCode.trim() || null,
    targetAgentCode: routeForm.targetAgentCode.trim(),
    taskType: routeForm.taskType.trim(),
    routeStatus: routeForm.routeStatus,
    priorityNo: routeForm.priorityNo,
    failoverEnabled: routeForm.failoverEnabled,
    fallbackAgentCodes: routeForm.fallbackAgentCodes.trim() || null,
    remark: routeForm.remark.trim() || null,
  }
}

function buildDispatchPayload(): A2aDispatchPayload {
  return {
    sourceAgentCode: dispatchForm.sourceAgentCode.trim() || null,
    targetAgentCode: dispatchForm.targetAgentCode.trim() || null,
    taskType: dispatchForm.taskType.trim(),
    payload: parseJson<Record<string, unknown>>(dispatchForm.payloadText, '调度负载'),
  }
}

async function loadDashboard() {
  loading.value = true
  try {
    const [agentList, deletedList, routeList, taskList, statValue, logList] = await Promise.all([
      queryA2aAgents(),
      queryDeletedA2aAgents(),
      queryA2aRoutes(),
      queryA2aTasks(),
      fetchA2aStats(),
      queryA2aLogs(logTaskCode.value.trim() || undefined),
    ])
    agents.value = agentList
    deletedAgents.value = deletedList
    routes.value = routeList
    tasks.value = taskList
    stats.value = statValue
    logs.value = logList
  } catch (error) {
    notice('error', getErrorMessage(error, 'A2A 控制台数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function submitAgent() {
  savingAgent.value = true
  try {
    const result = await saveA2aAgent(buildAgentPayload())
    notice('success', `智能体卡片已保存：${result.agentName}`)
    await loadDashboard()
    fillAgentForm(result)
  } catch (error) {
    notice('error', getErrorMessage(error, '保存智能体卡片失败'))
  } finally {
    savingAgent.value = false
  }
}

async function submitRoute() {
  savingRoute.value = true
  try {
    const result = await saveA2aRoute(buildRoutePayload())
    notice('success', `路由策略已保存：${result.routeName}`)
    await loadDashboard()
  } catch (error) {
    notice('error', getErrorMessage(error, '保存路由策略失败'))
  } finally {
    savingRoute.value = false
  }
}

async function handlePublish(agentId: number) {
  actioning.value = true
  try {
    await publishA2aAgent(agentId)
    notice('success', '智能体已发布，可参与跨智能体调度')
    await loadDashboard()
  } catch (error) {
    notice('error', getErrorMessage(error, '发布智能体失败'))
  } finally {
    actioning.value = false
  }
}

async function handleDelete(agentId: number) {
  actioning.value = true
  try {
    await removeA2aAgent(agentId)
    notice('success', '智能体卡片已下线并移入回收区')
    await loadDashboard()
    if (selectedAgentId.value === agentId) resetAgentForm()
  } catch (error) {
    notice('error', getErrorMessage(error, '删除智能体卡片失败'))
  } finally {
    actioning.value = false
  }
}

async function handleRestore(agentId: number) {
  actioning.value = true
  try {
    await restoreA2aAgent(agentId)
    notice('success', '智能体卡片已恢复')
    await loadDashboard()
  } catch (error) {
    notice('error', getErrorMessage(error, '恢复智能体卡片失败'))
  } finally {
    actioning.value = false
  }
}

async function handleDispatch() {
  dispatching.value = true
  try {
    const result = await dispatchA2aTask(buildDispatchPayload())
    lastTask.value = result
    logTaskCode.value = result.taskCode
    notice('success', `任务已派发：${result.taskCode}`)
    await loadDashboard()
  } catch (error) {
    notice('error', getErrorMessage(error, '派发任务失败'))
  } finally {
    dispatching.value = false
  }
}

async function loadLogs() {
  try {
    logs.value = await queryA2aLogs(logTaskCode.value.trim() || undefined)
  } catch (error) {
    notice('error', getErrorMessage(error, '加载日志失败'))
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <MainShell>
    <section class="a2a-page">
      <header class="hero panel-card">
        <div>
          <p class="section-kicker">跨智能体控制面</p>
          <h2>A2A 跨智能体协同与治理台</h2>
          <p>统一管理远程智能体卡片、任务路由、实时派发和链路审计，覆盖注册、发布、限流、回收和可观测需求。</p>
        </div>
        <div class="actions">
          <button type="button" class="app-button app-button--secondary" :disabled="loading" @click="loadDashboard"><RefreshCw :size="16" />刷新</button>
          <button type="button" class="app-button" @click="resetAgentForm"><Rocket :size="16" />新建智能体</button>
        </div>
      </header>

      <section class="stats">
        <article class="panel-card"><span>智能体总数</span><strong>{{ stats.agentCount }}</strong><small>已发布 {{ stats.publishedAgentCount }}</small></article>
        <article class="panel-card"><span>路由策略</span><strong>{{ stats.routeCount }}</strong><small>任务类型与备用链路</small></article>
        <article class="panel-card"><span>调度任务</span><strong>{{ stats.taskCount }}</strong><small>成功 {{ stats.successTaskCount }} / 失败 {{ stats.failedTaskCount }}</small></article>
        <article class="panel-card"><span>审计日志</span><strong>{{ stats.logCount }}</strong><small>按 taskCode 追踪</small></article>
      </section>

      <section class="grid grid--main">
        <article class="panel-card">
          <div class="panel-head">
            <div><p class="section-kicker">智能体卡片</p><h3>{{ selectedAgent ? '编辑已选智能体' : '注册新智能体' }}</h3></div>
            <button type="button" class="app-button app-button--ghost" @click="resetAgentForm"><Undo2 :size="15" />重置</button>
          </div>
          <div class="form-grid">
            <label><span>智能体编码</span><input v-model="agentForm.agentCode" type="text" /></label>
            <label><span>智能体名称</span><input v-model="agentForm.agentName" type="text" /></label>
            <label class="wide"><span>服务地址</span><input v-model="agentForm.endpointUrl" type="text" /></label>
            <label><span>协议版本</span><input v-model="agentForm.protocolVersion" type="text" /></label>
            <label><span>传输协议</span><select v-model="agentForm.transportType"><option>HTTP</option><option>GRPC</option><option>MQ</option></select></label>
            <label><span>认证模式</span><select v-model="agentForm.authType"><option>NONE</option><option>API_KEY</option><option>OAUTH2</option></select></label>
            <label><span>运行状态</span><select v-model="agentForm.agentStatus"><option>ENABLED</option><option>DISABLED</option></select></label>
            <label><span>风险等级</span><select v-model="agentForm.riskLevel"><option>LOW</option><option>MEDIUM</option><option>HIGH</option></select></label>
            <label><span>信任等级</span><select v-model="agentForm.trustLevel"><option>INTERNAL</option><option>PARTNER</option><option>EXTERNAL</option></select></label>
            <label><span>归属团队</span><input v-model="agentForm.ownerTeam" type="text" /></label>
            <label><span>超时毫秒</span><input v-model.number="agentForm.timeoutMs" type="number" min="1000" /></label>
            <label><span>重试次数</span><input v-model.number="agentForm.retryTimes" type="number" min="0" max="3" /></label>
            <label><span>QPS 限额</span><input v-model.number="agentForm.rateLimitQps" type="number" min="1" /></label>
            <label><span>SLO 成功率</span><input v-model.number="agentForm.successRateSlo" type="number" min="1" max="100" /></label>
            <label class="wide"><span>能力清单</span><input v-model="agentForm.capabilitiesText" type="text" placeholder="chat,tool_call,workflow" /></label>
            <label><span>输入模式</span><input v-model="agentForm.inputModesText" type="text" /></label>
            <label><span>输出模式</span><input v-model="agentForm.outputModesText" type="text" /></label>
            <label class="wide"><span>描述</span><textarea v-model="agentForm.description" rows="3"></textarea></label>
            <label class="wide"><span>认证配置 JSON</span><textarea v-model="agentForm.authConfigText" rows="5"></textarea></label>
            <label class="wide"><span>扩展元数据 JSON</span><textarea v-model="agentForm.metadataText" rows="5"></textarea></label>
            <label class="wide"><span>备注</span><textarea v-model="agentForm.remark" rows="2"></textarea></label>
          </div>
          <button type="button" class="app-button submit" :disabled="savingAgent" @click="submitAgent"><Save :size="16" />{{ savingAgent ? '保存中...' : '保存智能体卡片' }}</button>
        </article>

        <article class="panel-card">
          <div class="panel-head">
            <div><p class="section-kicker">注册中心</p><h3>智能体卡片列表</h3></div>
            <div class="actions"><input v-model="filters.keyword" type="search" placeholder="搜索" /><select v-model="filters.publishStatus"><option value="ALL">全部</option><option>DRAFT</option><option>PUBLISHED</option><option>OFFLINE</option></select></div>
          </div>
          <div class="cards">
            <button v-for="item in filteredAgents" :key="item.id" type="button" class="agent-card" :class="{ active: item.id === selectedAgentId }" @click="fillAgentForm(item)">
              <div><strong>{{ item.agentName }}</strong><span class="chip" :data-status="item.publishStatus">{{ item.publishStatus }}</span></div>
              <p>{{ item.agentCode }} · {{ item.ownerTeam || '未分配团队' }}</p>
              <small>{{ item.endpointUrl }}</small>
               <p>{{ item.riskLevel }} / {{ item.trustLevel }} / {{ item.transportType }} / 重试 {{ item.retryTimes ?? 1 }} 次</p>
              <div class="actions"><button type="button" class="mini" :disabled="actioning" @click.stop="handlePublish(item.id)">发布</button><button type="button" class="mini danger" :disabled="actioning" @click.stop="handleDelete(item.id)">下线</button></div>
            </button>
          </div>
          <h4>已删除智能体</h4>
          <div v-if="deletedAgents.length" class="cards">
            <div v-for="item in deletedAgents" :key="item.id" class="deleted"><span>{{ item.agentName }} / {{ item.agentCode }}</span><button type="button" class="mini" :disabled="actioning" @click="handleRestore(item.id)">恢复</button></div>
          </div>
          <p v-else class="empty">当前没有回收中的智能体卡片。</p>
        </article>
      </section>

      <section class="grid grid--three">
        <article class="panel-card">
          <div class="panel-head"><div><p class="section-kicker">路由治理</p><h3>路由策略</h3></div></div>
          <div class="form-grid">
            <label><span>路由编码</span><input v-model="routeForm.routeCode" type="text" /></label>
            <label><span>策略名称</span><input v-model="routeForm.routeName" type="text" /></label>
            <label><span>源智能体</span><input v-model="routeForm.sourceAgentCode" type="text" placeholder="可留空" /></label>
            <label><span>目标智能体</span><input v-model="routeForm.targetAgentCode" type="text" /></label>
            <label><span>任务类型</span><input v-model="routeForm.taskType" type="text" /></label>
            <label><span>状态</span><select v-model="routeForm.routeStatus"><option>ENABLED</option><option>DISABLED</option></select></label>
            <label><span>优先级</span><input v-model.number="routeForm.priorityNo" type="number" min="1" /></label>
            <label><span>故障转移</span><select v-model.number="routeForm.failoverEnabled"><option :value="1">开启</option><option :value="0">关闭</option></select></label>
            <label class="wide"><span>备用智能体</span><input v-model="routeForm.fallbackAgentCodes" type="text" /></label>
            <label class="wide"><span>备注</span><textarea v-model="routeForm.remark" rows="3"></textarea></label>
          </div>
          <button type="button" class="app-button submit" :disabled="savingRoute" @click="submitRoute"><Save :size="16" />{{ savingRoute ? '保存中...' : '保存路由策略' }}</button>
        </article>

        <article class="panel-card">
          <div class="panel-head"><div><p class="section-kicker">调度演练</p><h3>任务派发</h3></div></div>
          <div class="form-grid">
            <label><span>源智能体</span><input v-model="dispatchForm.sourceAgentCode" type="text" placeholder="可留空" /></label>
            <label><span>目标智能体</span><input v-model="dispatchForm.targetAgentCode" type="text" placeholder="可留空" /></label>
            <label class="wide"><span>任务类型</span><input v-model="dispatchForm.taskType" type="text" /></label>
            <label class="wide"><span>请求载荷 JSON</span><textarea v-model="dispatchForm.payloadText" rows="10"></textarea></label>
          </div>
          <button type="button" class="app-button submit" :disabled="dispatching" @click="handleDispatch"><Send :size="16" />{{ dispatching ? '派发中...' : '执行派发' }}</button>
          <div v-if="lastTask" class="preview"><strong>{{ lastTask.taskCode }}</strong><span class="chip" :data-status="lastTask.taskStatus">{{ lastTask.taskStatus }}</span><p>目标智能体：{{ lastTask.targetAgentCode }}，耗时：{{ lastTask.elapsedMs ?? 0 }}ms</p></div>
        </article>

        <article class="panel-card">
          <div class="panel-head"><div><p class="section-kicker">生效策略</p><h3>已生效路由</h3></div></div>
          <div class="rows">
            <div v-for="item in routes" :key="item.id" class="row"><span>{{ item.routeName }}</span><span>{{ item.taskType }}</span><span>{{ item.targetAgentCode }}</span><span>{{ item.priorityNo ?? 100 }}</span></div>
            <p v-if="!routes.length" class="empty">当前还没有路由策略。</p>
          </div>
        </article>
      </section>

      <section class="grid grid--tasks">
        <article class="panel-card">
          <div class="panel-head"><div><p class="section-kicker">任务记录</p><h3>最近任务</h3></div></div>
          <div class="rows">
            <div v-for="item in tasks" :key="item.id" class="row clickable" @click="logTaskCode = item.taskCode"><span>{{ item.taskCode }}</span><span>{{ item.taskStatus }}</span><span>{{ item.targetAgentCode }}</span><span>{{ item.elapsedMs ?? 0 }}ms</span></div>
            <p v-if="!tasks.length" class="empty">暂无派发任务记录。</p>
          </div>
        </article>
        <article class="panel-card">
            <div class="panel-head"><div><p class="section-kicker">审计日志</p><h3>执行日志</h3></div><div class="actions"><input v-model="logTaskCode" type="search" placeholder="任务编码" /><button type="button" class="app-button app-button--secondary" @click="loadLogs"><Play :size="16" />查询</button></div></div>
          <div class="rows">
            <div v-for="item in logs" :key="item.id" class="row row--log"><span>{{ item.taskCode }}</span><span>{{ item.targetAgentCode }} / #{{ item.attemptNo ?? 0 }}</span><span>{{ item.executeStatus || item.eventType }}</span><span>{{ item.retryIndex ?? 0 }}</span><span>{{ item.successFlag === 1 ? '成功' : '失败' }}</span><span>{{ formatTime(item.createTime) }}</span></div>
            <p v-if="!logs.length" class="empty">暂无审计日志。</p>
          </div>
        </article>
      </section>
    </section>
    <AppFeedbackDialog :model-value="Boolean(feedback)" :tone="feedback?.tone ?? 'info'" :message="feedback?.message ?? ''" @update:model-value="feedback = null" />
  </MainShell>
</template>

<style scoped>
.a2a-page,
.cards,
.rows {
  display: grid;
  gap: var(--layout-gap);
}

.a2a-page {
  min-height: 100%;
  min-width: 0;
}

.cards,
.rows {
  overflow: auto;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.hero,
.panel-head,
.actions,
.agent-card div,
.deleted {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.hero,
.panel-head,
.actions {
  align-items: flex-start;
  flex-wrap: wrap;
}

.hero {
  padding: var(--panel-padding);
  background:
    radial-gradient(circle at top left, rgba(34, 197, 94, 0.22), transparent 28%),
    radial-gradient(circle at right, rgba(59, 130, 246, 0.18), transparent 26%);
}

.hero h2,
.hero p,
.panel-head h3,
.panel-head p,
.agent-card p,
.agent-card small,
.deleted p,
.empty,
.preview p {
  margin: 0;
}

.stats,
.grid {
  display: grid;
  gap: 18px;
  align-items: start;
  min-width: 0;
}

.stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stats article {
  display: grid;
  gap: 8px;
}

.stats strong {
  font-size: 2rem;
  line-height: 1.2;
}

.grid--main,
.grid--tasks {
  grid-template-columns: minmax(0, 1.18fr) minmax(360px, 0.92fr);
}

.grid--three {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 0.9fr);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.wide {
  grid-column: 1 / -1;
}

label {
  display: grid;
  gap: 8px;
}

label span,
.stats span,
.stats small,
.empty {
  color: rgba(226, 232, 240, 0.72);
  line-height: 1.45;
}

input,
select,
textarea {
  width: 100%;
  min-height: 54px;
  padding: 12px 14px;
  color: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.82);
  line-height: 1.45;
}

textarea {
  min-height: 120px;
  line-height: 1.6;
  resize: vertical;
}

.submit {
  margin-top: 16px;
}

.agent-card,
.deleted,
.row,
.preview {
  padding: var(--compact-panel-padding);
  color: inherit;
  text-align: left;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: var(--sub-panel-radius);
  background: rgba(15, 23, 42, 0.62);
  min-width: 0;
}

.agent-card {
  display: grid;
  gap: 10px;
  width: 100%;
  cursor: pointer;
  overflow: visible;
}

.agent-card.active {
  border-color: rgba(96, 165, 250, 0.8);
}

.chip,
.mini {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  line-height: 1.4;
  background: rgba(148, 163, 184, 0.16);
}

.chip[data-status='PUBLISHED'],
.chip[data-status='SUCCESS'] {
  color: #86efac;
  background: rgba(34, 197, 94, 0.16);
}

.chip[data-status='FAILED'],
.chip[data-status='OFFLINE'] {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.16);
}

.mini {
  color: #dbeafe;
  border: 0;
  flex: 0 0 auto;
}

.mini.danger {
  color: #fecaca;
}

.row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.row--log {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-head strong,
.panel-head h3,
.agent-card strong,
.deleted span,
.preview strong {
  line-height: 1.35;
}

.panel-head > div:first-child,
.agent-card div:first-child,
.deleted span {
  min-width: 0;
  flex: 1 1 320px;
}

.actions > * {
  min-width: 0;
}

.panel-head .actions {
  width: 100%;
  justify-content: flex-end;
}

.panel-head,
.actions,
.row,
.row--log {
  min-width: 0;
}

.panel-head {
  min-height: 60px;
}

.panel-head .actions input,
.panel-head .actions select,
.panel-head .actions .app-button {
  flex: 1 1 180px;
}

.agent-card .actions {
  justify-content: flex-start;
}

.agent-card .actions .mini {
  min-width: 92px;
}

.clickable {
  cursor: pointer;
}

@media (max-width: 1320px) {
  .stats,
  .grid--main,
  .grid--three,
  .grid--tasks {
    grid-template-columns: 1fr;
  }

}

@media (max-width: 760px) {
  .hero,
  .panel-head,
  .actions {
    align-items: stretch;
    flex-direction: column;
  }

  .form-grid,
  .row,
  .row--log {
    grid-template-columns: 1fr;
  }
}
</style>
