package com.zhaocai.business.vendor.config;

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
public class DataMiddlePlatformConfig {

    /** 中台地址 */
    @Value("${dataMiddlePlatform.dataUrl}")
    private String dataUrl;

    /** 中台访问的id*/
    @Value("${dataMiddlePlatform.tydtcAppId}")
    private String tydtcAppId;

    /** 中台访问的key */
    @Value("${dataMiddlePlatform.tydtcAppKey}")
    private String tydtcAppKey;

    /** 中台访问的secret */
    @Value("${dataMiddlePlatform.tydtcAppSecret}")
    private String tydtcAppSecret;

    @Value("${dataMiddlePlatform.vendorAdd}")
    private String vendorAdd;

    @Value("${dataMiddlePlatform.vendorUpdate}")
    private String vendorUpdate;

    @Value("${dataMiddlePlatform.vendorAcctAdd}")
    private String vendorAcctAdd;

    @Value("${dataMiddlePlatform.vendorAcctUpdate}")
    private String vendorAcctUpdate;

    @Value("${dataMiddlePlatform.vendorAcctRemove}")
    private String vendorAcctRemove;

    @Value("${dataMiddlePlatform.vendorAcctEnable}")
    private String vendorAcctEnable;

    @Value("${dataMiddlePlatform.vendorAcctDisable}")
    private String vendorAcctDisable;

}
