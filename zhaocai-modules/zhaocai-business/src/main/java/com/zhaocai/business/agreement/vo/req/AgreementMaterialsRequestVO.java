package com.zhaocai.business.agreement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;


/**
 * 合同物料清单请求 VO
 *
 * @author chenming
 * @date 2024-09-04
 */
@Data
public class AgreementMaterialsRequestVO {

    @ApiModelProperty(value = "物料清单id")
    private Long materialsListId;

    @ApiModelProperty(value = "签订数量")
    private BigDecimal signCount;

    @ApiModelProperty(value = "签订单价（含税）")
    private BigDecimal signUnitPriceInclTax;
}
