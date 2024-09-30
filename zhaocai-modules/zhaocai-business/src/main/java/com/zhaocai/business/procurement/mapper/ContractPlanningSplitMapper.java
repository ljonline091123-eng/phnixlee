package com.zhaocai.business.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.procurement.domain.ContractPlanningSplit;
import com.zhaocai.business.procurement.vo.req.ProcurementPlanContractSplitQueryVO;
import com.zhaocai.business.procurement.vo.res.ProcurementPlanContractSplitVO;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeSplitListVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合约规划拆分Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface ContractPlanningSplitMapper extends BaseMapper<ContractPlanningSplit> {

    /**
     * 根据 planId 删除合约规划拆分记录
     *
     * @param planId
     */
    void deleteByPlanId(@Param("planId") Long planId);

    /**
     * 获取采购方案的合约拆分
     *
     * @param schemeId
     * @return
     */
    List<ProcurementSchemeSplitListVO> selectContractSplitListBySchemeId(@Param("schemeId") Long schemeId);

    /**
     * 获取采购计划和拆分合约
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<ProcurementPlanContractSplitVO> selectPlanContractSplitList(Page mybatisPage, @Param("queryVO") ProcurementPlanContractSplitQueryVO queryVO);

    /**
     * 获取已经被采购方案使用的合约拆分
     *
     * @param contractSplitIds
     * @return
     */
    List<ContractPlanningSplit> selectExistPlanningSplits(@Param("contractSplitIds") List<Long> contractSplitIds);
}
