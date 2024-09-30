package com.zhaocai.business.procurement.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 获取合约规划清单请求 DTO
 * @author chenming
 * @date 2024-07-09
 */
@Data
public class ContractPlanMaterialListQueryVO{

    @NotBlank(message = "合约规划 id 不能为空")
    @ApiModelProperty(value = "合约规划 id")
    private String conPlanId;

    @ApiModelProperty(value = "项目 id")
    @NotBlank(message = "项目 id 不能为空")
    private String projectId;

    @NotNull(message = "采购方案类型 不能为空")
    @ApiModelProperty(value = "采购方案类型")
    private Integer procurementType;

    @NotNull(message = "合约规划编码不能为空")
    @ApiModelProperty(value = "合约规划编码")
    private String conPlanCode;
}
