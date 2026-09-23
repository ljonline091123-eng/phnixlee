package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IVendorReportService;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vendorReport")
@Api(value = "供应商报表")
public class VendorReportController extends BladeController {

    @Autowired
    private IVendorReportService vendorReportService;

    /**
     * 定时刷新供应商报表数据
     * 定时任务job
     */
    @ApiOperation("定时刷新供应商报表数据")
    @GetMapping("/handleVendorReport")
    public ResultData<Boolean> handleVendorReport(){
        return ResultData.status(vendorReportService.handleVendorReport());
    }
}
