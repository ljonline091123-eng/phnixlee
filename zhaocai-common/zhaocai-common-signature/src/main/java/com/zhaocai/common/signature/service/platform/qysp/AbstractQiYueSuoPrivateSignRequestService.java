package com.zhaocai.common.signature.service.platform.qysp;

import com.zhaocai.common.signature.common.enums.RequestType;
import com.zhaocai.common.signature.common.exception.SignatureException;
import com.zhaocai.common.signature.common.exception.SignatureValidateException;
import com.zhaocai.common.signature.common.utils.SpringBeanUtils;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.service.impl.AgreementSignQysRequestLogService;
import com.zhaocai.common.signature.utils.JacksonUtil;
import com.zhaocai.common.signature.utils.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import net.qiyuesuo.v3sdk.SdkClient;
import net.qiyuesuo.v3sdk.exception.BaseSdkException;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import net.qiyuesuo.v3sdk.utils.SdkResponse;

import java.io.InputStream;

/**
 * 摘要齐岳硕签名请求默认服务
 *
 * @author chenming
 * @date 2024-08-27
 */
@Slf4j
public abstract class AbstractQiYueSuoPrivateSignRequestService implements QiYueSuoPrivateSignRequestService{
    // 调用异常错误码
    private final Integer EXCEPTION_CODE = -9999;

    private final AgreementSignQysRequestLogService requestLogService = SpringBeanUtils.getBean(AgreementSignQysRequestLogService.class);

    @Override
    public SignatureResponse sendSignRequest(SignatureRequest signatureRequest) {
        Long businessId =  signatureRequest.getBusinessId();
        RequestType requestType = signatureRequest.getRequestType();

        /*
         * 校验参数
         */
        log.info("[电子签章-契约锁-{}]，{} - 开始校验参数...",requestType.getRequestName(),businessId);
        checkRequestParam(signatureRequest);

        /*
         * 构建请求参数
         */
        log.info("[电子签章-契约锁-{}]，{} - 校验参数完成，开始构造请求参数...",requestType.getRequestName(),businessId);
        SdkRequest request = builderSignRequest(signatureRequest);

        SignatureResponse signatureResponse = new SignatureResponse();
        SdkResponse<?> response = null;
        long requestLogId = 0L;
        try {
            /*
             * 增加请求日志
             */
            requestLogId = addRequestLog(request,signatureRequest);
            log.info("[电子签章-契约锁-{}],{},插入请求日志成功,logId:{}",requestType.getRequestName(),businessId,requestLogId);
            /*
             * 发送请求
             */
            log.info("[电子签章-契约锁-{}],{}，构造请求参数成功，开始发送请求,url:{},params:{}",requestType.getRequestName(),businessId,request.getBaseServiceUrl(),
                                                                            JacksonUtil.toJsonString(request.getHttpParameter()));
            response = this.sendSignRequest(request,requestType);

            if (response.getResult() instanceof InputStream) {
                log.info("[电子签章-契约锁-{}],{}，请求发送成功，响应结果，responseCode:{},responseMsg:{},结果集:二进制数组", requestType.getRequestName(),businessId,
                        response.getCode(),response.getMessage());
            } else {
                log.info("[电子签章-契约锁-{}],{}，请求发送成功，响应结果，responseCode:{},responseMsg:{},结果集:{}", requestType.getRequestName(),businessId,
                        response.getCode(),response.getMessage(), JacksonUtil.toJsonString(response.getResult()));
            }

            signatureResponse.setIsSuccess(true);
            signatureResponse.setMessage("请求成功");
        } catch (BaseSdkException be) {
            log.error("[电子签章-契约锁-{}],{}，调用契约锁请求失败，cause by:{}",requestType.getRequestName(),businessId,be.getMessage(),be);
            response = new SdkResponse<>(EXCEPTION_CODE,be.getMessage(),null);

            signatureResponse.setIsSuccess(false);
            signatureResponse.setCode(be.getCode().toString());
            signatureResponse.setMessage("请求失败," + be.getMessage());
        } catch (SignatureException se) {
            log.error("[电子签章-契约锁-{}],{}，调用契约锁请求失败，errorCode:{},errorMessage:{}",requestType.getRequestName(),businessId,se.getErrorCode(),se.getErrorMessage());
            response = new SdkResponse<>(Integer.valueOf(se.getErrorCode()),se.getErrorMessage(),null);

            signatureResponse.setIsSuccess(false);
            signatureResponse.setCode(se.getErrorCode());
            signatureResponse.setMessage("请求失败," + se.getErrorMessage());
        } catch (Exception e) {
            log.error("[电子签章-契约锁-{}],{}，请求发送失败，cause by:{}",requestType.getRequestName(),businessId,e.getMessage(),e);
            response = new SdkResponse<>(EXCEPTION_CODE,e.getMessage(),null);

            signatureResponse.setIsSuccess(false);
            signatureResponse.setCode(EXCEPTION_CODE.toString());
            signatureResponse.setMessage("请求失败," + e.getMessage());
        } finally {
            /*
             * 响应
             */
            addResponseLog(response,requestLogId);
        }

        signatureResponse.setResponseResult(response.getResult());
        return signatureResponse;
    }

    /**
     * 增加响应结果
     * @param response
     * @param requestLogId
     */
    private void addResponseLog(SdkResponse<?> response, long requestLogId) {
        requestLogService.addResponseLog(requestLogId,response);
    }

    /**
     * 增加请求日志
     * @param request
     * @param signatureRequest
     */
    private Long addRequestLog(SdkRequest request, SignatureRequest signatureRequest) {
        return requestLogService.addRequestLog(signatureRequest,request);
    }

    /**
     * 获取 SdkClient
     * @return
     */
    protected SdkClient getSdkClient() {
        return SpringBeanUtils.getBean(SdkClient.class);
    }

    /**
     * 校验参数
     * @param signatureRequest
     */
    protected void checkRequestParam(SignatureRequest signatureRequest) {
        if (signatureRequest == null) {
            throw new SignatureValidateException("电子签章命令请求参数不能为空");
        }
        // 构造器里面将必要的参数做了控制，这里不需要了，如果子类有更加严格的控制，则重写该方法即可
        ValidationUtils.validateObject(signatureRequest);
    }

    /**
     * 构建请求参数
     * @param signatureRequest
     * @return
     */
    protected abstract SdkRequest builderSignRequest(SignatureRequest signatureRequest);


    /**
     * 发送请求
     * @param request
     * @return
     */
    protected abstract SdkResponse<?> sendSignRequest(SdkRequest request,RequestType requestType) throws Exception;
}
