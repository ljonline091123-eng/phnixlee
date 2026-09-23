package com.zhaocai.business.manager.http.dto.req;


import com.zhaocai.business.common.conver.ProcurementPlanTypeConver;
import com.zhaocai.business.procurement.vo.req.ContractPlanningListQueryVO;
import lombok.Data;

/**
 * 合约规划列表 DTO
 *
 * @author chenming
 * @date 2024-07-09
 */
@Data
public class ContractPlanListReqDTO extends UnderlyingPlatformPageBaseDTO {

    /**
     * 合约规划名称
     */
    private String conPlanName;

    /**
     * 合约规划类型
     */
    private String conPlanType;

    /**
     * 项目id
     */
    private String projectId;

    public ContractPlanListReqDTO(ContractPlanningListQueryVO queryVO) {
        this.pageSize = queryVO.getPageSize();
        this.pageNum = queryVO.getPageNumber();
        this.projectId = queryVO.getProjectId();
        this.conPlanName = queryVO.getContractName();
        this.conPlanType = ProcurementPlanTypeConver.converFromProcurementPlanType(queryVO.getContractType());
    }

}
