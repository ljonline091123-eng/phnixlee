package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同-机械台班对象
 *
 * @author chenming
 * @date 2024-06-26
 */
@Data
public class AgreementMachineShiftVO extends AdviceObject {
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "合同 id")
    private Long agreementId;

    @ApiModelProperty(value = "设备名称编码")
    private String equipmentNameCode;

    @ApiModelProperty(value = "设备名称")
    private String equipmentName;

    @ApiModelProperty(value = "规格型号")
    private String specification;

    @ApiModelProperty(value = "计量单位")
    private String unitMeasurement;

    @ApiModelProperty(value = "税率")
    private BigDecimal taxRate;

    @ApiModelProperty(value = "单价(不含税)")
    private BigDecimal unitPriceExcTax;

    @ApiModelProperty(value = "单价(含税)")
    private BigDecimal unitPriceIncTax;

    @ApiModelProperty(value = "备注")
    private String remark;

    @MoneyFormat(filedName = "taxRate",scale = 2)
    @ApiModelProperty(value = "税率")
    private String taxRateText;

    @MoneyFormat(filedName = "unitPriceExcTax")
    @ApiModelProperty(value = "单价(不含税)")
    private String unitPriceExcTaxText;

    @MoneyFormat(filedName = "unitPriceIncTax")
    @ApiModelProperty(value = "单价(含税)")
    private String unitPriceIncTaxText;
}
