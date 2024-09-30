package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.sign.qysp.CancelContractRequest;
import net.qiyuesuo.v3sdk.model.contract.request.ContractInitiateCancelRequest;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import org.springframework.stereotype.Service;

/**
 * 作废合同服务
 *
 * @author chenming
 * @date 2024-09-20
 */
@Service
public class CancelContractService extends AbstractQiYueSuoPrivateRequestDefaultService{

    @Override
    protected SdkRequest builderSignRequest(SignatureRequest signatureRequest) {
        CancelContractRequest contractRequest = (CancelContractRequest) signatureRequest;
        ContractInitiateCancelRequest request = new ContractInitiateCancelRequest();
        request.setContractId(contractRequest.getContractId());
        request.setReason(contractRequest.getReason());

        return request;
    }
}
