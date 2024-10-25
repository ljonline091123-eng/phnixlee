# 2024.10.24 下午6:41 合同物料清单 表新增字段

ALTER TABLE `hnjiantou-zhaocai-dev`.tb_agreement_materials_list ADD offer_goods_code varchar(64) NULL COMMENT '易料市集商品编码';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_agreement_materials_list ADD goods_name varchar(128) NULL COMMENT '易料市集商品名';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_agreement_materials_list ADD offer_brand varchar(50) NULL COMMENT '易料市集品牌';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_agreement_materials_list ADD offer_price decimal(20,5) NULL COMMENT '易料市集含税单价';