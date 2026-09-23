package com.zhaocai.common.signature.dto.sign.qysp;

import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateRequestTypeEnum;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class GetSignUrlRequest extends SignatureRequest {

    public GetSignUrlRequest(SignatureCommandRequest request) {
        super(request);

        setRequestType(QiYueSuoPrivateRequestTypeEnum.GET_SIGN_URL);
    }

    /**
     * 签署方 id
     */
    private Long signatureId;

    /**
     * 电子签约文件id
     */
    @NotNull(message = "电子签约文件id不能为空")
    private Long contractId;

    /**
     * 签署方名称
     */
    @NotBlank(message = "签署方名称不能为空")
    private String tenantName;

    /**
     * 经办人姓名
     */
    @NotBlank(message = "经办人姓名不能为空")
    private String receiverName;

    /**
     * 经办人联系方式
     */
    @NotBlank(message = "经办人联系方式不能为空")
    private String contact;
}
