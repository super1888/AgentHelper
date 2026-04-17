<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { CheckCircle2, CircleOff, Mail, Phone, ShieldCheck, UserRound } from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'
import type { TenantOption } from '@/types/tenant'
import type { CreateUserPayload, UpdateUserPayload, UserProfile } from '@/types/user'
import { normalizeOptionalText, parseTenantIdInput, validateCreateUserForm, validateUpdateUserForm, type UserCreateFormState, type UserUpdateFormState } from '@/utils/validation'

type DialogMode = 'create' | 'edit'
interface UserDialogFormState extends UserCreateFormState { username: string }
type UserDialogField = keyof UserDialogFormState

const props = defineProps<{
  modelValue: boolean
  mode: DialogMode
  user: UserProfile | null
  tenantOptions: TenantOption[]
  submitting: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  submit: [{ mode: DialogMode; payload: CreateUserPayload | UpdateUserPayload }]
}>()

const form = reactive<UserDialogFormState>(createEmptyForm())
const errors = reactive<Partial<Record<UserDialogField, string>>>({})

const dialogTitle = computed(() => (props.mode === 'create' ? '新建用户' : '编辑用户'))
const dialogDescription = computed(() => (
  props.mode === 'create'
    ? '录入账号基础信息、状态和所属租户，提交后直接写入用户中心。'
    : '维护显示名称、联系方式、租户和状态，用户名保持只读。'
))

function createEmptyForm(): UserDialogFormState {
  return { username: '', nickname: '', phone: '', email: '', password: '', confirmPassword: '', status: 1, tenantId: '' }
}

function clearErrors() {
  for (const key of Object.keys(errors) as UserDialogField[]) delete errors[key]
}

function syncFormWithProps() {
  clearErrors()
  if (props.mode === 'edit' && props.user) {
    form.username = props.user.username
    form.nickname = props.user.nickname ?? ''
    form.phone = props.user.phone ?? ''
    form.email = props.user.email ?? ''
    form.password = ''
    form.confirmPassword = ''
    form.status = props.user.status
    form.tenantId = props.user.tenantId ? String(props.user.tenantId) : ''
    return
  }
  Object.assign(form, createEmptyForm())
}

function validateForm() {
  clearErrors()
  const nextErrors = props.mode === 'create' ? validateCreateUserForm(form) : validateUpdateUserForm(form as UserUpdateFormState)
  Object.assign(errors, nextErrors)
  return Object.keys(nextErrors).length === 0
}

function handleSubmit() {
  if (!validateForm()) return
  if (props.mode === 'create') {
    const payload: CreateUserPayload = {
      username: form.username.trim(),
      nickname: normalizeOptionalText(form.nickname),
      phone: normalizeOptionalText(form.phone),
      email: normalizeOptionalText(form.email),
      password: form.password.trim(),
      confirmPassword: form.confirmPassword.trim(),
      status: form.status,
      tenantId: parseTenantIdInput(form.tenantId),
    }
    emit('submit', { mode: 'create', payload })
    return
  }
  const payload: UpdateUserPayload = {
    nickname: normalizeOptionalText(form.nickname),
    phone: normalizeOptionalText(form.phone),
    email: normalizeOptionalText(form.email),
    status: form.status,
    tenantId: parseTenantIdInput(form.tenantId),
  }
  emit('submit', { mode: 'edit', payload })
}

watch(
  () => [props.modelValue, props.mode, props.user] as const,
  ([visible]) => {
    if (visible) syncFormWithProps()
  },
  { immediate: true },
)
</script>

