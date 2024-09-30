package com.zhaocai.business.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.procurement.domain.ProcurementPlan;
import com.zhaocai.business.procurement.dto.ContractProcurementPlanDTO;
import com.zhaocai.business.procurement.vo.req.ProcurementPlanListQueryVO;
import com.zhaocai.business.procurement.vo.res.ProcurementPlanListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 采购计划Mapper接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface ProcurementPlanMapper extends BaseMapper<ProcurementPlan> {

    /**
     * 分页查询
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<ProcurementPlanListVO> selectListPage(Page mybatisPage, @Param("queryVO") ProcurementPlanListQueryVO queryVO);

    /**
     * 根据合约拆分id获取绑定的采购计划
     * @param splitId
     * @return
     */
    ProcurementPlan selectBySplitId(@Param("splitId") Long splitId);

    /**
     * 根据合约规划 id 获取对应的第一个采购计划
     *
     * @param contractIdList
     * @return
     */
    List<ContractProcurementPlanDTO> selectFirstTimeProcurementPlanByContractPlan(@Param("contractIdList") List<String> contractIdList);
}
