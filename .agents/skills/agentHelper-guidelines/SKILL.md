---
name: agenthelper-guidelines
description: SpringAi/AgentHelper 项目仓库开发规范与结构约束。Use when working in this repository and you need project-specific guidance on module placement, code style, API conventions, page location, comment requirements, validation expectations, or general implementation rules for new features, bug fixes, backend APIs, static pages, and module-level changes.
---

# AgentHelper Guidelines

按以下规则在本仓库内开发，不要按通用 Spring Boot 项目习惯自行发挥。

## Repo Layout

这是一个多模块 Maven 工程，根目录主要模块如下：

- `quickStart`: 启动模块，页面入口、静态资源、Web 配置集中在这里
- `common`: 公共常量、异常、通用配置、公共响应对象
- `core`: 核心能力
- `agent`: Agent 相关业务
- `vectorStore`: 向量存储能力
- `user`: 用户、租户、认证相关业务
- `graph`: 工作流/图相关能力
- `hooks`, `interceptors`, `tools`, `websocket`, `a2a`: 扩展能力模块
- `docs`: 项目文档
- `ui`: 前端实验区或独立前端资源，不默认作为当前运行入口

优先遵守“能力归属模块”原则：

- 通用能力放 `common`
- 明确业务能力放对应业务模块
- 启动接入、页面映射、静态资源放 `quickStart`

不要为了图省事把所有代码塞进 `quickStart`。

## Backend Rules

默认后端接口使用：

- `@RestController`
- `jakarta.annotation.Resource` 注入
- `ApiResponse.success(...)` / `ApiResponse.fail(...)` 作为统一返回包装

重要约束：

- `quickStart` 中 `ApiPrefixWebConfig` 会给所有 `@RestController` 统一添加 `/agentHelper` 前缀
- 写接口时只声明模块自己的相对路径，不要重复手写 `/agentHelper`
- 公共异常优先复用全局异常处理，不要随意吞异常
- Controller 只做参数接收和结果返回，业务逻辑放 Service
- 参数校验要前置，空值、非法范围、边界值都要处理

典型放置方式：

- `controller`: 对外接口
- `service`: 服务接口
- `service/impl`: 服务实现
- `domain/request`、`domain/response`: 入参与返回对象
- `config`: 模块配置
- `exception`: 模块异常

## Page Rules

如果需求是“加页面”或“写页面代码”，默认按这个仓库现状处理：

- 运行入口页面优先放 `quickStart/src/main/resources/static`
- 页面访问入口优先在 `quickStart/src/main/java/.../web` 下增加一个显式 `PageController`
- Controller 返回 `ClassPathResource("static/xxx.html")`
- 页面请求地址使用带前缀的真实接口：`/agentHelper/...`

不要默认引入新的前端框架。除非用户明确要求，否则优先：

- 单文件静态页
- 原生 HTML + CSS + JS
- 可直接被当前 Spring Boot 服务托管访问

## Style Rules

代码风格按以下要求执行：

- Java 缩进 4 空格
- HTML/CSS/JS 缩进 2 空格
- 变量、方法、类名语义化命名
- 变量/函数使用小驼峰
- 常量使用全大写下划线
- 禁止 `a`、`b`、`tmp`、`test1` 这类无语义命名
- 单个函数职责单一，不要一个方法里混太多层逻辑
- 结构清晰，避免超长方法和超长代码行

新增代码时优先保持与现有模块目录和包结构一致。

## Comment Rules

如果用户要求中文注释，必须满足：

- 文件顶部写明：文件用途、作者、创建时间、核心功能
- 公共方法上方写中文注释
- 复杂逻辑前写中文说明
- 页面样式按区域写模块化注释

不要写无意义注释：

- 不要写“定义变量”“执行方法”这种废话
- 不要中英文混乱
- 不要只写一句“TODO”就结束

## Data And Response Rules

返回对象优先单独建类，不要直接返回裸 `Map`，除非是临时聚合且结构非常简单。

接口设计优先：

- 入参清晰
- 返回结构稳定
- 错误信息可读
- 便于前端直接消费

如果已有响应风格，继续复用已有模式，不要引入第二套风格。

## Validation Rules

提交前至少做这些检查：

1. 确认代码放在正确模块
2. 确认接口路径不重复加 `/agentHelper`
3. 确认页面资源路径与 `ClassPathResource` 一致
4. 确认空状态、失败状态、边界值已处理
5. 确认没有明显未使用代码和未实现逻辑
6. 能编译时尽量编译；不能编译时明确说明阻塞原因

项目当前技术栈基于：

- Spring Boot `3.5.8`
- Spring AI `1.1.2`
- 多模块 Maven

因此运行/编译时应使用兼容的较新 JDK。不要按 Java 8 项目处理。

## Don’ts

不要做这些事：

- 不要擅自改动无关模块
- 不要随意重构大面积已有代码，除非用户明确要求
- 不要引入与当前仓库不一致的新目录层级
- 不要为了一个简单页面上 Vue/React 工程
- 不要写半成品代码、占位代码、伪实现逻辑
- 不要忽略中文文档/注释要求
- 不要重复造公共能力，先检查 `common` 和现有模块

## Default Workflow

在本仓库做需求时，默认按这个顺序：

1. 先判断功能归属哪个模块
2. 查现有 controller/service/domain/style 结构
3. 复用现有公共常量、响应对象、异常处理方式
4. 后端接口与页面接线都落到可运行状态
5. 做基础自检并说明验证结果与阻塞项

如果用户只说“按本项目风格开发”，就以本 skill 作为默认约束执行。
