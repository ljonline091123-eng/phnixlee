package com.zhaocai.common.signature.service;

import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

/**
 * 电子签章执行者
 *
 * @author chenming
 * @date 2024-08-20
 */
@Slf4j
public class SignatureCommandInvoker {

    /**
     * 电子签章命令
     */
    private final ISignatureCommand signatureCommand;

    public SignatureCommandInvoker(ISignatureCommand signatureCommand) {
        this.signatureCommand = signatureCommand;
    }

    /**
     * 执行操作
     */
    public SignatureResponse action() {
        return this.signatureCommand.execute();
    }

    /**
     * 执行操作
     * @param consumer
     */
    public SignatureResponse action(BiConsumer<SignatureCommandRequest,SignatureResponse> consumer) {
        /*
         * 执行操作
         */
        SignatureResponse response = this.action();

        /*
         * 执行后续操作
         */
        if (consumer != null) {
            log.info("[电子签章] - 电子签章请求已执行完成，开始执行回调逻辑...");
            consumer.accept(signatureCommand.getCommandRequest(),response);
            log.info("[电子签章] - 电子签章执行回调逻辑完成...");
        }

        return response;
    }
}
