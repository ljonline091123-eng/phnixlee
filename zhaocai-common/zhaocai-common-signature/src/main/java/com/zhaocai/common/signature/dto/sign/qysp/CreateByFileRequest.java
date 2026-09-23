package com.zhaocai.common.signature.dto.sign.qysp;

import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateRequestTypeEnum;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.io.InputStream;

/**
 * 创建签署文档
 */
@Getter
@Setter
public class CreateByFileRequest extends SignatureRequest {

    public CreateByFileRequest(SignatureCommandRequest request) {
        super(request);

        setRequestType(QiYueSuoPrivateRequestTypeEnum.CREATE_BY_FILE);
    }

    /**
     * 签署文档
     */
    @NotNull(message = "签署文档不能为空")
    private InputStream file;

    /**
     * 签署文档名称
     */
    @NotNull(message = "签署文档名称不能为空")
    private String title;

    /**
     * 签署文档类型
     */
    @NotNull(message = "签署文档类型不能为空")
    private String fileType;
}
