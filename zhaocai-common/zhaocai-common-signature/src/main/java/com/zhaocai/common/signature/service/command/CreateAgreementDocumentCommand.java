package com.zhaocai.common.signature.service.command;

import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.CreateAgreementDocumentCommandRequest;
import com.zhaocai.common.signature.service.AbstractISignatureCommand;

/**
 * 创建合同文档命令
 *
 * @author chenming
 * @date 2024-09-13
 */
public class CreateAgreementDocumentCommand extends AbstractISignatureCommand {

    public CreateAgreementDocumentCommand(CreateAgreementDocumentCommandRequest commandRequest) {
        this.commandRequest = commandRequest;
    }

    @Override
    protected SignatureResponse doExecute() {
        return getPlatformSignatureService().createSignDocument(commandRequest);
    }
}
