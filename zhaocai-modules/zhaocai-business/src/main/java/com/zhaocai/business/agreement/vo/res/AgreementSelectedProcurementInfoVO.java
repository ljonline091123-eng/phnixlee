package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeBiddingVendorVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同选定采购相关信息
 *
 * @author chenming
 * @date 2024/06/04
 */
@Data
@NoArgsConstructor
public class AgreementSelectedProcurementInfoVO extends AdviceObject {

    @ApiModelProperty(value = "上限价")
    private BigDecimal upperLimitPrice;

    @ApiModelProperty(value = "采购上限价")
    private BigDecimal procurementUpperLimitPrice;

    @ApiModelProperty(value = "已发生总价")
    private BigDecimal usedTotalAmount;

    @ApiModelProperty(value = "剩余可用总价")
    private BigDecimal surplusTotalAmount;

    @ApiModelProperty(value = "供应商信息")
    List<ProcurementSchemeBiddingVendorVO> biddingVendorList;

    @MoneyFormat(filedName = "upperLimitPrice",scale = 2)
    @ApiModelProperty(value = "上限价-文本")
    private String upperLimitPriceText;

    @MoneyFormat(filedName = "procurementUpperLimitPrice",scale = 2)
    @ApiModelProperty(value = "采购上限价-文本")
    private String procurementUpperLimitPriceText;

    @MoneyFormat(filedName = "usedTotalAmount",scale = 2)
    @ApiModelProperty(value = "已发生总价-文本")
    private String usedTotalAmountText;

    @MoneyFormat(filedName = "surplusTotalAmount",scale = 2)
    @ApiModelProperty(value = "剩余可用总价-文本")
    private String surplusTotalAmountText;
}
