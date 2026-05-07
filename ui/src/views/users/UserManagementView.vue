<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ChevronLeft,
  ChevronRight,
  CircleOff,
  Mail,
  Phone,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
  UserRoundPen,
  Users,
} from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import FaceCaptureDialog from '@/components/FaceCaptureDialog.vue'
import MainShell from '@/components/MainShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import UserFormDialog from '@/components/UserFormDialog.vue'
import { faceBind, fetchFaceStatus, faceUnbind } from '@/api/auth'
import { fetchTenantOptions } from '@/api/tenant'
import { createUser, fetchUserStats, queryUsers, removeUser, updateUser } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { TenantOption } from '@/types/tenant'
import type {
  CreateUserPayload,
  UpdateUserPayload,
  UserFaceStatus,
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
const tenantOptionsLoading = ref(false)
const faceStatusLoading = ref(false)
const submitting = ref(false)
const deletePending = ref(false)
const faceSubmitting = ref(false)
const faceUnbindPending = ref(false)
const dialogOpen = ref(false)
const faceDialogOpen = ref(false)
const faceUnbindConfirmOpen = ref(false)
const dialogMode = ref<DialogMode>('create')
const selectedUser = ref<UserProfile | null>(null)
const deleteTarget = ref<UserProfile | null>(null)
const faceStatus = ref<UserFaceStatus | null>(null)
const feedback = ref<FeedbackState | null>(null)
const faceErrorMessage = ref('')
const authStore = useAuthStore()

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

const statistics = ref<UserStatistics>({ totalCount: 0 })
const tenantOptions = ref<TenantOption[]>([])

const currentUserLabel = computed(() => authStore.user?.nickname || authStore.user?.username || '当前登录用户')

const faceStateLabel = computed(() => {
  if (faceStatusLoading.value) {
    return '状态同步中...'
  }
  if (!faceStatus.value?.bound) {
    return '未绑定'
  }
  return faceStatus.value.status || 'ENABLE'
})

const faceStateDescription = computed(() => {
  if (!faceStatus.value?.bound) {
    return '当前账号尚未绑定人脸，绑定后可直接通过摄像头进行登录。'
  }
  return faceStatus.value.lastVerifiedTime
    ? `最近验证时间：${faceStatus.value.lastVerifiedTime}`
    : '已绑定人脸模板，可用于快速登录。'
})

const totalUsersLabel = computed(() => (
  statsLoading.value ? '统计刷新中...' : `总用户数 ${statistics.value.totalCount}`
))

const resultsSummary = computed(() => (
  loading.value
    ? '正在加载用户列表...'
    : `共 ${pageState.value.total} 条，当前第 ${pageState.value.pageNum} / ${Math.max(pageState.value.pages, 1)} 页`
))

const deleteDescription = computed(() => (
  deleteTarget.value ? `确认删除用户“${deleteTarget.value.username}”吗？该操作不可撤销。` : ''
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
  const items = [user.phone, user.email].filter(Boolean)
  return items.length > 0 ? items.join(' / ') : '未填写联系方式'
}

function formatTenant(user: UserProfile) {
  return user.tenantName || (user.tenantId ? `租户 ${user.tenantId}` : '默认租户')
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

async function loadFaceStatus(successMessage?: string) {
  faceStatusLoading.value = true
  try {
    faceStatus.value = await fetchFaceStatus()
    if (successMessage) {
      showFeedback('success', successMessage)
    }
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '人脸状态加载失败。'))
  } finally {
    faceStatusLoading.value = false
  }
}

async function executeSearch() {
  await loadUsers(1)
}

async function refreshCurrentPage() {
  await Promise.all([
    loadUsers(pageState.value.pageNum),
    loadStatistics(),
    loadTenantOptions(),
    loadFaceStatus(),
  ])
}

function openFaceBindDialog() {
  clearFeedback()
  faceErrorMessage.value = ''
  faceDialogOpen.value = true
}

async function handleFaceBindSubmit(payload: {
  imageBase64: string
  imageFormat: string
  deviceId?: string | null
  clientIp?: string | null
  forceReplace?: boolean | null
  silentLogin?: boolean | null
}) {
  faceSubmitting.value = true
  const wasBound = Boolean(faceStatus.value?.bound)
  try {
    await faceBind({
      imageBase64: payload.imageBase64,
      imageFormat: payload.imageFormat,
      deviceId: payload.deviceId ?? null,
      clientIp: payload.clientIp ?? null,
      forceReplace: wasBound,
    })
    faceDialogOpen.value = false
    faceErrorMessage.value = ''
    await loadFaceStatus(wasBound ? '人脸模板已更新。' : '人脸绑定成功。')
  } catch (error) {
    faceErrorMessage.value = getErrorMessage(error, '人脸绑定失败。')
  } finally {
    faceSubmitting.value = false
  }
}

function requestFaceUnbind() {
  faceUnbindConfirmOpen.value = true
}

async function confirmFaceUnbind() {
  faceUnbindPending.value = true
  try {
    await faceUnbind()
    faceUnbindConfirmOpen.value = false
    await loadFaceStatus('人脸绑定已解除。')
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '解除人脸绑定失败。'))
  } finally {
    faceUnbindPending.value = false
  }
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
  if (tenantOptions.value.length === 0 && !tenantOptionsLoading.value) {
    void loadTenantOptions()
  }
  dialogOpen.value = true
}

