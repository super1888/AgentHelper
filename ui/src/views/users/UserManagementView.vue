<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  CircleUserRound,
  Layers3,
  Plus,
  RefreshCw,
  Search,
  SlidersHorizontal,
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
type SearchField = 'all' | 'username' | 'nickname' | 'phone' | 'email' | 'tenantId' | 'id'
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
  field: 'all' as SearchField,
  status: 'all' as FilterStatus,
})

const fieldOptions: Array<{ label: string; value: SearchField }> = [
  { label: '全部字段', value: 'all' },
  { label: '用户名', value: 'username' },
  { label: '昵称', value: 'nickname' },
  { label: '手机号', value: 'phone' },
  { label: '邮箱', value: 'email' },
  { label: '租户 ID', value: 'tenantId' },
  { label: '用户 ID', value: 'id' },
]

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
      detail: '当前目录',
      icon: Users,
    },
    {
      label: '启用账号',
      value: String(enabledCount),
      detail: '可正常登录',
      icon: UserCheck,
    },
    {
      label: '禁用账号',
      value: String(disabledCount),
      detail: '已冻结访问',
      icon: UserX,
    },
    {
      label: '租户分布',
      value: String(tenantCount),
      detail: '已绑定租户',
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

    if (!matchesStatus) {
      return false
    }

    if (!keyword) {
      return true
    }

    const fields: Record<SearchField, string> = {
      all: '',
      username: user.username || '',
      nickname: user.nickname || '',
      phone: user.phone || '',
      email: user.email || '',
      tenantId: user.tenantId !== null ? String(user.tenantId) : '',
      id: String(user.id),
    }

    if (filters.field === 'all') {
      return Object.values(fields)
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(keyword))
    }

    return fields[filters.field].toLowerCase().includes(keyword)
  })
})

const resultsSummary = computed(() => {
  if (loading.value) {
    return '正在同步用户列表...'
  }

  return `筛选结果 ${filteredUsers.value.length} / 全部 ${users.value.length}`
})

const deleteDescription = computed(() =>
  deleteTarget.value ? `确认删除用户 ${deleteTarget.value.username} 吗？该操作不可撤销。` : '',
)

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function resetFilters() {
  filters.query = ''
  filters.field = 'all'
  filters.status = 'all'
}

function formatContact(user: UserProfile) {
  const contactItems = [user.phone, user.email].filter(Boolean)
  return contactItems.length > 0 ? contactItems.join(' / ') : '未填写'
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
        <div class="stats-card__content">
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
          <p>统一查看账号、状态、联系方式与租户归属，支持指定字段筛选。</p>
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

      <div class="workspace__toolbar panel-card">
        <div class="workspace__toolbar-title">
          <div class="workspace__toolbar-icon" aria-hidden="true">
            <SlidersHorizontal :size="18" />
          </div>
          <div>
            <strong>筛选条件</strong>
            <p>按字段、关键字和状态快速定位目标账号。</p>
          </div>
        </div>

        <label class="field workspace__search">
          <span class="field__label">关键字</span>
          <div class="input-shell">
            <span class="input-shell__icon" aria-hidden="true">
              <Search :size="16" />
            </span>
            <input
              v-model="filters.query"
              class="app-input"
              type="search"
              placeholder="输入关键字后，结合指定字段筛选"
            />
          </div>
        </label>

        <label class="field workspace__field">
          <span class="field__label">指定字段</span>
          <select v-model="filters.field" class="app-select">
            <option v-for="option in fieldOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="field workspace__filter">
          <span class="field__label">账号状态</span>
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
                  <p>调整关键字、字段或状态条件后再试。</p>
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
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.stats-card__content {
  min-width: 0;
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
  max-width: 38rem;
}

.workspace__headline h2 {
  margin-top: 10px;
  font-size: 2rem;
  line-height: 1.08;
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
  grid-template-columns: minmax(220px, 0.95fr) minmax(0, 1.45fr) 180px 180px auto;
  gap: 16px;
  align-items: end;
  margin-top: 26px;
  padding: 18px;
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
    rgba(4, 10, 20, 0.58);
}

.workspace__toolbar-title {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 56px;
}

.workspace__toolbar-title strong {
  display: block;
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.workspace__toolbar-title p {
  margin-top: 4px;
  color: var(--color-ink-soft);
  font-size: 0.88rem;
  line-height: 1.55;
}

.workspace__toolbar-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 18px;
  color: var(--color-accent-strong);
  background: rgba(255, 255, 255, 0.05);
}

.workspace__meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  min-height: 56px;
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
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    box-shadow 180ms ease;
}

.app-select:hover {
  border-color: rgba(83, 184, 255, 0.22);
  background: rgba(255, 255, 255, 0.08);
}

.app-select option {
  color: #f0f5ff;
  background: #0a1524;
}

.app-select:focus {
  border-color: rgba(83, 184, 255, 0.42);
  box-shadow: var(--shadow-focus);
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

@media (max-width: 1220px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workspace__toolbar {
    grid-template-columns: 1fr 1fr;
  }

  .workspace__toolbar-title,
  .workspace__meta {
    grid-column: 1 / -1;
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

  .workspace__toolbar {
    grid-template-columns: 1fr;
  }

  .workspace__summary {
    white-space: normal;
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
