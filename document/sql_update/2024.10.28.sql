# 2024.10.28 上午11:18 采购物料清单 表新增字段

ALTER TABLE `hnjiantou-zhaocai-dev`.tb_materials_list ADD offer_price decimal(20,5) NULL COMMENT '易料市集含税单价';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_materials_list ADD offer_brand varchar(100) NULL COMMENT '易料市集品牌';
