package com.creditx.hold.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.creditx.hold.constants.EventTypes;
import com.creditx.hold.dto.TransactionAuthorizedEvent;
import com.creditx.hold.dto.TransactionFailedEvent;
import com.creditx.hold.dto.TransactionPostedEvent;
import com.creditx.hold.service.TransactionEventService;
import com.creditx.hold.tracing.TransactionSpanTagger;
import com.creditx.hold.util.EventValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class TransactionEventListenerTest {

  @Mock
  private TransactionEventService transactionEventService;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private TransactionSpanTagger transactionSpanTagger;

  private TransactionEventListener transactionEventListener;

  private Consumer<Message<String>> transactionAuthorizedConsumer;
  private Consumer<Message<String>> transactionPostedConsumer;
  private Consumer<Message<String>> transactionFailedConsumer;

  @BeforeEach
  void setUp() {
    transactionEventListener = new TransactionEventListener(transactionEventService,
        transactionSpanTagger, objectMapper);
    transactionAuthorizedConsumer = transactionEventListener.transactionAuthorized();
    transactionPostedConsumer = transactionEventListener.transactionPosted();
    transactionFailedConsumer = transactionEventListener.transactionFailed();
  }

  @Test
  void shouldProcessValidTransactionAuthorizedEvent() throws Exception {
    // given
    String payload = "{\"transactionId\":123,\"holdId\":456}";

    Message<String> message = MessageBuilder.withPayload(payload)
        .setHeader("eventType", EventTypes.TRANSACTION_AUTHORIZED).build();

    TransactionAuthorizedEvent event = new TransactionAuthorizedEvent();
    event.setTransactionId(123L);
    event.setHoldId(456L);

    try (MockedStatic<EventValidationUtils> mockedUtils = Mockito.mockStatic(
        EventValidationUtils.class)) {
      mockedUtils.when(
              () -> EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_AUTHORIZED))
          .thenReturn(true);

      when(objectMapper.readValue(payload, TransactionAuthorizedEvent.class)).thenReturn(event);

      // when
      transactionAuthorizedConsumer.accept(message);

      // then
      verify(transactionEventService, times(1)).processTransactionAuthorized(event);
      // Tagger should receive tag call
      verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
    }
  }

  @Test
  void shouldSkipTransactionAuthorizedEventWithoutHoldId() throws Exception {
    // given
    String payload = "{\"transactionId\":123}";

    Message<String> message = MessageBuilder.withPayload(payload)
        .setHeader("eventType", EventTypes.TRANSACTION_AUTHORIZED).build();

    TransactionAuthorizedEvent event = new TransactionAuthorizedEvent();
    event.setTransactionId(123L);
    event.setHoldId(null);

    try (MockedStatic<EventValidationUtils> mockedUtils = Mockito.mockStatic(
        EventValidationUtils.class)) {
      mockedUtils.when(
              () -> EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_AUTHORIZED))
          .thenReturn(true);

      when(objectMapper.readValue(payload, TransactionAuthorizedEvent.class)).thenReturn(event);

      // when
      transactionAuthorizedConsumer.accept(message);

      // then
      verify(transactionEventService, never()).processTransactionAuthorized(event);
      // Tag still added because transactionId present
      verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
    }
  }

  @Test
  void shouldProcessValidTransactionPostedEvent() throws Exception {
    // given
    String payload = "{\"transactionId\":123,\"holdId\":456}";

    Message<String> message = MessageBuilder.withPayload(payload)
        .setHeader("eventType", EventTypes.TRANSACTION_POSTED).build();

    TransactionPostedEvent event = new TransactionPostedEvent();
    event.setTransactionId(123L);
    event.setHoldId(456L);

    try (MockedStatic<EventValidationUtils> mockedUtils = Mockito.mockStatic(
        EventValidationUtils.class)) {
      mockedUtils.when(
              () -> EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_POSTED))
          .thenReturn(true);

      when(objectMapper.readValue(payload, TransactionPostedEvent.class)).thenReturn(event);

      // when
      transactionPostedConsumer.accept(message);

      // then
      verify(transactionEventService, times(1)).processTransactionPosted(event);
      verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
    }
  }

  @Test
  void shouldProcessValidTransactionFailedEvent() throws Exception {
    // given
    String payload = "{\"transactionId\":123,\"holdId\":456}";

    Message<String> message = MessageBuilder.withPayload(payload)
        .setHeader("eventType", EventTypes.TRANSACTION_FAILED).build();

    TransactionFailedEvent event = new TransactionFailedEvent();
    event.setTransactionId(123L);
    event.setHoldId(456L);

    try (MockedStatic<EventValidationUtils> mockedUtils = Mockito.mockStatic(
        EventValidationUtils.class)) {
      mockedUtils.when(
              () -> EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_FAILED))
          .thenReturn(true);

      when(objectMapper.readValue(payload, TransactionFailedEvent.class)).thenReturn(event);

      // when
      transactionFailedConsumer.accept(message);

      // then
      verify(transactionEventService, times(1)).processTransactionFailed(event);
      verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
    }
  }

  @Test
  void shouldSkipInvalidEventTypes() throws Exception {
    // given
    String payload = "{\"transactionId\":123}";

    Message<String> message = MessageBuilder.withPayload(payload)
        .setHeader("eventType", EventTypes.TRANSACTION_AUTHORIZED).build();

    try (MockedStatic<EventValidationUtils> mockedUtils = Mockito.mockStatic(
        EventValidationUtils.class)) {
      mockedUtils.when(
              () -> EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_AUTHORIZED))
          .thenReturn(false);

      // when
      transactionAuthorizedConsumer.accept(message);

      // then
      verify(transactionEventService, never()).processTransactionAuthorized(any());
      verify(objectMapper, never()).readValue(any(String.class), any(Class.class));
      verifyNoInteractions(transactionSpanTagger);
    }
  }

  @Test
  void shouldThrowExceptionOnJsonProcessingError() throws Exception {
    // given
    String payload = "invalid json";

    Message<String> message = MessageBuilder.withPayload(payload)
        .setHeader("eventType", EventTypes.TRANSACTION_AUTHORIZED).build();

    try (MockedStatic<EventValidationUtils> mockedUtils = Mockito.mockStatic(
        EventValidationUtils.class)) {
      mockedUtils.when(
              () -> EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_AUTHORIZED))
          .thenReturn(true);

      when(objectMapper.readValue(payload, TransactionAuthorizedEvent.class)).thenThrow(
          new RuntimeException("JSON processing error"));

      // when & then
      assertThrows(RuntimeException.class, () -> transactionAuthorizedConsumer.accept(message));

      verify(transactionEventService, never()).processTransactionAuthorized(any());
      // Tagger not invoked because JSON parsing failed before tag call
      verifyNoInteractions(transactionSpanTagger);
    }
  }
}
