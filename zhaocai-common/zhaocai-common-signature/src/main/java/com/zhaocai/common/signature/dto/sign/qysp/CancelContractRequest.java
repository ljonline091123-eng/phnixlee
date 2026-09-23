package com.zhaocai.common.signature.dto.sign.qysp;

import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateRequestTypeEnum;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 取消签署合同请求
 *
 * @author chenming
 * @date 2024-09-20
 */
@Getter
@Setter
public class CancelContractRequest extends SignatureRequest {

    public CancelContractRequest(SignatureCommandRequest request) {
        super(request);
        setRequestType(QiYueSuoPrivateRequestTypeEnum.COMPANY_AUTH);
    }

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 作废原因
     */
    private String reason;
}
