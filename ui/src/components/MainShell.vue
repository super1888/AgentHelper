<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LogOut, ShieldCheck } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const logoutPending = ref(false)

const currentLabel = computed(() => authStore.displayName)

async function handleLogout() {
  logoutPending.value = true

  try {
    await authStore.signOut()
    await router.replace({ name: 'login' })
  } finally {
    logoutPending.value = false
  }
}
</script>

<template>
  <div class="shell">
    <header class="shell__header panel-card">
      <div class="shell__brand">
        <div class="shell__logo" aria-hidden="true">
          <ShieldCheck :size="22" />
        </div>

        <div class="shell__copy">
          <p class="section-kicker">Agent Helper</p>
          <h1>Agent Helper 控制台</h1>
          <p class="shell__description">统一用户认证、账号管理与工作台入口。</p>
        </div>
      </div>

      <div class="shell__actions">
        <div class="shell__identity">
          <span class="shell__identity-label">当前登录</span>
          <strong>{{ currentLabel }}</strong>
        </div>

        <button
          type="button"
          class="app-button app-button--secondary"
          :disabled="logoutPending"
          :aria-busy="logoutPending"
          @click="handleLogout"
        >
          <span v-if="logoutPending" class="button-spinner" aria-hidden="true"></span>
          <LogOut v-else :size="16" aria-hidden="true" />
          {{ logoutPending ? '退出中...' : '退出登录' }}
        </button>
      </div>
    </header>

    <nav class="shell__nav" aria-label="主导航">
      <RouterLink
        to="/users"
        class="shell__nav-link"
        :class="{ 'shell__nav-link--active': route.name === 'users' }"
      >
        用户管理
      </RouterLink>
    </nav>

    <main class="shell__content">
      <slot></slot>
    </main>
  </div>
</template>

<style scoped>
.shell {
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
  padding: 28px 0 40px;
}

.shell__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  animation: rise-in 520ms ease both;
}

.shell__brand {
  display: flex;
  gap: 18px;
  min-width: 0;
}

.shell__copy {
  min-width: 0;
}

.shell__logo {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 52px;
  height: 52px;
  color: #ffffff;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-strong));
  box-shadow: 0 18px 36px rgba(83, 184, 255, 0.22);
}

.shell__brand h1 {
  margin: 8px 0 10px;
  color: var(--color-ink-strong);
  font-size: clamp(1.8rem, 2.2vw, 2.4rem);
  line-height: 1.08;
  letter-spacing: -0.02em;
  text-wrap: balance;
}

.shell__description {
  max-width: 32rem;
  margin: 0;
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.shell__actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shell__identity {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  min-width: 132px;
}

.shell__identity-label {
  color: var(--color-ink-muted);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.shell__identity strong {
  color: var(--color-ink-strong);
  font-size: 1rem;
  line-height: 1.4;
}

.shell__nav {
  display: flex;
  gap: 12px;
  margin: 18px 0 24px;
}

.shell__nav-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 18px;
  color: var(--color-ink-soft);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  transition:
    color 180ms ease,
    background-color 180ms ease,
    border-color 180ms ease,
    transform 180ms ease;
}

.shell__nav-link:hover {
  color: var(--color-ink-strong);
  border-color: rgba(83, 184, 255, 0.22);
  background: rgba(83, 184, 255, 0.08);
}

.shell__nav-link--active {
  color: #04111d;
  border-color: transparent;
  background: linear-gradient(135deg, rgba(143, 231, 255, 0.92), rgba(83, 184, 255, 0.92));
  box-shadow: 0 12px 28px rgba(83, 184, 255, 0.18);
}

.shell__content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

@media (max-width: 860px) {
  .shell {
    width: min(100%, calc(100% - 20px));
    padding-top: 16px;
  }

  .shell__header {
    flex-direction: column;
    padding: 22px;
  }

  .shell__actions {
    width: 100%;
    justify-content: space-between;
  }

  .shell__identity {
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .shell__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
