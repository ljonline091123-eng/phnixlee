package com.zhaocai.business.integration.controller;

import com.zhaocai.business.integration.domain.model.IntegrationOutboxEvent;
import com.zhaocai.business.integration.service.IIntegrationOutboxService;
import com.zhaocai.business.integration.service.IntegrationOutboxDispatcher;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.annotation.RequiresPermissions;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@Validated
@RestController
@RequestMapping("/integration/outbox")
@Api(value = "集成事件发件箱", tags = "集成事件发件箱")
public class IntegrationOutboxController {
    private final IIntegrationOutboxService outboxService;
    private final IntegrationOutboxDispatcher dispatcher;

    public IntegrationOutboxController(IIntegrationOutboxService outboxService,
                                       IntegrationOutboxDispatcher dispatcher) {
        this.outboxService = outboxService;
        this.dispatcher = dispatcher;
    }

    @GetMapping
    @ApiOperation("查询最近的集成事件")
    @RequiresPermissions("integration:outbox:query")
    public ResultData<List<IntegrationOutboxEvent>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return ResultData.data(outboxService.listRecent(status, limit));
    }

    @PostMapping("/{eventId}/retry")
    @ApiOperation("重新投递失败或死信事件")
    @RequiresPermissions("integration:outbox:retry")
    public ResultData<Boolean> retry(@PathVariable @Min(1) Long eventId) {
        boolean accepted = outboxService.requestRetry(eventId);
        if (accepted) {
            dispatcher.dispatchOnce();
        }
        return ResultData.data(accepted, accepted ? "已提交重试" : "事件不存在或当前状态不可重试");
    }
}
