package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IWorkbenchService;
import com.zhaocai.common.core.web.domain.AjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台统计
 *
 * 一个接口返回工作台全部统计数据（状态卡/采购方式分析/招采概览/TOP5供应商），
 * 待办列表由前端直接调用已有 flowable 待办接口，不在此重复实现。
 * 数据权限由 Service 内 ReportScopeUtil 强制收敛（与各报表一致）。
 *
 * @author claude
 */
@RestController
@RequestMapping("/report")
@Api(value = "工作台统计")
public class WorkbenchController extends BladeController {

    @Autowired
    private IWorkbenchService workbenchService;

    /**
     * 工作台统计
     *
     * @param year        年份(招采概览筛选用，空=全部年份)
     * @param orgId       组织id(sys_dept.thrid_dept_id，招采概览筛选用，空=全部单位)
     * @param methodYear  年份(采购方式分析筛选用，空=全部年份)
     * @param methodOrgId 组织id(sys_dept.thrid_dept_id，采购方式分析筛选用，空=全部单位)
     */
    @ApiOperation("工作台统计数据")
    @GetMapping("/workbench")
    public AjaxResult workbench(Integer year, String orgId, Integer methodYear, String methodOrgId) {
        return AjaxResult.success(workbenchService.getWorkbench(year, orgId, methodYear, methodOrgId));
    }
}
