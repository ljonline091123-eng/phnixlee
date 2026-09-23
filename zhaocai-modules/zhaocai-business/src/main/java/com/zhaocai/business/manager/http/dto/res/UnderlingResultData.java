package com.zhaocai.business.manager.http.dto.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/7/9 11:43
 */
@Getter
@Setter
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

}
