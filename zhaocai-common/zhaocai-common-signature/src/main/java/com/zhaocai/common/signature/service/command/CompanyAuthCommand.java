package com.zhaocai.common.signature.service.command;

import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.CompanyAuthCommandRequest;
import com.zhaocai.common.signature.service.AbstractISignatureCommand;


/**
 * 公司认证命令
 *
 * @author chenming
 * @date 2024-09-09
 */
public class CompanyAuthCommand extends AbstractISignatureCommand {

    public CompanyAuthCommand(CompanyAuthCommandRequest commandRequest) {
        this.commandRequest = commandRequest;
    }

    @Override
    protected SignatureResponse doExecute() {
        return getPlatformSignatureService().companyAuth(commandRequest);
    }
}