function openEditDialog(user: UserProfile) {
  clearFeedback()
  dialogMode.value = 'edit'
  selectedUser.value = user
  if (tenantOptions.value.length === 0 && !tenantOptionsLoading.value) {
    void loadTenantOptions()
  }
  dialogOpen.value = true
}

async function handleDialogSubmit(event: { mode: DialogMode; payload: CreateUserPayload | UpdateUserPayload }) {
  submitting.value = true
  try {
    if (event.mode === 'create') {
      await createUser(event.payload as CreateUserPayload)
      dialogOpen.value = false
      await Promise.all([
        loadUsers(1, '用户已创建。'),
        loadStatistics(),
      ])
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
    await removeUser(deleteTarget.value.id)
    const deletedName = deleteTarget.value.username
    const nextPage = pageState.value.list.length === 1 && pageState.value.pageNum > 1
      ? pageState.value.pageNum - 1
      : pageState.value.pageNum
    deleteTarget.value = null
    await Promise.all([
      loadUsers(nextPage, `用户 ${deletedName} 已删除。`),
      loadStatistics(),
    ])
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除用户失败。'))
  } finally {
    deletePending.value = false
  }
}

onMounted(() => {
  void Promise.all([loadUsers(), loadStatistics(), loadTenantOptions(), loadFaceStatus()])
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

    <FaceCaptureDialog
      v-model="faceDialogOpen"
      mode="bind"
      title="绑定人脸"
      description="采集当前登录账号的人脸模板，后续可直接通过摄像头登录。"
      :submitting="faceSubmitting"
      :force-replace="Boolean(faceStatus?.bound)"
      :error-message="faceErrorMessage"
      @submit="handleFaceBindSubmit"
    />

    <section class="management-page">
      <article class="panel-card management-hero">
        <div>
          <p class="section-kicker">User Center</p>
          <h2>用户管理</h2>
          <p class="management-hero__meta">{{ totalUsersLabel }}</p>
        </div>
        <div class="management-hero__actions">
          <button class="app-button app-button--secondary" :disabled="loading || statsLoading" @click="refreshCurrentPage">
            <RefreshCw :size="16" />
            刷新
          </button>
          <button class="app-button" @click="openCreateDialog">
            <Plus :size="16" />
            新建用户
          </button>
        </div>
      </article>

      <section class="management-stats">
        <article class="panel-card management-stat">
          <span>总用户数</span>
          <strong>{{ statistics.totalCount }}</strong>
        </article>
        <article class="panel-card management-stat">
          <span>当前页数量</span>
          <strong>{{ pageState.list.length }}</strong>
        </article>
        <article class="panel-card management-stat">
          <span>可选租户</span>
          <strong>{{ tenantOptions.length }}</strong>
        </article>
      </section>

      <article class="panel-card face-auth-card">
        <div class="face-auth-card__copy">
          <p class="section-kicker">Face Access</p>
          <h3>{{ currentUserLabel }}</h3>
          <p>{{ faceStateDescription }}</p>
        </div>

        <div class="face-auth-card__status">
          <div class="face-auth-card__badge" :class="{ 'face-auth-card__badge--bound': faceStatus?.bound }">
            <ShieldCheck :size="16" />
            <span>{{ faceStateLabel }}</span>
          </div>
          <span class="face-auth-card__template">模板 {{ faceStatus?.faceTemplateCode || '--' }}</span>
        </div>

        <div class="face-auth-card__actions">
          <button class="app-button" :disabled="faceStatusLoading" @click="openFaceBindDialog">
            <ShieldCheck :size="16" />
            {{ faceStatus?.bound ? '重新采集' : '绑定人脸' }}
          </button>
          <button class="app-button app-button--secondary" :disabled="faceStatusLoading" @click="loadFaceStatus()">
            <RefreshCw :size="16" />
            刷新状态
          </button>
          <button
            class="app-button app-button--danger"
            :disabled="!faceStatus?.bound || faceStatusLoading"
            @click="requestFaceUnbind"
          >
            <CircleOff :size="16" />
            解除绑定
          </button>
        </div>
      </article>

      <article class="panel-card management-panel">
        <div class="management-head">
          <div>
            <strong>筛选条件</strong>
            <p>{{ resultsSummary }}</p>
          </div>
          <button class="app-button app-button--secondary" @click="resetFilters">重置</button>
        </div>

        <div class="management-filter-grid management-filter-grid--wide">
          <label class="field">
            <span class="field__label">用户名</span>
            <div class="input-shell">
              <span class="input-shell__icon"><Search :size="16" /></span>
              <input v-model="filters.username" class="app-input" type="text" placeholder="按用户名搜索" />
            </div>
          </label>

          <label class="field">
            <span class="field__label">昵称</span>
            <div class="input-shell">
              <input v-model="filters.nickname" class="app-input" type="text" placeholder="按昵称搜索" />
            </div>
          </label>

          <label class="field">
            <span class="field__label">手机号</span>
            <div class="input-shell">
              <span class="input-shell__icon"><Phone :size="16" /></span>
              <input v-model="filters.phone" class="app-input" type="text" placeholder="按手机号搜索" />
            </div>
          </label>

          <label class="field">
            <span class="field__label">邮箱</span>
            <div class="input-shell">
              <span class="input-shell__icon"><Mail :size="16" /></span>
              <input v-model="filters.email" class="app-input" type="text" placeholder="按邮箱搜索" />
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
        <div v-if="loading" class="management-empty">正在加载用户列表...</div>
        <div v-else-if="pageState.list.length === 0" class="management-empty">
          当前没有匹配的用户数据，请调整筛选条件或新建用户。
        </div>
        <div v-else class="management-list">
          <article v-for="user in pageState.list" :key="user.id" class="management-card user-card">
            <div class="management-card__head">
              <div>
                <strong>{{ user.username }}</strong>
                <p>{{ user.nickname || '未设置昵称' }}</p>
              </div>
              <StatusBadge :status="user.status" />
            </div>

            <div class="management-card__meta">
              <span><Users :size="14" />{{ formatTenant(user) }}</span>
              <span>{{ formatContact(user) }}</span>
            </div>

            <div class="management-card__actions">
              <button class="app-button app-button--secondary" @click="openEditDialog(user)">
                <UserRoundPen :size="16" />
                编辑
              </button>
              <button class="app-button app-button--danger" @click="requestDelete(user)">
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

    <UserFormDialog
      v-model="dialogOpen"
      :mode="dialogMode"
      :user="selectedUser"
      :tenant-options="tenantOptions"
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

    <ConfirmDialog
      :model-value="faceUnbindConfirmOpen"
      title="解除人脸绑定"
      description="解除后该账号将无法继续使用人脸登录，是否继续？"
      confirm-text="确认解除"
      :loading="faceUnbindPending"
      @update:model-value="faceUnbindConfirmOpen = $event"
      @confirm="confirmFaceUnbind"
    />
  </MainShell>
</template>

<style scoped>
.user-card .management-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.face-auth-card {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) auto;
  gap: 18px;
  align-items: center;
  padding: var(--panel-padding);
}

.face-auth-card__copy {
  display: grid;
  gap: 8px;
}

.face-auth-card__copy h3 {
  font-size: 1.4rem;
}

.face-auth-card__copy p:last-child {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.face-auth-card__status {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.face-auth-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.04);
}

.face-auth-card__badge--bound {
  color: #d8fff3;
  border-color: rgba(100, 216, 190, 0.24);
  background: rgba(100, 216, 190, 0.12);
}

.face-auth-card__template {
  color: var(--color-ink-muted);
  font-size: 0.84rem;
}

.face-auth-card__actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .face-auth-card {
    grid-template-columns: 1fr;
  }

  .face-auth-card__status {
    justify-items: start;
  }
}
</style>
