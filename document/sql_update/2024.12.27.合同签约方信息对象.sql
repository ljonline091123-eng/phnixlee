


CREATE TABLE `tb_agreement_party_info` (
   `id` bigint NOT NULL COMMENT '主键 id',
   `agreement_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同id',
   `role_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同角色类型(数据字典CON_ROLE_TYPE)',
   `signer_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签约单位编号',
   `signer_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签约单位名称',
   `signer_rate` decimal(16,2) DEFAULT NULL COMMENT '签约单位所占比例',
   `signer_bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签约单位银行账户名称',
   `signer_bank_open` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签约单位开户支行',
   `signer_bank_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签约单位银行账号',
   `signer_taxpayer_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签约单位纳税人识别号',
   `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
   `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
   `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
   `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
   `update_time` datetime DEFAULT NULL COMMENT '修改时间',
   `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
   PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同签约方信息';
