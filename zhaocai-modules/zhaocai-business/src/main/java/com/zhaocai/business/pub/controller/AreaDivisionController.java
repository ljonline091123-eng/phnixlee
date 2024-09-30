package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.pub.service.IAreaDivisionService;
import com.zhaocai.business.pub.vo.res.AreaDivisionTreeVO;
import com.zhaocai.business.pub.vo.res.AreaDivisionVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 行政区划Controller
 *
 * @author WH
 * @date 2024-07-12
 */
@RestController
@RequestMapping("/division")
@Api(value = "行政区划")
public class AreaDivisionController extends BladeController {

    @Autowired
    private IAreaDivisionService areaDivisionService;

    @GetMapping("/listProvince")
    @ApiOperation(value = "获取省份")
    public ResultData<List<AreaDivisionVO>> listProvince() {
        return ResultData.data(areaDivisionService.listAreaDivisionByParentCode(null));
    }

    @GetMapping("/listCity")
    @ApiOperation(value = "获取市")
    public ResultData<List<AreaDivisionVO>> listCity(@RequestParam String parentCode) {
        return ResultData.data(areaDivisionService.listAreaDivisionByParentCode(parentCode));
    }

    @GetMapping("/listAreaDivisionTree")
    @ApiOperation(value = "获取省市树形结构")
    public ResultData<List<AreaDivisionTreeVO>> listAreaDivisionTree() {
        return ResultData.data(areaDivisionService.listAreaDivisionTree());
    }
}
