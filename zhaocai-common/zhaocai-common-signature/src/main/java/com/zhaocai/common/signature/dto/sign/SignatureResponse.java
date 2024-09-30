package com.zhaocai.common.signature.dto.sign;

import lombok.Data;

/**
 * 电子签章命令 DTO
 *
 * @author chenming
 * @date 2024-08-19
 */
@Data
public class SignatureResponse {

    /**
     * 是否成功
     */
    private Boolean isSuccess;

    /**
     * 描述
     */
    private String message;

    /**
     * 编码
     */
    private String code;

    /**
     * 响应结果
     */
    private Object responseResult;

    public Boolean isSuccess() {
        return this.getIsSuccess();
    }
}
