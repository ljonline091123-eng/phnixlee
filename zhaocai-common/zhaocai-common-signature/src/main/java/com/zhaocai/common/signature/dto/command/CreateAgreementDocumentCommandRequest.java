package com.zhaocai.common.signature.dto.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.common.signature.dto.SignatureContact;
import com.zhaocai.common.signature.dto.SignatureCreator;
import com.zhaocai.common.signature.dto.SignatureStamper;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.util.List;

/**
 * 创建合同文档命令请求参数
 *
 * @author chenming
 * @date 2024-09-13
 */
@Getter
@Setter
public class CreateAgreementDocumentCommandRequest  extends SignatureCommandRequest {

    protected CreateAgreementDocumentCommandRequest() {
    }

    /**
     * 合同附件
     */
    @JsonIgnore
    private InputStream agreementFile;

    /**
     * 合同附件类型
     */
    private String agreementFileType;

    /**
     * 合同名称
     */
    private String agreementName;

    /**
     * 合同编号
     */
    private String agreementCode;

    /**
     * 合同签订发起人
     */
    private SignatureCreator signatureCreator;

    /**
     * 合同签署方
     */
    private List<SignatureContact> signatureContactList;

    /**
     * 合同签署位置
     */
    private List<SignatureStamper> signatureStamperList;
}
