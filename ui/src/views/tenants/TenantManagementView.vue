<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ChevronLeft, ChevronRight, Plus, RefreshCw, Search, Trash2, UserRoundPen } from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MainShell from '@/components/MainShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import TenantFormDialog from '@/components/TenantFormDialog.vue'
import { createTenant, fetchTenantStats, queryTenants, removeTenant, updateTenant } from '@/api/tenant'
import type {
  CreateTenantPayload,
  TenantPageResult,
  TenantProfile,
  TenantStatistics,
  UpdateTenantPayload,
} from '@/types/tenant'
import { getErrorMessage } from '@/utils/errors'

type FilterStatus = 'all' | 'enabled' | 'disabled'
type DialogMode = 'create' | 'edit'
type FeedbackTone = 'success' | 'error' | 'info'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

const loading = ref(false)
const statsLoading = ref(false)
const submitting = ref(false)
const deletePending = ref(false)
const dialogOpen = ref(false)
const dialogMode = ref<DialogMode>('create')
const selectedTenant = ref<TenantProfile | null>(null)
const deleteTarget = ref<TenantProfile | null>(null)
const feedback = ref<FeedbackState | null>(null)

const filters = reactive({
  tenantCode: '',
  tenantName: '',
  status: 'all' as FilterStatus,
})

const pageState = ref<TenantPageResult>({
  list: [],
  total: 0,
  pageNum: 1,
  pageSize: 20,
  pages: 0,
})

const statistics = ref<TenantStatistics>({
  totalCount: 0,
  enabledCount: 0,
  disabledCount: 0,
})

const totalTenantsLabel = computed(() => (
  statsLoading.value ? '统计刷新中...' : `总租户数 ${statistics.value.totalCount}`
))

const statsSummary = computed(() => (
  statsLoading.value ? '正在刷新统计...' : `启用 ${statistics.value.enabledCount} / 停用 ${statistics.value.disabledCount}`
))

const resultsSummary = computed(() => (
  loading.value
    ? '正在加载租户列表...'
    : `共 ${pageState.value.total} 条，当前第 ${pageState.value.pageNum} / ${Math.max(pageState.value.pages, 1)} 页`
))

const deleteDescription = computed(() => (
  deleteTarget.value ? `确认删除租户“${deleteTarget.value.tenantName}”吗？该操作不可撤销。` : ''
))

const canGoPrev = computed(() => pageState.value.pageNum > 1)
const canGoNext = computed(() => pageState.value.pageNum < Math.max(pageState.value.pages, 1))

function normalizeText(value: string) {
  const normalized = value.trim()
  return normalized ? normalized : undefined
}

function normalizeStatus() {
  if (filters.status === 'enabled') return 1 as const
  if (filters.status === 'disabled') return 0 as const
  return null
}

function buildQuery(pageNum = 1) {
  return {
    pageNum,
    pageSize: 20,
    tenantCode: normalizeText(filters.tenantCode),
    tenantName: normalizeText(filters.tenantName),
    status: normalizeStatus(),
  }
}

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function resetFilters() {
  filters.tenantCode = ''
  filters.tenantName = ''
  filters.status = 'all'
  clearFeedback()
  void loadTenants(1)
}

function formatOwner(tenant: TenantProfile) {
  return tenant.ownerUserName || (tenant.ownerUserId ? `用户 ${tenant.ownerUserId}` : '未配置负责人')
}

function formatContact(tenant: TenantProfile) {
  const parts = [tenant.contactName, tenant.contactPhone].filter(Boolean)
  return parts.length > 0 ? parts.join(' / ') : '未填写联系人信息'
}

async function loadTenants(pageNum = 1, successMessage?: string) {
  loading.value = true
  try {
    pageState.value = await queryTenants(buildQuery(pageNum))
    if (successMessage) {
      showFeedback('success', successMessage)
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '租户列表加载失败。'))
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  statsLoading.value = true
  try {
    statistics.value = await fetchTenantStats()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '租户统计加载失败。'))
  } finally {
    statsLoading.value = false
  }
}

async function executeSearch() {
  await loadTenants(1)
}

async function refreshCurrentPage() {
  await Promise.all([loadTenants(pageState.value.pageNum), loadStatistics()])
}

async function goToPage(pageNum: number) {
  if (pageNum < 1 || pageNum > Math.max(pageState.value.pages, 1) || pageNum === pageState.value.pageNum) {
    return
  }
  await loadTenants(pageNum)
}

function openCreateDialog() {
  clearFeedback()
  dialogMode.value = 'create'
  selectedTenant.value = null
  dialogOpen.value = true
}

function openEditDialog(tenant: TenantProfile) {
  clearFeedback()
  dialogMode.value = 'edit'
  selectedTenant.value = tenant
  dialogOpen.value = true
}

