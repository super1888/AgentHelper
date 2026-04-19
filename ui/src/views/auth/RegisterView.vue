<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  ArrowRight,
  Eye,
  EyeOff,
  Mail,
  Phone,
  UserRound,
  UserRoundPlus,
} from 'lucide-vue-next'
import AuthFrame from '@/components/AuthFrame.vue'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/utils/errors'
import {
  normalizeOptionalText,
  validateRegisterForm,
  type RegisterFormState,
} from '@/utils/validation'

type RegisterField = keyof RegisterFormState

const authStore = useAuthStore()
const router = useRouter()

const form = reactive<RegisterFormState>({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const errors = reactive<Partial<Record<RegisterField | 'form', string>>>({})
const passwordVisible = ref(false)
const confirmPasswordVisible = ref(false)
const submitting = ref(false)

const highlights = [
  {
    label: '开户',
    value: '标准流程',
    detail: '保留现有字段结构',
  },
  {
    label: '风格',
    value: '统一认证',
    detail: '与登录页同一视觉体系',
  },
  {
    label: '回跳',
    value: '自动回填',
    detail: '注册后带回用户名',
  },
]

function clearErrors() {
  for (const key of Object.keys(errors) as Array<RegisterField | 'form'>) {
    delete errors[key]
  }
}

async function handleSubmit() {
  clearErrors()

  const nextErrors = validateRegisterForm(form)
  Object.assign(errors, nextErrors)

  if (Object.keys(nextErrors).length > 0) {
    return
  }

  submitting.value = true

  try {
    await authStore.registerWithPassword({
      username: form.username.trim(),
      nickname: normalizeOptionalText(form.nickname),
      phone: normalizeOptionalText(form.phone),
      email: normalizeOptionalText(form.email),
      password: form.password.trim(),
      confirmPassword: form.confirmPassword.trim(),
    })

    await router.replace({
      name: 'login',
      query: {
        registered: '1',
        username: form.username.trim(),
      },
    })
  } catch (error) {
    errors.form = getErrorMessage(error, '注册失败，请稍后重试。')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthFrame
    eyebrow="Registration"
    title="开通平台账号"
    description="保持注册字段清晰、流程克制，把账号初始化控制在最短闭环里完成。"
    panel-title="创建账号"
    panel-description="提交后返回登录页，并自动回填用户名，便于继续进入控制台。"
    :highlights="highlights"
  >
    <form class="auth-form" @submit.prevent="handleSubmit">
      <div v-if="errors.form" class="feedback-banner feedback-banner--error" role="alert">
        {{ errors.form }}
      </div>

      <div class="auth-form__grid">
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
              placeholder="4 到 64 位用户名"
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
              <UserRoundPlus :size="16" />
            </span>
            <input
              v-model="form.nickname"
              class="app-input"
              type="text"
              autocomplete="nickname"
              placeholder="选填，用于展示"
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
              placeholder="11 位手机号"
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

        <label class="field">
          <span class="field__label">密码</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.password) }">
            <input
              v-model="form.password"
              class="app-input"
              :type="passwordVisible ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="至少 8 位密码"
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

        <label class="field">
          <span class="field__label">确认密码</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.confirmPassword) }">
            <input
              v-model="form.confirmPassword"
              class="app-input"
              :type="confirmPasswordVisible ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="再次输入密码"
              :aria-invalid="Boolean(errors.confirmPassword)"
              :disabled="submitting"
              @input="delete errors.confirmPassword"
            />
            <button
              type="button"
              class="input-shell__toggle"
              :aria-label="confirmPasswordVisible ? '隐藏确认密码' : '显示确认密码'"
              :disabled="submitting"
              @click="confirmPasswordVisible = !confirmPasswordVisible"
            >
              <EyeOff v-if="confirmPasswordVisible" :size="16" aria-hidden="true" />
              <Eye v-else :size="16" aria-hidden="true" />
            </button>
          </div>
          <span v-if="errors.confirmPassword" class="field__error" role="alert">
            {{ errors.confirmPassword }}
          </span>
        </label>
      </div>

      <button
        type="submit"
        class="app-button auth-form__submit"
        :disabled="submitting"
        :aria-busy="submitting"
      >
        <span v-if="submitting" class="button-spinner" aria-hidden="true"></span>
        <ArrowRight v-else :size="16" aria-hidden="true" />
        {{ submitting ? '注册中...' : '创建账号并返回登录' }}
      </button>
    </form>

    <div class="auth-links">
      <span>已经有账号？</span>
      <RouterLink to="/login">返回登录</RouterLink>
    </div>
  </AuthFrame>
</template>

<style scoped>
.auth-form__submit {
  margin-top: 4px;
}
</style>
