ALTER TABLE `jkty-zxdl-dev`.tb_procurement_plan ADD wf_process_id varchar(100) NULL;

-- 是否低价中标
ALTER TABLE `jkty-zxdl-dev`.tb_procurement_scheme ADD low_price varchar(1) NULL;

-- 报价评分
ALTER TABLE `jkty-zxdl-dev`.tb_expert_score ADD quotation decimal(10,2) NULL COMMENT '报价评分';

-- 修改字段类型
ALTER TABLE `jkty-zxdl-dev`.tb_vendor_change MODIFY COLUMN account_branch varchar(200) NULL COMMENT '开户支行';

ALTER TABLE `jkty-zxdl-dev`.tb_vendor MODIFY COLUMN account_branch varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '开户支行';

