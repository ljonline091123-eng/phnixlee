package com.zhaocai.business.report.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购台账-明细宽表行（文档 5.3.1 表13 字段）
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

    @Excel(name = "组织机构")
    @ApiModelProperty(value = "组织机构")
    private String orgName;

    @Excel(name = "项目编号")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @Excel(name = "项目名称")
    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value = "采购需求类型")
    private Integer demandType;

    @Excel(name = "采购需求类型")
    @ApiModelProperty(value = "采购需求类型")
    private String demandTypeText;

    @Excel(name = "计划编号")
    @ApiModelProperty(value = "计划编号")
    private String planCode;

    @Excel(name = "计划名称")
    @ApiModelProperty(value = "计划名称")
    private String planName;

    @Excel(name = "计划拆分项/合约拆分名称")
    @ApiModelProperty(value = "计划拆分项/合约拆分名称")
    private String splitNames;

    @Excel(name = "任务编号")
    @ApiModelProperty(value = "任务编号(采购方案)")
    private String schemeCode;

    @Excel(name = "任务名称")
    @ApiModelProperty(value = "任务名称(采购方案)")
    private String schemeName;

    @ApiModelProperty(value = "采购方式")
    private Integer method;

    @Excel(name = "采购方式")
    @ApiModelProperty(value = "采购方式")
    private String methodText;

    @Excel(name = "采购控制金额(含税)")
    @ApiModelProperty(value = "采购控制金额/上限价(含税)")
    private BigDecimal controlAmount;

    @Excel(name = "当前环节")
    @ApiModelProperty(value = "当前环节")
    private String currentStage;

    @Excel(name = "采购状态")
    @ApiModelProperty(value = "采购状态")
    private String statusText;

    @ApiModelProperty(value = "采购状态 code")
    private String status;

    @Excel(name = "采购人")
    @ApiModelProperty(value = "采购人")
    private String purchaser;

    @Excel(name = "采购经办人")
    @ApiModelProperty(value = "采购经办人")
    private String handler;

    @Excel(name = "计划生效时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "计划生效时间")
    private Date planEffectTime;

    @Excel(name = "开标时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "开标时间")
    private Date openTime;

    @Excel(name = "定标时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "定标时间")
    private Date awardTime;

    @Excel(name = "中标/成交单位")
    @ApiModelProperty(value = "中标/成交单位")
    private String supplierName;

    @Excel(name = "中标/成交金额(含税)")
    @ApiModelProperty(value = "中标/成交金额(含税)")
    private BigDecimal awardAmount;

    @Excel(name = "节约金额(含税)")
    @ApiModelProperty(value = "节约金额(含税)")
    private BigDecimal savingAmount;

    @Excel(name = "节约率")
    @ApiModelProperty(value = "节约率")
    private BigDecimal savingRate;

    @Excel(name = "合同编号")
    @ApiModelProperty(value = "合同编号")
    private String contractCode;

    @ApiModelProperty(value = "合同状态(原值, 不导出)")
    private String contractState;

    @Excel(name = "合同状态")
    @ApiModelProperty(value = "合同状态")
    private String contractStateText;

    @Excel(name = "异常情况")
    @ApiModelProperty(value = "异常情况")
    private String exceptionInfo;

}
