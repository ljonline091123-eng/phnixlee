package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/6/3 15:25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingQuotatioDetailVO", description = "投标单清单报价详情信息VO")
public class BiddingQuotationDetailVO extends AdviceObject {

    @ApiModelProperty(value =  "清单编码")
    private String materialsCode;

    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value =  "投标单信息id")
    private Long biddingInfoId;

    @ApiModelProperty(value =  "含税单价(元)")
    private BigDecimal taxUnitPrice;

    @ApiModelProperty(value =  "不含税单价(元)")
    private BigDecimal notTaxUnitPrice;

    @ApiModelProperty(value =  "含税总价(元)")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价(元)")
    private BigDecimal notTaxPrice;

    @MoneyFormat(filedName = "taxUnitPrice")
    @ApiModelProperty(value =  "含税单价(元)（千分位）")
    private String taxUnitPricePattern;

    @MoneyFormat(filedName = "notTaxUnitPrice")
    @ApiModelProperty(value =  "不含税单价(元)（千分位）")
    private String notTaxUnitPricePattern;

    @MoneyFormat(filedName = "taxPrice",scale = 2)
    @ApiModelProperty(value =  "含税总价(元)（千分位）")
    private String taxPricePattern;

    @MoneyFormat(filedName = "notTaxPrice",scale = 2)
    @ApiModelProperty(value =  "不含税总价(元)（千分位）")
    private String notTaxPricePattern;

    @ApiModelProperty(value =  "税率")
    private BigDecimal taxRate;

    @ApiModelProperty(value =  "发票类型")
    private Integer billType;

    @DictCache(dictBizEnum = DictBizEnum.INVOICE_TYPE,filedName = "billType")
    @ApiModelProperty(value =  "发票类型-文本")
    private String billTypeText;

    @ApiModelProperty(value =  "物料名称")
    private String materialsName;

    @ApiModelProperty(value =  "规格型号")
    private String specification;

    @ApiModelProperty(value =  "计量单位")
    private String unitMeasurement;

    @ApiModelProperty(value =  "数量")
    private BigDecimal count;

    @ApiModelProperty(value =  "单价(含税)")
    private BigDecimal priceIncludingTax;


    @ApiModelProperty(value = "浮动价")
    private BigDecimal floatingPrice;

    @ApiModelProperty(value = "卸费")
    private BigDecimal unloadingFee;

    @ApiModelProperty(value = "基价")
    private BigDecimal basePrice;

    @MoneyFormat(filedName = "floatingPrice")
    @ApiModelProperty(value = "浮动价-文本")
    private String floatingPriceText;

    @MoneyFormat(filedName = "unloadingFee")
    @ApiModelProperty(value = "卸费-文本")
    private String unloadingFeeText;

    @MoneyFormat(filedName = "basePrice")
    @ApiModelProperty(value = "基价-文本")
    private String basePriceText;

}