<template>
  <AppDialog :model-value="modelValue" :title="dialogTitle" :description="dialogDescription" width="wide" @update:model-value="emit('update:modelValue', $event)">
    <form class="user-form" @submit.prevent="handleSubmit">
      <div v-if="mode === 'edit' && user" class="identity-grid">
        <article class="identity-card">
          <span class="identity-card__label">用户名</span>
          <strong>{{ user.username }}</strong>
        </article>
        <article class="identity-card">
          <span class="identity-card__label">用户 ID</span>
          <strong>#{{ user.id }}</strong>
        </article>
      </div>

      <div class="form-grid">
        <label v-if="mode === 'create'" class="field">
          <span class="field__label">用户名</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.username) }">
            <span class="input-shell__icon"><UserRound :size="16" /></span>
            <input v-model="form.username" class="app-input" type="text" autocomplete="username" :disabled="submitting" placeholder="请输入登录用户名" @input="delete errors.username" />
          </div>
          <span v-if="errors.username" class="field__error">{{ errors.username }}</span>
        </label>

        <label class="field">
          <span class="field__label">昵称</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.nickname) }">
            <span class="input-shell__icon"><ShieldCheck :size="16" /></span>
            <input v-model="form.nickname" class="app-input" type="text" autocomplete="nickname" :disabled="submitting" placeholder="用于显示，可留空" @input="delete errors.nickname" />
          </div>
          <span v-if="errors.nickname" class="field__error">{{ errors.nickname }}</span>
        </label>

        <label class="field">
          <span class="field__label">手机号</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.phone) }">
            <span class="input-shell__icon"><Phone :size="16" /></span>
            <input v-model="form.phone" class="app-input" type="tel" autocomplete="tel" :disabled="submitting" placeholder="11 位大陆手机号" @input="delete errors.phone" />
          </div>
          <span v-if="errors.phone" class="field__error">{{ errors.phone }}</span>
        </label>

        <label class="field">
          <span class="field__label">邮箱</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.email) }">
            <span class="input-shell__icon"><Mail :size="16" /></span>
            <input v-model="form.email" class="app-input" type="email" autocomplete="email" :disabled="submitting" placeholder="name@example.com" @input="delete errors.email" />
          </div>
          <span v-if="errors.email" class="field__error">{{ errors.email }}</span>
        </label>

        <label v-if="mode === 'create'" class="field">
          <span class="field__label">密码</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.password) }">
            <input v-model="form.password" class="app-input" type="password" autocomplete="new-password" :disabled="submitting" placeholder="至少 8 位" @input="delete errors.password" />
          </div>
          <span v-if="errors.password" class="field__error">{{ errors.password }}</span>
        </label>

        <label v-if="mode === 'create'" class="field">
          <span class="field__label">确认密码</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.confirmPassword) }">
            <input v-model="form.confirmPassword" class="app-input" type="password" autocomplete="new-password" :disabled="submitting" placeholder="再次输入密码" @input="delete errors.confirmPassword" />
          </div>
          <span v-if="errors.confirmPassword" class="field__error">{{ errors.confirmPassword }}</span>
        </label>

        <label class="field">
          <span class="field__label">所属租户</span>
          <select v-model="form.tenantId" class="app-select" :disabled="submitting">
            <option value="">默认租户（自动初始化）</option>
            <option v-for="tenant in tenantOptions" :key="tenant.id" :value="tenant.id">{{ tenant.tenantName }} / {{ tenant.tenantCode }}</option>
          </select>
          <span v-if="errors.tenantId" class="field__error">{{ errors.tenantId }}</span>
        </label>
      </div>

      <fieldset class="field field--stacked">
        <legend class="field__label">状态</legend>
        <div class="status-toggle">
          <label class="status-toggle__option" :class="{ 'status-toggle__option--active': form.status === 1 }">
            <input v-model="form.status" type="radio" name="user-status" :value="1" :disabled="submitting" />
            <CheckCircle2 :size="16" />
            <span>启用</span>
          </label>
          <label class="status-toggle__option" :class="{ 'status-toggle__option--active': form.status === 0 }">
            <input v-model="form.status" type="radio" name="user-status" :value="0" :disabled="submitting" />
            <CircleOff :size="16" />
            <span>禁用</span>
          </label>
        </div>
      </fieldset>
    </form>

    <template #footer>
      <button type="button" class="app-button app-button--secondary" :disabled="submitting" @click="emit('update:modelValue', false)">取消</button>
      <button type="button" class="app-button" :disabled="submitting" :aria-busy="submitting" @click="handleSubmit">
        <span v-if="submitting" class="button-spinner" aria-hidden="true"></span>
        {{ submitting ? '提交中...' : mode === 'create' ? '创建用户' : '保存修改' }}
      </button>
    </template>
  </AppDialog>
</template>

<style scoped>
.user-form { display: flex; flex-direction: column; gap: 22px; }
.identity-grid, .form-grid, .status-toggle { display: grid; gap: 16px; }
.identity-grid, .form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.identity-card { padding: 16px 18px; border-radius: 22px; background: rgba(255,255,255,.04); border: 1px solid rgba(255,255,255,.08); }
.identity-card__label { display: block; margin-bottom: 8px; color: var(--color-ink-muted); font-size: .76rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
.identity-card strong { color: var(--color-ink-strong); }
.status-toggle { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.status-toggle__option { position: relative; display: inline-flex; align-items: center; justify-content: center; gap: 10px; min-height: 54px; border: 1px solid rgba(255,255,255,.08); border-radius: 18px; color: var(--color-ink-soft); background: rgba(255,255,255,.04); cursor: pointer; transition: all 180ms ease; }
.status-toggle__option--active { color: var(--color-ink-strong); border-color: rgba(83,184,255,.28); background: rgba(83,184,255,.12); transform: translateY(-1px); }
.status-toggle__option input { position: absolute; opacity: 0; pointer-events: none; }
@media (max-width: 720px) { .identity-grid, .form-grid, .status-toggle { grid-template-columns: 1fr; } }
</style>
