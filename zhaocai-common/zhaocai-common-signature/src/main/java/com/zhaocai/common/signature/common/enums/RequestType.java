package com.zhaocai.common.signature.common.enums;


/**
 * 请求类型接口
 * @author chenming
 * @date 2024-08-20
 */
public interface RequestType {

    /**
     * 获取业务编码
     * @return
     */
    String getRequestCode();

    /**
     * 获取请求名称
     * @return
     */
    String getRequestName();

    /**
     * 获取请求 url
     * @return
     */
    String getRequestUrl();

    /**
     * 获取响应类型
     * @return
     */
    Class<?> getResultClass();

    /**
     * 获取平台
     * @return
     */
    String getPlatform();
}
