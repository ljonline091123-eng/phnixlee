package com.zhaocai.common.signature.service.command;

import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.CancelAgreementCommandRequest;
import com.zhaocai.common.signature.service.AbstractISignatureCommand;

/**
 * 作废签署合同命令
 *
 * @author chenming
 * @date 2024-09-20
 */
public class CancelAgreementCommand  extends AbstractISignatureCommand {

    public CancelAgreementCommand(CancelAgreementCommandRequest commandRequest) {
        this.commandRequest = commandRequest;
    }

    @Override
    protected SignatureResponse doExecute() {
        return getPlatformSignatureService().cancelAgreement(commandRequest);
    }
}
