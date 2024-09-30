package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.business.pub.service.IProjectService;
import com.zhaocai.common.log.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhaocai.common.log.annotation.Log;
/**
 * 项目Controller
 *
 * @author cff
 * @date 2024-09-26
 */
@RestController
@RequestMapping("/project")
public class ProjectController extends BladeController {
    @Autowired
    private IProjectService projectService;

    /**
     * 接收项目
     */
    @GetMapping("/receiptProject")
    @Log(title = "接收项目", businessType = BusinessType.INSERT)
    public ResultData receiptProject() {
        return ResultData.data(projectService.receiptProject());
    }
}
