package com.creditx.hold.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.creditx.hold.messaging.OutboxStreamPublisher;
import com.creditx.hold.model.OutboxEvent;
import com.creditx.hold.service.OutboxEventService;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublishingSchedulerTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private OutboxStreamPublisher outboxStreamPublisher;

    @InjectMocks
    private OutboxEventPublishingScheduler outboxEventPublishingScheduler;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(outboxEventPublishingScheduler, "batchSize", 10);
    }

    @Test
    void shouldPublishPendingEvents() {
        // given
        OutboxEvent event1 = createOutboxEvent(123L, "{\"holdId\":123}");
        OutboxEvent event2 = createOutboxEvent(456L, "{\"holdId\":456}");

        List<OutboxEvent> events = Arrays.asList(event1, event2);

        when(outboxEventService.fetchPendingEvents(10)).thenReturn(events);

        // when
        outboxEventPublishingScheduler.publishPendingEvents();

        // then
        verify(outboxStreamPublisher, times(1)).publish("123", "{\"holdId\":123}");
        verify(outboxStreamPublisher, times(1)).publish("456", "{\"holdId\":456}");
        verify(outboxEventService, times(1)).markAsPublished(event1);
        verify(outboxEventService, times(1)).markAsPublished(event2);
    }

    @Test
    void shouldNotPublishWhenNoPendingEvents() {
        // given
        when(outboxEventService.fetchPendingEvents(10)).thenReturn(Collections.emptyList());

        // when
        outboxEventPublishingScheduler.publishPendingEvents();

        // then
        verify(outboxStreamPublisher, never()).publish(any(), any());
        verify(outboxEventService, never()).markAsPublished(any());
        verify(outboxEventService, never()).markAsFailed(any());
    }

    @Test
    void shouldMarkAsFailedWhenPublishingFails() {
        // given
        OutboxEvent event = createOutboxEvent(123L, "{\"holdId\":123}");

        when(outboxEventService.fetchPendingEvents(10)).thenReturn(Arrays.asList(event));
        doThrow(new RuntimeException("Publishing failed")).when(outboxStreamPublisher)
                .publish("123", "{\"holdId\":123}");

        // when
        outboxEventPublishingScheduler.publishPendingEvents();

        // then
        verify(outboxStreamPublisher, times(1)).publish("123", "{\"holdId\":123}");
        verify(outboxEventService, never()).markAsPublished(event);
        verify(outboxEventService, times(1)).markAsFailed(event);
    }

    private OutboxEvent createOutboxEvent(Long aggregateId, String payload) {
        return OutboxEvent.builder()
                .aggregateId(aggregateId)
                .payload(payload)
                .build();
    }
}