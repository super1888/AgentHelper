<script setup lang="ts">
// 文件用途：用户管理 Skill 前端管理页
// 作者：Codex
// 创建时间：2026-04-17
// 核心功能：提供 Skill 的增删改查、版本查看、发布热更新、批量治理与导入导出能力
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { CheckSquare, Download, Plus, RefreshCw, Sparkles, Trash2, Upload, Wand2 } from 'lucide-vue-next'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  batchDeleteSkills,
  batchUpdateSkillStatus,
  createSkill,
  exportSkill,
  fetchSkillDetail,
  fetchSkillStats,
  hotUpdateSkill,
  importSkill,
  publishSkill,
  querySkills,
  removeSkill,
  updateSkill,
} from '@/api/skill'
import type {
  SkillBatchConfig,
  SkillExecutionConfig,
  SkillIntentConfig,
  SkillItem,
  SkillObservabilityConfig,
  SkillPayload,
  SkillPermissionConfig,
  SkillReleaseConfig,
  SkillRoutingConfig,
  SkillStatistics,
  SkillWorkflowConfig,
} from '@/types/skill'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'
type JsonFieldKey =
  | 'intentConfigsText'
  | 'executionConfigText'
  | 'routingConfigText'
  | 'permissionConfigText'
  | 'observabilityConfigText'
  | 'releaseConfigText'
  | 'batchConfigText'
  | 'workflowConfigText'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

interface JsonSection {
  title: string
  field: JsonFieldKey
  rows: number
}

const loading = ref(false)
const statsLoading = ref(false)
const submitting = ref(false)
const actionPending = ref(false)
const deletePending = ref(false)
const selectedSkillId = ref<number | null>(null)
const selectedSkill = ref<SkillItem | null>(null)
const deleteTarget = ref<SkillItem | null>(null)
const skills = ref<SkillItem[]>([])
const selectedIds = ref<number[]>([])
const importPayload = ref('')
const exportPayload = ref('')
const feedback = ref<FeedbackState | null>(null)
let feedbackTimer: ReturnType<typeof setTimeout> | null = null

const statistics = ref<SkillStatistics>({
  totalCount: 0,
  enabledCount: 0,
  publishedCount: 0,
  hotUpdateEnabledCount: 0,
})

// 各配置区块统一用 JSON 文本编辑，保证结构完整且便于导入导出。
const sections: JsonSection[] = [
  { title: '意图 / 关键词 / 参数配置', field: 'intentConfigsText', rows: 9 },
  { title: 'API / 函数执行配置', field: 'executionConfigText', rows: 9 },
  { title: '路由调度 + 上下文', field: 'routingConfigText', rows: 8 },
  { title: '权限 + 风控', field: 'permissionConfigText', rows: 8 },
  { title: '调试 + 测试 + 日志统计', field: 'observabilityConfigText', rows: 8 },
  { title: '发布上线 + 热更新', field: 'releaseConfigText', rows: 8 },
  { title: '批量管理 + 导入导出', field: 'batchConfigText', rows: 7 },
  { title: '工作流组合 + 多渠道适配', field: 'workflowConfigText', rows: 7 },
]

const form = reactive({
  skillCode: '',
  skillName: '',
  description: '',
  skillCategory: 'USER_MANAGEMENT',
  skillStatus: 'ENABLED',
  versionMode: 'MANUAL',
  hotUpdateEnabled: 0,
  intentConfigsText: '',
  executionConfigText: '',
  routingConfigText: '',
  permissionConfigText: '',
  observabilityConfigText: '',
  releaseConfigText: '',
  batchConfigText: '',
  workflowConfigText: '',
  remark: '',
})

const isEditing = computed(() => selectedSkillId.value !== null)
const hasSelection = computed(() => selectedIds.value.length > 0)
const versions = computed(() => selectedSkill.value?.versions ?? [])
const summary = computed(() =>
  statsLoading.value ? '统计加载中...' : `共 ${statistics.value.totalCount} 个 Skill，已发布 ${statistics.value.publishedCount} 个`,
)

