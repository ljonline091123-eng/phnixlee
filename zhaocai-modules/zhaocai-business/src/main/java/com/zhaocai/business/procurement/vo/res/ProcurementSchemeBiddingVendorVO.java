package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 采购方案投标的供应商信息
 *
 * @author chenming
 * @date 2024/06/04
 */
@Data
@NoArgsConstructor
public class ProcurementSchemeBiddingVendorVO extends AdviceObject {

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    // 中标结果
    @JsonIgnore
    private Integer bidResult;

    public ProcurementSchemeBiddingVendorVO(Long vendorId,String vendorName,Integer bidResult) {
        this.vendorId = vendorId;
        if (bidResult == 1) {
            this.vendorName = "(中标)" + vendorName;
        } else {
            this.vendorName = "(未中标)" + vendorName;
        }
    }
}
