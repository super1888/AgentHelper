<script setup lang="ts">
// 文件用途：Skills 管理页面
// 核心功能：提供技能列表、批量操作、基础编辑、版本管理、导入导出、调试、测试用例与日志查看
import { computed, onMounted, reactive, ref } from 'vue'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  batchDeleteSkills,
  batchMoveSkillCategory,
  batchOfflineSkills,
  batchPublishSkills,
  batchUpdateSkillStatus,
  batchUpdateSkillTags,
  compareSkillVersions,
  copySkill,
  createSkill,
  createSkillTestCase,
  debugSkill,
  exportSkill,
  fetchSkillDetail,
  fetchSkillStats,
  hotUpdateSkill,
  importSkill,
  offlineSkill,
  publishSkill,
  queryDeletedSkills,
  querySkillLogs,
  querySkills,
  querySkillTestCases,
  removeSkill,
  removeSkillTestCase,
  restoreSkill,
  rollbackSkill,
  runSkillTestCase,
  updateSkill,
  updateSkillTestCase,
} from '@/api/skill'
import type {
  SkillDebugResult,
  SkillExecutionLogItem,
  SkillItem,
  SkillPayload,
  SkillStatistics,
  SkillTestCaseItem,
  SkillTestCasePayload,
  SkillVersionCompareResult,
} from '@/types/skill'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

const loading = ref(false)
const saving = ref(false)
const actionLoading = ref(false)
const selectedSkillId = ref<number | null>(null)
const selectedSkill = ref<SkillItem | null>(null)
const selectedSkillIds = ref<number[]>([])
const selectedTestCaseId = ref<number | null>(null)
const skills = ref<SkillItem[]>([])
const deletedSkills = ref<SkillItem[]>([])
const testCases = ref<SkillTestCaseItem[]>([])
const logs = ref<SkillExecutionLogItem[]>([])
const debugResult = ref<SkillDebugResult | null>(null)
const compareResult = ref<SkillVersionCompareResult | null>(null)
const exportText = ref('')
const feedback = ref<FeedbackState | null>(null)

const stats = ref<SkillStatistics>({
  totalCount: 0,
  enabledCount: 0,
  publishedCount: 0,
  hotUpdateEnabledCount: 0,
  deletedCount: 0,
  totalTestCaseCount: 0,
  totalLogCount: 0,
  successLogCount: 0,
  failureLogCount: 0,
})

const filters = reactive({
  keyword: '',
  publishStatus: 'ALL',
  skillStatus: 'ALL',
})

const form = reactive({
  skillCode: '',
  skillName: '',
  description: '',
  skillType: 'API_CALL',
  skillCategory: 'USER_MANAGEMENT',
  skillStatus: 'ENABLED',
  sortWeight: 100,
  versionCode: '',
  versionDescription: '',
  versionMode: 'MANUAL',
  hotUpdateEnabled: 1,
  tagsText: '[]',
  observabilityConfigText: '{\n  "debugEnabled": 1,\n  "logEnabled": 1\n}',
  releaseConfigText: '{\n  "hotUpdateEnabled": 1,\n  "releaseStage": "DRAFT",\n  "publishStrategy": "MANUAL"\n}',
  batchConfigText: '{\n  "batchEnabled": 1,\n  "importEnabled": 1,\n  "exportEnabled": 1,\n  "logicalDeleteEnabled": 1,\n  "recycleEnabled": 1,\n  "copyEnabled": 1\n}',
  workflowConfigText:
    '{\n  "workflowEnabled": 0,\n  "workflowSteps": [],\n  "branchRules": [],\n  "loopEnabled": 0,\n  "childSkillCodes": [],\n  "channelAdapters": ["WEB"],\n  "orchestrationStrategy": "SEQUENTIAL"\n}',
  marketplaceConfigText: '{\n  "marketplaceEnabled": 0,\n  "reviewRequired": 1,\n  "storeVisible": 0\n}',
  remark: '',
})

const batchForm = reactive({
  skillStatus: 'ENABLED',
  targetCategoryCode: 'USER_MANAGEMENT',
  tagNamesText: '用户管理,核心',
})

const importForm = reactive({
  importPayload: '',
  publishAfterImport: 0,
})

const copyForm = reactive({
  newSkillCode: '',
  newSkillName: '',
  includeTestCases: 1,
})

const versionForm = reactive({
  sourceVersionNo: 1,
  targetVersionNo: 1,
  rollbackVersionNo: 1,
  rollbackDescription: '',
})

const debugForm = reactive({
  inputText: '查询用户 10001 的信息',
  forcedIntent: '',
  slotPayloadText: '{\n  "userId": "10001"\n}',
  contextPayloadText: '{\n  "operator": "admin"\n}',
})

const logQuery = reactive({
  sourceType: '',
  successFlag: '',
})

const testCaseForm = reactive({
  caseName: '',
  inputText: '',
  slotPayloadText: '{\n  "userId": "10001"\n}',
  expectedIntent: '',
  expectedResponseContains: '',
})

const filteredSkills = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return skills.value.filter((item) => {
    const matchKeyword = !keyword
      || [item.skillName, item.skillCode, item.description, item.skillCategory]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    const matchPublish = filters.publishStatus === 'ALL' || item.publishStatus === filters.publishStatus
    const matchStatus = filters.skillStatus === 'ALL' || item.skillStatus === filters.skillStatus
    return matchKeyword && matchPublish && matchStatus
  })
})

const currentSummary = computed(() => {
  if (!selectedSkill.value) return '未选择技能'
  return `${selectedSkill.value.skillName} / 当前版本 ${selectedSkill.value.currentVersionNo ?? '-'} / 发布状态 ${selectedSkill.value.publishStatus}`
})

const selectedCountText = computed(() => `已勾选 ${selectedSkillIds.value.length} 个技能`)

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

function resetTestCaseForm() {
  selectedTestCaseId.value = null
  testCaseForm.caseName = ''
  testCaseForm.inputText = ''
  testCaseForm.slotPayloadText = '{\n  "userId": "10001"\n}'
  testCaseForm.expectedIntent = ''
  testCaseForm.expectedResponseContains = ''
}

