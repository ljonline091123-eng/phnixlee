package com.zhaocai.business.common.config;

import com.zhaocai.business.common.interceptor.RequestParamLoggingInterceptor;
import com.zhaocai.business.common.interceptor.TraceIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 业务配置
 *
 * @author chenming
 * @date 2024-09-07
 */
@Configuration
public class BusinessWebConfig implements WebMvcConfigurer {

    @Autowired
    private RequestParamLoggingInterceptor requestParamLoggingInterceptor;

    @Autowired
    private TraceIdInterceptor traceIdInterceptor;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**");

//        registry.addInterceptor(requestParamLoggingInterceptor)
//                .addPathPatterns("/**");
    }
}
