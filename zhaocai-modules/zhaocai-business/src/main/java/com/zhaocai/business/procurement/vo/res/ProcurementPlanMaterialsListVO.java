package com.zhaocai.business.procurement.vo.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 采购计划物料清单
 *
 * @author chenming
 * @date 2024/05/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementPlanMaterialsListVO {

    @ApiModelProperty(value = "采购计划名称")
    private String procurementPlanName;

    @ApiModelProperty(value = "拆分合约规划-物料清单")
    private List<ContractSplitMaterialsVO> contractSplitMaterials;
}
