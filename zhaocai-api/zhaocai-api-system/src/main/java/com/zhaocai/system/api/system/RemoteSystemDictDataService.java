package com.zhaocai.system.api.system;

import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.ServiceNameConstants;
import com.zhaocai.system.api.config.InnerRFeignConfig;
import com.zhaocai.system.api.domain.SysDictData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "remoteSystemDictDataService",configuration = InnerRFeignConfig.class, value = ServiceNameConstants.SYSTEM_SERVICE)
public interface RemoteSystemDictDataService {

    /**
     * 获取字典的 label
     */
    @GetMapping("/dict/data/listDictDataLabel")
    List<SysDictData> listDictDataLabel(@RequestParam(value = "type") String type, @RequestParam(value = "value") String value
            ,@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 获取字典
     */
    @GetMapping("/dict/data/listDictByType")
    List<SysDictData> listDictByType(@RequestParam(value = "type")String type,
                                        @RequestHeader(SecurityConstants.FROM_SOURCE) String inner);
}
