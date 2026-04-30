-- Demo data for the A2A control plane.
-- Default tenant_id is 1. Adjust endpoint_url to match your local A2A server.

INSERT INTO `a2a_agent_card_record` (
  `id`, `agent_code`, `agent_name`, `description`, `endpoint_url`,
  `protocol_version`, `transport_type`, `auth_type`, `agent_status`, `publish_status`,
  `risk_level`, `trust_level`, `owner_team`, `timeout_ms`, `rate_limit_qps`, `success_rate_slo`,
  `tenant_id`, `deleted_flag`, `ext`, `remark`, `create_time`, `update_time`, `version`
) VALUES
(
  910000000000000001,
  'data-analysis-agent',
  'Data Analysis Agent',
  'A2A data analysis and structured answering agent registered in Nacos.',
  'http://127.0.0.1:18080/a2a/task',
  '1.0',
  'HTTP',
  'NONE',
  'ENABLED',
  'PUBLISHED',
  'MEDIUM',
  'INTERNAL',
  'agent-platform',
  15000,
  20,
  99,
  1,
  0,
  '{"capabilities":["chat","data_analysis","structured_answer"],"inputModes":["text/json"],"outputModes":["text/json"],"dispatchConfig":{"retryTimes":1},"authConfig":{"mode":"none"},"metadata":{"region":"local","tier":"production"}}',
  'Primary A2A demo agent.',
  NOW(),
  NOW(),
  0
),
(
  910000000000000002,
  'data-analysis-agent-backup',
  'Data Analysis Backup Agent',
  'Backup A2A data analysis agent for failover tests.',
  'http://127.0.0.1:18081/a2a/task',
  '1.0',
  'HTTP',
  'NONE',
  'ENABLED',
  'PUBLISHED',
  'MEDIUM',
  'INTERNAL',
  'agent-platform',
  15000,
  10,
  99,
  1,
  0,
  '{"capabilities":["chat","data_analysis"],"inputModes":["text/json"],"outputModes":["text/json"],"dispatchConfig":{"retryTimes":0},"authConfig":{"mode":"none"},"metadata":{"region":"local","tier":"backup"}}',
  'Backup A2A demo agent.',
  NOW(),
  NOW(),
  0
)
ON DUPLICATE KEY UPDATE
  `agent_name` = VALUES(`agent_name`),
  `description` = VALUES(`description`),
  `endpoint_url` = VALUES(`endpoint_url`),
  `publish_status` = VALUES(`publish_status`),
  `agent_status` = VALUES(`agent_status`),
  `timeout_ms` = VALUES(`timeout_ms`),
  `ext` = VALUES(`ext`),
  `update_time` = NOW();

INSERT INTO `a2a_route_record` (
  `id`, `route_code`, `route_name`, `source_agent_code`, `target_agent_code`,
  `task_type`, `route_status`, `priority_no`, `failover_enabled`, `fallback_agent_codes`,
  `tenant_id`, `remark`, `create_time`, `update_time`, `version`
) VALUES
(
  910000000000010001,
  'chat-completion-default',
  'Default Chat Completion Route',
  NULL,
  'data-analysis-agent',
  'chat.completion',
  'ENABLED',
  100,
  1,
  'data-analysis-agent-backup',
  1,
  'Default A2A route with fallback enabled.',
  NOW(),
  NOW(),
  0
)
ON DUPLICATE KEY UPDATE
  `route_name` = VALUES(`route_name`),
  `target_agent_code` = VALUES(`target_agent_code`),
  `task_type` = VALUES(`task_type`),
  `route_status` = VALUES(`route_status`),
  `priority_no` = VALUES(`priority_no`),
  `failover_enabled` = VALUES(`failover_enabled`),
  `fallback_agent_codes` = VALUES(`fallback_agent_codes`),
  `update_time` = NOW();


SET NAMES utf8mb4;

/*
  默认种子数据说明：
  1. 本文件用于初始化核心配置页的演示数据。
  2. provider_config_code / model_code 使用固定编码，便于联调。
  3. API Key 默认留空，导入后请在页面中补充真实密钥再启用。
  4. tenant_id / owner_user_id / create_id 请按你的环境替换。
*/

