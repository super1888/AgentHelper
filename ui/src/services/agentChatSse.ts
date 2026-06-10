import { appConfig } from '@/config/env'
import type { AgentChatEvent, AgentChatPayload } from '@/types/agent'
import { loadAuthSnapshot } from '@/utils/storage'

interface AgentChatSseOptions {
  onEvent: (event: AgentChatEvent) => void
  onConnectionChange?: (connected: boolean) => void
  onError?: (message: string) => void
}

export class AgentChatSse {
  private eventSource: EventSource | null = null
  private readonly onEvent: (event: AgentChatEvent) => void
  private readonly onConnectionChange?: (connected: boolean) => void
  private readonly onError?: (message: string) => void

  constructor(options: AgentChatSseOptions) {
    this.onEvent = options.onEvent
    this.onConnectionChange = options.onConnectionChange
    this.onError = options.onError
  }

  connect(payload: AgentChatPayload) {
    this.disconnect()
    this.eventSource = new EventSource(this.buildChatUrl(payload), { withCredentials: true })
    this.eventSource.onopen = () => {
      this.onConnectionChange?.(true)
    }
    this.eventSource.onmessage = (message) => {
      try {
        if (!message.data) {
          return
        }
        const event = JSON.parse(message.data) as AgentChatEvent
        if (event?.event) {
          this.onEvent(event)
        }
      } catch (error) {
        this.onError?.(error instanceof Error ? error.message : 'SSE 消息解析失败')
      }
    }
    this.eventSource.onerror = () => {
      const closed = this.eventSource?.readyState === EventSource.CLOSED
      if (closed) {
        this.onConnectionChange?.(false)
      }
    }
  }

  disconnect() {
    if (!this.eventSource) {
      return
    }
    this.eventSource.close()
    this.eventSource = null
    this.onConnectionChange?.(false)
  }

  private buildChatUrl(payload: AgentChatPayload) {
    const params = new URLSearchParams()
    params.set('agentId', payload.agentId)
    params.set('sessionId', payload.sessionId)
    params.set('message', payload.message)
    if (payload.lastReceivedEventSequence) {
      params.set('lastReceivedEventSequence', payload.lastReceivedEventSequence)
    }

    const snapshot = loadAuthSnapshot()
    if (snapshot?.token?.tokenName && snapshot.token.tokenValue) {
      params.set(snapshot.token.tokenName, snapshot.token.tokenValue)
    }

    return `${appConfig.apiBaseUrl}/sse/agent/chat?${params.toString()}`
  }
}
