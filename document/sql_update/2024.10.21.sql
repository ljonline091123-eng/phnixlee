# 2024.10.21 下午6:28 采购物料清单 表增加字段

ALTER TABLE `hnjiantou-zhaocai-dev`.tb_materials_list ADD code varchar(64) NULL COMMENT '商品编号';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_materials_list ADD name varchar(64) NULL COMMENT '商品名称';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_materials_list ADD category varchar(64) NULL COMMENT '商品规格';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_materials_list ADD unit_name varchar(64) NULL COMMENT '商品单位';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_materials_list ADD quantity decimal(20,5) NULL COMMENT '商品数量';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_materials_list ADD push_flag varchar(2) DEFAULT 'N' NULL COMMENT '是否推送 Y:推送 N:未推送';
