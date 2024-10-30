
# 2024.10.16 下午6:36 第三方数据合约规划推送记录 表增加字段
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_contract_planning_push_record ADD scheme_id bigint NULL COMMENT '采购方案id';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_contract_planning_push_record ADD notice_id bigint NULL COMMENT '招标对象id';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_contract_planning_push_record ADD procurement_scheme_code varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '采购方案编号';
# 2024.10.17 上午10:00 价格分析-劳务 视图新增
-- `hnjiantou-zhaocai-prod`.v_price_analysis_labor source

create or replace
    algorithm = UNDEFINED view `hnjiantou-zhaocai-prod`.`v_price_analysis_labor` as
select
    `a`.`unique_id` as `unique_id`,
    `a`.`con_id` as `con_id`,
    `a`.`subject_dtl_code` as `subject_dtl_code`,
    `a`.`subject_dtl_name` as `subject_dtl_name`,
    `a`.`specs` as `specs`,
    `a`.`measure_unit` as `measure_unit`,
    `a`.`quantity` as `quantity`,
    `a`.`ntax_price` as `ntax_price`,
    `a`.`last_price` as `last_price`,
    `a`.`create_time` as `create_time`,
    `a`.`project_id` as `project_id`,
    `a`.`contract_unique_id` as `contract_unique_id`,
    `a`.`con_code` as `con_code`,
    `a`.`con_name` as `con_name`,
    `a`.`sign_date` as `sign_date`,
    `b`.`min_price` as `min_price`,
    `b`.`max_price` as `max_price`,
    (sum((`b`.`min_price` + `b`.`max_price`)) / 2) as `avg_price`,
    `c`.`initial_price` as `initial_price`,
    `tmp`.`min_account_code` as `min_account_code`,
    `tmp`.`min_account_full_name` as `min_account_full_name`,
    `tmp`.`belonging_org_id` as `belonging_org_id`,
    `tmp`.`project_department_id` as `project_department_id`
