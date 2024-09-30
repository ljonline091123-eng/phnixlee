package com.zhaocai.common.signature.dto.callback.qysp;

import lombok.Data;


@Data
public class QiYueSuoPrivateCallBackData {
    /**
     * 签名字符串
     */
    private String signature;

    /**
     * 时间戳，精确到毫秒
     */
    private Long timestamp;

    /**
     * 随机字符串
     */
    private String nonce;

    /**
     * 加密后回调内容
     */
    private String encrypted;

}
