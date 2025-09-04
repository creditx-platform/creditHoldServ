package com.creditx.hold.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.creditx.hold.dto.TransactionAuthorizedEvent;
import com.creditx.hold.dto.TransactionFailedEvent;
import com.creditx.hold.dto.TransactionPostedEvent;
import com.creditx.hold.model.Hold;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.repository.HoldRepository;
import com.creditx.hold.service.ProcessedEventService;
import com.creditx.hold.util.EventIdGenerator;
import com.creditx.hold.tracing.TransactionSpanTagger;

@ExtendWith(MockitoExtension.class)
class TransactionEventServiceImplTest {

    @Mock
    private HoldRepository holdRepository;

    @Mock
    private ProcessedEventService processedEventService;

    @InjectMocks
    private TransactionEventServiceImpl transactionEventService;

    @Mock
    private TransactionSpanTagger transactionSpanTagger;

    @Test
    void shouldProcessTransactionAuthorizedEvent() {
        // given
        TransactionAuthorizedEvent event = createTransactionAuthorizedEvent(123L, 456L);
        String eventId = "transaction.authorized-123";
        String payloadHash = "hash123";
        Hold hold = createHold(456L, HoldStatus.AUTHORIZED);

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.authorized", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenReturn(payloadHash);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(processedEventService.isPayloadProcessed(payloadHash)).thenReturn(false);
            when(holdRepository.findById(456L)).thenReturn(Optional.of(hold));

            // when
            transactionEventService.processTransactionAuthorized(event);

            // then
            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).isEventProcessed(eventId);
            verify(processedEventService, times(1)).isPayloadProcessed(payloadHash);
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, payloadHash, "SUCCESS");
            
            ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
            verify(holdRepository, times(1)).save(holdCaptor.capture());
            
            Hold savedHold = holdCaptor.getValue();
            assertThat(savedHold.getStatus()).isEqualTo(HoldStatus.CAPTURED);
        }
    }

    @Test
    void shouldSkipProcessingWhenEventAlreadyProcessed() {
        // given
        TransactionAuthorizedEvent event = createTransactionAuthorizedEvent(123L, 456L);
        String eventId = "transaction.authorized-123";

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.authorized", 123L))
                    .thenReturn(eventId);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(true);

            // when
            transactionEventService.processTransactionAuthorized(event);

            // then
            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).isEventProcessed(eventId);
            verify(processedEventService, never()).isPayloadProcessed(anyString());
            verify(processedEventService, never()).markEventAsProcessed(anyString(), anyString(), anyString());
            verify(holdRepository, never()).findById(any());
        }
    }

    @Test
    void shouldSkipProcessingWhenPayloadAlreadyProcessed() {
        // given
        TransactionAuthorizedEvent event = createTransactionAuthorizedEvent(123L, 456L);
        String eventId = "transaction.authorized-123";
        String payloadHash = "hash123";

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.authorized", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenReturn(payloadHash);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(processedEventService.isPayloadProcessed(payloadHash)).thenReturn(true);

            // when
            transactionEventService.processTransactionAuthorized(event);

            // then
            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).isEventProcessed(eventId);
            verify(processedEventService, times(1)).isPayloadProcessed(payloadHash);
            verify(processedEventService, never()).markEventAsProcessed(anyString(), anyString(), anyString());
            verify(holdRepository, never()).findById(any());
        }
    }

    @Test
    void shouldThrowExceptionWhenHoldNotFound() {
        // given
        TransactionAuthorizedEvent event = createTransactionAuthorizedEvent(123L, 456L);
        String eventId = "transaction.authorized-123";
        String payloadHash = "hash123";

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.authorized", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenReturn(payloadHash);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(processedEventService.isPayloadProcessed(payloadHash)).thenReturn(false);
            when(holdRepository.findById(456L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> transactionEventService.processTransactionAuthorized(event))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Hold not found: 456");

            verify(processedEventService, times(1)).markEventAsProcessed(eventId, "", "FAILED");
        }
    }

    @Test
    void shouldProcessTransactionPostedEvent() {
        // given
        TransactionPostedEvent event = createTransactionPostedEvent(123L, 456L);
        String eventId = "transaction.posted-123";
        String payloadHash = "hash123";
        Hold hold = createHold(456L, HoldStatus.AUTHORIZED);

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.posted", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenReturn(payloadHash);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(processedEventService.isPayloadProcessed(payloadHash)).thenReturn(false);
            when(holdRepository.findById(456L)).thenReturn(Optional.of(hold));

            // when
            transactionEventService.processTransactionPosted(event);

            // then
            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).isEventProcessed(eventId);
            verify(processedEventService, times(1)).isPayloadProcessed(payloadHash);
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, payloadHash, "SUCCESS");
            
            ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
            verify(holdRepository, times(1)).save(holdCaptor.capture());
            
            Hold savedHold = holdCaptor.getValue();
            assertThat(savedHold.getStatus()).isEqualTo(HoldStatus.CAPTURED);
        }
    }

    @Test
    void shouldNotUpdateHoldWhenAlreadyCapturedForPostedEvent() {
        // given
        TransactionPostedEvent event = createTransactionPostedEvent(123L, 456L);
        String eventId = "transaction.posted-123";
        String payloadHash = "hash123";
        Hold hold = createHold(456L, HoldStatus.CAPTURED);

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.posted", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenReturn(payloadHash);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(processedEventService.isPayloadProcessed(payloadHash)).thenReturn(false);
            when(holdRepository.findById(456L)).thenReturn(Optional.of(hold));

            // when
            transactionEventService.processTransactionPosted(event);

            // then
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, payloadHash, "SUCCESS");
            verify(holdRepository, never()).save(any());
            assertThat(hold.getStatus()).isEqualTo(HoldStatus.CAPTURED);
        }
    }

    @Test
    void shouldProcessTransactionFailedEvent() {
        // given
        TransactionFailedEvent event = createTransactionFailedEvent(123L, 456L);
        String eventId = "transaction.failed-123";
        String payloadHash = "hash123";
        Hold hold = createHold(456L, HoldStatus.AUTHORIZED);

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.failed", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenReturn(payloadHash);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(processedEventService.isPayloadProcessed(payloadHash)).thenReturn(false);
            when(holdRepository.findById(456L)).thenReturn(Optional.of(hold));

            // when
            transactionEventService.processTransactionFailed(event);

            // then
            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).isEventProcessed(eventId);
            verify(processedEventService, times(1)).isPayloadProcessed(payloadHash);
            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, payloadHash, "SUCCESS");
            
            ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
            verify(holdRepository, times(1)).save(holdCaptor.capture());
            
            Hold savedHold = holdCaptor.getValue();
            assertThat(savedHold.getStatus()).isEqualTo(HoldStatus.VOIDED);
        }
    }

    @Test
    void shouldNotUpdateHoldWhenAlreadyVoidedForFailedEvent() {
        // given
        TransactionFailedEvent event = createTransactionFailedEvent(123L, 456L);
        String eventId = "transaction.failed-123";
        String payloadHash = "hash123";
        Hold hold = createHold(456L, HoldStatus.VOIDED);

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.failed", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenReturn(payloadHash);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(processedEventService.isPayloadProcessed(payloadHash)).thenReturn(false);
            when(holdRepository.findById(456L)).thenReturn(Optional.of(hold));

            // when
            transactionEventService.processTransactionFailed(event);

            // then
            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, payloadHash, "SUCCESS");
            verify(holdRepository, never()).save(any());
            assertThat(hold.getStatus()).isEqualTo(HoldStatus.VOIDED);
        }
    }

    @Test
    void shouldNotUpdateHoldWhenExpiredForFailedEvent() {
        // given
        TransactionFailedEvent event = createTransactionFailedEvent(123L, 456L);
        String eventId = "transaction.failed-123";
        String payloadHash = "hash123";
        Hold hold = createHold(456L, HoldStatus.EXPIRED);

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.failed", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenReturn(payloadHash);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(processedEventService.isPayloadProcessed(payloadHash)).thenReturn(false);
            when(holdRepository.findById(456L)).thenReturn(Optional.of(hold));

            // when
            transactionEventService.processTransactionFailed(event);

            // then
            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, payloadHash, "SUCCESS");
            verify(holdRepository, never()).save(any());
            assertThat(hold.getStatus()).isEqualTo(HoldStatus.EXPIRED);
        }
    }

    @Test
    void shouldMarkAsFailedWhenRepositoryThrowsException() {
        // given
        TransactionAuthorizedEvent event = createTransactionAuthorizedEvent(123L, 456L);
        String eventId = "transaction.authorized-123";
        String payloadHash = "hash123";

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.authorized", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenReturn(payloadHash);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(processedEventService.isPayloadProcessed(payloadHash)).thenReturn(false);
            doThrow(new RuntimeException("Database error"))
                    .when(holdRepository).findById(456L);

            // when & then
            assertThatThrownBy(() -> transactionEventService.processTransactionAuthorized(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");

            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, "", "FAILED");
        }
    }

    @Test
    void shouldMarkAsFailedWhenPayloadHashGenerationFails() {
        // given
        TransactionAuthorizedEvent event = createTransactionAuthorizedEvent(123L, 456L);
        String eventId = "transaction.authorized-123";

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.authorized", 123L))
                    .thenReturn(eventId);
            mockedGenerator.when(() -> EventIdGenerator.generatePayloadHash(anyString()))
                    .thenThrow(new RuntimeException("Hash generation failed"));

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> transactionEventService.processTransactionAuthorized(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Hash generation failed");            verify(processedEventService, times(1)).isEventProcessed(eventId);
            verify(processedEventService, never()).isPayloadProcessed(anyString());
            verify(transactionSpanTagger, times(1)).tagTransactionId(123L);
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, "", "FAILED");
            verify(holdRepository, never()).findById(any());
        }
    }

    private TransactionAuthorizedEvent createTransactionAuthorizedEvent(Long transactionId, Long holdId) {
        TransactionAuthorizedEvent event = new TransactionAuthorizedEvent();
        event.setTransactionId(transactionId);
        event.setHoldId(holdId);
        return event;
    }

    private TransactionPostedEvent createTransactionPostedEvent(Long transactionId, Long holdId) {
        TransactionPostedEvent event = new TransactionPostedEvent();
        event.setTransactionId(transactionId);
        event.setHoldId(holdId);
        return event;
    }

    private TransactionFailedEvent createTransactionFailedEvent(Long transactionId, Long holdId) {
        TransactionFailedEvent event = new TransactionFailedEvent();
        event.setTransactionId(transactionId);
        event.setHoldId(holdId);
        return event;
    }

    private Hold createHold(Long holdId, HoldStatus status) {
        Hold hold = new Hold();
        hold.setHoldId(holdId);
        hold.setStatus(status);
        return hold;
    }
}
