package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/8/20 15:48
 */
@Data
public class CompMaterialsContentVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "物料 id")
    private String materialsId;

    @ApiModelProperty(value = "物料编码")
    private String materialsCode;

    @ApiModelProperty(value = "物料名称")
    private String materialsName;

    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value = "规格型号")
    private String specification;

    @ApiModelProperty(value = "计量规则")
    private String measurementRules;

    @ApiModelProperty(value = "计量单位")
    private String unitMeasurement;

    @ApiModelProperty(value = "基本工作内容")
    private String workContent;

    @ApiModelProperty(value = "成本科目档案 id")
    private String costAccountId;

    @ApiModelProperty(value = "成本科目编码")
    private String costAccountCode;

    @ApiModelProperty(value = "成本科目名称")
    private String costAccountName;

    @ApiModelProperty(value = "数量")
    private BigDecimal count;

    @ApiModelProperty(value = "转换量")
    private BigDecimal transferQuantity;

    @ApiModelProperty(value = "税率")
    private BigDecimal taxRate;

    @ApiModelProperty(value = "税率编码")
    private String taxRateCode;

    @DictCache(dictBizEnum = DictBizEnum.RAX_ARCHIVES,filedName = "taxRateCode")
    @ApiModelProperty(value = "税率名称")
    private String taxRateName;

    @ApiModelProperty(value = "单价(不含税)")
    private BigDecimal unitPriceExclTax;

    @ApiModelProperty(value = "单价(含税) ")
    private BigDecimal unitPriceInclTax;

    @ApiModelProperty(value = "金额(不含税)")
    private BigDecimal amountExclTax;

    @ApiModelProperty(value = "金额(含税)")
    private BigDecimal amountInclTax;

    @ApiModelProperty(value = "税额")
    private BigDecimal taxAmount;

    @ApiModelProperty(value = "浮动价")
    private BigDecimal floatingPrice;

    @ApiModelProperty(value = "卸费")
    private BigDecimal unloadingFee;

    @ApiModelProperty(value = "基价")
    private BigDecimal basePrice;

    @ApiModelProperty(value = "租赁方式")
    private String rentMode;

    @ApiModelProperty(value = "租赁时间")
    private BigDecimal rentTime;

    @ApiModelProperty(value = "租赁数量")
    private BigDecimal rentQuantity;

    @MoneyFormat(filedName = "count")
    @ApiModelProperty(value = "数量")
    private String countText;

    @MoneyFormat(filedName = "count")
    @ApiModelProperty(value = "转换量")
    private String transferQuantityText;

    @MoneyFormat(filedName = "unitPriceInclTax")
    @ApiModelProperty(value = "单价(含税)")
    private String unitPriceInclTaxText;

    @MoneyFormat(filedName = "floatingPrice")
    @ApiModelProperty(value = "浮动价-文本")
    private String floatingPriceText;

    @MoneyFormat(filedName = "unloadingFee")
    @ApiModelProperty(value = "卸费-文本")
    private String unloadingFeeText;

    @MoneyFormat(filedName = "basePrice")
    @ApiModelProperty(value = "基价-文本")
    private String basePriceText;

    @ApiModelProperty(value = "租赁方式")
    private String rentModeText;

    @MoneyFormat(filedName = "rentTime")
    @ApiModelProperty(value = "租赁时间")
    private String rentTimeText;

    @MoneyFormat(filedName = "rentQuantity")
    @ApiModelProperty(value = "租赁数量")
    private String rentQuantityText;

    /** 投标参数 */
    @ApiModelProperty(value =  "含税单价(元)")
    private BigDecimal taxUnitPrice;

    @ApiModelProperty(value =  "不含税单价(元)")
    private BigDecimal notTaxUnitPrice;

    @ApiModelProperty(value =  "发票类型（1增值税专用发票/2增值税普通发票/3数电票） ")
    private Integer billType;

    @MoneyFormat(filedName = "taxUnitPrice")
    @ApiModelProperty(value =  "含税单价(元)（千分位）")
    private String taxUnitPriceText;

    @MoneyFormat(filedName = "notTaxUnitPrice")
    @ApiModelProperty(value =  "不含税单价(元)（千分位）")
    private String notTaxUnitPriceText;

    @ApiModelProperty(value =  "含税总价(元)")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价(元)")
    private BigDecimal notTaxPrice;

    @MoneyFormat(filedName = "taxPrice")
    @ApiModelProperty(value =  "含税总价(元)（千分位）")
    private String taxPriceText;

    @MoneyFormat(filedName = "notTaxPrice")
    @ApiModelProperty(value =  "不含税总价(元)（千分位）")
    private String notTaxPriceText;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

}
