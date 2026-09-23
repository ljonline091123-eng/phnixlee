package com.zhaocai.archives.task.controller;

import com.zhaocai.archives.task.service.ArchivesTaskService;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.controller.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 材料分类Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/archivesTask")
public class ArchivesTaskController extends BaseController {

    @Resource
    private ArchivesTaskService archivesTaskService;

    @PostMapping("/synchronizeMasterData")
    public ResultData synchronizeMasterData() {
        return ResultData.data(archivesTaskService.synchronizeMasterData());
    }



    @PostMapping("/pushMiddlePlatform")
    public ResultData pushMiddlePlatform() {
        return ResultData.data(archivesTaskService.pushMiddlePlatform());
    }


}
