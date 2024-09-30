package com.zhaocai.flowable.listener;

import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.flowable.mapper.ProcumentMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

public class ProcurementPlanExecutionListener implements ExecutionListener {

    private ProcumentMapper procumentMapper = SpringUtils.getBean(ProcumentMapper.class);

    @Override
    public void notify(DelegateExecution delegateExecution) {
        Long planId = Long.valueOf(delegateExecution.getVariable("planId").toString()) ;

        procumentMapper.updateProcurementPlanStatus(planId);
    }
}
