package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/5/29 17:35
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BidQuotationVO", description = "投标清单信息VO")
public class BidQuotationVO implements Serializable {
    private static final long serialVersionUID = -3008397029144338167L;

    @ApiModelProperty(value =  "清单编码")
    private String materialsCode;

    @ApiModelProperty(value =  "物料数量")
    private BigDecimal amount;

    @ApiModelProperty(value =  "不含税单价(元)")
    private BigDecimal notTaxUnitPrice;

    @ApiModelProperty(value =  "含税单价(元)")
    private BigDecimal taxUnitPrice;

    @ApiModelProperty(value =  "含税总价(元)")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价(元)")
    private BigDecimal notTaxPrice;

    @ApiModelProperty(value =  "税率")
    private BigDecimal taxRate;

    @ApiModelProperty(value = "税率编码")
    private String taxRateCode;

    @ApiModelProperty(value = "税率名称")
    private String taxRateName;

    @ApiModelProperty(value =  "发票类型（1增值税专用发票/2增值税普通发票/3数电票） ")
    private Integer billType;

    @ApiModelProperty(value =  "清单名称")
    private String materialsName;

    /** 08-15（增加固定价/浮动价概念） */

    @ApiModelProperty(value = "浮动价")
    private BigDecimal floatingPrice;

    @ApiModelProperty(value = "卸费")
    private BigDecimal unloadingFee;

    @ApiModelProperty(value = "基价")
    private BigDecimal basePrice;


    @ApiModelProperty(value = "拆分id")
    private Long splitId;

    @ApiModelProperty(value = "物料id（内部主键id）")
    private Long materialsId;

}
