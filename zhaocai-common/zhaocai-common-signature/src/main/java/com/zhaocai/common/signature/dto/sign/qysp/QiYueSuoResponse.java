package com.zhaocai.common.signature.dto.sign.qysp;

import lombok.Data;

/**
 * 契约锁响应
 *
 * @author chenming
 * @date 2024-09-20
 */
@Data
public class QiYueSuoResponse {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应信息
     */
    private String message;

}
