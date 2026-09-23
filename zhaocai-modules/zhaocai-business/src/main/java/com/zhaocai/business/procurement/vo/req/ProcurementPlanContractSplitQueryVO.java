package com.zhaocai.business.procurement.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 采购计划拆分合约
 *
 * @author chenming
 * @date 2024/06/15
 */
@Data
public class ProcurementPlanContractSplitQueryVO extends PageRecive {

    @ApiModelProperty(value =  "采购计划名称")
    private String procurementPlanName;

    @ApiModelProperty(value = "采购经办人")
    private Long procurementOfficer;

    @NotBlank(message = "项目编号不能为空，请先选择项目")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;
}
