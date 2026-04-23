<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  createHook,
  createHookBinding,
  createHookTestCase,
  debugHook,
  fetchHookCatalog,
  fetchHookDetail,
  fetchHookStats,
  hotUpdateHook,
  offlineHook,
  publishHook,
  queryDeletedHooks,
  queryHookBindings,
  queryHookLogs,
  queryHooks,
  queryHookTestCases,
  removeHook,
  runHookTestCase,
  updateHook,
} from '@/api/hook'
import type {
  HookBindingItem,
  HookCatalogItem,
  HookDebugResult,
  HookExecutionLogItem,
  HookItem,
  HookPayload,
  HookStatistics,
  HookTestCaseItem,
} from '@/types/hook'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

const loading = ref(false)
const saving = ref(false)
const actionLoading = ref(false)
const selectedHookId = ref<number | null>(null)
const hooks = ref<HookItem[]>([])
const deletedHooks = ref<HookItem[]>([])
const catalog = ref<HookCatalogItem[]>([])
const bindings = ref<HookBindingItem[]>([])
const testCases = ref<HookTestCaseItem[]>([])
const logs = ref<HookExecutionLogItem[]>([])
const debugResult = ref<HookDebugResult | null>(null)
const feedback = ref<{ tone: FeedbackTone; message: string } | null>(null)

const stats = ref<HookStatistics>({
  totalCount: 0,
  enabledCount: 0,
  publishedCount: 0,
  hotUpdateEnabledCount: 0,
  deletedCount: 0,
  highRiskCount: 0,
  totalBindingCount: 0,
  totalTestCaseCount: 0,
  totalLogCount: 0,
  successLogCount: 0,
  failureLogCount: 0,
})

const filters = reactive({
  keyword: '',
  stage: 'ALL',
  status: 'ALL',
})

const form = reactive({
  hookCode: '',
  hookName: '',
  description: '',
  hookType: 'AGENT',
  hookStage: 'PRE_MODEL',
  hookStatus: 'ENABLED',
  riskLevel: 'LOW',
  triggerMode: 'SYNC',
  failStrategy: 'CONTINUE',
  sortWeight: 100,
  timeoutMs: 10000,
  hotUpdateEnabled: 0,
  versionCode: '',
  versionDescription: '',
  builtinHookKey: '',
  scriptLanguage: 'JAVA',
  tagsText: 'guardrail,hook',
  targetChannelsText: 'WEB',
  targetEnvironmentsText: 'PROD',
  targetAgentCodesText: '',
  targetModelCodesText: '',
  conditionConfigText: '{\n  "matchMode": "ALL"\n}',
  runtimeConfigText: '{\n  "retryTimes": 1\n}',
  securityConfigText: '{\n  "approvalRequired": false\n}',
  observabilityConfigText: '{\n  "logEnabled": true,\n  "traceEnabled": true\n}',
  degradationConfigText: '{\n  "fallbackMode": "BYPASS"\n}',
  scriptContent: '',
  testPayloadJson: '{\n  "input": "hello"\n}',
  remark: '',
})

const debugForm = reactive({
  requestPayloadJson: '{\n  "input": "hello"\n}',
  contextPayloadJson: '{\n  "traceId": "demo"\n}',
  agentCode: 'agent-demo',
  sessionCode: 'session-demo',
  sourceType: 'DEBUG',
})

const bindingForm = reactive({
  bindingName: '',
  bindingScope: 'AGENT',
  targetAgentCode: '',
  targetModelCode: '',
  environmentCode: 'PROD',
  priorityNo: 100,
  enabled: 1,
  remark: '',
})

const testCaseForm = reactive({
  caseName: '',
  inputPayloadJson: '{\n  "input": "test"\n}',
  contextPayloadJson: '{\n  "channel": "WEB"\n}',
  expectedSuccess: 1,
  expectedResponseContains: '',
  enabled: 1,
})

