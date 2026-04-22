<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Play, Plus, RefreshCw } from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  createInterceptor,
  createInterceptorBinding,
  createInterceptorTestCase,
  debugInterceptor,
  fetchInterceptorCatalog,
  fetchInterceptorDetail,
  fetchInterceptorStats,
  hotUpdateInterceptor,
  offlineInterceptor,
  publishInterceptor,
  queryDeletedInterceptors,
  queryInterceptorBindings,
  queryInterceptorLogs,
  queryInterceptorTestCases,
  queryInterceptors,
  removeInterceptor,
  restoreInterceptor,
  runInterceptorTestCase,
  updateInterceptor,
} from '@/api/interceptor'
import type {
  InterceptorBindingItem,
  InterceptorCatalogItem,
  InterceptorDebugResult,
  InterceptorExecutionLogItem,
  InterceptorItem,
  InterceptorPayload,
  InterceptorStatistics,
  InterceptorTestCaseItem,
} from '@/types/interceptor'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

const feedback = ref<{ tone: FeedbackTone; message: string } | null>(null)
const loading = ref(false)
const saving = ref(false)
const acting = ref(false)
const selectedId = ref<number | null>(null)
const list = ref<InterceptorItem[]>([])
const deleted = ref<InterceptorItem[]>([])
const catalog = ref<InterceptorCatalogItem[]>([])
const bindings = ref<InterceptorBindingItem[]>([])
const testCases = ref<InterceptorTestCaseItem[]>([])
const logs = ref<InterceptorExecutionLogItem[]>([])
const debugResult = ref<InterceptorDebugResult | null>(null)
const stats = ref<InterceptorStatistics>({
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

const filters = reactive({ keyword: '' })

const form = reactive({
  interceptorCode: '',
  interceptorName: '',
  description: '',
  interceptorType: 'TOOL',
  interceptorStage: 'PRE_TOOL',
  interceptorStatus: 'ENABLED',
  riskLevel: 'MEDIUM',
  triggerMode: 'SYNC',
  failStrategy: 'CONTINUE',
  sortWeight: 100,
  timeoutMs: 5000,
  hotUpdateEnabled: 0,
  versionCode: '',
  versionDescription: '',
  builtinInterceptorKey: 'TOOL_RETRY',
  scriptLanguage: 'JAVA',
  tagsText: 'interceptor,governance',
  interceptorConfigText: '{\n  "maxRetries": 2\n}',
  testPayloadJson: '{\n  "toolName": "web_search",\n  "toolStatus": "FAILED"\n}',
})

const debugForm = reactive({
  requestPayloadJson: '{\n  "toolName": "web_search",\n  "toolStatus": "FAILED"\n}',
  contextPayloadJson: '{\n  "toolCandidates": ["web_search", "crm_search"]\n}',
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
  inputPayloadJson: '{\n  "toolName": "web_search",\n  "toolStatus": "FAILED"\n}',
  contextPayloadJson: '{\n  "toolCandidates": ["web_search"]\n}',
  expectedSuccess: 1,
  expectedResponseContains: 'RETRY',
  enabled: 1,
})

const filteredList = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return list.value.filter((item) => !keyword || [item.interceptorCode, item.interceptorName, item.description]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword)))
})

const selected = computed(() => (
  selectedId.value == null ? null : list.value.find((item) => item.id === selectedId.value) ?? null
))

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

function formatTime(value?: number | null) {
  return value
    ? new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value)
    : '未记录'
}

function resetForm() {
  selectedId.value = null
  form.interceptorCode = ''
  form.interceptorName = ''
  form.description = ''
  form.interceptorType = 'TOOL'
  form.interceptorStage = 'PRE_TOOL'
  form.interceptorStatus = 'ENABLED'
  form.riskLevel = 'MEDIUM'
  form.triggerMode = 'SYNC'
  form.failStrategy = 'CONTINUE'
  form.sortWeight = 100
  form.timeoutMs = 5000
  form.hotUpdateEnabled = 0
  form.versionCode = ''
  form.versionDescription = ''
  form.builtinInterceptorKey = 'TOOL_RETRY'
  form.scriptLanguage = 'JAVA'
  form.tagsText = 'interceptor,governance'
  form.interceptorConfigText = '{\n  "maxRetries": 2\n}'
  form.testPayloadJson = '{\n  "toolName": "web_search",\n  "toolStatus": "FAILED"\n}'
  bindings.value = []
  testCases.value = []
  logs.value = []
  debugResult.value = null
}

