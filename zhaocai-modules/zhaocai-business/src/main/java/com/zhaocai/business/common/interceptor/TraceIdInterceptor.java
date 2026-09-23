package com.zhaocai.business.common.interceptor;

import cn.hutool.core.lang.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TraceId 拦截器 <br/>
 * 采用 MDC 机制设置 traceId
 *
 * @author chenming
 * @date 2024-09-07
 */
@Component
public class TraceIdInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        MDC.put("traceId", UUID.fastUUID().toString(true));
        return true;
    }
}
