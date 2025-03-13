package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IDeviceTypeService;
import com.zhaocai.archives.dossier.tree.DeviceTypeTree;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.mapper.DeviceClassMapper;
import com.zhaocai.archives.main.service.IArchivesService;
import com.zhaocai.archives.main.service.IDeviceClassService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.PageUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 设备分类主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class archivesServiceImpl  implements IArchivesService {
    @Autowired
    private DeviceClassMapper deviceClassMapper;

    @Resource
    private IDeviceTypeService iDeviceTypeService;


}
