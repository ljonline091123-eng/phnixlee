package com.zhaocai.common.signature.service.command;

import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.DownloadDocumentCommandRequest;
import com.zhaocai.common.signature.service.AbstractISignatureCommand;


/**
 * 下载签署文档命令
 *
 * @author chenming
 * @date 2024-09-20
 */
public class DownloadDocumentCommand extends AbstractISignatureCommand {

    public DownloadDocumentCommand(DownloadDocumentCommandRequest commandRequest) {
        this.commandRequest = commandRequest;
    }

    @Override
    protected SignatureResponse doExecute() {
        return getPlatformSignatureService().downloadDocument(commandRequest);
    }
}
