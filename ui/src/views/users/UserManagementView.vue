<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ChevronLeft,
  ChevronRight,
  CircleUserRound,
  Layers3,
  Mail,
  Phone,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  UserCheck,
  UserRound,
  UserRoundPen,
  UserX,
  Users,
} from 'lucide-vue-next'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MainShell from '@/components/MainShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import UserFormDialog from '@/components/UserFormDialog.vue'
import { createUser, fetchUserStats, queryUsers, removeUser, updateUser } from '@/api/user'
import type { CreateUserPayload, UpdateUserPayload, UserPageResult, UserProfile, UserStatistics } from '@/types/user'
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
const selectedUser = ref<UserProfile | null>(null)
const deleteTarget = ref<UserProfile | null>(null)
const feedback = ref<FeedbackState | null>(null)

const filters = reactive({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  status: 'all' as FilterStatus,
})

const pageState = ref<UserPageResult>({
  list: [],
  total: 0,
  pageNum: 1,
  pageSize: 20,
  pages: 0,
})

const statistics = ref<UserStatistics>({
  totalCount: 0,
  enabledCount: 0,
  disabledCount: 0,
  tenantCount: 0,
})

const statCards = computed(() => [
  {
    label: '用户总数',
    value: String(statistics.value.totalCount),
    detail: '当前条件',
    icon: Users,
  },
  {
    label: '启用账号',
    value: String(statistics.value.enabledCount),
    detail: '状态 1',
    icon: UserCheck,
  },
  {
    label: '禁用账号',
    value: String(statistics.value.disabledCount),
    detail: '状态 0',
    icon: UserX,
  },
  {
    label: '租户分布',
    value: String(statistics.value.tenantCount),
    detail: '独立租户数',
    icon: Layers3,
  },
])

const resultsSummary = computed(() => {
  if (loading.value) {
    return '正在查询用户数据...'
  }

  const pages = Math.max(pageState.value.pages, 1)
  return `共 ${pageState.value.total} 条，第 ${pageState.value.pageNum} / ${pages} 页，每页 ${pageState.value.pageSize} 条`
})

const deleteDescription = computed(() =>
  deleteTarget.value ? `确认删除用户 ${deleteTarget.value.username} 吗？该操作不可撤销。` : '',
)

const canGoPrev = computed(() => pageState.value.pageNum > 1)
const canGoNext = computed(() => pageState.value.pageNum < Math.max(pageState.value.pages, 1))

function normalizeText(value: string) {
  const normalized = value.trim()
  return normalized ? normalized : undefined
}

function normalizeStatus() {
  if (filters.status === 'enabled') {
    return 1 as const
  }
  if (filters.status === 'disabled') {
    return 0 as const
  }
  return null
}

function buildQuery(pageNum = 1) {
  return {
    pageNum,
    pageSize: 20,
    username: normalizeText(filters.username),
    nickname: normalizeText(filters.nickname),
    phone: normalizeText(filters.phone),
    email: normalizeText(filters.email),
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
  filters.username = ''
  filters.nickname = ''
  filters.phone = ''
  filters.email = ''
  filters.status = 'all'
  clearFeedback()
  void executeSearch()
}

function formatContact(user: UserProfile) {
  const contactItems = [user.phone, user.email].filter(Boolean)
  return contactItems.length > 0 ? contactItems.join(' / ') : '未填写'
}

function formatTenant(tenantId: number | null) {
  return tenantId === null ? '默认租户' : `租户 ${tenantId}`
}

async function loadUsers(pageNum = 1, successMessage?: string) {
  loading.value = true

  try {
    pageState.value = await queryUsers(buildQuery(pageNum))
    if (successMessage) {
      showFeedback('success', successMessage)
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '用户列表加载失败。'))
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  statsLoading.value = true

  try {
    statistics.value = await fetchUserStats(buildQuery())
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '统计数据加载失败。'))
  } finally {
    statsLoading.value = false
  }
}

async function executeSearch() {
  await Promise.all([loadUsers(1), loadStatistics()])
}

async function refreshCurrentPage() {
  await Promise.all([loadUsers(pageState.value.pageNum), loadStatistics()])
}

