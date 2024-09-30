package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
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
@TableName(value = "tb_agreement_material_supply")
public class AgreementMaterialSupply extends BaseEntity {

    /**
     * 合同id
     */
    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    /**
     * 物资名称编码
     */
    @ApiModelProperty(value = "物资名称编码")
    private String materialNameCode;

    /**
     * 物资名称
     */
    @ApiModelProperty(value = "物资名称")
    private String materialName;

    /**
     * 规格型号
     */
    @ApiModelProperty(value = "规格型号")
    private String specification;

    /**
     * 计量单位
     */
    @ApiModelProperty(value = "计量单位")
    private String unitMeasurement;

    /**
     * 预估数量
     */
    @ApiModelProperty(value = "预估数量")
    private BigDecimal estimatedCount;

    /**
     * 预估税率
     */
    @ApiModelProperty(value = "预估税率")
    private BigDecimal estimatedTaxRate;

    /**
     * 预估单价(不含税)
     */
    @ApiModelProperty(value = "预估单价(不含税)")
    private BigDecimal estimatedUnitPriceExcTax;

    /**
     * 预估单价(含税)
     */
    @ApiModelProperty(value = "预估单价(含税)")
    private BigDecimal estimatedUnitPriceIncTax;

    /**
     * 预估金额(不含税)
     */
    @ApiModelProperty(value = "预估金额(不含税)")
    private BigDecimal estimatedAmountExcTax;

    /**
     * 预估金额(含税)
     */
    @ApiModelProperty(value = "预估金额(含税)")
    private BigDecimal estimatedAmountIncTax;

    /**
     * 预估税额
     */
    @ApiModelProperty(value = "预估税额")
    private BigDecimal estimatedTaxAmount;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;
}
