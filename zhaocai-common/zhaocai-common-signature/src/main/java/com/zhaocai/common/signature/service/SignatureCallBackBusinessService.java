package com.zhaocai.common.signature.service;


import com.zhaocai.common.signature.dto.callback.CallBackData;

/**
 * 电子签章回调业务服务
 *
 * @author chenming
 * @date 2024-09-11
 */
public interface SignatureCallBackBusinessService {

    /**
     * 个人授权回调
     * @param callBackData  回调数据
     */
    void personAuthCallBack(CallBackData callBackData);

    /**
     * 公司授权回调
     * @param callBackData  回调数据
     */
    void companyAuthCallBack(CallBackData callBackData);

    /**
     * 电子签章回调
     * @param callBackData  回调数据
     * @param signatureType     签署方类型
     * @param businessId        业务 id
     */
    void signCallBack(CallBackData callBackData, Integer signatureType, Long businessId);
}
