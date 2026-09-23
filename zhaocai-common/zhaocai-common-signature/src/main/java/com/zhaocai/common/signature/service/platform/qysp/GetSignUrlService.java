package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.sign.qysp.GetSignUrlRequest;
import net.qiyuesuo.v3sdk.model.contract.request.ContractSignurlV3Request;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import org.springframework.stereotype.Service;

/**
 * 获取签署 url
 *
 * @author chenming
 * @date 2024-09-19
 */
@Service
public class GetSignUrlService extends AbstractQiYueSuoPrivateRequestDefaultService{
    @Override
    protected SdkRequest builderSignRequest(SignatureRequest signatureRequest) {
        GetSignUrlRequest request = (GetSignUrlRequest) signatureRequest;

        ContractSignurlV3Request signUrlV3Request = new ContractSignurlV3Request();
        signUrlV3Request.setContractId(request.getContractId());
        signUrlV3Request.setTenantType("COMPANY");
        signUrlV3Request.setTenantName(request.getTenantName());
        signUrlV3Request.setReceiverName(request.getReceiverName());
        signUrlV3Request.setContact(request.getContact());


        return signUrlV3Request;
    }
}
