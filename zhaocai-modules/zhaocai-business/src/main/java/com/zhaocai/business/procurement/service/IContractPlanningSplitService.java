package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementMaterialsList;
import com.zhaocai.business.procurement.domain.ContractPlanningSplit;
import com.zhaocai.business.procurement.domain.MaterialsList;
import com.zhaocai.business.procurement.vo.req.ContractPlanningSplitRequestVO;
import com.zhaocai.business.procurement.vo.req.ProcurementPlanContractSplitQueryVO;
import com.zhaocai.business.procurement.vo.res.ContractSplitListVO;
import com.zhaocai.business.procurement.vo.res.ProcurementPlanContractSplitVO;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeSplitListVO;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 合约规划拆分Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IContractPlanningSplitService  extends IService<ContractPlanningSplit> {

    /**
     * 保存合约拆分记录
     * @param splitRequestList
     * @param planId
     */
    void saveContractPlanningSplit(List<ContractPlanningSplitRequestVO> splitRequestList, Long planId,Integer priceType);

    /**
     * 修改采购计划的合约拆分
     * @param splitRequestList
     * @param planId
     */
    void updateContractPlanningSplit(List<ContractPlanningSplitRequestVO> splitRequestList, Long planId,Integer priceType);

    /**
     * 获取采购计划的合约拆分记录
     * @param planId
     * @return
     */
    List<ContractSplitListVO> listContractSplitByPlanId(Long planId);

    /**
     * 根据采购方案
     * @param schemeId
     * @return
     */
    List<ProcurementSchemeSplitListVO> listContractSplitBySchemeId(Long schemeId);

    /**
     * 获取采购计划和拆分合约
     * @param queryVO
     * @return
     */
    PageResult<ProcurementPlanContractSplitVO> listPlanContractSplit(ProcurementPlanContractSplitQueryVO queryVO);

    /**
     * 获取已经被采购方案使用的合约拆分
     * @param contractSplitIds
     * @return
     */
    List<ContractPlanningSplit> listExistPlanningSplits(List<Long> contractSplitIds);

    /**
     * 修改合约拆分使用数据-新增
     * @param contractSplitId
     * @param materialsLists
     * @param agreementMaterialsLists
     */
    void updateContractPlanningSplitUseAdd(Long contractSplitId, List<MaterialsList> materialsLists, List<AgreementMaterialsList> agreementMaterialsLists);

    /**
     * 修改合约拆分使用数据-减少
     * @param contractSplitId
     * @param agreementMaterialsLists
     */
    void updateContractPlanningSplitUseSub(Long contractSplitId, List<AgreementMaterialsList> agreementMaterialsLists);
}
