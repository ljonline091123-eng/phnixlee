package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.sign.SignatureResponse;

/**
 * 电子签章私有服务请求
 *
 * @author chenming
 * @date 2024-08-27
 */
public interface QiYueSuoPrivateSignRequestService {

    /**
     * 发送请求
     * @param request
     */
    SignatureResponse sendSignRequest(SignatureRequest request);

}
