package com.zhaocai.system.api.factory;

import com.zhaocai.common.core.domain.R;
import com.zhaocai.system.api.domain.BusinessUser;
import com.zhaocai.system.api.domain.SysRole;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.model.LoginUser;
import com.zhaocai.system.api.system.RemoteUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户服务降级处理
 *
 * @author ruoyi
 */
@Component
public class RemoteUserFallbackFactory implements FallbackFactory<RemoteUserService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteUserFallbackFactory.class);

    @Override
    public RemoteUserService create(Throwable throwable)
    {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        return new RemoteUserService()
        {
            @Override
            public R<LoginUser> getUserInfo(String username, String source)
            {
                return R.fail("获取用户失败:" + throwable.getMessage());
            }

            @Override
            public R<SysUser> selectUserInFoById(Long id, String source) {
                return null;
            }

            @Override
            public R<Boolean> registerUserInfo(SysUser sysUser, String source)
            {
                return R.fail("注册用户失败:" + throwable.getMessage());
            }

            @Override
            public R<LoginUser> getUserInfoByUserType(String username, String userType, String source)
            {
                return R.fail("获取用户失败:" + throwable.getMessage());
            }

            @Override
            public R<Long> addBusinessUser(BusinessUser businessUser, String source) {
                return R.fail("注册用户失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysRole>> selectRoleAll(SysRole sysRole, String source) {
                return null;
            }

            @Override
            public R<List<SysUser>> selectUserList(SysUser sysUser, String source) {
                return null;
            }

            @Override
            public R<SysRole> selectRoleById(Long id, String source) {
                return null;
            }

            @Override
            public R<List<SysUser>> selectSysUserAll(SysUser sysUser, String source) {
                return null;
            }

            @Override
            public Boolean deleteSyncUser(String source) {
                return null;
            }

            @Override
            public Boolean addList(List<SysUser> users, String source) {
                return null;
            }

            @Override
            public SysUser getUserInfoById(Long userId, String source) {
                return null;
            }

            @Override
            public SysUser getUserInfoByUsername(String username, String source) {
                return null;
            }

        };
    }
}