const filteredHooks = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return hooks.value.filter((item) => {
    const matchKeyword = !keyword || [item.hookCode, item.hookName, item.description].filter(Boolean).some((value) => String(value).toLowerCase().includes(keyword))
    const matchStage = filters.stage === 'ALL' || item.hookStage === filters.stage
    const matchStatus = filters.status === 'ALL' || item.hookStatus === filters.status
    return matchKeyword && matchStage && matchStatus
  })
})

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function parseJson<T>(value: string, label: string): T {
  try {
    return JSON.parse(value) as T
  } catch {
    throw new Error(`${label} 不是合法 JSON`)
  }
}

function formatTime(value?: number | null) {
  if (!value) return '未记录'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value)
}

function resetForm() {
  selectedHookId.value = null
  form.hookCode = ''
  form.hookName = ''
  form.description = ''
  form.hookType = 'AGENT'
  form.hookStage = 'PRE_MODEL'
  form.hookStatus = 'ENABLED'
  form.riskLevel = 'LOW'
  form.triggerMode = 'SYNC'
  form.failStrategy = 'CONTINUE'
  form.sortWeight = 100
  form.timeoutMs = 10000
  form.hotUpdateEnabled = 0
  form.versionCode = ''
  form.versionDescription = ''
  form.builtinHookKey = ''
  form.scriptLanguage = 'JAVA'
  form.tagsText = 'guardrail,hook'
  form.targetChannelsText = 'WEB'
  form.targetEnvironmentsText = 'PROD'
  form.targetAgentCodesText = ''
  form.targetModelCodesText = ''
  form.conditionConfigText = '{\n  "matchMode": "ALL"\n}'
  form.runtimeConfigText = '{\n  "retryTimes": 1\n}'
  form.securityConfigText = '{\n  "approvalRequired": false\n}'
  form.observabilityConfigText = '{\n  "logEnabled": true,\n  "traceEnabled": true\n}'
  form.degradationConfigText = '{\n  "fallbackMode": "BYPASS"\n}'
  form.scriptContent = ''
  form.testPayloadJson = '{\n  "input": "hello"\n}'
  form.remark = ''
  bindings.value = []
  testCases.value = []
  logs.value = []
  debugResult.value = null
}

function fillForm(item: HookItem) {
  selectedHookId.value = item.id
  form.hookCode = item.hookCode
  form.hookName = item.hookName
  form.description = item.description ?? ''
  form.hookType = item.hookType
  form.hookStage = item.hookStage
  form.hookStatus = item.hookStatus
  form.riskLevel = item.riskLevel
  form.triggerMode = item.triggerMode
  form.failStrategy = item.failStrategy
  form.sortWeight = item.sortWeight ?? 100
  form.timeoutMs = item.timeoutMs ?? 10000
  form.hotUpdateEnabled = item.hotUpdateEnabled ?? 0
  form.versionCode = item.versionCode ?? ''
  form.versionDescription = item.versionDescription ?? ''
  form.builtinHookKey = item.builtinHookKey ?? ''
  form.scriptLanguage = item.scriptLanguage ?? 'JAVA'
  form.tagsText = (item.tags ?? []).join(',')
  form.targetChannelsText = (item.targetChannels ?? []).join(',')
  form.targetEnvironmentsText = (item.targetEnvironments ?? []).join(',')
  form.targetAgentCodesText = (item.targetAgentCodes ?? []).join(',')
  form.targetModelCodesText = (item.targetModelCodes ?? []).join(',')
  form.conditionConfigText = JSON.stringify(item.conditionConfig ?? {}, null, 2)
  form.runtimeConfigText = JSON.stringify(item.runtimeConfig ?? {}, null, 2)
  form.securityConfigText = JSON.stringify(item.securityConfig ?? {}, null, 2)
  form.observabilityConfigText = JSON.stringify(item.observabilityConfig ?? {}, null, 2)
  form.degradationConfigText = JSON.stringify(item.degradationConfig ?? {}, null, 2)
  form.scriptContent = item.scriptContent ?? ''
  form.testPayloadJson = item.testPayloadJson ?? '{\n  "input": "hello"\n}'
  form.remark = item.remark ?? ''
  debugForm.requestPayloadJson = item.testPayloadJson ?? '{\n  "input": "hello"\n}'
}

