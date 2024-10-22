# 2024.10.17 上午10:00 价格分析-劳务 视图新增
-- `hnjiantou-zhaocai-dev`.v_price_analysis_labor source

create or replace
algorithm = UNDEFINED view `hnjiantou-zhaocai-dev`.`v_price_analysis_labor` as
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
        (`hnjiantou-zhaocai-dev`.`contract_list_labor` `cll`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_labor` `cll`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_labor` `cll`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
        (((`cll`.`con_id` = `cb`.`id`)
            and (`cb`.`version` = 0))))) `c` on
    (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
        and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
left join `hnjiantou-zhaocai-dev`.`tb_min_project` `tmp` on
    ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;


# 2024.10.17 上午10:15 价格分析-租赁设备 视图新增
-- `hnjiantou-zhaocai-dev`.v_price_analysis_leased_device source

create or replace
algorithm = UNDEFINED view `hnjiantou-zhaocai-dev`.`v_price_analysis_leased_device` as
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
        (`hnjiantou-zhaocai-dev`.`contract_list_leased_device` `clld`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_leased_device` `clld`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_leased_device` `clld`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
        (((`clld`.`con_id` = `cb`.`id`)
            and (`cb`.`version` = 0))))) `c` on
    (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
        and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
left join `hnjiantou-zhaocai-dev`.`tb_min_project` `tmp` on
    ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;


# 2024.10.17 上午10:20 价格分析-租赁材料 视图新增
-- `hnjiantou-zhaocai-dev`.v_price_analysis_leased_materials source

create or replace
algorithm = UNDEFINED view `hnjiantou-zhaocai-dev`.`v_price_analysis_leased_materials` as
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
        (`hnjiantou-zhaocai-dev`.`contract_list_leased_materials` `cllm`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_leased_materials` `cllm`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_leased_materials` `cllm`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
        (((`cllm`.`con_id` = `cb`.`id`)
            and (`cb`.`version` = 0))))) `c` on
    (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
        and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
left join `hnjiantou-zhaocai-dev`.`tb_min_project` `tmp` on
    ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;

# 2024.10.17 上午10:25 价格分析-购买材料 视图新增
-- `hnjiantou-zhaocai-dev`.v_price_analysis_materials source

create or replace
algorithm = UNDEFINED view `hnjiantou-zhaocai-dev`.`v_price_analysis_materials` as
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
        (`hnjiantou-zhaocai-dev`.`contract_list_materials` `clm`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_materials` `clm`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_materials` `clm`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
        (((`clm`.`con_id` = `cb`.`id`)
            and (`cb`.`version` = 0))))) `c` on
    (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
        and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
left join `hnjiantou-zhaocai-dev`.`tb_min_project` `tmp` on
    ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;

# 2024.10.17 上午10:30 价格分析-其他 视图新增
-- `hnjiantou-zhaocai-dev`.v_price_analysis_other source

create or replace
algorithm = UNDEFINED view `hnjiantou-zhaocai-dev`.`v_price_analysis_other` as
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
        (`hnjiantou-zhaocai-dev`.`contract_list_other` `clo`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_other` `clo`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_other` `clo`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
        (((`clo`.`con_id` = `cb`.`id`)
            and (`cb`.`version` = 0))))) `c` on
    (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
        and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
left join `hnjiantou-zhaocai-dev`.`tb_min_project` `tmp` on
    ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;

# 2024.10.17 上午10:35 价格分析-专业 视图新增
-- `hnjiantou-zhaocai-dev`.v_price_analysis_specialty source

create or replace
algorithm = UNDEFINED view `hnjiantou-zhaocai-dev`.`v_price_analysis_specialty` as
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
        (`hnjiantou-zhaocai-dev`.`contract_list_specialty` `cls`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_specialty` `cls`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
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
        (`hnjiantou-zhaocai-dev`.`contract_list_specialty` `cls`
    join `hnjiantou-zhaocai-dev`.`contract_base` `cb` on
        (((`cls`.`con_id` = `cb`.`id`)
            and (`cb`.`version` = 0))))) `c` on
    (((`a`.`subject_dtl_code` = `c`.`subject_dtl_code`)
        and (`a`.`contract_unique_id` = `c`.`contract_unique_id`))))
left join `hnjiantou-zhaocai-dev`.`tb_min_project` `tmp` on
    ((`a`.`project_id` = `tmp`.`id`)))
group by
    `a`.`subject_dtl_code`,
    `a`.`contract_unique_id`;