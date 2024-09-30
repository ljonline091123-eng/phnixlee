package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementPaymentItem;
import com.zhaocai.business.agreement.vo.res.AgreementPaymentItemVO;

/**
 * 合同款项信息Service接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface IAgreementPaymentItemService  extends IService<AgreementPaymentItem> {

    /**
     * 保存合同款项信息
     * @param agreementPaymentItem
     * @param agreementId
     */
    void saveAgreementPaymentItem(AgreementPaymentItem agreementPaymentItem, Long agreementId);

    /**
     * 获取合同款项信息
     * @param agreementId
     * @return
     */
    AgreementPaymentItemVO getByAgreementId(Long agreementId);

    /**
     * 更新合同款项信息
     * @param agreementPaymentItem
     * @param agreementId
     */
    void updateAgreementPaymentItem(AgreementPaymentItem agreementPaymentItem, Long agreementId);
}
