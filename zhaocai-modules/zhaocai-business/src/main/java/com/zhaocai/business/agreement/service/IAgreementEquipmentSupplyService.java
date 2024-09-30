package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementEquipmentSupply;
import com.zhaocai.business.agreement.vo.res.AgreementEquipmentSupplyVO;

import java.util.List;

/**
 * 合同-甲供设备清单Service接口
 *
 * @author chenming
 * @date 2024-06-26
 */
public interface IAgreementEquipmentSupplyService  extends IService<AgreementEquipmentSupply> {

    /**
     * 保存合同-甲供设备清单
     * @param agreementEquipmentSupplies
     * @param agreementId
     */
    void saveAgreementEquipmentSupply(List<AgreementEquipmentSupply> agreementEquipmentSupplies, Long agreementId);

    /**
     * 获取合同-甲供设备清单
     * @param agreementId
     * @return
     */
    List<AgreementEquipmentSupplyVO> listByAgreementId(Long agreementId);

    /**
     * 修改合同-甲供设备清单
     * @param agreementEquipmentSupplies
     * @param agreementId
     */
    void updateAgreementEquipmentSupply(List<AgreementEquipmentSupply> agreementEquipmentSupplies, Long agreementId);
}
