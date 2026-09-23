package com.zhaocai.business.procurement.vo.req;

import com.zhaocai.business.procurement.domain.ContractPlanning;
import com.zhaocai.business.procurement.domain.ProcurementPlan;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

/**
 * 采购计划请求参数
 *
 * @author chenming
 * @Date 2024/05/27
 */
@Data
@ApiModel("采购计划请求参数")
public class ProcurementPlanRequestVO {

    @Valid
    @ApiModelProperty("采购计划基本信息")
    private ProcurementPlan procurementPlan;

    @Valid
    @ApiModelProperty("合约拆分")
    private ContractPlanning contractPlanning;

    @Valid
    @ApiModelProperty("合约规划拆分信息")
    private List<ContractPlanningSplitRequestVO> splitRequestList;
}
