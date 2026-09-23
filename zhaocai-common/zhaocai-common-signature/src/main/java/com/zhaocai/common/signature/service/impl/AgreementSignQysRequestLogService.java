package com.zhaocai.common.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.common.signature.common.enums.RequestType;
import com.zhaocai.common.signature.domain.AgreementSignQysRequestLog;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.mapper.AgreementSignQysRequestLogMapper;
import com.zhaocai.common.signature.utils.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import net.qiyuesuo.v3sdk.utils.SdkResponse;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;

/**
 * 合同签章-契约锁请求日志Service业务层处理
 *
 * @author chenming
 * @date 2024-09-09
 */
@Slf4j
@Service
public class AgreementSignQysRequestLogService extends ServiceImpl<AgreementSignQysRequestLogMapper, AgreementSignQysRequestLog> implements IService<AgreementSignQysRequestLog> {

    /**
     * 增加请求日志
     * @param signatureRequest
     * @param request
     */
    public Long addRequestLog(SignatureRequest signatureRequest, SdkRequest request) {
        AgreementSignQysRequestLog qysRequestLog = new AgreementSignQysRequestLog();
        qysRequestLog.setBizId(signatureRequest.getBizId());
        qysRequestLog.setBusinessCode(signatureRequest.getBusinessCode());
        qysRequestLog.setBusinessId(signatureRequest.getBusinessId());
        qysRequestLog.setApplicantName(signatureRequest.getApplicantName());
        qysRequestLog.setApplicantMobile(signatureRequest.getApplicantMobile());
        qysRequestLog.setCreateTime(new Date());

        RequestType requestType = signatureRequest.getRequestType();
        qysRequestLog.setRequestUrl(request.getBaseServiceUrl());
        qysRequestLog.setRequestName(requestType.getRequestName());

        qysRequestLog.setRequestTime(new Date());
        qysRequestLog.setRequestParams(JacksonUtil.toJsonString(request.getHttpParameter()));

        baseMapper.insert(qysRequestLog);
        return qysRequestLog.getId();
    }

    /**
     * 增加响应日志
     * @param requestLogId
     * @param response
     */
    public void addResponseLog(long requestLogId, SdkResponse<?> response) {
        String responseResult = "二进制流";
        if (!(response.getResult() instanceof InputStream)) {
            responseResult = JacksonUtil.toJsonString(response);
        }

        this.update(new LambdaUpdateWrapper<AgreementSignQysRequestLog>()
                .set(AgreementSignQysRequestLog::getResponseCode,response.getCode())
                .set(AgreementSignQysRequestLog::getResponseMessage,response.getMessage())
                .set(AgreementSignQysRequestLog::getResponseTime,new Date())
                .set(AgreementSignQysRequestLog::getUpdateTime,new Date())
                .set(AgreementSignQysRequestLog::getResponseResult, responseResult)
                .eq(AgreementSignQysRequestLog::getId,requestLogId));
    }

}
