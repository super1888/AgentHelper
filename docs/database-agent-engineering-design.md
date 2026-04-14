# Agent 工程化数据库设计

## 1. 当前阶段目标

当前先解决 Agent 工程化的第一步：设计基础用户表，并为后续 Agent、Session、AgentVersion 等表打好统一规范。

本阶段约定：

- 使用 `MyBatis + MyBatis-Plus`
- 主键使用雪花算法，业务层主动生成
- 数据删除统一使用逻辑删除
- 公共字段统一下沉到基础实体

## 2. 统一建表规范

后续业务表建议统一包含这些字段：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 雪花算法主键 |
| `create_time` | datetime | 创建时间 |
| `update_time` | datetime | 更新时间 |
| `create_by` | bigint | 创建人 ID |
| `update_by` | bigint | 更新人 ID |
| `delete_flag` | tinyint | 逻辑删除标记，0-未删除，1-已删除 |

说明：

- `id` 使用 `IdType.INPUT`
- `delete_flag` 使用 `@TableLogic`
- `create_time/update_time` 后续建议由 `MetaObjectHandler` 自动填充

## 3. 为什么先设计用户表

Agent 工程化里，Agent 一定要绑定“谁创建的、谁可见、谁可使用、归属哪个组织”。

所以用户表是整个后续设计的起点。

后续这些表都会依赖用户：

- `ai_agent`
- `ai_agent_version`
- `ai_agent_session`
- `ai_agent_message`
- `ai_tenant`
- `ai_role`

## 4. 用户表设计

表名建议：

```sql
ai_user
```

字段设计如下：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 主键，雪花 ID |
| `username` | varchar(64) | 登录账号，建议唯一 |
| `nickname` | varchar(64) | 显示名称 |
| `phone` | varchar(32) | 手机号 |
| `email` | varchar(128) | 邮箱 |
| `password_hash` | varchar(255) | 密码摘要 |
| `status` | tinyint | 状态，1-启用，0-禁用 |
| `tenant_id` | bigint | 所属租户 ID，单体阶段可为空 |
| `remark` | varchar(255) | 备注 |
| `create_time` | datetime | 创建时间 |
| `update_time` | datetime | 更新时间 |
| `create_by` | bigint | 创建人 |
| `update_by` | bigint | 更新人 |
| `delete_flag` | tinyint | 逻辑删除 |

## 5. 用户表 DDL

```sql
CREATE TABLE `ai_user` (
  `id` bigint NOT NULL COMMENT '主键，雪花算法生成',
  `username` varchar(64) NOT NULL COMMENT '登录账号',
  `nickname` varchar(64) DEFAULT NULL COMMENT '显示名称',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `password_hash` varchar(255) DEFAULT NULL COMMENT '密码摘要',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_user_username` (`username`),
  KEY `idx_ai_user_phone` (`phone`),
  KEY `idx_ai_user_email` (`email`),
  KEY `idx_ai_user_tenant_id` (`tenant_id`),
  KEY `idx_ai_user_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent系统用户表';
```

## 6. 本项目里的代码落点

### 6.1 common 模块

新增：

- 雪花算法配置类
- 雪花 ID 生成器
- 公共基础实体 `BaseEntity`

适合后续复用到：

- `user`
- `agent`
- `graph`
- `vectorStore`

### 6.2 user 模块

新增：

- `UserEntity`
- `UserMapper`

这一步先完成“表结构 + 实体 + Mapper”骨架，后续你再补：

- Service
- Controller
- 登录/注册
- 租户/角色插件

## 7. 后续 Agent 表建议

等用户表稳定后，建议按顺序继续设计：

1. `ai_agent`
   作用：保存 Agent 基础信息
2. `ai_agent_version`
   作用：保存 Agent 配置版本
3. `ai_agent_session`
   作用：保存会话信息
4. `ai_agent_message`
   作用：保存消息记录
5. `ai_agent_publish_record`
   作用：保存发布记录和启停状态

## 8. 下一步建议

你现在可以按这个顺序继续推进：

1. 补 MyBatis-Plus 的 `MetaObjectHandler`
2. 补用户 Service 和插入示例
3. 设计 `ai_agent` 主表
4. 设计 `ai_agent_version` 表
5. 让 `SimpleAgentRegistry` 过渡到数据库版本
