package com.zhaocai.business.integration.procurement;

import com.alibaba.fastjson.JSONObject;
import com.zhaocai.business.integration.domain.model.IntegrationOutboxEvent;
import com.zhaocai.business.integration.service.IntegrationEventHandler;
import com.zhaocai.business.manager.template.config.UnderlingPlatformConfig;
import com.zhaocai.business.procurement.service.IProcurementPlanService;
import org.springframework.stereotype.Component;

@Component
public class ContractPlanningPushEventHandler implements IntegrationEventHandler {
    public static final String EVENT_TYPE = "ContractPlanningPushed";

    private final IProcurementPlanService procurementPlanService;
    private final UnderlingPlatformConfig platformConfig;

    public ContractPlanningPushEventHandler(IProcurementPlanService procurementPlanService,
                                            UnderlingPlatformConfig platformConfig) {
        this.procurementPlanService = procurementPlanService;
        this.platformConfig = platformConfig;
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public void deliver(IntegrationOutboxEvent event) {
        ContractPlanningPushPayload payload = JSONObject.parseObject(
                event.getPayload(), ContractPlanningPushPayload.class);
        if (payload == null || payload.getPlan() == null) {
            throw new IllegalArgumentException("合约规划推送事件缺少业务载荷");
        }
        if (payload.getAuthorization() == null || payload.getAuthorization().trim().isEmpty()) {
            payload.setAuthorization(platformConfig.getAuthCode());
        }
        procurementPlanService.deliverProcurementPlanPush(payload);
    }
}
