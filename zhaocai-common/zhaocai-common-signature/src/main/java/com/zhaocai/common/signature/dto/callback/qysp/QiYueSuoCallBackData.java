package com.zhaocai.common.signature.dto.callback.qysp;

import com.zhaocai.common.signature.dto.callback.CallBackData;
import lombok.Data;

/**
 * 契约锁回调基础参数
 *
 * @author chenming
 * @date 2024-09-11
 */
@Data
public class QiYueSuoCallBackData extends CallBackData {

    /**
     * 回调事件类型
     */
    private String callbackType;

    /**
     * 回调时间戳
     */
    private String callbackTime;

    /**
     * 回调业务类型
     */
    private String callbackBizType;
}
