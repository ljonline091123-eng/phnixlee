package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

import java.util.List;

/**
 * 回写合约规划请求 DTO
 *
 * @author chenming
 * @date 2024-07-13
 */
@Data
public class UpdatePlanQuantityAmountRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 项目Id
     */
    private String projectId;

    /**
     * 是否是支出合同变更产生的
     */
    private Boolean isContractChange;

    /**
     * 项目编号
     */
    private String projectCode;

    /**
     * 合约规划的已发生金额（含税）
     */
    private List<UpdatePlanQuantityAmount4ContractAmount> contractAmountUpdates;

    /**
     * 成本科目的已发生金额
     */
    private List<UpdatePlanQuantityAmount4SubjectAmount> subjectAmountUpdates;

    /**
     * 清单的已使用数量
     */
    private List<UpdatePlanQuantityAmount4SubjectDtlQuantity> subjectDtlQuantityUpdates;

    public UpdatePlanQuantityAmountRequestDTO(String projectId,String projectCode) {
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.isContractChange = false;
    }
}
