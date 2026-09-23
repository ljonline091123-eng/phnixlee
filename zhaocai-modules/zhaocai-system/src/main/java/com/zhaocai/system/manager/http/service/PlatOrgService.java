package com.zhaocai.system.manager.http.service;

import com.zhaocai.common.core.utils.Map2ObjUtil;
import com.zhaocai.common.core.web.bean.thrid.ThridResultCode;
import com.zhaocai.system.manager.http.common.config.RestTemplateUtils;
import com.zhaocai.system.manager.http.common.config.UnderlingPlatformConfig2;
import com.zhaocai.system.manager.http.dto.PlatDept;
import com.zhaocai.system.manager.http.dto.res.UnderlingResultData;
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
    private UnderlingPlatformConfig2 underlingPlatformConfig;

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
                for (LinkedHashMap map : mapList){
                    PlatDept platDept = Map2ObjUtil.convertToObject(map, PlatDept.class);
                    depts.add(platDept);
                }
            }
        } catch (Exception ex) {
            log.error("第三方组织接口获取失败:{}", ex.getMessage());
        }
        return depts;
    }

}
