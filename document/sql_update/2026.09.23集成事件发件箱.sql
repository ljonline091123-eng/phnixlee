-- 外部系统可靠投递发件箱。只新增表；执行前必须完成数据库一致性备份。
CREATE TABLE IF NOT EXISTS `tb_integration_outbox_event` (
  `id` bigint NOT NULL COMMENT '事件ID',
  `aggregate_type` varchar(64) NOT NULL COMMENT '聚合类型',
  `aggregate_id` varchar(128) NOT NULL COMMENT '聚合标识',
  `event_type` varchar(128) NOT NULL COMMENT '事件类型',
  `idempotency_key` varchar(256) NOT NULL COMMENT '幂等键',
  `payload` longtext COMMENT 'JSON载荷',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待投递 1投递中 2成功 3失败 4死信',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_at` datetime DEFAULT NULL COMMENT '下次重试时间',
  `last_error` varchar(2000) DEFAULT NULL COMMENT '最后错误',
  `published_at` datetime DEFAULT NULL COMMENT '投递完成时间',
  `create_by` varchar(64) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbox_idempotency` (`idempotency_key`),
  KEY `idx_outbox_retry` (`status`,`next_retry_at`),
  KEY `idx_outbox_aggregate` (`aggregate_type`,`aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集成事件发件箱';

ALTER TABLE `tb_contract_planning_push_record`
  MODIFY COLUMN `push_status` tinyint DEFAULT 0 COMMENT '0待推送 1推送成功 2推送失败';
