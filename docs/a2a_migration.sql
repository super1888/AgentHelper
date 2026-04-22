-- A2A incremental migration for environments that already created a2a tables.
-- MySQL 8.0.29+ supports ADD COLUMN IF NOT EXISTS / ADD INDEX IF NOT EXISTS.

ALTER TABLE `a2a_execution_log_record`
  ADD COLUMN IF NOT EXISTS `route_code` varchar(128) DEFAULT NULL AFTER `target_agent_code`,
  ADD COLUMN IF NOT EXISTS `execute_status` varchar(32) DEFAULT NULL AFTER `event_type`,
  ADD COLUMN IF NOT EXISTS `attempt_no` int DEFAULT NULL AFTER `execute_status`,
  ADD COLUMN IF NOT EXISTS `retry_index` int DEFAULT NULL AFTER `attempt_no`;

ALTER TABLE `a2a_execution_log_record`
  ADD INDEX IF NOT EXISTS `idx_a2a_log_attempt` (`tenant_id`,`task_code`,`attempt_no`,`retry_index`);

-- Backfill old one-row-final-result logs as the first attempt.
UPDATE `a2a_execution_log_record`
SET
  `execute_status` = CASE WHEN `success_flag` = 1 THEN 'SUCCESS' ELSE 'FAILED' END,
  `attempt_no` = COALESCE(`attempt_no`, 1),
  `retry_index` = COALESCE(`retry_index`, 0)
WHERE `attempt_no` IS NULL OR `retry_index` IS NULL OR `execute_status` IS NULL;
