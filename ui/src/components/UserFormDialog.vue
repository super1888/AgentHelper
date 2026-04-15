<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
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
    ? '填写基础资料并设置启用状态，数据会直接写入后端用户模块。'
    : '更新用户昵称、联系方式和状态，用户名保持只读。',
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
        <div>
          <span class="user-form__identity-label">用户名</span>
          <strong>{{ user.username }}</strong>
        </div>

        <div>
          <span class="user-form__identity-label">用户 ID</span>
          <strong>#{{ user.id }}</strong>
        </div>
      </div>

      <div class="user-form__grid">
        <label v-if="mode === 'create'" class="field">
          <span class="field__label">用户名</span>
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
          <span v-if="errors.username" class="field__error" role="alert">{{ errors.username }}</span>
        </label>

        <label class="field">
          <span class="field__label">昵称</span>
          <input
            v-model="form.nickname"
            class="app-input"
            type="text"
            autocomplete="nickname"
            placeholder="用于页面展示，可留空"
            :aria-invalid="Boolean(errors.nickname)"
            :disabled="submitting"
            @input="delete errors.nickname"
          />
          <span v-if="errors.nickname" class="field__error" role="alert">{{ errors.nickname }}</span>
        </label>

        <label class="field">
          <span class="field__label">手机号</span>
          <input
            v-model="form.phone"
            class="app-input"
            type="tel"
            autocomplete="tel"
            placeholder="11 位中国大陆手机号"
            :aria-invalid="Boolean(errors.phone)"
            :disabled="submitting"
            @input="delete errors.phone"
          />
          <span v-if="errors.phone" class="field__error" role="alert">{{ errors.phone }}</span>
        </label>

        <label class="field">
          <span class="field__label">邮箱</span>
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
          <span v-if="errors.email" class="field__error" role="alert">{{ errors.email }}</span>
        </label>

        <label v-if="mode === 'create'" class="field">
          <span class="field__label">密码</span>
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
          <span v-if="errors.password" class="field__error" role="alert">{{ errors.password }}</span>
        </label>

        <label v-if="mode === 'create'" class="field">
          <span class="field__label">确认密码</span>
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
          <span v-if="errors.confirmPassword" class="field__error" role="alert">
            {{ errors.confirmPassword }}
          </span>
        </label>

        <label class="field">
          <span class="field__label">租户 ID</span>
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
          <span v-if="errors.tenantId" class="field__error" role="alert">{{ errors.tenantId }}</span>
        </label>
      </div>

      <fieldset class="field field--stacked">
        <legend class="field__label">状态</legend>
        <div class="status-toggle">
          <label class="status-toggle__option">
            <input
              v-model="form.status"
              type="radio"
              name="user-status"
              :value="1"
              :disabled="submitting"
            />
            <span>启用</span>
          </label>

          <label class="status-toggle__option">
            <input
              v-model="form.status"
              type="radio"
              name="user-status"
              :value="0"
              :disabled="submitting"
            />
            <span>禁用</span>
          </label>
        </div>
        <span class="field__hint">状态值与后端枚举一致：`1` 为启用，`0` 为禁用。</span>
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
  gap: 20px;
}

.user-form__identity {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 16px 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 22px;
  background: rgba(248, 250, 252, 0.84);
}

.user-form__identity-label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-text-muted);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.user-form__identity strong {
  color: var(--color-text-strong);
}

.user-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.status-toggle {
  display: inline-flex;
  gap: 10px;
  padding: 6px;
  border: 1px solid var(--color-border-strong);
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.72);
}

.status-toggle__option {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 18px;
  color: var(--color-text-soft);
  border-radius: 14px;
  cursor: pointer;
  transition: background-color 180ms ease, color 180ms ease;
}

.status-toggle__option input {
  accent-color: var(--color-accent-strong);
}

@media (max-width: 720px) {
  .user-form__grid,
  .user-form__identity {
    grid-template-columns: 1fr;
  }

  .status-toggle {
    display: grid;
  }
}
</style>
