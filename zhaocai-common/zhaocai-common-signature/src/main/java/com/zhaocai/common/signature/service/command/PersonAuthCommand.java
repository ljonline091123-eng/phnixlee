package com.zhaocai.common.signature.service.command;

import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.PersonAuthCommandRequest;
import com.zhaocai.common.signature.service.AbstractISignatureCommand;


/**
 * 个人认证授权命令
 *
 * @author chenming
 * @date 2024-09-10
 */
public class PersonAuthCommand extends AbstractISignatureCommand {

    public PersonAuthCommand(PersonAuthCommandRequest commandRequest) {
        this.commandRequest = commandRequest;
    }

    @Override
    protected SignatureResponse doExecute() {
        return getPlatformSignatureService().personAuth(commandRequest);
    }
}
