package com.zhaocai.flowable.listener;

import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.flowable.mapper.AgreementMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

/**
 * 合同新增审批流监听
 */
public class AgreementAddExecutionListener implements ExecutionListener {

    private AgreementMapper agreementMapper = SpringUtils.getBean(AgreementMapper.class);

    @Override
    public void notify(DelegateExecution delegateExecution) {
        Long agreementId = Long.valueOf(delegateExecution.getVariable("agreementId").toString()) ;
        agreementMapper.updateAgreementStatus(agreementId);
    }

}
