package com.zhaocai.business.pub.service;


import com.zhaocai.system.api.domain.SysUser;

/**
 * 系统用户服务
 *
 * @author chenming
 * @date 2024-08-13
 */
public interface ISystemUserService {

    /**
     * 获取登录用户
     * @return
     */
    SysUser getLoginUser();

    /**
     * 根据用户 id 获取用户数据
     * @param userId
     * @return
     */
    SysUser getUserById(Long userId);
}
