<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  Bot,
  Building2,
  Cpu,
  Database,
  FileCode2,
  GitBranch,
  Image,
  LogOut,
  PlugZap,
  ScanSearch,
  ShieldCheck,
  Sparkles,
  Users,
  Wrench,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const logoutPending = ref(false)

const currentLabel = computed(() => authStore.displayName)

const navItems = [
  {
    to: '/agents',
    label: '智能体管理',
    description: '角色、版本、会话与运行编排',
    icon: Bot,
    isActive: () => String(route.path).startsWith('/agents'),
  },
  {
    to: '/core-config',
    label: '核心配置',
    description: '模型提供商、模型参数与密钥托管',
    icon: Cpu,
    isActive: () => route.name === 'core-config',
  },
  {
    to: '/image-studio',
    label: 'gpt-image-2 图片生成',
    description: '提示、渲染、预览和导出',
    icon: Image,
    isActive: () => route.name === 'image-studio',
  },
  {
    to: '/opencv',
    label: 'OpenCV 识别',
    description: '上传图片，识别食材并叠加框选',
    icon: ScanSearch,
    isActive: () => route.name === 'opencv',
  },
  {
    to: '/prompts',
    label: '提示词模板',
    description: '模板资产、变量、规则与渲染测试',
    icon: FileCode2,
    isActive: () => route.name === 'prompts',
  },
  {
    to: '/skills',
    label: 'Skill 管理',
    description: '技能配置、版本发布与批量治理',
    icon: Sparkles,
    isActive: () => route.name === 'skills',
  },
  {
    to: '/tools',
    label: '工具管理',
    description: '工具注册、调试、发布与风险控制',
    icon: Wrench,
    isActive: () => route.name === 'tools',
  },
  {
    to: '/mcp',
    label: 'MCP 管理',
    description: 'MCP 目录、配置、调试、发布与日志审计',
    icon: PlugZap,
    isActive: () => route.name === 'mcp',
  },
  {
    to: '/hooks',
    label: 'Hook 管理',
    description: 'Hook 编排、绑定、调试与规则治理',
    icon: GitBranch,
    isActive: () => route.name === 'hooks',
  },
  {
    to: '/interceptors',
    label: '拦截器管理',
    description: '拦截器治理、发布、绑定与运行策略',
    icon: GitBranch,
    isActive: () => route.name === 'interceptors',
  },
  {
    to: '/a2a',
    label: 'A2A 协同',
    description: 'Agent Card、路由、调度与审计',
    icon: GitBranch,
    isActive: () => route.name === 'a2a',
  },
  {
    to: '/vectors',
    label: '向量管理',
    description: '知识入库、切片、检索与文件治理',
    icon: Database,
    isActive: () => route.name === 'vectors',
  },
  {
    to: '/tenants',
    label: '租户管理',
    description: '组织边界、租户配置与成员归属',
    icon: Building2,
    isActive: () => route.name === 'tenants',
  },
  {
    to: '/users',
    label: '用户管理',
    description: '账号、状态、联系方式与租户绑定',
    icon: Users,
    isActive: () => route.name === 'users',
  },
] as const

const activeNav = computed(() => navItems.find((item) => item.isActive()) ?? navItems[0])

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
    <aside class="shell__sidebar">
      <div class="shell__sidebar-inner panel-card">
        <section class="shell__brand">
          <div class="shell__brand-mark" aria-hidden="true">
            <ShieldCheck :size="20" />
          </div>
          <div class="shell__brand-copy">
            <p class="section-kicker">控制中枢</p>
            <h1>Agent Helper Console</h1>
            <p>统一管理智能体、模型、提示词、知识库、租户与平台扩展能力。</p>
          </div>
        </section>

        <nav class="shell__nav" aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="shell__nav-link"
            :class="{ 'shell__nav-link--active': item.isActive() }"
          >
            <div class="shell__nav-icon" aria-hidden="true">
              <component :is="item.icon" :size="16" />
            </div>
            <div class="shell__nav-copy">
              <strong>{{ item.label }}</strong>
              <span>{{ item.description }}</span>
            </div>
          </RouterLink>
        </nav>

        <section class="shell__sidebar-meta">
          <div class="shell__meta-block">
            <span>当前模块</span>
            <strong>{{ activeNav.label }}</strong>
          </div>
          <div class="shell__meta-divider"></div>
          <div class="shell__meta-block">
            <span>当前登录</span>
            <strong>{{ currentLabel }}</strong>
          </div>
        </section>

        <button
          type="button"
          class="app-button app-button--secondary shell__logout"
          :disabled="logoutPending"
          :aria-busy="logoutPending"
          @click="handleLogout"
        >
          <span v-if="logoutPending" class="button-spinner" aria-hidden="true"></span>
          <LogOut v-else :size="16" aria-hidden="true" />
          {{ logoutPending ? '退出中...' : '退出登录' }}
        </button>
      </div>
    </aside>

    <div class="shell__main">
      <header class="shell__topbar panel-card">
        <div class="shell__topbar-copy">
          <p class="section-kicker">当前工作区</p>
          <h2>{{ activeNav.label }}</h2>
          <p>{{ activeNav.description }}</p>
        </div>
        <div class="shell__topbar-tag">
          <span></span>
          <strong>稳定、克制、可运营</strong>
        </div>
      </header>

      <main class="shell__content">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: var(--layout-sidebar-width) minmax(0, 1fr);
  gap: var(--layout-gap);
  width: min(calc(100% - 32px), var(--layout-max-width));
  height: 100vh;
  height: 100dvh;
  margin: 0 auto;
  padding: 18px 0 20px;
  overflow: visible;
}

