import type { UserProfile, UserToken } from '@/types/user'

export interface AuthSnapshot {
  user: UserProfile | null
  token: UserToken | null
}

const AUTH_STORAGE_KEY = 'spring-ai:user-auth'

function isBrowser() {
  return typeof window !== 'undefined'
}

export function loadAuthSnapshot(): AuthSnapshot | null {
  if (!isBrowser()) {
    return null
  }

  const rawValue = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (!rawValue) {
    return null
  }

  try {
    return JSON.parse(rawValue) as AuthSnapshot
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export function saveAuthSnapshot(snapshot: AuthSnapshot | null) {
  if (!isBrowser()) {
    return
  }

  if (!snapshot?.token) {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
    return
  }

  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(snapshot))
}

export function clearAuthSnapshot() {
  if (!isBrowser()) {
    return
  }

  window.localStorage.removeItem(AUTH_STORAGE_KEY)
}
