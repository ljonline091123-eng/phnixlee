package com.zhaocai.business.manager.http.service;

import com.zhaocai.business.manager.http.common.config.RestTemplateUtils;
import com.zhaocai.business.manager.http.dto.PlatDept;
import com.zhaocai.business.manager.http.dto.res.UnderlingResultData;
import com.zhaocai.business.manager.template.config.UnderlingPlatformConfig;
import com.zhaocai.common.core.utils.Map2ObjUtil;
import com.zhaocai.common.core.web.bean.thrid.ThridResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author ssy
 * @date 2024/7/8 14:57
 */
@Service
public class PlatOrgService {
    private static final Logger log = LoggerFactory.getLogger(PlatOrgService.class);

    @Autowired
    private UnderlingPlatformConfig underlingPlatformConfig;

    public List<PlatDept> allDepts() {
        List<PlatDept> depts = new ArrayList<>();
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(underlingPlatformConfig.getBaseUrl() + "/rest/ctrl/api/depts")
                .queryParam("authCode", underlingPlatformConfig.getAuthCode())
                .build();

        try {
            UnderlingResultData<List<LinkedHashMap>> response = RestTemplateUtils.getForObject(uriComponents.toString(), UnderlingResultData.class);
            //如果成功返回
            if (response.getCode() == ThridResultCode.SUCCESS.getCode()){
                List<LinkedHashMap> mapList = response.getData();
//                depts = mapList.stream().map(map -> Map2ObjUtil.convertToObject(map, PlatDept.class)).collect(Collectors.toList());
                for (LinkedHashMap map : mapList){
                    PlatDept platDept = Map2ObjUtil.convertToObject(map, PlatDept.class);
                    depts.add(platDept);
                }
            }
//            TreeDeptUtil.buildTreeNode(depts, "0");
        } catch (Exception ex) {
            log.error("第三方组织接口获取失败:{}", ex.getMessage());
        }
        return depts;
    }





    /*public List<PlatDept> test() {
        List<PlatDept> depts = new ArrayList<>();

        WebClient webClient = WebClient
                .builder()
                .baseUrl("http://192.168.240.17:30163/jkptht")
                .codecs(item -> item.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();

        List<PlatDept> ans = webClient.get().uri("/rest/ctrl/api/depts?authCode={1}", "7976f20e9137482c8f6c0ecf3d59e307")
                .retrieve().bodyToFlux(PlatDept.class).collectList().block();

        String requestUrl = "http://192.168.240.17:30163/jkptht/rest/ctrl/api/depts";
        Map<String, String> paramMap = new HashMap<>(1);
        paramMap.put("authCode", "7976f20e9137482c8f6c0ecf3d59e307");
        String json = HttpClientUtil.doGet(requestUrl, paramMap);
        Map<String, Object> m = (Map<String, Object>) JSONObject.parseObject(json);
        return depts;
    }*/

    /*public void ttt(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("acc-key", "acc-key");

        JSONObject requestParam = new JSONObject();
        requestParam.put("code", "code");
        requestParam.put("message", "message");

        HttpEntity<String> requestBody = new HttpEntity<String>(JSON.toJSONString(requestParam), headers);

        String url = "测试地址";
        ResponseEntity<PlatDept> resp = RestTemplateUtils.post(url, requestBody, PlatDept.class);
    }*/




}
