package com.zhaocai.business.manager.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.dto.PlatDept;
import com.zhaocai.business.manager.http.service.PerformanceEvaluationService;
import com.zhaocai.business.manager.http.service.PlatOrgService;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ssy
 * @date 2024/7/8 16:35
 */
@RestController
@RequestMapping("/platOrg")
@Api(value = "第三方组织机构", tags = "第三方组织机构接口")
public class PlatOrgController extends BladeController {

    @Autowired
    private PlatOrgService platOrgService;


    @Resource
    private PerformanceEvaluationService performanceEvaluationService;

    @ApiOperation(value = "查询全部组织机构列表")
    @GetMapping("/allDepts")
    public ResultData<List<PlatDept>> allDepts() {
        List<PlatDept> depts = platOrgService.allDepts();

        return ResultData.data(depts);
    }


    @ApiOperation(value = "查看最小核算项目数")
    @GetMapping("/selectPrgAmount")
    public ResultData<String> selectPrgAmount() {
        String selectPrgAmount= performanceEvaluationService.selectPrgAmount();
        return ResultData.data(selectPrgAmount);
    }

}
