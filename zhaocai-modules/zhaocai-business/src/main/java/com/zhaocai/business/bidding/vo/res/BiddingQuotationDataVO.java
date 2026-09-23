package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/6/26 15:34
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingQuotationDataVO", description = "投标单信息报价数据VO")
public class BiddingQuotationDataVO extends AdviceObject implements Serializable {
    private static final long serialVersionUID = -120677699791785905L;

    @ApiModelProperty(value =  "投标单id")
    private Long id;

    @ApiModelProperty(value =  "含税总价")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价")
    private BigDecimal notTaxPrice;

    @MoneyFormat(filedName = "taxPrice")
    @ApiModelProperty(value =  "含税总价(元)（千分位）")
    private String taxPricePattern;

    @MoneyFormat(filedName = "notTaxPrice")
    @ApiModelProperty(value =  "不含税总价(元)（千分位）")
    private String notTaxPricePattern;

    @ApiModelProperty(value =  "二次报价版本号。从1开始")
    private Integer twiceQuotVersion;

    @ApiModelProperty(value =  "供应商调价状态")
    private Integer priceChangeState;

}
