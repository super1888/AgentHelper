import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/client'
import { appConfig } from './config/env'
import { pinia } from './stores'
import { useAuthStore } from './stores/auth'
import './style.css'

document.title = appConfig.appTitle

const app = createApp(App)

app.use(pinia)
app.use(router)

const authStore = useAuthStore(pinia)

setUnauthorizedHandler(() => {
  authStore.clearAuth()

  const currentPath = router.currentRoute.value.fullPath
  if (currentPath === '/login') {
    return
  }

  void router.push({
    name: 'login',
    query: currentPath && currentPath !== '/' ? { redirect: currentPath } : undefined,
  })
})

void authStore.bootstrap()

app.mount('#app')
