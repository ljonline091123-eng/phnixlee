package com.zhaocai.business.manager.http.service;

import com.zhaocai.business.manager.http.common.config.RestTemplateUtils;
import com.zhaocai.business.manager.http.dto.PlatAreaDivision;
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
 * @date 2024/7/11 16:04
 */
@Service
public class PlatAreaDivisionService {
    private static final Logger log = LoggerFactory.getLogger(PlatAreaDivisionService.class);

    @Autowired
    private UnderlingPlatformConfig underlingPlatformConfig;

    public List<PlatAreaDivision> getAreaDivisionList() {
        List<PlatAreaDivision> areaDivisions = new ArrayList<>();
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(underlingPlatformConfig.getBaseUrl() + "/rest/base/sysArea/list")
                .queryParam("authCode", underlingPlatformConfig.getAuthCode())
                .build();

        try {
            UnderlingResultData<List<LinkedHashMap>> response = RestTemplateUtils.getForObject(uriComponents.toString(), UnderlingResultData.class);
            //如果成功返回
            if (response.getCode() == ThridResultCode.SUCCESS.getCode()){
                List<LinkedHashMap> mapList = response.getData();
                for (LinkedHashMap map : mapList){
                    PlatAreaDivision platAreaDivision = Map2ObjUtil.convertToObject(map, PlatAreaDivision.class);
                    areaDivisions.add(platAreaDivision);
                }
//                areaDivisions = mapList.stream().map(map -> Map2ObjUtil.convertToObject(map, PlatAreaDivision.class)).collect(Collectors.toList());
            }
        } catch (Exception ex) {
            log.error("行政区划第三方接口获取失败:{}", ex.getMessage());
        }
        return areaDivisions;
    }

}
