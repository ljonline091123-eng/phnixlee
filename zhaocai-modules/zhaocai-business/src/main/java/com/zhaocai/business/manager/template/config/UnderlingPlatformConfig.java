package com.zhaocai.business.manager.template.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author ssy
 * @date 2024/7/9 11:35
 * @description 底层逻辑平台配置
 */
@Configuration
@Slf4j
@Getter
public class UnderlingPlatformConfig {

    /** base地址 */
    @Value("${underPlat.baseUrl}")
    private String baseUrl;

    /** 授权码 */
    @Value("${underPlat.authCode}")
    private String authCode;

    /** yjt地址 */
    @Value("${underPlat.yjtUrl}")
    private String yjtUrl;

    /** yjtappID */
    @Value("${underPlat.appId}")
    private String appId;

    /** yjtKey */
    @Value("${underPlat.yjtKey}")
    private String yjtKey;
}
