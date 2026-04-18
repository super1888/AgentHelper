-- Skills 管理模块建表脚本
-- 覆盖：技能主表、版本表、测试用例表、执行日志表

DROP TABLE IF EXISTS `skill_execution_log_record`;
DROP TABLE IF EXISTS `skill_test_case_record`;
DROP TABLE IF EXISTS `skill_version_record`;
DROP TABLE IF EXISTS `skill_record`;

CREATE TABLE `skill_record`
(
    `id`                   BIGINT       NOT NULL COMMENT '主键ID',
    `skill_code`           VARCHAR(64)  NOT NULL COMMENT '技能编码',
    `skill_name`           VARCHAR(128) NOT NULL COMMENT '技能名称',
    `description`          VARCHAR(500)          DEFAULT NULL COMMENT '技能描述',
    `skill_type`           VARCHAR(64)  NOT NULL COMMENT '技能类型',
    `skill_category`       VARCHAR(64)  NOT NULL COMMENT '技能分类',
    `skill_status`         VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '技能状态 ENABLED/DISABLED',
    `publish_status`       VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态 DRAFT/TESTING/PRE_RELEASE/PUBLISHED/OFFLINE',
    `version_mode`         VARCHAR(32)  NOT NULL DEFAULT 'MANUAL' COMMENT '版本模式 MANUAL/AUTO',
    `sort_weight`          INT          NOT NULL DEFAULT 0 COMMENT '排序权重',
    `current_version_no`   INT          NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `latest_version_no`    INT          NOT NULL DEFAULT 1 COMMENT '最新版本号',
    `published_version_no` INT                   DEFAULT NULL COMMENT '已发布版本号',
    `hot_update_enabled`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否启用热更新',
    `deleted_flag`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否逻辑删除 0-否 1-是',
    `tenant_id`            BIGINT       NOT NULL COMMENT '租户ID',
    `owner_user_id`        BIGINT       NOT NULL COMMENT '负责人用户ID',
    `owner_user_name`      VARCHAR(64)           DEFAULT NULL COMMENT '负责人名称',
    `ext`                  LONGTEXT              DEFAULT NULL COMMENT '技能完整快照 JSON',
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
    KEY `idx_skill_record_publish` (`tenant_id`, `publish_status`),
    KEY `idx_skill_record_deleted` (`tenant_id`, `deleted_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='技能主表';

CREATE TABLE `skill_version_record`
(
    `id`                  BIGINT       NOT NULL COMMENT '主键ID',
    `skill_id`            BIGINT       NOT NULL COMMENT '技能ID',
    `skill_code`          VARCHAR(64)  NOT NULL COMMENT '技能编码快照',
    `skill_name`          VARCHAR(128) NOT NULL COMMENT '技能名称快照',
    `tenant_id`           BIGINT       NOT NULL COMMENT '租户ID',
    `version_no`          INT          NOT NULL COMMENT '版本号',
    `version_code`        VARCHAR(32)  NOT NULL COMMENT '语义化版本号',
    `version_description` VARCHAR(500)          DEFAULT NULL COMMENT '版本说明',
    `version_status`      VARCHAR(32)  NOT NULL COMMENT '版本状态 CURRENT/HISTORY/ROLLBACK',
    `publish_status`      VARCHAR(32)  NOT NULL COMMENT '发布状态',
    `release_stage`       VARCHAR(32)           DEFAULT NULL COMMENT '发布阶段',
    `snapshot_json`       LONGTEXT              DEFAULT NULL COMMENT '版本快照 JSON',
    `ext`                 TEXT                  DEFAULT NULL COMMENT '扩展字段',
    `remark`              VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`           BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`         VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`         DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`           BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`         VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`         DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`             INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_skill_version_record` (`skill_id`, `version_no`),
    KEY `idx_skill_version_tenant` (`tenant_id`, `skill_id`),
    KEY `idx_skill_version_publish` (`tenant_id`, `publish_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='技能版本表';

CREATE TABLE `skill_test_case_record`
(
    `id`                        BIGINT       NOT NULL COMMENT '主键ID',
    `skill_id`                  BIGINT       NOT NULL COMMENT '技能ID',
    `skill_code`                VARCHAR(64)  NOT NULL COMMENT '技能编码',
    `case_name`                 VARCHAR(128) NOT NULL COMMENT '测试用例名称',
    `input_text`                VARCHAR(1000)         DEFAULT NULL COMMENT '测试输入问句',
    `slot_payload_json`         LONGTEXT              DEFAULT NULL COMMENT '槽位参数 JSON',
    `expected_intent`           VARCHAR(64)           DEFAULT NULL COMMENT '预期命中意图',
    `expected_success`          TINYINT      NOT NULL DEFAULT 1 COMMENT '预期是否成功',
    `expected_response_contains` VARCHAR(255)         DEFAULT NULL COMMENT '预期响应包含内容',
    `channel_code`              VARCHAR(64)           DEFAULT NULL COMMENT '渠道编码',
    `locale`                    VARCHAR(32)           DEFAULT NULL COMMENT '语言区域',
    `enabled`                   TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `last_run_status`           VARCHAR(32)           DEFAULT NULL COMMENT '最近运行状态',
    `last_run_duration_ms`      BIGINT                DEFAULT NULL COMMENT '最近耗时毫秒',
    `last_run_at`               DATETIME              DEFAULT NULL COMMENT '最近运行时间',
    `last_result_json`          LONGTEXT              DEFAULT NULL COMMENT '最近运行结果 JSON',
    `tenant_id`                 BIGINT       NOT NULL COMMENT '租户ID',
    `ext`                       TEXT                  DEFAULT NULL COMMENT '扩展字段',
    `remark`                    VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`                 BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`               VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`               DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`                 BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`               VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`               DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`                   INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_skill_test_case_skill` (`tenant_id`, `skill_id`),
    KEY `idx_skill_test_case_enabled` (`tenant_id`, `enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='技能测试用例表';

CREATE TABLE `skill_execution_log_record`
(
    `id`                   BIGINT       NOT NULL COMMENT '主键ID',
    `skill_id`             BIGINT                DEFAULT NULL COMMENT '技能ID',
    `skill_code`           VARCHAR(64)           DEFAULT NULL COMMENT '技能编码',
    `skill_name`           VARCHAR(128)          DEFAULT NULL COMMENT '技能名称',
    `tenant_id`            BIGINT       NOT NULL COMMENT '租户ID',
    `source_type`          VARCHAR(32)  NOT NULL COMMENT '日志来源 DEBUG/TEST/RUNTIME',
    `source_id`            BIGINT                DEFAULT NULL COMMENT '来源对象ID',
    `trace_id`             VARCHAR(64)           DEFAULT NULL COMMENT '链路追踪ID',
    `session_code`         VARCHAR(64)           DEFAULT NULL COMMENT '会话编号',
    `channel_code`         VARCHAR(64)           DEFAULT NULL COMMENT '渠道编码',
    `locale`               VARCHAR(32)           DEFAULT NULL COMMENT '语言区域',
    `input_text`           VARCHAR(1000)         DEFAULT NULL COMMENT '输入文本',
    `matched_intent`       VARCHAR(64)           DEFAULT NULL COMMENT '命中意图',
    `confidence_score`     DECIMAL(8, 4)         DEFAULT NULL COMMENT '置信度',
    `slot_payload_json`    LONGTEXT              DEFAULT NULL COMMENT '槽位参数 JSON',
    `context_payload_json` LONGTEXT              DEFAULT NULL COMMENT '上下文 JSON',
    `request_payload_json` LONGTEXT              DEFAULT NULL COMMENT '请求快照 JSON',
    `response_payload_json` LONGTEXT             DEFAULT NULL COMMENT '响应快照 JSON',
    `trace_payload_json`   LONGTEXT              DEFAULT NULL COMMENT '执行链路 JSON',
    `execute_status`       VARCHAR(32)           DEFAULT NULL COMMENT '执行状态',
    `success_flag`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否成功',
    `elapsed_ms`           BIGINT                DEFAULT NULL COMMENT '耗时毫秒',
    `failure_reason`       VARCHAR(500)          DEFAULT NULL COMMENT '失败原因',
    `satisfaction_level`   INT                   DEFAULT NULL COMMENT '满意度',
    `operator_user_id`     BIGINT                DEFAULT NULL COMMENT '操作人ID',
    `operator_user_name`   VARCHAR(64)           DEFAULT NULL COMMENT '操作人名称',
    `ext`                  TEXT                  DEFAULT NULL COMMENT '扩展字段',
    `remark`               VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_id`            BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `create_name`          VARCHAR(64)           DEFAULT NULL COMMENT '创建人名称',
    `create_time`          DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_id`            BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_name`          VARCHAR(64)           DEFAULT NULL COMMENT '更新人名称',
    `update_time`          DATETIME              DEFAULT NULL COMMENT '更新时间',
    `version`              INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_skill_execution_log_skill` (`tenant_id`, `skill_id`),
    KEY `idx_skill_execution_log_source` (`tenant_id`, `source_type`),
    KEY `idx_skill_execution_log_success` (`tenant_id`, `success_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='技能执行日志表';
