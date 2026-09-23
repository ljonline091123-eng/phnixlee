# 2024.11.19 下午17:24 专家、专家修改 表修改业态类型

ALTER TABLE `hnjiantou-zhaocai-dev`.tb_expert MODIFY COLUMN business_type varchar(255) NULL COMMENT '业态';

ALTER TABLE `hnjiantou-zhaocai-dev`.tb_expert_change MODIFY COLUMN business_type varchar(255) NULL COMMENT '业态';

