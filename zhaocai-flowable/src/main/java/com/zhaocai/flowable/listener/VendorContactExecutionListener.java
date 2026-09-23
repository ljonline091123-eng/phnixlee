package com.zhaocai.flowable.listener;

import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.flowable.mapper.VendorMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

public class VendorContactExecutionListener implements ExecutionListener {

    private VendorMapper vendorMapper = SpringUtils.getBean(VendorMapper.class);

    @Override
    public void notify(DelegateExecution delegateExecution) {
        Long vendorContactId = Long.valueOf(delegateExecution.getVariable("vendorContactId").toString()) ;

        vendorMapper.updateVendorContactState(vendorContactId);
    }
}
