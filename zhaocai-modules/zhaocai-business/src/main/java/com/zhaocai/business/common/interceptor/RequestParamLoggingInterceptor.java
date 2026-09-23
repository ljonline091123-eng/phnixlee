package com.zhaocai.business.common.interceptor;

import com.zhaocai.common.core.utils.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求参数日志拦截器
 *
 * @author chenming
 * @date 2024-09-07
 */
@Slf4j
@Component
public class RequestParamLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try{
            doLogRequestParam(request);
        } catch (Exception e) {
            log.error("RequestParamLoggingInterceptor - 打印日志失败,cause by :{}",e.getMessage(),e);
        }

        return true;
    }

    /**
     * 打印请求参数
     * @param request
     */
    private void doLogRequestParam(HttpServletRequest request) throws IOException {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Request URI: ").append(request.getRequestURI())
                .append(", Request Method: ").append(request.getMethod());

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            Enumeration<String> parameterNames = request.getParameterNames();
            if (parameterNames.hasMoreElements()) {
                Map<String, String> parameters = new HashMap<>();
                while (parameterNames.hasMoreElements()) {
                    String paramName = parameterNames.nextElement();
                    String paramValue = request.getParameter(paramName);
                    parameters.put(paramName, paramValue);
                }
                logMessage.append(", Parameters: ").append(JacksonUtil.toJsonString(parameters));
            }
        } else if ("POST".equalsIgnoreCase(request.getMethod())) {
            if (request.getContentType().startsWith("application/json") || request.getContentType().startsWith("APPLICATION/JSON")) {
                BodyReaderHttpServletRequestWrapper wrappedRequest = new BodyReaderHttpServletRequestWrapper(request);

                logMessage.append("Body: ").append(wrappedRequest.getBody());
            } else {
                logMessage.append("Body: ").append("非 json 数据，不记录请求参数");
            }
        } else {
            logMessage.append("Body: ").append("非 GET、POST 请求，不记录请求参数");
        }

        log.info(logMessage.toString());
    }
}
