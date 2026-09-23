package com.zhaocai.system.manager.http.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author ssy
 * @date 2024/7/9 11:35
 * @description 底层逻辑平台配置
 */
@Configuration
public class UnderlingPlatformConfig2 {

    /** base地址 */
    @Value("${underPlat.baseUrl}")
    private String baseUrl;

    /** 授权码 */
    @Value("${underPlat.authCode}")
    private String authCode;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAuthCode() {
        return authCode;
    }
}
