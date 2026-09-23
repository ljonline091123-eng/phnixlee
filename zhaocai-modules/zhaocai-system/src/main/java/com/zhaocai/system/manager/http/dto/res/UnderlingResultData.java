package com.zhaocai.system.manager.http.dto.res;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/7/9 11:43
 */
public class UnderlingResultData<T> implements Serializable {
    private static final long serialVersionUID = 3509266335264605291L;

    @ApiModelProperty(value = "状态码", required = true)
    private int code;

    @ApiModelProperty(value = "调用结果", required = true)
    private String message;

    @ApiModelProperty("承载数据")
    private T  data;

    @ApiModelProperty(value = "返回消息", required = true)
    private String msg;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
