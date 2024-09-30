package com.zhaocai.business.manager.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.service.SyncPlatformBasicDataService;
import com.zhaocai.common.core.web.bean.ResultData;
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
@RequestMapping("/syncPlatformBasicData")
@Api(value = "同步平台基础数据", tags = "同步平台基础数据接口")
public class SyncPlatformBasicDataController extends BladeController {

    @Autowired
    private SyncPlatformBasicDataService syncPlatformBasicDataService;

//    @ApiOperation(value = "同步部门")
//    @GetMapping("/syncDept")
//    @Deprecated
//    public ResultData<Boolean> syncDept() {
//        return ResultData.status(syncPlatformBasicDataService.syncDept());
//    }

//    @ApiOperation(value = "同步用户")
//    @GetMapping("/syncUser")
//    @Deprecated
//    public ResultData<Boolean> syncUser() {
//        return ResultData.status(syncPlatformBasicDataService.syncUser());
//    }

    @ApiOperation(value = "同步行政区划")
    @GetMapping("/syncAreaDivision")
    public ResultData<Boolean> syncAreaDivision() {
        return ResultData.status(syncPlatformBasicDataService.syncAreaDivision());
    }

    @ApiOperation(value = "同步国家和地区档案")
    @GetMapping("/syncCountry")
    public ResultData<Boolean> syncCountry() {
        return ResultData.status(syncPlatformBasicDataService.syncCountry());
    }

}
