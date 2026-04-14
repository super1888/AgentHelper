# springAi Agent 系统学习路线图

## 1. 文档目标

这份文档不是简单罗列概念，而是基于当前 `D:/code/springAi` 项目的真实结构，给出一份可执行的学习路线，帮助你从“已经学了很多 Agent 相关知识”走向“能系统设计、实现、评估和交付 Agent 系统”。

文档重点覆盖：

- 你在当前项目里已经掌握的能力
- 还缺少但非常关键的知识面
- 建议优先学习顺序
- 每一类能力在本项目里的落点
- MCP 的系统学习方向
- 接下来适合动手做的专题项目

---

## 2. 先说结论

从当前项目看，你已经不属于“Agent 初学者”了。

你已经接触并实现过这些关键能力：

- 多模型接入
- 多种 Agent 形态
- Tool 调用
- Hook 和 Interceptor 扩展
- Graph / Workflow
- RAG / Vector Store
- WebSocket 实时交互
- A2A 思路

你现在最需要补的，不是再多看几个 Agent demo，而是进入下面这三个层次：

1. 从“会搭 Agent”升级到“会做可运行的 Agent 系统”
2. 从“会写样例”升级到“会做稳定、可观测、可评估的工程”
3. 从“会调单 Agent”升级到“会设计多 Agent、MCP、Memory、Governance 的完整体系”

---

## 3. 当前项目能力盘点

### 3.1 模块结构

当前项目已经包含这些模块：

- `agent`
- `core`
- `tools`
- `hooks`
- `interceptors`
- `graph`
- `vectorStore`
- `skills`
- `a2a`
- `websocket`
- `quickStart`
- `common`

这意味着你已经把 Agent 生态中的大多数核心拼图都碰到了。

### 3.2 你已经掌握的部分

#### A. Agent 基础构建

你已经掌握：

- Agent DTO 设计
- Agent 工厂模式
- ReactAgent 封装
- 顺序、并行、路由、监督者等 Agent 形态

项目落点：

- [agent](D:/code/springAi/agent/src/main/java/com/spring/ai/agent)
- [AgentFactory.java](D:/code/springAi/agent/src/main/java/com/spring/ai/agent/factory/AgentFactory.java)
- [CustomAgent.java](D:/code/springAi/agent/src/main/java/com/spring/ai/agent/factory/impl/CustomAgent.java)

#### B. Tool 调用体系

你已经掌握：

- 方法工具注册
- 工具工厂
- AgentTool
- 自定义工具设计

项目落点：

- [tools](D:/code/springAi/tools/src/main/java/com/spring/ai/tools)

典型工具包括：

- Calculator
- Weather
- Python
- WebSearch
- DocumentSearch
- DatabaseQuery
- Email

#### C. Hook / Interceptor 扩展机制

你已经掌握：

- 前后处理扩展点
- 消息裁剪
- RAG 上下文增强
- 文本过滤
- 模型限流
- 工具重试/监控/缓存

项目落点：

- [hooks](D:/code/springAi/hooks/src/main/java/com/spring/ai/hooks)
- [interceptors](D:/code/springAi/interceptors/src/main/java/com/spring/ai/interceptors)

#### D. Graph / Workflow

你已经掌握：

- 节点式工作流
- 审批流
- 并行/条件分支
- checkpoint
- threadId
- human-in-the-loop 的基础思路

项目落点：

- [graph](D:/code/springAi/graph/src/main/java/com/spring/ai/graph)

#### E. RAG / 向量检索

你已经掌握：

- 文档上传
- 多格式解析
- 向量存储
- 相似度检索
- RAG 组件化调用

项目落点：

- [vectorStore](D:/code/springAi/vectorStore/src/main/java/com/spring/ai/vectorstore)
- [modular-rag-guide.md](D:/code/springAi/docs/modular-rag-guide.md)

#### F. 实时交互和前端接入

你已经掌握：

- WebSocket + STOMP
- Agent 会话流式推送
- 前端聊天页面
- 通过会话 ID 订阅和发送消息

项目落点：

