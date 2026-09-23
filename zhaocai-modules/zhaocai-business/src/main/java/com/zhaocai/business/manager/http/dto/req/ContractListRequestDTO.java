package com.zhaocai.business.manager.http.dto.req;


import com.zhaocai.business.common.conver.ProcurementPlanTypeConver;
import lombok.Data;

/**
 * 合同列表请求
 *
 * @author chenming
 * @date 2024-07-12
 */
@Data
public class ContractListRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 项目
     */
    private String projectId;

    /**
     * 合同乙方 id
     */
    private String partbId;

    /**
     * 业务类型
     */
    private String conType;

    public ContractListRequestDTO(Long vendorId,Integer expenditureBusinessType) {
        this.partbId = vendorId.toString();
        this.conType = ProcurementPlanTypeConver.converFromProcurementPlanType(expenditureBusinessType);
    }
}
