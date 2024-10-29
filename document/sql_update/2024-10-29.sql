# 2024.10.29 下午15:00 采购物料清单,易料采购合同清单,合同物料清单 表新增字段

ALTER TABLE `hnjiantou-zhaocai-dev`.tb_materials_list ADD sku_id varchar(64) NULL COMMENT '易料市集商品id';

ALTER TABLE `hnjiantou-zhaocai-dev`.tb_market_material_list ADD sku_id varchar(64) NULL COMMENT '易料市集商品id';

ALTER TABLE `hnjiantou-zhaocai-dev`.tb_agreement_materials_list ADD sku_id varchar(64) NULL COMMENT '易料市集商品id';