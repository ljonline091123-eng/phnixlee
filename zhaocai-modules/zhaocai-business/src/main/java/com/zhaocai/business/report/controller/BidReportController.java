package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IBidReportService;
import com.zhaocai.business.report.service.IContractBaseService;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidReport")
@Api(value = "招标率报表")
public class BidReportController extends BladeController {

    @Autowired
    private IBidReportService bidReportService;

    /**
     * 定时刷新招标率报表数据
     * 定时任务job
     */
    @ApiOperation("定时刷新招标率报表数据")
    @GetMapping("/handleBidReport")
    public ResultData<Boolean> handleBidReport(){
        return ResultData.status(bidReportService.handleBidReport());
    }
}
