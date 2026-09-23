package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同-甲供材料清单对象 tb_agreement_material_supply
 *
 * @author chenming
 * @date 2024-06-26
 */
@Data
public class AgreementMaterialSupplyVO extends AdviceObject {
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "合同 id")
    private Long agreementId;

    @ApiModelProperty(value = "物资名称编码")
    private String materialNameCode;

    @ApiModelProperty(value = "物资名称")
    private String materialName;

    @ApiModelProperty(value = "规格型号")
    private String specification;

    @ApiModelProperty(value = "计量单位")
    private String unitMeasurement;

    @ApiModelProperty(value = "预估数量")
    private BigDecimal estimatedCount;

    @ApiModelProperty(value = "预估税率")
    private BigDecimal estimatedTaxRate;

    @ApiModelProperty(value = "预估单价(不含税)")
    private BigDecimal estimatedUnitPriceExcTax;

    @ApiModelProperty(value = "预估单价(含税)")
    private BigDecimal estimatedUnitPriceIncTax;

    @ApiModelProperty(value = "预估金额(不含税)")
    private BigDecimal estimatedAmountExcTax;

    @ApiModelProperty(value = "预估金额(含税)")
    private BigDecimal estimatedAmountIncTax;

    @ApiModelProperty(value = "预估税额")
    private BigDecimal estimatedTaxAmount;

    @ApiModelProperty(value = "备注")
    private String remark;

    @MoneyFormat(filedName = "estimatedCount",scale = 2)
    @ApiModelProperty(value = "预估数量")
    private String estimatedCountText;

    @MoneyFormat(filedName = "estimatedTaxRate",scale = 2)
    @ApiModelProperty(value = "预估税率")
    private String estimatedTaxRateText;

    @MoneyFormat(filedName = "estimatedUnitPriceExcTax")
    @ApiModelProperty(value = "预估单价(不含税)")
    private String estimatedUnitPriceExcTaxText;

    @MoneyFormat(filedName = "estimatedUnitPriceIncTax")
    @ApiModelProperty(value = "预估单价(含税)")
    private String estimatedUnitPriceIncTaxText;

    @MoneyFormat(filedName = "estimatedAmountExcTax",scale = 2)
    @ApiModelProperty(value = "预估金额(不含税)")
    private String estimatedAmountExcTaxText;

    @MoneyFormat(filedName = "estimatedAmountIncTax",scale = 2)
    @ApiModelProperty(value = "预估金额(含税)")
    private String estimatedAmountIncTaxText;

    @MoneyFormat(filedName = "estimatedTaxAmount",scale = 2)
    @ApiModelProperty(value = "预估税额")
    private String estimatedTaxAmountText;
}
