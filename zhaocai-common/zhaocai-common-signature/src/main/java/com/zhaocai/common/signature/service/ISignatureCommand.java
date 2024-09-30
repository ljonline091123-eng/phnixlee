package com.zhaocai.common.signature.service;


import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;

/**
 * 电子签章命令接口，顶层接口
 *
 * @author chenming
 * @date 2024-08-19
 */
public interface ISignatureCommand {

    /**
     * 执行命令
     */
    SignatureResponse execute();

    /**
     * 获取请求参数
     * @return
     */
    SignatureCommandRequest getCommandRequest();
}
