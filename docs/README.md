# WebSocket 模块接入说明

## 模块目标

`websocket` 模块用于统一处理服务端到前端的实时消息推送，适合以下场景：

- Agent 流式输出 token
- 长任务执行过程中的阶段性进度通知
- 指定业务方法的开始、结束、异常事件自动推送
- 方法内部按需推送增量结果，而不是一次性返回

## 设计说明

模块提供两类能力：

- `@WebSocketPush`
  作用：标记哪些业务方法需要自动推送生命周期事件
- `WebSocketPushService`
  作用：在方法内部手动推送增量消息，比如流式 token、步骤状态、工具调用结果

默认情况下，模块会把同一次业务调用绑定到一个 `sessionId`，并将消息推送到：

```text
/topic/session/{sessionId}
```

## 接入步骤

### 1. 引入模块

启动模块或业务模块依赖：

```xml
<dependency>
    <groupId>com.spring.ai</groupId>
    <artifactId>websocket</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置 WebSocket 参数

可在 `application.yml` 中加入：

```yaml
app:
  websocket:
    enabled: true
    endpoint: /ws
    broker-destination-prefix: /topic
    app-destination-prefix: /app
    session-destination-prefix: /topic/session
    allowed-origin-patterns: "*"
```

说明：

- `endpoint`：前端建立 WebSocket/SockJS 连接的入口
- `session-destination-prefix`：会话消息默认主题前缀
- `enabled`：是否启用模块推送能力

### 3. 在业务方法上打注解

```java
@WebSocketPush(sessionId = "#request.sessionId")
public ChatResult chat(ChatRequest request) {
    return doChat(request);
}
```

上面这个写法会自动推送：

- `METHOD_START`
- `METHOD_RESULT`
- `METHOD_ERROR`

如果你想自定义目标地址，也可以这样写：

```java
@WebSocketPush(
        destination = "'/topic/custom/' + #request.sessionId",
        sessionId = "#request.sessionId"
)
public ChatResult chat(ChatRequest request) {
    return doChat(request);
}
```

## 手动推送增量消息

如果一个方法内部要持续输出过程消息，可以注入 `WebSocketPushService`：

```java
@Service
public class AgentChatService {

    private final WebSocketPushService webSocketPushService;

    public AgentChatService(WebSocketPushService webSocketPushService) {
        this.webSocketPushService = webSocketPushService;
    }

    @WebSocketPush(sessionId = "#sessionId", sendResult = false)
    public String streamReply(String sessionId, String userInput) {
        webSocketPushService.sendToSession(sessionId, "STREAM_START", "开始生成");
        webSocketPushService.sendToSession(sessionId, "STREAM_TOKEN", "你好");
        webSocketPushService.sendToSession(sessionId, "STREAM_TOKEN", "，");
        webSocketPushService.sendToSession(sessionId, "STREAM_TOKEN", "这是增量内容");
        webSocketPushService.sendToSession(sessionId, "STREAM_FINISH", "生成完成");
        return "done";
    }
}
```

适用建议：

- 想要自动推送方法开始/结束/异常：用 `@WebSocketPush`
- 想要推送流式 token 或步骤状态：用 `WebSocketPushService`
- 两者通常一起用

## 前端接入示例

前端先建立连接，再按 `sessionId` 订阅主题。

### 连接地址

```text
/ws
```

### 订阅地址

```text
/topic/session/{sessionId}
```

### 消息格式

服务端统一发送的数据结构为：

```json
{
  "event": "STREAM_TOKEN",
  "sessionId": "chat-001",
  "destination": "/topic/session/chat-001",
  "data": "你好",
  "timestamp": 1770000000000
}
```

## 推荐接入模式

### 场景 1：普通业务方法自动推送

适合接口执行时间较长，但不需要真正流式分片的场景。

做法：

- 给方法加 `@WebSocketPush`
- 前端监听 `METHOD_START`、`METHOD_RESULT`、`METHOD_ERROR`

### 场景 2：Agent 流式输出

适合模型 token、思考内容、工具调用过程同步到前端。

做法：

- 外层方法加 `@WebSocketPush(sessionId = "...", sendResult = false)`
- 在 `ReactAgent.stream(...)` 的订阅过程中调用 `webSocketPushService.sendToSession(...)`
- 自定义事件名，例如：
  - `STREAM_TOKEN`
  - `STREAM_REASONING`
  - `TOOL_FINISHED`
  - `STREAM_FINISH`

## 备注

- 当前模块已从 `quickStart` 中剥离，为独立模块
- 当前环境未完成编译校验，因为本机默认 `java` 版本仍是 1.8，而项目要求 Java 21
- 如果后续你要把现有某个 Agent 会话接口接进来，建议直接把 `ReactAgent.stream(...)` 的事件映射到本模块的 `sendToSession(...)`
