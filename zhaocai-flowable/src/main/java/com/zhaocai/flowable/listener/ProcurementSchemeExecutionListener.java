package com.zhaocai.flowable.listener;

import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.flowable.mapper.ProcurementSchemeMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

/**
 * 采购方案审批流监听
 */
public class ProcurementSchemeExecutionListener implements ExecutionListener {

    private ProcurementSchemeMapper procurementSchemeMapper = SpringUtils.getBean(ProcurementSchemeMapper.class);

    @Override
    public void notify(DelegateExecution delegateExecution) {
        String procurementSchemeCode = delegateExecution.getVariable("procurementSchemeCode").toString();
        procurementSchemeMapper.updateProcurementSchemeState(procurementSchemeCode);
    }

}
