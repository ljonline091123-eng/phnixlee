
# 结算阶段 数据库字典值settlement_stage是请求大汉的字典列表{@link UnderlingPlatformUrlEnum#DICT_LIST_MAP} 但是下面还是备用加上
INSERT INTO `hnjiantou-zhaocai-dev`.sys_dict_type
(dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
VALUES(149, '结算阶段', 'settlement_stage', '0', '18374562968', '2024-12-04 09:39:57', '18374562968', '2024-12-04 09:40:09', '结算阶段');

INSERT INTO `hnjiantou-zhaocai-dev`.sys_dict_data
(dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
VALUES(359, 4, '最终结算', '4', 'settlement_stage', NULL, 'default', 'N', '0', '18374562968', '2024-12-04 09:41:36', '', NULL, '最终结算');
INSERT INTO `hnjiantou-zhaocai-dev`.sys_dict_data
(dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
VALUES(358, 3, '完工结算', '3', 'settlement_stage', NULL, 'default', 'N', '0', '18374562968', '2024-12-04 09:41:28', '', NULL, '完工结算');
INSERT INTO `hnjiantou-zhaocai-dev`.sys_dict_data
(dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
VALUES(357, 2, '过程结算', '2', 'settlement_stage', NULL, 'default', 'N', '0', '18374562968', '2024-12-04 09:41:19', '', NULL, '过程结算');
INSERT INTO `hnjiantou-zhaocai-dev`.sys_dict_data
(dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
VALUES(356, 1, '其他', '1', 'settlement_stage', NULL, 'default', 'N', '0', '18374562968', '2024-12-04 09:40:57', '18374562968', '2024-12-04 09:41:04', '其他');




# 专家类型多选
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_expert MODIFY COLUMN expert_type varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '专家类别（1技术类 2经济类）';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_expert_change MODIFY COLUMN expert_type varchar(50) NULL COMMENT '专家类别（1技术类 2经济类）';