function resetForm() {
  selectedSkillId.value = null
  selectedSkill.value = null
  exportText.value = ''
  debugResult.value = null
  compareResult.value = null
  testCases.value = []
  logs.value = []
  form.skillCode = ''
  form.skillName = ''
  form.description = ''
  form.skillType = 'API_CALL'
  form.skillCategory = 'USER_MANAGEMENT'
  form.skillStatus = 'ENABLED'
  form.sortWeight = 100
  form.versionCode = ''
  form.versionDescription = ''
  form.versionMode = 'MANUAL'
  form.hotUpdateEnabled = 1
  form.tagsText = '[]'
  form.observabilityConfigText = '{\n  "debugEnabled": 1,\n  "logEnabled": 1\n}'
  form.releaseConfigText = '{\n  "hotUpdateEnabled": 1,\n  "releaseStage": "DRAFT",\n  "publishStrategy": "MANUAL"\n}'
  form.batchConfigText =
    '{\n  "batchEnabled": 1,\n  "importEnabled": 1,\n  "exportEnabled": 1,\n  "logicalDeleteEnabled": 1,\n  "recycleEnabled": 1,\n  "copyEnabled": 1\n}'
  form.workflowConfigText =
    '{\n  "workflowEnabled": 0,\n  "workflowSteps": [],\n  "branchRules": [],\n  "loopEnabled": 0,\n  "childSkillCodes": [],\n  "channelAdapters": ["WEB"],\n  "orchestrationStrategy": "SEQUENTIAL"\n}'
  form.marketplaceConfigText = '{\n  "marketplaceEnabled": 0,\n  "reviewRequired": 1,\n  "storeVisible": 0\n}'
  form.remark = ''
  resetTestCaseForm()
}

function fillForm(skill: SkillItem) {
  selectedSkillId.value = skill.id
  selectedSkill.value = skill
  form.skillCode = skill.skillCode
  form.skillName = skill.skillName
  form.description = skill.description ?? ''
  form.skillType = skill.skillType
  form.skillCategory = skill.skillCategory
  form.skillStatus = skill.skillStatus
  form.sortWeight = skill.sortWeight ?? 100
  form.versionCode = skill.versionCode ?? ''
  form.versionDescription = skill.versionDescription ?? ''
  form.versionMode = skill.versionMode
  form.hotUpdateEnabled = skill.hotUpdateEnabled ?? 0
  form.tagsText = JSON.stringify(skill.tags ?? [], null, 2)
  form.observabilityConfigText = JSON.stringify(skill.observabilityConfig ?? {}, null, 2)
  form.releaseConfigText = JSON.stringify(skill.releaseConfig ?? {}, null, 2)
  form.batchConfigText = JSON.stringify(skill.batchConfig ?? {}, null, 2)
  form.workflowConfigText = JSON.stringify(skill.workflowConfig ?? {}, null, 2)
  form.marketplaceConfigText = JSON.stringify(skill.marketplaceConfig ?? {}, null, 2)
  form.remark = skill.remark ?? ''
  if (skill.versions?.length) {
    versionForm.sourceVersionNo = skill.versions[0].versionNo
    versionForm.targetVersionNo = skill.versions[skill.versions.length - 1].versionNo
    versionForm.rollbackVersionNo = skill.versions[0].versionNo
  }
}

function fillTestCaseForm(item: SkillTestCaseItem) {
  selectedTestCaseId.value = item.id
  testCaseForm.caseName = item.caseName
  testCaseForm.inputText = item.inputText
  testCaseForm.slotPayloadText = item.slotPayloadJson || '{}'
  testCaseForm.expectedIntent = item.expectedIntent || ''
  testCaseForm.expectedResponseContains = item.expectedResponseContains || ''
}

function buildPayload(): SkillPayload {
  return {
    skillCode: form.skillCode.trim(),
    skillName: form.skillName.trim(),
    description: form.description.trim() || null,
    skillType: form.skillType,
    skillCategory: form.skillCategory.trim(),
    categoryChain: [{ categoryCode: form.skillCategory.trim(), categoryName: form.skillCategory.trim(), categoryLevel: 1 }],
    tags: parseJson(form.tagsText, '标签配置'),
    skillStatus: form.skillStatus,
    sortWeight: form.sortWeight,
    versionCode: form.versionCode.trim() || null,
    versionDescription: form.versionDescription.trim() || null,
    versionMode: form.versionMode,
    hotUpdateEnabled: form.hotUpdateEnabled,
    observabilityConfig: parseJson(form.observabilityConfigText, '观测配置'),
    releaseConfig: parseJson(form.releaseConfigText, '发布配置'),
    batchConfig: parseJson(form.batchConfigText, '批量配置'),
    workflowConfig: parseJson(form.workflowConfigText, '工作流配置'),
    channelAdaptations: [],
    marketplaceConfig: parseJson(form.marketplaceConfigText, '市场配置'),
    remark: form.remark.trim() || null,
  }
}

function buildTestCasePayload(): SkillTestCasePayload {
  return {
    caseName: testCaseForm.caseName.trim(),
    inputText: testCaseForm.inputText.trim(),
    slotPayload: parseJson(testCaseForm.slotPayloadText, '测试用例槽位'),
    expectedIntent: testCaseForm.expectedIntent.trim() || null,
    expectedSuccess: 1,
    expectedResponseContains: testCaseForm.expectedResponseContains.trim() || null,
    channelCode: 'WEB',
    locale: 'zh-CN',
    enabled: 1,
  }
}

async function loadStats() {
  stats.value = await fetchSkillStats()
}

async function loadSkills() {
  skills.value = await querySkills()
  deletedSkills.value = await queryDeletedSkills()
}

async function loadLogs() {
  logs.value = await querySkillLogs({
    skillId: selectedSkillId.value,
    sourceType: logQuery.sourceType || null,
    successFlag: logQuery.successFlag === '' ? null : Number(logQuery.successFlag),
  })
}

async function selectSkill(skillId: number) {
  const [detail, detailTestCases] = await Promise.all([fetchSkillDetail(skillId), querySkillTestCases(skillId)])
  fillForm(detail)
  testCases.value = detailTestCases
  await loadLogs()
}

async function refreshAll(keepSelection = true) {
  loading.value = true
  try {
    const currentId = keepSelection ? selectedSkillId.value : null
    await Promise.all([loadSkills(), loadStats()])
    if (currentId) {
      const exists = skills.value.find((item) => item.id === currentId)
      if (exists) {
        await selectSkill(currentId)
      } else {
        resetForm()
      }
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '技能数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const payload = buildPayload()
    if (selectedSkillId.value) {
      await updateSkill(selectedSkillId.value, payload)
      showFeedback('success', '技能已更新')
      await refreshAll()
    } else {
      const created = await createSkill(payload)
      showFeedback('success', '技能已创建')
      await refreshAll(false)
      await selectSkill(created.id)
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '技能保存失败'))
  } finally {
    saving.value = false
  }
}

