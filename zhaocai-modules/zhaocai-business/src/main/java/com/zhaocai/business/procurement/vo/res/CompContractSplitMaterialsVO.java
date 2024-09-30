package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/8/19 16:55
 */
@Data
public class CompContractSplitMaterialsVO extends AdviceObject {

    @ApiModelProperty(value = "合约规划名称（计划名称）")
    private String compName;

    @ApiModelProperty(hidden = true)
    private Long splitId;

    @ApiModelProperty(value = "拆分合约规划名称")
    private String splitContractName;

    @ApiModelProperty(value = "拟签约合同拆包范围")
    private String contractScope;

    @ApiModelProperty(value = "物料清单")
    private List<CompMaterialsContentVO> materialsLists;

}
