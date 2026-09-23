package com.zhaocai.business.procurement.controller;

import com.zhaocai.business.procurement.service.IProjectCertificationTypeService;
import com.zhaocai.business.procurement.vo.res.ProjectCertificationTypeVO;
import com.zhaocai.business.vendor.service.IVendorClassifyService;
import com.zhaocai.business.vendor.vo.res.VendorClassifyTreeVO;
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
@Api("项目-资质分类")
@RestController
@RequestMapping("/ProjectCertificationType")
public class ProjectCertificationTypeController {

    @Autowired
    private IProjectCertificationTypeService projectCertificationTypeService;

    /**
     * 获取供应商分类树
     */
    @GetMapping("/getProjectCertificationTypeTree")
    @ApiOperation("获取资质分类树")
    public ResultData<List<ProjectCertificationTypeVO>> getProjectCertificationTypeTree() {
        return ResultData.data(projectCertificationTypeService.getProjectCertificationTypeTree());
    }

}
