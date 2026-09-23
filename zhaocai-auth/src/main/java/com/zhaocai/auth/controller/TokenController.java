package com.zhaocai.auth.controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.zhaocai.auth.form.LoginBody;
import com.zhaocai.auth.form.RegisterBody;
import com.zhaocai.auth.service.SysLoginService;
import com.zhaocai.auth.vo.LoginResult;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.utils.JwtUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.auth.AuthUtil;
import com.zhaocai.common.security.service.TokenService;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.model.LoginUser;

import java.util.HashMap;
import java.util.Map;

/**
 * token 控制
 * 
 * @author ruoyi
 */
@RestController
public class TokenController
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysLoginService sysLoginService;

    @PostMapping("login")
    public R<?> login(@RequestBody LoginBody form)
    {
        // 统一登录：未指定用户类型时按用户名自动识别身份
        if (StringUtils.isEmpty(form.getUserType()))
        {
            LoginResult result = sysLoginService.autoDetectLogin(form.getUsername(), form.getPassword());
            if (result.isNeedChoose())
            {
                // 双身份：不签发token，返回类型列表由前端弹窗让用户选择
                Map<String, Object> chooseMap = new HashMap<>();
                chooseMap.put("needChoose", true);
                chooseMap.put("userTypes", result.getUserTypes());
                return R.ok(chooseMap);
            }
            Map<String, Object> tokenMap = tokenService.createToken(result.getLoginUser());
            tokenMap.put("user_type", result.getLoginUser().getSysUser().getUserType());
            return R.ok(tokenMap);
        }
        // 指定用户类型登录（原有流程）
        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword(), form.getUserType());
        // 获取登录token
        Map<String, Object> tokenMap = tokenService.createToken(userInfo);
        tokenMap.put("user_type", userInfo.getSysUser().getUserType());
        return R.ok(tokenMap);
    }

    @PostMapping("ssoLogin")
    public R<?> ssoLogin(@RequestBody LoginBody form)
    {
        return R.ok(sysLoginService.ssoLogin(form));
    }

    @DeleteMapping("logout")
    public R<?> logout(HttpServletRequest request)
    {
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            String username = JwtUtils.getUserName(token);
            // 删除用户缓存记录
            AuthUtil.logoutByToken(token);
            // 记录用户退出日志
            sysLoginService.logout(username);
        }
        return R.ok();
    }

    @PostMapping("refresh")
    public R<?> refresh(HttpServletRequest request)
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            // 刷新令牌有效期
            tokenService.refreshToken(loginUser);
            return R.ok();
        }
        return R.ok();
    }

    @PostMapping("register")
    public R<?> register(@RequestBody RegisterBody registerBody)
    {
        // 用户注册
        sysLoginService.register(registerBody.getUsername(), registerBody.getPassword());
        return R.ok();
    }
}
