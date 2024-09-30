package com.zhaocai.system.api.business;

import com.zhaocai.common.core.constant.ServiceNameConstants;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.system.api.config.ResultDataFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * 资产(DM071)、服务(DM073)数据同步
 *
 * @author chenming
 * @date 2024-08-31
 */
@FeignClient(contextId = "remoteDwMmInfoSynchronizeDataService",configuration = ResultDataFeignConfig.class, value = ServiceNameConstants.BUSINESS_SERVICE)
public interface RemoteDwMmInfoSynchronizeDataService {

    /**
     * 同步 dm071 数据
     */
    @GetMapping("/dwMmInfo/synchronizeDm071Data")
    Boolean synchronizeDm071Data();

    /**
     * 同步 dm073 数据
     */
    @GetMapping("/dwMmInfo/synchronizeDm073Data")
    Boolean synchronizeDm073Data();
}
