package com.zhaocai.business.manager.http.common.config;

import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.common.core.utils.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author ssy
 * @date 2024/7/9 10:10
 */
@Slf4j
@Component
public class RestTemplateUtils {

    private static RestTemplate restTemplate;

    @Resource
    public void setRestTemplate(RestTemplate restTemplate) {
        RestTemplateUtils.restTemplate = restTemplate;
    }

    /**
     * POST请求调用方式
     * @param url          请求URL
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> ResponseEntity<T> postForEntity(String url, Object requestBody, Class<T> responseType) throws RestClientException {
        return restTemplate.postForEntity(url, requestBody, responseType);
    }


    public static <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType){
        return restTemplate.getForEntity(url, responseType);
    }

    public static <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> varMap){
        return restTemplate.getForEntity(url, responseType, varMap);
    }

    public static <T> T getForObject(String url, Class<T> responseType){
        return restTemplate.getForObject(url, responseType);
    }

    public static <T> T getForObject(String url, Class<T> responseType, Map<String, ?> varMap){
        return restTemplate.getForObject(url, responseType, varMap);
    }

    /**
     * 发送 Get 请求
     * @param baseUrl           请求 url
     * @param requestParams     请求参数
     * @param responseType      响应结果类型
     * @return
     * @param <T>
     */
    public static <T> T getForObject(String baseUrl,Object requestParams, Class<T> responseType,HttpHeaders headers){
        // 将参数转化为 Map
        Map<String,?> paramsMap = JacksonUtil.object2Map(requestParams);
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(baseUrl);

        for (Map.Entry<String,?> entry : paramsMap.entrySet()) {
            if(entry.getValue() != null) {
                uriComponentsBuilder.queryParam(entry.getKey(),entry.getValue());
            }
        }
        UriComponents uriComponents = uriComponentsBuilder.build();

        return getForObject2Header(uriComponents.toString(),responseType,headers);
    }

    /**
     * 发送 Get 请求，有请求头的
     * @param url           请求参数
     * @param responseType  响应结果类型
     * @return
     * @param <T>
     */
    public static <T> T getForObject2Header(String url, Class<T> responseType,HttpHeaders headers){
        HttpEntity<T> httpEntity = new HttpEntity<>(headers);

        log.info("[RestTemplate] - 开始发送 get 请求，url:{},headers:{}",url, JacksonUtil.toJsonString(headers));
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET,httpEntity,responseType);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new BusinessException("发送 HTTP 请求失败,http code:" + response.getStatusCode());
        }
        return response.getBody();
    }

    /**
     * 发送 POST 请求
     * @param url
     * @param requestEntity
     * @param responseType
     * @return
     */
    public static <T> T postForObject(String url, HttpEntity<?> requestEntity, Class<T> responseType ) {
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST,requestEntity,responseType);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new BusinessException("发送 HTTP 请求失败,http code:" + response.getStatusCode());
        }
        return response.getBody();
    }
}
