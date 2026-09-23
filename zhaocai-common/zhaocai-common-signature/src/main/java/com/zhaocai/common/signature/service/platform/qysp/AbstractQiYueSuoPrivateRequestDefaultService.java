package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.common.enums.RequestType;
import com.zhaocai.common.signature.common.exception.SignatureException;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import net.qiyuesuo.v3sdk.utils.SdkResponse;

/**
 * 默认发送请求处理类
 *
 * @author chenming
 * @date 2024/05/08
 */
public abstract class AbstractQiYueSuoPrivateRequestDefaultService extends AbstractQiYueSuoPrivateSignRequestService {

	@Override
	public SdkResponse<?> sendSignRequest(SdkRequest request, RequestType requestType) throws Exception {
		SdkResponse<?> response = getSdkClient().service(request,requestType.getResultClass());
		if (response.getCode() != 0) {
			throw new SignatureException(response.getCode().toString(),response.getMessage());
		}
		return response;
	}
}
