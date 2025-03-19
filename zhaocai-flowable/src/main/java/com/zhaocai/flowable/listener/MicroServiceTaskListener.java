package com.zhaocai.flowable.listener;

import org.flowable.engine.impl.el.FixedValue;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Resource(name = "microServiceTaskListener")
public class MicroServiceTaskListener implements TaskListener {
    private FixedValue notifyUrl;
    private FixedValue targetType;
    private FixedValue actionType;
    private FixedValue flowMode;
    private FixedValue callbackMode;


    @Override
    public void notify(DelegateTask delegateTask) {
        System.out.println("(♥◠‿◠)ﾉﾞ  收到流程任务处理监听通知  ლ(´ڡ`ლ)ﾞ  \n");
        if (this.flowMode != null && "approval".equals(this.flowMode.getExpressionText())) {
            Map<String, FixedValue> variables = new HashMap();
            variables.put("notifyUrl", this.notifyUrl);
            variables.put("targetType", this.targetType);
            variables.put("actionType", this.actionType);
            variables.put("flowMode", this.flowMode);
            variables.put("callbackMode", this.callbackMode);
        }

    }
}
