<script setup lang="ts">
// 文件用途：Skills 管理页面
// 核心功能：提供技能列表、批量操作、基础编辑、版本管理、导入导出、调试、测试用例与日志查看
import { computed, onMounted, reactive, ref } from 'vue'
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
        <div v-if="feedback" class="feedback-banner" :class="`feedback-banner--${feedback.tone}`">
          {{ feedback.message }}
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
            <h3>技能列表与批量操作</h3>
            <p>先筛选和选择技能，再执行批量动作或进入下方单个技能编辑。</p>
          </div>
          <p class="section-panel__hint">{{ selectedCountText }}</p>
        </div>

        <div class="filter-grid">
          <label class="field"><span class="field__label">关键字搜索</span><input v-model="filters.keyword" class="app-input" type="text" placeholder="按名称、编码、描述、分类搜索" /></label>
          <label class="field"><span class="field__label">发布状态</span><select v-model="filters.publishStatus" class="app-select"><option value="ALL">全部</option><option value="DRAFT">草稿</option><option value="PUBLISHED">已发布</option><option value="OFFLINE">已下线</option></select></label>
          <label class="field"><span class="field__label">技能状态</span><select v-model="filters.skillStatus" class="app-select"><option value="ALL">全部</option><option value="ENABLED">启用</option><option value="DISABLED">停用</option></select></label>
        </div>

        <div class="batch-toolbar">
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

        <div v-if="loading" class="empty-state">正在加载技能列表...</div>
        <div v-else-if="filteredSkills.length === 0" class="empty-state">当前没有符合筛选条件的技能。</div>
        <div v-else class="skill-list">
          <label v-for="item in filteredSkills" :key="item.id" class="skill-row" :class="{ 'skill-row--active': selectedSkillId === item.id }">
            <div class="skill-row__select"><input v-model="selectedSkillIds" type="checkbox" :value="item.id" /></div>
            <button type="button" class="skill-row__main" @click="selectSkill(item.id)">
              <div class="skill-row__title"><strong>{{ item.skillName }}</strong><span>{{ item.skillCode }}</span></div>
              <div class="skill-row__meta"><span>{{ item.skillCategory }}</span><span>{{ item.skillType }}</span><span>{{ item.skillStatus }}</span><span>{{ item.publishStatus }}</span><span>V{{ item.currentVersionNo ?? '-' }}</span></div>
              <p class="skill-row__desc">{{ item.description || '暂无描述' }}</p>
            </button>
            <div class="skill-row__actions">
              <button class="app-button app-button--secondary" type="button" @click="selectSkill(item.id)">编辑</button>
              <button class="app-button app-button--secondary" type="button" @click="handleDelete(item.id)">删除</button>
            </div>
          </label>
        </div>
      </article>

      <article class="panel-card section-panel">
        <div class="section-panel__head">
          <div>
            <h3>已删除技能</h3>
            <p>这里展示回收站中的技能，恢复后会重新出现在列表中。</p>
          </div>
        </div>
        <div v-if="deletedSkills.length === 0" class="empty-state empty-state--compact">暂无已删除技能</div>
        <div v-else class="deleted-list">
          <div v-for="item in deletedSkills" :key="item.id" class="deleted-row">
            <div>
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
            <h3>基础信息与技能配置</h3>
            <p>单个技能的核心信息放在一个区域内，减少来回视线切换。</p>
          </div>
          <div class="toolbar-actions">
            <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId || actionLoading" @click="handlePublish">发布</button>
            <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId || actionLoading" @click="handleOffline">下线</button>
            <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId || actionLoading" @click="handleHotUpdate">热更新</button>
            <button class="app-button" type="button" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存技能' }}</button>
          </div>
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

        <label class="field"><span class="field__label">描述</span><textarea v-model="form.description" class="app-textarea" rows="3" /></label>
        <label class="field"><span class="field__label">版本说明</span><input v-model="form.versionDescription" class="app-input" type="text" /></label>
        <label class="field"><span class="field__label">标签 JSON</span><textarea v-model="form.tagsText" class="app-textarea code-area" rows="5" /></label>
        <label class="field"><span class="field__label">观测配置 JSON</span><textarea v-model="form.observabilityConfigText" class="app-textarea code-area" rows="6" /></label>
        <label class="field"><span class="field__label">发布配置 JSON</span><textarea v-model="form.releaseConfigText" class="app-textarea code-area" rows="6" /></label>
        <label class="field"><span class="field__label">批量配置 JSON</span><textarea v-model="form.batchConfigText" class="app-textarea code-area" rows="6" /></label>
        <label class="field"><span class="field__label">工作流配置 JSON</span><textarea v-model="form.workflowConfigText" class="app-textarea code-area" rows="6" /></label>
        <label class="field"><span class="field__label">市场配置 JSON</span><textarea v-model="form.marketplaceConfigText" class="app-textarea code-area" rows="5" /></label>
        <label class="field"><span class="field__label">备注</span><textarea v-model="form.remark" class="app-textarea" rows="3" /></label>
      </article>

      <article class="panel-card section-panel">
        <div class="section-panel__head">
          <div>
            <h3>版本管理与导入导出</h3>
            <p>把版本动作和资产流转放在同一行程里，避免分散到右侧窄栏。</p>
          </div>
        </div>

        <div class="split-grid">
          <section class="sub-panel">
            <h4>版本对比与回滚</h4>
            <div class="form-grid form-grid--double">
              <label class="field"><span class="field__label">源版本</span><input v-model.number="versionForm.sourceVersionNo" class="app-input" type="number" /></label>
              <label class="field"><span class="field__label">目标版本</span><input v-model.number="versionForm.targetVersionNo" class="app-input" type="number" /></label>
            </div>
            <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleCompareVersions">执行版本对比</button>
            <label class="field"><span class="field__label">回滚版本</span><input v-model.number="versionForm.rollbackVersionNo" class="app-input" type="number" /></label>
            <label class="field"><span class="field__label">回滚说明</span><input v-model="versionForm.rollbackDescription" class="app-input" type="text" /></label>
            <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleRollback">回滚到指定版本</button>
            <div v-if="selectedSkill?.versions?.length" class="version-list">
              <div v-for="version in selectedSkill.versions" :key="version.id" class="version-row">
                <strong>V{{ version.versionNo }}</strong>
                <span>{{ version.versionStatus }}</span>
                <span>{{ version.publishStatus }}</span>
              </div>
            </div>
            <pre v-if="compareResult" class="result-box">{{ compareResult.diffSummary }}</pre>
          </section>

          <section class="sub-panel">
            <h4>导入、导出与复制</h4>
            <label class="field"><span class="field__label">导入 JSON</span><textarea v-model="importForm.importPayload" class="app-textarea code-area" rows="8" /></label>
            <label class="field field--inline"><span class="field__label">导入后立即发布</span><input v-model="importForm.publishAfterImport" type="checkbox" :true-value="1" :false-value="0" /></label>
            <div class="toolbar-actions">
              <button class="app-button app-button--secondary" type="button" @click="handleImport">导入技能</button>
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleExport">导出技能</button>
            </div>
            <label class="field"><span class="field__label">导出结果</span><textarea :value="exportText" class="app-textarea code-area" rows="8" readonly /></label>
            <div class="form-grid form-grid--double">
              <label class="field"><span class="field__label">复制后编码</span><input v-model="copyForm.newSkillCode" class="app-input" type="text" /></label>
              <label class="field"><span class="field__label">复制后名称</span><input v-model="copyForm.newSkillName" class="app-input" type="text" /></label>
            </div>
            <label class="field field--inline"><span class="field__label">复制测试用例</span><input v-model="copyForm.includeTestCases" type="checkbox" :true-value="1" :false-value="0" /></label>
            <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleCopy">复制当前技能</button>
          </section>
        </div>
      </article>

      <article class="panel-card section-panel">
        <div class="section-panel__head">
          <div>
            <h3>调试、测试与执行日志</h3>
            <p>运行过程相关内容统一收拢，便于从输入、结果到日志连续排查。</p>
          </div>
        </div>

        <div class="sub-panel">
          <h4>在线调试</h4>
          <label class="field"><span class="field__label">输入文本</span><textarea v-model="debugForm.inputText" class="app-textarea" rows="3" /></label>
          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">强制意图</span><input v-model="debugForm.forcedIntent" class="app-input" type="text" /></label>
            <div></div>
          </div>
          <div class="form-grid form-grid--double">
            <label class="field"><span class="field__label">槽位 JSON</span><textarea v-model="debugForm.slotPayloadText" class="app-textarea code-area" rows="6" /></label>
            <label class="field"><span class="field__label">上下文 JSON</span><textarea v-model="debugForm.contextPayloadText" class="app-textarea code-area" rows="6" /></label>
          </div>
          <button class="app-button" type="button" @click="handleDebug">执行调试</button>
          <pre v-if="debugResult" class="result-box">{{ JSON.stringify(debugResult, null, 2) }}</pre>
        </div>

        <div class="split-grid">
          <section class="sub-panel">
            <h4>测试用例</h4>
            <label class="field"><span class="field__label">用例名称</span><input v-model="testCaseForm.caseName" class="app-input" type="text" /></label>
            <label class="field"><span class="field__label">输入文本</span><textarea v-model="testCaseForm.inputText" class="app-textarea" rows="3" /></label>
            <label class="field"><span class="field__label">槽位 JSON</span><textarea v-model="testCaseForm.slotPayloadText" class="app-textarea code-area" rows="5" /></label>
            <div class="form-grid form-grid--double">
              <label class="field"><span class="field__label">期望意图</span><input v-model="testCaseForm.expectedIntent" class="app-input" type="text" /></label>
              <label class="field"><span class="field__label">期望响应包含</span><input v-model="testCaseForm.expectedResponseContains" class="app-input" type="text" /></label>
            </div>
            <div class="toolbar-actions">
              <button class="app-button app-button--secondary" type="button" :disabled="!selectedSkillId" @click="handleSaveTestCase">{{ selectedTestCaseId ? '更新用例' : '新增用例' }}</button>
              <button class="app-button app-button--secondary" type="button" @click="resetTestCaseForm">清空表单</button>
            </div>
            <div v-if="testCases.length === 0" class="empty-state empty-state--compact">暂无测试用例</div>
            <div v-else class="stack-list">
              <article v-for="item in testCases" :key="item.id" class="stack-card">
                <div class="stack-card__head">
                  <strong>{{ item.caseName }}</strong>
                  <small>{{ item.lastRunStatus || '未执行' }} / {{ formatTime(item.lastRunAt) }}</small>
                </div>
                <p>{{ item.inputText }}</p>
                <div class="toolbar-actions">
                  <button class="app-button app-button--secondary" type="button" @click="fillTestCaseForm(item)">编辑</button>
                  <button class="app-button app-button--secondary" type="button" @click="handleRunTestCase(item.id)">运行</button>
                  <button class="app-button app-button--secondary" type="button" @click="handleDeleteTestCase(item.id)">删除</button>
                </div>
              </article>
            </div>
          </section>

          <section class="sub-panel">
            <h4>执行日志</h4>
            <div class="form-grid form-grid--double">
              <label class="field"><span class="field__label">来源</span><input v-model="logQuery.sourceType" class="app-input" type="text" /></label>
              <label class="field"><span class="field__label">结果</span><select v-model="logQuery.successFlag" class="app-select"><option value="">全部</option><option value="1">成功</option><option value="0">失败</option></select></label>
            </div>
            <button class="app-button app-button--secondary" type="button" @click="loadLogs">筛选日志</button>
            <div v-if="logs.length === 0" class="empty-state empty-state--compact">暂无执行日志</div>
            <div v-else class="stack-list">
              <article v-for="item in logs" :key="item.id" class="stack-card">
                <div class="stack-card__head">
                  <strong>{{ item.sourceType || 'UNKNOWN' }} / {{ item.skillCode || 'NO_SKILL' }}</strong>
                  <small>{{ item.successFlag === 1 ? '成功' : '失败' }} / {{ formatTime(item.createTime) }}</small>
                </div>
                <p>{{ item.inputText || '无输入文本' }}</p>
              </article>
            </div>
          </section>
        </div>
      </article>
    </section>
  </MainShell>
