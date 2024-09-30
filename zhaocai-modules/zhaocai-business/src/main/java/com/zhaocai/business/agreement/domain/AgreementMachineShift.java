package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同-机械台班对象 tb_agreement_machine_shift
 *
 * @author chenming
 * @date 2024-06-26
 */
@Data
@TableName(value = "tb_agreement_machine_shift")
public class AgreementMachineShift extends BaseEntity {
    /**
     * 合同id
     */
    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    /**
     * 设备名称编码
     */
    @ApiModelProperty(value = "设备名称编码")
    private String equipmentNameCode;

    /**
     * 设备名称
     */
    @ApiModelProperty(value = "设备名称")
    private String equipmentName;

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
     * 税率
     */
    @ApiModelProperty(value = "税率")
    private BigDecimal taxRate;

    /**
     * 单价(不含税)
     */
    @ApiModelProperty(value = "单价(不含税)")
    private BigDecimal unitPriceExcTax;

    /**
     * 单价(含税)
     */
    @ApiModelProperty(value = "单价(含税)")
    private BigDecimal unitPriceIncTax;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;
}
