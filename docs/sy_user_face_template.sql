-- sy_user_face_template 人脸模板表
-- 用于保存用户人脸特征向量、质量分、活体分和绑定状态

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sy_user_face_template`;
CREATE TABLE `sy_user_face_template`  (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `face_template_code` varchar(64) NOT NULL,
  `embedding_cipher_text` longtext NOT NULL,
  `embedding_dimension` int NOT NULL,
  `embedding_version` varchar(32) DEFAULT NULL,
  `quality_score` decimal(6,4) DEFAULT NULL,
  `liveness_score` decimal(6,4) DEFAULT NULL,
  `source_image_url` varchar(500) DEFAULT NULL,
  `image_sha256` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ENABLE',
  `last_verified_time` datetime DEFAULT NULL,
  `ext` longtext DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_name` varchar(128) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_id` bigint DEFAULT NULL,
  `update_name` varchar(128) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_face_template_code` (`face_template_code`),
  UNIQUE KEY `uk_face_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