</template>

<style scoped>
.skill-page {
  display: grid;
  gap: 24px;
}

.hero-panel,
.section-panel,
.sub-panel,
.metric-card,
.skill-row,
.deleted-row,
.stack-card,
.feedback-banner,
.result-box,
.empty-state {
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
}

.hero-panel,
.section-panel {
  padding: 28px;
  border-radius: 28px;
}

.section-panel {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
    rgba(7, 14, 26, 0.78);
}

.hero-panel__head,
.section-panel__head,
.toolbar-actions,
.stack-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.hero-panel__head h2,
.section-panel__head h3,
.sub-panel h4 {
  margin: 0;
  color: var(--color-ink-strong);
}

.hero-panel__head h2 {
  font-size: clamp(2rem, 2.8vw, 2.8rem);
}

.hero-panel__summary,
.section-panel__head p,
.metric-card span,
.skill-row__desc,
.skill-row__title span,
.stack-card p,
.stack-card small,
.deleted-row p,
.empty-state,
.section-panel__hint {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.feedback-banner {
  padding: 14px 16px;
  margin-top: 18px;
  border-radius: 18px;
}

.feedback-banner--success {
  border-color: rgba(84, 214, 160, 0.35);
}

.feedback-banner--error {
  border-color: rgba(255, 112, 112, 0.35);
}

.feedback-banner--info {
  border-color: rgba(102, 186, 255, 0.35);
}

.stats-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.metric-card {
  padding: 18px;
  border-radius: 22px;
  border-color: rgba(255, 255, 255, 0.05);
  background: rgba(255, 255, 255, 0.028);
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  color: var(--color-ink-strong);
  font-size: 1.55rem;
}

.filter-grid,
.form-grid,
.batch-toolbar,
.split-grid,
.action-grid {
  display: grid;
  gap: 16px;
}

.filter-grid,
.batch-toolbar,
.form-grid--triple {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.form-grid--double,
.split-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.action-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.field {
  display: grid;
  gap: 8px;
}

.field > .app-input,
.field > .app-textarea,
.field > .app-select {
  width: 100%;
  border: 1px solid rgba(141, 171, 224, 0.2);
  border-radius: 18px;
  background: rgba(8, 16, 30, 0.84);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 0 0 1px rgba(255, 255, 255, 0.02);
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.field > .app-input {
  min-height: 56px;
  padding: 0 16px;
}

.field > .app-textarea {
  min-height: 132px;
  padding: 16px;
}

.field > .app-select {
  min-height: 56px;
  padding: 0 16px;
}

.field > .app-input:hover,
.field > .app-textarea:hover,
.field > .app-select:hover {
  border-color: rgba(104, 187, 255, 0.26);
  background: rgba(10, 19, 34, 0.92);
}

.field > .app-input:focus,
.field > .app-textarea:focus,
.field > .app-select:focus {
  border-color: rgba(94, 194, 255, 0.46);
  background: rgba(10, 19, 34, 0.98);
  box-shadow:
    0 0 0 4px rgba(77, 179, 255, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  transform: translateY(-1px);
}

.field > .app-input::placeholder,
.field > .app-textarea::placeholder {
  color: rgba(166, 183, 211, 0.56);
}

.field__label {
  color: var(--color-ink-strong);
  font-size: 0.92rem;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.field--inline {
  grid-template-columns: auto auto;
  justify-content: flex-start;
  align-items: center;
  gap: 12px;
}

.skill-list,
.deleted-list,
.stack-list,
.version-list {
  display: grid;
  gap: 12px;
}

.skill-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 18px;
  border-radius: 22px;
  border-color: rgba(255, 255, 255, 0.05);
  background: rgba(255, 255, 255, 0.028);
  transition: border-color 180ms ease, background-color 180ms ease, transform 180ms ease;
}

.skill-row:hover {
  transform: translateY(-1px);
  border-color: rgba(110, 200, 255, 0.22);
}

.skill-row--active {
  border-color: rgba(119, 224, 255, 0.38);
  background: rgba(83, 184, 255, 0.08);
}

.skill-row__main {
  display: grid;
  gap: 8px;
  padding: 0;
  border: 0;
  color: inherit;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.skill-row__title,
.skill-row__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.skill-row__title strong {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.skill-row__meta span,
.version-row span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.06);
  font-size: 0.8rem;
}

.skill-row__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.deleted-row,
.sub-panel,
.stack-card {
  padding: 18px;
  border-radius: 22px;
}

.deleted-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-color: rgba(255, 255, 255, 0.05);
  background: rgba(255, 255, 255, 0.025);
}

.sub-panel {
  display: grid;
  gap: 16px;
  border-color: rgba(255, 255, 255, 0.04);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.032), rgba(255, 255, 255, 0.012)),
    rgba(5, 11, 22, 0.56);
}

.sub-panel h4 {
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.stack-card {
  border-color: rgba(255, 255, 255, 0.05);
  background: rgba(255, 255, 255, 0.026);
}

.version-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.version-row:last-child {
  border-bottom: 0;
}

.result-box {
  margin: 0;
  padding: 16px;
  border-radius: 20px;
  border-color: rgba(105, 188, 255, 0.18);
  background: rgba(8, 16, 30, 0.82);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
  font-size: 0.84rem;
}

.code-area {
  font-family: var(--font-mono);
  font-size: 0.84rem;
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 120px;
  padding: 20px;
  border-radius: 20px;
  border-style: dashed;
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.018);
  text-align: center;
}

.empty-state--compact {
  min-height: 84px;
}

@media (max-width: 1380px) {
  .stats-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .action-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .filter-grid,
  .batch-toolbar,
  .form-grid--triple,
  .form-grid--double,
  .split-grid,
  .stats-strip {
    grid-template-columns: 1fr;
  }

  .skill-row {
    grid-template-columns: 28px minmax(0, 1fr);
  }

  .skill-row__actions {
    grid-column: 2;
  }
}

@media (max-width: 760px) {
  .hero-panel,
  .section-panel {
    padding: 20px;
  }

  .hero-panel__head,
  .section-panel__head,
  .toolbar-actions,
  .deleted-row,
  .stack-card__head {
    flex-direction: column;
    align-items: stretch;
  }

  .action-grid {
    grid-template-columns: 1fr;
  }

  .skill-row {
    grid-template-columns: 1fr;
  }

  .skill-row__select,
  .skill-row__actions {
    grid-column: auto;
  }
}
</style>
