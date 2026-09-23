package com.zhaocai.system.manager.controller;

import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.system.manager.http.service.SyncPlatformDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ssy
 * @date 2024/7/9 17:32
 */
@RestController
@RequestMapping("/syncPlatformData")
@Api(value = "同步平台数据", tags = "同步平台数据接口")
public class SyncPlatformDataController extends BaseController {

    @Autowired
    private SyncPlatformDataService syncPlatformDataService;

    @ApiOperation(value = "同步部门")
    @GetMapping("/syncDept")
    public ResultData<Boolean> syncDept() {
        return ResultData.status(syncPlatformDataService.syncDept());
    }

    @ApiOperation(value = "同步用户")
    @GetMapping("/syncUser")
    public ResultData<Boolean> syncUser() {
        return ResultData.status(syncPlatformDataService.syncUser());
    }

}
