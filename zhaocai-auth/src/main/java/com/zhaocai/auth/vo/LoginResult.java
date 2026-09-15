package com.zhaocai.auth.vo;

import com.zhaocai.system.api.model.LoginUser;

import java.util.List;

/**
 * 统一登录结果
 *
 * 唯一身份时携带 loginUser；双身份账号不签发 token，
 * 携带 needChoose=true 和可选类型列表，由前端弹窗让用户选择。
 */
public class LoginResult {

    /** 唯一身份时的登录用户信息 */
    private LoginUser loginUser;

    /** 是否需要用户选择身份（双身份账号） */
    private boolean needChoose;

    /** 双身份时可选择的用户类型列表（purchase / vendor 等） */
    private List<String> userTypes;

    public LoginResult() {
    }

    public LoginResult(LoginUser loginUser) {
        this.loginUser = loginUser;
    }

    public LoginResult(boolean needChoose, List<String> userTypes) {
        this.needChoose = needChoose;
        this.userTypes = userTypes;
    }

    public LoginUser getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(LoginUser loginUser) {
        this.loginUser = loginUser;
    }

    public boolean isNeedChoose() {
        return needChoose;
    }

    public void setNeedChoose(boolean needChoose) {
        this.needChoose = needChoose;
    }

    public List<String> getUserTypes() {
        return userTypes;
    }

    public void setUserTypes(List<String> userTypes) {
        this.userTypes = userTypes;
    }
}
