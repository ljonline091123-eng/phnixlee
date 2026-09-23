package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementDeposit;
import com.zhaocai.business.agreement.vo.res.AgreementDepositVO;

import java.util.List;

/**
 * 合同保证金Service接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface IAgreementDepositService  extends IService<AgreementDeposit> {

    /**
     * 保存合同保证金
     * @param agreementDeposits
     * @param agreementId
     */
    void saveAgreementDeposit(List<AgreementDeposit> agreementDeposits, Long agreementId);

    /**
     * 合同保证金
     * @param agreementId
     * @return
     */
    List<AgreementDepositVO> listByAgreementId(Long agreementId);

    /**
     * 修改合同保证金
     * @param agreementDeposits
     * @param agreementId
     */
    void updateAgreementDeposit(List<AgreementDeposit> agreementDeposits, Long agreementId);
}
