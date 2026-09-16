package com.zhaocai.business.report.vo.res;

import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购台账-汇总视图行（组织机构 × 项目名称 × 采购需求类型 聚合五类状态）
 *
 * @author claude
 */
@Data
public class PurchaseLedgerSummaryVo {

    @ApiModelProperty(value = "组织机构id")
    private String orgId;

    @Excel(name = "组织机构")
    @ApiModelProperty(value = "组织机构名称")
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
    @ApiModelProperty(value = "采购需求类型名称")
    private String demandTypeText;

    @Excel(name = "待招采")
    @ApiModelProperty(value = "待招采")
    private Long pending = 0L;

    @Excel(name = "待开标")
    @ApiModelProperty(value = "待开标")
    private Long preopen = 0L;

    @Excel(name = "待定标")
    @ApiModelProperty(value = "待定标")
    private Long preaward = 0L;

    @Excel(name = "已完成")
    @ApiModelProperty(value = "已完成")
    private Long completed = 0L;

    @Excel(name = "异常/终止")
    @ApiModelProperty(value = "异常/终止")
    private Long exception = 0L;

}