INSERT INTO `model_provider_config` (
    `id`, `provider_config_code`, `provider_enum`, `provider_name`, `base_url`,
    `api_key_cipher_text`, `organization_id`, `default_headers_json`, `status`,
    `tenant_id`, `owner_user_id`, `owner_user_name`, `remark`,
    `create_id`, `create_name`, `create_time`, `update_id`, `update_name`, `update_time`, `version`
) VALUES
      (920000000000000001, 'provider-openai-default', 'OPENAI', 'OpenAI 默认账号', 'https://api.openai.com', NULL, NULL, '{"x-env":"prod"}', 'DISABLED', 170231497272463360, 170110680559652864, '294419455', '请补充真实 API Key 后启用', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0),
      (920000000000000002, 'provider-deepseek-default', 'DEEPSEEK', 'DeepSeek 默认账号', 'https://api.deepseek.com', NULL, NULL, NULL, 'DISABLED', 170231497272463360, 170110680559652864, '294419455', '请补充真实 API Key 后启用', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0),
      (920000000000000003, 'provider-dashscope-default', 'DASHSCOPE', 'DashScope 默认账号', NULL, NULL, NULL, NULL, 'DISABLED', 170231497272463360, 170110680559652864, '294419455', '请补充真实 API Key 后启用', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0),
      (920000000000000004, 'provider-anthropic-default', 'ANTHROPIC', 'Anthropic 默认账号', NULL, NULL, NULL, NULL, 'DISABLED', 170231497272463360, 170110680559652864, '294419455', '请补充真实 API Key 后启用', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0),
      (920000000000000005, 'provider-zhipu-default', 'ZHIPU', '智谱默认账号', NULL, NULL, NULL, NULL, 'DISABLED', 170231497272463360, 170110680559652864, '294419455', '请补充真实 API Key 后启用', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0);

INSERT INTO `model_definition` (
    `id`, `model_code`, `model_name`, `provider_config_id`, `provider_config_code`, `provider_enum`,
    `model_type`, `model_identifier`, `temperature`, `top_p`, `presence_penalty`, `frequency_penalty`,
    `max_tokens`, `context_window`, `rpm_limit`, `tpm_limit`, `timeout_ms`,
    `support_streaming`, `support_tools`, `support_vision`, `support_json_schema`,
    `is_default`, `status`, `tenant_id`, `owner_user_id`, `owner_user_name`,
    `advanced_config_json`, `remark`,
    `create_id`, `create_name`, `create_time`, `update_id`, `update_name`, `update_time`, `version`
) VALUES
      (920000000000100001, 'model-openai-gpt41', 'GPT-4.1 主模型', 920000000000000001, 'provider-openai-default', 'OPENAI', 'CHAT', 'gpt-4.1', 0.7, 0.9, 0, 0, 4096, 128000, 120, 240000, 60000, 1, 1, 1, 1, 1, 'DISABLED', 170231497272463360, 170110680559652864, '294419455', '{"reasoningEffort":"medium"}', 'OpenAI 主路由模型', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0),
      (920000000000100002, 'model-deepseek-chat', 'DeepSeek Chat 主模型', 920000000000000002, 'provider-deepseek-default', 'DEEPSEEK', 'CHAT', 'deepseek-chat', 0.5, 0.9, 0, 0, 8192, 64000, 200, 300000, 60000, 1, 1, 0, 1, 1, 'DISABLED', 170231497272463360, 170110680559652864, '294419455', NULL, 'DeepSeek 主路由模型', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0),
      (920000000000100003, 'model-qwen-max', 'Qwen Max 主模型', 920000000000000003, 'provider-dashscope-default', 'DASHSCOPE', 'CHAT', 'qwen-max', 0.7, 0.9, 0, 0, 4096, 32000, 120, 240000, 60000, 1, 1, 1, 0, 1, 'DISABLED', 170231497272463360, 170110680559652864, '294419455', NULL, 'DashScope 主路由模型', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0),
      (920000000000100004, 'model-claude-sonnet', 'Claude Sonnet 主模型', 920000000000000004, 'provider-anthropic-default', 'ANTHROPIC', 'CHAT', 'claude-3-7-sonnet-latest', 0.4, 0.95, 0, 0, 4096, 200000, 60, 200000, 60000, 1, 1, 1, 0, 1, 'DISABLED', 170231497272463360, 170110680559652864, '294419455', NULL, 'Anthropic 主路由模型', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0),
      (920000000000100005, 'model-glm-4-plus', 'GLM-4-Plus 主模型', 920000000000000005, 'provider-zhipu-default', 'ZHIPU', 'CHAT', 'glm-4-plus', 0.6, 0.9, 0, 0, 4096, 128000, 120, 240000, 60000, 1, 1, 0, 0, 1, 'DISABLED', 170231497272463360, 170110680559652864, '294419455', NULL, '智谱主路由模型', 170110680559652864, '294419455', NOW(), 170110680559652864, '294419455', NOW(), 0);
