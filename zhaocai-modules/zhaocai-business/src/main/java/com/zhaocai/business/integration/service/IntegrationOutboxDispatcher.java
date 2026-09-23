package com.zhaocai.business.integration.service;

import com.zhaocai.business.integration.domain.model.IntegrationOutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 发件箱后台派发器；单条事件通过条件更新领取，支持多实例并发运行。 */
@Slf4j
@Component
public class IntegrationOutboxDispatcher {
    private final IIntegrationOutboxService outboxService;
    private final Map<String, IntegrationEventHandler> handlers = new HashMap<>();

    public IntegrationOutboxDispatcher(IIntegrationOutboxService outboxService,
                                       List<IntegrationEventHandler> handlerList) {
        this.outboxService = outboxService;
        for (IntegrationEventHandler handler : handlerList) {
            IntegrationEventHandler previous = handlers.put(handler.eventType(), handler);
            if (previous != null) {
                throw new IllegalStateException("重复的集成事件处理器: " + handler.eventType());
            }
        }
    }

    @Scheduled(initialDelayString = "${integration.outbox.dispatch.initial-delay-ms:30000}",
            fixedDelayString = "${integration.outbox.dispatch.fixed-delay-ms:60000}")
    public void scheduledDispatch() {
        dispatchOnce();
    }

    public int dispatchOnce() {
        outboxService.recoverStaleProcessingEvents();
        List<IntegrationOutboxEvent> events = outboxService.findDispatchable(50);
        int published = 0;
        for (IntegrationOutboxEvent event : events) {
            IntegrationEventHandler handler = handlers.get(event.getEventType());
            if (handler == null) {
                failUnsupportedEvent(event);
                continue;
            }
            try {
                if (outboxService.execute(event, () -> handler.deliver(event))) {
                    published++;
                }
            } catch (RuntimeException ex) {
                log.warn("集成事件投递失败, eventId={}, eventType={}, retryCount={}",
                        event.getId(), event.getEventType(), event.getRetryCount(), ex);
            }
        }
        return published;
    }

    private void failUnsupportedEvent(IntegrationOutboxEvent event) {
        try {
            outboxService.execute(event, () -> {
                throw new IllegalStateException("未注册集成事件处理器: " + event.getEventType());
            });
        } catch (RuntimeException ex) {
            log.warn("集成事件缺少处理器, eventId={}, eventType={}", event.getId(), event.getEventType());
        }
    }
}
