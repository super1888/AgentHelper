import type { Router } from 'vue-router'
import { trackStatistics } from '@/api/statistics'

const VISITOR_STORAGE_KEY = 'spring-ai:statistics-visitor-id'
const VISIT_STORAGE_KEY = 'spring-ai:statistics-visit-id'
const VISIT_STARTED_KEY = 'spring-ai:statistics-visit-started-at'
const VISIT_TTL = 30 * 60 * 1000

function isBrowser() {
  return typeof window !== 'undefined'
}

function createId(prefix: string) {
  const randomValue = typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `${prefix}-${randomValue}`
}

function readOrCreateVisitorId() {
  const existed = window.localStorage.getItem(VISITOR_STORAGE_KEY)
  if (existed) {
    return existed
  }
  const visitorId = createId('visitor')
  window.localStorage.setItem(VISITOR_STORAGE_KEY, visitorId)
  return visitorId
}

function readOrCreateVisitId() {
  const now = Date.now()
  const startedAt = Number(window.sessionStorage.getItem(VISIT_STARTED_KEY) || 0)
  const existed = window.sessionStorage.getItem(VISIT_STORAGE_KEY)
  if (existed && startedAt && now - startedAt <= VISIT_TTL) {
    window.sessionStorage.setItem(VISIT_STARTED_KEY, String(now))
    return existed
  }
  const visitId = createId('visit')
  window.sessionStorage.setItem(VISIT_STORAGE_KEY, visitId)
  window.sessionStorage.setItem(VISIT_STARTED_KEY, String(now))
  return visitId
}

export function installStatisticsTracker(router: Router) {
  if (!isBrowser()) {
    return
  }
  router.afterEach((to) => {
    if (to.meta.guestOnly) {
      return
    }
    void trackStatistics({
      path: to.fullPath,
      title: document.title,
      visitorId: readOrCreateVisitorId(),
      visitId: readOrCreateVisitId(),
    }).catch(() => undefined)
  })
}