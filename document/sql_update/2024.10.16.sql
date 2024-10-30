
# 2024.10.16 下午6:36 第三方数据合约规划推送记录 表增加字段
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_contract_planning_push_record ADD scheme_id bigint NULL COMMENT '采购方案id';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_contract_planning_push_record ADD notice_id bigint NULL COMMENT '招标对象id';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_contract_planning_push_record ADD procurement_scheme_code varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '采购方案编号';
