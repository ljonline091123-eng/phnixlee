package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同规划
 *
 * @author chenming
 * @date 2024-09-07
 */
@Data
public class ContractPlanningVO extends AdviceObject {

    @ApiModelProperty(value = "合约规划列表")
    private List<ContractPlanningListVO> contractPlanningList;

    @ApiModelProperty(value = "总数量")
    private Integer totalSize;

    @ApiModelProperty(value = "策划金额总金额")
    private BigDecimal totalPlannedAmountInclTax;

    @ApiModelProperty(value = "引用金额总金额")
    private BigDecimal totalIncurredPlannedAmount;

    @ApiModelProperty(value = "余额总金额")
    private BigDecimal totalPlanningBalance;

    @MoneyFormat(filedName = "totalPlannedAmountInclTax",scale = 2)
    @ApiModelProperty(value = "策划金额总金额-文本")
    private String totalPlannedAmountInclTaxText;

    @MoneyFormat(filedName = "totalIncurredPlannedAmount",scale = 2)
    @ApiModelProperty(value = "引用金额总金额-文本")
    private String totalIncurredPlannedAmountText;

    @MoneyFormat(filedName = "totalPlanningBalance",scale = 2)
    @ApiModelProperty(value = "余额总金额-文本")
    private String totalPlanningBalanceText;
}
