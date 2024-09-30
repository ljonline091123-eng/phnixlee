package com.zhaocai.business.procurement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购方案拆分清单
 *
 * @author chenming
 * @date 2024/06/04
 */
@Data
public class ProcurementSchemeSplitListVO {

    @ApiModelProperty(value = "拆分id")
    private Long splitId;

    @ApiModelProperty(value = "拆分合约规划名称")
    private String splitContractName;

    @ApiModelProperty(value = "拟签约合同拆包范围")
    private String contractScope;

    @ApiModelProperty(value = "是否已用完")
    private Integer isUseUp;
}
