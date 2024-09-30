package com.zhaocai.common.signature.dto.sign.qysp;

import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateRequestTypeEnum;
import com.zhaocai.common.signature.dto.SignatureContact;
import com.zhaocai.common.signature.dto.SignatureCreator;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.SignatureStamper;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建电子签约请求
 *
 * @author chenming
 * @date 2024-09-18
 */
@Getter
@Setter
public class CreateByCategoryRequest extends SignatureRequest {

    public CreateByCategoryRequest(SignatureCommandRequest request) {
        super(request);

        setRequestType(QiYueSuoPrivateRequestTypeEnum.CREATE_BY_CATEGORY);
    }

    /**
     * 签署文档id
     */
    @NotNull(message = "签署文档id")
    private Long documents;

    /**
     * 签署文件编号，即合同号
     */
    @NotBlank(message = "签署文件编号不能为空")
    private String documentCode;

    /**
     * 文件主题
     * 电子签约文件的标题
     */
    @NotBlank(message = "文件主题不能为空")
    private String subject;

    /**
     * 签章发起人
     */
    @NotNull(message = "签章发起人不能为空")
    private SignatureCreator signatureCreator;

    /**
     * 签署方
     */
    @NotNull(message = "签署方不能为空")
    private List<SignatureContact> signatureContactList;

    /**
     * 签署方位置信息
     */
    @NotNull(message = "签署方位置信息不能为空")
    private List<SignatureStamper> signatureStamperList;
}
