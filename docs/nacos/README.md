# Nacos 配置导入说明

以下文件用于第一阶段微服务拆分，可在 Nacos 控制台按 Data ID 创建配置。

| Data ID | Group | Format | 说明 |
| --- | --- | --- | --- |
| `gateway-service-dev.yml` | `DEFAULT_GROUP` | YAML | 网关端口、路由、基础鉴权白名单 |
| `statistics-service-dev.yml` | `DEFAULT_GROUP` | YAML | 统计服务端口与 Redis 连接 |
| `agenthelper-common-dev.yml` | `DEFAULT_GROUP` | YAML | 服务公共监控与日志配置 |

如果使用非 public 命名空间，启动服务时通过环境变量 `NACOS_NAMESPACE` 传入命名空间 ID。