function fillForm(item: InterceptorItem) {
  selectedId.value = item.id
  form.interceptorCode = item.interceptorCode
  form.interceptorName = item.interceptorName
  form.description = item.description ?? ''
  form.interceptorType = item.interceptorType
  form.interceptorStage = item.interceptorStage
  form.interceptorStatus = item.interceptorStatus
  form.riskLevel = item.riskLevel
  form.triggerMode = item.triggerMode
  form.failStrategy = item.failStrategy
  form.sortWeight = item.sortWeight ?? 100
  form.timeoutMs = item.timeoutMs ?? 5000
  form.hotUpdateEnabled = item.hotUpdateEnabled ?? 0
  form.versionCode = item.versionCode ?? ''
  form.versionDescription = item.versionDescription ?? ''
  form.builtinInterceptorKey = item.builtinInterceptorKey ?? 'TOOL_RETRY'
  form.scriptLanguage = item.scriptLanguage ?? 'JAVA'
  form.tagsText = (item.tags ?? []).join(',')
  form.interceptorConfigText = JSON.stringify(item.interceptorConfig ?? {}, null, 2)
  form.testPayloadJson = item.testPayloadJson ?? '{}'
  debugForm.requestPayloadJson = form.testPayloadJson
}

function buildPayload(): InterceptorPayload {
  return {
    interceptorCode: form.interceptorCode.trim(),
    interceptorName: form.interceptorName.trim(),
    description: form.description.trim() || null,
    interceptorType: form.interceptorType,
    interceptorStage: form.interceptorStage,
    interceptorStatus: form.interceptorStatus,
    riskLevel: form.riskLevel,
    triggerMode: form.triggerMode,
    failStrategy: form.failStrategy,
    sortWeight: form.sortWeight,
    timeoutMs: form.timeoutMs,
    hotUpdateEnabled: form.hotUpdateEnabled,
    versionCode: form.versionCode.trim() || null,
    versionDescription: form.versionDescription.trim() || null,
    builtinInterceptorKey: form.builtinInterceptorKey.trim() || null,
    scriptLanguage: form.scriptLanguage,
    tags: form.tagsText.split(',').map((item) => item.trim()).filter(Boolean),
    targetChannels: [],
    targetEnvironments: [],
    targetAgentCodes: [],
    targetModelCodes: [],
    conditionConfig: {},
    runtimeConfig: {},
    securityConfig: {},
    observabilityConfig: {},
    degradationConfig: {},
    interceptorConfig: parseJson(form.interceptorConfigText, '拦截器配置'),
    scriptContent: null,
    testPayloadJson: form.testPayloadJson.trim() || null,
    remark: null,
  }
}

async function loadDashboard() {
  loading.value = true
  try {
    const [items, deletedItems, catalogItems, statValue] = await Promise.all([
      queryInterceptors(),
      queryDeletedInterceptors(),
      fetchInterceptorCatalog(),
      fetchInterceptorStats(),
    ])
    list.value = items
    deleted.value = deletedItems
    catalog.value = catalogItems
    stats.value = statValue
  } catch (error) {
    notice('error', getErrorMessage(error, 'Interceptor 数据加载失败。'))
  } finally {
    loading.value = false
  }
}

async function loadDetail(id: number) {
  acting.value = true
  try {
    const [detail, bindingItems, testCaseItems, logItems] = await Promise.all([
      fetchInterceptorDetail(id),
      queryInterceptorBindings(id),
      queryInterceptorTestCases(id),
      queryInterceptorLogs({ interceptorId: id }),
    ])
    const index = list.value.findIndex((item) => item.id === id)
    if (index >= 0) {
      list.value[index] = detail
    }
    fillForm(detail)
    bindings.value = bindingItems
    testCases.value = testCaseItems
    logs.value = logItems
  } catch (error) {
    notice('error', getErrorMessage(error, 'Interceptor 详情加载失败。'))
  } finally {
    acting.value = false
  }
}

async function handleSave() {
  if (!form.interceptorCode.trim() || !form.interceptorName.trim()) {
    notice('error', '请填写编码和名称。')
    return
  }
  saving.value = true
  try {
    const result = selectedId.value == null
      ? await createInterceptor(buildPayload())
      : await updateInterceptor(selectedId.value, buildPayload())
    await loadDashboard()
    await loadDetail(result.id)
    notice('success', '拦截器已保存。')
  } catch (error) {
    notice('error', getErrorMessage(error, '保存失败。'))
  } finally {
    saving.value = false
  }
}

