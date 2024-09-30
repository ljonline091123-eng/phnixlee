package com.zhaocai.common.signature.dto.command;


import com.zhaocai.common.signature.common.enums.SignatureTypeEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * 获取签署 url 命令参数
 *
 * @author chenming
 * @date 2024-09-19
 */
@Getter
@Setter
public class GetSignUrlCommandRequest extends SignatureCommandRequest{

    protected GetSignUrlCommandRequest(){

    }

    /**
     * 签署方
     */
    private SignatureTypeEnum signatureType;

    /**
     * 签署方名称
     */
    private String signatureName;

    /**
     * 签署经办人联系名
     */
    private String signatureContactName;

    /**
     * 签署经办人联系方式
     */
    private String signatureContactPhone;
}
