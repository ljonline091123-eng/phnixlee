package com.zhaocai.common.signature.service;


import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;

import java.util.function.BiConsumer;

/**
 * 电子签章命令工厂类，用于统一签章命令的调度
 *
 * @author chenming
 * @date 2024-08-28
 */
public class SignatureCommandFactory {

    private static volatile SignatureCommandFactory instance;

    private SignatureCommandFactory() {

    }

    private static class SingletonHelper {
        private static final SignatureCommandFactory INSTANCE = new SignatureCommandFactory();
    }

    public static SignatureCommandFactory getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * 执行电子签章命令
     * @param signatureCommand
     * @param consumer
     */
    public SignatureResponse executeSignCommand(ISignatureCommand signatureCommand, BiConsumer<SignatureCommandRequest, SignatureResponse> consumer) {
        SignatureCommandInvoker commandInvoker = new SignatureCommandInvoker(signatureCommand);
        return commandInvoker.action(consumer);
    }
}
