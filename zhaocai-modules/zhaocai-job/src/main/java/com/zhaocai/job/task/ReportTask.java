package com.zhaocai.job.task;

import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.web.bean.ResultCode;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.system.api.business.RemoteBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ssy
 * @date 2024/12/18
 * @Description 报表任务调度
 */
@Component("reportTask")
public class ReportTask {

    @Autowired
    private RemoteBusinessService remoteBusinessService;

    public void handleBidReport(){
        ResultData<Boolean> res = this.remoteBusinessService.handleBidReport(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("招标率报表任务执行成功");
        }
    }

    public void handleVendorReport(){
        ResultData<Boolean> res = this.remoteBusinessService.handleVendorReport(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("供应商报表任务执行成功");
        }
    }

    public void handleProblemReport(){
        ResultData<Boolean> res = this.remoteBusinessService.handleProblemReport(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("问题报表-异常报表任务执行成功");
        }
    }

    public void handlePurchaseLedgerReport(){
        ResultData<Boolean> res = this.remoteBusinessService.handlePurchaseLedgerReport(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("采购台账报表任务执行成功");
        } else {
            // 抛出异常让调度日志(sys_job_log.status=1)记下失败原因，避免静默失败难排查
            throw new RuntimeException("采购台账报表数据刷新失败：" + res.getMsg());
        }
    }

}