async function handleAction(action: () => Promise<unknown>, message: string) {
  actionLoading.value = true
  try {
    await action()
    showFeedback('success', message)
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, `${message}失败`))
  } finally {
    actionLoading.value = false
  }
}

async function handleDelete(skillId: number) {
  if (!window.confirm('确认删除该技能吗？')) return
  await handleAction(() => removeSkill(skillId), '技能已删除')
  if (selectedSkillId.value === skillId) resetForm()
}

async function handleRestore(skillId: number) {
  await handleAction(() => restoreSkill(skillId), '技能已恢复')
}

async function handlePublish() {
  if (!selectedSkillId.value) return
  await handleAction(() => publishSkill(selectedSkillId.value as number), '技能已发布')
}

async function handleOffline() {
  if (!selectedSkillId.value) return
  await handleAction(() => offlineSkill(selectedSkillId.value as number), '技能已下线')
}

async function handleHotUpdate() {
  if (!selectedSkillId.value) return
  await handleAction(() => hotUpdateSkill(selectedSkillId.value as number), '技能已热更新')
}

async function handleExport() {
  if (!selectedSkillId.value) return
  actionLoading.value = true
  try {
    const result = await exportSkill(selectedSkillId.value)
    exportText.value = result.exportPayload
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(result.exportPayload)
    }
    showFeedback('success', '导出内容已生成')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '技能导出失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handleImport() {
  await handleAction(
    () =>
      importSkill({
        importPayload: importForm.importPayload.trim(),
        importFormat: 'JSON',
        publishAfterImport: importForm.publishAfterImport,
      }),
    '技能已导入',
  )
  importForm.importPayload = ''
}

async function handleCopy() {
  if (!selectedSkillId.value) return
  await handleAction(
    () =>
      copySkill(selectedSkillId.value as number, {
        newSkillCode: copyForm.newSkillCode.trim(),
        newSkillName: copyForm.newSkillName.trim(),
        includeTestCases: copyForm.includeTestCases,
      }),
    '技能已复制',
  )
}

