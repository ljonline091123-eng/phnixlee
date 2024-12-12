package com.zhaocai.auth.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhaocai.auth.config.UnderlingPlatformConfig;
import com.zhaocai.auth.dto.DecryptionTokenResDTO;
import com.zhaocai.auth.form.LoginBody;
import com.zhaocai.common.core.enums.UserTypeEnum;
import com.zhaocai.common.security.service.TokenService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zhaocai.common.core.constant.CacheConstants;
import com.zhaocai.common.core.constant.Constants;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.enums.UserStatus;
import com.zhaocai.common.core.exception.ServiceException;
import com.zhaocai.common.core.text.Convert;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.ip.IpUtils;
import com.zhaocai.common.redis.service.RedisService;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.system.RemoteUserService;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.model.LoginUser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.zhaocai.common.security.service.TokenService.log;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Component
public class SysLoginService
{
    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private SysPasswordService passwordService;

    @Autowired
    private SysRecordLogService recordLogService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private UnderlingPlatformConfig underlingPlatformConfig;

    @Autowired
    private TokenService tokenService;

    /**
     * 登录
     */
    public LoginUser login(String username, String password,String userType)
    {
//        if (StringUtils.isBlank(userType)) {
//            userType = UserTypeEnum.PURCHASE.getName();
//        }
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户/密码必须填写");
            throw new ServiceException("用户/密码必须填写");
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户密码不在指定范围");
            throw new ServiceException("用户密码不在指定范围");
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户名不在指定范围");
            throw new ServiceException("用户名不在指定范围");
        }
        // IP黑名单校验
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "很遗憾，访问IP已被列入系统黑名单");
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }
        // 查询用户信息
        R<LoginUser> userResult = remoteUserService.getUserInfoByUserType(username,userType, SecurityConstants.INNER);

        if (StringUtils.isNull(userResult) || StringUtils.isNull(userResult.getData()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "登录用户不存在");
            throw new ServiceException("登录用户：" + username + " 不存在");
        }

        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        LoginUser userInfo = userResult.getData();
        SysUser user = userResult.getData().getSysUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "对不起，您的账号已被删除");
            throw new ServiceException("对不起，您的账号：" + username + " 已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户已停用，请联系管理员");
            throw new ServiceException("对不起，您的账号：" + username + " 已停用");
        }
        passwordService.validate(user, password);
        recordLogService.recordLogininfor(username, Constants.LOGIN_SUCCESS, "登录成功");
        return userInfo;
    }

    public void logout(String loginName)
    {
        recordLogService.recordLogininfor(loginName, Constants.LOGOUT, "退出成功");
    }

    /**
     * 注册
     */
    public void register(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            throw new ServiceException("用户/密码必须填写");
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            throw new ServiceException("账户长度必须在2到20个字符之间");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }

        // 注册用户信息
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        R<?> registerResult = remoteUserService.registerUserInfo(sysUser, SecurityConstants.INNER);

        if (R.FAIL == registerResult.getCode())
        {
            throw new ServiceException(registerResult.getMsg());
        }
        recordLogService.recordLogininfor(username, Constants.REGISTER, "注册成功");
    }

    public Map<String, Object> ssoLogin(LoginBody form) {

        String token = form.getUserToken();
        if (StrUtil.isBlank(token)) {
            throw new ServiceException("用户token为空");
        }


        Map<String, Object> formMap = new HashMap<>();
        formMap.put("authCode", underlingPlatformConfig.getAuthCode());
        formMap.put("token", token);

        String url = underlingPlatformConfig.getBaseUrl() + "/rest/ctrl/base/decodeToken?authCode=" + underlingPlatformConfig.getAuthCode()
                + "&token=" + form.getUserToken();
        //解密token
        String res = HttpRequest.get(url).execute()
                .body();
        DecryptionTokenResDTO decryptionTokenResDTO = JSON.parseObject(res, DecryptionTokenResDTO.class);
        if (decryptionTokenResDTO.getCode() != 200) {
            throw new ServiceException("单点登录,token解密失败，请检查token是否过期");
        }
        String userType = form.getUserType();
        String username = decryptionTokenResDTO.getUser().getUsername();
        if (StringUtils.isBlank(userType)) {
            userType = UserTypeEnum.PURCHASE.getName();
        }


        // IP黑名单校验
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr())) {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "很遗憾，访问IP已被列入系统黑名单");
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }
        // 查询用户信息
        R<LoginUser> userResult = remoteUserService.getUserInfoByUserType(username, userType, SecurityConstants.INNER);

        if (StringUtils.isNull(userResult) || StringUtils.isNull(userResult.getData())) {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "登录用户不存在");
            throw new ServiceException("登录用户：" + username + " 不存在");
        }

        if (R.FAIL == userResult.getCode()) {
            throw new ServiceException(userResult.getMsg());
        }

        LoginUser userInfo = userResult.getData();
        SysUser user = userResult.getData().getSysUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "对不起，您的账号已被删除");
            throw new ServiceException("对不起，您的账号：" + username + " 已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户已停用，请联系管理员");
            throw new ServiceException("对不起，您的账号：" + username + " 已停用");
        }
        recordLogService.recordLogininfor(username, Constants.LOGIN_SUCCESS, "登录成功");
        Map<String, Object> objectMap = tokenService.createToken(userInfo);
//        String key = CacheConstants.USER_PERMISSION_KEY+user.getUserId();
//        redisService.setCacheSet(key,decryptionTokenResDTO.getPermission());
//        redisService.expire(key,CacheConstants.EXPIRATION, TimeUnit.SECONDS);
//        log.info("digestHex:{},permission:{}",key,JSONObject.toJSONString(decryptionTokenResDTO.getPermission()));
        return objectMap;
    }
}
