package com.zhaocai.common.signature.configure;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.qiyuesuo.v3sdk.SdkClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 契约锁本地化
 *
 * @author chenming
 * @date 2024-08-27
 */
@Slf4j
@Data
@Component
@Configuration
@ConditionalOnProperty(name = "signature.platform",havingValue = "qiyuesuo-private")
@ConfigurationProperties(prefix = "signature.qiyuesuo.private")
public class QiYueSuoPrivateConfiguration {

    /**
     * 开放平台请求地址
     */
    private String serviceUrl;

    /**
     * 开放平台申请的App Token
     */
    private String accessToken;

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
        log.info("开始初始化契约锁配置,serviceUrl:{};;accessToken:{};;accessSecret:{}",serviceUrl,accessToken,accessSecret);
        this.sdkClient = new SdkClient(serviceUrl, accessToken, accessSecret);
        log.info("完成契约锁配置初始化...");
    }

    @Bean
    public SdkClient getClient() {
        return this.sdkClient;
    }
}
