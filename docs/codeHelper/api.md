# codeHelper API 文档

`codeHelper` 是项目中的独立 Java 编程助手模块，负责围绕本地工作区进行会话管理、上下文构建、模型决策、受控工具调用和工具审计。

## 1. 设计目标

- 保持 `codeHelper` 与 `agent`、`a2a`、`tools` 的职责边界清晰。
- 让模型只负责“判断下一步做什么”，真正的文件和命令执行交给 `tools` 模块。
- 对高风险操作进行显式确认，避免模型误删文件或执行危险命令。
- 记录会话、事件和工具日志，便于回放和调试。

## 2. 基础约定

- 所有接口统一前缀：`/agentHelper`
- `codeHelper` 模块接口前缀：`/agentHelper/code-helper`
- 请求和响应统一使用 JSON
- 需要登录态，依赖当前用户上下文和租户上下文
- 工作区路径必须在允许的本地目录内
- 编程助手模型来自 `core` 模块已启用的模型配置，可在页面中自由切换供应商和模型

## 3. 数据库表

先执行 `docs/sql/code_helper.sql` 创建以下表：

- `code_helper_session`
- `code_helper_session_event`
- `code_helper_tool_log`

## 4. 接口清单

### 4.1 创建会话

`POST /agentHelper/code-helper/sessions`

请求示例：

```json
{
  "sessionName": "订单模块梳理",
  "workspacePath": "D:/code/springAi",
  "projectName": "springAi",
  "branchName": "main",
  "taskDescription": "分析 a2a 模块入口并给出改造建议",
  "modelCode": "gpt-4.1",
  "allowedCommands": ["mvn", "git", "java", "gradlew"]
}
```

说明：

- `workspacePath` 必填。
- `modelCode` 可选，不填时会使用配置默认值或规则兜底。
- `allowedCommands` 用于限制 `shell` 工具的命令白名单。

---

### 4.2 查询会话列表

`GET /agentHelper/code-helper/sessions`

返回当前租户下的会话列表。

---

### 4.3 发送消息

`POST /agentHelper/code-helper/sessions/send?sessionId=xxx`

请求示例：

```json
{
  "content": "帮我查找 a2a 模块的 Controller，并说明入口链路",
  "modelCode": "gpt-4.1",
  "autoToolCall": true
}
```

说明：

- `content` 必填。
- 模型会返回结构化 JSON 决策。
- 低风险工具会自动执行。
- `shell`、`git_status`、`git_diff` 等高风险工具会被标记为需要确认。

---

### 4.4 查看上下文

`GET /agentHelper/code-helper/context?sessionId=xxx`

返回：

- 会话摘要
- 最近消息
- 任务列表

---

### 4.5 压缩上下文

`POST /agentHelper/code-helper/context/compact?sessionId=xxx`

请求体可选：

```json
{
  "summaryHint": "保留 Controller、Service 和异常相关上下文"
}
```

说明：

- 会把历史消息和任务压缩成更短的摘要，降低上下文长度。

---

### 4.6 查看系统提示词

`GET /agentHelper/code-helper/prompt?sessionId=xxx`

用于调试模型为什么会做出某个工具选择。

---

### 4.7 查询工具清单

`GET /agentHelper/code-helper/tools`

返回内置工具列表，例如：

- `read_file`
- `write_file`
- `edit_file`
- `list_directory`
- `glob`
- `grep`
- `shell`
- `git_status`
- `git_diff`
- `todo_update`
- `compact_context`

---

### 4.8 查询编程助手可用模型

`GET /agentHelper/code-helper/models/options`

返回 `core` 模块中当前租户已启用的模型选项，包括：

- `modelCode`：模型配置编码，创建会话和发送消息时传入
- `modelName`：模型名称
- `providerEnum`：供应商枚举
- `providerName`：供应商配置名称
- `modelIdentifier`：供应商侧模型标识
- `defaultModel`：是否默认模型

说明：

- 前端编程助手页面会使用该接口渲染模型下拉框。
- 创建会话时传入 `modelCode`，该会话后续默认使用这个模型。
- 发送单条消息时也可以传入 `modelCode`，用于临时切换本次调用模型。
- 如果创建会话未传 `modelCode`，后端优先使用 `agent-helper.code-helper.default-model-code`，再回退到 `core` 的默认启用模型。

---

### 4.9 显式执行工具

`POST /agentHelper/code-helper/tool/execute`

请求示例：

```json
{
  "sessionId": "xxx",
  "toolName": "grep",
  "arguments": {
    "keyword": "Controller"
  },
  "allowedCommands": ["mvn", "git", "java"]
}
```

说明：

- 用于确认后执行，或者前端手动执行工具。
- 会写入工具日志。

---

### 4.10 查询工具日志

`GET /agentHelper/code-helper/tool/logs?sessionId=xxx`

返回该会话的工具执行记录。

---

### 4.11 权限检查

`POST /agentHelper/code-helper/permission/check`

请求示例：

```json
{
  "toolName": "shell",
  "workspacePath": "D:/code/springAi",
  "command": "mvn test",
  "allowedCommands": ["mvn", "git", "java"]
}
```

返回：

- 是否允许执行
- 风险等级
- 拒绝原因

## 5. 模型输出协议

模型需要输出 JSON，对应字段如下：

```json
{
  "assistantReply": "我会先搜索相关 Controller。",
  "requireConfirmation": false,
  "toolCalls": [
    {
      "toolName": "grep",
      "arguments": {
        "keyword": "Controller"
      }
    }
  ]
}
```

字段说明：

- `assistantReply`：给用户看的自然语言回复
- `requireConfirmation`：是否需要确认高风险工具
- `toolCalls`：工具调用数组
- `toolCalls[].toolName`：工具名称
- `toolCalls[].arguments`：工具参数

## 6. 工具执行原理

`codeHelper` 本身不直接做文件读写和命令执行，而是调用 `tools` 模块中的工作区工具执行器。

执行流程：

1. 前端创建会话，绑定工作区。
2. 用户输入任务。
3. `codeHelper` 生成系统提示词。
4. 按会话 `modelCode` 或本次消息 `modelCode` 调用 `core` 动态创建对应供应商的 `ChatModel`。
5. 模型输出 JSON 决策。
6. `codeHelper` 规范化工具参数并检查风险。
7. 低风险工具自动执行，高风险工具要求确认。
8. 每次工具执行都会写入日志。

## 7. 前端页面

页面入口：`/code-helper`

页面能力：

- 创建和切换会话
- 发送消息
- 查看提示词
- 查看上下文
- 查看工具清单
- 显式执行工具
- 查看工具日志

## 8. 常见使用建议

- 先创建会话，再发送消息。
- 先用 `grep`、`list_directory` 观察项目结构，再做修改。
- 修改前尽量使用 `read_file` / `edit_file` 组合。
- 对 `shell` 命令保持谨慎，优先走白名单。
- 如果模型输出内容不是 JSON，系统会降级为普通回复。
