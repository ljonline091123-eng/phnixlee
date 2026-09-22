-- 招采领域对象规范化（增量迁移）
-- 目的：将投标版本、评标任务、评标表、定标决策、候选人、公示从旧的混合实体中拆出。
-- 注意：本脚本只新增表，不删除或修改既有表；上线前请先完成数据库一致性备份。

CREATE TABLE IF NOT EXISTS `tb_bid_submission` (
  `id` bigint NOT NULL COMMENT '投标提交ID',
  `notice_id` bigint NOT NULL COMMENT '招标公告ID',
  `scheme_id` bigint NOT NULL COMMENT '采购方案ID',
  `vendor_id` bigint NOT NULL COMMENT '供应商ID',
  `current_version_id` bigint DEFAULT NULL COMMENT '当前生效投标版本ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿、1已提交、2已撤回、3已拒绝',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `create_by` varchar(64) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bid_submission_notice_vendor` (`notice_id`,`vendor_id`),
  KEY `idx_bid_submission_scheme` (`scheme_id`),
  KEY `idx_bid_submission_vendor` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投标提交';

CREATE TABLE IF NOT EXISTS `tb_bid_submission_version` (
  `id` bigint NOT NULL COMMENT '投标版本ID',
  `submission_id` bigint NOT NULL COMMENT '投标提交ID',
  `previous_version_id` bigint DEFAULT NULL COMMENT '前一版本ID',
  `version_no` int NOT NULL COMMENT '版本号',
  `tax_price` decimal(20,6) DEFAULT NULL COMMENT '含税总价',
  `not_tax_price` decimal(20,6) DEFAULT NULL COMMENT '不含税总价',
  `contact` varchar(64) DEFAULT NULL COMMENT '投标联系人',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿、1已提交、2已作废',
  `submitted_at` datetime DEFAULT NULL COMMENT '版本提交时间',
  `create_by` varchar(64) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bid_version_no` (`submission_id`,`version_no`),
  KEY `idx_bid_version_previous` (`previous_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投标版本';

CREATE TABLE IF NOT EXISTS `tb_evaluation_assignment` (
  `id` bigint NOT NULL COMMENT '评标任务ID',
  `notice_id` bigint NOT NULL COMMENT '招标公告ID',
  `expert_id` bigint NOT NULL COMMENT '专家ID',
  `role_type` tinyint DEFAULT NULL COMMENT '评标角色：1技术、2商务、3综合',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待处理、1进行中、2已完成、3已取消',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` varchar(64) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_evaluation_assignment` (`notice_id`,`expert_id`,`role_type`),
  KEY `idx_evaluation_assignment_status` (`notice_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评标任务';

CREATE TABLE IF NOT EXISTS `tb_evaluation_sheet` (
  `id` bigint NOT NULL COMMENT '评标表ID',
  `assignment_id` bigint NOT NULL COMMENT '评标任务ID',
  `submission_version_id` bigint NOT NULL COMMENT '投标版本ID',
  `business_score` decimal(10,4) DEFAULT NULL COMMENT '商务评分',
  `technical_score` decimal(10,4) DEFAULT NULL COMMENT '技术评分',
  `quotation_score` decimal(10,4) DEFAULT NULL COMMENT '报价评分',
  `total_score` decimal(10,4) DEFAULT NULL COMMENT '总分',
  `opinion` varchar(2000) DEFAULT NULL COMMENT '评标意见',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿、1已提交',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `create_by` varchar(64) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_evaluation_sheet` (`assignment_id`,`submission_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评标表';

CREATE TABLE IF NOT EXISTS `tb_award_decision` (
  `id` bigint NOT NULL COMMENT '定标决策ID',
  `notice_id` bigint NOT NULL COMMENT '招标公告ID',
  `scheme_id` bigint NOT NULL COMMENT '采购方案ID',
  `selected_candidate_id` bigint DEFAULT NULL COMMENT '中选候选人ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿、1已定标、2已公示、3已取消',
  `decision_basis` varchar(2000) DEFAULT NULL COMMENT '定标依据',
  `decided_by` bigint DEFAULT NULL COMMENT '决策人用户ID',
  `decided_at` datetime DEFAULT NULL COMMENT '定标时间',
  `create_by` varchar(64) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_award_decision_notice` (`notice_id`),
  KEY `idx_award_decision_scheme` (`scheme_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定标决策';

CREATE TABLE IF NOT EXISTS `tb_award_candidate` (
  `id` bigint NOT NULL COMMENT '候选人ID',
  `decision_id` bigint NOT NULL COMMENT '定标决策ID',
  `submission_version_id` bigint NOT NULL COMMENT '投标版本ID',
  `vendor_id` bigint NOT NULL COMMENT '供应商ID',
  `rank` int NOT NULL COMMENT '综合排名',
  `total_score` decimal(10,4) DEFAULT NULL COMMENT '综合得分',
  `recommendation_reason` varchar(2000) DEFAULT NULL COMMENT '推荐理由',
  `create_by` varchar(64) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_award_candidate_rank` (`decision_id`,`rank`),
  KEY `idx_award_candidate_vendor` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中标候选人';

CREATE TABLE IF NOT EXISTS `tb_award_publicity` (
  `id` bigint NOT NULL COMMENT '公示ID',
  `decision_id` bigint NOT NULL COMMENT '定标决策ID',
  `title` varchar(256) NOT NULL COMMENT '公示标题',
  `content` text COMMENT '公示内容',
  `start_time` datetime DEFAULT NULL COMMENT '公示开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '公示结束时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿、1公示中、2已结束、3已撤销',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` varchar(64) DEFAULT NULL,
  `create_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_id` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_award_publicity_decision` (`decision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中标公示';
