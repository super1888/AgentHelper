interface RuntimeAppConfig {
  apiBaseUrl?: string
  proxyTarget?: string
  appTitle?: string
}

declare global {
  interface Window {
    __APP_CONFIG__?: RuntimeAppConfig
  }
}

const runtimeConfig = window.__APP_CONFIG__ ?? {}
const isDev = import.meta.env.DEV

function trimTrailingSlash(value: string) {
  return value.replace(/\/+$/, '')
}

function readConfigValue(...candidates: Array<string | undefined>) {
  const value = candidates.find((item) => typeof item === 'string' && item.trim().length > 0)?.trim()
  return value ? trimTrailingSlash(value) : undefined
}

export const appConfig = {
  appTitle: readConfigValue(
    ...(isDev
      ? [import.meta.env.VITE_APP_TITLE, runtimeConfig.appTitle]
      : [runtimeConfig.appTitle, import.meta.env.VITE_APP_TITLE]),
    'Agent Helper Console',
  )!,
  apiBaseUrl: readConfigValue(
    ...(isDev
      ? [import.meta.env.VITE_API_BASE_URL, runtimeConfig.apiBaseUrl]
      : [runtimeConfig.apiBaseUrl, import.meta.env.VITE_API_BASE_URL]),
    '/agentHelper',
  )!,
  proxyTarget: readConfigValue(
    ...(isDev
      ? [import.meta.env.VITE_PROXY_TARGET, runtimeConfig.proxyTarget]
      : [runtimeConfig.proxyTarget, import.meta.env.VITE_PROXY_TARGET]),
    'http://127.0.0.1:8080',
  )!,
}
