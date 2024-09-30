package com.zhaocai.system.manager.http.service;

import com.zhaocai.common.core.utils.Map2ObjUtil;
import com.zhaocai.common.core.web.bean.thrid.ThridResultCode;
import com.zhaocai.system.manager.http.common.config.RestTemplateUtils;
import com.zhaocai.system.manager.http.common.config.UnderlingPlatformConfig2;
import com.zhaocai.system.manager.http.dto.PlatUser;
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
 * @date 2024/7/9 13:56
 */
@Service
public class PlatUserService {
    private static final Logger log = LoggerFactory.getLogger(PlatUserService.class);

    @Autowired
    private UnderlingPlatformConfig2 underlingPlatformConfig;

    public List<PlatUser> getPlatUser() {
        List<PlatUser> users = new ArrayList<>();
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(underlingPlatformConfig.getBaseUrl() + "/rest/ctrl/api/users")
                .queryParam("authCode", underlingPlatformConfig.getAuthCode())
                .build();

        try {
            UnderlingResultData<List<LinkedHashMap>> response = RestTemplateUtils.getForObject(uriComponents.toString(), UnderlingResultData.class);
            //如果成功返回
            if (response.getCode() == ThridResultCode.SUCCESS.getCode()){
                List<LinkedHashMap> mapList = response.getData();
                for (LinkedHashMap map : mapList){
                    PlatUser platUser = Map2ObjUtil.convertToObject(map, PlatUser.class);
                    users.add(platUser);
                }
            }
        } catch (Exception ex) {
            log.error("第三方用户列表接口获取失败:{}", ex.getMessage());
        }
        return users;
    }

}
