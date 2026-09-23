package com.zhaocai.business.procurement.dto;

import lombok.Data;

import java.util.Date;

/**
 * 合约规划-采购计划
 *
 * @author chenming
 * @date 2024-09-07
 */
@Data
public class ContractProcurementPlanDTO {

    /**
     * 合约规划 id
     */
    private String contractPlanningId;

    /**
     * 进场时间
     */
    private Date arrivalDate;

    /**
     * 采购计划名称
     */
    private String procurementPlanName;
}
