package com.zhaocai.system.api.system;

import com.zhaocai.system.api.domain.BusinessUser;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.ServiceNameConstants;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.factory.RemoteUserFallbackFactory;
import com.zhaocai.system.api.model.LoginUser;

import java.util.List;

/**
 * 用户服务
 *
 * @author ruoyi
 */
@FeignClient(contextId = "remoteUserService",
        value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteUserFallbackFactory.class)
public interface RemoteUserService
{
    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/user/info/{username}")
    public R<LoginUser> getUserInfo(@PathVariable("username") String username, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/user/{id}")
    public R<SysUser> selectUserInFoById(@PathVariable("id")Long id, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 注册用户信息
     *
     * @param sysUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PostMapping("/user/register")
    public R<Boolean> registerUserInfo(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/user/infoByUserType")
    public R<LoginUser> getUserInfoByUserType(@RequestParam("username") String username,
                                              @RequestParam("userType")  String userType, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @PostMapping("/user/addBusinessUser")
    public R<Long> addBusinessUser(@Validated @RequestBody BusinessUser businessUser,@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @PostMapping("/role/all")
    public R<List<SysRole>> selectRoleAll(@RequestBody SysRole sysRole, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * @param sysUser 用户信息
     * @return 结果
     */
    @PostMapping("/user/list")
    public R<List<SysUser>> selectUserList(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/role/{id}")
    public R<SysRole> selectRoleById(@PathVariable("id")Long id, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @PostMapping("/user/all")
    public R<List<SysUser>> selectSysUserAll(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 删除同步的用户
     */
    @PostMapping("/user/deleteSyncUser")
    Boolean deleteSyncUser(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 获取用户列表
     */
    @PostMapping("/user/addList")
    Boolean addList(@RequestBody List<SysUser> users, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/user/getUserInfoById")
    SysUser getUserInfoById(@RequestParam("userId") Long userId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/user/getUserInfoByUsername")
    SysUser getUserInfoByUsername(@RequestParam("username") String username, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
