<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { CheckCircle2, CircleOff, Mail, Phone, ShieldCheck, UserRound } from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'
import type { CreateUserPayload, UpdateUserPayload, UserProfile } from '@/types/user'
import {
  normalizeOptionalText,
  parseTenantIdInput,
  validateCreateUserForm,
  validateUpdateUserForm,
  type UserCreateFormState,
  type UserUpdateFormState,
} from '@/utils/validation'

type DialogMode = 'create' | 'edit'

interface UserDialogFormState extends UserCreateFormState {
  username: string
}

type UserDialogField = keyof UserDialogFormState

const props = defineProps<{
  modelValue: boolean
  mode: DialogMode
  user: UserProfile | null
  submitting: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  submit: [{ mode: DialogMode; payload: CreateUserPayload | UpdateUserPayload }]
}>()

const form = reactive<UserDialogFormState>(createEmptyForm())
const errors = reactive<Partial<Record<UserDialogField, string>>>({})

const dialogTitle = computed(() => (props.mode === 'create' ? '新增用户' : '编辑用户'))
const dialogDescription = computed(() =>
  props.mode === 'create'
    ? '录入账号基础信息与状态，提交后直接写入用户模块。'
    : '更新昵称、联系方式、租户与状态，用户名保持只读。',
)

function createEmptyForm(): UserDialogFormState {
  return {
    username: '',
    nickname: '',
    phone: '',
    email: '',
    password: '',
    confirmPassword: '',
    status: 1,
    tenantId: '',
  }
}

function clearErrors() {
  for (const key of Object.keys(errors) as UserDialogField[]) {
    delete errors[key]
  }
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

  const nextErrors =
    props.mode === 'create'
      ? validateCreateUserForm(form)
      : validateUpdateUserForm(form as UserUpdateFormState)

  Object.assign(errors, nextErrors)
  return Object.keys(nextErrors).length === 0
}

function closeDialog() {
  emit('update:modelValue', false)
}