- [websocket](D:/code/springAi/websocket/src/main/java/com/spring/ai/websocket)
- [SimpleAgentChatService.java](D:/code/springAi/agent/src/main/java/com/spring/ai/agent/service/SimpleAgentChatService.java)
- [agent-studio.html](D:/code/springAi/quickStart/src/main/resources/static/agent-studio.html)

#### G. 大量实验性学习

你已经做了非常多实验，这一点很重要。

项目落点：

- [QuickStartApplicationTests.java](D:/code/springAi/quickStart/src/test/java/com/spring/quickstart/QuickStartApplicationTests.java)

这说明你已经不是纸上学习，而是进入了“边学边试、边做边理解”的阶段。

---

## 4. 你还需要系统补齐的知识地图

下面这些内容，才是你下一阶段真正应该补的。

---

## 5. 学习主题一：Agent 工程化

### 5.1 为什么要学

很多人会做 Agent demo，但做不出能长期运行的 Agent 系统。

你的项目现在已经具备功能拼装能力，但还缺这些工程化能力：

- Agent 配置持久化
- Agent 生命周期管理
- 会话状态管理
- 多租户隔离
- 权限控制
- 配置版本化
- 失败恢复

### 5.2 你需要掌握什么

- Agent 定义如何存库
- 用户创建的 Agent 如何持久保存
- Agent 配置变更如何版本管理
- 一个会话如何绑定到某个 Agent 版本
- 失败任务如何恢复
- WebSocket 会话断开后如何重连和补发

### 5.3 本项目落点建议

- `agent`：保存 Agent 元信息和运行配置
- `user`：绑定用户/组织/角色
- `common`：统一状态模型和错误模型
- `websocket`：会话状态同步

### 5.4 动手建议

建议先做：

1. 给 `SimpleAgentRegistry` 增加数据库持久化版本
2. 给会话增加 `session` 表
3. 增加 Agent 发布、启停、版本切换能力
4. 增加会话历史查询接口

---

## 6. 学习主题二：评测与可观测性

### 6.1 为什么要学

你现在已经能把 Agent 跑起来，但还不等于“知道它好不好、稳不稳、贵不贵”。

真正的 Agent 工程，需要知道：

- 回复质量怎么样
- 哪一步经常失败
- 哪个工具最慢
- 哪个 prompt 改动导致质量下降
- 每轮对话消耗多少 token 和成本

### 6.2 你需要掌握什么

- 离线评测集设计
- 自动回归测试
- prompt 版本比较
- tool 成功率和耗时统计
- latency tracing
- token usage 统计
- failure case 回放

### 6.3 本项目落点建议

- `interceptors`：打指标、收 trace、记日志
- `hooks`：插入调试事件
- `common`：统一 traceId、requestId
- `websocket`：把关键运行事件同步到前端

### 6.4 动手建议

先做最小闭环：

1. 给每次 Agent 调用生成 traceId
2. 记录模型耗时、工具耗时、token 数
3. 把失败样本保存下来
4. 建一个简单评测集目录，定期回归

---

## 7. 学习主题三：Memory 与长期记忆

### 7.1 为什么要学

很多 Agent 项目做着做着就发现：

- 上下文太长
- 历史消息太乱
- 记忆污染
- 旧记忆和新指令冲突

你现在已经接触到 checkpoint 和 memory，但还需要系统理解“记忆体系设计”。

### 7.2 你需要掌握什么

- 短期记忆和长期记忆的区别
- 会话历史压缩
- 摘要记忆
- 用户画像记忆
- 事实记忆和偏好记忆
- 记忆写入策略
- 记忆清理和遗忘策略

### 7.3 本项目落点建议

- `graph`：短期状态、checkpoint
- `vectorStore`：长期语义记忆
- `user`：用户画像与偏好
- `hooks`：自动摘要与记忆写入

### 7.4 动手建议

建议按顺序做：

1. 会话结束自动生成摘要
2. 摘要写入长期记忆
3. 给用户建立偏好档案
4. 下一次会话自动注入偏好和历史摘要

---

## 8. 学习主题四：多 Agent 编排

### 8.1 为什么要学

你已经有多 Agent 的类型定义，但还需要深入理解“什么时候用哪种编排方式”。

### 8.2 你需要掌握什么

