ALTER TABLE `jkty-zxdl-dev`.tb_procurement_plan ADD wf_process_id varchar(100) NULL;

-- 是否低价中标
ALTER TABLE `jkty-zxdl-dev`.tb_procurement_scheme ADD low_price varchar(1) NULL;

--报价评分
ALTER TABLE `jkty-zxdl-dev`.tb_expert_score ADD quotation decimal(10,2) NULL COMMENT '报价评分';

