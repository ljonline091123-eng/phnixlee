

-- 先保证我们系统有该 税率 的字典值 rax_archives

-- 将[清单列表] 税率财务编码 税率名称 按字典值补全
UPDATE `hnjiantou-zhaocai-dev`.tb_materials_list AS m
    JOIN `hnjiantou-zhaocai-dev`.sys_dict_data AS d
ON d.dict_type = 'rax_archives' and m.tax_rate = d.remark
    SET m.tax_rate_code = d.dict_value,
        m.tax_rate_name = d.dict_label;

-- 将[供应商投标清单列表] 税率财务编码 税率名称 按字典值补全
UPDATE `hnjiantou-zhaocai-dev`.tb_bidding_list_quotation AS b
    JOIN `hnjiantou-zhaocai-dev`.sys_dict_data AS d
ON d.dict_type = 'rax_archives' and b.tax_rate  = d.remark
    SET b.tax_rate_code = d.dict_value,
        b.tax_rate_name = d.dict_label;

-- 将[合同清单列表] 税率财务编码 税率名称 按字典值补全
UPDATE `hnjiantou-zhaocai-dev`.tb_agreement_materials_list AS a
    JOIN `hnjiantou-zhaocai-dev`.sys_dict_data AS d
ON d.dict_type = 'rax_archives' and a.sign_tax_rate = d.remark
    SET a.sign_tax_rate_code = d.dict_value,
        a.sign_tax_rate_name = d.dict_label;


-- 用来排查这三张表是否有不在 税率标准值 范围外的数据
-- -- 将[清单列表]
-- SELECT m.*
-- FROM `hnjiantou-zhaocai-dev`.tb_materials_list AS m
--          LEFT JOIN `hnjiantou-zhaocai-dev`.sys_dict_data AS d
--                    ON m.tax_rate = d.remark AND d.dict_type = 'rax_archives'
-- WHERE d.dict_label IS NULL;
-- -- 将[供应商投标清单列表]
-- SELECT b.*
-- FROM `hnjiantou-zhaocai-dev`.tb_bidding_list_quotation AS b
--          LEFT JOIN `hnjiantou-zhaocai-dev`.sys_dict_data AS d
--                    ON b.tax_rate = d.remark AND d.dict_type = 'rax_archives'
-- WHERE d.dict_label IS NULL;
-- -- 将[合同清单列表]
-- SELECT a.*
-- FROM `hnjiantou-zhaocai-dev`.tb_agreement_materials_list AS a
--          LEFT JOIN `hnjiantou-zhaocai-dev`.sys_dict_data AS d
--                    ON a.sign_tax_rate = d.remark AND d.dict_type = 'rax_archives'
-- WHERE d.dict_label IS NULL;








