package com.zhaocai.business.integration.service;

import com.zhaocai.business.integration.domain.model.IntegrationOutboxEvent;

import java.util.List;

public interface IIntegrationOutboxService {
    IntegrationOutboxEvent enqueue(String aggregateType, String aggregateId, String eventType,
                                   String idempotencyKey, String payload);

    boolean execute(IntegrationOutboxEvent event, Runnable delivery);

    List<IntegrationOutboxEvent> findDispatchable(int batchSize);

    List<IntegrationOutboxEvent> listRecent(Integer status, int limit);

    boolean requestRetry(Long eventId);

    int recoverStaleProcessingEvents();
}
