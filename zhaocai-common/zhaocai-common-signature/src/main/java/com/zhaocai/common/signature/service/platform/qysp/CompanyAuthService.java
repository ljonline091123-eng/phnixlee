package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.sign.qysp.CompanyAuthRequest;
import net.qiyuesuo.v3sdk.model.company.request.CompanyauthH5pageRequest;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import org.springframework.stereotype.Service;

/**
 * 获取法人认证页面
 *
 * @author chenming
 * @date 2024-09-09
 */
@Service
public class CompanyAuthService extends AbstractQiYueSuoPrivateRequestDefaultService {

    @Override
    protected SdkRequest builderSignRequest(SignatureRequest signatureRequest) {
        CompanyAuthRequest authRequest = (CompanyAuthRequest) signatureRequest;

        CompanyauthH5pageRequest request = new CompanyauthH5pageRequest();
        request.setOpenCompanyId(authRequest.getCompanyId().toString());
        request.setName(authRequest.getCompanyName());
        request.setRegisterNo(authRequest.getRegisterNo());
        request.setLegalPerson(authRequest.getLegalPersonName());
        request.setCharger(authRequest.getChargerName());
        request.setMobile(authRequest.getChargerMobile());
        return request;
    }
}
