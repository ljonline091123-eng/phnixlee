package com.zhaocai.job.task;

import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.web.bean.ResultCode;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.system.api.business.RemoteBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ssy
 * @date 2024/6/1 14:56
 * @Description 招投标任务调度
 */
@Component("biddingTask")
public class BiddingTask {

    @Autowired
    private RemoteBusinessService remoteBusinessService;

    public void biddingTest(String params)
    {
        System.out.println("类调用-参数" + params);
    }

    public void handleNoticeIssueStatus(){
        ResultData<Boolean> res = this.remoteBusinessService.handleNoticeIssueStatus(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("招标业务任务执行成功");
        }
    }

    public void handleNoticePublicityStatus(){
        ResultData<Boolean> res = this.remoteBusinessService.handleNoticePublicityStatus(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("招标业务任务执行成功");
        }
    }


    public void handleProjectReceiptProject(){
        ResultData res = this.remoteBusinessService.receiptProject(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("招标业务任务执行成功");
        }
    }

}
