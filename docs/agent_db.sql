DROP TABLE IF EXISTS `agent_task`;
DROP TABLE IF EXISTS `agent_session_event`;
DROP TABLE IF EXISTS `agent_session`;
DROP TABLE IF EXISTS `agent_version`;
DROP TABLE IF EXISTS `agent`;
DROP TABLE IF EXISTS `sy_user`;
DROP TABLE IF EXISTS `sy_tenant`;
DROP TABLE IF EXISTS `vector_store_file`;
DROP TABLE IF EXISTS `prompt_template`;
DROP TABLE IF EXISTS `skill_version_record`;
DROP TABLE IF EXISTS `skill_record`;

CREATE TABLE `sy_tenant`
(
    `id`              BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_code`     VARCHAR(64)  NOT NULL COMMENT '租户编码',
    `tenant_name`     VARCHAR(128) NOT NULL COMMENT '租户名称',
    `status`          INT          NOT NULL DEFAULT 1 COMMENT '租户状态 1-启用 0-禁用',
    `is_default`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认租户 1-是 0-否',
    `owner_user_id`   BIGINT                DEFAULT NULL COMMENT '默认租户归属用户ID',
    `owner_user_name` VARCHAR(64)           DEFAULT NULL COMMENT '默认租户归属用户名',
    `contact_name`    VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    `contact_phone`   VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    `description`     VARCHAR(500)          DEFAULT NULL COMMENT '租户描述',
    `ext`             TEXT                  DEFAULT NULL COMMENT '扩展字段',
    `remark`          VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`       BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`     DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`       BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`     VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`     DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sy_tenant_code` (`tenant_code`),
    KEY `idx_sy_tenant_status` (`status`),
    KEY `idx_sy_tenant_owner_default` (`owner_user_id`, `is_default`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='租户表';

CREATE TABLE `sy_user`
(
    `id`            BIGINT NOT NULL COMMENT '主键ID',
    `username`      VARCHAR(64)     DEFAULT NULL COMMENT '登录账号',
    `nickname`      VARCHAR(64)     DEFAULT NULL COMMENT '用户显示名称',
    `phone`         VARCHAR(32)     DEFAULT NULL COMMENT '手机号',
    `email`         VARCHAR(128)    DEFAULT NULL COMMENT '邮箱',
    `password_hash` VARCHAR(255)    DEFAULT NULL COMMENT '密码摘要',
    `status`        INT    NOT NULL DEFAULT 1 COMMENT '用户状态 1-启用 0-禁用',
    `tenant_id`     BIGINT          DEFAULT NULL COMMENT '租户ID',
    `ext`           TEXT            DEFAULT NULL COMMENT '扩展字段',
    `remark`        VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    `create_id`     BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `create_name`   VARCHAR(64)     DEFAULT NULL COMMENT '创建人名称',
    `create_time`   DATETIME        DEFAULT NULL COMMENT '创建时间',
    `update_id`     BIGINT          DEFAULT NULL COMMENT '更新人ID',
    `update_name`   VARCHAR(64)     DEFAULT NULL COMMENT '更新人名称',
    `update_time`   DATETIME        DEFAULT NULL COMMENT '更新时间',
    `version`       INT    NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`),
    KEY `idx_sy_user_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户表';

CREATE TABLE `agent`
(
    `id`                   BIGINT       NOT NULL COMMENT '主键ID',
    `agent_code`           VARCHAR(64)  NOT NULL COMMENT 'Agent 外部编码',
    `agent_name`           VARCHAR(128) NOT NULL COMMENT 'Agent 名称',
    `description`          VARCHAR(500)          DEFAULT NULL COMMENT 'Agent 描述',
    `agent_type`           VARCHAR(32)  NOT NULL COMMENT 'Agent 类型',
    `agent_status`         VARCHAR(32)  NOT NULL COMMENT 'Agent 状态 DRAFT/PUBLISHED/DISABLED',
    `tenant_id`            BIGINT                DEFAULT NULL COMMENT '租户ID',
    `owner_user_id`        BIGINT       NOT NULL COMMENT '创建人用户ID',
    `owner_user_name`      VARCHAR(64)           DEFAULT NULL COMMENT '创建人用户名',
    `current_version_id`   BIGINT                DEFAULT NULL COMMENT '当前版本ID',
    `published_version_id` BIGINT                DEFAULT NULL COMMENT '已发布版本ID',
    `published_version_no` INT                   DEFAULT NULL COMMENT '已发布版本号',
    `latest_version_no`    INT          NOT NULL DEFAULT 0 COMMENT '最新版本号',
    `ext`                  TEXT                  DEFAULT NULL COMMENT '扩展字段',
    `remark`               VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`            BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`          VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`          DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`            BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`          VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`          DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`              INT          NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sy_agent_code` (`agent_code`),
    KEY `idx_sy_agent_tenant_owner` (`tenant_id`, `owner_user_id`),
    KEY `idx_sy_agent_status` (`tenant_id`, `agent_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Agent 定义表';

