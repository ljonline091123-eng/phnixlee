package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.dto.AgreementMaterialsInfoDTO;
import com.zhaocai.business.procurement.domain.MaterialsList;
import com.zhaocai.business.procurement.domain.ProcurementPlan;
import com.zhaocai.business.procurement.dto.SubjectMatterDTO;
import com.zhaocai.business.procurement.vo.req.ContractSplitMaterialsQueryVO;
import com.zhaocai.business.procurement.vo.res.CompContractSplitMaterialsVO;
import com.zhaocai.business.procurement.vo.res.ContractSplitMaterialsVO;

import java.util.List;

/**
 * 采购物料清单Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IMaterialsListService  extends IService<MaterialsList> {

    /**
     * 保存物料清单
     * @param materialsLists
     * @param contractSplitId
     * @param planId
     */
    List<MaterialsList> saveMaterialsList(List<MaterialsList> materialsLists, Long contractSplitId, Long planId, ProcurementPlan procurementPlan,Integer[] floatCount,Integer[] fixedCount);

    /**
     * 删除物料清单数据
     * @param planId
     */
    void deleteByPlanId(Long planId);

    /**
     * 获取采购订单的物料清单
     * @param planId
     * @return
     */
    List<ContractSplitMaterialsVO> listMaterialsByPlanId(Long planId,Boolean isFilter);

    /**
     * 批量获取采购计划的采购物料
     * @param planIds
     * @return
     */
    List<MaterialsList> listMaterialsListByPlanIds(List<Long> planIds);

    /**
     * 获取合约拆分的物料清单
     * @param contractSplitIds
     * @return
     */
    List<MaterialsList> listMaterialsListByContractSplitIds(List<Long> contractSplitIds);

    /**
     * 获取采购计划的合约拆分物料记录
     * @param queryVO
     * @return
     */
    List<ContractSplitMaterialsVO> listContractSplitMaterials(ContractSplitMaterialsQueryVO queryVO);

    /**
     * 获取采购计划的合约拆分物料记录（投标）
     * @param queryVO
     * @return
     */
    List<CompContractSplitMaterialsVO> listContractSplitMaterials4Bidding(ContractSplitMaterialsQueryVO queryVO);

    /**
     * 获取清单的交易标的物
     * @param procurementPlanType
     * @param materialsCode
     * @param contractPlanCode
     * @return
     */
    SubjectMatterDTO getSubjectMatter(Integer procurementPlanType,String materialsCode,String contractPlanCode);

    /**
     * 获取交易标的物类型 0其它 1 钢筋采购 2 商品砼采购
     * @param subjectMatterCode
     * @return
     */
    Integer getSubjectMatterType(String subjectMatterCode);

    /**
     * 修改合同使用数量
     * @param materialsLists
     */
    void updateMaterialsListUsedCount(List<MaterialsList> materialsLists);
}
