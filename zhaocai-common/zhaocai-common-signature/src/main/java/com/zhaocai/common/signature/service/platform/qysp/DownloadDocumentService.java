package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.sign.qysp.DownloadDocumentRequest;
import net.qiyuesuo.v3sdk.model.document.request.DocumentDownloadRequest;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import org.springframework.stereotype.Service;


/**
 * 下载文档服务
 *
 * @author chenming
 * @date 2024-09-20
 */
@Service
public class DownloadDocumentService extends AbstractQiYueSuoPrivateRequestFileService{

    @Override
    protected SdkRequest builderSignRequest(SignatureRequest signatureRequest) {
        DownloadDocumentRequest request = (DownloadDocumentRequest) signatureRequest;

        DocumentDownloadRequest downloadRequest = new DocumentDownloadRequest();
        downloadRequest.setDocumentId(request.getDocumentId());
        downloadRequest.setName(request.getApplicantName());
        downloadRequest.setContact(request.getApplicantMobile());

        return downloadRequest;
    }
}