CREATE TABLE `agent_version`
(
    `id`                         BIGINT       NOT NULL COMMENT '主键ID',
    `agent_id`                   BIGINT       NOT NULL COMMENT 'Agent ID',
    `tenant_id`                  BIGINT                DEFAULT NULL COMMENT '租户ID',
    `version_no`                 INT          NOT NULL COMMENT '版本号',
    `agent_name`                 VARCHAR(128) NOT NULL COMMENT 'Agent 名称快照',
    `description`                VARCHAR(500)          DEFAULT NULL COMMENT '描述快照',
    `system_prompt`              TEXT                  DEFAULT NULL COMMENT '系统提示词快照',
    `selected_capabilities_json` TEXT                  DEFAULT NULL COMMENT '能力配置 JSON',
    `config_snapshot_json`       LONGTEXT              DEFAULT NULL COMMENT '完整配置快照 JSON',
    `is_published`               TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已发布 1-是 0-否',
    `ext`                        TEXT                  DEFAULT NULL COMMENT '扩展字段',
    `remark`                     VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`                  BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`                VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`                DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`                  BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`                VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`                DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`                    INT          NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sy_agent_version` (`agent_id`, `version_no`),
    KEY `idx_sy_agent_version_tenant` (`tenant_id`, `agent_id`),
    KEY `idx_sy_agent_version_publish` (`tenant_id`, `is_published`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Agent 版本表';

CREATE TABLE `agent_session`
(
    `id`                     BIGINT      NOT NULL COMMENT '主键ID',
    `session_code`           VARCHAR(64) NOT NULL COMMENT '会话外部编码',
    `agent_id`               BIGINT      NOT NULL COMMENT 'Agent ID',
    `agent_code`             VARCHAR(64) NOT NULL COMMENT 'Agent 外部编码',
    `agent_version_id`       BIGINT      NOT NULL COMMENT '绑定版本ID',
    `agent_version_no`       INT         NOT NULL COMMENT '绑定版本号',
    `tenant_id`              BIGINT               DEFAULT NULL COMMENT '租户ID',
    `owner_user_id`          BIGINT      NOT NULL COMMENT '会话所属用户ID',
    `owner_user_name`        VARCHAR(64)          DEFAULT NULL COMMENT '会话所属用户名',
    `session_status`         VARCHAR(32) NOT NULL COMMENT '会话状态 ACTIVE/CLOSED/FAILED',
    `connection_status`      VARCHAR(32) NOT NULL COMMENT '连接状态 CONNECTED/DISCONNECTED',
    `last_event_sequence`    BIGINT      NOT NULL DEFAULT 0 COMMENT '最后事件序号',
    `last_user_message`      TEXT                 DEFAULT NULL COMMENT '最后一条用户消息',
    `last_assistant_message` LONGTEXT             DEFAULT NULL COMMENT '最后一条助手消息',
    `last_connected_time`    DATETIME             DEFAULT NULL COMMENT '最后连接时间',
    `last_disconnected_time` DATETIME             DEFAULT NULL COMMENT '最后断开时间',
    `ext`                    TEXT                 DEFAULT NULL COMMENT '扩展字段',
    `remark`                 VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_id`              BIGINT               DEFAULT NULL COMMENT '创建人ID',
    `create_name`            VARCHAR(64)          DEFAULT NULL COMMENT '创建人名称',
    `create_time`            DATETIME             DEFAULT NULL COMMENT '创建时间',
    `update_id`              BIGINT               DEFAULT NULL COMMENT '更新人ID',
    `update_name`            VARCHAR(64)          DEFAULT NULL COMMENT '更新人名称',
    `update_time`            DATETIME             DEFAULT NULL COMMENT '更新时间',
    `version`                INT         NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sy_agent_session_code` (`session_code`),
    KEY `idx_sy_agent_session_owner` (`tenant_id`, `owner_user_id`),
    KEY `idx_sy_agent_session_agent` (`tenant_id`, `agent_id`, `agent_version_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Agent 会话表';

CREATE TABLE `agent_session_event`
(
    `id`               BIGINT      NOT NULL COMMENT '主键ID',
    `session_id`       BIGINT      NOT NULL COMMENT '会话ID',
    `session_code`     VARCHAR(64) NOT NULL COMMENT '会话外部编码',
    `agent_id`         BIGINT      NOT NULL COMMENT 'Agent ID',
    `agent_version_id` BIGINT      NOT NULL COMMENT '版本ID',
    `tenant_id`        BIGINT               DEFAULT NULL COMMENT '租户ID',
    `task_id`          BIGINT               DEFAULT NULL COMMENT '关联任务ID',
    `event_sequence`   BIGINT      NOT NULL COMMENT '事件序号',
    `event_type`       VARCHAR(64) NOT NULL COMMENT '事件类型',
    `event_body`       LONGTEXT             DEFAULT NULL COMMENT '事件内容',
    `replayable`       TINYINT     NOT NULL DEFAULT 1 COMMENT '是否允许补发 1-是 0-否',
    `ext`              TEXT                 DEFAULT NULL COMMENT '扩展字段',
    `remark`           VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_id`        BIGINT               DEFAULT NULL COMMENT '创建人ID',
    `create_name`      VARCHAR(64)          DEFAULT NULL COMMENT '创建人名称',
    `create_time`      DATETIME             DEFAULT NULL COMMENT '创建时间',
    `update_id`        BIGINT               DEFAULT NULL COMMENT '更新人ID',
    `update_name`      VARCHAR(64)          DEFAULT NULL COMMENT '更新人名称',
    `update_time`      DATETIME             DEFAULT NULL COMMENT '更新时间',
    `version`          INT         NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sy_agent_session_event` (`session_id`, `event_sequence`),
    KEY `idx_sy_agent_session_event_replay` (`tenant_id`, `session_id`, `replayable`),
    KEY `idx_sy_agent_session_event_task` (`tenant_id`, `task_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Agent 会话事件表';

CREATE TABLE `agent_task`
(
    `id`               BIGINT      NOT NULL COMMENT '主键ID',
    `task_code`        VARCHAR(64) NOT NULL COMMENT '任务外部编码',
    `source_task_id`   BIGINT               DEFAULT NULL COMMENT '来源失败任务ID',
    `session_id`       BIGINT      NOT NULL COMMENT '会话ID',
    `session_code`     VARCHAR(64) NOT NULL COMMENT '会话外部编码',
    `agent_id`         BIGINT      NOT NULL COMMENT 'Agent ID',
    `agent_version_id` BIGINT      NOT NULL COMMENT '版本ID',
    `tenant_id`        BIGINT               DEFAULT NULL COMMENT '租户ID',
    `owner_user_id`    BIGINT      NOT NULL COMMENT '任务所属用户ID',
    `task_status`      VARCHAR(32) NOT NULL COMMENT '任务状态 RUNNING/SUCCESS/FAILED',
    `request_message`  LONGTEXT             DEFAULT NULL COMMENT '请求消息',
    `response_message` LONGTEXT             DEFAULT NULL COMMENT '响应消息',
    `error_message`    LONGTEXT             DEFAULT NULL COMMENT '失败原因',
    `retry_count`      INT         NOT NULL DEFAULT 0 COMMENT '重试次数',
    `ext`              TEXT                 DEFAULT NULL COMMENT '扩展字段',
    `remark`           VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_id`        BIGINT               DEFAULT NULL COMMENT '创建人ID',
    `create_name`      VARCHAR(64)          DEFAULT NULL COMMENT '创建人名称',
    `create_time`      DATETIME             DEFAULT NULL COMMENT '创建时间',
    `update_id`        BIGINT               DEFAULT NULL COMMENT '更新人ID',
    `update_name`      VARCHAR(64)          DEFAULT NULL COMMENT '更新人名称',
    `update_time`      DATETIME             DEFAULT NULL COMMENT '更新时间',
    `version`          INT         NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sy_agent_task_code` (`task_code`),
    KEY `idx_sy_agent_task_session_status` (`tenant_id`, `session_id`, `task_status`),
    KEY `idx_sy_agent_task_owner` (`tenant_id`, `owner_user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Agent 任务表';

CREATE TABLE `prompt_template`
(
    `id`               BIGINT       NOT NULL COMMENT '主键ID',
    `template_code`    VARCHAR(64)  NOT NULL COMMENT '模板编码',
    `template_name`    VARCHAR(128) NOT NULL COMMENT '模板名称',
    `description`      VARCHAR(500)          DEFAULT NULL COMMENT '模板描述',
    `template_type`    VARCHAR(64)  NOT NULL COMMENT '模板类型',
    `source_type`      VARCHAR(32)  NOT NULL COMMENT '来源类型 INLINE_TEXT/FILE_PATH',
    `template_content` LONGTEXT              DEFAULT NULL COMMENT '模板内容快照',
    `source_path`      VARCHAR(512)          DEFAULT NULL COMMENT '文件路径',
    `template_status`  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '模板状态 ENABLED/DISABLED',
    `tenant_id`        BIGINT       NOT NULL COMMENT '租户ID',
    `owner_user_id`    BIGINT       NOT NULL COMMENT '创建人用户ID',
    `owner_user_name`  VARCHAR(64)           DEFAULT NULL COMMENT '创建人用户名称',
    `ext`              TEXT                  DEFAULT NULL COMMENT '扩展字段',
    `remark`           VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`        BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`      VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`      DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`        BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`      VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`      DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`          INT          NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prompt_template_code` (`tenant_id`, `template_code`),
    KEY `idx_prompt_template_status` (`tenant_id`, `template_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='提示词模板表';


CREATE TABLE `vector_store_file`
(
    `id`                     BIGINT       NOT NULL COMMENT '主键ID',
    `module_name`            VARCHAR(64)  NOT NULL COMMENT '模块名称',
    `file_name`              VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_extension`         VARCHAR(32)           DEFAULT NULL COMMENT '文件后缀',
    `content_type`           VARCHAR(128)          DEFAULT NULL COMMENT '内容类型',
    `file_size`              BIGINT                DEFAULT NULL COMMENT '文件大小',
    `source_document_count`  INT          NOT NULL DEFAULT 0 COMMENT '源文档数量',
    `chunk_count`            INT          NOT NULL DEFAULT 0 COMMENT '切片数量',
    `uploaded_at`            VARCHAR(64)           DEFAULT NULL COMMENT '上传时间',
    `store_status`           VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT '存储状态 ACTIVE/DELETED',
    `last_operation_message` VARCHAR(255)          DEFAULT NULL COMMENT '最近一次操作说明',
    `ext`                    TEXT                  DEFAULT NULL COMMENT '扩展字段',
    `remark`                 VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`              BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`            VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`            DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`              BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`            VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`            DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`                INT          NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vector_store_file` (`module_name`, `file_name`),
    KEY `idx_vector_store_file_status` (`module_name`, `store_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='向量文件台账表';



CREATE TABLE `skill_record`
(
    `id`                   BIGINT       NOT NULL COMMENT '主键ID',
    `skill_code`           VARCHAR(64)  NOT NULL COMMENT 'Skill 编码',
    `skill_name`           VARCHAR(128) NOT NULL COMMENT 'Skill 名称',
    `description`          VARCHAR(500)          DEFAULT NULL COMMENT 'Skill 描述',
    `skill_category`       VARCHAR(64)  NOT NULL COMMENT 'Skill 分类',
    `skill_status`         VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT 'Skill 状态 ENABLED/DISABLED',
    `publish_status`       VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态 DRAFT/PUBLISHED',
    `version_mode`         VARCHAR(32)  NOT NULL DEFAULT 'MANUAL' COMMENT '版本模式 MANUAL/AUTO',
    `current_version_no`   INT          NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `latest_version_no`    INT          NOT NULL DEFAULT 1 COMMENT '最新版本号',
    `published_version_no` INT                   DEFAULT NULL COMMENT '已发布版本号',
    `hot_update_enabled`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否开启热更新 1-是 0-否',
    `tenant_id`            BIGINT       NOT NULL COMMENT '租户ID',
    `owner_user_id`        BIGINT       NOT NULL COMMENT '负责人用户ID',
    `owner_user_name`      VARCHAR(64)           DEFAULT NULL COMMENT '负责人用户名',
    `ext`                  LONGTEXT              DEFAULT NULL COMMENT '完整配置快照 JSON',
    `remark`               VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`            BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`          VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`          DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`            BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`          VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`          DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`              INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_skill_record_code` (`tenant_id`, `skill_code`),
    KEY `idx_skill_record_status` (`tenant_id`, `skill_status`),
    KEY `idx_skill_record_publish` (`tenant_id`, `publish_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Skill 主表';

CREATE TABLE `skill_version_record`
(
    `id`             BIGINT       NOT NULL COMMENT '主键ID',
    `skill_id`       BIGINT       NOT NULL COMMENT 'Skill ID',
    `skill_code`     VARCHAR(64)  NOT NULL COMMENT 'Skill 编码快照',
    `skill_name`     VARCHAR(128) NOT NULL COMMENT 'Skill 名称快照',
    `tenant_id`      BIGINT       NOT NULL COMMENT '租户ID',
    `version_no`     INT          NOT NULL COMMENT '版本号',
    `version_status` VARCHAR(32)  NOT NULL COMMENT '版本状态 CURRENT/HISTORY',
    `publish_status` VARCHAR(32)  NOT NULL COMMENT '发布状态 DRAFT/PUBLISHED',
    `snapshot_json`  LONGTEXT              DEFAULT NULL COMMENT '版本快照 JSON',
    `ext`            TEXT                  DEFAULT NULL COMMENT '扩展字段',
    `remark`         VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`      BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`    VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`    DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`      BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`    VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`    DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_skill_version_record` (`skill_id`, `version_no`),
    KEY `idx_skill_version_tenant` (`tenant_id`, `skill_id`),
    KEY `idx_skill_version_publish` (`tenant_id`, `publish_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Skill 版本表';