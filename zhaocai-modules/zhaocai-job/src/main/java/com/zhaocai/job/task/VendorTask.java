package com.zhaocai.job.task;

import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.web.bean.ResultCode;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.system.api.business.RemoteBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author xb
 * @date 2025/1/5
 * @Description 供应商任务调度
 */
@Component("vendorTask")
public class VendorTask {

    @Autowired
    private RemoteBusinessService remoteBusinessService;

    public void biddingTest(String params)
    {
        System.out.println("类调用-参数" + params);
    }

    public void removeBlacklistTime(){
        ResultData<Boolean> res = this.remoteBusinessService.removeBlacklist(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("供应商移除黑名单定时任务执行成功");
        }
    }

}
