package com.zhaocai.common.signature.service.platform;


import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;

/**
 * 电子签章平台服务接口
 *
 * @author chenming
 * @date 2024-08-19
 */
public interface PlatformSignatureService {

    /**
     * 个人认证
     * @param commandRequest
     * @return
     */
    SignatureResponse personAuth(SignatureCommandRequest commandRequest);

    /**
     * 公司认证
     * @param commandRequest
     * @return
     */
    SignatureResponse companyAuth(SignatureCommandRequest commandRequest);

    /**
     * 创建电子签署文档
     * @param request
     */
    SignatureResponse createSignDocument(SignatureCommandRequest request);

    /**
     * 获取签署 url
     * @param commandRequest
     * @return
     */
    SignatureResponse getSignUrl(SignatureCommandRequest commandRequest);

    /**
     * 下载电子签章文档
     * @param commandRequest
     * @return
     */
    SignatureResponse downloadDocument(SignatureCommandRequest commandRequest);

    /**
     * 作废合同
     * @param commandRequest
     * @return
     */
    SignatureResponse cancelAgreement(SignatureCommandRequest commandRequest);
}
