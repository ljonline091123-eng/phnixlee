package com.zhaocai.business.manager.http.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ResultCode;
import com.zhaocai.business.manager.http.common.config.RestTemplateUtils;
import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.UnderlyingPlatformBaseDTO;
import com.zhaocai.business.manager.template.config.UnderlingPlatformConfig;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.JacksonUtil;
import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.common.core.web.bean.thrid.ThridResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * 底层逻辑平台请求发送
 *
 * @author chenming
 * @date 2024-07-09
 */
@Slf4j
public class UnderlingRestTemplateService {

    /**
     * 响应结果 code 编码
     */
    private final static String RESPONSE_RESULT_CODE = "code";

    /**
     * 响应结果 data 编码
     */
    private final static String RESPONSE_RESULT_DATA_CODE = "data";

    /**
     * 响应消息 message 编码
     */
    private final static String RESPONSE_RESULT_MESSAGE_CODE = "message";

    /**
     * 响应消息 msg 编码
     */
    private final static String RESPONSE_RESULT_MSG_CODE = "msg";

    /**
     * 底层逻辑平台配置
     */
    private static final UnderlingPlatformConfig PLATFORM_CONFIG;

    /**
     * Json 转换
     */
    private static ObjectMapper objectMapper = null;

    static {
        PLATFORM_CONFIG = SpringUtils.getBean(UnderlingPlatformConfig.class);

        objectMapper = new ObjectMapper()
                // 过滤空值
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 转换对象时忽略不存在的字段
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 发送 get 请求，获取为实体类
     *
     * @param platformUrl
     * @param responseType
     * @param <T>
     * @return
     */
    public static <T> T getForObject(UnderlingPlatformUrlEnum platformUrl, Class<T> responseType, UnderlyingPlatformBaseDTO requestDTO) {
        return toObject(sendForGet(platformUrl, requestDTO), responseType);
    }

    /**
     * 发送 get 请求，获取为实体类
     *
     * @param platformUrl
     * @return
     */
    public static JsonNode getForObject(UnderlingPlatformUrlEnum platformUrl, UnderlyingPlatformBaseDTO requestDTO) {
        return sendForGet(platformUrl, requestDTO);
    }

    /**
     * 发送 get 请求，获取为 List
     *
     * @param platformUrl
     * @param responseType
     * @param requestDTO
     * @return
     */
    public static <T> List<T> listForObject(UnderlingPlatformUrlEnum platformUrl, Class<?> responseType, UnderlyingPlatformBaseDTO requestDTO) {
        return toList(sendForGet(platformUrl, requestDTO), responseType);
    }


    /**
     * 发送 post 请求
     *
     * @param platformUrl
     * @param responseType
     * @param requestDTO
     * @param <T>
     * @return
     */
    public static <T> T postForObject(UnderlingPlatformUrlEnum platformUrl, Class<T> responseType, UnderlyingPlatformBaseDTO requestDTO) {
        // 请求头
        HttpHeaders headers = getHttpHeaders(requestDTO);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UnderlyingPlatformBaseDTO> requestEntity = new HttpEntity<>(requestDTO, headers);

        // 请求 url
        String postUrl = PLATFORM_CONFIG.getBaseUrl() + platformUrl.getUrl() + "?authCode=" + PLATFORM_CONFIG.getAuthCode();

        log.info("[调用底层逻辑平台] -[{}]，开始发送 POSt 请求，url:{},params:{}", platformUrl.getDesc(), postUrl, JSON.toJSONString(requestDTO));

        String responseStr = RestTemplateUtils.postForObject(postUrl, requestEntity, String.class);

        log.info("[调用底层逻辑平台] -[{}]，请求发送成功，响应结果为:{}", platformUrl.getDesc(), responseStr);

        JsonNode responseNode = getJsonNode(responseStr);

        int responseCode = responseNode.get(RESPONSE_RESULT_CODE).asInt();
        if (responseCode == ThridResultCode.SUCCESS.getCode()) {
            log.info("[调用底层逻辑平台] -[{}]，响应结果成功，结果为:{}", platformUrl.getDesc(), responseNode.get("data").toString());
            return toObject(responseNode.get(RESPONSE_RESULT_DATA_CODE), responseType);
        }

        // 请求失败则获取错误信息
        String message = getResponseMessage(responseNode);
        throw new BusinessException("请求底层逻辑平台失败:" + message);
    }

    public static <T, U> T postForObject(UnderlingPlatformUrlEnum platformUrl, Class<T> responseType, UnderlyingPlatformBaseDTO requestDTO, List<U> dataList) {
        // 请求头
        HttpHeaders headers = getHttpHeaders(requestDTO);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<U>> requestEntity = new HttpEntity<>(dataList, headers);

        // 请求 url
        String postUrl = PLATFORM_CONFIG.getBaseUrl() + platformUrl.getUrl() + "?authCode=" + PLATFORM_CONFIG.getAuthCode();

        log.info("[调用底层逻辑平台] -[{}]，开始发送 POSt 请求，url:{},params:{}", platformUrl.getDesc(), postUrl, JSON.toJSONString(requestDTO));

        String responseStr = RestTemplateUtils.postForObject(postUrl, requestEntity, String.class);

        log.info("[调用底层逻辑平台] -[{}]，请求发送成功，响应结果为:{}", platformUrl.getDesc(), responseStr);

        JsonNode responseNode = getJsonNode(responseStr);

        int responseCode = responseNode.get(RESPONSE_RESULT_CODE).asInt();
        if (responseCode == ThridResultCode.SUCCESS.getCode()) {
            log.info("[调用底层逻辑平台] -[{}]，响应结果成功，结果为:{}", platformUrl.getDesc(), responseNode.get("data") == null ?
                    null : responseNode.get("data").toString());
            return toObject(responseNode.get(RESPONSE_RESULT_DATA_CODE), responseType);
        }

        // 请求失败则获取错误信息
        String message = getResponseMessage(responseNode);
        throw new BusinessException("请求底层逻辑平台失败:" + message);
    }

    /**
     * 发送 get 请求，获取为 Page
     *
     * @param platformUrl
     * @param responseType
     * @param requestDTO
     * @return
     */
    public static <T> PageResult<T> pageForObject(UnderlingPlatformUrlEnum platformUrl, Class<?> responseType, UnderlyingPlatformBaseDTO requestDTO,
                                                  String totalFieldName, String listFieldName) {
        // 获取结果
        JsonNode jsonNode = sendForGet(platformUrl, requestDTO);

        // 构建结果集
        PageResult<T> pageResult = new PageResult<>();

        // 获取总数
        Integer total = jsonNode.get(totalFieldName).asInt();
        pageResult.setTotal(total);

        // 获取数据
        JsonNode listNode = jsonNode.get(listFieldName);
        if (listNode != null && listNode.isArray()) {
            pageResult.setRows(toList(listNode, responseType));
        }
        return pageResult;
    }

    /**
     * 发送
     *
     * @param platformUrl
     * @param requestDTO
     * @return
     */
    public static JsonNode sendForGet(UnderlingPlatformUrlEnum platformUrl, UnderlyingPlatformBaseDTO requestDTO) {
        // 参数转换
        Map<String, ?> paramsMap = JacksonUtil.object2Map(requestDTO);

        // 拼接请求
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(PLATFORM_CONFIG.getBaseUrl() + platformUrl.getUrl());
        uriComponentsBuilder.queryParam("authCode", PLATFORM_CONFIG.getAuthCode());

        // 拼接参数
        for (Map.Entry<String, ?> entry : paramsMap.entrySet()) {
            if (entry.getValue() != null) {
                uriComponentsBuilder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        UriComponents uriComponents = uriComponentsBuilder.build();

        log.info("[调用底层逻辑平台] -[{}]，开始发送 GET 请求，url:{}，请求参数:{}", platformUrl.getDesc(), uriComponents.toUriString(),JSON.toJSONString(requestDTO));
        HttpHeaders headers = getHttpHeaders(requestDTO);
        String responseStr = RestTemplateUtils.getForObject2Header(uriComponents.toString(), String.class, headers);

        if (requestDTO.getLogResponseData()) {
            log.info("[调用底层逻辑平台] -[{}]，请求发送成功，响应结果为:{}", platformUrl.getDesc(), responseStr);
        } else {
            log.info("[调用底层逻辑平台] -[{}]，请求发送成功", platformUrl.getDesc());
        }

        JsonNode responseNode = getJsonNode(responseStr);
        int responseCode = responseNode.get(RESPONSE_RESULT_CODE).asInt();

        if (responseCode == ThridResultCode.SUCCESS.getCode()) {
            if (requestDTO.getLogResponseData()) {
                log.info("[调用底层逻辑平台] -[{}]，响应结果成功，结果为:{}", platformUrl.getDesc(), responseNode.get("data").toString());
            }

            return responseNode.get(RESPONSE_RESULT_DATA_CODE);
        }

        // 请求失败则获取错误信息
        String message = getResponseMessage(responseNode);

        log.error("[调用底层逻辑平台] -[{}]，响应失败失败，errorCode:{},errorMsg:{}", platformUrl.getDesc(), responseCode, message);
        throw new BusinessException("请求底层逻辑平台失败:" + message);
    }

    /**
     * 获取响应消息
     *
     * @param responseNode
     * @return
     */
    private static String getResponseMessage(JsonNode responseNode) {
        JsonNode messageNode = responseNode.get(RESPONSE_RESULT_MSG_CODE);
        if (messageNode == null) {
            messageNode = responseNode.get(RESPONSE_RESULT_MESSAGE_CODE);
        }

        return messageNode == null ? "" : messageNode.toString();
    }

    private static JsonNode getJsonNode(String responseStr) {
        try {
            return objectMapper.readTree(responseStr);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAILURE, "参数转换[JsonNode]失败..", e);
        }
    }


    public static <T> List<T> postForList(UnderlingPlatformUrlEnum platformUrl, Class<T> responseType, UnderlyingPlatformBaseDTO requestDTO) {
        // 请求头
        HttpHeaders headers = getHttpHeaders(requestDTO);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UnderlyingPlatformBaseDTO> requestEntity = new HttpEntity<>(requestDTO, headers);

        // 请求 url
        String postUrl = PLATFORM_CONFIG.getBaseUrl() + platformUrl.getUrl() + "?authCode=" + PLATFORM_CONFIG.getAuthCode();

        log.info("[调用底层逻辑平台] -[{}]，开始发送 POSt 请求，url:{},params:{}", platformUrl.getDesc(), postUrl, JSON.toJSONString(requestDTO));

        String responseStr = RestTemplateUtils.postForObject(postUrl, requestEntity, String.class);

        log.info("[调用底层逻辑平台] -[{}]，请求发送成功，响应结果为:{}", platformUrl.getDesc(), responseStr);

        JsonNode responseNode = getJsonNode(responseStr);

        int responseCode = responseNode.get(RESPONSE_RESULT_CODE).asInt();
        if (responseCode == ThridResultCode.SUCCESS.getCode()) {
            log.info("[调用底层逻辑平台] -[{}]，响应结果成功，结果为:{}", platformUrl.getDesc(), responseNode.get("data").toString());
            return toList(responseNode.get(RESPONSE_RESULT_DATA_CODE), responseType);
        }
        // 请求失败则获取错误信息
        String message = getResponseMessage(responseNode);
        throw new BusinessException("请求底层逻辑平台失败:" + message);
    }

    /**
     * 转换为单一 Object 对象
     *
     * @param jsonNode
     * @param responseType
     * @param <T>
     * @return
     */
    private static <T> T toObject(JsonNode jsonNode, Class<T> responseType) {
        try {
            return objectMapper.treeToValue(jsonNode, responseType);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAILURE, "参数转换失败..", e);
        }
    }

    /**
     * 转换为 List 集合对象
     *
     * @param jsonNode
     * @param responseType
     * @return
     */
    private static <T> List<T> toList(JsonNode jsonNode, Class<?> responseType) {
        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, responseType);
        return objectMapper.convertValue(jsonNode, listType);
    }