function defaultIntentConfigs(): SkillIntentConfig[] {
  return [
    {
      intentName: 'user_query',
      keywords: ['用户查询', '账号查询', '用户列表'],
      parameterConfigs: [{ parameterName: 'keyword', parameterType: 'string', required: 0, defaultValue: '', description: '查询关键词' }],
    },
    {
      intentName: 'user_update',
      keywords: ['修改用户', '更新账号', '用户编辑'],
      parameterConfigs: [{ parameterName: 'userId', parameterType: 'long', required: 1, defaultValue: null, description: '目标用户 ID' }],
    },
  ]
}

function defaultExecutionConfig(): SkillExecutionConfig {
  return {
    executionType: 'API_AND_FUNCTION',
    apiEndpoint: '/agentHelper/users/page',
    httpMethod: 'POST',
    functionName: 'queryUsers',
    timeoutMs: '5000',
    requestTemplate: JSON.stringify({ username: '{{keyword}}', tenantScoped: true }, null, 2),
    responseMapping: JSON.stringify({ list: '$.data.list', total: '$.data.total' }, null, 2),
  }
}

function defaultRoutingConfig(): SkillRoutingConfig {
  return {
    routePolicy: 'KEYWORD_FIRST',
    routeTags: ['user', 'management', 'account'],
    contextWindowStrategy: 'RECENT_10',
    memoryPolicy: 'KEEP_SESSION',
    fallbackSkillCode: 'USER_ASSIST_FALLBACK',
  }
}

function defaultPermissionConfig(): SkillPermissionConfig {
  return {
    allowedRoles: ['ADMIN', 'OPERATOR'],
    dataScopes: ['TENANT', 'SELF'],
    approvalPolicy: 'AUTO',
    riskLevel: 'MEDIUM',
    riskControlPolicy: '敏感操作二次确认 + 高频限流',
  }
}

function defaultObservabilityConfig(): SkillObservabilityConfig {
  return {
    debugEnabled: 1,
    debugScript: '覆盖增删改查、版本、权限、批量导入导出和热更新回归。',
    testCaseSummary: '验证查询、创建、修改、停用、发布、导入、导出、工作流调度。',
    logEnabled: 1,
    metricsPolicy: '统计调用量、成功率、耗时、失败原因和渠道占比。',
  }
}

function defaultReleaseConfig(): SkillReleaseConfig {
  return {
    hotUpdateEnabled: 1,
    releaseChannel: 'WEB',
    grayPolicy: '按租户与角色灰度发布',
    rollbackPolicy: '按最近稳定版本快速回滚',
  }
}

function defaultBatchConfig(): SkillBatchConfig {
  return {
    batchEnabled: 1,
    importEnabled: 1,
    exportEnabled: 1,
    importTemplate: 'JSON',
    exportTemplate: 'JSON',
  }
}

function defaultWorkflowConfig(): SkillWorkflowConfig {
  return {
    workflowSteps: ['意图识别', '参数装配', '权限校验', '路由执行', '结果包装', '日志统计'],
    channelAdapters: ['WEB', 'WORKFLOW', 'API'],
    orchestrationStrategy: 'SEQUENTIAL',
  }
}

function toJson(value: unknown) {
  return JSON.stringify(value, null, 2)
}

// 重置表单到默认模板，便于新建 Skill 时直接开始编辑。
function resetForm() {
  selectedSkillId.value = null
  selectedSkill.value = null
  exportPayload.value = ''
  form.skillCode = ''
  form.skillName = ''
  form.description = ''
  form.skillCategory = 'USER_MANAGEMENT'
  form.skillStatus = 'ENABLED'
  form.versionMode = 'MANUAL'
  form.hotUpdateEnabled = 0
  form.intentConfigsText = toJson(defaultIntentConfigs())
  form.executionConfigText = toJson(defaultExecutionConfig())
  form.routingConfigText = toJson(defaultRoutingConfig())
  form.permissionConfigText = toJson(defaultPermissionConfig())
  form.observabilityConfigText = toJson(defaultObservabilityConfig())
  form.releaseConfigText = toJson(defaultReleaseConfig())
  form.batchConfigText = toJson(defaultBatchConfig())
  form.workflowConfigText = toJson(defaultWorkflowConfig())
  form.remark = ''
}

