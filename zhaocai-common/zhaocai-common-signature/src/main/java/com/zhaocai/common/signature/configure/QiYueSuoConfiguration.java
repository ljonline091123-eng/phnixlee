package com.zhaocai.common.signature.configure;


import com.qiyuesuo.sdk.v2.SdkClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 契约锁配置
 *
 * @author chenming
 * @date 2024-08-20
 */
@Slf4j
@Data
@Component
@Configuration
@ConditionalOnProperty(name = "signature.platform",havingValue = "qiyuesuo")
@ConfigurationProperties(prefix = "signature.qiyuesuo")
public class QiYueSuoConfiguration {

    /**
     * 开放平台请求地址
     */
    private String url;

    /**
     * 开放平台申请的App Token
     */
    private String accessKey;

    /**
     * 开放平台申请的App Secret
     */
    private String accessSecret;

    /**
     * 契约锁 sdk
     */
    private SdkClient sdkClient;

    @PostConstruct
    public void init() {
        log.info("开始初始化契约锁配置,url:{};;accessKey:{};;accessSecret:{}",url,accessKey,accessSecret);
        this.sdkClient = new SdkClient(url, accessKey, accessSecret);
        log.info("完成契约锁配置初始化...");
    }


    @Bean
    public SdkClient getClient() {
        return this.sdkClient;
    }
}
