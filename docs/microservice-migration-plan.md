# AgentHelper 微服务改造方案

## 背景

当前项目是 Maven 多模块、单 Spring Boot 进程运行形态，`quickStart` 通过组件扫描把 `com.spring.ai.*` 下的业务模块全部装配到同一个应用中。后续需要演进为基于 Nacos 的微服务架构，由网关统一鉴权和路由，服务间通过 OpenFeign 调用。

## 目标架构

```text
UI / 外部调用
  -> gateway-service
      -> user-service
      -> core-service
      -> agent-service
      -> prompt-service
      -> tools-service
      -> skills-service
      -> hooks-service
      -> interceptor-service
      -> vector-store-service
      -> bigfile-service
      -> statistics-service
      -> link-service
      -> mcp-service
      -> graph-service
      -> a2a-service
      -> opencv-service
      -> websocket-service
```

Nacos 承担两类职责：

- 服务发现与注册：所有可独立运行的业务服务和网关注册到 Nacos。
- 配置中心：按服务维护配置，例如 `gateway-service-dev.yml`、`statistics-service-dev.yml`。

## 模块分类

### 网关模块

新增 `gateway` 模块，服务名 `gateway-service`。

职责：

- 统一入口 `/agentHelper/**`
- 统一鉴权、白名单、Token 校验
- 统一 CORS、限流、TraceId、用户上下文透传
- 通过 Nacos 服务发现路由到下游服务

### 独立服务模块

| 当前模块 | 服务名 | 说明 |
| --- | --- | --- |
| `user` | `user-service` | 用户、租户、登录、Token |
| `core` | `core-service` | 模型供应商、模型配置、密钥 |
| `agent` | `agent-service` | 智能体、会话、运行编排 |
| `prompt` | `prompt-service` | 提示词模板 |
| `tools` | `tools-service` | 工具管理 |
| `skills` | `skills-service` | Skill 管理 |
| `hooks` | `hooks-service` | Hook 管理 |
| `interceptors` | `interceptor-service` | 拦截器管理 |
| `vectorStore` | `vector-store-service` | 向量库、文档切片、检索 |
| `bigfile` | `bigfile-service` | 大文件分片上传、合并 |
| `statistics` | `statistics-service` | PV/VV/UV/IP 统计 |
| `link` | `link-service` | 短链 |
| `mcp` | `mcp-service` | MCP 管理 |
| `graph` | `graph-service` | 工作流/图 |
| `a2a` | `a2a-service` | A2A 协同 |
| `opencv` | `opencv-service` | OpenCV 识别 |
| `websocket` | `websocket-service` | WebSocket 长连接 |

### 公共依赖模块

- `common`：公共响应、异常、工具、共享 DTO。后续需要逐步减少 Repository 聚合。
- `logging`：日志能力。
- `quickStart`：过渡期保留单体启动能力，最终弱化或移除。

## Gateway 鉴权设计

网关负责统一鉴权，下游业务服务不直接面向外部暴露。

建议白名单：

```text
/agentHelper/user/login
/agentHelper/user/register
/agentHelper/s/**
/agentHelper/statistics/track
```

鉴权流程：

1. Gateway 从请求头读取 Token。
2. 校验 Token 是否有效。
3. 解析用户、租户、角色等上下文。
4. 将上下文通过 Header 透传给下游服务：

```text
X-User-Id
X-Tenant-Id
X-Username
X-User-Roles
X-Trace-Id
```

第一阶段可先搭建 Gateway 路由，鉴权过滤器后续接入 `user-service`。

## 服务间调用设计

服务间调用统一使用 OpenFeign。

外部接口：

```text
/statistics/**
/big-files/**
/vectorStore/**
```

内部接口建议使用：

```text
/internal/**
```

示例：

