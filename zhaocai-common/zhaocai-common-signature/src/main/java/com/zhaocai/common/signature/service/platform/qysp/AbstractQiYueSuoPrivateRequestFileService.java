package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.common.enums.RequestType;
import com.zhaocai.common.signature.common.exception.SignatureException;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import net.qiyuesuo.v3sdk.utils.SdkResponse;
import net.qiyuesuo.v3sdk.utils.StreamResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 下载文件请求类
 *
 * @author chenming
 * @date 2024-09-20
 */
public abstract class AbstractQiYueSuoPrivateRequestFileService extends AbstractQiYueSuoPrivateSignRequestService {

    private static final Logger log = LoggerFactory.getLogger(AbstractQiYueSuoPrivateRequestFileService.class);

    @Override
    protected SdkResponse<?> sendSignRequest(SdkRequest request, RequestType requestType) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            // 下载文件
            StreamResponse<Object> response = getSdkClient().download(request,bos);

            if (response.getCode() == 0) {
                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                response.setResult(bis);
                return response;
            }
            throw new SignatureException(response.getCode().toString(),response.getMessage());
        } catch (Exception e) {
            throw e;
        }

    }
}