.shell__sidebar,
.shell__main {
  min-width: 0;
  min-height: 0;
}

.shell__sidebar-inner {
  position: sticky;
  top: 0;
  display: grid;
  gap: 16px;
  max-height: calc(100vh - 38px);
  max-height: calc(100dvh - 38px);
  padding: var(--compact-panel-padding);
  overflow: auto;
  scrollbar-gutter: stable;
}

.shell__brand {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.shell__brand-mark {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  color: #f5fcff;
  border-radius: 15px;
  background: linear-gradient(135deg, rgba(146, 235, 255, 0.95), rgba(77, 179, 255, 0.88));
  box-shadow:
    0 14px 28px rgba(63, 169, 255, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.shell__brand-copy {
  display: grid;
  gap: 8px;
}

.shell__brand-copy h1 {
  margin: 0;
  color: var(--color-ink-strong);
  font-size: 1.72rem;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.shell__brand-copy p:last-child {
  margin: 0;
  color: var(--color-ink-soft);
  font-size: 0.9rem;
  line-height: 1.6;
}

.shell__nav {
  display: grid;
  gap: 10px;
}

.shell__nav-link {
  position: relative;
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 72px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 18px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.03);
  transition:
    transform 220ms ease,
    border-color 220ms ease,
    background-color 220ms ease,
    box-shadow 220ms ease,
    color 220ms ease;
}

.shell__nav-link::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  border: 1px solid transparent;
  opacity: 0;
  pointer-events: none;
  transition: opacity 220ms ease, border-color 220ms ease;
}

.shell__nav-link:hover {
  transform: translateX(2px);
  color: var(--color-ink-strong);
  border-color: rgba(111, 208, 255, 0.14);
  background: rgba(77, 178, 255, 0.055);
}

.shell__nav-link--active {
  color: var(--color-ink-strong);
  background:
    linear-gradient(135deg, rgba(103, 194, 255, 0.12), rgba(103, 194, 255, 0.03)),
    rgba(255, 255, 255, 0.045);
  border-color: rgba(116, 213, 255, 0.14);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 12px 28px rgba(18, 74, 118, 0.08);
  animation: nav-breathe 4.2s ease-in-out infinite;
}

.shell__nav-link--active::before {
  opacity: 1;
  border-color: rgba(142, 228, 255, 0.24);
}

.shell__nav-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
}

.shell__nav-link--active .shell__nav-icon {
  background: rgba(100, 198, 255, 0.12);
  box-shadow: inset 0 0 0 1px rgba(142, 228, 255, 0.16);
}

.shell__nav-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.shell__nav-copy strong {
  font-size: 0.95rem;
  line-height: 1.35;
}

.shell__nav-copy span {
  color: inherit;
  opacity: 0.74;
  font-size: 0.78rem;
  line-height: 1.5;
}

.shell__sidebar-meta {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(255, 255, 255, 0.025);
}

.shell__meta-block {
  display: grid;
  gap: 4px;
}

.shell__meta-block span {
  color: var(--color-ink-muted);
  font-size: 0.72rem;
  line-height: 1.45;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.shell__meta-block strong {
  color: var(--color-ink-strong);
  font-size: 0.92rem;
  line-height: 1.35;
}

.shell__meta-divider {
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0));
}

.shell__logout {
  width: 100%;
}

.shell__main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 16px;
}

.shell__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 86px;
  padding: 18px var(--panel-padding);
}

.shell__topbar-copy {
  display: grid;
  gap: 8px;
}

.shell__topbar-copy h2 {
  margin: 0;
  color: var(--color-ink-strong);
  font-size: 1.58rem;
  line-height: 1.12;
  letter-spacing: -0.03em;
}

.shell__topbar-copy p:last-child {
  max-width: 30rem;
  margin: 0;
  color: var(--color-ink-soft);
  font-size: 0.9rem;
  line-height: 1.58;
}

.shell__topbar-tag {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  color: var(--color-ink-muted);
}

.shell__topbar-tag span {
  width: 42px;
  height: 1px;
  background: linear-gradient(90deg, rgba(137, 228, 255, 0.08), rgba(137, 228, 255, 0.55));
}

.shell__topbar-tag strong {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
  line-height: 1.45;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.shell__content {
  display: flex;
  flex-direction: column;
  gap: var(--layout-gap);
  min-width: 0;
  min-height: 0;
  overflow: auto;
  overflow-x: visible;
  overflow-y: auto;
  padding-top: 4px;
  padding-right: 4px;
  padding-bottom: 8px;
  scrollbar-gutter: stable;
}

@keyframes nav-breathe {
  0%,
  100% {
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.08),
      0 12px 28px rgba(18, 74, 118, 0.08);
  }

  50% {
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.1),
      0 14px 30px rgba(41, 118, 178, 0.1);
  }
}

@media (max-width: 1180px) {
  .shell {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 100vh;
    min-height: 100dvh;
    gap: 18px;
    overflow: visible;
  }

  .shell__sidebar-inner {
    position: static;
    max-height: none;
  }

  .shell__content {
    overflow: visible;
    padding-right: 0;
  }
}

@media (max-width: 720px) {
  .shell {
    width: min(100%, calc(100% - 20px));
    padding-top: 12px;
    padding-bottom: 14px;
  }

  .shell__sidebar-inner,
  .shell__topbar {
    padding: var(--compact-panel-padding);
  }

  .shell__topbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