// 详情回填时统一把对象转回格式化 JSON，避免编辑区结构错乱。
function fillForm(skill: SkillItem) {
  selectedSkillId.value = skill.id
  selectedSkill.value = skill
  form.skillCode = skill.skillCode
  form.skillName = skill.skillName
  form.description = skill.description ?? ''
  form.skillCategory = skill.skillCategory
  form.skillStatus = skill.skillStatus
  form.versionMode = skill.versionMode
  form.hotUpdateEnabled = skill.hotUpdateEnabled
  form.intentConfigsText = toJson(skill.intentConfigs ?? defaultIntentConfigs())
  form.executionConfigText = toJson(skill.executionConfig ?? defaultExecutionConfig())
  form.routingConfigText = toJson(skill.routingConfig ?? defaultRoutingConfig())
  form.permissionConfigText = toJson(skill.permissionConfig ?? defaultPermissionConfig())
  form.observabilityConfigText = toJson(skill.observabilityConfig ?? defaultObservabilityConfig())
  form.releaseConfigText = toJson(skill.releaseConfig ?? defaultReleaseConfig())
  form.batchConfigText = toJson(skill.batchConfig ?? defaultBatchConfig())
  form.workflowConfigText = toJson(skill.workflowConfig ?? defaultWorkflowConfig())
  form.remark = skill.remark ?? ''
}

// 保存前把表单中的 JSON 文本还原成请求对象。
function buildPayload(): SkillPayload {
  return {
    skillCode: form.skillCode.trim(),
    skillName: form.skillName.trim(),
    description: form.description.trim() || null,
    skillCategory: form.skillCategory.trim(),
    skillStatus: form.skillStatus,
    versionMode: form.versionMode,
    hotUpdateEnabled: form.hotUpdateEnabled,
    intentConfigs: JSON.parse(form.intentConfigsText),
    executionConfig: JSON.parse(form.executionConfigText),
    routingConfig: JSON.parse(form.routingConfigText),
    permissionConfig: JSON.parse(form.permissionConfigText),
    observabilityConfig: JSON.parse(form.observabilityConfigText),
    releaseConfig: JSON.parse(form.releaseConfigText),
    batchConfig: JSON.parse(form.batchConfigText),
    workflowConfig: JSON.parse(form.workflowConfigText),
    remark: form.remark.trim() || null,
  }
}

// 统一控制悬浮提示的展示与自动关闭。
function showFeedback(tone: FeedbackTone, message: string) {
  if (feedbackTimer) clearTimeout(feedbackTimer)
  feedback.value = { tone, message }
  feedbackTimer = setTimeout(() => {
    feedback.value = null
    feedbackTimer = null
  }, 3200)
}

function clearFeedback() {
  if (feedbackTimer) clearTimeout(feedbackTimer)
  feedbackTimer = null
  feedback.value = null
}

function formatTime(value: number | null) {
  if (!value) return '未记录'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value)
}

