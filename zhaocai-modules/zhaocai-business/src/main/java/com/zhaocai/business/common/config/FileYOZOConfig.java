package com.zhaocai.business.common.config;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

//@Component
//@ConfigurationProperties(prefix = "file-yozo")

@Configuration
@Slf4j
@Getter
public class FileYOZOConfig {
    //服务部署地址
    @Value("${file-yozo.server-url}")
    private String serverUrl;

    //分配的应用代码
    @Value("${file-yozo.appCode}")
    private String appCode;

    //分配的应用秘钥，请不要通过参数传递
    @Value("${file-yozo.appSecret}")
    private String appSecret;

    //回调地址
    @Value("${file-yozo.callback-url}")
    private String callbackUrl;

    //临时文件存储路径
    @Value("${file-yozo.tempFilePath}")
    private String tempFilePath;

    //原responseURL的前缀（服务器地址）
    @Value("${file-yozo.oldResponsePrefix}")
    private String oldResponsePrefix;

    //新responseURL的前缀（服务器地址）
    @Value("${file-yozo.newResponsePrefix}")
    private String newResponsePrefix;


}
