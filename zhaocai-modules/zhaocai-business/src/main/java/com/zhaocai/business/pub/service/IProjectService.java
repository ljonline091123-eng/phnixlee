package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.pub.domain.Project;

/**
 * 项目Service接口
 *
 * @author cff
 * @date 2024-09-26
 */
public interface IProjectService  extends IService<Project> {
    /**
     * 接收项目
     * @return 结果
     */
    public boolean receiptProject();

}
