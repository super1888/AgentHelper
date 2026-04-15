<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  CircleUserRound,
  Layers3,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  UserCheck,
  UserRoundPen,
  UserX,
  Users,
} from 'lucide-vue-next'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MainShell from '@/components/MainShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import UserFormDialog from '@/components/UserFormDialog.vue'
import { createUser, fetchUsers, removeUser, updateUser } from '@/api/user'
import type { CreateUserPayload, UpdateUserPayload, UserProfile } from '@/types/user'
import { getErrorMessage } from '@/utils/errors'

type FilterStatus = 'all' | 'enabled' | 'disabled'
type DialogMode = 'create' | 'edit'
type FeedbackTone = 'success' | 'error' | 'info'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

const users = ref<UserProfile[]>([])
const loading = ref(false)
const submitting = ref(false)
const deletePending = ref(false)
const dialogOpen = ref(false)
const dialogMode = ref<DialogMode>('create')
const selectedUser = ref<UserProfile | null>(null)
const deleteTarget = ref<UserProfile | null>(null)
const feedback = ref<FeedbackState | null>(null)

const filters = reactive({
  query: '',
  status: 'all' as FilterStatus,
})

const statCards = computed(() => {
  const total = users.value.length
  const enabledCount = users.value.filter((user) => user.status === 1).length
  const disabledCount = total - enabledCount
  const tenantCount = new Set(
    users.value
      .filter((user) => user.tenantId !== null)
      .map((user) => user.tenantId),
  ).size

  return [
    {
      label: '用户总数',
      value: String(total),
      detail: '当前系统中的全部账号数量。',
      icon: Users,
    },
    {
      label: '启用账号',
      value: String(enabledCount),
      detail: '可以正常登录并访问受保护资源的账号。',
      icon: UserCheck,
    },
    {
      label: '禁用账号',
      value: String(disabledCount),
      detail: '状态为 0 的账号，后端登录时会直接拒绝。',
      icon: UserX,
    },
    {
      label: '租户分布',
      value: String(tenantCount),
      detail: '已设置租户 ID 的不同租户数量。',
      icon: Layers3,
    },
  ]
})

const filteredUsers = computed(() => {
  const keyword = filters.query.trim().toLowerCase()

  return users.value.filter((user) => {
    const matchesStatus =
      filters.status === 'all' ||
      (filters.status === 'enabled' && user.status === 1) ||
      (filters.status === 'disabled' && user.status === 0)

    const matchesKeyword =
      !keyword ||
      [
        user.username,
        user.nickname,
        user.email,
        user.phone,
        String(user.id),
        user.tenantId !== null ? String(user.tenantId) : '',
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))

    return matchesStatus && matchesKeyword
  })
})

const resultsSummary = computed(() => {
  if (loading.value) {
    return '正在同步用户列表...'
  }

  return `共 ${filteredUsers.value.length} 条结果`
})

const deleteDescription = computed(() =>
  deleteTarget.value ? `确认删除用户 ${deleteTarget.value.username} 吗？该操作无法撤销。` : '',
)

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function resetFilters() {
  filters.query = ''
  filters.status = 'all'
}

function formatContact(user: UserProfile) {
  const contactItems = [user.phone, user.email].filter(Boolean)
  return contactItems.length > 0 ? contactItems.join(' / ') : '暂无联系方式'
}

function formatTenant(tenantId: number | null) {
  return tenantId === null ? '默认租户' : `租户 ${tenantId}`
}

