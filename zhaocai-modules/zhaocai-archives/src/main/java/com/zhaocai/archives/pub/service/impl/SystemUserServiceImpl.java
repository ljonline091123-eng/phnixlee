package com.zhaocai.archives.pub.service.impl;


import com.zhaocai.archives.pub.service.ISystemUserService;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemUserServiceImpl implements ISystemUserService {

    @Autowired
    private RemoteUserService remoteUserService;


    @Override
    public SysUser getLoginUser() {
        return this.getUserById(SecurityUtils.getUserId());
    }

    @Override
    public SysUser getUserById(Long userId) {
        return remoteUserService.getUserInfoById(userId, SecurityConstants.INNER);
    }
}
