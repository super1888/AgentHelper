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

        <div>
          <p class="section-kicker">AgentHelper</p>
          <h1>用户中心</h1>
          <p class="shell__description">
            管理系统账户、状态和基础资料，所有操作都与 `user` 模块接口保持一致。
          </p>
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

.shell__logo {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  color: #ffffff;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-strong));
  box-shadow: 0 20px 40px rgba(14, 165, 233, 0.24);
}

.shell__brand h1 {
  margin: 8px 0 10px;
  color: var(--color-text-strong);
  font-size: clamp(1.8rem, 2.2vw, 2.5rem);
  line-height: 1.1;
}

.shell__description {
  max-width: 38rem;
  margin: 0;
  color: var(--color-text-soft);
  line-height: 1.7;
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
  color: var(--color-text-muted);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.shell__identity strong {
  color: var(--color-text-strong);
  font-size: 1rem;
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
  padding: 0 16px;
  color: var(--color-text-soft);
  border-radius: 999px;
  transition: color 180ms ease, background-color 180ms ease, transform 180ms ease;
}

.shell__nav-link:hover {
  color: var(--color-text-strong);
  background: rgba(255, 255, 255, 0.7);
}

.shell__nav-link--active {
  color: var(--color-text-strong);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-soft);
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