async function doAction(task: () => Promise<unknown>, success: string) {
  acting.value = true
  try {
    await task()
    await loadDashboard()
    if (selectedId.value) {
      await loadDetail(selectedId.value)
    }
    notice('success', success)
  } catch (error) {
    notice('error', getErrorMessage(error, '操作失败。'))
  } finally {
    acting.value = false
  }
}

async function handleDebug() {
  if (!selectedId.value) {
    notice('error', '请先选择拦截器。')
    return
  }
  acting.value = true
  try {
    debugResult.value = await debugInterceptor({
      interceptorId: selectedId.value,
      requestPayloadJson: debugForm.requestPayloadJson,
      contextPayload: parseJson(debugForm.contextPayloadJson, '调试上下文'),
      sourceType: 'DEBUG',
    })
    logs.value = await queryInterceptorLogs({ interceptorId: selectedId.value })
    notice('success', '调试执行完成。')
  } catch (error) {
    notice('error', getErrorMessage(error, '调试失败。'))
  } finally {
    acting.value = false
  }
}

async function handleCreateBinding() {
  if (!selectedId.value) return
  await doAction(async () => {
    await createInterceptorBinding(selectedId.value!, { ...bindingForm })
  }, '绑定已创建。')
}

async function handleCreateTestCase() {
  if (!selectedId.value) return
  acting.value = true
  try {
    await createInterceptorTestCase(selectedId.value, {
      caseName: testCaseForm.caseName,
      inputPayload: parseJson(testCaseForm.inputPayloadJson, '测试输入'),
      contextPayload: parseJson(testCaseForm.contextPayloadJson, '测试上下文'),
      expectedSuccess: testCaseForm.expectedSuccess,
      expectedResponseContains: testCaseForm.expectedResponseContains || null,
      enabled: testCaseForm.enabled,
    })
    testCases.value = await queryInterceptorTestCases(selectedId.value)
    notice('success', '测试用例已创建。')
  } catch (error) {
    notice('error', getErrorMessage(error, '创建测试用例失败。'))
  } finally {
    acting.value = false
  }
}

async function handleRunTestCase(testCaseId: number) {
  acting.value = true
  try {
    debugResult.value = await runInterceptorTestCase(testCaseId)
    if (selectedId.value) {
      testCases.value = await queryInterceptorTestCases(selectedId.value)
      logs.value = await queryInterceptorLogs({ interceptorId: selectedId.value })
    }
    notice('success', '测试用例执行完成。')
  } catch (error) {
    notice('error', getErrorMessage(error, '执行测试失败。'))
  } finally {
    acting.value = false
  }
}

function applyCatalog(item: InterceptorCatalogItem) {
  form.interceptorName = item.interceptorName
  form.description = item.description ?? ''
  form.interceptorType = item.interceptorType
  form.interceptorStage = item.interceptorStage
  form.riskLevel = item.riskLevel
  form.failStrategy = item.failStrategy
  form.builtinInterceptorKey = item.interceptorKey
  form.tagsText = (item.tags ?? []).join(',')
  form.interceptorConfigText = item.defaultConfigJson ?? '{}'
  form.testPayloadJson = item.defaultTestPayloadJson ?? '{}'
  debugForm.requestPayloadJson = form.testPayloadJson
}

