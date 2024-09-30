package com.zhaocai.system.vo;

import lombok.Data;

@Data
public class SmsPasswordRetrievalVo {
    /**
     * 手机号
     */
   private String phoneNumber;
    /**
     * 短信验证码
     */
   private String smsCode;
    /**
     * 发送短信唯一标识（密码找回-发送短信验证码返回的数据）
     */
    private   String uuid;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String confirmPassword;
}
