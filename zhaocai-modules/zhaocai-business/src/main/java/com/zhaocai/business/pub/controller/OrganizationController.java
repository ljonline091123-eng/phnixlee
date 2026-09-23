package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.pub.service.IOrganizationService;
import com.zhaocai.business.pub.vo.res.OrganizationVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 组织机构
 *
 * @author chenming
 * @date 2024-06-18
 */
@RestController
@RequestMapping("/organization")
public class OrganizationController extends BladeController {

    @Autowired
    private IOrganizationService organizationService;

    /**
     * 获取公司
     */
    @GetMapping("/listOrganization4Company")
    @ApiOperation(value = "获取公司")
    public ResultData<List<OrganizationVO>> listOrganization4Company() {
        return ResultData.data(organizationService.listOrganization4Company());
    }

    /**
     * 范本选择获取公司
     */
    @GetMapping("/listOrganizationCalligraphy")
    @ApiOperation(value = "范本选择获取公司")
    public ResultData<List<OrganizationVO>> listOrganizationCalligraphy() {
        return ResultData.data(organizationService.listOrganizationCalligraphy());
    }
}