async function handleCompareVersions() {
  if (!selectedSkillId.value) return
  actionLoading.value = true
  try {
    compareResult.value = await compareSkillVersions(selectedSkillId.value, {
      sourceVersionNo: versionForm.sourceVersionNo,
      targetVersionNo: versionForm.targetVersionNo,
    })
    showFeedback('success', '版本对比完成')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '版本对比失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handleRollback() {
  if (!selectedSkillId.value) return
  await handleAction(
    () =>
      rollbackSkill(selectedSkillId.value as number, {
        targetVersionNo: versionForm.rollbackVersionNo,
        versionDescription: versionForm.rollbackDescription.trim() || undefined,
      }),
    '版本已回滚',
  )
}

async function handleBatch(action: () => Promise<unknown>, message: string) {
  if (!selectedSkillIds.value.length) {
    showFeedback('info', '请先勾选技能')
    return
  }
  await handleAction(action, message)
}

async function handleBatchStatus() {
  await handleBatch(
    () => batchUpdateSkillStatus({ skillIds: selectedSkillIds.value, skillStatus: batchForm.skillStatus }),
    '批量状态更新成功',
  )
}

async function handleBatchTags() {
  await handleBatch(
    () =>
      batchUpdateSkillTags({
        skillIds: selectedSkillIds.value,
        tagNames: batchForm.tagNamesText.split(',').map((item) => item.trim()).filter(Boolean),
      }),
    '批量标签更新成功',
  )
}

async function handleBatchCategory() {
  await handleBatch(
    () => batchMoveSkillCategory({ skillIds: selectedSkillIds.value, targetCategoryCode: batchForm.targetCategoryCode }),
    '批量分类迁移成功',
  )
}

async function handleBatchPublish() {
  await handleBatch(() => batchPublishSkills({ skillIds: selectedSkillIds.value }), '批量发布成功')
}

async function handleBatchOffline() {
  await handleBatch(() => batchOfflineSkills({ skillIds: selectedSkillIds.value }), '批量下线成功')
}

async function handleBatchDelete() {
  if (!selectedSkillIds.value.length || !window.confirm('确认批量删除选中技能吗？')) return
  await handleBatch(() => batchDeleteSkills({ skillIds: selectedSkillIds.value }), '批量删除成功')
}

async function handleDebug() {
  actionLoading.value = true
  try {
    debugResult.value = await debugSkill({
      skillId: selectedSkillId.value,
      inputText: debugForm.inputText.trim(),
      forcedIntent: debugForm.forcedIntent.trim() || undefined,
      slotPayload: parseJson(debugForm.slotPayloadText, '调试槽位'),
      contextPayload: parseJson(debugForm.contextPayloadText, '调试上下文'),
      channelCode: 'WEB',
      locale: 'zh-CN',
    })
    await loadLogs()
    showFeedback('success', '调试执行完成')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '调试执行失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handleSaveTestCase() {
  if (!selectedSkillId.value) return
  actionLoading.value = true
  try {
    if (selectedTestCaseId.value) {
      await updateSkillTestCase(selectedTestCaseId.value, buildTestCasePayload())
      showFeedback('success', '测试用例已更新')
    } else {
      await createSkillTestCase(selectedSkillId.value, buildTestCasePayload())
      showFeedback('success', '测试用例已创建')
    }
    testCases.value = await querySkillTestCases(selectedSkillId.value)
    resetTestCaseForm()
    await refreshAll()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '测试用例保存失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handleRunTestCase(testCaseId: number) {
  actionLoading.value = true
  try {
    debugResult.value = await runSkillTestCase(testCaseId)
    if (selectedSkillId.value) {
      testCases.value = await querySkillTestCases(selectedSkillId.value)
      await loadLogs()
    }
    showFeedback('success', '测试用例已执行')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '测试用例执行失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handleDeleteTestCase(testCaseId: number) {
  if (!window.confirm('确认删除该测试用例吗？')) return
  await handleAction(() => removeSkillTestCase(testCaseId), '测试用例已删除')
  if (selectedSkillId.value) {
    testCases.value = await querySkillTestCases(selectedSkillId.value)
  }
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
    <section class="skill-page">
      <article class="panel-card hero-panel">
        <div class="hero-panel__head">
          <div>
            <p class="section-kicker">Skill Workspace</p>
            <h2>Skills 管理台</h2>
            <p class="hero-panel__summary">{{ currentSummary }}</p>
          </div>
          <div class="toolbar-actions">
            <button class="app-button app-button--secondary" type="button" :disabled="loading" @click="refreshAll()">刷新</button>
            <button class="app-button" type="button" @click="resetForm">新建技能</button>
          </div>
        </div>
        <div v-if="false && feedback" class="feedback-banner" :class="`feedback-banner--${feedback?.tone}`">
          {{ feedback?.message }}
        </div>
        <div class="stats-strip">
          <article class="metric-card"><span>技能总数</span><strong>{{ stats.totalCount }}</strong></article>
          <article class="metric-card"><span>已发布</span><strong>{{ stats.publishedCount }}</strong></article>
          <article class="metric-card"><span>回收站</span><strong>{{ stats.deletedCount ?? 0 }}</strong></article>
          <article class="metric-card"><span>测试用例</span><strong>{{ stats.totalTestCaseCount ?? 0 }}</strong></article>
          <article class="metric-card"><span>执行日志</span><strong>{{ stats.totalLogCount ?? 0 }}</strong></article>
        </div>
      </article>

      <article class="panel-card section-panel">
        <div class="section-panel__head">
          <div>
            <h3>技能列表</h3>
            <p>先在这里筛选、勾选和定位技能，再进入下方编辑区处理详细配置。</p>
          </div>
          <p class="section-panel__hint">{{ selectedCountText }}</p>
        </div>

        <div class="filter-grid">
          <label class="field"><span class="field__label">关键字搜索</span><input v-model="filters.keyword" class="app-input" type="text" placeholder="按名称、编码、描述、分类搜索" /></label>
          <label class="field"><span class="field__label">发布状态</span><select v-model="filters.publishStatus" class="app-select"><option value="ALL">全部</option><option value="DRAFT">草稿</option><option value="PUBLISHED">已发布</option><option value="OFFLINE">已下线</option></select></label>
          <label class="field"><span class="field__label">技能状态</span><select v-model="filters.skillStatus" class="app-select"><option value="ALL">全部</option><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label>
        </div>

        <details class="config-card batch-card">
          <summary>批量操作面板</summary>
          <div class="config-card__body">
            <div class="batch-panel__form">
              <label class="field"><span class="field__label">批量状态</span><select v-model="batchForm.skillStatus" class="app-select"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label>
              <label class="field"><span class="field__label">批量分类</span><input v-model="batchForm.targetCategoryCode" class="app-input" type="text" /></label>
              <label class="field"><span class="field__label">批量标签</span><input v-model="batchForm.tagNamesText" class="app-input" type="text" /></label>
            </div>

            <div class="action-grid">
              <button class="app-button app-button--secondary" type="button" @click="handleBatchStatus">更新状态</button>
              <button class="app-button app-button--secondary" type="button" @click="handleBatchTags">更新标签</button>
              <button class="app-button app-button--secondary" type="button" @click="handleBatchCategory">迁移分类</button>
              <button class="app-button app-button--secondary" type="button" @click="handleBatchPublish">批量发布</button>
              <button class="app-button app-button--secondary" type="button" @click="handleBatchOffline">批量下线</button>
              <button class="app-button app-button--secondary" type="button" @click="handleBatchDelete">批量删除</button>
            </div>
          </div>
        </details>

        <div v-if="loading" class="empty-state">正在加载技能列表...</div>
        <div v-else-if="filteredSkills.length === 0" class="empty-state">当前没有符合筛选条件的技能。</div>
        <div v-else class="skill-table">
          <div class="skill-table__header">
            <span>选择</span>
            <span>技能信息</span>
            <span>分类 / 类型</span>
            <span>状态 / 版本</span>
            <span>操作</span>
          </div>
          <label v-for="item in filteredSkills" :key="item.id" class="skill-row" :class="{ 'skill-row--active': selectedSkillId === item.id }">
            <div class="skill-row__select"><input v-model="selectedSkillIds" type="checkbox" :value="item.id" /></div>
            <button type="button" class="skill-row__main" @click="selectSkill(item.id)">
              <strong>{{ item.skillName }}</strong>
              <span>{{ item.skillCode }}</span>
              <p class="skill-row__desc">{{ item.description || '暂无描述' }}</p>
            </button>
            <div class="skill-row__group">
              <span class="table-tag">{{ item.skillCategory }}</span>
              <span class="table-tag">{{ item.skillType }}</span>
            </div>
            <div class="skill-row__group">
              <span class="table-tag">{{ item.skillStatus }}</span>
              <span class="table-tag">{{ item.publishStatus }}</span>
              <span class="table-tag">V{{ item.currentVersionNo ?? '-' }}</span>
            </div>
            <div class="skill-row__actions">
              <button class="app-button app-button--secondary" type="button" @click="selectSkill(item.id)">编辑</button>
              <button class="app-button app-button--secondary" type="button" @click="handleDelete(item.id)">删除</button>
            </div>
          </label>
        </div>
      </article>

      <article class="panel-card section-panel section-panel--compact">
        <div class="section-panel__head">
          <div>
            <h3>回收站</h3>
            <p>已删除技能单独放在这里，避免和主列表混在一起。</p>
          </div>
        </div>
        <div v-if="deletedSkills.length === 0" class="empty-state empty-state--compact">暂无已删除技能</div>
        <div v-else class="deleted-list">
          <div v-for="item in deletedSkills" :key="item.id" class="deleted-row">
            <div class="deleted-row__main">
              <strong>{{ item.skillName }}</strong>
              <p>{{ item.skillCode }}</p>
            </div>
            <button class="app-button app-button--secondary" type="button" @click="handleRestore(item.id)">恢复</button>
          </div>
        </div>
      </article>

      <article class="panel-card section-panel">
        <div class="section-panel__head">
          <div>
            <h3>技能编辑</h3>
            <p>基础信息直接展示，高级 JSON 配置折叠收起，减少页面压迫感。</p>
          </div>
          <div class="toolbar-actions">
            <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId || actionLoading" @click="handlePublish">发布</button>
            <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId || actionLoading" @click="handleOffline">下线</button>
            <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId || actionLoading" @click="handleHotUpdate">热更新</button>
            <button class="app-button" type="button" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存技能' }}</button>
          </div>
        </div>

        <div class="selected-banner">
          <strong>{{ selectedSkillId ? '当前正在编辑已选技能' : '当前为新建技能' }}</strong>
          <span>{{ currentSummary }}</span>
        </div>

        <section class="editor-block">
          <div class="editor-block__head">
            <h4>基础信息</h4>
            <p>常用字段全部保持展开，避免来回查找。</p>
          </div>
          <div class="form-grid form-grid--triple">
            <label class="field"><span class="field__label">技能编码</span><input v-model="form.skillCode" class="app-input" type="text" :disabled="Boolean(selectedSkillId)" /></label>
            <label class="field"><span class="field__label">技能名称</span><input v-model="form.skillName" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">技能类型</span><input v-model="form.skillType" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">技能分类</span><input v-model="form.skillCategory" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">技能状态</span><select v-model="form.skillStatus" class="app-select"><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label>
            <label class="field"><span class="field__label">排序权重</span><input v-model.number="form.sortWeight" class="app-input" type="number" /></label>
            <label class="field"><span class="field__label">版本编码</span><input v-model="form.versionCode" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">版本模式</span><select v-model="form.versionMode" class="app-select"><option value="MANUAL">手动</option><option value="AUTO">自动</option></select></label>
            <label class="field"><span class="field__label">热更新</span><select v-model.number="form.hotUpdateEnabled" class="app-select"><option :value="1">开启</option><option :value="0">关闭</option></select></label>
          </div>
          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">描述</span><textarea v-model="form.description" class="app-textarea" rows="4" /></label>
            <label class="field"><span class="field__label">版本说明</span><textarea v-model="form.versionDescription" class="app-textarea" rows="4" /></label>
          </div>
          <label class="field"><span class="field__label">备注</span><textarea v-model="form.remark" class="app-textarea" rows="3" /></label>
        </section>

        <section class="editor-block">
          <div class="editor-block__head">
            <h4>高级配置</h4>
            <p>不常改的 JSON 统一折叠，保留功能但减少视觉堆叠。</p>
          </div>
          <div class="config-list">
            <details class="config-card" open>
              <summary>标签与观测配置</summary>
              <div class="config-card__body form-grid form-grid--double">
                <label class="field"><span class="field__label">标签 JSON</span><textarea v-model="form.tagsText" class="app-textarea code-area" rows="8" /></label>
                <label class="field"><span class="field__label">观测配置 JSON</span><textarea v-model="form.observabilityConfigText" class="app-textarea code-area" rows="8" /></label>
              </div>
            </details>
            <details class="config-card">
              <summary>发布与批量配置</summary>
              <div class="config-card__body form-grid form-grid--double">
                <label class="field"><span class="field__label">发布配置 JSON</span><textarea v-model="form.releaseConfigText" class="app-textarea code-area" rows="8" /></label>
                <label class="field"><span class="field__label">批量配置 JSON</span><textarea v-model="form.batchConfigText" class="app-textarea code-area" rows="8" /></label>
              </div>
            </details>
            <details class="config-card">
              <summary>工作流与市场配置</summary>
              <div class="config-card__body form-grid form-grid--double">
                <label class="field"><span class="field__label">工作流配置 JSON</span><textarea v-model="form.workflowConfigText" class="app-textarea code-area" rows="9" /></label>
                <label class="field"><span class="field__label">市场配置 JSON</span><textarea v-model="form.marketplaceConfigText" class="app-textarea code-area" rows="9" /></label>
              </div>
            </details>
          </div>
        </section>
      </article>

      <article class="panel-card section-panel">
        <div class="section-panel__head">
          <div>
            <h3>版本与资产操作</h3>
            <p>导入、导出、复制和版本回滚集中在同一区域，减少跳转和挤压。</p>
          </div>
        </div>

        <div class="workbench-tabs">
          <input id="asset-tab-version" class="tab-radio" name="asset-workbench" type="radio" checked />
          <input id="asset-tab-transfer" class="tab-radio" name="asset-workbench" type="radio" />

          <div class="tab-strip tab-strip--double">
            <label for="asset-tab-version">
              <strong>版本管理</strong>
              <span>对比、回滚、历史版本</span>
            </label>
            <label for="asset-tab-transfer">
              <strong>导入导出</strong>
              <span>导入、导出、复制技能</span>
            </label>
          </div>

          <div class="tab-panels">
            <section class="tab-panel tab-panel--asset-version">
              <div class="editor-block__head">
                <h4>版本管理</h4>
                <p>版本对比、回滚和历史版本一屏查看。</p>
              </div>
              <div class="form-grid form-grid--double">
                <label class="field"><span class="field__label">源版本</span><input v-model.number="versionForm.sourceVersionNo" class="app-input" type="number" /></label>
                <label class="field"><span class="field__label">目标版本</span><input v-model.number="versionForm.targetVersionNo" class="app-input" type="number" /></label>
              </div>
              <div class="toolbar-actions toolbar-actions--left">
                <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleCompareVersions">执行版本对比</button>
              </div>
              <div class="form-grid form-grid--double">
                <label class="field"><span class="field__label">回滚版本</span><input v-model.number="versionForm.rollbackVersionNo" class="app-input" type="number" /></label>
                <label class="field"><span class="field__label">回滚说明</span><input v-model="versionForm.rollbackDescription" class="app-input" type="text" /></label>
              </div>
              <div class="toolbar-actions toolbar-actions--left">
                <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleRollback">回滚到指定版本</button>
              </div>
              <div v-if="selectedSkill?.versions?.length" class="version-list">
                <div v-for="version in selectedSkill.versions" :key="version.id" class="version-row">
                  <strong>V{{ version.versionNo }}</strong>
                  <span>{{ version.versionStatus }}</span>
                  <span>{{ version.publishStatus }}</span>
                </div>
              </div>
              <pre v-if="compareResult" class="result-box">{{ compareResult.diffSummary }}</pre>
            </section>

            <section class="tab-panel tab-panel--asset-transfer">
              <div class="editor-block__head">
                <h4>导入、导出与复制</h4>
                <p>导入导出保留大输入框，复制参数单独放到下面。</p>
              </div>
              <label class="field"><span class="field__label">导入 JSON</span><textarea v-model="importForm.importPayload" class="app-textarea code-area" rows="9" /></label>
              <label class="field field--inline"><span class="field__label">导入后立即发布</span><input v-model="importForm.publishAfterImport" type="checkbox" :true-value="1" :false-value="0" /></label>
              <div class="toolbar-actions toolbar-actions--left">
                <button class="app-button app-button--secondary" type="button" @click="handleImport">导入技能</button>
                <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleExport">导出技能</button>
              </div>
              <label class="field"><span class="field__label">导出结果</span><textarea :value="exportText" class="app-textarea code-area" rows="9" readonly /></label>
              <div class="form-grid form-grid--double">
                <label class="field"><span class="field__label">复制后编码</span><input v-model="copyForm.newSkillCode" class="app-input" type="text" /></label>
                <label class="field"><span class="field__label">复制后名称</span><input v-model="copyForm.newSkillName" class="app-input" type="text" /></label>
              </div>
              <label class="field field--inline"><span class="field__label">复制测试用例</span><input v-model="copyForm.includeTestCases" type="checkbox" :true-value="1" :false-value="0" /></label>
              <div class="toolbar-actions toolbar-actions--left">
                <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleCopy">复制当前技能</button>
              </div>
            </section>
          </div>
        </div>
      </article>

      <article class="panel-card section-panel">
        <div class="section-panel__head">
          <div>
            <h3>调试、测试与日志</h3>
            <p>调试、用例和日志改成页签切换，默认只展开当前要处理的内容。</p>
          </div>
        </div>

        <div class="workbench-tabs">
          <input id="skill-tab-debug" class="tab-radio" name="skill-workbench" type="radio" checked />
          <input id="skill-tab-cases" class="tab-radio" name="skill-workbench" type="radio" />
          <input id="skill-tab-logs" class="tab-radio" name="skill-workbench" type="radio" />

          <div class="tab-strip">
            <label for="skill-tab-debug">
              <strong>在线调试</strong>
              <span>输入文本、槽位和上下文</span>
            </label>
            <label for="skill-tab-cases">
              <strong>测试用例</strong>
              <span>{{ testCases.length }} 个用例</span>
            </label>
            <label for="skill-tab-logs">
              <strong>执行日志</strong>
              <span>{{ logs.length }} 条记录</span>
            </label>
          </div>

          <div class="tab-panels">
            <section class="tab-panel tab-panel--debug">
              <div class="editor-block__head">
                <h4>在线调试</h4>
                <p>调试输入区域保持单独成段，便于看清 JSON 输入框。</p>
              </div>
              <label class="field"><span class="field__label">输入文本</span><textarea v-model="debugForm.inputText" class="app-textarea" rows="4" /></label>
              <label class="field"><span class="field__label">强制意图</span><input v-model="debugForm.forcedIntent" class="app-input" type="text" /></label>
              <div class="form-grid form-grid--double">
                <label class="field"><span class="field__label">槽位 JSON</span><textarea v-model="debugForm.slotPayloadText" class="app-textarea code-area" rows="8" /></label>
                <label class="field"><span class="field__label">上下文 JSON</span><textarea v-model="debugForm.contextPayloadText" class="app-textarea code-area" rows="8" /></label>
              </div>
              <div class="toolbar-actions toolbar-actions--left">
                <button class="app-button" type="button" @click="handleDebug">执行调试</button>
              </div>
              <pre v-if="debugResult" class="result-box">{{ JSON.stringify(debugResult, null, 2) }}</pre>
            </section>

            <section class="tab-panel tab-panel--cases">
              <div class="editor-block__head">
                <h4>测试用例</h4>
                <p>上面编辑，下面列表，避免字段和卡片混在一起。</p>
              </div>
              <label class="field"><span class="field__label">用例名称</span><input v-model="testCaseForm.caseName" class="app-input" type="text" /></label>
              <label class="field"><span class="field__label">输入文本</span><textarea v-model="testCaseForm.inputText" class="app-textarea" rows="4" /></label>
              <label class="field"><span class="field__label">槽位 JSON</span><textarea v-model="testCaseForm.slotPayloadText" class="app-textarea code-area" rows="6" /></label>
              <div class="form-grid form-grid--double">
                <label class="field"><span class="field__label">期望意图</span><input v-model="testCaseForm.expectedIntent" class="app-input" type="text" /></label>
                <label class="field"><span class="field__label">期望响应包含</span><input v-model="testCaseForm.expectedResponseContains" class="app-input" type="text" /></label>
              </div>
              <div class="toolbar-actions toolbar-actions--left">
                <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleSaveTestCase">{{ selectedTestCaseId ? '更新用例' : '新增用例' }}</button>
                <button class="app-button app-button--secondary" type="button" @click="resetTestCaseForm">清空表单</button>
              </div>
              <div v-if="testCases.length === 0" class="empty-state empty-state--compact">暂无测试用例</div>
              <div v-else class="compact-table">
                <div class="compact-table__header">
                  <span>用例</span>
                  <span>最近运行</span>
                  <span>操作</span>
                </div>
                <div v-for="item in testCases" :key="item.id" class="compact-row">
                  <div>
                    <strong>{{ item.caseName }}</strong>
                    <p>{{ item.inputText }}</p>
                  </div>
                  <span>{{ item.lastRunStatus || '未执行' }} / {{ formatTime(item.lastRunAt) }}</span>
                  <div class="compact-row__actions">
                    <button class="app-button app-button--secondary" type="button" @click="fillTestCaseForm(item)">编辑</button>
                    <button class="app-button app-button--secondary" type="button" @click="handleRunTestCase(item.id)">运行</button>
                    <button class="app-button app-button--secondary" type="button" @click="handleDeleteTestCase(item.id)">删除</button>
                  </div>
                </div>
              </div>
            </section>

            <section class="tab-panel tab-panel--logs">
              <div class="editor-block__head">
                <h4>执行日志</h4>
                <p>筛选条件在上方，结果列表保留清晰的纵向阅读顺序。</p>
              </div>
              <div class="form-grid form-grid--double">
                <label class="field"><span class="field__label">来源</span><input v-model="logQuery.sourceType" class="app-input" type="text" /></label>
                <label class="field"><span class="field__label">结果</span><select v-model="logQuery.successFlag" class="app-select"><option value="">全部</option><option value="1">成功</option><option value="0">失败</option></select></label>
              </div>
              <div class="toolbar-actions toolbar-actions--left">
                <button class="app-button app-button--secondary" type="button" @click="loadLogs">筛选日志</button>
              </div>
              <div v-if="logs.length === 0" class="empty-state empty-state--compact">暂无执行日志</div>
              <div v-else class="compact-table">
                <div class="compact-table__header">
                  <span>来源 / 技能</span>
                  <span>结果 / 时间</span>
                  <span>输入</span>
                </div>
                <div v-for="item in logs" :key="item.id" class="compact-row compact-row--logs">
                  <strong>{{ item.sourceType || 'UNKNOWN' }} / {{ item.skillCode || 'NO_SKILL' }}</strong>
                  <span>{{ item.successFlag === 1 ? '成功' : '失败' }} / {{ formatTime(item.createTime) }}</span>
                  <p>{{ item.inputText || '无输入文本' }}</p>
                </div>
              </div>
            </section>
          </div>
        </div>
      </article>
    </section>
  </MainShell>
</template>

<style scoped>
.skill-page { display: grid; gap: var(--layout-gap); }
.hero-panel, .section-panel, .metric-card, .feedback-banner, .result-box, .empty-state, .stack-card, .config-card { border: 1px solid rgba(255,255,255,.08); }
.hero-panel, .section-panel {
  padding: var(--panel-padding);
  border-radius: var(--panel-radius);
  background: linear-gradient(180deg, rgba(255,255,255,.034), rgba(255,255,255,.012)), rgba(7,14,26,.82);
  box-shadow: 0 24px 56px rgba(0,0,0,.22);
  min-width: 0;
  overflow: visible;
}
.section-panel--compact { padding-top: var(--compact-panel-padding); padding-bottom: var(--compact-panel-padding); }
.hero-panel__head, .section-panel__head, .toolbar-actions, .stack-card__head, .editor-block__head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; min-width: 0;
}
.hero-panel__head h2, .section-panel__head h3, .editor-block__head h4 { margin: 0; color: var(--color-ink-strong); }
.hero-panel__head h2 { font-size: clamp(2rem, 2.8vw, 2.8rem); }
.section-kicker, .hero-panel__summary, .section-panel__head p, .section-panel__hint, .metric-card span, .editor-block__head p, .stack-card p, .stack-card small, .deleted-row p, .empty-state {
  color: var(--color-ink-soft); line-height: 1.7;
}
.feedback-banner { margin-top: 18px; padding: 14px 16px; border-radius: 18px; background: rgba(255,255,255,.045); }
.feedback-banner--success { border-color: rgba(84,214,160,.35); }
.feedback-banner--error { border-color: rgba(255,112,112,.35); }
.feedback-banner--info { border-color: rgba(102,186,255,.35); }
.stats-strip { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-top: 22px; }
.metric-card { padding: 18px 20px; border-radius: 18px; background: linear-gradient(180deg, rgba(255,255,255,.045), rgba(255,255,255,.02)); }
.metric-card strong { display: block; margin-top: 10px; color: var(--color-ink-strong); font-size: 1.6rem; }
.filter-grid, .form-grid, .split-grid, .action-grid, .batch-panel__form { display: grid; gap: 16px; }
.filter-grid, .batch-panel__form, .form-grid--triple { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.form-grid--double, .split-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.field { display: grid; gap: 8px; }
.field > .app-input, .field > .app-textarea, .field > .app-select {
  width: 100%;
  border: 1px solid rgba(147,177,233,.28);
  border-radius: 14px;
  background: rgba(8,16,30,.96);
  box-shadow: inset 0 1px 0 rgba(255,255,255,.03), 0 10px 24px rgba(0,0,0,.1);
  transition: border-color 180ms ease, background-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}
.field > .app-input, .field > .app-select { min-height: 52px; padding: 0 14px; }
.field > .app-textarea { min-height: 132px; padding: 14px; }
.field > .app-input:hover, .field > .app-textarea:hover, .field > .app-select:hover { border-color: rgba(104,187,255,.38); background: rgba(10,19,34,1); }
.field > .app-input:focus, .field > .app-textarea:focus, .field > .app-select:focus {
  border-color: rgba(94,194,255,.48);
  background: rgba(10,19,34,1);
  box-shadow: 0 0 0 4px rgba(77,179,255,.14), 0 16px 32px rgba(0,0,0,.18);
  transform: translateY(-1px);
}
.field > .app-input::placeholder, .field > .app-textarea::placeholder { color: rgba(166,183,211,.56); }
.field__label { margin-left: 2px; color: var(--color-ink-strong); font-size: .92rem; font-weight: 600; letter-spacing: .01em; }
.field--inline { grid-template-columns: auto auto; justify-content: flex-start; align-items: center; gap: 12px; }
.batch-card .config-card__body { display: grid; gap: 16px; }
.action-grid { grid-template-columns: repeat(6, minmax(0, 1fr)); }
.skill-table { overflow: hidden; border: 1px solid rgba(255,255,255,.06); border-radius: 20px; background: rgba(255,255,255,.018); }
.skill-table__header, .skill-row {
  display: grid;
  grid-template-columns: 72px minmax(260px, 2.3fr) minmax(200px, 1.2fr) minmax(200px, 1.2fr) 180px;
  gap: 16px;
  align-items: center;
}
.skill-table__header {
  padding: 16px 18px; color: rgba(191,207,235,.82); font-size: .82rem; letter-spacing: .08em; text-transform: uppercase; background: rgba(255,255,255,.035);
}
.skill-row { padding: 18px; border-top: 1px solid rgba(255,255,255,.06); background: rgba(255,255,255,.01); transition: background-color 180ms ease, border-color 180ms ease; }
.skill-row:hover { background: rgba(255,255,255,.03); }
.skill-row--active { background: rgba(76,162,255,.08); border-color: rgba(108,201,255,.22); }
.skill-row__main { display: grid; gap: 6px; padding: 0; border: 0; background: transparent; color: inherit; text-align: left; cursor: pointer; }
.skill-row__main strong { color: var(--color-ink-strong); font-size: 1rem; }
.skill-row__main span, .skill-row__desc { color: var(--color-ink-soft); }
.skill-row__desc, .deleted-row p, .stack-card p { margin: 0; }
.skill-row__group, .skill-row__actions, .version-row { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.skill-row__actions { justify-content: flex-end; }
.table-tag, .version-row span {
  display: inline-flex; align-items: center; min-height: 28px; padding: 0 10px; border-radius: 999px; background: rgba(255,255,255,.06); color: var(--color-ink-soft); font-size: .8rem;
}
.deleted-list, .stack-list, .version-list, .config-list { display: grid; gap: 14px; }
.deleted-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 16px 0; border-bottom: 1px solid rgba(255,255,255,.06); }
.deleted-row:last-child, .version-row:last-child { border-bottom: 0; }
.deleted-row__main { display: grid; gap: 4px; }
.selected-banner, .editor-block, .config-card { background: rgba(255,255,255,.018); }
.selected-banner { display: grid; gap: 4px; padding: 16px 18px; border: 1px solid rgba(114,198,255,.16); border-radius: 18px; }
.selected-banner strong { color: var(--color-ink-strong); }
.selected-banner span { color: var(--color-ink-soft); }
.editor-block { display: grid; gap: 18px; padding: 22px; border: 1px solid rgba(255,255,255,.06); border-radius: 20px; }
.editor-block--soft { height: fit-content; }
.editor-block__head { padding-bottom: 14px; border-bottom: 1px solid rgba(255,255,255,.06); }
.config-card { overflow: hidden; border-radius: 18px; }
.config-card summary { padding: 16px 18px; color: var(--color-ink-strong); font-weight: 600; cursor: pointer; list-style: none; }
.config-card summary::-webkit-details-marker { display: none; }
.config-card summary::after { content: '+'; float: right; color: rgba(173,195,229,.72); font-size: 1.1rem; }
.config-card[open] summary::after { content: '-'; }
.config-card__body { padding: 0 18px 18px; }
.stack-card { padding: var(--compact-panel-padding); border-radius: 18px; background: rgba(255,255,255,.022); }
.toolbar-actions { flex-wrap: wrap; }
.hero-panel__head,
.section-panel__head,
.stack-card__head,
.editor-block__head {
  min-height: 60px;
}
.hero-panel__head > div:first-child,
.section-panel__head > div:first-child,
.stack-card__head > div:first-child,
.editor-block__head > div:first-child {
  min-width: 0;
  flex: 1 1 320px;
}
.toolbar-actions {
  flex: 1 1 320px;
  justify-content: flex-end;
}
.toolbar-actions--left { justify-content: flex-start; }
.workbench-tabs { display: grid; gap: 18px; }
.tab-radio { position: absolute; opacity: 0; pointer-events: none; }
.tab-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.tab-strip--double { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.tab-strip label {
  display: grid;
  gap: 4px;
  padding: 16px 18px;
  border: 1px solid rgba(255,255,255,.08);
  border-radius: 18px;
  background: rgba(255,255,255,.02);
  cursor: pointer;
  transition: border-color 180ms ease, background-color 180ms ease, transform 180ms ease;
}
.tab-strip label:hover { transform: translateY(-1px); border-color: rgba(111,193,255,.2); background: rgba(255,255,255,.032); }
.tab-strip strong { color: var(--color-ink-strong); }
.tab-strip span { color: var(--color-ink-soft); font-size: .85rem; }
#skill-tab-debug:checked ~ .tab-strip label[for='skill-tab-debug'],
#skill-tab-cases:checked ~ .tab-strip label[for='skill-tab-cases'],
#skill-tab-logs:checked ~ .tab-strip label[for='skill-tab-logs'] {
  border-color: rgba(94,194,255,.34);
  background: rgba(76,162,255,.09);
  box-shadow: 0 12px 24px rgba(0,0,0,.14);
}
.tab-panels {
  border: 1px solid rgba(255,255,255,.06);
  border-radius: 22px;
  background: rgba(255,255,255,.016);
  overflow: hidden;
}
.tab-panel {
  display: none;
  gap: 18px;
  padding: 24px;
}
#skill-tab-debug:checked ~ .tab-panels .tab-panel--debug,
#skill-tab-cases:checked ~ .tab-panels .tab-panel--cases,
#skill-tab-logs:checked ~ .tab-panels .tab-panel--logs {
  display: grid;
}
#asset-tab-version:checked ~ .tab-panels .tab-panel--asset-version,
#asset-tab-transfer:checked ~ .tab-panels .tab-panel--asset-transfer {
  display: grid;
}
.compact-table {
  display: grid;
  border: 1px solid rgba(255,255,255,.06);
  border-radius: 18px;
  overflow: hidden;
  background: rgba(255,255,255,.016);
}
.compact-table__header,
.compact-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.6fr) minmax(180px, .9fr) minmax(220px, 1.2fr);
  gap: 16px;
  align-items: center;
  padding: 16px 18px;
}
.compact-table__header {
  color: rgba(191,207,235,.82);
  font-size: .8rem;
  letter-spacing: .08em;
  text-transform: uppercase;
  background: rgba(255,255,255,.035);
}
.compact-row { border-top: 1px solid rgba(255,255,255,.06); }
.compact-row strong { color: var(--color-ink-strong); }
.compact-row span,
.compact-row p { color: var(--color-ink-soft); }
.compact-row__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-start;
}
.compact-row--logs p { margin: 0; }
.version-row { padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,.06); }
.result-box {
  margin: 0; padding: 16px; border-radius: 14px; background: rgba(8,16,30,.96); white-space: pre-wrap; word-break: break-word; font-family: var(--font-mono); font-size: .84rem;
}
.code-area { font-family: var(--font-mono); font-size: .84rem; }
.empty-state {
  display: grid; place-items: center; min-height: 120px; padding: 20px; border-style: dashed; border-radius: 16px; background: rgba(255,255,255,.014); text-align: center;
}
.empty-state--compact { min-height: 84px; }
@media (max-width: 1440px) {
  .stats-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .action-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .skill-table__header, .skill-row { grid-template-columns: 72px minmax(220px, 2fr) minmax(180px, 1fr) minmax(180px, 1fr) 160px; }
}
@media (max-width: 1120px) {
  .filter-grid, .batch-panel__form, .form-grid--triple, .form-grid--double, .split-grid, .stats-strip { grid-template-columns: 1fr; }
  .skill-table { display: grid; gap: 12px; overflow: visible; border: 0; background: transparent; }
  .skill-table__header { display: none; }
  .skill-row { grid-template-columns: 1fr; gap: 12px; border: 1px solid rgba(255,255,255,.06); border-radius: 18px; }
  .skill-row__actions { justify-content: flex-start; }
  .tab-strip { grid-template-columns: 1fr; }
  .compact-table__header { display: none; }
  .compact-row { grid-template-columns: 1fr; gap: 10px; }
}
@media (max-width: 760px) {
  .hero-panel, .section-panel { padding: var(--compact-panel-padding); border-radius: var(--sub-panel-radius); }
  .hero-panel__head, .section-panel__head, .toolbar-actions, .stack-card__head, .editor-block__head, .deleted-row { flex-direction: column; align-items: stretch; }
  .action-grid { grid-template-columns: 1fr; }
  .editor-block { padding: 18px; }
  .tab-panel { padding: 18px; }
}
</style>
