package com.zhaocai.business.pub.service.impl;

import java.util.List;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.pub.mapper.ProjectWorkMapper;
import com.zhaocai.business.pub.domain.ProjectWork;
import com.zhaocai.business.pub.service.IProjectWorkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 项目明细Service业务层处理
 *
 * @author cff
 * @date 2024-09-26
 */
@Service
public class ProjectWorkServiceImpl extends ServiceImpl<ProjectWorkMapper,ProjectWork> implements IProjectWorkService {

    @Autowired
    private ProjectWorkMapper projectWorkMapper;


}
