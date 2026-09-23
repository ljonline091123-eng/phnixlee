package com.zhaocai.system.api.archives;

import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.ServiceNameConstants;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.system.api.config.ResultDataFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * @author lzq
 * @date 2025/01/15 11:37
 */
@FeignClient(contextId = "remoteArchivesDeviceService", configuration = ResultDataFeignConfig.class, value = ServiceNameConstants.ARCHIVES_SERVICE)
public interface RemoteArchivesDeviceService {

    /**
     * 流程提交 可能是 审批中 也可能是 审批通过 要根据completedFlag状态判断
     */
    @PostMapping("/materialApprove/processStart")
    void processStart(@RequestBody Map<String, Object> variables);

    /**
     * 审批通过，可设置状态为 已完成
     */
    @PostMapping("/materialApprove/processAuditPass")
    void processAuditPass(@RequestBody Map<String, Object> variables);

    /**
     * 驳回到发起人，可设置状态为 保存/自由态
     */
    @PostMapping("/materialApprove/processAuditFreedom")
    void processAuditFreedom(@RequestBody Map<String, Object> variables);

    /**
     * 驳回到中途节点，可以设置状态为 审批中
     */
    @PostMapping("/materialApprove/processAuditReject")
    void processAuditReject(@RequestBody Map<String, Object> variables);

    /**
     * 撤回流程，设置由发起人撤回，可以设置状态为 已撤回
     */
    @PostMapping("/materialApprove/processAuditRevoke")
    void processAuditRevoke(@RequestBody Map<String, Object> variables);

    /**
     * 定时任务同步主库数据
     * @param inner
     * @return
     */
    @PostMapping("/archivesTask/synchronizeMasterData")
    ResultData<Boolean> synchronizeMasterData(@RequestHeader(SecurityConstants.FROM_SOURCE) String inner);

    /**
     * 推送数据到中台
     * @param inner
     * @return
     */
    @PostMapping("/archivesTask/pushMiddlePlatform")
    ResultData<Boolean> pushMiddlePlatform(@RequestHeader(SecurityConstants.FROM_SOURCE) String inner);
}