async function goToPage(pageNum: number) {
  if (pageNum < 1 || pageNum > Math.max(pageState.value.pages, 1) || pageNum === pageState.value.pageNum) {
    return
  }

  await loadUsers(pageNum)
}

function openCreateDialog() {
  clearFeedback()
  dialogMode.value = 'create'
  selectedUser.value = null
  dialogOpen.value = true
}

function openEditDialog(user: UserProfile) {
  clearFeedback()
  dialogMode.value = 'edit'
  selectedUser.value = user
  dialogOpen.value = true
}

async function handleDialogSubmit(event: { mode: DialogMode; payload: CreateUserPayload | UpdateUserPayload }) {
  submitting.value = true

  try {
    if (event.mode === 'create') {
      await createUser(event.payload as CreateUserPayload)
      dialogOpen.value = false
      await Promise.all([loadUsers(1, '用户已创建。'), loadStatistics()])
      return
    }

    if (!selectedUser.value) {
      throw new Error('缺少待编辑的用户信息。')
    }

    await updateUser(selectedUser.value.id, event.payload as UpdateUserPayload)
    dialogOpen.value = false
    await Promise.all([loadUsers(pageState.value.pageNum, '用户信息已更新。'), loadStatistics()])
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '保存用户失败。'))
  } finally {
    submitting.value = false
  }
}

function requestDelete(user: UserProfile) {
  deleteTarget.value = user
}

function closeDeleteDialog() {
  deleteTarget.value = null
}

function handleDeleteDialogVisibility(visible: boolean) {
  if (!visible) {
    closeDeleteDialog()
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) {
    return
  }

  deletePending.value = true

  try {
    await removeUser(deleteTarget.value.id)
    const deletedUsername = deleteTarget.value.username
    const nextPage =
      pageState.value.list.length === 1 && pageState.value.pageNum > 1
        ? pageState.value.pageNum - 1
        : pageState.value.pageNum

    deleteTarget.value = null
    await Promise.all([loadUsers(nextPage, `用户 ${deletedUsername} 已删除。`), loadStatistics()])
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除用户失败。'))
  } finally {
    deletePending.value = false
  }
}

onMounted(() => {
  void Promise.all([loadUsers(), loadStatistics()])
})
</script>

