import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { AgentChatEvent, AgentChatPayload } from '@/types/agent'
import { loadAuthSnapshot } from '@/utils/storage'

interface WebSocketPushMessage<T> {
  event: string
  sessionId: string
  destination: string
  data: T
  timestamp: number
}

interface AgentChatSocketOptions {
  sessionId: string
  onEvent: (event: AgentChatEvent) => void
  onConnectionChange?: (connected: boolean) => void
  onError?: (message: string) => void
}

export class AgentChatSocket {
  private readonly client: Client
  private readonly sessionId: string
  private readonly onEvent: (event: AgentChatEvent) => void
  private readonly onConnectionChange?: (connected: boolean) => void
  private readonly onError?: (message: string) => void

  constructor(options: AgentChatSocketOptions) {
    this.sessionId = options.sessionId
    this.onEvent = options.onEvent
    this.onConnectionChange = options.onConnectionChange
    this.onError = options.onError

    this.client = new Client({
      connectHeaders: this.buildConnectHeaders(),
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        this.onConnectionChange?.(true)
        this.client.subscribe(`/topic/session/${this.sessionId}`, (message) => {
          try {
            const payload = JSON.parse(message.body) as WebSocketPushMessage<AgentChatEvent>
            this.onEvent(payload.data)
          } catch (error) {
            this.onError?.(error instanceof Error ? error.message : 'WebSocket 消息解析失败')
          }
        })
      },
      onStompError: (frame) => {
        this.onConnectionChange?.(false)
        this.onError?.(frame.headers.message || 'WebSocket STOMP 错误')
      },
      onWebSocketClose: () => {
        this.onConnectionChange?.(false)
      },
      onWebSocketError: () => {
        this.onConnectionChange?.(false)
        this.onError?.('WebSocket 连接异常')
      },
    })
  }

  connect() {
    if (!this.client.active) {
      this.client.activate()
    }
  }

  disconnect() {
    if (this.client.active) {
      void this.client.deactivate()
    }
  }

  send(payload: AgentChatPayload) {
    if (!this.client.connected) {
      throw new Error('WebSocket 尚未连接')
    }

    this.client.publish({
      destination: '/app/agent/chat',
      body: JSON.stringify(payload),
    })
  }

  /**
   * 构建 STOMP CONNECT 头，显式透传前端登录 token。
   */
  private buildConnectHeaders(): Record<string, string> {
    const snapshot = loadAuthSnapshot()
    if (!snapshot?.token?.tokenName || !snapshot.token.tokenValue) {
      return {}
    }
    return {
      [snapshot.token.tokenName]: snapshot.token.tokenValue,
    }
  }
}
