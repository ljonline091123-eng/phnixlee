package com.zhaocai.job.task;

import com.zhaocai.system.api.business.RemoteDwMmInfoSynchronizeDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 资产(DM071)、服务(DM073)数据同步任务
 *
 * @author chenming
 * @date 2024-08-31
 */
@Slf4j
@Component("dwMmInfoSynchronizeDataTask")
public class DwMmInfoSynchronizeDataTask {

    @Autowired
    private RemoteDwMmInfoSynchronizeDataService remoteDwMmInfoSynchronizeDataService;

    /**
     * 同步 DM071 数据
     */
    public void synchronizeDm071Data(){
        log.info("开始执行同步 DM071 数据定时任务...");
        remoteDwMmInfoSynchronizeDataService.synchronizeDm071Data();
        log.info("执行同步 DM071 数据定时任务完成...");
    }

    /**
     * 同步 DM073 数据
     */
    public void synchronizeDm073Data(){
        log.info("开始执行同步 DM073 数据定时任务...");
        remoteDwMmInfoSynchronizeDataService.synchronizeDm073Data();
        log.info("执行同步 DM073 数据定时任务完成...");
    }
}