function buildPayload(): HookPayload {
  return {
    hookCode: form.hookCode.trim(),
    hookName: form.hookName.trim(),
    description: form.description.trim() || null,
    hookType: form.hookType,
    hookStage: form.hookStage,
    hookStatus: form.hookStatus,
    riskLevel: form.riskLevel,
    triggerMode: form.triggerMode,
    failStrategy: form.failStrategy,
    sortWeight: form.sortWeight,
    timeoutMs: form.timeoutMs,
    hotUpdateEnabled: form.hotUpdateEnabled,
    versionCode: form.versionCode.trim() || null,
    versionDescription: form.versionDescription.trim() || null,
    builtinHookKey: form.builtinHookKey.trim() || null,
    scriptLanguage: form.scriptLanguage,
    tags: form.tagsText.split(',').map((item) => item.trim()).filter(Boolean),
    targetChannels: form.targetChannelsText.split(',').map((item) => item.trim()).filter(Boolean),
    targetEnvironments: form.targetEnvironmentsText.split(',').map((item) => item.trim()).filter(Boolean),
    targetAgentCodes: form.targetAgentCodesText.split(',').map((item) => item.trim()).filter(Boolean),
    targetModelCodes: form.targetModelCodesText.split(',').map((item) => item.trim()).filter(Boolean),
    conditionConfig: parseJson(form.conditionConfigText, '条件配置'),
    runtimeConfig: parseJson(form.runtimeConfigText, '运行配置'),
    securityConfig: parseJson(form.securityConfigText, '安全配置'),
    observabilityConfig: parseJson(form.observabilityConfigText, '观测配置'),
    degradationConfig: parseJson(form.degradationConfigText, '降级配置'),
    scriptContent: form.scriptContent.trim() || null,
    testPayloadJson: form.testPayloadJson.trim() || null,
    remark: form.remark.trim() || null,
  }
}

async function selectHook(hookId: number) {
  const [detail, bindingList, testCaseList, logList] = await Promise.all([
    fetchHookDetail(hookId),
    queryHookBindings(hookId),
    queryHookTestCases(hookId),
    queryHookLogs({ hookId }),
  ])
  fillForm(detail)
  bindings.value = bindingList
  testCases.value = testCaseList
  logs.value = logList
}

async function refreshAll(keepSelection = true) {
  loading.value = true
  try {
    const currentId = keepSelection ? selectedHookId.value : null
    const [hookList, deletedList, statResult, catalogResult] = await Promise.all([
      queryHooks(),
      queryDeletedHooks(),
      fetchHookStats(),
      fetchHookCatalog(),
    ])
    hooks.value = hookList
    deletedHooks.value = deletedList
    stats.value = statResult
    catalog.value = catalogResult
    if (currentId) {
      const exists = hookList.find((item) => item.id === currentId)
      if (exists) await selectHook(currentId)
      else resetForm()
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '钩子数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const payload = buildPayload()
    if (selectedHookId.value) {
      await updateHook(selectedHookId.value, payload)
      showFeedback('success', '钩子已更新')
    } else {
      const created = await createHook(payload)
      showFeedback('success', '钩子已创建')
      await refreshAll(false)
      await selectHook(created.id)
      return
    }
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '钩子保存失败'))
  } finally {
    saving.value = false
  }
}

