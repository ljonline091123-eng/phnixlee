package com.zhaocai.flowable.listener;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

public class TestBusiness implements ExecutionListener {
    @Override
    public void notify(DelegateExecution delegateExecution) {
        delegateExecution.getTransientVariables();
        System.out.println("11111");
}
}