<template>
  <MainShell>
    <section class="stats-grid" :aria-busy="statsLoading">
      <article v-for="card in statCards" :key="card.label" class="stats-card panel-card">
        <div class="stats-card__icon" aria-hidden="true">
          <component :is="card.icon" :size="18" />
        </div>
        <div>
          <p class="stats-card__label">{{ card.label }}</p>
          <strong class="stats-card__value">{{ card.value }}</strong>
          <p class="stats-card__detail">{{ card.detail }}</p>
        </div>
      </article>
    </section>

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

    <section class="workspace panel-card">
      <header class="workspace__header">
        <div class="workspace__headline">
          <p class="section-kicker">User Directory</p>
          <h2>用户管理</h2>
          <p>按用户名、昵称、手机号、邮箱和状态组合查询，默认第 1 页，每页 20 条。</p>
        </div>

        <div class="workspace__actions">
          <button
            type="button"
            class="app-button app-button--secondary"
            :disabled="loading || statsLoading"
            @click="refreshCurrentPage"
          >
            <RefreshCw :size="16" aria-hidden="true" />
            刷新列表
          </button>

          <button type="button" class="app-button" @click="openCreateDialog">
            <Plus :size="16" aria-hidden="true" />
            新增用户
          </button>
        </div>
      </header>

      <div class="workspace__toolbar panel-card">
        <label class="field">
          <span class="field__label">用户名</span>
          <div class="input-shell">
            <span class="input-shell__icon" aria-hidden="true">
              <UserRound :size="16" />
            </span>
            <input
              v-model="filters.username"
              class="app-input"
              type="text"
              placeholder="按用户名查询"
              @keyup.enter="executeSearch"
            />
          </div>
        </label>

        <label class="field">
          <span class="field__label">昵称</span>
          <div class="input-shell">
            <span class="input-shell__icon" aria-hidden="true">
              <Search :size="16" />
            </span>
            <input
              v-model="filters.nickname"
              class="app-input"
              type="text"
              placeholder="按昵称查询"
              @keyup.enter="executeSearch"
            />
          </div>
        </label>

        <label class="field">
          <span class="field__label">手机号</span>
          <div class="input-shell">
            <span class="input-shell__icon" aria-hidden="true">
              <Phone :size="16" />
            </span>
            <input
              v-model="filters.phone"
              class="app-input"
              type="text"
              placeholder="按手机号查询"
              @keyup.enter="executeSearch"
            />
          </div>
        </label>

        <label class="field">
          <span class="field__label">邮箱</span>
          <div class="input-shell">
            <span class="input-shell__icon" aria-hidden="true">
              <Mail :size="16" />
            </span>
            <input
              v-model="filters.email"
              class="app-input"
              type="text"
              placeholder="按邮箱查询"
              @keyup.enter="executeSearch"
            />
          </div>
        </label>

        <label class="field">
          <span class="field__label">状态</span>
          <select v-model="filters.status" class="app-select">
            <option value="all">全部状态</option>
            <option value="enabled">启用</option>
            <option value="disabled">禁用</option>
          </select>
        </label>

        <div class="workspace__query-actions">
          <button type="button" class="app-button" :disabled="loading" @click="executeSearch">
            查询
          </button>
          <button type="button" class="app-button app-button--ghost" @click="resetFilters">
            重置
          </button>
        </div>

        <div class="workspace__meta">
          <span class="workspace__summary">{{ resultsSummary }}</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="user-table" :aria-busy="loading">
          <thead>
            <tr>
              <th scope="col">用户</th>
              <th scope="col">状态</th>
              <th scope="col">联系方式</th>
              <th scope="col">租户</th>
              <th scope="col">操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr>
              <td colspan="5" class="table-wrap__loading">正在加载用户列表...</td>
            </tr>
          </tbody>
          <tbody v-else-if="pageState.list.length > 0">
            <tr v-for="user in pageState.list" :key="user.id">
              <td data-label="用户">
                <div class="user-cell">
                  <div class="user-cell__avatar" aria-hidden="true">
                    <CircleUserRound :size="18" />
                  </div>
                  <div class="user-cell__copy">
                    <strong>{{ user.nickname || user.username }}</strong>
                    <p>{{ user.username }} / ID {{ user.id }}</p>
                  </div>
                </div>
              </td>
              <td data-label="状态">
                <StatusBadge :status="user.status" />
              </td>
              <td data-label="联系方式">{{ formatContact(user) }}</td>
              <td data-label="租户">{{ formatTenant(user.tenantId) }}</td>
              <td data-label="操作">
                <div class="table-actions">
                  <button
                    type="button"
                    class="app-button app-button--ghost"
                    @click="openEditDialog(user)"
                  >
                    <UserRoundPen :size="15" aria-hidden="true" />
                    编辑
                  </button>
                  <button
                    type="button"
                    class="app-button app-button--ghost app-button--danger-ghost"
                    @click="requestDelete(user)"
                  >
                    <Trash2 :size="15" aria-hidden="true" />
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td colspan="5">
                <div class="empty-state">
                  <strong>没有匹配的用户</strong>
                  <p>调整查询条件后重新搜索。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="pagination-bar">
        <div class="pagination-bar__info">
          <span>当前页 {{ pageState.pageNum }}</span>
          <span>总页数 {{ Math.max(pageState.pages, 1) }}</span>
        </div>

        <div class="pagination-bar__actions">
          <button
            type="button"
            class="app-button app-button--ghost"
            :disabled="!canGoPrev || loading"
            @click="goToPage(pageState.pageNum - 1)"
          >
            <ChevronLeft :size="16" aria-hidden="true" />
            上一页
          </button>

          <button
            type="button"
            class="app-button app-button--ghost"
            :disabled="!canGoNext || loading"
            @click="goToPage(pageState.pageNum + 1)"
          >
            下一页
            <ChevronRight :size="16" aria-hidden="true" />
          </button>
        </div>
      </footer>
    </section>

    <UserFormDialog
      v-model="dialogOpen"
      :mode="dialogMode"
      :user="selectedUser"
      :submitting="submitting"
      @submit="handleDialogSubmit"
    />

    <ConfirmDialog
      :model-value="deleteTarget !== null"
      title="删除用户"
      :description="deleteDescription"
      confirm-text="确认删除"
      :loading="deletePending"
      @update:model-value="handleDeleteDialogVisibility"
      @confirm="confirmDelete"
    />
  </MainShell>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stats-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px;
}

