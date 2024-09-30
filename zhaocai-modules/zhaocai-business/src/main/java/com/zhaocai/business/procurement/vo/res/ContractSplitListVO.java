package com.zhaocai.business.procurement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 合同拆分列表
 *
 * @author chenming
 * @date 2024/06/07
 */
@Data
public class ContractSplitListVO {

    @ApiModelProperty(value = "拆分合约规划 id")
    private Long splitContractId;

    @ApiModelProperty(value = "拆分合约规划名称")
    private String splitContractName;

    @ApiModelProperty(value = "拟签约合同拆包范围")
    private String contractScope;

    @ApiModelProperty(value = "是否用尽")
    private Integer isUseUp;
}
