package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.pub.service.IProjectWorkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目明细Controller
 *
 * @author cff
 * @date 2024-09-26
 */
@RestController
@RequestMapping("/projectWork")
public class ProjectWorkController extends BladeController {
    @Autowired
    private IProjectWorkService projectWorkService;


}
