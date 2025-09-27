package com.zhaocai.archives.common.base;

import com.zhaocai.common.core.web.controller.BaseController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * web 基础 Controller
 *
 * @author chenming
 * @date 2024/05/27
 */
public class WebBaseController extends BaseController {

    /**
     * 获取 HttpServletRequest
     * @return
     */
    protected HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return servletRequestAttributes.getRequest();
    }

    /**
     * 获取 HttpServletResponse
     * @return
     */
    protected HttpServletResponse getResponse() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();

        return servletRequestAttributes.getResponse();
    }

    /**
     * 获取 HttpSession
     * @return
     */
    protected HttpSession getSession() {
        return getRequest().getSession();
    }

}
