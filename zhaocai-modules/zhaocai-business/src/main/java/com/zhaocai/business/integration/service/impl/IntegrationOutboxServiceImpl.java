package com.zhaocai.business.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhaocai.business.integration.domain.model.IntegrationOutboxEvent;
import com.zhaocai.business.integration.mapper.IntegrationOutboxEventMapper;
import com.zhaocai.business.integration.service.IIntegrationOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.util.Date;
import java.util.List;

@Service
public class IntegrationOutboxServiceImpl implements IIntegrationOutboxService {
    private static final int MAX_ERROR_LENGTH = 2000;
    private static final int MAX_RETRIES = 8;
    private static final long PROCESSING_TIMEOUT_MILLIS = 10 * 60_000L;
    private final IntegrationOutboxEventMapper mapper;

    public IntegrationOutboxServiceImpl(IntegrationOutboxEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public IntegrationOutboxEvent enqueue(String aggregateType, String aggregateId, String eventType,
                                          String idempotencyKey, String payload) {
        IntegrationOutboxEvent existing = mapper.selectOne(new LambdaQueryWrapper<IntegrationOutboxEvent>()
                .eq(IntegrationOutboxEvent::getIdempotencyKey, idempotencyKey).last("limit 1"));
        if (existing != null) {
            return existing;
        }
        IntegrationOutboxEvent event = new IntegrationOutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setIdempotencyKey(idempotencyKey);
        event.setPayload(payload);
        event.setStatus(IntegrationOutboxEvent.PENDING);
        event.setRetryCount(0);
        try {
            mapper.insert(event);
            return event;
        } catch (DuplicateKeyException ex) {
            IntegrationOutboxEvent concurrent = mapper.selectOne(new LambdaQueryWrapper<IntegrationOutboxEvent>()
                    .eq(IntegrationOutboxEvent::getIdempotencyKey, idempotencyKey).last("limit 1"));
            if (concurrent != null) {
                return concurrent;
            }
            throw ex;
        }
    }

    @Override
    public boolean execute(IntegrationOutboxEvent event, Runnable delivery) {
        if (Integer.valueOf(IntegrationOutboxEvent.PUBLISHED).equals(event.getStatus())) {
            return true;
        }
        if (Integer.valueOf(IntegrationOutboxEvent.DEAD).equals(event.getStatus()) || !claim(event)) {
            return false;
        }
        event.setStatus(IntegrationOutboxEvent.PROCESSING);
        try {
            delivery.run();
            event.setStatus(IntegrationOutboxEvent.PUBLISHED);
            event.setPublishedAt(new Date());
            event.setLastError(null);
            event.setNextRetryAt(null);
            mapper.updateById(event);
            return true;
        } catch (RuntimeException ex) {
            int retries = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
            event.setRetryCount(retries);
            boolean dead = retries >= MAX_RETRIES;
            event.setStatus(dead ? IntegrationOutboxEvent.DEAD : IntegrationOutboxEvent.FAILED);
            event.setNextRetryAt(dead ? null : new Date(System.currentTimeMillis() + retryDelayMillis(retries)));
            String message = ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage();
            event.setLastError(message.substring(0, Math.min(message.length(), MAX_ERROR_LENGTH)));
            mapper.updateById(event);
            throw ex;
        }
    }

    @Override
    public List<IntegrationOutboxEvent> findDispatchable(int batchSize) {
        int limit = Math.max(1, Math.min(batchSize, 200));
        Date now = new Date();
        return mapper.selectList(new LambdaQueryWrapper<IntegrationOutboxEvent>()
                .and(query -> query.eq(IntegrationOutboxEvent::getStatus, IntegrationOutboxEvent.PENDING)
                        .or(failed -> failed.eq(IntegrationOutboxEvent::getStatus, IntegrationOutboxEvent.FAILED)
                                .and(due -> due.isNull(IntegrationOutboxEvent::getNextRetryAt)
                                        .or().le(IntegrationOutboxEvent::getNextRetryAt, now))))
                .orderByAsc(IntegrationOutboxEvent::getCreateTime)
                .last("limit " + limit));
    }

    @Override
    public List<IntegrationOutboxEvent> listRecent(Integer status, int limit) {
        LambdaQueryWrapper<IntegrationOutboxEvent> query = new LambdaQueryWrapper<>();
        if (status != null) {
            query.eq(IntegrationOutboxEvent::getStatus, status);
        }
        return mapper.selectList(query.orderByDesc(IntegrationOutboxEvent::getCreateTime)
                .last("limit " + Math.max(1, Math.min(limit, 200))));
    }

    @Override
    public boolean requestRetry(Long eventId) {
        return mapper.update(null, new LambdaUpdateWrapper<IntegrationOutboxEvent>()
                .eq(IntegrationOutboxEvent::getId, eventId)
                .in(IntegrationOutboxEvent::getStatus, IntegrationOutboxEvent.FAILED, IntegrationOutboxEvent.DEAD)
                .set(IntegrationOutboxEvent::getStatus, IntegrationOutboxEvent.PENDING)
                .set(IntegrationOutboxEvent::getRetryCount, 0)
                .set(IntegrationOutboxEvent::getNextRetryAt, null)
                .set(IntegrationOutboxEvent::getLastError, null)
                .set(IntegrationOutboxEvent::getUpdateTime, new Date())) == 1;
    }

    @Override
    public int recoverStaleProcessingEvents() {
        Date now = new Date();
        Date deadline = new Date(now.getTime() - PROCESSING_TIMEOUT_MILLIS);
        return mapper.update(null, new LambdaUpdateWrapper<IntegrationOutboxEvent>()
                .eq(IntegrationOutboxEvent::getStatus, IntegrationOutboxEvent.PROCESSING)
                .and(query -> query.isNull(IntegrationOutboxEvent::getUpdateTime)
                        .or().le(IntegrationOutboxEvent::getUpdateTime, deadline))
                .set(IntegrationOutboxEvent::getStatus, IntegrationOutboxEvent.FAILED)
                .set(IntegrationOutboxEvent::getNextRetryAt, now)
                .set(IntegrationOutboxEvent::getLastError, "投递进程超时，已自动恢复")
                .set(IntegrationOutboxEvent::getUpdateTime, now));
    }

    private boolean claim(IntegrationOutboxEvent event) {
        Date now = new Date();
        return mapper.update(null, new LambdaUpdateWrapper<IntegrationOutboxEvent>()
                .eq(IntegrationOutboxEvent::getId, event.getId())
                .in(IntegrationOutboxEvent::getStatus, IntegrationOutboxEvent.PENDING, IntegrationOutboxEvent.FAILED)
                .lt(IntegrationOutboxEvent::getRetryCount, MAX_RETRIES)
                .set(IntegrationOutboxEvent::getStatus, IntegrationOutboxEvent.PROCESSING)
                .set(IntegrationOutboxEvent::getUpdateTime, now)) == 1;
    }

    private long retryDelayMillis(int retries) {
        return Math.min(60L, 1L << Math.min(retries, 6)) * 60_000L;
    }
}
