package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 投标清单报价对象 tb_bidding_list_quotation
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_bidding_list_quotation")
public class BiddingListQuotation extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 清单编码 */
    @ApiModelProperty(value =  "清单编码")
    private String materialsCode;

    /** 投标单信息id */
    @ApiModelProperty(value =  "投标单信息id")
    private Long biddingInfoId;

    /** 含税单价(元) */
    @ApiModelProperty(value =  "含税单价(元)")
    private BigDecimal taxUnitPrice;

    /** 不含税单价(元) */
    @ApiModelProperty(value =  "不含税单价(元)")
    private BigDecimal notTaxUnitPrice;

    /** 含税总价(元) */
    @ApiModelProperty(value =  "含税总价(元)")
    private BigDecimal taxPrice;

    /** 不含税总价(元) */
    @ApiModelProperty(value =  "不含税总价(元)")
    private BigDecimal notTaxPrice;

    /** 税率 */
    @ApiModelProperty(value =  "税率")
    private BigDecimal taxRate;

    /** 发票类型（1增值税专用发票/2增值税普通发票） */
    @ApiModelProperty(value =  "发票类型")
    private Integer billType;

    @ApiModelProperty(value = "浮动价")
    private BigDecimal floatingPrice;

    @ApiModelProperty(value = "卸费")
    private BigDecimal unloadingFee;

    @ApiModelProperty(value = "基价")
    private BigDecimal basePrice;

    @ApiModelProperty(value = "拆分id")
    private Long splitId;

    @ApiModelProperty(value = "物料id")
    private Long materialsId;

}
