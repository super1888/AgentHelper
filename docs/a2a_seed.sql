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
