<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ChevronLeft, ChevronRight, Mail, Phone, Plus, RefreshCw, Search, Trash2, UserRoundPen, Users } from 'lucide-vue-next'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MainShell from '@/components/MainShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import UserFormDialog from '@/components/UserFormDialog.vue'
import { fetchTenantOptions } from '@/api/tenant'
import { createUser, fetchUserStats, queryUsers, removeUser, updateUser } from '@/api/user'
import type { TenantOption } from '@/types/tenant'
import type { CreateUserPayload, UpdateUserPayload, UserPageResult, UserProfile, UserStatistics } from '@/types/user'
import { getErrorMessage } from '@/utils/errors'

type FilterStatus = 'all' | 'enabled' | 'disabled'
type DialogMode = 'create' | 'edit'
type FeedbackTone = 'success' | 'error' | 'info'
interface FeedbackState { tone: FeedbackTone; message: string }

const loading = ref(false)
const statsLoading = ref(false)
const tenantOptionsLoading = ref(false)
const submitting = ref(false)
const deletePending = ref(false)
const dialogOpen = ref(false)
const dialogMode = ref<DialogMode>('create')
const selectedUser = ref<UserProfile | null>(null)
const deleteTarget = ref<UserProfile | null>(null)
const feedback = ref<FeedbackState | null>(null)

const filters = reactive({ username: '', nickname: '', phone: '', email: '', status: 'all' as FilterStatus })
const pageState = ref<UserPageResult>({ list: [], total: 0, pageNum: 1, pageSize: 20, pages: 0 })
const statistics = ref<UserStatistics>({ totalCount: 0 })
const tenantOptions = ref<TenantOption[]>([])

const totalUsersLabel = computed(() => (statsLoading.value ? '统计中...' : `总用户 ${statistics.value.totalCount}`))
const resultsSummary = computed(() => loading.value ? '正在加载用户数据...' : `共 ${pageState.value.total} 条，当前第 ${pageState.value.pageNum} / ${Math.max(pageState.value.pages, 1)} 页`)
const deleteDescription = computed(() => deleteTarget.value ? `确认删除用户 ${deleteTarget.value.username} 吗？该操作不可撤销。` : '')
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
  return { pageNum, pageSize: 20, username: normalizeText(filters.username), nickname: normalizeText(filters.nickname), phone: normalizeText(filters.phone), email: normalizeText(filters.email), status: normalizeStatus() }
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
  const items = [user.phone, user.email].filter(Boolean)
  return items.length > 0 ? items.join(' / ') : '未填写'
}

function formatTenant(user: UserProfile) {
  return user.tenantName || (user.tenantId ? `租户 ${user.tenantId}` : '默认租户')
}

async function loadUsers(pageNum = 1, successMessage?: string) {
  loading.value = true
  try {
    pageState.value = await queryUsers(buildQuery(pageNum))
    if (successMessage) showFeedback('success', successMessage)
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
    showFeedback('error', getErrorMessage(error, '用户统计加载失败。'))
  } finally {
    statsLoading.value = false
  }
}

async function loadTenantOptions() {
  tenantOptionsLoading.value = true
  try {
    tenantOptions.value = await fetchTenantOptions()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '租户选项加载失败。'))
  } finally {
    tenantOptionsLoading.value = false
  }
}

async function executeSearch() {
  await loadUsers(1)
}

async function refreshCurrentPage() {
  await Promise.all([loadUsers(pageState.value.pageNum), loadStatistics(), loadTenantOptions()])
}

async function goToPage(pageNum: number) {
  if (pageNum < 1 || pageNum > Math.max(pageState.value.pages, 1) || pageNum === pageState.value.pageNum) return
  await loadUsers(pageNum)
}

function openCreateDialog() {
  clearFeedback()
  dialogMode.value = 'create'
  selectedUser.value = null
  if (tenantOptions.value.length === 0 && !tenantOptionsLoading.value) void loadTenantOptions()
  dialogOpen.value = true
}

