package com.zhaocai.common.signature.service.command;


import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.GetSignUrlCommandRequest;
import com.zhaocai.common.signature.service.AbstractISignatureCommand;

/**
 * 获取签署 url 命令
 *
 * @author chenming
 * @date 2024-09-19
 */
public class GetSignUrlCommand extends AbstractISignatureCommand {

    public GetSignUrlCommand(GetSignUrlCommandRequest commandRequest) {
        this.commandRequest = commandRequest;
    }

    @Override
    protected SignatureResponse doExecute() {
        return getPlatformSignatureService().getSignUrl(commandRequest);
    }
}
