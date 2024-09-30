package com.zhaocai.business.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.procurement.domain.ContractPlanning;
import com.zhaocai.business.procurement.vo.res.ProcurementContractPlanListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 合约规划Mapper接口
 *
 * @author chenming
 * @date 2024-06-17
 */
public interface ContractPlanningMapper extends BaseMapper<ContractPlanning> {

    /**
     * 获取采购计划的合约规划
     * @param planIds
     * @return
     */
    List<ProcurementContractPlanListVO> selectProcurementContractPlanningList(@Param("planIds") List<Long> planIds);

    /**
     * 根据合约拆分记录获取合约规划
     *
     * @param splitId
     * @return
     */
    ContractPlanning selectByContractSplitId(@Param("splitId") Long splitId);

    /**
     * 根据采购计划删除合约规划
     *
     * @param planId
     */
    void deleteByPlanId(@Param("planId") Long planId);
}
