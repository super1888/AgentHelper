<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ChevronLeft,
  ChevronRight,
  CircleUserRound,
  Mail,
  Phone,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  UserRound,
  UserRoundPen,
  Users,
} from 'lucide-vue-next'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MainShell from '@/components/MainShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import UserFormDialog from '@/components/UserFormDialog.vue'
import { createUser, fetchUserStats, queryUsers, removeUser, updateUser } from '@/api/user'
import type {
  CreateUserPayload,
  UpdateUserPayload,
  UserPageResult,
  UserProfile,
  UserStatistics,
} from '@/types/user'
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
})

const totalUsersLabel = computed(() => {
  if (statsLoading.value) {
    return '统计中...'
  }
  return `总用户 ${statistics.value.totalCount}`
})

const resultsSummary = computed(() => {
  if (loading.value) {
    return '正在加载用户数据...'
  }

  const pages = Math.max(pageState.value.pages, 1)
  return `共 ${pageState.value.total} 条，当前第 ${pageState.value.pageNum} / ${pages} 页，每页 ${pageState.value.pageSize} 条`
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
  void loadUsers(1)
}

function formatContact(user: UserProfile) {
  const contactItems = [user.phone, user.email].filter(Boolean)
  return contactItems.length > 0 ? contactItems.join(' / ') : '未填写'
}

function formatTenant(tenantId: string | null) {
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
    statistics.value = await fetchUserStats()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '总用户统计加载失败。'))
  } finally {
    statsLoading.value = false
  }
}

async function executeSearch() {
  await loadUsers(1)
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
    await loadUsers(pageState.value.pageNum, '用户信息已更新。')
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
      <header class="workspace__hero">
        <div class="workspace__headline">
          <p class="section-kicker">User Center</p>
          <div class="workspace__title-row">
            <h2>用户管理</h2>
            <span class="workspace__count-badge">
              <Users :size="15" aria-hidden="true" />
              {{ totalUsersLabel }}
            </span>
          </div>
          <p class="workspace__subtitle">统一管理账号、状态与租户信息，支持分页与条件检索。</p>
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

      <section class="workspace__filters panel-card">
        <div class="workspace__filters-head">
          <div>
            <strong>条件筛选</strong>
            <p>按字段组合查询当前页数据，总用户数始终展示系统总量。</p>
          </div>
          <span class="workspace__summary">{{ resultsSummary }}</span>
        </div>

        <div class="workspace__filters-grid">
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
        </div>

        <div class="workspace__filters-actions">
          <button type="button" class="app-button" :disabled="loading" @click="executeSearch">
            查询
          </button>
          <button type="button" class="app-button app-button--ghost" @click="resetFilters">
            重置
          </button>
        </div>
      </section>

      <section class="workspace__table panel-card">
        <div class="workspace__table-head">
          <strong>用户列表</strong>
          <span>默认按修改时间倒序展示</span>
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
                    <p>调整筛选条件后重新查询。</p>
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
.workspace {
  padding: 30px;
}

.workspace__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 28px;
}

.workspace__headline {
  min-width: 0;
  max-width: 42rem;
}

.workspace__title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 12px;
}

.workspace__headline h2 {
  font-size: clamp(2rem, 2.6vw, 2.5rem);
  line-height: 1.04;
  letter-spacing: -0.03em;
}

.workspace__count-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 16px;
  color: #d8f7ff;
  border: 1px solid rgba(116, 210, 255, 0.22);
  border-radius: 999px;
  background:
    linear-gradient(135deg, rgba(116, 210, 255, 0.16), rgba(116, 210, 255, 0.06)),
    rgba(255, 255, 255, 0.03);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
  font-size: 0.9rem;
  font-weight: 700;
  white-space: nowrap;
}

.workspace__subtitle {
  margin-top: 14px;
  color: var(--color-ink-soft);
  font-size: 0.96rem;
  line-height: 1.7;
  text-wrap: balance;
}

.workspace__actions {
  display: flex;
  gap: 12px;
}

.workspace__filters,
.workspace__table {
  padding: 22px;
  border-radius: 26px;
}

.workspace__filters {
  background:
    radial-gradient(circle at top right, rgba(83, 184, 255, 0.08), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.018)),
    rgba(4, 10, 20, 0.54);
}

.workspace__filters-head,
.workspace__table-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.workspace__filters-head strong,
.workspace__table-head strong {
  display: block;
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.workspace__filters-head p,
.workspace__table-head span,
.workspace__summary {
  color: var(--color-ink-soft);
  font-size: 0.9rem;
}

.workspace__filters-head p {
  margin-top: 6px;
}

.workspace__filters-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.workspace__filters-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.app-select {
  min-height: 56px;
  padding: 0 16px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  color: var(--color-ink-strong);
  background: rgba(255, 255, 255, 0.06);
  outline: 0;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    box-shadow 180ms ease;
}

.app-select:hover {
  border-color: rgba(83, 184, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
}

.app-select:focus {
  border-color: rgba(77, 179, 255, 0.46);
  box-shadow: var(--shadow-focus);
}

.app-select option {
  color: #f0f5ff;
  background: #0a1524;
}

.workspace__table {
  margin-top: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.016)),
    rgba(6, 12, 24, 0.72);
}

.table-wrap {
  margin-top: 18px;
  overflow-x: auto;
  border: 1px solid rgba(150, 181, 255, 0.1);
  border-radius: 22px;
  background: rgba(5, 10, 18, 0.68);
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

.user-table tbody tr {
  transition: background-color 180ms ease;
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
  background:
    linear-gradient(135deg, rgba(83, 184, 255, 0.14), rgba(83, 184, 255, 0.04)),
    rgba(255, 255, 255, 0.04);
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
  padding: 36px 16px;
  text-align: center;
}

.empty-state strong {
  display: block;
  color: var(--color-ink-strong);
  font-size: 1.04rem;
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

.pagination-bar__info,
.pagination-bar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination-bar__info {
  color: var(--color-ink-soft);
  font-size: 0.92rem;
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
  .workspace__filters-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .workspace {
    padding: 22px;
  }

  .workspace__hero,
  .workspace__actions,
  .workspace__filters-head,
  .workspace__filters-actions,
  .table-actions,
  .pagination-bar,
  .pagination-bar__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .workspace__summary {
    text-align: left;
  }
}

@media (max-width: 720px) {
  .workspace__filters-grid {
    grid-template-columns: 1fr;
  }

  .pagination-bar__info {
    flex-direction: column;
    align-items: flex-start;
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
