package com.zhaocai.business.contract.service;

import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.common.signature.dto.callback.qysp.SignCallBackData;

public interface IContractCompatibilityService {
    void syncContract(Agreement agreement);

    void syncSignCallback(Long legacyAgreementId, SignCallBackData callback);
}
