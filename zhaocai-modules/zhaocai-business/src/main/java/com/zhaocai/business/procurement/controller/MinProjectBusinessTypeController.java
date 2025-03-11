package com.zhaocai.business.procurement.controller;

import com.zhaocai.business.procurement.service.IMinProjectBusinessTypeService;
import com.zhaocai.business.procurement.service.IProjectCertificationTypeService;
import com.zhaocai.business.procurement.vo.res.MinProjectBusinessTypeVO;
import com.zhaocai.business.procurement.vo.res.ProjectCertificationTypeVO;
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
@Api("项目-业务分类")
@RestController
@RequestMapping("/MinProjectBusinessType")
public class MinProjectBusinessTypeController {

    @Autowired
    private IMinProjectBusinessTypeService minProjectBusinessTypeService;

    /**
     * 获取供应商分类树
     */
    @GetMapping("/getMinProjectBusinessTypeTree")
    @ApiOperation("获取业务分类分类树")
    public ResultData<List<MinProjectBusinessTypeVO>> getMinProjectBusinessTypeTree() {
        return ResultData.data(minProjectBusinessTypeService.getVendorClassifyTree());
    }

}
