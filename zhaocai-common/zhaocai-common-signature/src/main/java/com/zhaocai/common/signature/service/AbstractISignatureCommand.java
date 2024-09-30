package com.zhaocai.common.signature.service;

import com.zhaocai.common.signature.common.exception.SignatureValidateException;
import com.zhaocai.common.signature.common.utils.SpringBeanUtils;
import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import com.zhaocai.common.signature.service.platform.PlatformSignatureService;
import lombok.extern.slf4j.Slf4j;

/**
 * 电子签章抽象命令
 *
 * @author chenming
 * @date 2024-08-19
 */
@Slf4j
public abstract class AbstractISignatureCommand implements ISignatureCommand {

    /**
     * 电子签章命令请求参数
     */
    protected SignatureCommandRequest commandRequest;

    @Override
    public SignatureResponse execute() {
        if (commandRequest == null) {
            throw new SignatureValidateException("电子签章命令请求参数不能为空");
        }
        log.info("[电子签章] - 开始执行电子签章命令...");

        SignatureResponse response = doExecute();

        log.info("[电子签章] - 电子签章命令执行完成...");
        return response;
    }

    @Override
    public SignatureCommandRequest getCommandRequest() {
        return this.commandRequest;
    }

    /**
     * 获取电子签章执行者，相当于命令模式中的 Receiver
     * @return
     */
    protected PlatformSignatureService getPlatformSignatureService() {
        return SpringBeanUtils.getBean(PlatformSignatureService.class);
    }

    /**
     * 执行业务逻辑
     */
    protected abstract SignatureResponse doExecute();
}
