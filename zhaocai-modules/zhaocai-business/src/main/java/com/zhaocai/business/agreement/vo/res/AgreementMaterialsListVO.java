package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同物料清单对象
 *
 * @author chenming
 * @date 2024-06-19
 */
@Data
public class AgreementMaterialsListVO extends AdviceObject {
    @ApiModelProperty(value = "主键 id")
    private String id;

    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    @ApiModelProperty(value = "物料清单id")
    private Long materialsListId;

    @ApiModelProperty(value = "投标清单报价 id")
    private Long biddingListQuotationId;

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

    @ApiModelProperty(value = "计量单位")
    private String unitMeasurement;

    @ApiModelProperty(value = "计量规则")
    private String measurementRules;

    @ApiModelProperty(value = "基本工作内容")
    private String workContent;

    @ApiModelProperty(value = "成本科目")
    private String costAccount;

    @ApiModelProperty(value = "数量")
    private BigDecimal count;

    @ApiModelProperty(value =  "含税单价(元)")
    private BigDecimal taxUnitPrice;

    @ApiModelProperty(value =  "不含税单价(元)")
    private BigDecimal notTaxUnitPrice;

    @ApiModelProperty(value =  "含税总价(元)")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价(元)")
    private BigDecimal notTaxPrice;

    @ApiModelProperty(value =  "税率")
    private BigDecimal taxRate;

    @ApiModelProperty(value = "税额")
    private BigDecimal taxAmount;

    @ApiModelProperty(value =  "发票类型")
    private Integer billType;

    @ApiModelProperty(value = "品牌")
    private String brand;

    @ApiModelProperty(value = "价款类型")
    private String paymentType;

    @ApiModelProperty(value = "计租单位")
    private String rentalUnit;

    @ApiModelProperty(value = "工作量")
    private String workload;

    @ApiModelProperty(value = "备注")
    private String remark;

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

    @ApiModelProperty(value = "租赁方式")
    private String rentModeText;

    @ApiModelProperty(value = "计租单位")
    private String rentalUnitText;

    @ApiModelProperty(value = "签订数量")
    private BigDecimal signCount;

    @ApiModelProperty(value = "签订税率")
    private BigDecimal signTaxRate;

    @ApiModelProperty(value = "签订单价（含税）")
    private BigDecimal signUnitPriceInclTax;

    @ApiModelProperty(value = "签订单价（不含税）")
    private BigDecimal signUnitPriceExclTax;

    @ApiModelProperty(value = "签订金额（含税）")
    private BigDecimal signAmountInclTax;

    @ApiModelProperty(value = "签订金额（不含税）")
    private BigDecimal signAmountExclTax;

    @ApiModelProperty(value = "签订税额")
    private BigDecimal signTaxAmount;

    @DictCache(dictBizEnum = DictBizEnum.PRICE_TYPE,filedName = "paymentType")
    @ApiModelProperty(value = "价款类型")
    private String paymentTypeText;

    @DictCache(dictBizEnum = DictBizEnum.INVOICE_TYPE,filedName = "billType")
    @ApiModelProperty(value =  "发票类型-文本")
    private String billTypeText;

    @MoneyFormat(filedName = "count")
    @ApiModelProperty(value = "数量")
    private String countText;

    @MoneyFormat(filedName = "taxUnitPrice")
    @ApiModelProperty(value =  "含税单价(元)")
    private String taxUnitPriceText;

    @MoneyFormat(filedName = "notTaxUnitPrice")
    @ApiModelProperty(value =  "不含税单价(元)")
    private String notTaxUnitPriceText;

    @MoneyFormat(filedName = "taxPrice",scale = 2)
    @ApiModelProperty(value =  "含税总价(元)")
    private String taxPriceText;

    @MoneyFormat(filedName = "notTaxPrice",scale = 2)
    @ApiModelProperty(value =  "不含税总价(元)")
    private String notTaxPriceText;

    @MoneyFormat(filedName = "taxRate",scale = 2)
    @ApiModelProperty(value =  "税率")
    private String taxRateText;

    @MoneyFormat(filedName = "taxAmount",scale = 2)
    @ApiModelProperty(value =  "税额")
    private String taxAmountText;

    @MoneyFormat(filedName = "rentTime")
    @ApiModelProperty(value = "租赁时间")
    private String rentTimeText;

    @MoneyFormat(filedName = "rentQuantity")
    @ApiModelProperty(value = "租赁数量")
    private String rentQuantityText;

    @MoneyFormat(filedName = "floatingPrice")
    @ApiModelProperty(value = "浮动价")
    private String floatingPriceText;

    @MoneyFormat(filedName = "unloadingFee")
    @ApiModelProperty(value = "卸费")
    private String unloadingFeeText;

    @MoneyFormat(filedName = "basePrice")
    @ApiModelProperty(value = "基价")
    private String basePriceText;

    @MoneyFormat(filedName = "signCount")
    @ApiModelProperty(value = "签订数量-文本")
    private String signCountText;

    @MoneyFormat(filedName = "signUnitPriceInclTax")
    @ApiModelProperty(value = "签订单价（含税）-文本")
    private String signUnitPriceInclTaxText;

    @MoneyFormat(filedName = "signUnitPriceExclTax")
    @ApiModelProperty(value = "签订单价（不含税）-文本")
    private String signUnitPriceExclTaxText;

    @MoneyFormat(filedName = "signAmountInclTax")
    @ApiModelProperty(value = "签订金额（含税）-文本")
    private String signAmountInclTaxText;

    @MoneyFormat(filedName = "signAmountExclTax")
    @ApiModelProperty(value = "签订金额（不含税）-文本")
    private String signAmountExclTaxText;

    @MoneyFormat(filedName = "signTaxAmount")
    @ApiModelProperty(value = "签订税额-文本")
    private String signTaxAmountText;

    @MoneyFormat(filedName = "signTaxRate")
    @ApiModelProperty(value = "签订税率")
    private String signTaxRateText;

    @ApiModelProperty(value = "易料市集商品编码")
    private String offerGoodsCode;

    @ApiModelProperty(value = "易料市集商品名")
    private String goodsName;

    @ApiModelProperty(value = "易料市集品牌")
    private String offerBrand;

    @ApiModelProperty(value = "易料市集含税单价")
    private BigDecimal offerPrice;

    @ApiModelProperty(value = "易料市集商品id")
    private String skuId;
}