```java
@FeignClient(name = "bigfile-service", contextId = "bigFileFeignClient")
public interface BigFileFeignClient {
    @GetMapping("/internal/big-files/{fileId}/resource")
    ApiResponse<BigFileResourceResponse> getCompletedFile(@PathVariable String fileId);
}
```

## 数据拆分策略

分阶段推进：

1. 共享数据库，先完成服务独立运行和注册发现。
2. 按服务逐步迁移表到独立 schema。
3. 清理跨服务 Repository 直接访问，改为 OpenFeign/API 调用。

## 配置中心设计

推荐按服务拆配置：

```text
common-dev.yml
redis-dev.yml
mysql-dev.yml
gateway-service-dev.yml
statistics-service-dev.yml
```

服务本地 `application.yml` 只保留启动必需配置和 Nacos import，业务配置放 Nacos。

## 迁移路线

### 第一阶段：基础设施与试点服务

- 新增 `gateway` 模块。
- `gateway-service` 注册到 Nacos。
- `statistics` 模块增加独立启动类，注册为 `statistics-service`。
- Gateway 配置 `/agentHelper/statistics/**` 路由到 `statistics-service`。
- 保留 `quickStart` 单体启动能力。

### 第二阶段：用户与鉴权

- 拆 `user-service`。
- Gateway 接入 Token 校验。
- 下游服务通过 Header 获取用户上下文。

### 第三阶段：低耦合业务拆分

优先拆：`link`、`bigfile`、`prompt`、`tools`、`skills`。

### 第四阶段：核心业务拆分

拆：`core`、`agent`、`vectorStore`、`graph`、`a2a`、`mcp`。

### 第五阶段：清理单体遗留

- 弱化或移除 `quickStart`。
- 服务间调用改用 OpenFeign。
- 将属于具体服务的 Repository 从 `common` 迁回服务内。

## 第一阶段验收标准

- Nacos 控制台能看到 `gateway-service` 和 `statistics-service`。
- 访问 Gateway 的 `/agentHelper/statistics/overview` 能路由到 `statistics-service`。
- 前端可继续通过 `/agentHelper/statistics/**` 访问统计接口。
- `quickStart` 仍可作为过渡期单体入口启动。
## 第一阶段代码落地

- 新增 `gateway` 模块，作为统一入口服务注册到 Nacos，默认端口 `18080`。
- 网关通过 `lb://statistics-service` 转发 `/agentHelper/statistics/**`，下游仍保持模块内 `/statistics/**` 路径。
- 网关新增基础鉴权过滤器，当前支持免登录路径白名单和 `Authorization` 请求头占位校验，后续迁移用户登录态与权限规则。
- `statistics` 模块新增独立启动入口，作为 `statistics-service` 注册到 Nacos，默认端口 `18081`。
- `statistics` 模块补齐 Web、Redis、Nacos Discovery、Nacos Config、OpenFeign 依赖，保留 quickStart 聚合运行方式，第一阶段不破坏单体启动。
## Nacos 配置文件

- 本地 `application.yml` 只保留服务名、环境、Nacos 地址和配置导入，不再放端口、Redis、路由等运行配置。
- `docs/nacos/gateway-service-dev.yml` 用于部署网关端口、路由和基础鉴权白名单。
- `docs/nacos/statistics-service-dev.yml` 用于部署统计服务端口和 Redis 连接。
- `docs/nacos/agenthelper-common-dev.yml` 用于部署各服务共用的监控和日志配置。
- 微服务启动类只扫描本服务包，避免网关或统计服务加载 `common.repository` 等单体业务 Bean。
## Gateway WebFlux 注意事项

- 父 POM 当前存在全局 `spring-boot-starter-web`，网关必须显式设置 `spring.main.web-application-type=reactive`。
- 网关启动类中也强制设置 `WebApplicationType.REACTIVE`，避免 Spring MVC 类路径影响 Gateway 启动。
- 后续模块化改造时，建议逐步把父 POM 的全局业务依赖下沉到各服务模块，减少微服务之间的无关依赖继承。
