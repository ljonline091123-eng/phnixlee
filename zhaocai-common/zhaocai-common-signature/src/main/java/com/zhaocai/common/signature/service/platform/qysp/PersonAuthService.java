package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.sign.qysp.PersonAuthRequest;
import net.qiyuesuo.v3sdk.model.auth.request.UserauthAuthurl2Request;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import org.springframework.stereotype.Service;


/**
 * 个人身份验证服务
 *
 * @author chenming
 * @date 2024-09-10
 */
@Service
public class PersonAuthService extends AbstractQiYueSuoPrivateRequestDefaultService{

    @Override
    protected SdkRequest builderSignRequest(SignatureRequest signatureRequest) {
        PersonAuthRequest authRequest = (PersonAuthRequest) signatureRequest;

        UserauthAuthurl2Request request = new UserauthAuthurl2Request();
        request.setBizNo(authRequest.getBizId());

        request.setOpenUserId(authRequest.getUserId());
        request.setMobile(authRequest.getUserPhone());
        request.setName(authRequest.getUserName());

        return request;
    }
}
