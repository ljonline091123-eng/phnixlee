package com.zhaocai.business.manager.http.service;

import com.zhaocai.business.manager.http.common.config.RestTemplateUtils;
import com.zhaocai.business.manager.http.dto.PlatCountry;
import com.zhaocai.business.manager.http.dto.req.UnderlyingPlatformBaseDTO;
import com.zhaocai.business.manager.http.dto.res.UnderlingResultData;
import com.zhaocai.business.manager.template.config.UnderlingPlatformConfig;
import com.zhaocai.common.core.utils.Map2ObjUtil;
import com.zhaocai.common.core.web.bean.thrid.ThridResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author ssy
 * @date 2024/7/14 19:59
 */
@Service
public class PlatCountryService {
    private static final Logger log = LoggerFactory.getLogger(PlatCountryService.class);

    @Autowired
    private UnderlingPlatformConfig underlingPlatformConfig;

    public List<PlatCountry> getCountryList() {
        List<PlatCountry> countrys = new ArrayList<>();
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(underlingPlatformConfig.getBaseUrl() + "/rest/base/sysCountry/page")
                .queryParam("authCode", underlingPlatformConfig.getAuthCode())
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10000)
                .build();

        try {
            UnderlyingPlatformBaseDTO requestDTO = new UnderlyingPlatformBaseDTO();
            HttpHeaders headers = new HttpHeaders();
            // 授权码
            headers.add("Authorization", requestDTO.getAuthorization());
//            headers.add("Authorization",
//                    "eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6IjFiZjFhZjA2LTFiODQtNDUyMy05ZGE5LTk5NWFmMThjNDU0NiJ9.c9XrAWa5Tr5rOKfterv6cRa8fEyEzCR8O-oReR5yo1H7trpWCfqGYdJNlk6M-jcUk2Z2mtjofwMowTBkWbgeoQ");
            UnderlingResultData<LinkedHashMap> response = RestTemplateUtils.getForObject2Header(uriComponents.toString(), UnderlingResultData.class, headers);
            //如果成功返回
            if (response.getCode() == ThridResultCode.SUCCESS.getCode()){
                LinkedHashMap<String, List<LinkedHashMap>> map = response.getData();
                List<LinkedHashMap> dataList = map.get("list");
                for (LinkedHashMap dataMap : dataList){
                    PlatCountry platCountry = Map2ObjUtil.convertToObject(dataMap, PlatCountry.class);
                    countrys.add(platCountry);
                }
            }
        } catch (Exception ex) {
            log.error("国家和地区档案第三方接口获取失败:{}", ex.getMessage());
        }
        return countrys;
    }

}