function openEditDialog(user: UserProfile) {
  clearFeedback()
  dialogMode.value = 'edit'
  selectedUser.value = user
  if (tenantOptions.value.length === 0 && !tenantOptionsLoading.value) void loadTenantOptions()
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
    if (!selectedUser.value) throw new Error('缺少待编辑的用户信息。')
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

function handleDeleteDialogVisibility(visible: boolean) {
  if (!visible) deleteTarget.value = null
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  deletePending.value = true
  try {
    await removeUser(deleteTarget.value.id)
    const deletedName = deleteTarget.value.username
    const nextPage = pageState.value.list.length === 1 && pageState.value.pageNum > 1 ? pageState.value.pageNum - 1 : pageState.value.pageNum
    deleteTarget.value = null
    await Promise.all([loadUsers(nextPage, `用户 ${deletedName} 已删除。`), loadStatistics()])
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除用户失败。'))
  } finally {
    deletePending.value = false
  }
}

onMounted(() => { void Promise.all([loadUsers(), loadStatistics(), loadTenantOptions()]) })
</script>

<template>
  <MainShell>
    <section v-if="feedback" class="feedback-banner" :class="`feedback-banner--${feedback.tone}`">
      <span>{{ feedback.message }}</span>
      <button type="button" class="app-button app-button--secondary" @click="clearFeedback">关闭</button>
    </section>

    <section class="page-grid">
      <article class="panel-card page-hero">
        <div>
          <p class="section-kicker">User Center</p>
          <h2>用户管理</h2>
          <p class="page-hero__meta">{{ totalUsersLabel }}</p>
        </div>
        <div class="page-hero__actions">
          <button class="app-button app-button--secondary" :disabled="loading || statsLoading" @click="refreshCurrentPage"><RefreshCw :size="16" />刷新</button>
          <button class="app-button" @click="openCreateDialog"><Plus :size="16" />新增用户</button>
        </div>
      </article>

      <article class="panel-card panel-block">
        <div class="panel-block__head">
          <div><strong>筛选条件</strong><p>{{ resultsSummary }}</p></div>
          <button class="app-button app-button--secondary" @click="resetFilters">重置</button>
        </div>

        <div class="filter-grid">
          <label class="field">
            <span class="field__label">用户名</span>
            <div class="input-shell"><span class="input-shell__icon"><Search :size="16" /></span><input v-model="filters.username" class="app-input" type="text" placeholder="按用户名搜索" /></div>
          </label>
          <label class="field">
            <span class="field__label">昵称</span>
            <div class="input-shell"><input v-model="filters.nickname" class="app-input" type="text" placeholder="按昵称搜索" /></div>
          </label>
          <label class="field">
            <span class="field__label">手机号</span>
            <div class="input-shell"><span class="input-shell__icon"><Phone :size="16" /></span><input v-model="filters.phone" class="app-input" type="text" placeholder="按手机号搜索" /></div>
          </label>
          <label class="field">
            <span class="field__label">邮箱</span>
            <div class="input-shell"><span class="input-shell__icon"><Mail :size="16" /></span><input v-model="filters.email" class="app-input" type="text" placeholder="按邮箱搜索" /></div>
          </label>
          <label class="field">
            <span class="field__label">状态</span>
            <select v-model="filters.status" class="app-select">
              <option value="all">全部</option>
              <option value="enabled">启用</option>
              <option value="disabled">禁用</option>
            </select>
          </label>
          <button class="app-button filter-grid__submit" :disabled="loading" @click="executeSearch">执行搜索</button>
        </div>
      </article>

      <article class="panel-card panel-block">
        <div v-if="loading" class="empty-state">正在加载用户列表...</div>
        <div v-else-if="pageState.list.length === 0" class="empty-state">当前没有用户数据，请调整筛选条件或新建用户。</div>
        <div v-else class="user-list">
          <article v-for="user in pageState.list" :key="user.id" class="user-card">
            <div class="user-card__head">
              <div>
                <strong>{{ user.username }}</strong>
                <p>{{ user.nickname || '未设置昵称' }}</p>
              </div>
              <StatusBadge :status="user.status" />
            </div>
            <div class="user-card__meta">
              <span><Users :size="14" />{{ formatTenant(user) }}</span>
              <span>{{ formatContact(user) }}</span>
            </div>
            <div class="user-card__actions">
              <button class="app-button app-button--secondary" @click="openEditDialog(user)"><UserRoundPen :size="16" />编辑</button>
              <button class="app-button app-button--danger" @click="requestDelete(user)"><Trash2 :size="16" />删除</button>
            </div>
          </article>
        </div>

        <div class="pager">
          <button class="app-button app-button--secondary" :disabled="!canGoPrev || loading" @click="goToPage(pageState.pageNum - 1)"><ChevronLeft :size="16" />上一页</button>
          <span class="pager__summary">第 {{ pageState.pageNum }} / {{ Math.max(pageState.pages, 1) }} 页</span>
          <button class="app-button app-button--secondary" :disabled="!canGoNext || loading" @click="goToPage(pageState.pageNum + 1)">下一页<ChevronRight :size="16" /></button>
        </div>
      </article>
    </section>

    <UserFormDialog v-model="dialogOpen" :mode="dialogMode" :user="selectedUser" :tenant-options="tenantOptions" :submitting="submitting" @submit="handleDialogSubmit" />
    <ConfirmDialog :model-value="deleteTarget !== null" title="删除用户" :description="deleteDescription" confirm-text="确认删除" :loading="deletePending" @update:model-value="handleDeleteDialogVisibility" @confirm="confirmDelete" />
  </MainShell>
</template>

<style scoped>
.page-grid,.filter-grid { display: grid; gap: 18px; }
.page-hero,.panel-block { padding: 26px; }
.page-hero,.page-hero__actions,.panel-block__head,.user-card__head,.user-card__actions,.pager,.user-card__meta { display: flex; gap: 12px; }
.page-hero,.panel-block__head,.user-card__head,.pager { justify-content: space-between; }
.page-hero { align-items: flex-start; }
.page-hero__actions,.user-card__actions,.user-card__meta { flex-wrap: wrap; }
.page-hero__meta,.panel-block__head p,.user-card__head p { color: var(--color-ink-soft); }
.filter-grid { grid-template-columns: repeat(5, minmax(0, 1fr)) 180px; align-items: end; }
.filter-grid__submit { width: 100%; }
.user-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.user-card { padding: 18px; border-radius: 24px; background: rgba(255,255,255,.04); box-shadow: inset 0 0 0 1px rgba(255,255,255,.06); }
.user-card__meta { margin: 14px 0 16px; font-size: .9rem; color: var(--color-ink-muted); }
.user-card__meta span { display: inline-flex; align-items: center; gap: 6px; }
.pager { align-items: center; margin-top: 18px; }
.pager__summary { color: var(--color-ink-soft); }
.empty-state { display: grid; place-items: center; min-height: 220px; color: var(--color-ink-soft); text-align: center; }
@media (max-width: 1100px) { .filter-grid,.user-list { grid-template-columns: 1fr; } .page-hero,.panel-block__head,.pager { flex-direction: column; align-items: stretch; } }
</style>