- Planner / Executor 模式
- Reviewer / Critic / Judge 模式
- Router Agent 模式
- Supervisor 模式
- 并行子任务聚合
- Agent 间共享状态设计
- 防循环、防空转、防重复调用

### 8.3 本项目落点建议

- `agent`：继续扩展多 Agent 工厂
- `graph`：把多 Agent 编排落成真正工作流
- `tools`：把某些 Agent 包装成 AgentTool

### 8.4 动手建议

建议做一个“三角色协作 Agent”：

- Planner：拆任务
- Worker：执行
- Reviewer：检查

然后对比它与单 Agent 的效果差异。

---

## 9. 学习主题五：安全、治理与 Human-in-the-Loop

### 9.1 为什么要学

你已经接触了 TextFilter、HITL、审批流，但这一块还需要从“特性”升级成“治理体系”。

### 9.2 你需要掌握什么

- Prompt Injection 防护
- Tool 权限隔离
- 高风险操作审批
- PII 识别与脱敏
- 输出合规性检查
- 审计日志
- 会话回溯和责任归因

### 9.3 本项目落点建议

- `hooks`：审批前置、敏感信息检测
- `interceptors`：输入输出治理
- `graph`：人工审批流
- `common`：审计模型

### 9.4 动手建议

优先做：

1. 高风险工具调用前强制审批
2. 所有工具调用写审计日志
3. 敏感词、PII、越权请求拦截

---

## 10. 学习主题六：前端交互协议与产品化体验

### 10.1 为什么要学

你已经开始做 WebSocket 前端，但要做真正的产品，还要补交互协议和会话体验。

### 10.2 你需要掌握什么

- 统一事件协议
- 流式增量渲染
- 断线重连
- 会话恢复
- 消息幂等
- 历史消息拉取
- 前端展示 reasoning/tool call/status

### 10.3 本项目落点建议

- `websocket`：统一推送协议
- `agent`：聊天服务
- `quickStart`：前端页面

### 10.4 动手建议

建议继续升级 `agent-studio.html`：

- 增加历史会话列表
- 增加重连机制
- 增加消息恢复
- 增加 tool / reasoning 折叠展示

---

## 11. 学习主题七：MCP

### 11.1 为什么 MCP 必须学

你自己也已经意识到了，MCP 是你当前知识体系里的明显缺口。

MCP 不只是“多一个协议”，它本质上会改变 Agent 集成外部能力的方式。

传统做法里，工具通常直接写在本地服务中。
MCP 的思路是：

- 外部能力独立成服务
- Agent 通过标准协议发现和调用这些能力
- 工具能力可以跨语言、跨进程、跨团队复用

### 11.2 MCP 要掌握的核心问题

你至少要回答清楚这几个问题：

- MCP 和普通 Tool 的区别是什么
- MCP Server 的职责是什么
- MCP Client 的职责是什么
- 能力是如何声明的
- 工具如何被发现
- 参数 Schema 如何定义
- 调用结果如何返回
- 如何做鉴权、限流和隔离

### 11.3 你应该学习的 MCP 内容

#### A. 协议层

- MCP 基本概念
- Client / Server 模式
- Resource / Tool / Prompt 的区别
- JSON-RPC 风格通信方式

#### B. 工具接入层

- 把已有工具改造成 MCP Server
- Agent 侧如何注册 MCP Client
- 多个 MCP Server 如何管理

#### C. 工程治理

- MCP Server 鉴权
- 能力目录管理
- 工具版本管理
- 失败重试与降级
- 日志与审计

### 11.4 本项目落点建议

你现在项目里已经有一些适合承接 MCP 的位置：

- `tools`：本地工具能力
- `agent`：Agent 编排与调用入口
- `a2a`：跨 Agent 调用思路
- `skills`：技能与能力注入

一个很自然的升级方向是：

1. 保留本地 Tool
2. 新增 MCP Tool Adapter
3. 把某些外部能力改成 MCP Server
4. 让 Agent 通过统一入口同时调用本地 Tool 和 MCP Tool

### 11.5 MCP 建议动手项目

你可以按这个顺序做：

1. 做一个最简单的 MCP Server
   - 提供天气查询或时间查询