async function loadUsers(successMessage?: string) {
  loading.value = true

  try {
    users.value = await fetchUsers()
    if (successMessage) {
      showFeedback('success', successMessage)
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '用户列表加载失败。'))
  } finally {
    loading.value = false
  }
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
      await loadUsers('用户已创建。')
      return
    }

    if (!selectedUser.value) {
      throw new Error('缺少待编辑的用户信息。')
    }

    await updateUser(selectedUser.value.id, event.payload as UpdateUserPayload)
    dialogOpen.value = false
    await loadUsers('用户信息已更新。')
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
    deleteTarget.value = null
    await loadUsers(`用户 ${deletedUsername} 已删除。`)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除用户失败。'))
  } finally {
    deletePending.value = false
  }
}

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <MainShell>
    <section class="stats-grid">
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
        知道了
      </button>
    </section>

    <section class="workspace panel-card">
      <header class="workspace__header">
        <div>
          <p class="section-kicker">User Management</p>
          <h2>用户管理</h2>
          <p>支持搜索、状态筛选、新增、编辑和删除，所有操作都直接调用后端用户接口。</p>
        </div>

        <div class="workspace__actions">
          <button
            type="button"
            class="app-button app-button--secondary"
            :disabled="loading"
            @click="loadUsers()"
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

      <div class="workspace__toolbar">
        <label class="field workspace__search">
          <span class="field__label">搜索用户</span>
          <div class="input-shell">
            <span class="input-shell__icon" aria-hidden="true">
              <Search :size="16" />
            </span>
            <input
              v-model="filters.query"
              class="app-input"
              type="search"
              placeholder="按用户名、昵称、邮箱、手机号、ID 或租户搜索"
            />
          </div>
        </label>

        <label class="field workspace__filter">
          <span class="field__label">状态筛选</span>
          <select v-model="filters.status" class="app-select">
            <option value="all">全部状态</option>
            <option value="enabled">仅启用</option>
            <option value="disabled">仅禁用</option>
          </select>
        </label>

        <div class="workspace__meta">
          <span class="workspace__summary">{{ resultsSummary }}</span>
          <button type="button" class="app-button app-button--ghost" @click="resetFilters">
            重置筛选
          </button>
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
          <tbody v-else-if="filteredUsers.length > 0">
            <tr v-for="user in filteredUsers" :key="user.id">
              <td data-label="用户">
                <div class="user-cell">
                  <div class="user-cell__avatar" aria-hidden="true">
                    <CircleUserRound :size="18" />
                  </div>
                  <div>
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
                  <p>试试调整关键字或状态筛选，或者直接新增一个用户。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
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
  gap: 16px;
  padding: 22px;
}

.stats-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  color: var(--color-accent-strong);
  background: rgba(255, 255, 255, 0.06);
}

.stats-card__label {
  color: var(--color-ink-muted);
  font-size: 0.82rem;
}

.stats-card__value {
  display: block;
  margin-top: 8px;
  color: var(--color-ink-strong);
  font-size: 1.8rem;
}

.stats-card__detail {
  margin-top: 10px;
  color: var(--color-ink-soft);
  line-height: 1.65;
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

.workspace__header h2 {
  margin-top: 10px;
  font-size: 2rem;
}

.workspace__header p:last-child {
  margin-top: 10px;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.workspace__actions {
  display: flex;
  gap: 12px;
}

.workspace__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) 220px auto;
  gap: 16px;
  align-items: end;
  margin-top: 26px;
}

.workspace__meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.workspace__summary {
  color: var(--color-ink-soft);
  font-size: 0.92rem;
}

.app-select {
  min-height: 56px;
  padding: 0 16px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  color: var(--color-ink-strong);
  background: rgba(255, 255, 255, 0.05);
  outline: 0;
}

.app-select:focus {
  border-color: rgba(243, 201, 145, 0.42);
  box-shadow: var(--shadow-focus);
}

.table-wrap {
  margin-top: 22px;
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: 24px;
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
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.user-table td {
  color: var(--color-ink-soft);
}

.user-table tbody tr:hover {
  background: rgba(255, 255, 255, 0.035);
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

.user-cell strong {
  color: var(--color-ink-strong);
}

.user-cell p {
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
  padding: 28px 16px;
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

@media (max-width: 1120px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workspace__toolbar {
    grid-template-columns: 1fr;
  }

  .workspace__meta {
    justify-content: flex-start;
  }
}

@media (max-width: 800px) {
  .workspace {
    padding: 20px;
  }

  .workspace__header {
    flex-direction: column;
  }

  .workspace__actions {
    width: 100%;
    flex-direction: column;
  }

  .table-actions {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
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
