

ALTER TABLE tb_agreement_materials_list ADD base_price decimal(20,5) NULL COMMENT '基价';
ALTER TABLE tb_agreement_materials_list ADD floating_price decimal(20,5) NULL COMMENT '浮动价';
ALTER TABLE tb_agreement_materials_list ADD floating_rate decimal(5,2) NULL COMMENT '浮动率（百分比）';
