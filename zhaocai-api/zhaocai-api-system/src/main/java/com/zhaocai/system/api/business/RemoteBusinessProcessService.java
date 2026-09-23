package com.zhaocai.system.api.business;


import com.alibaba.fastjson2.JSONObject;
import com.zhaocai.common.core.constant.ServiceNameConstants;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.system.api.config.ResultDataFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author lzq
 * @date 2025/1/15 11:37
 */
@FeignClient(contextId = "remoteBusinessProcessService", configuration = ResultDataFeignConfig.class, value = ServiceNameConstants.BUSINESS_SERVICE)
public interface RemoteBusinessProcessService {

    @PostMapping("/process/startProcess")
    public ResultData<String> startProcess(@RequestBody JSONObject body);

    @PostMapping("/process/auditProcess")
    public ResultData<String> auditProcess(@RequestBody JSONObject body);

    @PostMapping("/process/initialize")
    ResultData<String> initialize(@RequestBody JSONObject body);

    @PostMapping("/process/revokedProcess")
    public ResultData<String> revokedProcess(@RequestBody JSONObject body);
}
