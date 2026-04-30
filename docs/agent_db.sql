/*
 Navicat Premium Data Transfer

 Source Server         : redmi数据库
 Source Server Type    : MySQL
 Source Server Version : 80040
 Source Host           : localhost:3306
 Source Schema         : agent_db

 Target Server Type    : MySQL
 Target Server Version : 80040
 File Encoding         : 65001

 Date: 30/04/2026 13:42:04
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for a2a_agent_card_record
-- ----------------------------
DROP TABLE IF EXISTS `a2a_agent_card_record`;
CREATE TABLE `a2a_agent_card_record`  (
  `id` bigint(0) NOT NULL,
  `agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `endpoint_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `protocol_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '1.0',
  `transport_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'HTTP',
  `auth_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'NONE',
  `agent_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ENABLED',
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'DRAFT',
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'MEDIUM',
  `trust_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'INTERNAL',
  `owner_team` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `timeout_ms` int(0) NULL DEFAULT 10000,
  `rate_limit_qps` int(0) NULL DEFAULT 10,
  `success_rate_slo` int(0) NULL DEFAULT 99,
  `tenant_id` bigint(0) NOT NULL,
  `deleted_flag` tinyint(0) NULL DEFAULT 0,
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_id` bigint(0) NULL DEFAULT NULL,
  `create_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint(0) NULL DEFAULT NULL,
  `update_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `version` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_a2a_agent_code_tenant`(`tenant_id`, `agent_code`) USING BTREE,
  INDEX `idx_a2a_agent_publish`(`tenant_id`, `publish_status`, `agent_status`) USING BTREE,
  INDEX `idx_a2a_agent_owner`(`tenant_id`, `owner_team`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for a2a_execution_log_record
-- ----------------------------
DROP TABLE IF EXISTS `a2a_execution_log_record`;
CREATE TABLE `a2a_execution_log_record`  (
  `id` bigint(0) NOT NULL,
  `task_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `target_agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `route_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `execute_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `attempt_no` int(0) NULL DEFAULT NULL,
  `retry_index` int(0) NULL DEFAULT NULL,
  `success_flag` tinyint(0) NULL DEFAULT 0,
  `elapsed_ms` bigint(0) NULL DEFAULT NULL,
  `request_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `response_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `tenant_id` bigint(0) NOT NULL,
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_id` bigint(0) NULL DEFAULT NULL,
  `create_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint(0) NULL DEFAULT NULL,
  `update_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `version` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_a2a_log_task`(`tenant_id`, `task_code`, `create_time`) USING BTREE,
  INDEX `idx_a2a_log_attempt`(`tenant_id`, `task_code`, `attempt_no`, `retry_index`) USING BTREE,
  INDEX `idx_a2a_log_trace`(`trace_id`) USING BTREE,
  INDEX `idx_a2a_log_agent`(`tenant_id`, `target_agent_code`, `success_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for a2a_route_record
-- ----------------------------
DROP TABLE IF EXISTS `a2a_route_record`;
CREATE TABLE `a2a_route_record`  (
  `id` bigint(0) NOT NULL,
  `route_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `route_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `target_agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `task_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `route_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ENABLED',
  `priority_no` int(0) NULL DEFAULT 100,
  `failover_enabled` tinyint(0) NULL DEFAULT 0,
  `fallback_agent_codes` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `tenant_id` bigint(0) NOT NULL,
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_id` bigint(0) NULL DEFAULT NULL,
  `create_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint(0) NULL DEFAULT NULL,
  `update_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `version` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_a2a_route_code_tenant`(`tenant_id`, `route_code`) USING BTREE,
  INDEX `idx_a2a_route_match`(`tenant_id`, `route_status`, `task_type`, `source_agent_code`, `priority_no`) USING BTREE,
  INDEX `idx_a2a_route_target`(`tenant_id`, `target_agent_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for a2a_task_record
-- ----------------------------
DROP TABLE IF EXISTS `a2a_task_record`;
CREATE TABLE `a2a_task_record`  (
  `id` bigint(0) NOT NULL,
  `task_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `task_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `target_agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `route_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `task_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `response_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `elapsed_ms` bigint(0) NULL DEFAULT NULL,
  `tenant_id` bigint(0) NOT NULL,
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_id` bigint(0) NULL DEFAULT NULL,
  `create_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint(0) NULL DEFAULT NULL,
  `update_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `version` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_a2a_task_code_tenant`(`tenant_id`, `task_code`) USING BTREE,
  INDEX `idx_a2a_task_query`(`tenant_id`, `task_type`, `task_status`, `create_time`) USING BTREE,
  INDEX `idx_a2a_task_agent`(`tenant_id`, `source_agent_code`, `target_agent_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent
-- ----------------------------
DROP TABLE IF EXISTS `agent`;
CREATE TABLE `agent`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Agent 外部编码',
  `agent_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Agent 名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Agent 描述',
  `agent_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Agent 类型',
  `agent_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Agent 状态 DRAFT/PUBLISHED/DISABLED',
  `tenant_id` bigint(0) NULL DEFAULT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '创建人用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `current_version_id` bigint(0) NULL DEFAULT NULL COMMENT '当前版本ID',
  `published_version_id` bigint(0) NULL DEFAULT NULL COMMENT '已发布版本ID',
  `published_version_no` int(0) NULL DEFAULT NULL COMMENT '已发布版本号',
  `latest_version_no` int(0) NOT NULL DEFAULT 0 COMMENT '最新版本号',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sy_agent_code`(`agent_code`) USING BTREE,
  INDEX `idx_sy_agent_tenant_owner`(`tenant_id`, `owner_user_id`) USING BTREE,
  INDEX `idx_sy_agent_status`(`tenant_id`, `agent_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent 定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent_session
-- ----------------------------
DROP TABLE IF EXISTS `agent_session`;
CREATE TABLE `agent_session`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `session_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话外部编码',
  `agent_id` bigint(0) NOT NULL COMMENT 'Agent ID',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Agent 外部编码',
  `agent_version_id` bigint(0) NOT NULL COMMENT '绑定版本ID',
  `agent_version_no` int(0) NOT NULL COMMENT '绑定版本号',
  `tenant_id` bigint(0) NULL DEFAULT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '会话所属用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话所属用户名称',
  `session_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话状态 ACTIVE/CLOSED/FAILED',
  `connection_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '连接状态 CONNECTED/DISCONNECTED',
  `last_event_sequence` bigint(0) NOT NULL DEFAULT 0 COMMENT '最后事件序号',
  `last_user_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '最后一条用户消息',
  `last_assistant_message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '最后一条助手消息',
  `last_connected_time` datetime(0) NULL DEFAULT NULL COMMENT '最后连接时间',
  `last_disconnected_time` datetime(0) NULL DEFAULT NULL COMMENT '最后断开时间',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sy_agent_session_code`(`session_code`) USING BTREE,
  INDEX `idx_sy_agent_session_owner`(`tenant_id`, `owner_user_id`) USING BTREE,
  INDEX `idx_sy_agent_session_agent`(`tenant_id`, `agent_id`, `agent_version_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent 会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent_session_event
-- ----------------------------
DROP TABLE IF EXISTS `agent_session_event`;
CREATE TABLE `agent_session_event`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `session_id` bigint(0) NOT NULL COMMENT '会话ID',
  `session_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话外部编码',
  `agent_id` bigint(0) NOT NULL COMMENT 'Agent ID',
  `agent_version_id` bigint(0) NOT NULL COMMENT '版本ID',
  `tenant_id` bigint(0) NULL DEFAULT NULL COMMENT '租户ID',
  `task_id` bigint(0) NULL DEFAULT NULL COMMENT '关联任务ID',
  `event_sequence` bigint(0) NOT NULL COMMENT '事件序号',
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型',
  `event_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '事件内容',
  `replayable` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否允许补发 1-是 0-否',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sy_agent_session_event`(`session_id`, `event_sequence`) USING BTREE,
  INDEX `idx_sy_agent_session_event_replay`(`tenant_id`, `session_id`, `replayable`) USING BTREE,
  INDEX `idx_sy_agent_session_event_task`(`tenant_id`, `task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent 会话事件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent_task
-- ----------------------------
DROP TABLE IF EXISTS `agent_task`;
CREATE TABLE `agent_task`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `task_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务外部编码',
  `source_task_id` bigint(0) NULL DEFAULT NULL COMMENT '来源失败任务ID',
  `session_id` bigint(0) NOT NULL COMMENT '会话ID',
  `session_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话外部编码',
  `agent_id` bigint(0) NOT NULL COMMENT 'Agent ID',
  `agent_version_id` bigint(0) NOT NULL COMMENT '版本ID',
  `tenant_id` bigint(0) NULL DEFAULT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '任务所属用户ID',
  `task_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务状态 RUNNING/SUCCESS/FAILED',
  `request_message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求消息',
  `response_message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '响应消息',
  `error_message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败原因',
  `retry_count` int(0) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sy_agent_task_code`(`task_code`) USING BTREE,
  INDEX `idx_sy_agent_task_session_status`(`tenant_id`, `session_id`, `task_status`) USING BTREE,
  INDEX `idx_sy_agent_task_owner`(`tenant_id`, `owner_user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent 任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent_version
-- ----------------------------
DROP TABLE IF EXISTS `agent_version`;
CREATE TABLE `agent_version`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `agent_id` bigint(0) NOT NULL COMMENT 'Agent ID',
  `tenant_id` bigint(0) NULL DEFAULT NULL COMMENT '租户ID',
  `version_no` int(0) NOT NULL COMMENT '版本号',
  `agent_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Agent 名称快照',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述快照',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '系统提示词快照',
  `selected_capabilities_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '能力配置 JSON',
  `config_snapshot_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '完整配置快照 JSON',
  `is_published` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否已发布 1-是 0-否',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sy_agent_version`(`agent_id`, `version_no`) USING BTREE,
  INDEX `idx_sy_agent_version_tenant`(`tenant_id`, `agent_id`) USING BTREE,
  INDEX `idx_sy_agent_version_publish`(`tenant_id`, `is_published`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent 版本表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hook_agent_binding_record
-- ----------------------------
DROP TABLE IF EXISTS `hook_agent_binding_record`;
CREATE TABLE `hook_agent_binding_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `hook_id` bigint(0) NOT NULL COMMENT 'Hook ID',
  `hook_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Hook 编码',
  `binding_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '绑定名称',
  `binding_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '绑定范围',
  `target_agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标 Agent 编码',
  `target_model_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标模型编码',
  `environment_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '环境编码',
  `priority_no` int(0) NOT NULL DEFAULT 100 COMMENT '优先级',
  `enabled` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_hook_binding_hook`(`tenant_id`, `hook_id`) USING BTREE,
  INDEX `idx_hook_binding_agent`(`tenant_id`, `target_agent_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Hook Agent 绑定表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hook_execution_log_record
-- ----------------------------
DROP TABLE IF EXISTS `hook_execution_log_record`;
CREATE TABLE `hook_execution_log_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `hook_id` bigint(0) NULL DEFAULT NULL COMMENT 'Hook ID',
  `hook_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Hook 编码',
  `hook_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Hook 名称',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '来源 DEBUG/TEST/RUNTIME',
  `source_id` bigint(0) NULL DEFAULT NULL COMMENT '来源对象ID',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标 Agent 编码',
  `session_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话编码',
  `request_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求 JSON',
  `context_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上下文 JSON',
  `response_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '响应 JSON',
  `execute_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '执行状态',
  `success_flag` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否成功',
  `elapsed_ms` bigint(0) NULL DEFAULT NULL COMMENT '耗时毫秒',
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `operator_user_id` bigint(0) NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人名称',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_hook_execution_log_hook`(`tenant_id`, `hook_id`) USING BTREE,
  INDEX `idx_hook_execution_log_source`(`tenant_id`, `source_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Hook 执行日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hook_record
-- ----------------------------
DROP TABLE IF EXISTS `hook_record`;
CREATE TABLE `hook_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `hook_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Hook 编码',
  `hook_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Hook 名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Hook 描述',
  `hook_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Hook 类型',
  `hook_stage` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行阶段',
  `hook_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态 DRAFT/PUBLISHED/OFFLINE',
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LOW' COMMENT '风险等级 LOW/MEDIUM/HIGH/CRITICAL',
  `trigger_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SYNC' COMMENT '触发模式',
  `fail_strategy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CONTINUE' COMMENT '失败策略',
  `sort_weight` int(0) NOT NULL DEFAULT 100 COMMENT '排序权重',
  `timeout_ms` int(0) NOT NULL DEFAULT 10000 COMMENT '超时毫秒',
  `hot_update_enabled` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否开启热更新',
  `current_version_no` int(0) NOT NULL DEFAULT 1 COMMENT '当前版本号',
  `latest_version_no` int(0) NOT NULL DEFAULT 1 COMMENT '最新版本号',
  `published_version_no` int(0) NULL DEFAULT NULL COMMENT '已发布版本号',
  `builtin_hook_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内置模板键',
  `script_language` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '脚本语言',
  `deleted_flag` tinyint(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '负责人用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人名称',
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'Hook 完整快照 JSON',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_hook_record_code`(`tenant_id`, `hook_code`) USING BTREE,
  INDEX `idx_hook_record_status`(`tenant_id`, `hook_status`) USING BTREE,
  INDEX `idx_hook_record_publish`(`tenant_id`, `publish_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Hook 主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hook_test_case_record
-- ----------------------------
DROP TABLE IF EXISTS `hook_test_case_record`;
CREATE TABLE `hook_test_case_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `hook_id` bigint(0) NOT NULL COMMENT 'Hook ID',
  `hook_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Hook 编码',
  `case_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '测试用例名称',
  `input_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '输入 JSON',
  `context_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上下文 JSON',
  `expected_success` tinyint(0) NOT NULL DEFAULT 1 COMMENT '期望是否成功',
  `expected_response_contains` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '期望响应包含内容',
  `enabled` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `last_run_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近运行状态',
  `last_run_duration_ms` bigint(0) NULL DEFAULT NULL COMMENT '最近运行耗时',
  `last_run_at` datetime(0) NULL DEFAULT NULL COMMENT '最近运行时间',
  `last_result_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '最近运行结果',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_hook_test_case_hook`(`tenant_id`, `hook_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Hook 测试用例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hook_version_record
-- ----------------------------
DROP TABLE IF EXISTS `hook_version_record`;
CREATE TABLE `hook_version_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `hook_id` bigint(0) NOT NULL COMMENT 'Hook ID',
  `hook_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Hook 编码快照',
  `hook_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Hook 名称快照',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `version_no` int(0) NOT NULL COMMENT '版本号',
  `version_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '语义化版本',
  `version_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本说明',
  `version_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '版本状态 CURRENT/HISTORY/ROLLBACK',
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发布状态',
  `snapshot_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '版本快照 JSON',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_hook_version_record`(`hook_id`, `version_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Hook 版本表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interceptor_agent_binding_record
-- ----------------------------
DROP TABLE IF EXISTS `interceptor_agent_binding_record`;
CREATE TABLE `interceptor_agent_binding_record`  (
  `id` bigint(0) NOT NULL,
  `interceptor_id` bigint(0) NOT NULL,
  `interceptor_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `binding_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `binding_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `target_model_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `environment_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `priority_no` int(0) NULL DEFAULT 100,
  `enabled` tinyint(0) NULL DEFAULT 1,
  `tenant_id` bigint(0) NOT NULL,
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_id` bigint(0) NULL DEFAULT NULL,
  `create_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint(0) NULL DEFAULT NULL,
  `update_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `version` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_interceptor_binding_scope`(`tenant_id`, `interceptor_id`, `binding_scope`, `enabled`) USING BTREE,
  INDEX `idx_interceptor_binding_agent`(`tenant_id`, `target_agent_code`, `environment_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interceptor_execution_log_record
-- ----------------------------
DROP TABLE IF EXISTS `interceptor_execution_log_record`;
CREATE TABLE `interceptor_execution_log_record`  (
  `id` bigint(0) NOT NULL,
  `interceptor_id` bigint(0) NOT NULL,
  `interceptor_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `interceptor_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tenant_id` bigint(0) NOT NULL,
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_id` bigint(0) NULL DEFAULT NULL,
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `agent_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `session_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `request_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `context_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `response_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `execute_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `success_flag` tinyint(0) NULL DEFAULT 0,
  `elapsed_ms` bigint(0) NULL DEFAULT NULL,
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `operator_user_id` bigint(0) NULL DEFAULT NULL,
  `operator_user_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_id` bigint(0) NULL DEFAULT NULL,
  `create_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint(0) NULL DEFAULT NULL,
  `update_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `version` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_interceptor_log_query`(`tenant_id`, `interceptor_id`, `source_type`, `success_flag`) USING BTREE,
  INDEX `idx_interceptor_trace`(`trace_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interceptor_record
-- ----------------------------
DROP TABLE IF EXISTS `interceptor_record`;
CREATE TABLE `interceptor_record`  (
  `id` bigint(0) NOT NULL,
  `interceptor_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `interceptor_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `interceptor_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `interceptor_stage` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `interceptor_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `trigger_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `fail_strategy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_weight` int(0) NULL DEFAULT 100,
  `timeout_ms` int(0) NULL DEFAULT 5000,
  `hot_update_enabled` tinyint(0) NULL DEFAULT 0,
  `current_version_no` int(0) NULL DEFAULT 1,
  `latest_version_no` int(0) NULL DEFAULT 1,
  `published_version_no` int(0) NULL DEFAULT NULL,
  `builtin_interceptor_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `script_language` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'JAVA',
  `deleted_flag` tinyint(0) NULL DEFAULT 0,
  `tenant_id` bigint(0) NOT NULL,
  `owner_user_id` bigint(0) NULL DEFAULT NULL,
  `owner_user_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_id` bigint(0) NULL DEFAULT NULL,
  `create_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint(0) NULL DEFAULT NULL,
  `update_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `version` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_interceptor_code_tenant`(`tenant_id`, `interceptor_code`) USING BTREE,
  INDEX `idx_interceptor_stage_status`(`tenant_id`, `interceptor_stage`, `interceptor_status`) USING BTREE,
  INDEX `idx_interceptor_publish`(`tenant_id`, `publish_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interceptor_test_case_record
-- ----------------------------
DROP TABLE IF EXISTS `interceptor_test_case_record`;
CREATE TABLE `interceptor_test_case_record`  (
  `id` bigint(0) NOT NULL,
  `interceptor_id` bigint(0) NOT NULL,
  `interceptor_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `case_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `input_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `context_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `expected_success` tinyint(0) NULL DEFAULT 1,
  `expected_response_contains` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `enabled` tinyint(0) NULL DEFAULT 1,
  `tenant_id` bigint(0) NOT NULL,
  `last_run_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `last_run_duration_ms` bigint(0) NULL DEFAULT NULL,
  `last_run_at` datetime(0) NULL DEFAULT NULL,
  `last_result_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_id` bigint(0) NULL DEFAULT NULL,
  `create_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint(0) NULL DEFAULT NULL,
  `update_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `version` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_interceptor_test_case`(`tenant_id`, `interceptor_id`, `enabled`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interceptor_version_record
-- ----------------------------
DROP TABLE IF EXISTS `interceptor_version_record`;
CREATE TABLE `interceptor_version_record`  (
  `id` bigint(0) NOT NULL,
  `interceptor_id` bigint(0) NOT NULL,
  `interceptor_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `interceptor_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tenant_id` bigint(0) NOT NULL,
  `version_no` int(0) NOT NULL,
  `version_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `version_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `version_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `snapshot_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_id` bigint(0) NULL DEFAULT NULL,
  `create_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint(0) NULL DEFAULT NULL,
  `update_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `version` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_interceptor_version`(`interceptor_id`, `tenant_id`, `version_no`) USING BTREE,
  INDEX `idx_interceptor_version_code`(`tenant_id`, `interceptor_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mcp_execution_log_record
-- ----------------------------
DROP TABLE IF EXISTS `mcp_execution_log_record`;
CREATE TABLE `mcp_execution_log_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `server_id` bigint(0) NULL DEFAULT NULL COMMENT 'MCP服务ID',
  `server_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MCP服务编码',
  `server_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MCP服务名称',
  `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '调用工具名称',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日志来源 DEBUG/RUNTIME',
  `request_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求快照 JSON',
  `response_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '响应快照 JSON',
  `execute_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '执行状态 SUCCESS/FAILED',
  `success_flag` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否成功 1-是 0-否',
  `elapsed_ms` bigint(0) NULL DEFAULT NULL COMMENT '耗时毫秒',
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `operator_user_id` bigint(0) NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人名称',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mcp_execution_log_server`(`tenant_id`, `server_id`) USING BTREE,
  INDEX `idx_mcp_execution_log_source`(`tenant_id`, `source_type`) USING BTREE,
  INDEX `idx_mcp_execution_log_success`(`tenant_id`, `success_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'MCP执行日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mcp_server_record
-- ----------------------------
DROP TABLE IF EXISTS `mcp_server_record`;
CREATE TABLE `mcp_server_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `server_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'MCP服务编码',
  `server_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'MCP服务名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MCP服务描述',
  `server_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务类型 BUILTIN/REMOTE',
  `transport_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '传输协议 STDIO/SSE/STREAMABLE_HTTP',
  `server_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '服务状态 ENABLED/DISABLED',
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态 DRAFT/PUBLISHED/OFFLINE',
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LOW' COMMENT '风险等级 LOW/MEDIUM/HIGH',
  `sort_weight` int(0) NOT NULL DEFAULT 0 COMMENT '排序权重',
  `timeout_ms` int(0) NOT NULL DEFAULT 15000 COMMENT '超时毫秒',
  `auth_required` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否需要认证 1-是 0-否',
  `builtin_server_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内置MCP标识',
  `endpoint_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '远程MCP地址',
  `deleted_flag` tinyint(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 0-否 1-是',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '负责人用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人用户名称',
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'MCP扩展配置 JSON',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mcp_server_record_code`(`tenant_id`, `server_code`) USING BTREE,
  INDEX `idx_mcp_server_record_status`(`tenant_id`, `server_status`) USING BTREE,
  INDEX `idx_mcp_server_record_publish`(`tenant_id`, `publish_status`) USING BTREE,
  INDEX `idx_mcp_server_record_deleted`(`tenant_id`, `deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'MCP服务管理主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for model_definition
-- ----------------------------
DROP TABLE IF EXISTS `model_definition`;
CREATE TABLE `model_definition`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `model_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型配置编码',
  `model_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型名称',
  `provider_config_id` bigint(0) NOT NULL COMMENT '提供商配置ID',
  `provider_config_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供商配置编码',
  `provider_enum` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供商枚举',
  `model_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CHAT' COMMENT '模型类型',
  `model_identifier` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '远端模型标识',
  `temperature` decimal(6, 3) NULL DEFAULT NULL COMMENT '温度',
  `top_p` decimal(6, 3) NULL DEFAULT NULL COMMENT 'Top P',
  `presence_penalty` decimal(6, 3) NULL DEFAULT NULL COMMENT 'Presence Penalty',
  `frequency_penalty` decimal(6, 3) NULL DEFAULT NULL COMMENT 'Frequency Penalty',
  `max_tokens` int(0) NULL DEFAULT NULL COMMENT '最大输出 Token',
  `context_window` int(0) NULL DEFAULT NULL COMMENT '上下文窗口',
  `rpm_limit` int(0) NULL DEFAULT NULL COMMENT '每分钟请求数',
  `tpm_limit` int(0) NULL DEFAULT NULL COMMENT '每分钟 Token 数',
  `timeout_ms` int(0) NULL DEFAULT NULL COMMENT '超时毫秒',
  `support_streaming` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否支持流式输出',
  `support_tools` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否支持工具调用',
  `support_vision` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否支持视觉能力',
  `support_json_schema` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否支持 JSON Schema',
  `is_default` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否为默认模型',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '归属用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '归属用户名称',
  `advanced_config_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '高级参数 JSON',
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_model_definition_code`(`tenant_id`, `model_code`) USING BTREE,
  UNIQUE INDEX `uk_model_definition_name`(`tenant_id`, `model_name`) USING BTREE,
  INDEX `idx_model_definition_provider`(`tenant_id`, `provider_config_id`, `status`) USING BTREE,
  INDEX `idx_model_definition_default`(`tenant_id`, `provider_config_id`, `is_default`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '模型配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for model_provider_config
-- ----------------------------
DROP TABLE IF EXISTS `model_provider_config`;
CREATE TABLE `model_provider_config`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `provider_config_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供商配置编码',
  `provider_enum` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供商枚举',
  `provider_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供商配置名称',
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '自定义基础地址',
  `api_key_cipher_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '加密后的 API Key',
  `organization_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '组织标识',
  `default_headers_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '默认请求头 JSON',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '归属用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '归属用户名称',
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_model_provider_config_code`(`tenant_id`, `provider_config_code`) USING BTREE,
  UNIQUE INDEX `uk_model_provider_config_name`(`tenant_id`, `provider_name`) USING BTREE,
  INDEX `idx_model_provider_status`(`tenant_id`, `status`, `provider_enum`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '模型提供商配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for prompt_template
-- ----------------------------
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板编码',
  `template_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模板描述',
  `template_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板类型',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '来源类型 INLINE_TEXT/FILE_PATH',
  `template_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '模板内容快照',
  `source_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件路径',
  `template_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '模板状态 ENABLED/DISABLED',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '创建人用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人用户名称',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_prompt_template_code`(`tenant_id`, `template_code`) USING BTREE,
  INDEX `idx_prompt_template_status`(`tenant_id`, `template_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提示词模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for skill_execution_log_record
-- ----------------------------
DROP TABLE IF EXISTS `skill_execution_log_record`;
CREATE TABLE `skill_execution_log_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `skill_id` bigint(0) NULL DEFAULT NULL COMMENT '技能ID',
  `skill_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '技能编码',
  `skill_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '技能名称',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日志来源 DEBUG/TEST/RUNTIME',
  `source_id` bigint(0) NULL DEFAULT NULL COMMENT '来源对象ID',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `session_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话编号',
  `channel_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渠道编码',
  `locale` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '语言区域',
  `input_text` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '输入文本',
  `matched_intent` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '命中意图',
  `confidence_score` decimal(8, 4) NULL DEFAULT NULL COMMENT '置信度',
  `slot_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '槽位参数 JSON',
  `context_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上下文 JSON',
  `request_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求快照 JSON',
  `response_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '响应快照 JSON',
  `trace_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '执行链路 JSON',
  `execute_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '执行状态',
  `success_flag` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否成功',
  `elapsed_ms` bigint(0) NULL DEFAULT NULL COMMENT '耗时毫秒',
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `satisfaction_level` int(0) NULL DEFAULT NULL COMMENT '满意度',
  `operator_user_id` bigint(0) NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人名称',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_skill_execution_log_skill`(`tenant_id`, `skill_id`) USING BTREE,
  INDEX `idx_skill_execution_log_source`(`tenant_id`, `source_type`) USING BTREE,
  INDEX `idx_skill_execution_log_success`(`tenant_id`, `success_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技能执行日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for skill_record
-- ----------------------------
DROP TABLE IF EXISTS `skill_record`;
CREATE TABLE `skill_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `skill_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '技能编码',
  `skill_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '技能名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '技能描述',
  `skill_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '技能类型',
  `skill_category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '技能分类',
  `skill_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '技能状态 ENABLED/DISABLED',
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态 DRAFT/TESTING/PRE_RELEASE/PUBLISHED/OFFLINE',
  `version_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MANUAL' COMMENT '版本模式 MANUAL/AUTO',
  `sort_weight` int(0) NOT NULL DEFAULT 0 COMMENT '排序权重',
  `current_version_no` int(0) NOT NULL DEFAULT 1 COMMENT '当前版本号',
  `latest_version_no` int(0) NOT NULL DEFAULT 1 COMMENT '最新版本号',
  `published_version_no` int(0) NULL DEFAULT NULL COMMENT '已发布版本号',
  `hot_update_enabled` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否启用热更新',
  `deleted_flag` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除 0-否 1-是',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '负责人用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人名称',
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '技能完整快照 JSON',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_record_code`(`tenant_id`, `skill_code`) USING BTREE,
  INDEX `idx_skill_record_status`(`tenant_id`, `skill_status`) USING BTREE,
  INDEX `idx_skill_record_publish`(`tenant_id`, `publish_status`) USING BTREE,
  INDEX `idx_skill_record_deleted`(`tenant_id`, `deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技能主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for skill_test_case_record
-- ----------------------------
DROP TABLE IF EXISTS `skill_test_case_record`;
CREATE TABLE `skill_test_case_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `skill_id` bigint(0) NOT NULL COMMENT '技能ID',
  `skill_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '技能编码',
  `case_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '测试用例名称',
  `input_text` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '测试输入问句',
  `slot_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '槽位参数 JSON',
  `expected_intent` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预期命中意图',
  `expected_success` tinyint(0) NOT NULL DEFAULT 1 COMMENT '预期是否成功',
  `expected_response_contains` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预期响应包含内容',
  `channel_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渠道编码',
  `locale` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '语言区域',
  `enabled` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `last_run_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近运行状态',
  `last_run_duration_ms` bigint(0) NULL DEFAULT NULL COMMENT '最近耗时毫秒',
  `last_run_at` datetime(0) NULL DEFAULT NULL COMMENT '最近运行时间',
  `last_result_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '最近运行结果 JSON',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_skill_test_case_skill`(`tenant_id`, `skill_id`) USING BTREE,
  INDEX `idx_skill_test_case_enabled`(`tenant_id`, `enabled`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技能测试用例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for skill_version_record
-- ----------------------------
DROP TABLE IF EXISTS `skill_version_record`;
CREATE TABLE `skill_version_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `skill_id` bigint(0) NOT NULL COMMENT '技能ID',
  `skill_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '技能编码快照',
  `skill_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '技能名称快照',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `version_no` int(0) NOT NULL COMMENT '版本号',
  `version_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '语义化版本号',
  `version_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本说明',
  `version_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '版本状态 CURRENT/HISTORY/ROLLBACK',
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发布状态',
  `release_stage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发布阶段',
  `snapshot_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '版本快照 JSON',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_version_record`(`skill_id`, `version_no`) USING BTREE,
  INDEX `idx_skill_version_tenant`(`tenant_id`, `skill_id`) USING BTREE,
  INDEX `idx_skill_version_publish`(`tenant_id`, `publish_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技能版本表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sy_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sy_tenant`;
CREATE TABLE `sy_tenant`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `tenant_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `tenant_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户名称',
  `status` int(0) NOT NULL DEFAULT 1 COMMENT '租户状态 1-启用 0-禁用',
  `is_default` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否默认租户 1-是 0-否',
  `owner_user_id` bigint(0) NULL DEFAULT NULL COMMENT '默认租户归属用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认租户归属用户名',
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户描述',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sy_tenant_code`(`tenant_code`) USING BTREE,
  INDEX `idx_sy_tenant_status`(`status`) USING BTREE,
  INDEX `idx_sy_tenant_owner_default`(`owner_user_id`, `is_default`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sy_user
-- ----------------------------
DROP TABLE IF EXISTS `sy_user`;
CREATE TABLE `sy_user`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录账号',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户显示名称',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码摘要',
  `status` int(0) NOT NULL DEFAULT 1 COMMENT '用户状态: 1-启用, 0-禁用',
  `tenant_id` bigint(0) NULL DEFAULT NULL COMMENT '租户ID',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tool_execution_log_record
-- ----------------------------
DROP TABLE IF EXISTS `tool_execution_log_record`;
CREATE TABLE `tool_execution_log_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `tool_id` bigint(0) NULL DEFAULT NULL COMMENT '工具ID',
  `tool_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具编码',
  `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具名称',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日志来源 DEBUG/RUNTIME',
  `request_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求快照 JSON',
  `response_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '响应快照 JSON',
  `execute_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '执行状态 SUCCESS/FAILED',
  `success_flag` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否成功 1-是 0-否',
  `elapsed_ms` bigint(0) NULL DEFAULT NULL COMMENT '耗时毫秒',
  `failure_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `operator_user_id` bigint(0) NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人名称',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tool_execution_log_tool`(`tenant_id`, `tool_id`) USING BTREE,
  INDEX `idx_tool_execution_log_source`(`tenant_id`, `source_type`) USING BTREE,
  INDEX `idx_tool_execution_log_success`(`tenant_id`, `success_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工具执行日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tool_record
-- ----------------------------
DROP TABLE IF EXISTS `tool_record`;
CREATE TABLE `tool_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `tool_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具编码',
  `tool_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具描述',
  `tool_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具类型',
  `tool_category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具分类',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具来源 BUILTIN/API/MCP/AGENT/CUSTOM',
  `tool_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '工具状态 ENABLED/DISABLED',
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态 DRAFT/PUBLISHED/OFFLINE',
  `risk_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LOW' COMMENT '风险等级 LOW/MEDIUM/HIGH',
  `execution_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SYNC' COMMENT '执行模式 SYNC/ASYNC',
  `sort_weight` int(0) NOT NULL DEFAULT 0 COMMENT '排序权重',
  `timeout_ms` int(0) NOT NULL DEFAULT 15000 COMMENT '执行超时时间毫秒',
  `auth_required` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否需要认证 1-是 0-否',
  `builtin_tool_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内置工具键',
  `endpoint_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '外部工具地址',
  `http_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'HTTP方法',
  `deleted_flag` tinyint(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 0-否 1-是',
  `tenant_id` bigint(0) NOT NULL COMMENT '租户ID',
  `owner_user_id` bigint(0) NOT NULL COMMENT '负责人用户ID',
  `owner_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人用户名',
  `ext` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '工具扩展配置 JSON',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tool_record_code`(`tenant_id`, `tool_code`) USING BTREE,
  INDEX `idx_tool_record_status`(`tenant_id`, `tool_status`) USING BTREE,
  INDEX `idx_tool_record_publish`(`tenant_id`, `publish_status`) USING BTREE,
  INDEX `idx_tool_record_deleted`(`tenant_id`, `deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工具管理主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for vector_store_file
-- ----------------------------
DROP TABLE IF EXISTS `vector_store_file`;
CREATE TABLE `vector_store_file`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `module_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模块名称',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_extension` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件后缀',
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容类型',
  `file_size` bigint(0) NULL DEFAULT NULL COMMENT '文件大小',
  `source_document_count` int(0) NOT NULL DEFAULT 0 COMMENT '源文档数量',
  `chunk_count` int(0) NOT NULL DEFAULT 0 COMMENT '切片数量',
  `uploaded_at` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上传时间',
  `store_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '存储状态 ACTIVE/DELETED',
  `last_operation_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近一次操作说明',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人ID',
  `create_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_id` bigint(0) NULL DEFAULT NULL COMMENT '更新人ID',
  `update_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人名称',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `version` int(0) NOT NULL DEFAULT 0 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_vector_store_file`(`module_name`, `file_name`) USING BTREE,
  INDEX `idx_vector_store_file_status`(`module_name`, `store_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '向量文件台账表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
