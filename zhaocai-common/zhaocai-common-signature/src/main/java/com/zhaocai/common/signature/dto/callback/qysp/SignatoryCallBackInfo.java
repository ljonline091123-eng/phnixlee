package com.zhaocai.common.signature.dto.callback.qysp;

import lombok.Data;


/**
 * 签署方回调信息
 *
 * @author chenming
 * @date 2024-09-19
 */
@Data
public class SignatoryCallBackInfo {

    /**
     * 主题签约名称
     */
    private String tenantName;

    /**
     * 接收方名称
     */
    private String receiverName;

    /**
     * 接收方联系方式
     */
    private String receiverContact;
}