// 刷新列表时尽量保持当前选中项，避免编辑中的上下文丢失。
async function loadSkills(successMessage?: string) {
  loading.value = true
  try {
    skills.value = await querySkills()
    selectedIds.value = selectedIds.value.filter((id) => skills.value.some((item) => item.id === id))
    if (selectedSkillId.value) {
      const current = skills.value.find((item) => item.id === selectedSkillId.value)
      if (current) {
        await selectSkill(current.id, false)
      } else {
        resetForm()
      }
    }
    if (successMessage) showFeedback('success', successMessage)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Skill 列表加载失败。'))
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  statsLoading.value = true
  try {
    statistics.value = await fetchSkillStats()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Skill 统计加载失败。'))
  } finally {
    statsLoading.value = false
  }
}

async function refreshAll(successMessage?: string) {
  await Promise.all([loadSkills(successMessage), loadStatistics()])
}

// 选中 Skill 后加载完整详情，而不是只依赖列表卡片中的简要数据。
async function selectSkill(skillId: number, clearExport = true) {
  try {
    fillForm(await fetchSkillDetail(skillId))
    if (clearExport) exportPayload.value = ''
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Skill 详情加载失败。'))
  }
}

// 发布、热更新、导出、导入等动作统一复用刷新逻辑，保持页面状态一致。
async function handleSubmit() {
  submitting.value = true
  try {
    const payload = buildPayload()
    if (selectedSkillId.value) {
      await updateSkill(selectedSkillId.value, payload)
      await refreshAll('Skill 已更新。')
    } else {
      await createSkill(payload)
      await refreshAll('Skill 已创建。')
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Skill 保存失败，请检查 JSON 配置。'))
  } finally {
    submitting.value = false
  }
}

async function handlePublish() {
  if (!selectedSkillId.value) return
  actionPending.value = true
  try {
    await publishSkill(selectedSkillId.value)
    await refreshAll('Skill 已发布上线。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Skill 发布失败。'))
  } finally {
    actionPending.value = false
  }
}

async function handleHotUpdate() {
  if (!selectedSkillId.value) return
  actionPending.value = true
  try {
    await hotUpdateSkill(selectedSkillId.value)
    await refreshAll('Skill 已开启热更新。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '热更新配置失败。'))
  } finally {
    actionPending.value = false
  }
}

async function handleExport(skillId?: number) {
  const targetId = skillId ?? selectedSkillId.value
  if (!targetId) return
  actionPending.value = true
  try {
    const result = await exportSkill(targetId)
    exportPayload.value = result.exportPayload
    await navigator.clipboard.writeText(result.exportPayload)
    if (selectedSkillId.value !== targetId) await selectSkill(targetId)
    showFeedback('success', '导出内容已复制到剪贴板。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Skill 导出失败。'))
  } finally {
    actionPending.value = false
  }
}

async function handleImport() {
  if (!importPayload.value.trim()) {
    showFeedback('error', '请输入导入内容。')
    return
  }
  actionPending.value = true
  try {
    await importSkill(importPayload.value.trim())
    importPayload.value = ''
    await refreshAll('Skill 已导入。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Skill 导入失败。'))
  } finally {
    actionPending.value = false
  }
}

