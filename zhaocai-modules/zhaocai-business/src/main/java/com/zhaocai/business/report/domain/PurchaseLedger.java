package com.zhaocai.business.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购台账报表对象 tb_purchase_ledger
 *
 * 口径（2026-09-15 确认，勿改动）：
 * 待招采   = 采购计划 state=3 的合约拆分项，未被"state≠2 且已发布非废标公告"的方案占用
 * 待开标   = 公告 notice_status ∈ {11,12,13,1}
 * 待定标   = 公告 notice_status ∈ {2,3,5,6}
 * 已完成   = 公告 notice_status ∈ {7,8}
 * 异常/终止= 公告 notice_status=0 且公告 state=3（真正发布过又被废标）
 *
 * @author claude
 */
@Data
@TableName(value = "tb_purchase_ledger")
public class PurchaseLedger {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 粒度：SPLIT=待招采拆分项 / TASK=采购任务 */
    @ApiModelProperty(value = "粒度 SPLIT/TASK")
    private String grain;

    /** 源记录id：SPLIT=拆分项id / TASK=方案id */
    @ApiModelProperty(value = "源记录id")
    private Long recordId;

    /** 采购计划id（穿透计划详情用；SPLIT=拆分项所属计划，TASK=关联的第一个计划） */
    @ApiModelProperty(value = "采购计划id")
    private Long planId;

    /** 招标公告id（TASK 行穿透招标详情用） */
    @ApiModelProperty(value = "招标公告id")
    private Long noticeId;

    /** 招标公告状态（TASK 行穿透招标详情用） */
    @ApiModelProperty(value = "招标公告状态")
    private Integer noticeStatus;

    /** 组织id(tb_min_project.project_department_id, 对 sys_dept.thrid_dept_id) */
    @ApiModelProperty(value = "组织id")
    private String orgId;

    /** 组织名称 */
    @ApiModelProperty(value = "组织名称")
    private String orgName;

    /** 项目编号(min_account_code) */
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    /** 项目名称 */
    @ApiModelProperty(value = "项目名称")
    private String projectName;

    /** 采购需求类型(字典 procurement_plan_type 1-6) */
    @ApiModelProperty(value = "采购需求类型")
    private Integer demandType;

    /** 计划编号(多计划拼接) */
    @ApiModelProperty(value = "计划编号")
    private String planCode;

    /** 计划名称 */
    @ApiModelProperty(value = "计划名称")
    private String planName;

    /** 计划拆分项/合约拆分名称(多值拼接) */
    @ApiModelProperty(value = "计划拆分项名称")
    private String splitNames;

    /** 合约名称(新增采购计划时填写的合约名称, tb_contract_planning.contract_planning_name) */
    @ApiModelProperty(value = "合约名称")
    private String contractName;

    /** 合约类别(合约规划分类名称, tb_contract_planning.contract_planning_category_name) */
    @ApiModelProperty(value = "合约类别")
    private String contractCategory;

    /** 计划金额(含税)(计划表单"计划金额", tb_contract_planning.planned_amount_incl_tax) */
    @ApiModelProperty(value = "计划金额(含税)")
    private BigDecimal planAmount;

    /** 计划完成时间(计划表单"完成时间", tb_procurement_plan.end_date) */
    @ApiModelProperty(value = "计划完成时间")
    private Date planFinishTime;

    /** 任务编号(采购方案编号) */
    @ApiModelProperty(value = "任务编号")
    private String schemeCode;

    /** 任务名称(采购方案名称) */
    @ApiModelProperty(value = "任务名称")
    private String schemeName;

    /** 采购方式(字典 procurement_type 1-6) */
    @ApiModelProperty(value = "采购方式")
    private Integer method;

    /** 采购控制金额/上限价(含税) */
    @ApiModelProperty(value = "采购控制金额")
    private BigDecimal controlAmount;

    /** 采购人(采购计划确定人员, 多计划拼接) */
    @ApiModelProperty(value = "采购人")
    private String purchaser;

    /** 采购经办人(采购方案负责人) */
    @ApiModelProperty(value = "采购经办人")
    private String handler;

    /** 计划生效时间 */
    @ApiModelProperty(value = "计划生效时间")
    private Date planEffectTime;

    /** 开标时间 */
    @ApiModelProperty(value = "开标时间")
    private Date openTime;

    /** 定标时间(中标公示开始时间) */
    @ApiModelProperty(value = "定标时间")
    private Date awardTime;

    /** 结果发布时间(中标通知发布时间, tb_bidding_result.notifi_time, "已完成"时间锚点) */
    @ApiModelProperty(value = "结果发布时间")
    private Date resultPublishTime;

    /** 中标/成交单位 */
    @ApiModelProperty(value = "中标/成交单位")
    private String supplierName;

    /** 中标/成交金额(含税) */
    @ApiModelProperty(value = "中标/成交金额")
    private BigDecimal awardAmount;

    /** 节约金额 */
    @ApiModelProperty(value = "节约金额")
    private BigDecimal savingAmount;

    /** 节约率% */
    @ApiModelProperty(value = "节约率")
    private BigDecimal savingRate;

    /** 合同编号(多合同拼接) */
    @ApiModelProperty(value = "合同编号")
    private String contractCode;

    /** 合同状态(原值拼接) */
    @ApiModelProperty(value = "合同状态")
    private String contractState;

    /** 合同id(多合同按id拼接, 穿透合同详情用, 不显示) */
    @ApiModelProperty(value = "合同id")
    private String agreementId;

    /** 合同名称(多合同按id拼接) */
    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    /** 合同金额(含税, 多合同按id拼接, 字符串因拼接) */
    @ApiModelProperty(value = "合同金额(含税)")
    private String agreementAmount;

    /** 合同签订日期(yyyy-MM-dd, 多合同按id拼接) */
    @ApiModelProperty(value = "合同签订日期")
    private String agreementSignDate;

    /** 合同业务类型(多合同按id拼接, 穿透合同详情用, 不显示) */
    @ApiModelProperty(value = "合同业务类型")
    private String expenditureBusinessType;

    /** pending/preopen/preaward/completed/exception */
    @ApiModelProperty(value = "采购状态")
    private String status;

    /** 当前环节 */
    @ApiModelProperty(value = "当前环节")
    private String currentStage;

    /** 异常说明 */
    @ApiModelProperty(value = "异常说明")
    private String exceptionInfo;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}
