package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.sign.qysp.CreateByFileRequest;
import net.qiyuesuo.v3sdk.http.StreamFile;
import net.qiyuesuo.v3sdk.model.v2document.request.V2DocumentCreatebyfileRequest;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import org.springframework.stereotype.Service;

/**
 * 创建签署文档
 *
 * @author chenming
 * @date 2024-09-14
 */
@Service
public class CreateByFileService extends AbstractQiYueSuoPrivateRequestDefaultService{
    @Override
    protected SdkRequest builderSignRequest(SignatureRequest signatureRequest) {
        CreateByFileRequest createByFile = (CreateByFileRequest) signatureRequest;
        V2DocumentCreatebyfileRequest createByFileRequest = new V2DocumentCreatebyfileRequest();

        String streamFileName = createByFile.getTitle() + "." + createByFile.getFileType();
        createByFileRequest.setFile(new StreamFile(streamFileName, createByFile.getFile()));
        createByFileRequest.setFileType(createByFile.getFileType());
        createByFileRequest.setTitle(createByFile.getTitle());

        return createByFileRequest;
    }
}
