# 日志模块运维说明

## 1. 模块目标

当前项目已接入统一企业级日志模块，覆盖以下场景：

- 应用运行日志
- 调试日志
- 错误日志
- HTTP 访问日志
- 全局异常日志
- SQL 执行日志
- 慢 SQL 专项日志

所有日志均支持按时间和大小滚动，适合线上问题定位与磁盘容量控制。

## 2. 日志目录

日志根目录由 `app.logging.path` 控制，示例：

```yaml
app:
  logging:
    path: /data/agent-helper/logs
```

输出目录结构示例：

```text
/data/agent-helper/logs/AgentHelper/
  app.log
  debug.log
  error.log
  access.log
  exception.log
  sql.log
  slow-sql.log
  archive/
```

## 3. 核心配置

```yaml
app:
  logging:
    app-name: AgentHelper
    path: ./logs
    console-enabled: true
    rolling:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
      clean-history-on-start: false
    trace:
      header-name: X-Trace-Id
      response-header-enabled: true
    access:
      enabled: true
      console-enabled: false
      log-request-parameters: true
      log-request-headers: false
      max-body-length: 1000
      exclude-path-prefixes:
        - /favicon.ico
        - /error
        - /actuator
    exception:
      console-enabled: true
    sql:
      enabled: false
      console-enabled: false
      slow-threshold-ms: 1000
      log-parameters: true
      max-sql-length: 4000

logging:
  level:
    root: INFO
    com.spring.ai: INFO
    org.springframework.web: INFO
    com.baomidou: INFO
```

## 4. 配置项说明

- `app.logging.path`：日志文件根目录
- `app.logging.console-enabled`：是否输出控制台日志
- `app.logging.rolling.max-file-size`：单文件最大大小
- `app.logging.rolling.max-history`：归档保留天数
- `app.logging.rolling.total-size-cap`：日志总大小上限
- `app.logging.trace.header-name`：链路标识请求头名称
- `app.logging.access.enabled`：是否开启 HTTP 访问日志
- `app.logging.access.console-enabled`：是否在控制台输出访问日志
- `app.logging.access.log-request-headers`：默认关闭，避免敏感头落盘
- `app.logging.exception.console-enabled`：是否在控制台输出异常日志
- `app.logging.sql.enabled`：是否打印 SQL 内容
- `app.logging.sql.console-enabled`：是否在控制台输出 SQL
- `app.logging.sql.slow-threshold-ms`：慢 SQL 阈值
- `app.mybatis-plus.enable-sql-log`：是否挂载 SQL 日志拦截器，保留原配置兼容

## 5. 各日志文件用途

- `app.log`：正常业务主日志
- `debug.log`：仅 DEBUG 级别
- `error.log`：所有 ERROR 级别日志
- `access.log`：请求方法、路径、状态码、耗时、客户端 IP、traceId
- `exception.log`：统一异常日志，区分业务异常、参数异常、系统异常
- `sql.log`：SQL 执行日志
- `slow-sql.log`：超过阈值的慢 SQL

## 6. 推荐配置

### 开发环境

```yaml
app:
  logging:
    path: ./logs
    console-enabled: true
    sql:
      enabled: true
      slow-threshold-ms: 300
  mybatis-plus:
    enable-sql-log: true

logging:
  level:
    root: INFO
    com.spring.ai: DEBUG
```

### 生产环境

```yaml
app:
  logging:
    path: /data/agent-helper/logs
    console-enabled: false
    rolling:
      max-file-size: 200MB
      max-history: 30
      total-size-cap: 10GB
    access:
      log-request-headers: false
    sql:
      enabled: true
      slow-threshold-ms: 800
  mybatis-plus:
    enable-sql-log: true

logging:
  level:
    root: INFO
    com.spring.ai: INFO
    org.springframework.web: WARN
    com.baomidou: WARN
```

## 7. 运维排查建议

### HTTP 问题排查

1. 从调用方响应头中拿到 `X-Trace-Id`
2. 在 `access.log` 中搜索该 `traceId`
3. 再到 `exception.log`、`error.log` 中定位同一链路异常

### SQL 性能问题排查

1. 优先查看 `slow-sql.log`
2. 根据 `sqlId` 找到对应 Mapper 方法
3. 结合数据库执行计划排查索引、回表、全表扫描

## 8. 注意事项

- `access.log` 默认不记录请求头，避免 `Authorization`、`Cookie` 等敏感信息落盘
- 多文件日志输出会增加磁盘占用，生产环境建议显式配置 `total-size-cap`
- 如需彻底关闭控制台输出，将 `app.logging.console-enabled` 设为 `false`


mvn -pl quickStart -am package
mvn -Pface-platform-linux-x86_64 -pl quickStart -am package
mvn -Pface-platform-linux-aarch64 -pl quickStart -am package
mvn -Pface-platform-macos-aarch64 -pl quickStart -am package
