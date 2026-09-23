package com.zhaocai.common.core.web.bean.thrid;

import com.zhaocai.common.core.web.bean.IResultCode;

/**
 * @author ssy
 * @date 2024/5/24 17:13
 */
public enum ThridResultCode implements IResultCode {
    //返回结果枚举类

    SUCCESS(200, "操作成功"),
    UN_AUTHORIZED(401, "请求未授权"),
    ACCESS_FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "没找到请求");

    final int code;
    final String message;

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    private ThridResultCode(final int code, final String message) {
        this.code = code;
        this.message = message;
    }
}
