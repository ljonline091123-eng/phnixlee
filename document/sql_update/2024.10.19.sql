
# 2024.10.19 上午10:56 最小核算项目信息 表增加字段
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_min_project ADD project_department_id varchar(32) NULL COMMENT '归属项目部id';


# 2024.10.19 上午10:58 招标率统计 视图修改
-- `hnjiantou-zhaocai-dev`.v_bid_count source

create or replace
algorithm = UNDEFINED view `hnjiantou-zhaocai-dev`.`v_bid_count` as
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
        (`hnjiantou-zhaocai-dev`.`tb_procurement_scheme` `tps`
    left join `hnjiantou-zhaocai-dev`.`tb_tender_notice` `tn` on
        (((`tn`.`scheme_id` = `tps`.`id`)
            and (`tn`.`del_flag` = 0))))
    where
        ((`tps`.`del_flag` = 0)
            and (`tps`.`state` = 3)
                and (`tn`.`notice_status` = 8))
    group by
        `tps`.`project_code`) `tps`
left join `hnjiantou-zhaocai-dev`.`tb_min_project` `tmp` on
    ((`tmp`.`min_account_code` = `tps`.`project_code`)))
left join (
    select
        `hnjiantou-zhaocai-dev`.`contract_base`.`project_id` as `project_id`,
        count(1) as `num`
    from
        `hnjiantou-zhaocai-dev`.`contract_base`
    where
        ((`hnjiantou-zhaocai-dev`.`contract_base`.`introduce_flag` = 0)
            and (`hnjiantou-zhaocai-dev`.`contract_base`.`proc_status` = 4)
                and (`hnjiantou-zhaocai-dev`.`contract_base`.`last_version_flag` = 1))
    group by
        `hnjiantou-zhaocai-dev`.`contract_base`.`project_id`) `contract` on
    ((`tmp`.`id` = `contract`.`project_id`)));
