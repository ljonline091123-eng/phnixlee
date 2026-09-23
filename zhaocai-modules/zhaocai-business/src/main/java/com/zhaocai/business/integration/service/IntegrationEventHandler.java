package com.zhaocai.business.integration.service;

import com.zhaocai.business.integration.domain.model.IntegrationOutboxEvent;

/** 按事件类型隔离外部投递实现，避免发件箱依赖具体业务模块。 */
public interface IntegrationEventHandler {
    String eventType();

    void deliver(IntegrationOutboxEvent event);
}
