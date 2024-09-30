package com.zhaocai.common.signature.dto.command;

import lombok.Getter;
import lombok.Setter;

/**
 * 作废签署合同命令参数
 *
 * @author chenming
 * @date 2024-09-20
 */
@Getter
@Setter
public class CancelAgreementCommandRequest extends SignatureCommandRequest{

    protected CancelAgreementCommandRequest(){

    }

    /**
     * 作废原因
     */
    private String cancelReason;
}
