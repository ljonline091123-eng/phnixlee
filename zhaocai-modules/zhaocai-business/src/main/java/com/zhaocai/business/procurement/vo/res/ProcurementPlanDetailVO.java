package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 采购计划详情
 *
 * @author chenming
 * @date 2024/05/28
 */
@Data
@Builder
public class ProcurementPlanDetailVO extends AdviceObject {

    @ApiModelProperty(value = "采购计划基本信息")
    private ProcurementPlanVO procurementPlan;

    @ApiModelProperty(value = "物料清单信息")
    private List<ContractSplitMaterialsVO> splitMaterials;

    @ApiModelProperty(value = "合约规划清单")
    private ContractPlanningListVO contractPlanning;
}
