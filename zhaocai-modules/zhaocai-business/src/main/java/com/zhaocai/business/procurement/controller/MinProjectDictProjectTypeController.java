package com.zhaocai.business.procurement.controller;

import com.zhaocai.business.procurement.service.IMinProjectDictProjectTypeService;
import com.zhaocai.business.procurement.vo.res.MinProjectDictProjectTypeVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *项目管理-资质类别
 *  @author xwj
 */
@Api("项目-工程分类")
@RestController
@RequestMapping("/MinProjectDictProjectType")
public class MinProjectDictProjectTypeController {

    @Autowired
    private IMinProjectDictProjectTypeService MinProjectDictProjectTypeService;

    /**
     * 获取供应商分类树
     */
    @GetMapping("/getMinProjectDictProjectTypeTree")
    @ApiOperation("获取工程分类分类树")
    public ResultData<List<MinProjectDictProjectTypeVO>> getMinProjectDictProjectTypeTree() {
        return ResultData.data(MinProjectDictProjectTypeService.getMinProjectDictProjectTypeTree());
    }

}
