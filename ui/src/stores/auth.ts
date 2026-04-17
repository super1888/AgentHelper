import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  fetchCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  registerAccount,
} from '@/api/auth'
import type {
  LoginPayload,
  RegisterPayload,
  UserAuthLoginResult,
  UserProfile,
  UserToken,
} from '@/types/user'
import { saveAuthSnapshot, loadAuthSnapshot } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const snapshot = loadAuthSnapshot()

  const user = ref<UserProfile | null>(snapshot?.user ?? null)
  const token = ref<UserToken | null>(snapshot?.token ?? null)
  const bootstrapped = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value?.authorizationValue))
  const displayName = computed(() => user.value?.nickname || user.value?.username || '未登录')

  watch(
    [user, token],
    ([nextUser, nextToken]) => {
      saveAuthSnapshot(nextToken ? { user: nextUser, token: nextToken } : null)
    },
    { deep: true },
  )

  function applyAuth(result: UserAuthLoginResult) {
    user.value = result.user
    token.value = result.token
    bootstrapped.value = true
  }

  function clearAuth() {
    user.value = null
    token.value = null
    bootstrapped.value = true
  }

  async function bootstrap() {
    if (bootstrapped.value) {
      return
    }

    if (!token.value) {
      bootstrapped.value = true
      return
    }

    try {
      user.value = await fetchCurrentUser()
    } catch {
      clearAuth()
      return
    }

    bootstrapped.value = true
  }

  async function loginWithPassword(payload: LoginPayload) {
    const result = await loginRequest(payload)
    applyAuth(result)
    return result
  }

  async function registerWithPassword(payload: RegisterPayload) {
    await registerAccount(payload)
  }

  async function signOut() {
    try {
      if (token.value) {
        await logoutRequest()
      }
    } finally {
      clearAuth()
    }
  }

  return {
    user,
    token,
    bootstrapped,
    isAuthenticated,
    displayName,
    bootstrap,
    clearAuth,
    loginWithPassword,
    registerWithPassword,
    signOut,
  }
})
