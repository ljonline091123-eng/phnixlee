package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementPaymentList;
import com.zhaocai.business.agreement.vo.res.AgreementPaymentListVO;

import java.util.List;

/**
 * 合同结算与付款节点信息Service接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface IAgreementPaymentListService  extends IService<AgreementPaymentList> {

    /**
     * 保存合同结算与付款节点信息
     * @param agreementPaymentLists
     * @param agreementId
     */
    void saveAgreementPaymentList(List<AgreementPaymentList> agreementPaymentLists, Long agreementId);

    /**
     * 获取合同结算与付款节点信息
     * @param agreementId
     * @return
     */
    List<AgreementPaymentListVO> listByAgreementId(Long agreementId);

    /**
     * 修改合同结算与付款节点信息
     * @param agreementPaymentLists
     * @param agreementId
     */
    void updateAgreementPaymentList(List<AgreementPaymentList> agreementPaymentLists, Long agreementId);
}
