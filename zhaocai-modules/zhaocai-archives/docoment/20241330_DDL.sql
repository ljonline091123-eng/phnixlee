-- `hnjiantou-zhaocai-dev`.tb_material_type definition

CREATE TABLE `tb_material_type` (
                                    `id` bigint NOT NULL COMMENT '主键',
                                    `up_id` bigint DEFAULT NULL COMMENT '父级id',
                                    `material_name` varchar(200) DEFAULT NULL COMMENT '材料分类名称',
                                    `material_type` varchar(200) DEFAULT NULL COMMENT '材料分类类型',
                                    `material_code` varchar(100) DEFAULT NULL COMMENT '材料分类编码',
                                    `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                    `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                    `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                    `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                    `unit` varchar(100) DEFAULT NULL COMMENT '单位',
                                    `is_transaction` varchar(100) DEFAULT NULL COMMENT '是否交易标的物',
                                    `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                    `material_level` varchar(100) DEFAULT NULL COMMENT '层级',
                                    `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                    `state` int DEFAULT NULL COMMENT '状态',
                                    `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                    `wf_batch` varchar(100) DEFAULT NULL COMMENT '流程批次',
                                    `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                    `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                    `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                    `material_level_cd` varchar(100) DEFAULT NULL COMMENT '层级',
                                    `host_id` varchar(32) DEFAULT NULL COMMENT '主库同步id',
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='材料分类表';

-- `hnjiantou-zhaocai-dev`.tb_material_item definition

CREATE TABLE `tb_material_item` (
                                    `id` bigint NOT NULL COMMENT '主键',
                                    `type_id` bigint DEFAULT NULL COMMENT '材料类型id',
                                    `item_name` varchar(100) DEFAULT NULL COMMENT '材料特征名称',
                                    `item_code` varchar(100) DEFAULT NULL COMMENT '材料特征编号',
                                    `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                    `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                    `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                    `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                    `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                    `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                    `state` int DEFAULT NULL COMMENT '状态',
                                    `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                    `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                    `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                    `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                    `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                    `host_id` varchar(32) DEFAULT NULL COMMENT '同步主表id',
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='材料特征项';

-- `hnjiantou-zhaocai-dev`.tb_material_eigenvalue definition

CREATE TABLE `tb_material_eigenvalue` (
                                          `id` bigint NOT NULL COMMENT '主键',
                                          `eigenvalue_name` varchar(200) DEFAULT NULL COMMENT '特征值名称',
                                          `eigenvalue_code` varchar(100) DEFAULT NULL COMMENT '特征值编号',
                                          `type_id` bigint DEFAULT NULL COMMENT '材料类型id',
                                          `item_id` bigint DEFAULT NULL COMMENT '特征项id',
                                          `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                          `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                          `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                          `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                          `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                          `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                          `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                          `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                          `state` int DEFAULT NULL COMMENT '状态',
                                          `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                          `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                          `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                          `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                          `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                          `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='材料特征值表';

-- `hnjiantou-zhaocai-dev`.tb_material_details definition

CREATE TABLE `tb_material_details` (
                                       `id` bigint NOT NULL COMMENT '主键',
                                       `type_id` bigint DEFAULT NULL COMMENT '材料类型id',
                                       `material_code` varchar(200) DEFAULT NULL COMMENT '材料编号',
                                       `material_name` varchar(200) DEFAULT NULL COMMENT '材料名称',
                                       `type_name` varchar(200) DEFAULT NULL COMMENT '所属分类',
                                       `material_specifications` varchar(200) DEFAULT NULL COMMENT '规格',
                                       `unit` varchar(100) DEFAULT NULL COMMENT '单位',
                                       `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                       `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                       `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                       `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                       `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                       `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                       `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                       `is_main` varchar(100) DEFAULT NULL COMMENT '是否为主库同步数据',
                                       `state` int DEFAULT NULL COMMENT '状态',
                                       `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                       `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                       `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                       `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                       `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                       `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                       PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='材料详情';


-- `hnjiantou-zhaocai-dev`.tb_device_type definition

CREATE TABLE `tb_device_type` (
                                  `id` bigint NOT NULL COMMENT '主键',
                                  `up_id` bigint DEFAULT NULL COMMENT '父级id',
                                  `device_name` varchar(200) DEFAULT NULL COMMENT '设备分类名称',
                                  `device_type` varchar(200) DEFAULT NULL COMMENT '设备分类类型',
                                  `device_code` varchar(100) DEFAULT NULL COMMENT '设备分类编码',
                                  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                  `unit` varchar(100) DEFAULT NULL COMMENT '单位',
                                  `is_transaction` varchar(100) DEFAULT NULL COMMENT '是否交易标的物',
                                  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                  `device_level` varchar(100) DEFAULT NULL COMMENT '层级',
                                  `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                  `state` int DEFAULT NULL COMMENT '状态',
                                  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                  `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                  `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                  `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                  `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                  `device_level_cd` varchar(100) DEFAULT NULL COMMENT '层级',
                                  `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                  `subject_matter_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '交易标的物编号',
                                  `subject_matter_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '交易标的物名称',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备分类表';

-- `hnjiantou-zhaocai-dev`.tb_device_item definition

CREATE TABLE `tb_device_item` (
                                  `id` bigint NOT NULL COMMENT '主键',
                                  `type_id` bigint DEFAULT NULL COMMENT '设备类型id',
                                  `item_name` varchar(100) DEFAULT NULL COMMENT '设备特征名称',
                                  `item_code` varchar(100) DEFAULT NULL COMMENT '设备特征编号',
                                  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                  `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                  `state` int DEFAULT NULL COMMENT '状态',
                                  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                  `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                  `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                  `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                  `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                  `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备特征项';


-- `hnjiantou-zhaocai-dev`.tb_device_eigenvalue definition

CREATE TABLE `tb_device_eigenvalue` (
                                        `id` bigint NOT NULL COMMENT '主键',
                                        `eigenvalue_name` varchar(200) DEFAULT NULL COMMENT '特征值名称',
                                        `eigenvalue_code` varchar(100) DEFAULT NULL COMMENT '特征值编号',
                                        `type_id` bigint DEFAULT NULL COMMENT '设备类型id',
                                        `item_id` bigint DEFAULT NULL COMMENT '特征项id',
                                        `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                        `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                        `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                        `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                        `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                        `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                        `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                        `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                        `state` int DEFAULT NULL COMMENT '状态',
                                        `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                        `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                        `main_id` varchar(32) DEFAULT NULL COMMENT '关联主表id',
                                        `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                        `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                        `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备特征值表';


-- `hnjiantou-zhaocai-dev`.tb_device_details definition

CREATE TABLE `tb_device_details` (
                                     `id` bigint NOT NULL COMMENT '主键',
                                     `type_id` bigint DEFAULT NULL COMMENT '设备类型id',
                                     `device_code` varchar(200) DEFAULT NULL COMMENT '设备编号',
                                     `device_name` varchar(200) DEFAULT NULL COMMENT '设备名称',
                                     `type_name` varchar(200) DEFAULT NULL COMMENT '所属分类',
                                     `device_specifications` varchar(200) DEFAULT NULL COMMENT '规格',
                                     `unit` varchar(100) DEFAULT NULL COMMENT '单位',
                                     `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                     `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                     `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                     `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                     `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                     `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                     `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                     `is_main` varchar(100) DEFAULT NULL COMMENT '是否为主库同步数据',
                                     `state` int DEFAULT NULL COMMENT '状态',
                                     `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                     `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                     `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                     `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                     `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                     `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                     `feature` varchar(100) DEFAULT NULL COMMENT '特征项',
                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备详情';

-- `hnjiantou-zhaocai-dev`.tb_labour_type definition

CREATE TABLE `tb_labour_type` (
                                  `id` bigint NOT NULL COMMENT '主键',
                                  `up_id` bigint DEFAULT NULL COMMENT '父级id',
                                  `labour_name` varchar(200) DEFAULT NULL COMMENT '劳务分类名称',
                                  `labour_type` varchar(200) DEFAULT NULL COMMENT '劳务分类类型',
                                  `labour_code` varchar(100) DEFAULT NULL COMMENT '劳务分类编码',
                                  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                  `unit` varchar(100) DEFAULT NULL COMMENT '单位',
                                  `is_transaction` varchar(100) DEFAULT NULL COMMENT '是否交易标的物',
                                  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                  `labour_level` varchar(100) DEFAULT NULL COMMENT '层级',
                                  `labour_level_cd` varchar(100) DEFAULT NULL COMMENT '层级',
                                  `state` int DEFAULT NULL COMMENT '状态',
                                  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                  `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                  `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                  `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                  `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                  `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                  `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='劳务分类表';


-- `hnjiantou-zhaocai-dev`.tb_labour_item definition

CREATE TABLE `tb_labour_item` (
                                  `id` bigint NOT NULL COMMENT '主键',
                                  `type_id` bigint DEFAULT NULL COMMENT '劳务类型id',
                                  `item_name` varchar(100) DEFAULT NULL COMMENT '劳务特征名称',
                                  `item_code` varchar(100) DEFAULT NULL COMMENT '劳务特征编号',
                                  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                  `state` int DEFAULT NULL COMMENT '状态',
                                  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                  `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                  `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                  `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                  `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                  `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                  `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='劳务特征项';

-- `hnjiantou-zhaocai-dev`.tb_labour_eigenvalue definition

CREATE TABLE `tb_labour_eigenvalue` (
                                        `id` bigint NOT NULL COMMENT '主键',
                                        `eigenvalue_name` varchar(200) DEFAULT NULL COMMENT '特征值名称',
                                        `eigenvalue_code` varchar(100) DEFAULT NULL COMMENT '特征值编号',
                                        `type_id` bigint DEFAULT NULL COMMENT '劳务类型id',
                                        `item_id` bigint DEFAULT NULL COMMENT '特征项id',
                                        `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                        `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                        `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                        `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                        `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                        `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                        `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                        `state` int DEFAULT NULL COMMENT '状态',
                                        `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                        `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                        `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                        `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                        `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                        `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                        `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='劳务特征值表';

-- `hnjiantou-zhaocai-dev`.tb_labour_details definition

CREATE TABLE `tb_labour_details` (
                                     `id` bigint NOT NULL COMMENT '主键',
                                     `type_id` bigint DEFAULT NULL COMMENT '劳务类型id',
                                     `labour_code` varchar(200) DEFAULT NULL COMMENT '劳务编号',
                                     `labour_name` varchar(200) DEFAULT NULL COMMENT '劳务名称',
                                     `type_name` varchar(200) DEFAULT NULL COMMENT '所属分类',
                                     `item_and_eigenvalue` text COMMENT '特征项及特征值',
                                     `measurement_rules` varchar(200) DEFAULT NULL COMMENT '计量规则',
                                     `work_content` text COMMENT '工作内容',
                                     `unit` varchar(100) DEFAULT NULL COMMENT '单位',
                                     `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                     `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                     `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                     `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                     `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                     `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                     `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                     `is_main` varchar(100) DEFAULT NULL COMMENT '是否为主库同步数据',
                                     `state` int DEFAULT NULL COMMENT '状态',
                                     `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                     `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                     `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                     `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                     `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                     `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                     `feature` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '特征项及特征值',
                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='劳务详情';


-- `hnjiantou-zhaocai-dev`.tb_subcontracting_type definition

CREATE TABLE `tb_subcontracting_type` (
                                          `id` bigint NOT NULL COMMENT '主键',
                                          `up_id` bigint DEFAULT NULL COMMENT '父级id',
                                          `subcontracting_name` varchar(200) DEFAULT NULL COMMENT '专业分包分类名称',
                                          `subcontracting_type` varchar(200) DEFAULT NULL COMMENT '专业分包分类类型',
                                          `subcontracting_code` varchar(100) DEFAULT NULL COMMENT '专业分包分类编码',
                                          `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                          `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                          `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                          `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                          `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                          `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                          `unit` varchar(100) DEFAULT NULL COMMENT '单位',
                                          `is_transaction` varchar(100) DEFAULT NULL COMMENT '是否交易标的物',
                                          `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                          `subcontracting_level` varchar(100) DEFAULT NULL COMMENT '层级',
                                          `subcontracting_level_cd` varchar(100) DEFAULT NULL COMMENT '层级',
                                          `state` int DEFAULT NULL COMMENT '状态',
                                          `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                          `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                          `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                          `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                          `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                          `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                          `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专业分包分类表';


-- `hnjiantou-zhaocai-dev`.tb_subcontracting_item definition

CREATE TABLE `tb_subcontracting_item` (
                                          `id` bigint NOT NULL COMMENT '主键',
                                          `type_id` bigint DEFAULT NULL COMMENT '专业分包类型id',
                                          `item_name` varchar(100) DEFAULT NULL COMMENT '专业分包特征名称',
                                          `item_code` varchar(100) DEFAULT NULL COMMENT '专业分包特征编号',
                                          `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                          `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                          `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                          `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                          `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                          `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                          `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                          `state` int DEFAULT NULL COMMENT '状态',
                                          `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                          `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                          `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                          `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                          `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                          `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                          `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专业分包特征项';


-- `hnjiantou-zhaocai-dev`.tb_subcontracting_eigenvalue definition

CREATE TABLE `tb_subcontracting_eigenvalue` (
                                                `id` bigint NOT NULL COMMENT '主键',
                                                `eigenvalue_name` varchar(200) DEFAULT NULL COMMENT '特征值名称',
                                                `eigenvalue_code` varchar(100) DEFAULT NULL COMMENT '特征值编号',
                                                `type_id` bigint DEFAULT NULL COMMENT '专业分包类型id',
                                                `item_id` bigint DEFAULT NULL COMMENT '特征项id',
                                                `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                                `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                                `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                                `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                                `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                                `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                                `state` int DEFAULT NULL COMMENT '状态',
                                                `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                                `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                                `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                                `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                                `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                                `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                                `is_main` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为主库同步数据',
                                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专业分包特征值表';


-- `hnjiantou-zhaocai-dev`.tb_subcontracting_details definition

CREATE TABLE `tb_subcontracting_details` (
                                             `id` bigint NOT NULL COMMENT '主键',
                                             `type_id` bigint DEFAULT NULL COMMENT '专业分包类型id',
                                             `subcontracting_code` varchar(200) DEFAULT NULL COMMENT '专业分包编号',
                                             `subcontracting_name` varchar(200) DEFAULT NULL COMMENT '专业分包名称',
                                             `type_name` varchar(200) DEFAULT NULL COMMENT '所属分类',
                                             `item_and_eigenvalue` text COMMENT '特征项及特征值',
                                             `measurement_rules` varchar(200) DEFAULT NULL COMMENT '计量规则',
                                             `work_content` text COMMENT '工作内容',
                                             `unit` varchar(100) DEFAULT NULL COMMENT '单位',
                                             `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                             `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                             `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                             `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                             `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                             `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                                             `is_main` varchar(100) DEFAULT NULL COMMENT '是否为主库同步数据',
                                             `state` int DEFAULT NULL COMMENT '状态',
                                             `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                             `wf_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程批次',
                                             `main_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '关联主表id',
                                             `organ_code` varchar(200) DEFAULT NULL COMMENT '机构编码',
                                             `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                             `host_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '同步主表id',
                                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专业分包详情';


-- 测试库表结构

-- jiantou_base.device_archives definition

CREATE TABLE `device_archives` (
                                   `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                   `device_class_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备分类id',
                                   `device_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备编号',
                                   `device_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
                                   `feature` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '特征项',
                                   `measure_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '计量单位',
                                   `subject_matter` tinyint(1) DEFAULT NULL COMMENT '是否交易标的物 true是 false不是',
                                   `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                   `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                   `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                   `valid` int DEFAULT '0',
                                   `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='设备档案主表';

-- jiantou_base.device_class definition

CREATE TABLE `device_class` (
                                `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                `device_class_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备分类编码',
                                `device_class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备分类名称',
                                `device_class_type` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '设备分类下级类型 1特征项 2具体设备',
                                `measure_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '计量单位',
                                `subject_matter_code` varchar(64) DEFAULT NULL COMMENT '交易标的物编号',
                                `subject_matter_name` varchar(64) DEFAULT NULL COMMENT '交易标的物名称',
                                `subject_matter` tinyint(1) DEFAULT '0' COMMENT '是否交易标的物 true是 false不是',
                                `class_level` varchar(5) DEFAULT NULL COMMENT '分类层级',
                                `class_level_cd` varchar(5) DEFAULT NULL COMMENT '分类层级',
                                `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '父节点',
                                `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                `valid` int DEFAULT '0',
                                `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                `is_tb` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '是否同步中台0否1是2需要更新',
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='设备分类主表';

-- jiantou_base.device_feature definition

CREATE TABLE `device_feature` (
                                  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                  `device_class_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '设备分类id',
                                  `feature_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '特征项编号',
                                  `feature_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '特征项名称',
                                  `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                  `valid` int DEFAULT '0',
                                  `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                  `is_tb` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '是否同步中台0否1是2需要更新',
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='设备特征项表主表';

-- jiantou_base.device_feature_value definition

CREATE TABLE `device_feature_value` (
                                        `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                        `device_feature_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '设备特征项id',
                                        `feature_value_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '特征值名称',
                                        `feature_value_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '特征值编号',
                                        `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                        `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                        `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                        `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                        `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                        `valid` int DEFAULT '0',
                                        `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                        `is_tb` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '是否同步中台0否1是2需要更新',
                                        PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='设备特征值表主表';


-- jiantou_base.labor_services_archives definition

CREATE TABLE `labor_services_archives` (
                                           `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                           `labor_services_class_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '劳务分类id',
                                           `labor_services_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '劳务编号',
                                           `labor_services_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '劳务名称',
                                           `feature` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '特征项及特征值',
                                           `measure_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '计量单位',
                                           `specs` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '规格',
                                           `metrological_rules` varchar(200) DEFAULT NULL COMMENT '计量规则',
                                           `basic_job` varchar(2000) DEFAULT NULL COMMENT '基本工作内容',
                                           `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                           `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                           `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                           `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                           `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                           `valid` int DEFAULT '0',
                                           `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                           PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='劳务档案主表';


-- jiantou_base.labor_services_class definition

CREATE TABLE `labor_services_class` (
                                        `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                        `labor_services_class_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '劳务分类编码',
                                        `labor_services_class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '劳务分类名称',
                                        `labor_services_class_type` varchar(1) DEFAULT NULL COMMENT '劳务分类下级类型 1特征项 2具体档案',
                                        `measure_unit` varchar(64) DEFAULT NULL COMMENT '计量单位',
                                        `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '父节点',
                                        `subject_matter` tinyint(1) DEFAULT '0' COMMENT '是否交易标的物 true是 false不是',
                                        `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                        `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                        `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                        `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                        `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                        `valid` int DEFAULT '0',
                                        `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                        PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='劳务分类主表';


-- jiantou_base.labor_services_feature definition

CREATE TABLE `labor_services_feature` (
                                          `id` varchar(32) NOT NULL,
                                          `labor_services_class_id` varchar(32) DEFAULT NULL COMMENT '劳务分类id',
                                          `feature_code` varchar(32) NOT NULL COMMENT '特征项编号',
                                          `feature_name` varchar(32) NOT NULL COMMENT '特征项名称',
                                          `create_id` varchar(32) DEFAULT NULL COMMENT '创建者id',
                                          `create_by` varchar(32) DEFAULT NULL COMMENT '创建者',
                                          `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                          `update_by` varchar(32) DEFAULT NULL COMMENT '更新者',
                                          `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                          `valid` int DEFAULT '0',
                                          `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='劳务特征项表主表';


-- jiantou_base.labor_services_feature_value definition

CREATE TABLE `labor_services_feature_value` (
                                                `id` varchar(32) NOT NULL,
                                                `labor_services_feature_id` varchar(32) DEFAULT NULL COMMENT '劳务特征项id',
                                                `feature_value_name` varchar(32) NOT NULL COMMENT '特征值名称',
                                                `feature_value_code` varchar(32) NOT NULL COMMENT '特征值编号',
                                                `create_id` varchar(32) DEFAULT NULL COMMENT '创建者id',
                                                `create_by` varchar(32) DEFAULT NULL COMMENT '创建者',
                                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                                `update_by` varchar(32) DEFAULT NULL COMMENT '更新者',
                                                `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                                `valid` int DEFAULT '0',
                                                `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='劳务特征值表主表';


-- jiantou_base.major_subcontracting_archives definition

CREATE TABLE `major_subcontracting_archives` (
                                                 `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                                 `major_subcontracting_class_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专业分包分类id',
                                                 `major_subcontracting_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专业分包编号',
                                                 `major_subcontracting_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专业分包名称',
                                                 `feature` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '特征项及特征值',
                                                 `measure_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '计量单位',
                                                 `specs` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '规格',
                                                 `metrological_rules` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '计量规则',
                                                 `basic_job` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '基本工作内容',
                                                 `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                                 `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                                 `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                                 `valid` int DEFAULT '0',
                                                 `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='专业分包档案主表';


-- jiantou_base.major_subcontracting_class definition

CREATE TABLE `major_subcontracting_class` (
                                              `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                              `major_subcontracting_class_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专业分包分类编码',
                                              `major_subcontracting_class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专业分包分类名称',
                                              `major_subcontracting_class_type` varchar(1) DEFAULT NULL COMMENT '专业分包分类下级类型 1特征项 2具体档案',
                                              `measure_unit` varchar(64) DEFAULT NULL COMMENT '计量单位',
                                              `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '父节点',
                                              `subject_matter` tinyint(1) DEFAULT '0' COMMENT '是否交易标的物 true是 false不是',
                                              `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                              `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                              `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                              `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                              `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                              `valid` int DEFAULT '0',
                                              `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                              PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='专业分包分类主表';


-- jiantou_base.major_subcontracting_feature definition

CREATE TABLE `major_subcontracting_feature` (
                                                `id` varchar(32) NOT NULL,
                                                `major_subcontracting_class_id` varchar(32) DEFAULT NULL COMMENT '专业分包分类id',
                                                `feature_code` varchar(32) NOT NULL COMMENT '特征项编号',
                                                `feature_name` varchar(32) NOT NULL COMMENT '特征项名称',
                                                `create_id` varchar(32) DEFAULT NULL COMMENT '创建者id',
                                                `create_by` varchar(32) DEFAULT NULL COMMENT '创建者',
                                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                                `update_by` varchar(32) DEFAULT NULL COMMENT '更新者',
                                                `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                                `valid` int DEFAULT '0',
                                                `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='专业分包特征项表主表';

-- jiantou_base.major_subcontracting_feature_value definition

CREATE TABLE `major_subcontracting_feature_value` (
                                                      `id` varchar(32) NOT NULL,
                                                      `major_subcontracting_feature_id` varchar(32) DEFAULT NULL COMMENT '专业分包特征项id',
                                                      `feature_value_name` varchar(32) NOT NULL COMMENT '特征值名称',
                                                      `feature_value_code` varchar(32) NOT NULL COMMENT '特征值编号',
                                                      `create_id` varchar(32) DEFAULT NULL COMMENT '创建者id',
                                                      `create_by` varchar(32) DEFAULT NULL COMMENT '创建者',
                                                      `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                                      `update_by` varchar(32) DEFAULT NULL COMMENT '更新者',
                                                      `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                                      `valid` int DEFAULT '0',
                                                      `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                                      PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='专业分包特征值表主表';


-- jiantou_base.mtr_archives definition

CREATE TABLE `mtr_archives` (
                                `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                `mtr_class_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '材料分类id',
                                `mtr_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '材料编号',
                                `mtr_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '材料名称',
                                `specs` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '规格',
                                `measure_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '计量单位',
                                `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                `valid` int DEFAULT '0',
                                `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='材料档案主表';


-- jiantou_base.mtr_class definition

CREATE TABLE `mtr_class` (
                             `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                             `mtr_class_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '材料分类编码',
                             `mtr_class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '材料分类名称',
                             `mtr_class_type` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '材料分类下级类型 1特征项 2具体材料',
                             `measure_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '计量单位',
                             `subject_matter` tinyint(1) DEFAULT '0' COMMENT '是否交易标的物 true是 false不是',
                             `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '父节点',
                             `class_level` varchar(5) DEFAULT NULL COMMENT '分类层级',
                             `class_level_cd` varchar(5) DEFAULT NULL COMMENT '分类层级',
                             `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                             `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                             `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                             `valid` int DEFAULT '0',
                             `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                             `is_tb` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '是否同步中台0否1是2需要更新',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE KEY `code` (`mtr_class_code`,`valid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='材料分类表主表';


-- jiantou_base.mtr_feature definition

CREATE TABLE `mtr_feature` (
                               `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                               `mtr_class_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '材料分类id',
                               `feature_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '特征项编号',
                               `feature_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '特征项名称',
                               `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                               `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                               `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                               `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                               `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                               `valid` int DEFAULT '0',
                               `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                               `is_tb` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '是否同步中台0否1是2需要更新',
                               PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='材料特征项表主表';


-- jiantou_base.mtr_feature_value definition

CREATE TABLE `mtr_feature_value` (
                                     `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                     `mtr_feature_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '材料特征项id',
                                     `feature_value_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '特征值名称',
                                     `feature_value_code` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '特征值编号',
                                     `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者id',
                                     `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建者',
                                     `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                     `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新者',
                                     `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                     `valid` int DEFAULT '0',
                                     `son_id` bigint(32) DEFAULT NULL COMMENT '副库id',
                                     `is_tb` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '是否同步中台0否1是2需要更新',
                                     PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='材料特征值表主表';



-- `hnjiantou-zhaocai-dev`.tb_material_approve definition

CREATE TABLE `tb_material_approve` (
                                       `id` bigint NOT NULL COMMENT '主键',
                                       `type` varchar(100) DEFAULT NULL COMMENT '类型',
                                       `state` int DEFAULT NULL COMMENT '状态',
                                       `join_id` bigint DEFAULT NULL COMMENT '关联业务id',
                                       `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
                                       `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                       `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
                                       `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
                                       `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                       `del_flag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                       `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
                                       `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                       PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料审批辅表';

-- `hnjiantou-zhaocai-dev`.t_interface_log definition

CREATE TABLE `tb_material_interface_log` (
                                             `id` bigint NOT NULL COMMENT 'id',
                                             `send_time` datetime DEFAULT NULL COMMENT '访问时间',
                                             `send_data` text COMMENT '访问参数',
                                             `send_url` varchar(512) DEFAULT NULL COMMENT '访问地址',
                                             `receive_data` text COMMENT '返回参数',
                                             `receive_time` datetime DEFAULT NULL COMMENT '返回时间',
                                             `log_type` varchar(64) DEFAULT NULL COMMENT '日志类型',
                                             `flag` varchar(64) DEFAULT NULL COMMENT '是否成功',
                                             `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
                                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                             `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
                                             `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                             `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
                                             `business_id` varchar(64) DEFAULT NULL,
                                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='接口访问记录对象';
