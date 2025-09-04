package com.creditx.hold.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxStreamPublisherTest {

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private OutboxStreamPublisher outboxStreamPublisher;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(outboxStreamPublisher, "bindingName", "hold-events-out");
    }

    @Test
    void shouldPublishWithKeyAndPayload() {
        // given
        String key = "hold-123";
        String payload = "{\"holdId\":123, \"status\":\"AUTHORIZED\"}";
        String eventType = "hold.created";
        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        // when
        outboxStreamPublisher.publish(key, payload, eventType);
        
        // then
        verify(streamBridge, times(1)).send(eq("hold-events-out"), messageCaptor.capture());
        Message<String> sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getPayload()).isEqualTo(payload);
        assertThat(sentMessage.getHeaders().get("key")).isEqualTo(key);
        assertThat(sentMessage.getHeaders().get("eventType")).isEqualTo(eventType);
    }

    @Test
    void shouldNotPublishWithoutKey() {
        // given
        String key = null;
        String payload = "{\"holdId\":123, \"status\":\"AUTHORIZED\"}";
        String eventType = "hold.created";
        
        // when
        outboxStreamPublisher.publish(key, payload, eventType);
        
        // then
        verify(streamBridge, never()).send(anyString(), anyString());
    }

    @Test
    void shouldNotPublishWithoutPayload() {
        // given
        String key = "hold-123";
        String payload = "";
        String eventType = "hold.created";
        
        // when
        outboxStreamPublisher.publish(key, payload, eventType);
        
        // then
        verify(streamBridge, never()).send(anyString(), anyString());
    }

    @Test
    void shouldNotPublishWithoutEventType() {
        // given
        String key = "hold-123";
        String payload = "{\"holdId\":123, \"status\":\"AUTHORIZED\"}";
        String eventType = "";
        
        // when
        outboxStreamPublisher.publish(key, payload, eventType);
        
        // then
        verify(streamBridge, never()).send(anyString(), anyString());
    }
}