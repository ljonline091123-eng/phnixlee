package com.zhaocai.common.signature.dto.command;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 个人认证授权命令请求
 *
 * @author chenming
 * @date 2024-09-10
 */
@Getter
@Setter
public class PersonAuthCommandRequest extends SignatureCommandRequest {

    protected PersonAuthCommandRequest() {
    }

    /**
     * 用户 id
     */
    private String userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户联系方式
     */
    private String userPhone;
}
