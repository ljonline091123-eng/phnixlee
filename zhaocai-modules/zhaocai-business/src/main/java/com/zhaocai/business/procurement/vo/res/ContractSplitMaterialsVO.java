package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 拆分合约规划-物料清单
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
public class ContractSplitMaterialsVO extends AdviceObject {

    @ApiModelProperty(hidden = true)
    private Long splitId;

    @ApiModelProperty(value = "拆分合约规划名称")
    private String splitContractName;

    @ApiModelProperty(value = "拟签约合同拆包范围")
    private String contractScope;

    @ApiModelProperty(value = "物料清单")
    private List<MaterialsVO> materialsLists;
}