    /**
     * 构建 HttpHeader
     *
     * @param requestDTO
     * @return
     */
    private static HttpHeaders getHttpHeaders(UnderlyingPlatformBaseDTO requestDTO) {
        HttpHeaders headers = new HttpHeaders();
        if (StrUtil.isBlank(requestDTO.getAuthorization())) {
            // 授权码
            headers.add("Authorization", "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjay1hbm9ueW1vdXMiLCJub25jZSI6ImIwMDFkMmZiLTZmZGItNDhhNC04NDBmLTliZmYyNmVmZmViZiIsInRpbWVzdGFtcCI6MTcyMTcwNDg5NjgxOCwidXNlcklkIjoiemMtYW5vbnltb3VzIiwidXNlcm5hbWUiOiLmi5vph4ciLCJleHBpcmUiOjE4OTM0MjcyMDAsIm9yZ0lkIjoiMTAwMDAwMDAwMCIsIm9yZ0NvZGUiOiIxMDAwMDAwMDAwIiwib3JnTmFtZSI6Iua5luWNl-W7uuiuvuaKlei1hOmbhuWbouaciemZkOi0o-S7u-WFrOWPuCIsImV4cCI6MzYxNTEzMjA5Nn0.P14cdMNe3Ao-69JV1VXofM0jPuYIO58pNZu0ZWqjngTzmMgScc_RnLUekdDWBEM_3wklUNJqaw6QBLv49_DBuqOf4YutaBn7uBOjm6H7lVEu1OxwaaxwDbNr0qFnT4X8x-E7ihex8G4HpLiSLfwqdekFTPXXqIKLdAngWFyQI64");
            //系统标识
            headers.add("x-client-token", "jiantou-zhaocai~zcAnonymous");
        } else {
            // 授权码
            headers.add("Authorization", requestDTO.getAuthorization());
            //系统标识
            headers.add("x-client-token", requestDTO.getXClientToken());
        }

        //账套Id
        headers.add("corp-id-header", requestDTO.getCorpIdHeader());

        //公众号AppID
        headers.add("mp-app-id", requestDTO.getMpAppid());

        //小程序AppID
        headers.add("mini-app-id", requestDTO.getMiniAppId());

        //易料市集AppID
        headers.add("appId", requestDTO.getAppId());
        return headers;
    }
}
