package com.zhaocai.business.filez.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhaocai.business.common.config.EnvironmentUtil;
import com.zhaocai.business.common.enums.FileZResponseCodeEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.filez.service.dto.FileZRequest;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.dto.FileZResponse;
import com.zhaocai.business.manager.http.common.config.RestTemplateUtils;
import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.file.FileUtils;
import com.zhaocai.common.core.utils.uuid.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * FileZ 服务的抽象类
 *
 * @author chenming
 * @date 2024-07-05
 */
@Slf4j
public abstract class AbstractFileZRequestService implements FileZRequestService{

    private IFileZTaskService fileZTaskService = SpringUtils.getBean(IFileZTaskService.class);

    @Override
    public void sendFileZRequest(FileZRequestContext requestContext) {
        // 校验请求参数
        checkRequestContext(requestContext);

        // 保存请求任务
        long fileTaskId = fileZTaskService.addFileZTask(requestContext);
        requestContext.setFileZTaskId(fileTaskId);

        // 构建请求参数
        FileZRequest request = buildRequestParams(requestContext);

        // 保存参数
        fileZTaskService.setFileZTaskRequestBody(fileTaskId,JSON.toJSONString(request));

        // 设置 HttpHeaders
        HttpHeaders headers = getHttpHeaders(request,requestContext.getRequestTypeEnum().getHttpMethod());

        // 发送请求
        Object responseBody = null;
        if (HttpMethod.POST.matches(requestContext.getRequestTypeEnum().getHttpMethod().name())) {
            // 发送 post 请求，POST 请求的 ResponseType 统一为 String
           responseBody = sendFileZ4Post(requestContext,request,headers);
        } else if (HttpMethod.GET.matches(requestContext.getRequestTypeEnum().getHttpMethod().name())) {
            // 发送 GET 请求，GET 请求 responseType 不确定，统一为 Object
           responseBody = sendFileZ4Get(requestContext,request,headers);
        }

        // 处理返回结果
        responseHandler(fileTaskId,responseBody,requestContext);
    }

    /**
     * 处理响应结果
     * @param fileTaskId
     * @param responseBody
     */
    protected void responseHandler(long fileTaskId, Object responseBody,FileZRequestContext requestContext) {
        if (responseBody instanceof String) {
            String responseStr = (String) responseBody;
            FileZResponse response = JSON.parseObject(responseStr, FileZResponse.class);
            fileZTaskService.setFileZTaskResponse(fileTaskId,response);

            if (!FileZResponseCodeEnum.isSuccess(response.getCode())) {
                throw new BusinessException("请求联想文档失败" + (StringUtils.isNotBlank(response.getExceptionMessage()) ? "，原因:" + response.getExceptionMessage() : ""));
            }
        } else {
            throw new BusinessException("通用型方法 responseHandler 只处理 String 类型的，非 String 类型请自行实现");
        }

    }

    /**
     * 校验参数
     * @param requestContext
     */
    private void checkRequestContext(FileZRequestContext requestContext) {
        if (requestContext == null) {
            throw new ParamValidateException("文档中台请求上下文不能为空");
        }
    }

