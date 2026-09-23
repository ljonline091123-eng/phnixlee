-- 合同对象拆分：合同聚合、合同参与方、签署任务。
-- 只新增表，不修改现有合同和电子签章历史表；执行前请先备份数据库。
CREATE TABLE IF NOT EXISTS `tb_procurement_contract` (
  `id` bigint NOT NULL,
  `award_decision_id` bigint DEFAULT NULL COMMENT '定标决策ID',
  `legacy_agreement_id` bigint DEFAULT NULL COMMENT '旧合同ID',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案ID',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `contract_no` varchar(64) DEFAULT NULL COMMENT '合同编号',
  `contract_name` varchar(256) NOT NULL COMMENT '合同名称',
  `total_amount` decimal(20,6) DEFAULT NULL COMMENT '合同含税金额',
  `signed_at` datetime DEFAULT NULL COMMENT '合同签署日期',
  `effective_at` datetime DEFAULT NULL COMMENT '合同生效日期',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '合同状态',
  `create_by` varchar(64) DEFAULT NULL, `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL, `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`), KEY `idx_contract_award` (`award_decision_id`),
  UNIQUE KEY `uk_contract_legacy` (`legacy_agreement_id`),
  KEY `idx_contract_scheme_vendor` (`scheme_id`,`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购合同';

CREATE TABLE IF NOT EXISTS `tb_contract_party` (
  `id` bigint NOT NULL, `contract_id` bigint NOT NULL COMMENT '合同ID',
  `party_type` tinyint NOT NULL COMMENT '参与方类型', `party_id` varchar(128) NOT NULL,
  `party_name` varchar(256) DEFAULT NULL, `need_sign` tinyint NOT NULL DEFAULT 1,
  `create_by` varchar(64) DEFAULT NULL, `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL, `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0', PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_party` (`contract_id`,`party_type`,`party_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同参与方';

CREATE TABLE IF NOT EXISTS `tb_contract_sign_task` (
  `id` bigint NOT NULL, `contract_id` bigint NOT NULL, `party_id` bigint NOT NULL,
  `platform` varchar(32) DEFAULT NULL, `external_contract_id` varchar(128) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0, `last_callback_event_id` varchar(128) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL, `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL, `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0', PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_sign_party` (`contract_id`,`party_id`),
  UNIQUE KEY `uk_contract_sign_callback` (`last_callback_event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同签署任务';