function requestDelete(skill: SkillItem) {
  deleteTarget.value = skill
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  deletePending.value = true
  try {
    await removeSkill(deleteTarget.value.id)
    if (selectedSkillId.value === deleteTarget.value.id) resetForm()
    deleteTarget.value = null
    await refreshAll('Skill 已删除。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, 'Skill 删除失败。'))
  } finally {
    deletePending.value = false
  }
}

async function handleBatchDisable() {
  if (!hasSelection.value) return
  actionPending.value = true
  try {
    await batchUpdateSkillStatus(selectedIds.value, 'DISABLED')
    selectedIds.value = []
    await refreshAll('已批量停用选中的 Skill。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '批量停用失败。'))
  } finally {
    actionPending.value = false
  }
}

async function handleBatchDelete() {
  if (!hasSelection.value) return
  actionPending.value = true
  try {
    await batchDeleteSkills(selectedIds.value)
    selectedIds.value = []
    resetForm()
    await refreshAll('已批量删除选中的 Skill。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '批量删除失败。'))
  } finally {
    actionPending.value = false
  }
}

function toggleSelection(skillId: number, checked: boolean) {
  selectedIds.value = checked
    ? [...selectedIds.value.filter((item) => item !== skillId), skillId]
    : selectedIds.value.filter((item) => item !== skillId)
}

onMounted(() => {
  resetForm()
  void refreshAll()
})

onBeforeUnmount(() => clearFeedback())
</script>

<template>
  <MainShell>
    <Transition name="toast-fade">
      <section v-if="feedback" class="feedback-toast" :class="`feedback-toast--${feedback.tone}`">
        <div class="feedback-toast__body">
          <strong>{{ feedback.tone === 'success' ? '操作成功' : feedback.tone === 'error' ? '操作失败' : '提示' }}</strong>
          <span>{{ feedback.message }}</span>
        </div>
        <button type="button" class="feedback-toast__close" @click="clearFeedback">关闭</button>
      </section>
    </Transition>

    <section class="skill-page">
      <article class="panel-card skill-hero">
        <div class="skill-hero__copy">
          <p class="section-kicker">Skill Studio</p>
          <h2>用户管理 Skill 中心</h2>
          <p class="skill-hero__meta">覆盖增删改查、版本、意图、执行、路由、权限、风控、调试、测试、发布、热更新、批量治理与多渠道适配。</p>
          <p class="skill-hero__summary">{{ summary }}</p>
        </div>
        <div class="skill-hero__actions">
          <button type="button" class="app-button app-button--secondary" :disabled="loading || statsLoading" @click="refreshAll()">
            <RefreshCw :size="16" />
            刷新
          </button>
          <button type="button" class="app-button" @click="resetForm">
            <Plus :size="16" />
            新建 Skill
          </button>
        </div>
      </article>

      <div class="skill-grid">
        <article class="panel-card skill-list-panel">
          <div class="section-header skill-list-panel__head">
            <div>
              <strong>Skill 列表</strong>
              <p>统一治理用户管理类 Skill 的状态、版本和发布节奏。</p>
            </div>
            <div class="skill-list-panel__stats">
              <span>启用 {{ statistics.enabledCount }}</span>
              <span>发布 {{ statistics.publishedCount }}</span>
              <span>热更新 {{ statistics.hotUpdateEnabledCount }}</span>
            </div>
          </div>

          <div class="skill-batch-bar">
            <button type="button" class="app-button app-button--secondary" :disabled="!hasSelection || actionPending" @click="handleBatchDisable">
              <CheckSquare :size="16" />
              批量停用
            </button>
            <button type="button" class="app-button app-button--secondary" :disabled="!hasSelection || actionPending" @click="handleBatchDelete">
              <Trash2 :size="16" />
              批量删除
            </button>
          </div>

          <div v-if="loading" class="empty-state">正在加载 Skill 列表...</div>
          <div v-else-if="skills.length === 0" class="empty-state">当前还没有用户管理 Skill，请先创建。</div>
          <div v-else class="skill-list">
            <article v-for="item in skills" :key="item.id" class="skill-card" :class="{ 'skill-card--active': selectedSkillId === item.id }" @click="selectSkill(item.id)">
              <label class="skill-card__checkbox" @click.stop>
                <input :checked="selectedIds.includes(item.id)" type="checkbox" @change="toggleSelection(item.id, ($event.target as HTMLInputElement).checked)" />
              </label>
              <div class="skill-card__body">
                <div class="skill-card__head">
                  <div>
                    <strong>{{ item.skillName }}</strong>
                    <p>{{ item.skillCode }}</p>
                  </div>
                  <span class="skill-card__tag">{{ item.publishStatus }}</span>
                </div>
                <p class="skill-card__desc">{{ item.description || '暂未填写 Skill 描述。' }}</p>
                <div class="skill-card__meta">
                  <span>版本 {{ item.currentVersionNo || '-' }}</span>
                  <span>{{ item.skillStatus }}</span>
                  <span>{{ item.hotUpdateEnabled ? '热更新开启' : '热更新关闭' }}</span>
                </div>
                <div class="skill-card__actions">
                  <button type="button" class="app-button app-button--secondary" :disabled="actionPending" @click.stop="handleExport(item.id)">导出</button>
                  <button type="button" class="app-button app-button--secondary" :disabled="deletePending" @click.stop="requestDelete(item)">删除</button>
                </div>
              </div>
            </article>
          </div>
        </article>

        <article class="panel-card skill-editor">
          <div class="section-header skill-editor__head">
            <div>
              <strong>{{ isEditing ? '编辑 Skill' : '新建 Skill' }}</strong>
              <p>按模块维护配置，便于后续发布、导入导出和版本追踪。</p>
            </div>
            <div class="skill-editor__actions">
              <button type="button" class="app-button app-button--secondary" :disabled="!isEditing || actionPending" @click="handlePublish">
                <Wand2 :size="16" />
                发布上线
              </button>
              <button type="button" class="app-button app-button--secondary" :disabled="!isEditing || actionPending" @click="handleHotUpdate">
                <Sparkles :size="16" />
                热更新
              </button>
              <button type="button" class="app-button" :disabled="submitting" @click="handleSubmit">{{ submitting ? '保存中...' : '保存 Skill' }}</button>
            </div>
          </div>

          <div class="skill-form">
            <section class="skill-section">
              <p class="section-kicker">基础信息</p>
              <div class="skill-form__grid skill-form__grid--basic">
                <label class="field"><span class="field__label">Skill 编码</span><div class="input-shell"><input v-model="form.skillCode" class="app-input" type="text" :disabled="isEditing" placeholder="USER_MANAGEMENT_SKILL" /></div></label>
                <label class="field"><span class="field__label">Skill 名称</span><div class="input-shell"><input v-model="form.skillName" class="app-input" type="text" placeholder="用户管理 Skill" /></div></label>
                <label class="field"><span class="field__label">分类</span><div class="input-shell"><input v-model="form.skillCategory" class="app-input" type="text" placeholder="USER_MANAGEMENT" /></div></label>
                <label class="field"><span class="field__label">状态</span><select v-model="form.skillStatus" class="app-select"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label>
                <label class="field"><span class="field__label">版本模式</span><select v-model="form.versionMode" class="app-select"><option value="MANUAL">手动版本</option><option value="AUTO">自动版本</option></select></label>
                <label class="field"><span class="field__label">热更新</span><select v-model.number="form.hotUpdateEnabled" class="app-select"><option :value="0">关闭</option><option :value="1">开启</option></select></label>
              </div>
              <label class="field"><span class="field__label">描述</span><div class="input-shell input-shell--textarea"><textarea v-model="form.description" class="app-textarea" rows="3" placeholder="描述 Skill 的范围、边界和适用场景" /></div></label>
              <label class="field"><span class="field__label">备注</span><div class="input-shell input-shell--textarea"><textarea v-model="form.remark" class="app-textarea" rows="2" placeholder="补充上线说明或协作备注"></textarea></div></label>
            </section>

            <section v-for="section in sections" :key="section.field" class="skill-section">
              <p class="section-kicker">{{ section.title }}</p>
              <div class="input-shell input-shell--textarea">
                <textarea v-model="form[section.field]" class="app-textarea skill-json" :rows="section.rows" />
              </div>
            </section>

            <section class="skill-section">
              <p class="section-kicker">导入 / 导出</p>
              <div class="skill-form__grid skill-form__grid--dual">
                <label class="field">
                  <span class="field__label">导入内容</span>
                  <div class="input-shell input-shell--textarea"><textarea v-model="importPayload" class="app-textarea skill-json" rows="8" placeholder="粘贴导出的 Skill JSON 内容" /></div>
                  <button type="button" class="app-button app-button--secondary" :disabled="actionPending" @click="handleImport"><Upload :size="16" />导入 Skill</button>
                </label>
                <label class="field">
                  <span class="field__label">导出内容</span>
                  <div class="input-shell input-shell--textarea"><textarea :value="exportPayload" class="app-textarea skill-json" rows="8" readonly placeholder="选中 Skill 后执行导出"></textarea></div>
                  <button type="button" class="app-button app-button--secondary" :disabled="!isEditing || actionPending" @click="handleExport()"><Download :size="16" />导出并复制</button>
                </label>
              </div>
            </section>

            <section v-if="isEditing" class="skill-section">
              <p class="section-kicker">版本信息</p>
              <div v-if="versions.length === 0" class="empty-state">当前 Skill 还没有版本记录。</div>
              <div v-else class="version-list">
                <article v-for="item in versions" :key="item.id" class="version-card">
                  <strong>V{{ item.versionNo }}</strong>
                  <span>{{ item.versionStatus }} / {{ item.publishStatus }}</span>
                  <small>{{ formatTime(item.createTime) }}</small>
                </article>
              </div>
            </section>
          </div>
        </article>
      </div>
    </section>

    <ConfirmDialog :model-value="Boolean(deleteTarget)" title="删除 Skill" :description="deleteTarget ? `确认删除 Skill「${deleteTarget.skillName}」吗？` : ''" confirm-text="删除" :loading="deletePending" @update:model-value="deleteTarget = null" @confirm="confirmDelete" />
  </MainShell>
</template>

<style scoped>
/* 悬浮提示 */
.feedback-toast { position: fixed; top: 24px; right: 24px; z-index: 1200; display: flex; gap: 16px; width: min(420px, calc(100vw - 32px)); padding: 16px 18px; border: 1px solid rgba(255,255,255,.12); border-radius: 20px; background: rgba(9,16,28,.94); box-shadow: 0 18px 48px rgba(3,8,18,.42); backdrop-filter: blur(18px); }
.feedback-toast--success { border-color: rgba(86,214,164,.34); }
.feedback-toast--error { border-color: rgba(255,120,120,.34); }
.feedback-toast--info { border-color: rgba(105,190,255,.34); }
.feedback-toast__body { display: grid; gap: 6px; flex: 1; }
.feedback-toast__body strong { color: var(--color-ink-strong); font-size: .94rem; }
.feedback-toast__body span { color: var(--color-ink-soft); line-height: 1.6; }
.feedback-toast__close { border: 0; padding: 0; color: var(--color-ink-muted); background: transparent; cursor: pointer; }
.toast-fade-enter-active,.toast-fade-leave-active { transition: opacity 180ms ease, transform 180ms ease; }
.toast-fade-enter-from,.toast-fade-leave-to { opacity: 0; transform: translate3d(0,-10px,0); }

/* 页面骨架 */
.skill-page { display: grid; gap: 24px; }
.skill-hero { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; padding: 26px 28px; }
.skill-hero__copy { display: grid; gap: 10px; max-width: 54rem; }
.skill-hero__copy h2 { font-size: clamp(1.9rem, 2.2vw, 2.55rem); }
.skill-hero__meta,.skill-hero__summary,.empty-state,.version-card span,.version-card small { color: var(--color-ink-soft); line-height: 1.7; }
.skill-hero__actions,.skill-batch-bar,.skill-editor__actions { display: flex; flex-wrap: wrap; gap: 12px; }
.skill-grid { display: grid; grid-template-columns: 360px minmax(0,1fr); gap: 24px; }
.skill-list-panel,.skill-editor { padding: 22px; }

/* 列表卡片 */
.skill-list-panel__head,.skill-editor__head,.skill-card__head,.skill-card__meta,.skill-card__actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.skill-list-panel__stats { display: grid; gap: 6px; color: var(--color-ink-muted); font-size: .82rem; text-align: right; }
.skill-list,.skill-form { display: grid; gap: 16px; margin-top: 18px; }
.skill-card { display: grid; grid-template-columns: 28px minmax(0,1fr); gap: 12px; padding: 16px; border: 1px solid rgba(255,255,255,.06); border-radius: 22px; background: rgba(255,255,255,.03); cursor: pointer; transition: transform 180ms ease, border-color 180ms ease, background-color 180ms ease; }
.skill-card:hover { transform: translateY(-1px); border-color: rgba(83,184,255,.22); }
.skill-card--active { border-color: rgba(119,224,255,.4); background: rgba(83,184,255,.08); }
.skill-card__checkbox { display: flex; align-items: flex-start; justify-content: center; padding-top: 4px; }
.skill-card__head p,.skill-card__desc { margin-top: 6px; color: var(--color-ink-soft); line-height: 1.6; }
.skill-card__tag { display: inline-flex; align-items: center; min-height: 28px; padding: 0 10px; border-radius: 999px; background: rgba(255,255,255,.08); color: var(--color-ink-strong); font-size: .74rem; }
.skill-card__meta { margin-top: 12px; color: var(--color-ink-muted); font-size: .8rem; }
.skill-card__actions { margin-top: 14px; }

/* 编辑表单 */
.skill-section { display: grid; gap: 12px; padding: 18px; border: 1px solid rgba(255,255,255,.05); border-radius: 24px; background: linear-gradient(180deg, rgba(255,255,255,.04), rgba(255,255,255,.02)), rgba(6,12,24,.7); }
.skill-form__grid { display: grid; gap: 14px; }
.skill-form__grid--basic { grid-template-columns: repeat(3, minmax(0,1fr)); }
.skill-form__grid--dual { grid-template-columns: repeat(2, minmax(0,1fr)); }
.skill-json { min-height: 180px; font-family: var(--font-mono); font-size: .84rem; }
.version-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px,1fr)); gap: 12px; }
.version-card { display: grid; gap: 6px; padding: 14px; border-radius: 18px; background: rgba(255,255,255,.035); border: 1px solid rgba(255,255,255,.05); }

/* 响应式适配 */
@media (max-width: 1180px) { .skill-grid,.skill-form__grid--basic,.skill-form__grid--dual { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .feedback-toast { top: 16px; right: 16px; left: 16px; width: auto; } .skill-hero,.skill-list-panel__head,.skill-editor__head,.skill-hero__actions,.skill-batch-bar,.skill-editor__actions { flex-direction: column; align-items: stretch; } }
</style>
