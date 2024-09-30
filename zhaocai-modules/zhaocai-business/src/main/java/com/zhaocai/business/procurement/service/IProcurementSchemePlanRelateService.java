package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.domain.ProcurementSchemePlanRelate;

import java.util.List;

/**
 * 采购方案-采购计划对应关系Service接口
 *
 * @author chenming
 * @date 2024-05-29
 */
public interface IProcurementSchemePlanRelateService  extends IService<ProcurementSchemePlanRelate> {

    /**
     * 保存对应关系
     * @param contractSplitId
     * @param schemeId
     */
    void saveRelate(List<Long> contractSplitId, Long schemeId);

    /**
     * 根据采购方案获取
     * @param schemeId
     * @return
     */
    List<ProcurementSchemePlanRelate> listBySchemeId(Long schemeId);

    /**
     * 获取与采购计划关联的采购方案
     * @param planId
     * @return
     */
    List<ProcurementScheme> listProcurementSchemeByRelatePlanId(Long planId);
}