async function handleDebug() {
  if (!selectedHookId.value) return
  actionLoading.value = true
  try {
    debugResult.value = await debugHook({
      hookId: selectedHookId.value,
      requestPayloadJson: debugForm.requestPayloadJson.trim(),
      contextPayload: parseJson(debugForm.contextPayloadJson, '调试上下文'),
      agentCode: debugForm.agentCode.trim() || undefined,
      sessionCode: debugForm.sessionCode.trim() || undefined,
      sourceType: debugForm.sourceType,
    })
    logs.value = await queryHookLogs({ hookId: selectedHookId.value })
    showFeedback('success', '调试执行完成')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '调试执行失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handlePublish() {
  if (!selectedHookId.value) return
  await publishHook(selectedHookId.value)
  await refreshAll()
}

async function handleOffline() {
  if (!selectedHookId.value) return
  await offlineHook(selectedHookId.value)
  await refreshAll()
}

async function handleHotUpdate() {
  if (!selectedHookId.value) return
  await hotUpdateHook(selectedHookId.value)
  await refreshAll()
}

async function handleDelete() {
  if (!selectedHookId.value) return
  if (!window.confirm('确认删除当前钩子吗？')) return
  await removeHook(selectedHookId.value)
  resetForm()
  await refreshAll(false)
}

async function handleCreateBinding() {
  if (!selectedHookId.value) return
  await createHookBinding(selectedHookId.value, { ...bindingForm })
  bindings.value = await queryHookBindings(selectedHookId.value)
  showFeedback('success', '绑定已新增')
}

async function handleCreateTestCase() {
  if (!selectedHookId.value) return
  await createHookTestCase(selectedHookId.value, {
    caseName: testCaseForm.caseName.trim(),
    inputPayload: parseJson(testCaseForm.inputPayloadJson, '测试输入'),
    contextPayload: parseJson(testCaseForm.contextPayloadJson, '测试上下文'),
    expectedSuccess: testCaseForm.expectedSuccess,
    expectedResponseContains: testCaseForm.expectedResponseContains.trim() || null,
    enabled: testCaseForm.enabled,
  })
  testCases.value = await queryHookTestCases(selectedHookId.value)
  showFeedback('success', '测试用例已新增')
}

async function handleRunTestCase(testCaseId: number) {
  debugResult.value = await runHookTestCase(testCaseId)
  if (selectedHookId.value) {
    testCases.value = await queryHookTestCases(selectedHookId.value)
    logs.value = await queryHookLogs({ hookId: selectedHookId.value })
  }
  showFeedback('success', '测试用例已执行')
}

function applyCatalog(item: HookCatalogItem) {
  if (selectedHookId.value) return
  form.hookCode = item.hookKey
  form.hookName = item.hookName
  form.description = item.description ?? ''
  form.hookType = item.hookType
  form.hookStage = item.hookStage
  form.riskLevel = item.riskLevel
  form.failStrategy = item.failStrategy
  form.tagsText = (item.tags ?? []).join(',')
  form.runtimeConfigText = item.defaultConfigJson ?? '{}'
  form.testPayloadJson = item.defaultTestPayloadJson ?? '{}'
  debugForm.requestPayloadJson = item.defaultTestPayloadJson ?? '{}'
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
    <section class="hook-page">
      <article class="panel-card hero-panel">
        <div class="hero-panel__head">
          <div>
            <p class="section-kicker">钩子控制台</p>
            <h2>钩子管理台</h2>
            <p class="hero-panel__summary">管理智能体钩子编排、风险治理、绑定策略、测试回归和调试日志。</p>
          </div>
          <div class="toolbar-actions">
            <button class="app-button app-button--secondary" type="button" @click="resetForm">新建钩子</button>
            <button class="app-button" type="button" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存钩子' }}</button>
          </div>
        </div>
        <div class="stats-strip">
          <div class="metric-card"><span>钩子总数</span><strong>{{ stats.totalCount }}</strong></div>
          <div class="metric-card"><span>已启用</span><strong>{{ stats.enabledCount }}</strong></div>
          <div class="metric-card"><span>已发布</span><strong>{{ stats.publishedCount }}</strong></div>
          <div class="metric-card"><span>高风险</span><strong>{{ stats.highRiskCount }}</strong></div>
          <div class="metric-card"><span>绑定总数</span><strong>{{ stats.totalBindingCount }}</strong></div>
        </div>
      </article>

      <div class="hook-layout">
        <article class="panel-card section-panel sidebar-panel">
          <div class="filter-grid">
            <input v-model="filters.keyword" class="app-input" type="text" placeholder="搜索编码、名称、描述" />
            <select v-model="filters.stage" class="app-select"><option value="ALL">全部阶段</option><option value="PRE_MODEL">PRE_MODEL</option><option value="POST_MODEL">POST_MODEL</option><option value="PRE_TOOL_CALL">PRE_TOOL_CALL</option></select>
            <select v-model="filters.status" class="app-select"><option value="ALL">全部状态</option><option value="ENABLED">ENABLED</option><option value="DISABLED">DISABLED</option></select>
          </div>
          <div class="catalog-strip">
            <button v-for="item in catalog" :key="item.hookKey" class="catalog-chip" type="button" @click="applyCatalog(item)">{{ item.hookName }}</button>
          </div>
          <div v-if="loading" class="empty-state">加载中...</div>
          <div v-else class="list-stack">
            <button v-for="item in filteredHooks" :key="item.id" class="list-item" :class="{ 'list-item--active': selectedHookId === item.id }" type="button" @click="selectHook(item.id)">
              <strong>{{ item.hookName }}</strong>
              <span>{{ item.hookCode }}</span>
              <small>{{ item.hookStage }} / {{ item.riskLevel }} / {{ item.publishStatus }}</small>
            </button>
          </div>
          <div class="deleted-box">
            <h4>回收站</h4>
            <p>{{ deletedHooks.length }} 个已删除钩子</p>
          </div>
        </article>

        <article class="panel-card section-panel">
          <div class="section-panel__head">
            <div>
              <h3>{{ selectedHookId ? '钩子配置' : '新建钩子' }}</h3>
              <p>{{ selectedHookId ? `当前钩子：${form.hookName} / ${form.hookCode}` : '未选择钩子' }}</p>
            </div>
            <div class="toolbar-actions">
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedHookId" @click="handlePublish">发布</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedHookId" @click="handleOffline">下线</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedHookId" @click="handleHotUpdate">热更新</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedHookId" @click="handleDelete">删除</button>
            </div>
          </div>

          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">钩子编码</span><input v-model="form.hookCode" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">钩子名称</span><input v-model="form.hookName" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">钩子类型</span><input v-model="form.hookType" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">执行阶段</span><input v-model="form.hookStage" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">状态</span><select v-model="form.hookStatus" class="app-select"><option>ENABLED</option><option>DISABLED</option></select></label>
            <label class="field"><span class="field__label">风险等级</span><select v-model="form.riskLevel" class="app-select"><option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option></select></label>
            <label class="field"><span class="field__label">触发模式</span><select v-model="form.triggerMode" class="app-select"><option>SYNC</option><option>ASYNC</option><option>APPROVAL</option></select></label>
            <label class="field"><span class="field__label">失败策略</span><select v-model="form.failStrategy" class="app-select"><option>CONTINUE</option><option>BLOCK</option><option>FALLBACK</option></select></label>
            <label class="field"><span class="field__label">超时毫秒</span><input v-model.number="form.timeoutMs" class="app-input" type="number" /></label>
            <label class="field"><span class="field__label">排序权重</span><input v-model.number="form.sortWeight" class="app-input" type="number" /></label>
          </div>
          <label class="field"><span class="field__label">描述</span><textarea v-model="form.description" class="app-textarea" rows="3" /></label>
          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">标签</span><input v-model="form.tagsText" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">智能体目标</span><input v-model="form.targetAgentCodesText" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">模型目标</span><input v-model="form.targetModelCodesText" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">环境目标</span><input v-model="form.targetEnvironmentsText" class="app-input" type="text" /></label>
          </div>
          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">条件配置 JSON</span><textarea v-model="form.conditionConfigText" class="app-textarea code-area" rows="8" /></label>
            <label class="field"><span class="field__label">运行配置 JSON</span><textarea v-model="form.runtimeConfigText" class="app-textarea code-area" rows="8" /></label>
            <label class="field"><span class="field__label">安全配置 JSON</span><textarea v-model="form.securityConfigText" class="app-textarea code-area" rows="8" /></label>
            <label class="field"><span class="field__label">观测配置 JSON</span><textarea v-model="form.observabilityConfigText" class="app-textarea code-area" rows="8" /></label>
          </div>
          <label class="field"><span class="field__label">降级配置 JSON</span><textarea v-model="form.degradationConfigText" class="app-textarea code-area" rows="6" /></label>
          <label class="field"><span class="field__label">测试载荷 JSON</span><textarea v-model="form.testPayloadJson" class="app-textarea code-area" rows="6" /></label>

          <div class="tab-grid">
            <section class="stack-card">
              <div class="stack-card__head"><h4>调试</h4><button class="app-button app-button--secondary" type="button" :disabled="actionLoading || !selectedHookId" @click="handleDebug">执行调试</button></div>
              <label class="field"><span class="field__label">请求 JSON</span><textarea v-model="debugForm.requestPayloadJson" class="app-textarea code-area" rows="6" /></label>
              <label class="field"><span class="field__label">上下文 JSON</span><textarea v-model="debugForm.contextPayloadJson" class="app-textarea code-area" rows="4" /></label>
              <pre v-if="debugResult" class="result-box">{{ JSON.stringify(debugResult, null, 2) }}</pre>
            </section>

            <section class="stack-card">
              <div class="stack-card__head"><h4>绑定</h4><button class="app-button app-button--secondary" type="button" :disabled="!selectedHookId" @click="handleCreateBinding">新增绑定</button></div>
              <div class="form-grid">
                <input v-model="bindingForm.bindingName" class="app-input" type="text" placeholder="绑定名称" />
                <input v-model="bindingForm.bindingScope" class="app-input" type="text" placeholder="绑定范围" />
                <input v-model="bindingForm.targetAgentCode" class="app-input" type="text" placeholder="智能体编码" />
              </div>
              <div v-if="bindings.length === 0" class="empty-state empty-state--compact">暂无绑定</div>
              <div v-else class="compact-list"><div v-for="item in bindings" :key="item.id" class="compact-item"><strong>{{ item.bindingName }}</strong><span>{{ item.bindingScope }} / {{ item.targetAgentCode || item.targetModelCode || item.environmentCode }}</span></div></div>
            </section>

            <section class="stack-card">
              <div class="stack-card__head"><h4>测试用例</h4><button class="app-button app-button--secondary" type="button" :disabled="!selectedHookId" @click="handleCreateTestCase">新增用例</button></div>
              <input v-model="testCaseForm.caseName" class="app-input" type="text" placeholder="用例名称" />
              <textarea v-model="testCaseForm.inputPayloadJson" class="app-textarea code-area" rows="4" />
              <textarea v-model="testCaseForm.contextPayloadJson" class="app-textarea code-area" rows="3" />
              <div v-if="testCases.length === 0" class="empty-state empty-state--compact">暂无测试用例</div>
              <div v-else class="compact-list"><div v-for="item in testCases" :key="item.id" class="compact-item compact-item--action"><div><strong>{{ item.caseName }}</strong><span>{{ item.lastRunStatus || '未执行' }} / {{ formatTime(item.lastRunAt) }}</span></div><button class="app-button app-button--secondary" type="button" @click="handleRunTestCase(item.id)">运行</button></div></div>
            </section>

            <section class="stack-card">
              <div class="stack-card__head"><h4>日志</h4><span>{{ logs.length }} 条</span></div>
              <div v-if="logs.length === 0" class="empty-state empty-state--compact">暂无日志</div>
              <div v-else class="compact-list"><div v-for="item in logs" :key="item.id" class="compact-item"><strong>{{ item.sourceType }} / {{ item.executeStatus }}</strong><span>{{ item.hookCode }} / {{ formatTime(item.createTime) }}</span></div></div>
            </section>
          </div>
        </article>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.hook-page { display: grid; gap: var(--layout-gap); min-height: 100%; min-width: 0; }
.hero-panel, .section-panel, .metric-card, .catalog-chip, .list-item, .stack-card, .result-box, .empty-state { border: 1px solid rgba(255,255,255,.08); }
.hero-panel, .section-panel { padding: var(--panel-padding); border-radius: var(--panel-radius); background: linear-gradient(180deg, rgba(255,255,255,.034), rgba(255,255,255,.012)), rgba(7,14,26,.82); box-shadow: 0 24px 56px rgba(0,0,0,.22); }
.hero-panel__head, .section-panel__head, .toolbar-actions, .stack-card__head { display: flex; justify-content: space-between; gap: 14px; align-items: flex-start; flex-wrap: wrap; }
.hero-panel__head h2, .section-panel__head h3, .stack-card__head h4 { margin: 0; color: var(--color-ink-strong); }
.hero-panel__summary, .section-panel__head p, .deleted-box p, .compact-item span { color: var(--color-ink-soft); line-height: 1.6; }
.stats-strip { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-top: 20px; }
.metric-card { padding: 16px 18px; border-radius: 18px; background: rgba(255,255,255,.03); }
.metric-card span { color: var(--color-ink-soft); }
.metric-card strong { display: block; margin-top: 10px; color: var(--color-ink-strong); font-size: 1.5rem; }
.hook-layout { display: grid; grid-template-columns: minmax(0, var(--layout-side-column)) minmax(0, 1fr); gap: var(--layout-gap); align-items: start; min-width: 0; }
.sidebar-panel, .list-stack, .tab-grid, .form-grid, .filter-grid { display: grid; gap: 14px; }
.sidebar-panel { align-content: start; }
.list-stack { overflow: auto; padding-right: 4px; scrollbar-gutter: stable; min-height: 0; }
.form-grid--double { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.catalog-strip { display: flex; flex-wrap: wrap; gap: 10px; }
.catalog-chip, .list-item { border-radius: 16px; background: rgba(255,255,255,.03); color: var(--color-ink-soft); }
.catalog-chip { padding: 10px 14px; cursor: pointer; }
.list-item { display: grid; gap: 4px; padding: 14px 16px; text-align: left; }
.list-item--active { background: rgba(76,162,255,.08); border-color: rgba(108,201,255,.22); }
.deleted-box { padding: 16px; border-radius: 16px; background: rgba(255,255,255,.018); }
.field { display: grid; gap: 8px; }
.field__label { color: var(--color-ink-strong); font-size: .92rem; }
.app-input, .app-select, .app-textarea { width: 100%; border: 1px solid rgba(147,177,233,.28); border-radius: 14px; background: rgba(8,16,30,.96); }
.app-input, .app-select { min-height: 48px; padding: 0 14px; }
.app-textarea { min-height: 120px; padding: 14px; }
.code-area, .result-box { font-family: var(--font-mono); font-size: .84rem; }
.tab-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 18px; }
.stack-card { display: grid; gap: 14px; padding: var(--compact-panel-padding); border-radius: 18px; background: rgba(255,255,255,.018); min-width: 0; align-content: start; }
.compact-list { display: grid; gap: 10px; overflow: auto; padding-right: 4px; scrollbar-gutter: stable; min-height: 0; }
.compact-item { display: grid; gap: 4px; padding: 12px 14px; border-radius: 14px; background: rgba(255,255,255,.024); }
.compact-item--action { grid-template-columns: 1fr auto; align-items: center; }
.result-box { margin: 0; padding: 14px; border-radius: 14px; background: rgba(8,16,30,.96); white-space: pre-wrap; word-break: break-word; }
.empty-state { display: grid; place-items: center; min-height: 100px; padding: 18px; border-radius: 16px; background: rgba(255,255,255,.014); color: var(--color-ink-soft); text-align: center; }
.empty-state--compact { min-height: 72px; }
@media (max-width: 1320px) { .hook-layout, .tab-grid, .form-grid--double, .stats-strip { grid-template-columns: 1fr; } }
</style>
