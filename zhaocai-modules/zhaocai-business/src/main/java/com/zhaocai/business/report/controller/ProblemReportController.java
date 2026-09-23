package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IProblemReportService;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/problemReport")
@Api(value = "问题报表-异常报表")
public class ProblemReportController extends BladeController {

    @Autowired
    private IProblemReportService problemReportService;

    /**
     * 定时刷新问题报表-异常报表数据
     * 定时任务job
     */
    @ApiOperation("定时刷新问题报表-异常报表数据")
    @GetMapping("/handleProblemReport")
    public ResultData<Boolean> handleProblemReport(){
        return ResultData.status(problemReportService.handleProblemReport());
    }
}
