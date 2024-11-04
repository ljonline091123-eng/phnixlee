package com.zhaocai.business.expert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.expert.domain.ExpertChange;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;

/**
 * 专家修改Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IExpertChangeService extends IService<ExpertChange> , IProcessBusinessBaseService, IPBMOverrideService {



}
