package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.procurement.domain.ProcurementSchemeBidding;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeBiddingVO;

/**
 * 采购方案-招标信息Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IProcurementSchemeBiddingService  extends IService<ProcurementSchemeBidding> {

    /**
     * 保存采购方案 - 招标信息
     * @param requestVO
     * @param schemeId
     */
    void saveProcurementSchemeBidding(ProcurementSchemeBidding requestVO, Long schemeId, Integer procurementType);

    /**
     * 修改采购方案 - 招标信息
     * @param procurementSchemeBidding
     * @param schemeId
     */
    void updateProcurementSchemeBidding(ProcurementSchemeBidding procurementSchemeBidding, Long schemeId, Integer procurementType);

    /**
     * 根据采购方案获取
     * @param schemeId
     * @return
     */
    ProcurementSchemeBiddingVO getBySchemeId(Long schemeId);

    ProcurementSchemeBiddingVO getBiddingTemplateBySchemeId(Long schemeId);

    /**
     * 根据采购方案 id 获取采购方方案的招标信息
     * @param schemeId
     * @return
     */
    ProcurementSchemeBidding getDomainBySchemeId(Long schemeId);
}
