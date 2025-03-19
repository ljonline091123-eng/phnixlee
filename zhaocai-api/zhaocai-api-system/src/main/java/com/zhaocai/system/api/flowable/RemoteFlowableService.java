package com.zhaocai.system.api.flowable;

import com.zhaocai.common.core.constant.ServiceNameConstants;
import com.zhaocai.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(contextId = "zhaocai-flowable", value = ServiceNameConstants.FLOWABLE_SERVICE)
public interface RemoteFlowableService {

    /**
     * 根据流程定义id启动流程实例
     * @param procDefId
     * @param variables
     * @return
     */
    @PostMapping("/definition/startReturnInstanceId/{procDefId}")
    AjaxResult startReturnInstanceId(@ApiParam(value = "流程定义id") @PathVariable(value = "procDefId") String procDefId,
                                                @ApiParam(value = "变量集合,json对象") @RequestBody Map<String, Object> variables) ;

    @PostMapping("/task/initialize")
    AjaxResult initialize(@RequestBody Map<String, Object> variables);
}
