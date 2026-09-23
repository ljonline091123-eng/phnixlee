package com.zhaocai.business.report.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购台账-明细宽表行（列序对齐《采购台账字段字典.xlsx》32 字段，2026-09-17）
 *
 * @Excel 字段声明顺序 = 导出列顺序（RuoYi 同 sort 值下按声明序输出）
 *
 * @author claude
 */
@Data
public class PurchaseLedgerListVo extends AdviceObject {

    @ApiModelProperty(value = "粒度 SPLIT/TASK")
    private String grain;

    @ApiModelProperty(value = "源记录id")
    private Long recordId;

    @ApiModelProperty(value = "采购计划id(穿透用)")
    private Long planId;

    @ApiModelProperty(value = "招标公告id(穿透用)")
    private Long noticeId;

    @ApiModelProperty(value = "招标公告状态(穿透用)")
    private Integer noticeStatus;

    @ApiModelProperty(value = "组织机构id")
    private String orgId;

    @ApiModelProperty(value = "采购状态 code")
    private String status;

    @ApiModelProperty(value = "采购方式")
    private Integer method;

    @ApiModelProperty(value = "合同id(多合同拼接, 穿透用)")
    private String agreementId;

    @ApiModelProperty(value = "合同业务类型(多合同拼接, 穿透用)")
    private String expenditureBusinessType;

    @ApiModelProperty(value = "合同状态(原值, 不导出)")
    private String contractState;

    // ===== 以下 @Excel 字段按字段字典列序声明（导出顺序）=====

    @Excel(name = "组织机构")
    @ApiModelProperty(value = "组织机构")
    private String orgName;

    @Excel(name = "项目编号")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @Excel(name = "项目名称")
    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @Excel(name = "计划编号")
    @ApiModelProperty(value = "计划编号")
    private String planCode;

    @Excel(name = "采购名称")
    @ApiModelProperty(value = "采购名称(即采购计划名称, 直接映射)")
    private String planName;

    @Excel(name = "合约类别")
    @ApiModelProperty(value = "合约类别(合约规划分类名称)")
    private String contractCategory;

    @Excel(name = "合约名称")
    @ApiModelProperty(value = "合约名称(新增采购计划时填写的合约名称)")
    private String contractName;

    @Excel(name = "计划金额（含税）")
    @ApiModelProperty(value = "计划金额(含税)(计划表单\"计划金额\")")
    private BigDecimal planAmount;

    @Excel(name = "采购人")
    @ApiModelProperty(value = "采购人")
    private String purchaser;

    @Excel(name = "计划完成时间", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划完成时间(计划表单\"完成时间\")")
    private Date planFinishTime;

    @Excel(name = "任务编号（采购方案）")
    @ApiModelProperty(value = "任务编号(采购方案)")
    private String schemeCode;

    @Excel(name = "任务名称（采购方案）")
    @ApiModelProperty(value = "任务名称(采购方案)")
    private String schemeName;

    @Excel(name = "采购类别")
    @ApiModelProperty(value = "采购类别(原采购需求类型)")
    private String demandTypeText;

    @ApiModelProperty(value = "采购需求类型")
    private Integer demandType;

    @Excel(name = "采购方式")
    @ApiModelProperty(value = "采购方式")
    private String methodText;

    @Excel(name = "控制金额/上限价（含税）")
    @ApiModelProperty(value = "控制金额/上限价(含税)")
    private BigDecimal controlAmount;

    @Excel(name = "采购经办人")
    @ApiModelProperty(value = "采购经办人")
    private String handler;

    @Excel(name = "当前环节")
    @ApiModelProperty(value = "当前环节")
    private String currentStage;

    @Excel(name = "采购状态")
    @ApiModelProperty(value = "采购状态")
    private String statusText;

    @Excel(name = "开标时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "开标时间")
    private Date openTime;

    @Excel(name = "定标时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "定标时间(中标公示开始时间)")
    private Date awardTime;

    @Excel(name = "结果发布时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "结果发布时间(中标通知发布时间)")
    private Date resultPublishTime;

    @Excel(name = "中标/成交单位")
    @ApiModelProperty(value = "中标/成交单位")
    private String supplierName;

    @Excel(name = "中标/成交金额（含税）")
    @ApiModelProperty(value = "中标/成交金额(含税)")
    private BigDecimal awardAmount;

    @Excel(name = "节约金额（含税）")
    @ApiModelProperty(value = "节约金额(含税)")
    private BigDecimal savingAmount;

    @Excel(name = "节约率")
    @ApiModelProperty(value = "节约率")
    private BigDecimal savingRate;

    @Excel(name = "合同编号")
    @ApiModelProperty(value = "合同编号")
    private String contractCode;

    @Excel(name = "合同名称")
    @ApiModelProperty(value = "合同名称(多合同拼接)")
    private String agreementName;

    @Excel(name = "合同金额（含税）")
    @ApiModelProperty(value = "合同金额(含税, 多合同拼接)")
    private String agreementAmount;

    @Excel(name = "合同状态")
    @ApiModelProperty(value = "合同状态")
    private String contractStateText;

    @Excel(name = "合同签订日期")
    @ApiModelProperty(value = "合同签订日期(多合同拼接)")
    private String agreementSignDate;

    @Excel(name = "异常情况")
    @ApiModelProperty(value = "异常情况")
    private String exceptionInfo;

    @ApiModelProperty(value = "计划生效时间(字典外字段, 页面/导出不展示, 保留兼容)")
    private Date planEffectTime;

    @ApiModelProperty(value = "计划拆分项/合约拆分名称(字典外字段, 页面/导出不展示, 保留兼容)")
    private String splitNames;

}
