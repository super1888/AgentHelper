import { createRouter, createWebHistory } from 'vue-router'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/agents',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: {
        guestOnly: true,
      },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: {
        guestOnly: true,
      },
    },
    {
      path: '/agents',
      name: 'agents',
      component: () => import('@/views/agents/AgentManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/agents/:agentId/chat',
      name: 'agent-chat',
      component: () => import('@/views/agents/AgentChatView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/core-config',
      name: 'core-config',
      component: () => import('@/views/core/CoreManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/image-studio',
      name: 'image-studio',
      component: () => import('@/views/images/ImageStudioView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/opencv',
      name: 'opencv',
      component: () => import('@/views/opencv/OpenCvView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/prompts',
      name: 'prompts',
      component: () => import('@/views/prompts/PromptTemplateManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/skills',
      name: 'skills',
      component: () => import('@/views/skills/SkillManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/tools',
      name: 'tools',
      component: () => import('@/views/tools/ToolManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/links',
      name: 'links',
      component: () => import('@/views/links/ShortLinkManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/mcp',
      name: 'mcp',
      component: () => import('@/views/mcp/McpManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/hooks',
      name: 'hooks',
      component: () => import('@/views/hooks/HookManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/interceptors',
      name: 'interceptors',
      component: () => import('@/views/interceptors/InterceptorManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/a2a',
      name: 'a2a',
      component: () => import('@/views/a2a/A2aManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/vectors',
      name: 'vectors',
      component: () => import('@/views/vectors/VectorManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/tenants',
      name: 'tenants',
      component: () => import('@/views/tenants/TenantManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/users',
      name: 'users',
      component: () => import('@/views/users/UserManagementView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)

  if (!authStore.bootstrapped) {
    await authStore.bootstrap()
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return {
      name: 'agents',
    }
  }

  return true
})

export default router