    /**
     * 获取 HttpHeaders
     * @return
     */
    private HttpHeaders getHttpHeaders(FileZRequest request,HttpMethod httpMethod) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));

        // 验证类型
        headers.add("zOffice-auth-type","s2s_MD5_sig");

        // 时间戳
        long timeStamp = System.currentTimeMillis();
        headers.add("timeStamp",timeStamp + "");

        // zOffice-message-nonce
        String messageNonceHeaderValue = UUID.randomUUID().toString();
        headers.add("zOffice-message-nonce",messageNonceHeaderValue);

        // 请求体
        String requestBody = JSONObject.toJSONString(request);

        // 获取 token
        String token = getAuthToken(timeStamp,messageNonceHeaderValue,requestBody,httpMethod);

        String repoId = EnvironmentUtil.getProperty("file-z.repoId");
        String appId = EnvironmentUtil.getProperty("file-z.appId","publicApi");

        String authorization = repoId + ":" + appId + ":" + token;
        headers.add("Authorization",authorization);

        return headers;
    }

    /**
     * 获取 token
     * @param timeStamp
     * @param messageNonceHeaderValue
     * @param requestBody
     * @return
     */
    private String getAuthToken(long timeStamp, String messageNonceHeaderValue, String requestBody,HttpMethod httpMethod) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            String secret = EnvironmentUtil.getProperty("file-z.secret");
            String seed = secret + "@@" + timeStamp + "@@" + messageNonceHeaderValue;
            if (StringUtils.isNoneBlank(requestBody) && HttpMethod.POST.matches(httpMethod.name())) {
                // 只有 POST 请求体需要加
                seed += "@@" + requestBody;
            }

            md5.update(seed.getBytes(StandardCharsets.UTF_8));
            return String.format("%032x", new BigInteger(1, md5.digest()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送 GET 请求，GET 请求 responseType 不确定，统一为 Object
     * @param requestContext
     * @param request
     * @param headers
     * @return
     */
    private Object sendFileZ4Get(FileZRequestContext requestContext, FileZRequest request, HttpHeaders headers) {
        long businessId = requestContext.getBusinessId();
        String requestDesc = requestContext.getRequestTypeEnum().getRequestDesc();
        String requestUrl = getRequestUrl(requestContext);

        log.info("联想文档 - [{}-{}] - 开始发送 GET 请求，url:{},params:{}",requestDesc,businessId,requestUrl, JSON.toJSONString(request));
        byte[] responseBody = RestTemplateUtils.getForObject(requestUrl,request,byte[].class,headers);

        if (FileUtils.isValidFile(responseBody)) {
            // 为文件，直接返回
            return responseBody;
        } else {
            return new String(responseBody);
        }
    }

    /**
     * 发送 POST 请求<br/>
     * POST 返回结果统一处理为 String
     * @param request
     * @param headers
     */
    private String sendFileZ4Post(FileZRequestContext requestContext,FileZRequest request, HttpHeaders headers) {
        long businessId = requestContext.getBusinessId();
        String requestDesc = requestContext.getRequestTypeEnum().getRequestDesc();
        String requestUrl = getRequestUrl(requestContext);

        log.info("联想文档 - [{}-{}] - 开始发送 POST 请求，url:{},body:{}",requestDesc,businessId,requestUrl, JSON.toJSONString(request));

        JSONObject requestObject = JSON.parseObject(JSON.toJSONString(request));
        HttpEntity<JSONObject> httpEntity = new HttpEntity<>(requestObject,headers);
        try {
            String responseStr = RestTemplateUtils.postForObject(requestUrl,httpEntity,String.class);

            log.info("联想文档 - [{}-{}] - 发送 POST 请求成功,body:{}",requestDesc,businessId, responseStr);
            return responseStr;
        } catch (HttpClientErrorException e) {
            log.error("联想文档 -[{}-{}] - 发送请求失败，cause by:{}",requestDesc,businessId,e.getMessage(),e);
            FileZResponse response = new FileZResponse();
            response.setCode("HttpClientErrorException");
            response.setExceptionMessage(e.getMessage());
            return JSON.toJSONString(response);
        } catch (Exception ex) {
            log.error("联想文档 -[{}-{}] - 发送请求失败，cause by:{}",requestDesc,businessId,ex.getMessage(),ex);
            FileZResponse response = new FileZResponse();
            response.setCode("Exception");
            response.setExceptionMessage(ex.getMessage());
            return JSON.toJSONString(response);
        }
    }

    /**
     * 请求 url
     * @param requestContext
     * @return
     */
    private String getRequestUrl(FileZRequestContext requestContext) {
        return EnvironmentUtil.getProperty("file-z.apiPrefix") + requestContext.getRequestTypeEnum().getRequestUrl();
    }

    /**
     * 回调地址
     * @return
     */
    protected String getCallback() {
        return EnvironmentUtil.getProperty("file-z.callback-prefix") + "/callback/contentUpdate";
    }

    protected abstract FileZRequest buildRequestParams(FileZRequestContext requestContext);
}