function handleSubmit() {
  if (!validateForm()) {
    return
  }

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
  ([isOpen]) => {
    if (isOpen) {
      syncFormWithProps()
    }
  },
  { immediate: true },
)
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    :title="dialogTitle"
    :description="dialogDescription"
    width="wide"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <form class="user-form" @submit.prevent="handleSubmit">
      <div v-if="mode === 'edit' && user" class="user-form__identity">
        <div class="user-form__identity-card">
          <span class="user-form__identity-label">用户名</span>
          <strong>{{ user.username }}</strong>
        </div>

        <div class="user-form__identity-card">
          <span class="user-form__identity-label">用户 ID</span>
          <strong>#{{ user.id }}</strong>
        </div>
      </div>

      <div class="user-form__grid">
        <label v-if="mode === 'create'" class="field">
          <span class="field__label">用户名</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.username) }">
            <span class="input-shell__icon" aria-hidden="true">
              <UserRound :size="16" />
            </span>
            <input
              v-model="form.username"
              class="app-input"
              type="text"
              autocomplete="username"
              placeholder="请输入登录用户名"
              :aria-invalid="Boolean(errors.username)"
              :disabled="submitting"
              @input="delete errors.username"
            />
          </div>
          <span v-if="errors.username" class="field__error" role="alert">{{ errors.username }}</span>
        </label>

        <label class="field">
          <span class="field__label">昵称</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.nickname) }">
            <span class="input-shell__icon" aria-hidden="true">
              <ShieldCheck :size="16" />
            </span>
            <input
              v-model="form.nickname"
              class="app-input"
              type="text"
              autocomplete="nickname"
              placeholder="用于展示，可留空"
              :aria-invalid="Boolean(errors.nickname)"
              :disabled="submitting"
              @input="delete errors.nickname"
            />
          </div>
          <span v-if="errors.nickname" class="field__error" role="alert">{{ errors.nickname }}</span>
        </label>

        <label class="field">
          <span class="field__label">手机号</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.phone) }">
            <span class="input-shell__icon" aria-hidden="true">
              <Phone :size="16" />
            </span>
            <input
              v-model="form.phone"
              class="app-input"
              type="tel"
              autocomplete="tel"
              placeholder="11 位大陆手机号"
              :aria-invalid="Boolean(errors.phone)"
              :disabled="submitting"
              @input="delete errors.phone"
            />
          </div>
          <span v-if="errors.phone" class="field__error" role="alert">{{ errors.phone }}</span>
        </label>

        <label class="field">
          <span class="field__label">邮箱</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.email) }">
            <span class="input-shell__icon" aria-hidden="true">
              <Mail :size="16" />
            </span>
            <input
              v-model="form.email"
              class="app-input"
              type="email"
              autocomplete="email"
              placeholder="name@example.com"
              :aria-invalid="Boolean(errors.email)"
              :disabled="submitting"
              @input="delete errors.email"
            />
          </div>
          <span v-if="errors.email" class="field__error" role="alert">{{ errors.email }}</span>
        </label>

        <label v-if="mode === 'create'" class="field">
          <span class="field__label">密码</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.password) }">
            <input
              v-model="form.password"
              class="app-input"
              type="password"
              autocomplete="new-password"
              placeholder="至少 8 位"
              :aria-invalid="Boolean(errors.password)"
              :disabled="submitting"
              @input="delete errors.password"
            />
          </div>
          <span v-if="errors.password" class="field__error" role="alert">{{ errors.password }}</span>
        </label>

        <label v-if="mode === 'create'" class="field">
          <span class="field__label">确认密码</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.confirmPassword) }">
            <input
              v-model="form.confirmPassword"
              class="app-input"
              type="password"
              autocomplete="new-password"
              placeholder="再次输入密码"
              :aria-invalid="Boolean(errors.confirmPassword)"
              :disabled="submitting"
              @input="delete errors.confirmPassword"
            />
          </div>
          <span v-if="errors.confirmPassword" class="field__error" role="alert">
            {{ errors.confirmPassword }}
          </span>
        </label>

        <label class="field">
          <span class="field__label">租户 ID</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.tenantId) }">
            <input
              v-model="form.tenantId"
              class="app-input"
              type="text"
              inputmode="numeric"
              placeholder="留空表示默认租户"
              :aria-invalid="Boolean(errors.tenantId)"
              :disabled="submitting"
              @input="delete errors.tenantId"
            />
          </div>
          <span v-if="errors.tenantId" class="field__error" role="alert">{{ errors.tenantId }}</span>
        </label>
      </div>

      <fieldset class="field field--stacked">
        <legend class="field__label">状态</legend>
        <div class="status-toggle">
          <label class="status-toggle__option" :class="{ 'status-toggle__option--active': form.status === 1 }">
            <input
              v-model="form.status"
              type="radio"
              name="user-status"
              :value="1"
              :disabled="submitting"
            />
            <CheckCircle2 :size="16" aria-hidden="true" />
            <span>启用</span>
          </label>

          <label class="status-toggle__option" :class="{ 'status-toggle__option--active': form.status === 0 }">
            <input
              v-model="form.status"
              type="radio"
              name="user-status"
              :value="0"
              :disabled="submitting"
            />
            <CircleOff :size="16" aria-hidden="true" />
            <span>禁用</span>
          </label>
        </div>
      </fieldset>
    </form>

    <template #footer>
      <button
        type="button"
        class="app-button app-button--secondary"
        :disabled="submitting"
        @click="closeDialog"
      >
        取消
      </button>

      <button
        type="button"
        class="app-button"
        :disabled="submitting"
        :aria-busy="submitting"
        @click="handleSubmit"
      >
        <span v-if="submitting" class="button-spinner" aria-hidden="true"></span>
        {{ submitting ? '提交中...' : mode === 'create' ? '创建用户' : '保存修改' }}
      </button>
    </template>
  </AppDialog>
</template>

<style scoped>
.user-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.user-form__identity {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.user-form__identity-card {
  padding: 16px 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.04);
}

.user-form__identity-label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-ink-muted);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.user-form__identity strong {
  color: var(--color-ink-strong);
}

.user-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field--stacked {
  padding: 0;
  border: 0;
  margin: 0;
}

.status-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.status-toggle__option {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 54px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease,
    transform 180ms ease;
}

.status-toggle__option:hover {
  border-color: rgba(83, 184, 255, 0.22);
  background: rgba(255, 255, 255, 0.06);
}

.status-toggle__option--active {
  color: var(--color-ink-strong);
  border-color: rgba(83, 184, 255, 0.28);
  background: rgba(83, 184, 255, 0.12);
  transform: translateY(-1px);
}

.status-toggle__option input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

@media (max-width: 720px) {
  .user-form__grid,
  .user-form__identity,
  .status-toggle {
    grid-template-columns: 1fr;
  }
}
</style>
