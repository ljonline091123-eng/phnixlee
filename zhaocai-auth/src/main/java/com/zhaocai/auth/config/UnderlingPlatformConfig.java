package com.zhaocai.auth.config;

import jdk.nashorn.internal.objects.annotations.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnderlingPlatformConfig {

    /** base地址 */
    @Value("${underPlat.baseUrl}")
    private String baseUrl;

    /** 授权码 */
    @Value("${underPlat.authCode}")
    private String authCode;

    public String getAuthCode() {
        return authCode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}