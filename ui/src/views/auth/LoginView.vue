<script setup lang="ts">
import { computed, reactive, ref, watchEffect } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ArrowRight, Eye, EyeOff, KeyRound, Server, UserRound } from 'lucide-vue-next'
import AuthFrame from '@/components/AuthFrame.vue'
import { appConfig } from '@/config/env'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/utils/errors'
import { validateLoginForm, type LoginFormState } from '@/utils/validation'

type LoginField = keyof LoginFormState

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const form = reactive<LoginFormState>({
  username: '',
  password: '',
})

const errors = reactive<Partial<Record<LoginField | 'form', string>>>({})
const passwordVisible = ref(false)
const submitting = ref(false)

const apiEndpointLabel = computed(() => {
  try {
    const endpoint = appConfig.apiBaseUrl.startsWith('/')
      ? appConfig.proxyTarget
      : appConfig.apiBaseUrl
    return new URL(endpoint, window.location.origin).host
  } catch {
    return appConfig.apiBaseUrl
  }
})

const registeredNotice = computed(() =>
  route.query.registered === '1' ? '注册成功，账号已创建，现在可以直接登录。' : '',
)

const highlights = [
  {
    label: '环境',
    value: 'Development',
    detail: '本地联调工作台',
  },
  {
    label: '认证',
    value: 'Sa-Token',
    detail: '统一登录态管理',
  },
  {
    label: '入口',
    value: 'User Console',
    detail: '控制台统一入口',
  },
]

function clearErrors() {
  for (const key of Object.keys(errors) as Array<LoginField | 'form'>) {
    delete errors[key]
  }
}

async function handleSubmit() {
  clearErrors()

  const nextErrors = validateLoginForm(form)
  Object.assign(errors, nextErrors)

  if (Object.keys(nextErrors).length > 0) {
    return
  }

  submitting.value = true

  try {
    await authStore.loginWithPassword({
      username: form.username.trim(),
      password: form.password.trim(),
    })

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/agents'
    await router.replace(redirect)
  } catch (error) {
    errors.form = getErrorMessage(error, '登录失败，请稍后重试。')
  } finally {
    submitting.value = false
  }
}

watchEffect(() => {
  if (!form.username && typeof route.query.username === 'string') {
    form.username = route.query.username
  }
})
</script>

<template>
  <AuthFrame
    eyebrow="Authentication"
    title="Agent Helper 控制台"
    description="统一认证入口与工作台访问控制，收敛账号登录、环境接入和权限校验流程。"
    panel-title="欢迎回来"
    panel-description="登录后进入业务控制台，继续处理 Agent、向量库、提示词和租户管理任务。"
    :highlights="highlights"
  >
    <div class="auth-meta">
      <span class="auth-meta__badge auth-meta__badge--accent">DEV</span>
      <span class="auth-meta__badge">
        <Server :size="14" aria-hidden="true" />
        {{ apiEndpointLabel }}
      </span>
    </div>

    <form class="auth-form" @submit.prevent="handleSubmit">
      <div v-if="registeredNotice" class="feedback-banner feedback-banner--success" aria-live="polite">
        {{ registeredNotice }}
      </div>

      <div v-if="errors.form" class="feedback-banner feedback-banner--error" role="alert">
        {{ errors.form }}
      </div>

      <label class="field">
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
            placeholder="请输入用户名"
            :aria-invalid="Boolean(errors.username)"
            :disabled="submitting"
            @input="delete errors.username"
          />
        </div>
        <span v-if="errors.username" class="field__error" role="alert">{{ errors.username }}</span>
      </label>

      <label class="field">
        <span class="field__label">密码</span>
        <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.password) }">
          <span class="input-shell__icon" aria-hidden="true">
            <KeyRound :size="16" />
          </span>
          <input
            v-model="form.password"
            class="app-input"
            :type="passwordVisible ? 'text' : 'password'"
            autocomplete="current-password"
            placeholder="请输入密码"
            :aria-invalid="Boolean(errors.password)"
            :disabled="submitting"
            @input="delete errors.password"
          />
          <button
            type="button"
            class="input-shell__toggle"
            :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
            :disabled="submitting"
            @click="passwordVisible = !passwordVisible"
          >
            <EyeOff v-if="passwordVisible" :size="16" aria-hidden="true" />
            <Eye v-else :size="16" aria-hidden="true" />
          </button>
        </div>
        <span v-if="errors.password" class="field__error" role="alert">{{ errors.password }}</span>
      </label>

      <button
        type="submit"
        class="app-button auth-form__submit"
        :disabled="submitting"
        :aria-busy="submitting"
      >
        <span v-if="submitting" class="button-spinner" aria-hidden="true"></span>
        <ArrowRight v-else :size="16" aria-hidden="true" />
        {{ submitting ? '登录中...' : '进入控制台' }}
      </button>
    </form>

    <div class="auth-links">
      <span>还没有账号？</span>
      <RouterLink to="/register">创建新账号</RouterLink>
    </div>
  </AuthFrame>
</template>

<style scoped>
.auth-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.auth-meta__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.05);
  font-size: 0.82rem;
}

.auth-meta__badge--accent {
  color: #04111d;
  background: linear-gradient(135deg, var(--color-accent-strong), var(--color-accent));
  border-color: transparent;
  font-weight: 700;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.auth-form__submit {
  width: 100%;
  margin-top: 6px;
}

.auth-links {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
  color: var(--color-ink-soft);
}

.auth-links a {
  color: var(--color-accent-strong);
  font-weight: 700;
}
</style>
