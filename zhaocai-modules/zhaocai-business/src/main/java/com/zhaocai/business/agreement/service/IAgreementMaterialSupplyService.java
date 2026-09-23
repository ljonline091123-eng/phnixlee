package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementMaterialSupply;
import com.zhaocai.business.agreement.vo.res.AgreementMaterialSupplyVO;

import java.util.List;

/**
 * 合同-甲供材料清单Service接口
 *
 * @author chenming
 * @date 2024-06-26
 */
public interface IAgreementMaterialSupplyService  extends IService<AgreementMaterialSupply> {

    /**
     * 保存合同-甲供材料清单
     * @param agreementMaterialSupplies
     * @param agreementId
     */
    void saveAgreementMaterialSupply(List<AgreementMaterialSupply> agreementMaterialSupplies, Long agreementId);

    /**
     * 获取合同-甲供材料清单
     * @param id
     * @return
     */
    List<AgreementMaterialSupplyVO> listByAgreementId(Long agreementId);

    /**
     * 修改合同-甲供材料清单
     * @param agreementMaterialSupplies
     * @param agreementId
     */
    void updateAgreementMaterialSupply(List<AgreementMaterialSupply> agreementMaterialSupplies, Long agreementId);
}
