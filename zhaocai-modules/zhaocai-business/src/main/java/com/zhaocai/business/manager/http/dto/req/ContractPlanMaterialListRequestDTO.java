package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 获取合约规划清单请求 DTO
 * @author chenming
 * @date 2024-07-09
 */
@Data
public class ContractPlanMaterialListRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 合约规划ID
     */
    @NotBlank(message = "合约规划 id 不能为空")
    private String conPlanId;

    /**
     * 合约规划类型
     */
    private String conPlanType;

    /**
     * 项目id
     */
    @NotBlank(message = "项目 id 不能为空")
    private String projectId;
}