async function handleDialogSubmit(event: { mode: DialogMode; payload: CreateTenantPayload | UpdateTenantPayload }) {
  submitting.value = true
  try {
    if (event.mode === 'create') {
      await createTenant(event.payload as CreateTenantPayload)
      dialogOpen.value = false
      await Promise.all([
        loadTenants(1, '租户已创建。'),
        loadStatistics(),
      ])
      return
    }
    if (!selectedTenant.value) {
      throw new Error('缺少待编辑的租户信息。')
    }
    await updateTenant(selectedTenant.value.id, event.payload as UpdateTenantPayload)
    dialogOpen.value = false
    await loadTenants(pageState.value.pageNum, '租户信息已更新。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '保存租户失败。'))
  } finally {
    submitting.value = false
  }
}

function requestDelete(tenant: TenantProfile) {
  deleteTarget.value = tenant
}

function handleDeleteDialogVisibility(visible: boolean) {
  if (!visible) {
    deleteTarget.value = null
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) {
    return
  }
  deletePending.value = true
  try {
    await removeTenant(deleteTarget.value.id)
    const deletedName = deleteTarget.value.tenantName
    const nextPage = pageState.value.list.length === 1 && pageState.value.pageNum > 1
      ? pageState.value.pageNum - 1
      : pageState.value.pageNum
    deleteTarget.value = null
    await Promise.all([
      loadTenants(nextPage, `租户 ${deletedName} 已删除。`),
      loadStatistics(),
    ])
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除租户失败。'))
  } finally {
    deletePending.value = false
  }
}

onMounted(() => {
  void Promise.all([loadTenants(), loadStatistics()])
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

    <section class="management-page">
      <article class="panel-card management-hero">
        <div>
          <p class="section-kicker">Tenant Center</p>
          <h2>租户管理</h2>
          <p class="management-hero__meta">{{ totalTenantsLabel }} · {{ statsSummary }}</p>
        </div>
        <div class="management-hero__actions">
          <button class="app-button app-button--secondary" :disabled="loading || statsLoading" @click="refreshCurrentPage">
            <RefreshCw :size="16" />
            刷新
          </button>
          <button class="app-button" @click="openCreateDialog">
            <Plus :size="16" />
            新建租户
          </button>
        </div>
      </article>

      <section class="management-stats">
        <article class="panel-card management-stat">
          <span>总租户数</span>
          <strong>{{ statistics.totalCount }}</strong>
        </article>
        <article class="panel-card management-stat">
          <span>启用租户</span>
          <strong>{{ statistics.enabledCount }}</strong>
        </article>
        <article class="panel-card management-stat">
          <span>停用租户</span>
          <strong>{{ statistics.disabledCount }}</strong>
        </article>
      </section>

      <article class="panel-card management-panel">
        <div class="management-head">
          <div>
            <strong>筛选条件</strong>
            <p>{{ resultsSummary }}</p>
          </div>
          <button class="app-button app-button--secondary" @click="resetFilters">重置</button>
        </div>

        <div class="management-filter-grid">
          <label class="field">
            <span class="field__label">租户编码</span>
            <div class="input-shell">
              <span class="input-shell__icon"><Search :size="16" /></span>
              <input v-model="filters.tenantCode" class="app-input" type="text" placeholder="按租户编码搜索" />
            </div>
          </label>
          <label class="field">
            <span class="field__label">租户名称</span>
            <div class="input-shell">
              <input v-model="filters.tenantName" class="app-input" type="text" placeholder="按租户名称搜索" />
            </div>
          </label>
          <label class="field">
            <span class="field__label">状态</span>
            <select v-model="filters.status" class="app-select">
              <option value="all">全部</option>
              <option value="enabled">启用</option>
              <option value="disabled">停用</option>
            </select>
          </label>
          <button class="app-button management-filter-grid__submit" :disabled="loading" @click="executeSearch">
            执行搜索
          </button>
        </div>
      </article>

      <article class="panel-card management-panel">
        <div v-if="loading" class="management-empty">正在加载租户列表...</div>
        <div v-else-if="pageState.list.length === 0" class="management-empty">当前没有匹配的租户数据，请调整筛选条件或新建租户。</div>
        <div v-else class="management-list">
          <article v-for="tenant in pageState.list" :key="tenant.id" class="management-card">
            <div class="management-card__head">
              <div>
                <strong>{{ tenant.tenantName }}</strong>
                <p>{{ tenant.tenantCode }}</p>
              </div>
              <StatusBadge :status="tenant.status" />
            </div>
            <p class="management-card__desc">{{ tenant.description || '暂无租户描述' }}</p>
            <div class="management-card__meta">
              <span>负责人：{{ formatOwner(tenant) }}</span>
              <span>联系人：{{ formatContact(tenant) }}</span>
              <span>成员数：{{ tenant.memberCount }}</span>
            </div>
            <div class="management-card__actions">
              <button class="app-button app-button--secondary" @click="openEditDialog(tenant)">
                <UserRoundPen :size="16" />
                编辑
              </button>
              <button class="app-button app-button--danger" @click="requestDelete(tenant)">
                <Trash2 :size="16" />
                删除
              </button>
            </div>
          </article>
        </div>

        <div class="management-pager">
          <button class="app-button app-button--secondary" :disabled="!canGoPrev || loading" @click="goToPage(pageState.pageNum - 1)">
            <ChevronLeft :size="16" />
            上一页
          </button>
          <span class="management-pager__summary">第 {{ pageState.pageNum }} / {{ Math.max(pageState.pages, 1) }} 页</span>
          <button class="app-button app-button--secondary" :disabled="!canGoNext || loading" @click="goToPage(pageState.pageNum + 1)">
            下一页
            <ChevronRight :size="16" />
          </button>
        </div>
      </article>
    </section>

    <TenantFormDialog
      v-model="dialogOpen"
      :mode="dialogMode"
      :tenant="selectedTenant"
      :submitting="submitting"
      @submit="handleDialogSubmit"
    />
    <ConfirmDialog
      :model-value="deleteTarget !== null"
      title="删除租户"
      :description="deleteDescription"
      confirm-text="确认删除"
      :loading="deletePending"
      @update:model-value="handleDeleteDialogVisibility"
      @confirm="confirmDelete"
    />
  </MainShell>
</template>
