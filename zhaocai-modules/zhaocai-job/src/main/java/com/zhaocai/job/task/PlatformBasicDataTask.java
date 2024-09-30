package com.zhaocai.job.task;

import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.web.bean.ResultCode;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.system.api.business.RemoteBusinessService;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ssy
 * @date 2024/7/11 14:00
 */
@Component("platformBasicDataTask")
public class PlatformBasicDataTask {

    @Autowired
    private RemoteSystemService remoteSystemService;
    @Autowired
    private RemoteBusinessService remoteBusinessService;

    public void handleSyncDept(){
        ResultData<Boolean> res = remoteSystemService.syncDept(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("定时同步第三方部门数据成功");
        }
    }

    public void handleSyncUser(){
        ResultData<Boolean> res = remoteSystemService.syncUser(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("定时同步第三方用户数据成功");
        }
    }

    public void handleInitUserRole(){
        Integer res = remoteSystemService.initUserRole(SecurityConstants.INNER);
        if (res > 0){
            System.out.println("定时初始化同步的用户角色成功");
        }
    }

    public void structSyncDept(){
        Boolean res = remoteSystemService.structDept(SecurityConstants.INNER);
        if (res){
            System.out.println("定时结构化同步的部门数据成功");
        }
    }

    public void handleSyncAreaDivision(){
        ResultData<Boolean> res = remoteBusinessService.syncAreaDivision(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("定时同步第三方行政区划数据成功");
        }
    }

    public void handleSyncCountry(){
        ResultData<Boolean> res = remoteBusinessService.syncCountry(SecurityConstants.INNER);
        if (res.getCode() == ResultCode.SUCCESS.getCode()){
            System.out.println("定时同步第三方国家和地区档案数据成功");
        }
    }

}
