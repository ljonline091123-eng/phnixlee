package com.zhaocai.business.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zhaocai.business.integration.domain.model.IntegrationOutboxEvent;
import com.zhaocai.business.integration.mapper.IntegrationOutboxEventMapper;
import org.junit.Test;
import org.junit.BeforeClass;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IntegrationOutboxServiceImplTest {

    @BeforeClass
    public static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "unit-test"),
                IntegrationOutboxEvent.class);
    }

    @Test
    public void successfulDeliveryShouldPublishEvent() {
        IntegrationOutboxEventMapper mapper = mock(IntegrationOutboxEventMapper.class);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        IntegrationOutboxServiceImpl service = new IntegrationOutboxServiceImpl(mapper);
        IntegrationOutboxEvent event = pendingEvent(1L, 0);

        assertTrue(service.execute(event, () -> { }));

        assertEquals(Integer.valueOf(IntegrationOutboxEvent.PUBLISHED), event.getStatus());
        assertTrue(event.getPublishedAt() != null);
        verify(mapper).updateById(event);
    }

    @Test
    public void eighthFailureShouldMoveEventToDeadLetter() {
        IntegrationOutboxEventMapper mapper = mock(IntegrationOutboxEventMapper.class);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        IntegrationOutboxServiceImpl service = new IntegrationOutboxServiceImpl(mapper);
        IntegrationOutboxEvent event = pendingEvent(2L, 7);

        try {
            service.execute(event, () -> {
                throw new IllegalStateException("external service unavailable");
            });
            fail("delivery failure should be propagated");
        } catch (IllegalStateException expected) {
            assertEquals("external service unavailable", expected.getMessage());
        }

        assertEquals(Integer.valueOf(IntegrationOutboxEvent.DEAD), event.getStatus());
        assertEquals(Integer.valueOf(8), event.getRetryCount());
        assertNull(event.getNextRetryAt());
    }

    private IntegrationOutboxEvent pendingEvent(Long id, int retryCount) {
        IntegrationOutboxEvent event = new IntegrationOutboxEvent();
        event.setId(id);
        event.setStatus(IntegrationOutboxEvent.PENDING);
        event.setRetryCount(retryCount);
        return event;
    }
}