2. 在 `agent` 模块里实现 MCP Client 适配层
3. 让现有 Agent 同时支持：
   - 本地 Tool
   - MCP Tool
4. 做一个 MCP Server 注册表
5. 做一个可视化能力目录页

---

## 12. 学习主题八：A2A

你项目里已经有 `a2a` 模块，这说明你已经碰到了“Agent 调 Agent”的思路。

但这一块还值得继续深入：

- A2A 与 MCP 的边界
- A2A 更适合什么场景
- MCP 更适合什么场景
- AgentCard / 能力发现
- 远程 Agent 编排

简单理解：

- MCP 更偏“标准化工具能力接入”
- A2A 更偏“Agent 之间的能力协作”

建议你后续把 MCP 和 A2A 放在一起比较学习。

---

## 13. 推荐学习顺序

如果按收益和实战价值排序，我建议你这样学：

### 第一阶段：把现有能力做实

1. Agent 工程化
2. WebSocket 会话闭环
3. 日志、指标、Tracing
4. 历史会话和状态持久化

### 第二阶段：把系统做稳

1. 评测体系
2. Memory 体系
3. 安全治理
4. Human-in-the-loop

### 第三阶段：把能力做深

1. 多 Agent 编排
2. MCP
3. A2A
4. 更复杂的 Graph Workflow

### 第四阶段：把产品做强

1. 前端体验升级
2. 可视化 Agent 配置台
3. 可视化工具目录
4. 可视化工作流编排台

---

## 14. 推荐你接下来做的 6 个专题项目

### 专题 1：Agent 配置持久化平台

目标：

- 用户创建 Agent
- 配置保存到数据库
- 支持发布和版本切换

### 专题 2：带历史记忆的实时聊天

目标：

- WebSocket 对话
- 历史消息恢复
- 摘要记忆
- 用户偏好记忆

### 专题 3：Agent 评测中心

目标：

- 建评测样本集
- 支持 prompt 对比
- 输出成功率/耗时/token 消耗

### 专题 4：多 Agent 协作任务系统

目标：

- Planner / Worker / Reviewer
- 并行子任务
- 汇总与评分

### 专题 5：MCP 集成平台

目标：

- 接入 1 到 2 个 MCP Server
- 做统一注册和调用入口
- 支持 Agent 调 MCP Tool

### 专题 6：治理与审批系统

目标：

- 高风险工具调用审批
- 审计日志
- 敏感信息治理

---

## 15. 建议阅读与实践方式

建议你以后不要再按“学一个模块、学一个 API”的方式推进，而要按下面方式：

### 15.1 每学一个主题，都回答这四个问题

1. 它解决什么问题
2. 它和已有模块是什么关系
3. 它在系统里的边界是什么
4. 它如何落成可运行代码

### 15.2 每学一个主题，都做三件事

1. 写一个最小 demo
2. 接入当前工程
3. 写一篇自己的总结文档

### 15.3 每学一个主题，都沉淀这三类产物

- 代码
- 文档
- 测试样例

---

## 16. 最后建议

你当前最值得继续投入的方向，不是再横向多看几个 Agent 框架，而是纵向把下面三件事做深：

1. 可观测与评测
2. Memory 与状态管理
3. MCP 与多 Agent 编排

因为你现在已经具备“搭能力”的基础了，下一步真正决定上限的是：

- 你能不能把系统做稳
- 你能不能把质量做可测
- 你能不能把外部能力做标准化接入

---

## 17. 针对你当前项目的下一步建议

如果基于这个仓库继续推进，我建议你下一轮按这个顺序动手：

1. 清理并稳定当前 `websocket + agent-studio + simple-agent-chat` 闭环
2. 给 Agent 和 Session 加持久化
3. 给会话加 trace、metrics、审计日志
4. 给当前 Tool 体系增加 MCP 接入层
5. 做一个最小 MCP Server
6. 做多 Agent 评测样本和回归测试

如果你愿意，我下一步可以继续帮你把这份文档再拆成：

- 初级阶段学习计划
- 中级阶段学习计划
- 高级阶段学习计划

或者直接帮你生成一份“未来 8 周学习与开发排期表”。  
