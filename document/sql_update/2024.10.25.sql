ALTER TABLE `hnjiantou-zhaocai-dev`.tb_expert ADD state int NULL COMMENT '审批状态';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_expert ADD wf_process_id varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '流程实例 id';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_expert ADD process_type int NULL COMMENT '流程类型';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_expert ADD operate_comment varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '审批批语';

update `hnjiantou-zhaocai-dev`.tb_expert set state = 3 where state is null