.stats-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 18px;
  color: var(--color-accent-strong);
  background:
    linear-gradient(135deg, rgba(143, 231, 255, 0.12), rgba(83, 184, 255, 0.08)),
    rgba(255, 255, 255, 0.04);
}

.stats-card__label {
  color: var(--color-ink-muted);
  font-size: 0.78rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.stats-card__value {
  display: block;
  margin-top: 8px;
  color: var(--color-ink-strong);
  font-size: 1.8rem;
  line-height: 1;
}

.stats-card__detail {
  margin-top: 10px;
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.workspace {
  margin-top: 22px;
  padding: 28px;
}

.workspace__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.workspace__headline {
  max-width: 40rem;
}

.workspace__headline h2 {
  margin-top: 10px;
  font-size: 2rem;
}

.workspace__headline p:last-child {
  margin-top: 10px;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.workspace__actions {
  display: flex;
  gap: 12px;
}

.workspace__toolbar {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  align-items: end;
  margin-top: 26px;
  padding: 18px;
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
    rgba(4, 10, 20, 0.58);
}

.workspace__query-actions {
  display: flex;
  gap: 10px;
}

.workspace__meta {
  display: flex;
  align-items: center;
}

.workspace__summary {
  color: var(--color-ink-soft);
  font-size: 0.92rem;
  white-space: nowrap;
}

.app-select {
  min-height: 56px;
  padding: 0 16px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  color: var(--color-ink-strong);
  background: rgba(255, 255, 255, 0.06);
  outline: 0;
}

.app-select option {
  color: #f0f5ff;
  background: #0a1524;
}

.table-wrap {
  margin-top: 22px;
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: 24px;
  background: rgba(7, 12, 22, 0.72);
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th,
.user-table td {
  padding: 18px 16px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.user-table th {
  color: var(--color-ink-muted);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.02);
}

.user-table td {
  color: var(--color-ink-soft);
}

.user-table tbody tr:hover {
  background: rgba(83, 184, 255, 0.05);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-cell__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  color: var(--color-accent-strong);
  background: rgba(255, 255, 255, 0.06);
}

.user-cell__copy strong {
  display: block;
  color: var(--color-ink-strong);
}

.user-cell__copy p {
  margin-top: 4px;
  color: var(--color-ink-muted);
  font-size: 0.84rem;
}

.table-actions {
  display: flex;
  gap: 10px;
}

.table-wrap__loading,
.empty-state {
  padding: 34px 16px;
  text-align: center;
}

.empty-state strong {
  display: block;
  color: var(--color-ink-strong);
  font-size: 1.05rem;
}

.empty-state p {
  margin-top: 8px;
  color: var(--color-ink-soft);
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.pagination-bar__info {
  display: flex;
  gap: 14px;
  color: var(--color-ink-soft);
  font-size: 0.92rem;
}

.pagination-bar__actions {
  display: flex;
  gap: 10px;
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

@media (max-width: 1280px) {
  .workspace__toolbar {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .workspace__query-actions,
  .workspace__meta {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1180px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 800px) {
  .workspace {
    padding: 20px;
  }

  .workspace__header,
  .workspace__actions,
  .table-actions,
  .pagination-bar,
  .pagination-bar__actions,
  .workspace__query-actions {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .stats-grid,
  .workspace__toolbar {
    grid-template-columns: 1fr;
  }

  .workspace__summary,
  .pagination-bar__info {
    white-space: normal;
    flex-direction: column;
    gap: 6px;
  }

  .user-table thead {
    display: none;
  }

  .user-table,
  .user-table tbody,
  .user-table tr,
  .user-table td {
    display: block;
    width: 100%;
  }

  .user-table tr {
    padding: 10px 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  .user-table td {
    border: 0;
    padding: 10px 14px;
  }

  .user-table td::before {
    content: attr(data-label);
    display: block;
    margin-bottom: 6px;
    color: var(--color-ink-muted);
    font-size: 0.75rem;
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }
}
</style>
