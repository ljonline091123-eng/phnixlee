package com.zhaocai.business.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 环境配置工具类
 *
 * @author chenming
 * @date 2024-07-05
 */
@Component
public class EnvironmentUtil {
    private static Environment environment;

    @Autowired
    public EnvironmentUtil(Environment environment) {
        EnvironmentUtil.environment = environment;
    }

    /**
     * 获取 key 的值
     * @param key
     * @return
     */
    public static String getProperty(String key) {
        return environment.getProperty(key);
    }

    /**
     * 获取 key 的值
     * @param key
     * @return
     */
    public static String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }
}