async function handleRestore(id: number) {
  await doAction(async () => {
    await restoreInterceptor(id)
  }, '拦截器已恢复。')
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <MainShell>
    <AppFeedbackDialog
      :model-value="Boolean(feedback)"
      :tone="feedback?.tone || 'info'"
      :message="feedback?.message || ''"
      @update:model-value="!$event && (feedback = null)"
    />

    <section class="page">
      <header class="panel-card hero">
        <div>
          <p class="section-kicker">拦截器治理</p>
          <h2>拦截器工作台</h2>
          <p class="muted">围绕 Agent 的工具调用、上下文治理和调试模拟做统一配置。</p>
        </div>
        <button class="app-button app-button--secondary" :disabled="loading" @click="loadDashboard">
          <RefreshCw :size="16" />刷新
        </button>
      </header>

      <div class="stats">
        <article class="panel-card stat"><strong>{{ stats.totalCount }}</strong><span>总数</span></article>
        <article class="panel-card stat"><strong>{{ stats.publishedCount }}</strong><span>已发布</span></article>
        <article class="panel-card stat"><strong>{{ stats.totalBindingCount }}</strong><span>绑定</span></article>
        <article class="panel-card stat"><strong>{{ stats.successLogCount }}</strong><span>成功日志</span></article>
      </div>

      <div class="grid">
        <article class="panel-card section">
          <div class="head">
            <strong>配置</strong>
            <button class="app-button app-button--ghost" @click="resetForm">新建</button>
          </div>
          <div class="form">
            <input v-model="form.interceptorCode" class="app-input" type="text" placeholder="编码" />
            <input v-model="form.interceptorName" class="app-input" type="text" placeholder="名称" />
            <textarea v-model="form.description" class="app-textarea full" rows="2" placeholder="描述" />
            <select v-model="form.interceptorType" class="app-select">
              <option value="TOOL">TOOL</option>
              <option value="MODEL">MODEL</option>
              <option value="AGENT">AGENT</option>
            </select>
            <select v-model="form.interceptorStage" class="app-select">
              <option value="PRE_MODEL">PRE_MODEL</option>
              <option value="PRE_TOOL">PRE_TOOL</option>
              <option value="POST_TOOL">POST_TOOL</option>
              <option value="POST_MODEL">POST_MODEL</option>
            </select>
            <select v-model="form.riskLevel" class="app-select">
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
            <input v-model="form.builtinInterceptorKey" class="app-input" type="text" placeholder="内置模板键" />
            <input v-model="form.versionCode" class="app-input" type="text" placeholder="版本标识" />
            <input v-model="form.tagsText" class="app-input full" type="text" placeholder="标签" />
            <textarea v-model="form.interceptorConfigText" class="app-textarea full code" rows="7" placeholder="拦截器配置 JSON" />
            <textarea v-model="form.testPayloadJson" class="app-textarea full code" rows="5" placeholder="调试样本 JSON" />
          </div>
          <div class="actions">
            <button class="app-button" :disabled="saving" @click="handleSave">
              <Plus :size="16" />{{ saving ? '保存中...' : '保存' }}
            </button>
            <button class="app-button app-button--secondary" :disabled="!selectedId || acting" @click="doAction(() => publishInterceptor(selectedId!), '拦截器已发布。')">发布</button>
            <button class="app-button app-button--ghost" :disabled="!selectedId || acting" @click="doAction(() => hotUpdateInterceptor(selectedId!), '热更新已开启。')">热更新</button>
            <button class="app-button app-button--ghost" :disabled="!selectedId || acting" @click="doAction(() => offlineInterceptor(selectedId!), '拦截器已下线。')">下线</button>
            <button class="app-button app-button--ghost app-button--danger-ghost" :disabled="!selectedId || acting" @click="doAction(() => removeInterceptor(selectedId!), '拦截器已删除。')">删除</button>
          </div>
        </article>

        <article class="panel-card section">
          <div class="head">
            <strong>列表</strong>
            <input v-model="filters.keyword" class="app-input" type="text" placeholder="搜索编码或名称" />
          </div>
          <div v-if="loading" class="empty">正在加载...</div>
          <div v-else class="stack">
            <button v-for="item in filteredList" :key="item.id" class="list-item" :class="{ active: selectedId === item.id }" @click="loadDetail(item.id)">
              <strong>{{ item.interceptorName }}</strong>
              <small class="muted">{{ item.interceptorCode }} / {{ item.interceptorStage }} / {{ item.publishStatus }}</small>
            </button>
          </div>
        </article>
      </div>

      <div class="grid">
        <article class="panel-card section">
          <div class="head">
            <strong>目录模板</strong>
            <small class="muted">已删除 {{ deleted.length }}</small>
          </div>
          <div class="stack">
            <button v-for="item in catalog" :key="item.interceptorKey" class="list-item" @click="applyCatalog(item)">
              <strong>{{ item.interceptorName }}</strong>
              <small class="muted">{{ item.interceptorKey }} / {{ item.interceptorStage }}</small>
            </button>
            <article v-for="item in deleted" :key="`deleted-${item.id}`" class="mini">
              <div class="head">
                <strong>{{ item.interceptorCode }}</strong>
                <button class="app-button app-button--ghost" :disabled="acting" @click="handleRestore(item.id)">恢复</button>
              </div>
              <small class="muted">{{ item.interceptorName }}</small>
            </article>
          </div>
        </article>

        <article class="panel-card section">
          <div class="head">
            <strong>调试</strong>
            <button class="app-button app-button--secondary" :disabled="acting || !selectedId" @click="handleDebug">执行调试</button>
          </div>
          <textarea v-model="debugForm.requestPayloadJson" class="app-textarea code" rows="5" />
          <textarea v-model="debugForm.contextPayloadJson" class="app-textarea code" rows="4" />
          <pre v-if="debugResult" class="code-block">{{ debugResult.responsePayloadJson }}</pre>
        </article>
      </div>

      <div class="grid">
        <article class="panel-card section">
          <div class="head">
            <strong>绑定与版本</strong>
            <small class="muted">{{ selected?.versions?.length || 0 }} 个版本</small>
          </div>
          <div class="form">
            <input v-model="bindingForm.bindingName" class="app-input" type="text" placeholder="绑定名称" />
            <input v-model="bindingForm.targetAgentCode" class="app-input" type="text" placeholder="目标 Agent 编码" />
            <input v-model="bindingForm.targetModelCode" class="app-input full" type="text" placeholder="目标模型编码" />
          </div>
          <button class="app-button app-button--secondary" :disabled="acting || !selectedId" @click="handleCreateBinding">新增绑定</button>
          <div class="stack">
            <article v-for="item in bindings" :key="item.id" class="mini">
              <strong>{{ item.bindingName }}</strong>
              <small class="muted">{{ item.bindingScope }} / {{ item.targetAgentCode || 'ALL_AGENT' }}</small>
            </article>
            <article v-for="version in selected?.versions || []" :key="version.id" class="mini">
              <strong>v{{ version.versionNo }}</strong>
              <small class="muted">{{ version.versionCode || '未命名' }} / {{ formatTime(version.createTime) }}</small>
            </article>
          </div>
        </article>

        <article class="panel-card section">
          <div class="head">
            <strong>测试与日志</strong>
            <small class="muted">{{ logs.length }} 条日志</small>
          </div>
          <div class="form">
            <input v-model="testCaseForm.caseName" class="app-input full" type="text" placeholder="测试用例名称" />
            <textarea v-model="testCaseForm.inputPayloadJson" class="app-textarea full code" rows="4" />
            <textarea v-model="testCaseForm.contextPayloadJson" class="app-textarea full code" rows="3" />
          </div>
          <button class="app-button app-button--secondary" :disabled="acting || !selectedId" @click="handleCreateTestCase">新增测试用例</button>
          <div class="stack">
            <article v-for="item in testCases" :key="item.id" class="mini">
              <div class="head">
                <strong>{{ item.caseName }}</strong>
                <button class="app-button app-button--ghost" :disabled="acting" @click="handleRunTestCase(item.id)">
                  <Play :size="14" />运行
                </button>
              </div>
              <small class="muted">{{ item.lastRunStatus || '未执行' }} / {{ formatTime(item.lastRunAt) }}</small>
            </article>
            <article v-for="item in logs" :key="`log-${item.id}`" class="mini">
              <strong>{{ item.executeStatus }}</strong>
              <small class="muted">{{ item.sourceType }} / {{ formatTime(item.createTime) }} / {{ item.failureReason || item.interceptorCode }}</small>
            </article>
          </div>
        </article>
      </div>
    </section>
  </MainShell>
</template>

<style scoped>
.page,
.stats,
.grid,
.form,
.stack {
  display: grid;
  gap: 16px;
}

.page {
  min-height: 100%;
  min-width: 0;
}

.hero,
.head,
.actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.grid {
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  align-items: start;
}

.stack {
  overflow: auto;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.full {
  grid-column: 1 / -1;
}

.section,
.stat,
.list-item,
.mini,
.empty {
  display: grid;
  gap: 12px;
  align-content: start;
  min-width: 0;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.04);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.hero,
.head,
.actions {
  align-items: flex-start;
  flex-wrap: wrap;
}

.section {
  min-height: 0;
}

.list-item {
  text-align: left;
  cursor: pointer;
}

.active {
  box-shadow:
    inset 0 0 0 1px rgba(77, 179, 255, 0.32),
    0 16px 30px rgba(77, 179, 255, 0.1);
}

.muted {
  color: var(--color-ink-soft);
}

.code,
.code-block {
  font-family: 'JetBrains Mono', monospace;
}

.code-block {
  margin: 0;
  padding: 14px;
  border-radius: 18px;
  background: rgba(4, 17, 29, 0.58);
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1080px) {
  .grid,
  .stats,
  .form {
    grid-template-columns: 1fr;
  }
}
</style>
