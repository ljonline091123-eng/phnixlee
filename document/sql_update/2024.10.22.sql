ALTER TABLE `hnjiantou-zhaocai-dev`.tb_bidding_info ADD expert_state int NULL COMMENT '专家评分状态 0未评分 1已评分';

# 2024.10.22 上午11:50 易料采购合同 新增表
-- `hnjiantou-zhaocai-dev`.tb_market_material_contract definition

CREATE TABLE `tb_market_material_contract` (
  `id` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '合同主键id',
  `plan_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计划id',
  `require_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '清单id',
  `belong_accounting_item` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目',
  `belong_accounting_item_code` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目编码',
  `agreement_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同编码',
  `agreement_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同名称',
  `vendor_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方（供应商）Id',
  `party_b_name` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方名称',
  `expenditure_business_type` varchar(8) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出业务分类',
  `party_b_legal_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方法人代表',
  `party_b_legal_id_card` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方法人身份证',
  `party_b_legal_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方法人联系方式',
  `party_b_responsible_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方现场实际履职负责人',
  `party_b_responsible_id_card` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方现场实际履职负责人身份证',
  `party_b_responsible_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方现场实际履职负责人联系方式',
  `agreement_perform_country` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家地区代码(履行地)',
  `agreement_perform_district` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划代码(履行地)',
  `agreement_perform_address` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同履行地',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='易料采购合同';

# 2024.10.22 上午11:50 易料采购合同清单 新增表
-- `hnjiantou-zhaocai-dev`.tb_market_material_list definition

CREATE TABLE `tb_market_material_list` (
  `require_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '清单id',
  `contract_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同id',
  `quote_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '清单名称',
  `quote_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '清单编码',
  `status` int DEFAULT NULL COMMENT '报价状态 0:未报价,1:已报价,2:不报价',
  `category` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '规格型号',
  `unit_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '单位',
  `quantity` decimal(20,5) DEFAULT NULL COMMENT '数量',
  `price` decimal(20,5) DEFAULT NULL COMMENT '含税单价',
  `no_tax_price` decimal(20,5) DEFAULT NULL COMMENT '不含税单价',
  `tax_rate` decimal(20,5) DEFAULT NULL COMMENT '税率',
  `offer_price` decimal(20,5) DEFAULT NULL COMMENT '易料市集含税单价',
  `offer_brand` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集品牌',
  `offer_goods_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集商品编码',
  `created_time` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `offer_supplier_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集供应商名称',
  `offer_supplier_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集供应商编码',
  `goods_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集商品名',
  PRIMARY KEY (`require_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='易料采购合同清单';

# 2024.10.22 下午6:41 合同基本信息 表新增字段

ALTER TABLE `hnjiantou-zhaocai-dev`.tb_agreement ADD market_material_contract_id varchar(32) NULL COMMENT '易料采购合同id';