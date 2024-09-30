package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.procurement.domain.ContractPlanning;
import com.zhaocai.business.procurement.vo.res.ContractPlanningListVO;
import com.zhaocai.business.procurement.vo.res.ProcurementContractPlanListVO;

import java.util.List;

/**
 * 合约规划Service接口
 *
 * @author chenming
 * @date 2024-06-17
 */
public interface IContractPlanningService  extends IService<ContractPlanning> {


    /**
     * 保存合约规划
     * @param contractPlanning
     * @param planId
     */
    void addContractPlanning(ContractPlanning contractPlanning, Long planId);

    /**
     * 根据合约拆分记录获取合约规划信息
     * @param contractSplitIds
     * @return
     */
    List<ProcurementContractPlanListVO> listProcurementContractPlanByContractSplit(List<Long> contractSplitIds);

    /**
     * 根据合约规划获取合约规划
     * @param contractPlanningId
     * @return
     */
    ContractPlanning getByContractPlanningId(String contractPlanningId);

    /**
     * 从底层逻辑获取采购计划的合约规划数据
     * @param procurementPlanId
     * @return
     */
    ContractPlanningListVO getByProcurementIdFromUnderling(Long procurementPlanId);

    /**
     * 根据合约拆分记录获取对应的合约规划
     * @param splitId
     * @return
     */
    ContractPlanning getByContractSplitId(Long splitId);

    /**
     * 根据采购计划 id 获取合约规划数据
     * @param procurementPlanId
     * @return
     */
    ContractPlanning getByProcurementId(Long procurementPlanId);

    /**
     * 修改采购计划的合约规划
     * @param contractPlanning
     * @param planId
     */
    void updateContractPlanning(ContractPlanning contractPlanning, Long planId);

    /**
     * 根据采购计划id获取对应的合约规划
     * @param planIdList
     * @return
     */
    List<ProcurementContractPlanListVO> listByPlanIds(List<Long> planIdList);
}
