package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 物料清单vo
 *
 * @author chenming
 * @date 2024/05/28
 */
@Data
public class MaterialsVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "物料 id")
    private String materialsId;

    @ApiModelProperty(value = "物料清单唯一 id")
    private String materialsUniqueId;

    @ApiModelProperty(value = "物料编码")
    private String materialsCode;

    @ApiModelProperty(value = "物料名称")
    private String materialsName;

    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value = "交易标的物标识")
    private String subjectMatterFlag;

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

    @MoneyFormat(filedName = "rentTime",scale = 2)
    @ApiModelProperty(value = "租赁时间")
    private String rentTimeText;

    @MoneyFormat(filedName = "rentQuantity",scale = 2)
    @ApiModelProperty(value = "租赁数量")
    private String rentQuantityText;
    
    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @ApiModelProperty(value = "商品编号")
    private String code;

    @ApiModelProperty(value = "商品名称")
    private String name;

    @ApiModelProperty(value = "商品规格")
    private String category;

    @ApiModelProperty(value = "商品单位")
    private String unitName;

    @ApiModelProperty(value = "商品数量")
    private BigDecimal quantity;

    @ApiModelProperty(value = "是否推送 Y:推送 N:未推送")
    private String pushFlag;

    public BigDecimal getTransferQuantity() {
        return this.getCount();
    }
}
