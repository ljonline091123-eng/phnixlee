package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购方案-合约规划
 *
 * @author chenming
 * @date 2024-06-17
 */
@Data
public class ProcurementContractPlanListVO extends AdviceObject {

    @ApiModelProperty(value = "采购计划 id")
    private Long planId;

    @ApiModelProperty(value =  "采购计划编号")
    private String procurementPlanCode;

    @ApiModelProperty(value =  "采购计划名称")
    private String procurementPlanName;

    @ApiModelProperty(value = "合约规划编码")
    private String contractPlanningCode;

    @ApiModelProperty(value = "合约规划名称")
    private String contractPlanningName;

    @ApiModelProperty(value = "合约规划类别")
    private String contractPlanningCategory;

    @ApiModelProperty(value = "规划金额")
    private BigDecimal plannedAmountInclTax;

    @ApiModelProperty(value = "已发生规划金额")
    private BigDecimal incurredPlannedAmount;

    @ApiModelProperty(value = "规划余量")
    private BigDecimal planningBalance;

    @ApiModelProperty(value = "拟定招标方式")
    private String biddingMethod;

    @MoneyFormat(filedName = "plannedAmountInclTax",scale = 2)
    @ApiModelProperty(value = "规划金额")
    private String plannedAmountInclTaxText;

    @MoneyFormat(filedName = "incurredPlannedAmount",scale = 2)
    @ApiModelProperty(value = "已发生规划金额")
    private String incurredPlannedAmountText;

    @MoneyFormat(filedName = "planningBalance",scale = 2)
    @ApiModelProperty(value = "规划余量")
    private String planningBalanceText;

    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @ApiModelProperty(value = "项目名称")
    private String projectName;
}
