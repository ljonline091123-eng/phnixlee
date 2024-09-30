package com.zhaocai.common.signature.dto.sign.qysp;

import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateRequestTypeEnum;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.command.SignatureCommandRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 下载电子签章文档请求
 *
 * @author chenming
 * @date 2024-09-20
 */
@Getter
@Setter
public class DownloadDocumentRequest extends SignatureRequest {

    public DownloadDocumentRequest(SignatureCommandRequest request) {
        super(request);

        setRequestType(QiYueSuoPrivateRequestTypeEnum.GET_SIGN_URL);
    }

    /**
     * 签署文档id
     */
    private Long documentId;
}
