package com.zhaocai.flowable.listener;

import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.flowable.mapper.VendorMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

public class VendorRegisterExecutionListener  implements ExecutionListener {

    private VendorMapper vendorMapper = SpringUtils.getBean(VendorMapper.class);

    @Override
    public void notify(DelegateExecution delegateExecution) {
        Long vendorId = Long.valueOf(delegateExecution.getVariable("vendorId").toString()) ;

        vendorMapper.updateVendorState(vendorId);
    }
}