from
    ((((
        select
            `cll`.`unique_id` as `unique_id`,
            `cll`.`con_id` as `con_id`,
            `cll`.`subject_dtl_code` as `subject_dtl_code`,
            `cll`.`subject_dtl_name` as `subject_dtl_name`,
            `cll`.`specs` as `specs`,
            `cll`.`measure_unit` as `measure_unit`,
            `cll`.`quantity` as `quantity`,
            `cll`.`ntax_price` as `ntax_price`,
            `cll`.`ntax_price` as `last_price`,
            `cll`.`create_time` as `create_time`,
            `cb`.`project_id` as `project_id`,
            `cb`.`unique_id` as `contract_unique_id`,
            `cb`.`con_code` as `con_code`,
            `cb`.`con_name` as `con_name`,
            `cb`.`sign_date` as `sign_date`
        from
            (`hnjiantou-zhaocai-prod`.`contract_list_labor` `cll`
                join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                (((`cll`.`con_id` = `cb`.`id`)
                    and (`cb`.`last_version_flag` = 1)
                    and (`cb`.`proc_status` = 4))))) `a`
        left join (
            select
                `cll`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `cll`.`subject_dtl_code` as `subject_dtl_code`,
                `cll`.`subject_dtl_name` as `subject_dtl_name`,
                min(`cll`.`ntax_price`) as `min_price`,
                max(`cll`.`ntax_price`) as `max_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_labor` `cll`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`cll`.`con_id` = `cb`.`id`)
                        and (`cb`.`proc_status` = 4))))
            group by
                `cll`.`subject_dtl_code`,
                `cb`.`unique_id`) `b` on
        (((`a`.`subject_dtl_code` = `b`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `b`.`contract_unique_id`))))
        left join (
            select
                `cll`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `cll`.`subject_dtl_code` as `subject_dtl_code`,
                `cll`.`subject_dtl_name` as `subject_dtl_name`,
                `cll`.`ntax_price` as `initial_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_labor` `cll`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`cll`.`con_id` = `cb`.`id`)
                        and (`cb`.`version` = 0))))) `c` on
        (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
        left join `hnjiantou-zhaocai-prod`.`tb_min_project` `tmp` on
        ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;


# 2024.10.17 上午10:15 价格分析-租赁设备 视图新增
-- `hnjiantou-zhaocai-prod`.v_price_analysis_leased_device source

create or replace
    algorithm = UNDEFINED view `hnjiantou-zhaocai-prod`.`v_price_analysis_leased_device` as
select
    `a`.`unique_id` as `unique_id`,
    `a`.`con_id` as `con_id`,
    `a`.`subject_dtl_code` as `subject_dtl_code`,
    `a`.`subject_dtl_name` as `subject_dtl_name`,
    `a`.`specs` as `specs`,
    `a`.`measure_unit` as `measure_unit`,
    `a`.`brand` as `brand`,
    `a`.`rent_mode` as `rent_mode`,
    `a`.`rent_unit` as `rent_unit`,
    `a`.`rent_time` as `rent_time`,
    `a`.`number` as `quantity`,
    `a`.`quantity` as `number`,
    `a`.`ntax_price` as `ntax_price`,
    `a`.`last_price` as `last_price`,
    `a`.`create_time` as `create_time`,
    `a`.`project_id` as `project_id`,
    `a`.`contract_unique_id` as `contract_unique_id`,
    `a`.`con_code` as `con_code`,
    `a`.`con_name` as `con_name`,
    `a`.`sign_date` as `sign_date`,
    `b`.`min_price` as `min_price`,
    `b`.`max_price` as `max_price`,
    (sum((`b`.`min_price` + `b`.`max_price`)) / 2) as `avg_price`,
    `c`.`initial_price` as `initial_price`,
    `tmp`.`min_account_code` as `min_account_code`,
    `tmp`.`min_account_full_name` as `min_account_full_name`,
    `tmp`.`belonging_org_id` as `belonging_org_id`,
    `tmp`.`project_department_id` as `project_department_id`
from
    ((((
        select
            `clld`.`unique_id` as `unique_id`,
            `clld`.`con_id` as `con_id`,
            `clld`.`subject_dtl_code` as `subject_dtl_code`,
            `clld`.`subject_dtl_name` as `subject_dtl_name`,
            `clld`.`specs` as `specs`,
            `clld`.`measure_unit` as `measure_unit`,
            `clld`.`brand` as `brand`,
            `clld`.`rent_mode` as `rent_mode`,
            `clld`.`rent_unit` as `rent_unit`,
            `clld`.`rent_time` as `rent_time`,
            `clld`.`number` as `number`,
            `clld`.`quantity` as `quantity`,
            `clld`.`ntax_price` as `ntax_price`,
            `clld`.`ntax_price` as `last_price`,
            `clld`.`create_time` as `create_time`,
            `cb`.`project_id` as `project_id`,
            `cb`.`unique_id` as `contract_unique_id`,
            `cb`.`con_code` as `con_code`,
            `cb`.`con_name` as `con_name`,
            `cb`.`sign_date` as `sign_date`
        from
            (`hnjiantou-zhaocai-prod`.`contract_list_leased_device` `clld`
                join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                (((`clld`.`con_id` = `cb`.`id`)
                    and (`cb`.`last_version_flag` = 1)
                    and (`cb`.`proc_status` = 4))))) `a`
        left join (
            select
                `clld`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `clld`.`subject_dtl_code` as `subject_dtl_code`,
                `clld`.`subject_dtl_name` as `subject_dtl_name`,
                min(`clld`.`ntax_price`) as `min_price`,
                max(`clld`.`ntax_price`) as `max_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_leased_device` `clld`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`clld`.`con_id` = `cb`.`id`)
                        and (`cb`.`proc_status` = 4))))
            group by
                `clld`.`subject_dtl_code`,
                `cb`.`unique_id`) `b` on
        (((`a`.`subject_dtl_code` = `b`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `b`.`contract_unique_id`))))
        left join (
            select
                `clld`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `clld`.`subject_dtl_code` as `subject_dtl_code`,
                `clld`.`subject_dtl_name` as `subject_dtl_name`,
                `clld`.`ntax_price` as `initial_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_leased_device` `clld`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`clld`.`con_id` = `cb`.`id`)
                        and (`cb`.`version` = 0))))) `c` on
        (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
        left join `hnjiantou-zhaocai-prod`.`tb_min_project` `tmp` on
        ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;


# 2024.10.17 上午10:20 价格分析-租赁材料 视图新增
-- `hnjiantou-zhaocai-prod`.v_price_analysis_leased_materials source

create or replace
    algorithm = UNDEFINED view `hnjiantou-zhaocai-prod`.`v_price_analysis_leased_materials` as
select
    `a`.`unique_id` as `unique_id`,
    `a`.`con_id` as `con_id`,
    `a`.`subject_dtl_code` as `subject_dtl_code`,
    `a`.`subject_dtl_name` as `subject_dtl_name`,
    `a`.`specs` as `specs`,
    `a`.`measure_unit` as `measure_unit`,
    `a`.`brand` as `brand`,
    `a`.`rent_mode` as `rent_mode`,
    `a`.`rent_unit` as `rent_unit`,
    `a`.`rent_time` as `rent_time`,
    `a`.`number` as `quantity`,
    `a`.`quantity` as `number`,
    `a`.`ntax_price` as `ntax_price`,
    `a`.`last_price` as `last_price`,
    `a`.`create_time` as `create_time`,
    `a`.`project_id` as `project_id`,
    `a`.`contract_unique_id` as `contract_unique_id`,
    `a`.`con_code` as `con_code`,
    `a`.`con_name` as `con_name`,
    `a`.`sign_date` as `sign_date`,
    `b`.`min_price` as `min_price`,
    `b`.`max_price` as `max_price`,
    (sum((`b`.`min_price` + `b`.`max_price`)) / 2) as `avg_price`,
    `c`.`initial_price` as `initial_price`,
    `tmp`.`min_account_code` as `min_account_code`,
    `tmp`.`min_account_full_name` as `min_account_full_name`,
    `tmp`.`belonging_org_id` as `belonging_org_id`,
    `tmp`.`project_department_id` as `project_department_id`
from
    ((((
        select
            `cllm`.`unique_id` as `unique_id`,
            `cllm`.`con_id` as `con_id`,
            `cllm`.`subject_dtl_code` as `subject_dtl_code`,
            `cllm`.`subject_dtl_name` as `subject_dtl_name`,
            `cllm`.`specs` as `specs`,
            `cllm`.`measure_unit` as `measure_unit`,
            `cllm`.`brand` as `brand`,
            `cllm`.`rent_mode` as `rent_mode`,
            `cllm`.`rent_unit` as `rent_unit`,
            `cllm`.`rent_time` as `rent_time`,
            `cllm`.`number` as `number`,
            `cllm`.`quantity` as `quantity`,
            `cllm`.`ntax_price` as `ntax_price`,
            `cllm`.`ntax_price` as `last_price`,
            `cllm`.`create_time` as `create_time`,
            `cb`.`project_id` as `project_id`,
            `cb`.`unique_id` as `contract_unique_id`,
            `cb`.`con_code` as `con_code`,
            `cb`.`con_name` as `con_name`,
            `cb`.`sign_date` as `sign_date`
        from
            (`hnjiantou-zhaocai-prod`.`contract_list_leased_materials` `cllm`
                join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                (((`cllm`.`con_id` = `cb`.`id`)
                    and (`cb`.`last_version_flag` = 1)
                    and (`cb`.`proc_status` = 4))))) `a`
        left join (
            select
                `cllm`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `cllm`.`subject_dtl_code` as `subject_dtl_code`,
                `cllm`.`subject_dtl_name` as `subject_dtl_name`,
                min(`cllm`.`ntax_price`) as `min_price`,
                max(`cllm`.`ntax_price`) as `max_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_leased_materials` `cllm`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`cllm`.`con_id` = `cb`.`id`)
                        and (`cb`.`proc_status` = 4))))
            group by
                `cllm`.`subject_dtl_code`,
                `cb`.`unique_id`) `b` on
        (((`a`.`subject_dtl_code` = `b`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `b`.`contract_unique_id`))))
        left join (
            select
                `cllm`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `cllm`.`subject_dtl_code` as `subject_dtl_code`,
                `cllm`.`subject_dtl_name` as `subject_dtl_name`,
                `cllm`.`ntax_price` as `initial_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_leased_materials` `cllm`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`cllm`.`con_id` = `cb`.`id`)
                        and (`cb`.`version` = 0))))) `c` on
        (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
        left join `hnjiantou-zhaocai-prod`.`tb_min_project` `tmp` on
        ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;

# 2024.10.17 上午10:25 价格分析-购买材料 视图新增
-- `hnjiantou-zhaocai-prod`.v_price_analysis_materials source

create or replace
    algorithm = UNDEFINED view `hnjiantou-zhaocai-prod`.`v_price_analysis_materials` as
select
    `a`.`unique_id` as `unique_id`,
    `a`.`con_id` as `con_id`,
    `a`.`subject_dtl_code` as `subject_dtl_code`,
    `a`.`subject_dtl_name` as `subject_dtl_name`,
    `a`.`specs` as `specs`,
    `a`.`measure_unit` as `measure_unit`,
    `a`.`quantity` as `quantity`,
    `a`.`ntax_price` as `ntax_price`,
    `a`.`last_price` as `last_price`,
    `a`.`create_time` as `create_time`,
    `a`.`project_id` as `project_id`,
    `a`.`contract_unique_id` as `contract_unique_id`,
    `a`.`con_code` as `con_code`,
    `a`.`con_name` as `con_name`,
    `a`.`sign_date` as `sign_date`,
    `b`.`min_price` as `min_price`,
    `b`.`max_price` as `max_price`,
    (sum((`b`.`min_price` + `b`.`max_price`)) / 2) as `avg_price`,
    `c`.`initial_price` as `initial_price`,
    `tmp`.`min_account_code` as `min_account_code`,
    `tmp`.`min_account_full_name` as `min_account_full_name`,
    `tmp`.`belonging_org_id` as `belonging_org_id`,
    `tmp`.`project_department_id` as `project_department_id`
from
    ((((
        select
            `clm`.`unique_id` as `unique_id`,
            `clm`.`con_id` as `con_id`,
            `clm`.`subject_dtl_code` as `subject_dtl_code`,
            `clm`.`subject_dtl_name` as `subject_dtl_name`,
            `clm`.`specs` as `specs`,
            `clm`.`measure_unit` as `measure_unit`,
            `clm`.`quantity` as `quantity`,
            `clm`.`ntax_price` as `ntax_price`,
            `clm`.`ntax_price` as `last_price`,
            `clm`.`create_time` as `create_time`,
            `cb`.`project_id` as `project_id`,
            `cb`.`unique_id` as `contract_unique_id`,
            `cb`.`con_code` as `con_code`,
            `cb`.`con_name` as `con_name`,
            `cb`.`sign_date` as `sign_date`
        from
            (`hnjiantou-zhaocai-prod`.`contract_list_materials` `clm`
                join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                (((`clm`.`con_id` = `cb`.`id`)
                    and (`cb`.`last_version_flag` = 1)
                    and (`cb`.`proc_status` = 4))))) `a`
        left join (
            select
                `clm`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `clm`.`subject_dtl_code` as `subject_dtl_code`,
                `clm`.`subject_dtl_name` as `subject_dtl_name`,
                min(`clm`.`ntax_price`) as `min_price`,
                max(`clm`.`ntax_price`) as `max_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_materials` `clm`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`clm`.`con_id` = `cb`.`id`)
                        and (`cb`.`proc_status` = 4))))
            group by
                `clm`.`subject_dtl_code`,
                `cb`.`unique_id`) `b` on
        (((`a`.`subject_dtl_code` = `b`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `b`.`contract_unique_id`))))
        left join (
            select
                `clm`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `clm`.`subject_dtl_code` as `subject_dtl_code`,
                `clm`.`subject_dtl_name` as `subject_dtl_name`,
                `clm`.`ntax_price` as `initial_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_materials` `clm`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`clm`.`con_id` = `cb`.`id`)
                        and (`cb`.`version` = 0))))) `c` on
        (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
        left join `hnjiantou-zhaocai-prod`.`tb_min_project` `tmp` on
        ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;

# 2024.10.17 上午10:30 价格分析-其他 视图新增
-- `hnjiantou-zhaocai-prod`.v_price_analysis_other source

create or replace
    algorithm = UNDEFINED view `hnjiantou-zhaocai-prod`.`v_price_analysis_other` as
select
    `a`.`unique_id` as `unique_id`,
    `a`.`con_id` as `con_id`,
    `a`.`subject_dtl_code` as `subject_dtl_code`,
    `a`.`subject_dtl_name` as `subject_dtl_name`,
    `a`.`specs` as `specs`,
    `a`.`measure_unit` as `measure_unit`,
    `a`.`quantity` as `quantity`,
    `a`.`ntax_price` as `ntax_price`,
    `a`.`last_price` as `last_price`,
    `a`.`create_time` as `create_time`,
    `a`.`project_id` as `project_id`,
    `a`.`contract_unique_id` as `contract_unique_id`,
    `a`.`con_code` as `con_code`,
    `a`.`con_name` as `con_name`,
    `a`.`sign_date` as `sign_date`,
    `b`.`min_price` as `min_price`,
    `b`.`max_price` as `max_price`,
    (sum((`b`.`min_price` + `b`.`max_price`)) / 2) as `avg_price`,
    `c`.`initial_price` as `initial_price`,
    `tmp`.`min_account_code` as `min_account_code`,
    `tmp`.`min_account_full_name` as `min_account_full_name`,
    `tmp`.`belonging_org_id` as `belonging_org_id`,
    `tmp`.`project_department_id` as `project_department_id`
from
    ((((
        select
            `clo`.`unique_id` as `unique_id`,
            `clo`.`con_id` as `con_id`,
            `clo`.`subject_dtl_code` as `subject_dtl_code`,
            `clo`.`subject_dtl_name` as `subject_dtl_name`,
            `clo`.`specs` as `specs`,
            `clo`.`measure_unit` as `measure_unit`,
            `clo`.`quantity` as `quantity`,
            `clo`.`ntax_price` as `ntax_price`,
            `clo`.`ntax_price` as `last_price`,
            `clo`.`create_time` as `create_time`,
            `cb`.`project_id` as `project_id`,
            `cb`.`unique_id` as `contract_unique_id`,
            `cb`.`con_code` as `con_code`,
            `cb`.`con_name` as `con_name`,
            `cb`.`sign_date` as `sign_date`
        from
            (`hnjiantou-zhaocai-prod`.`contract_list_other` `clo`
                join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                (((`clo`.`con_id` = `cb`.`id`)
                    and (`cb`.`last_version_flag` = 1)
                    and (`cb`.`proc_status` = 4))))) `a`
        left join (
            select
                `clo`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `clo`.`subject_dtl_code` as `subject_dtl_code`,
                `clo`.`subject_dtl_name` as `subject_dtl_name`,
                min(`clo`.`ntax_price`) as `min_price`,
                max(`clo`.`ntax_price`) as `max_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_other` `clo`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`clo`.`con_id` = `cb`.`id`)
                        and (`cb`.`proc_status` = 4))))
            group by
                `clo`.`subject_dtl_code`,
                `cb`.`unique_id`) `b` on
        (((`a`.`subject_dtl_code` = `b`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `b`.`contract_unique_id`))))
        left join (
            select
                `clo`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `clo`.`subject_dtl_code` as `subject_dtl_code`,
                `clo`.`subject_dtl_name` as `subject_dtl_name`,
                `clo`.`ntax_price` as `initial_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_other` `clo`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`clo`.`con_id` = `cb`.`id`)
                        and (`cb`.`version` = 0))))) `c` on
        (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
        left join `hnjiantou-zhaocai-prod`.`tb_min_project` `tmp` on
        ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;

# 2024.10.17 上午10:35 价格分析-专业 视图新增
-- `hnjiantou-zhaocai-prod`.v_price_analysis_specialty source

create or replace
    algorithm = UNDEFINED view `hnjiantou-zhaocai-prod`.`v_price_analysis_specialty` as
select
    `a`.`unique_id` as `unique_id`,
    `a`.`con_id` as `con_id`,
    `a`.`subject_dtl_code` as `subject_dtl_code`,
    `a`.`subject_dtl_name` as `subject_dtl_name`,
    `a`.`specs` as `specs`,
    `a`.`measure_unit` as `measure_unit`,
    `a`.`quantity` as `quantity`,
    `a`.`ntax_price` as `ntax_price`,
    `a`.`last_price` as `last_price`,
    `a`.`create_time` as `create_time`,
    `a`.`project_id` as `project_id`,
    `a`.`contract_unique_id` as `contract_unique_id`,
    `a`.`con_code` as `con_code`,
    `a`.`con_name` as `con_name`,
    `a`.`sign_date` as `sign_date`,
    `b`.`min_price` as `min_price`,
    `b`.`max_price` as `max_price`,
    (sum((`b`.`min_price` + `b`.`max_price`)) / 2) as `avg_price`,
    `c`.`initial_price` as `initial_price`,
    `tmp`.`min_account_code` as `min_account_code`,
    `tmp`.`min_account_full_name` as `min_account_full_name`,
    `tmp`.`belonging_org_id` as `belonging_org_id`,
    `tmp`.`project_department_id` as `project_department_id`
from
    ((((
        select
            `cls`.`unique_id` as `unique_id`,
            `cls`.`con_id` as `con_id`,
            `cls`.`subject_dtl_code` as `subject_dtl_code`,
            `cls`.`subject_dtl_name` as `subject_dtl_name`,
            `cls`.`specs` as `specs`,
            `cls`.`measure_unit` as `measure_unit`,
            `cls`.`metrological_rules` as `metrological_rules`,
            `cls`.`basic_job` as `basic_job`,
            `cls`.`quantity` as `quantity`,
            `cls`.`ntax_price` as `ntax_price`,
            `cls`.`ntax_price` as `last_price`,
            `cls`.`create_time` as `create_time`,
            `cb`.`project_id` as `project_id`,
            `cb`.`unique_id` as `contract_unique_id`,
            `cb`.`con_code` as `con_code`,
            `cb`.`con_name` as `con_name`,
            `cb`.`sign_date` as `sign_date`
        from
            (`hnjiantou-zhaocai-prod`.`contract_list_specialty` `cls`
                join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                (((`cls`.`con_id` = `cb`.`id`)
                    and (`cb`.`last_version_flag` = 1)
                    and (`cb`.`proc_status` = 4))))) `a`
        left join (
            select
                `cls`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `cls`.`subject_dtl_code` as `subject_dtl_code`,
                `cls`.`subject_dtl_name` as `subject_dtl_name`,
                min(`cls`.`ntax_price`) as `min_price`,
                max(`cls`.`ntax_price`) as `max_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_specialty` `cls`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`cls`.`con_id` = `cb`.`id`)
                        and (`cb`.`proc_status` = 4))))
            group by
                `cls`.`subject_dtl_code`,
                `cb`.`unique_id`) `b` on
        (((`a`.`subject_dtl_code` = `b`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `b`.`contract_unique_id`))))
        left join (
            select
                `cls`.`unique_id` as `unique_id`,
                `cb`.`unique_id` as `contract_unique_id`,
                `cls`.`subject_dtl_code` as `subject_dtl_code`,
                `cls`.`subject_dtl_name` as `subject_dtl_name`,
                `cls`.`ntax_price` as `initial_price`
            from
                (`hnjiantou-zhaocai-prod`.`contract_list_specialty` `cls`
                    join `hnjiantou-zhaocai-prod`.`contract_base` `cb` on
                    (((`cls`.`con_id` = `cb`.`id`)
                        and (`cb`.`version` = 0))))) `c` on
        (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
            and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
        left join `hnjiantou-zhaocai-prod`.`tb_min_project` `tmp` on
        ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;


# 2024.10.19 上午10:56 最小核算项目信息 表增加字段
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_min_project ADD project_department_id varchar(32) NULL COMMENT '归属项目部id';


# 2024.10.19 上午10:58 招标率统计 视图修改
-- `hnjiantou-zhaocai-prod`.v_bid_count source

create or replace
    algorithm = UNDEFINED view `hnjiantou-zhaocai-prod`.`v_bid_count` as
select
    `tmp`.`min_account_full_name` as `min_account_full_name`,
    `tmp`.`min_account_code` as `min_account_code`,
    `tmp`.`prj_state` as `prj_state`,
    `tmp`.`duty_unit` as `duty_unit`,
    `tmp`.`management_org_id` as `management_org_id`,
    `tmp`.`belonging_org_id` as `belonging_org_id`,
    `tmp`.`project_department_id` as `project_department_id`,
    `tps`.`cg_num` as `cg_num`,
    `tps`.`gk_num` as `gk_num`,
    `tps`.`yq_num` as `yq_num`,
    `tps`.`xj_num` as `xj_num`,
    `tps`.`dy_num` as `dy_num`,
    (`tps`.`gk_num` + `tps`.`yq_num`) as `gk_total_num`,
    (`tps`.`xj_num` + `tps`.`dy_num`) as `ngk_total_num`,
    ifnull(`contract`.`num`, 0) as `n_bid_total_num`,
    coalesce(round(((`tps`.`gk_num` / nullif(`tps`.`cg_num`, 0)) * 100), 2), '') as `gk_ratio`
from
    (((
        select
            `tps`.`project_code` as `project_code`,
            sum((case when (`tps`.`procurement_type` = '1') then 1 else 0 end)) as `gk_num`,
            sum((case when (`tps`.`procurement_type` = '2') then 1 else 0 end)) as `yq_num`,
            sum((case when (`tps`.`procurement_type` = '3') then 1 else 0 end)) as `xj_num`,
            sum((case when (`tps`.`procurement_type` = '4') then 1 else 0 end)) as `dy_num`,
            (((sum((case when (`tps`.`procurement_type` = '1') then 1 else 0 end)) + sum((case when (`tps`.`procurement_type` = '2') then 1 else 0 end))) + sum((case when (`tps`.`procurement_type` = '3') then 1 else 0 end))) + sum((case when (`tps`.`procurement_type` = '4') then 1 else 0 end))) as `cg_num`
        from
            (`hnjiantou-zhaocai-prod`.`tb_procurement_scheme` `tps`
                left join `hnjiantou-zhaocai-prod`.`tb_tender_notice` `tn` on
                (((`tn`.`scheme_id` = `tps`.`id`)
                    and (`tn`.`del_flag` = 0))))
        where
            ((`tps`.`del_flag` = 0)
                and (`tps`.`state` = 3)
                and (`tn`.`notice_status` = 8))
        group by
            `tps`.`project_code`) `tps`
        left join `hnjiantou-zhaocai-prod`.`tb_min_project` `tmp` on
        ((`tmp`.`min_account_code` = `tps`.`project_code`)))
        left join (
            select
                `hnjiantou-zhaocai-prod`.`contract_base`.`project_id` as `project_id`,
                count(1) as `num`
            from
                `hnjiantou-zhaocai-prod`.`contract_base`
            where
                ((`hnjiantou-zhaocai-prod`.`contract_base`.`introduce_flag` = 0)
                    and (`hnjiantou-zhaocai-prod`.`contract_base`.`proc_status` = 4)
                    and (`hnjiantou-zhaocai-prod`.`contract_base`.`last_version_flag` = 1))
            group by
                `hnjiantou-zhaocai-prod`.`contract_base`.`project_id`) `contract` on
        ((`tmp`.`id` = `contract`.`project_id`)));

# 2024.10.21 下午6:28 采购物料清单 表增加字段

ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD code varchar(64) NULL COMMENT '商品编号';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD name varchar(64) NULL COMMENT '商品名称';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD category varchar(64) NULL COMMENT '商品规格';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD unit_name varchar(64) NULL COMMENT '商品单位';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD quantity decimal(20,5) NULL COMMENT '商品数量';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD push_flag varchar(2) DEFAULT 'N' NULL COMMENT '是否推送 Y:推送 N:未推送';



ALTER TABLE `hnjiantou-zhaocai-prod`.tb_bidding_info ADD expert_state int NULL COMMENT '专家评分状态 0未评分 1已评分';

# 2024.10.22 上午11:50 易料采购合同 新增表
-- `hnjiantou-zhaocai-prod`.tb_market_material_contract definition

CREATE TABLE `tb_market_material_contract` (
                                               `id` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '合同主键id',
                                               `plan_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计划id',
                                               `require_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '清单id',
                                               `belong_accounting_item` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目',
                                               `belong_accounting_item_code` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目编码',
                                               `agreement_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同编码',
                                               `agreement_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同名称',
                                               `vendor_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方（供应商）Id',
                                               `party_b_name` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方名称',
                                               `expenditure_business_type` varchar(8) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出业务分类',
                                               `party_b_legal_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方法人代表',
                                               `party_b_legal_id_card` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方法人身份证',
                                               `party_b_legal_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方法人联系方式',
                                               `party_b_responsible_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方现场实际履职负责人',
                                               `party_b_responsible_id_card` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方现场实际履职负责人身份证',
                                               `party_b_responsible_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方现场实际履职负责人联系方式',
                                               `agreement_perform_country` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家地区代码(履行地)',
                                               `agreement_perform_district` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划代码(履行地)',
                                               `agreement_perform_address` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同履行地',
                                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='易料采购合同';

# 2024.10.22 上午11:50 易料采购合同清单 新增表
-- `hnjiantou-zhaocai-prod`.tb_market_material_list definition

CREATE TABLE `tb_market_material_list` (
                                           `require_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '清单id',
                                           `contract_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同id',
                                           `quote_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '清单名称',
                                           `quote_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '清单编码',
                                           `status` int DEFAULT NULL COMMENT '报价状态 0:未报价,1:已报价,2:不报价',
                                           `category` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '规格型号',
                                           `unit_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '单位',
                                           `quantity` decimal(20,5) DEFAULT NULL COMMENT '数量',
                                           `price` decimal(20,5) DEFAULT NULL COMMENT '含税单价',
                                           `no_tax_price` decimal(20,5) DEFAULT NULL COMMENT '不含税单价',
                                           `tax_rate` decimal(20,5) DEFAULT NULL COMMENT '税率',
                                           `offer_price` decimal(20,5) DEFAULT NULL COMMENT '易料市集含税单价',
                                           `offer_brand` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集品牌',
                                           `offer_goods_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集商品编码',
                                           `created_time` datetime DEFAULT NULL COMMENT '创建日期',
                                           `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
                                           `offer_supplier_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集供应商名称',
                                           `offer_supplier_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集供应商编码',
                                           `goods_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '易料市集商品名',
                                           PRIMARY KEY (`require_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='易料采购合同清单';

# 2024.10.22 下午6:41 合同基本信息 表新增字段

ALTER TABLE `hnjiantou-zhaocai-prod`.tb_agreement ADD market_material_contract_id varchar(32) NULL COMMENT '易料采购合同id';


ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD price_type int NULL COMMENT '价格类型';

# 增加批语
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_vendor ADD operate_comment varchar(255) NULL COMMENT '审批批语';


ALTER TABLE `hnjiantou-zhaocai-prod`.tb_expert ADD state int NULL COMMENT '审批状态';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_expert ADD wf_process_id varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '流程实例 id';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_expert ADD process_type int NULL COMMENT '流程类型';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_expert ADD operate_comment varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '审批批语';

update `hnjiantou-zhaocai-prod`.tb_expert set state = 3 where state is null
# 2024.10.28 上午11:18 采购物料清单 表新增字段

ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD offer_price decimal(20,5) NULL COMMENT '易料市集含税单价';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD offer_brand varchar(100) NULL COMMENT '易料市集品牌';
# 2024.10.24 下午6:41 合同物料清单 表新增字段

ALTER TABLE `hnjiantou-zhaocai-prod`.tb_agreement_materials_list ADD offer_goods_code varchar(64) NULL COMMENT '易料市集商品编码';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_agreement_materials_list ADD goods_name varchar(128) NULL COMMENT '易料市集商品名';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_agreement_materials_list ADD offer_brand varchar(50) NULL COMMENT '易料市集品牌';
ALTER TABLE `hnjiantou-zhaocai-prod`.tb_agreement_materials_list ADD offer_price decimal(20,5) NULL COMMENT '易料市集含税单价';# 2024.10.29 下午15:00 采购物料清单,易料采购合同清单,合同物料清单 表新增字段

ALTER TABLE `hnjiantou-zhaocai-prod`.tb_materials_list ADD sku_id varchar(64) NULL COMMENT '易料市集商品id';

ALTER TABLE `hnjiantou-zhaocai-prod`.tb_market_material_list ADD sku_id varchar(64) NULL COMMENT '易料市集商品id';

ALTER TABLE `hnjiantou-zhaocai-prod`.tb_agreement_materials_list ADD sku_id varchar(64) NULL COMMENT '易料市集商品id';
# 2024.10.29 下午17:17 合同基本信息 表新增字段

ALTER TABLE `hnjiantou-zhaocai-prod`.tb_agreement ADD procurement_scheme_code varchar(32) NULL COMMENT '易料采购合同招标编码';

ALTER TABLE `hnjiantou-zhaocai-prod`.tb_vendor  ADD operate_comment varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '审批批语';
