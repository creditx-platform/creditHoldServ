package com.creditx.hold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.creditx.hold.dto.TransactionAuthorizedEvent;
import com.creditx.hold.model.Hold;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.repository.HoldRepository;
import com.creditx.hold.service.ProcessedEventService;
import com.creditx.hold.service.impl.TransactionEventServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionEventServiceTest {

    @Mock
    private HoldRepository holdRepository;
    
    @Mock
    private ProcessedEventService processedEventService;
    
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionEventServiceImpl transactionEventService;

    private TransactionAuthorizedEvent transactionAuthorizedEvent;
    private Hold hold;

    @BeforeEach
    void setUp() {
        transactionAuthorizedEvent = TransactionAuthorizedEvent.builder()
                .transactionId(999L)
                .holdId(12345L)
                .issuerAccountId(1L)
                .merchantAccountId(2L)
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .status("AUTHORIZED")
                .build();

        hold = Hold.builder()
                .holdId(12345L)
                .transactionId(999L)
                .accountId(1L)
                .amount(new BigDecimal("250.00"))
                .status(HoldStatus.AUTHORIZED)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .build();
    }

    @Test
    void processTransactionAuthorized_success() throws JsonProcessingException {
        // Given: Valid event and existing hold, no deduplication
        given(holdRepository.findById(12345L)).willReturn(Optional.of(hold));
        given(processedEventService.isEventProcessed(any())).willReturn(false);
        given(processedEventService.isPayloadProcessed(any())).willReturn(false);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"test\":\"payload\"}");

        // When: Processing transaction authorized event
        transactionEventService.processTransactionAuthorized(transactionAuthorizedEvent);

        // Then: Hold status should be updated to CAPTURED
        ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
        verify(holdRepository).save(holdCaptor.capture());
        
        Hold updatedHold = holdCaptor.getValue();
        assertThat(updatedHold.getStatus()).isEqualTo(HoldStatus.CAPTURED);
        assertThat(updatedHold.getHoldId()).isEqualTo(12345L);
        assertThat(updatedHold.getTransactionId()).isEqualTo(999L);
        
        // Verify event was marked as processed
        verify(processedEventService).markEventAsProcessed(any(), any(), eq("SUCCESS"));
    }
    
    @Test
    void processTransactionAuthorized_skipsWhenEventAlreadyProcessed() throws JsonProcessingException {
        // Given: Event has already been processed
        given(processedEventService.isEventProcessed(any())).willReturn(true);

        // When: Processing transaction authorized event
        transactionEventService.processTransactionAuthorized(transactionAuthorizedEvent);

        // Then: Should skip processing and not update hold
        verify(holdRepository, never()).save(any());
        verify(processedEventService, never()).markEventAsProcessed(any(), any(), any());
    }
    
    @Test
    void processTransactionAuthorized_skipsWhenPayloadAlreadyProcessed() throws JsonProcessingException {
        // Given: Event not processed but payload has been processed
        given(processedEventService.isEventProcessed(any())).willReturn(false);
        given(processedEventService.isPayloadProcessed(any())).willReturn(true);

        // When: Processing transaction authorized event
        transactionEventService.processTransactionAuthorized(transactionAuthorizedEvent);

        // Then: Should skip processing and not update hold
        verify(holdRepository, never()).save(any());
        verify(processedEventService, never()).markEventAsProcessed(any(), any(), any());
    }

    @Test
    void processTransactionAuthorized_holdNotFound() throws JsonProcessingException {
        // Given: Hold doesn't exist, but deduplication checks pass
        given(processedEventService.isEventProcessed(any())).willReturn(false);
        given(processedEventService.isPayloadProcessed(any())).willReturn(false);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"test\":\"payload\"}");
        given(holdRepository.findById(12345L)).willReturn(Optional.empty());

        // When & Then: Should throw exception
        assertThatThrownBy(() -> transactionEventService.processTransactionAuthorized(transactionAuthorizedEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hold not found: 12345");
    }

    @Test
    void processTransactionAuthorized_holdAlreadyCaptured() throws JsonProcessingException {
        // Given: Hold is already captured, but deduplication checks pass
        Hold capturedHold = Hold.builder()
                .holdId(12345L)
                .transactionId(999L)
                .accountId(1L)
                .amount(new BigDecimal("250.00"))
                .status(HoldStatus.CAPTURED) // Already captured
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .build();

        given(processedEventService.isEventProcessed(any())).willReturn(false);
        given(processedEventService.isPayloadProcessed(any())).willReturn(false);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"test\":\"payload\"}");
        given(holdRepository.findById(12345L)).willReturn(Optional.of(capturedHold));

        // When: Processing transaction authorized event
        transactionEventService.processTransactionAuthorized(transactionAuthorizedEvent);

        // Then: Hold status should still be CAPTURED (no change)
        ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
        verify(holdRepository).save(holdCaptor.capture());
        
        Hold updatedHold = holdCaptor.getValue();
        assertThat(updatedHold.getStatus()).isEqualTo(HoldStatus.CAPTURED);
    }

    @Test
    void processTransactionAuthorized_voidedHoldBecomesAuthorized() throws JsonProcessingException {
        // Given: Hold was previously voided but now authorized, deduplication checks pass
        Hold voidedHold = Hold.builder()
                .holdId(12345L)
                .transactionId(999L)
                .accountId(1L)
                .amount(new BigDecimal("250.00"))
                .status(HoldStatus.VOIDED)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .build();

        given(processedEventService.isEventProcessed(any())).willReturn(false);
        given(processedEventService.isPayloadProcessed(any())).willReturn(false);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"test\":\"payload\"}");
        given(holdRepository.findById(12345L)).willReturn(Optional.of(voidedHold));

        // When: Processing transaction authorized event
        transactionEventService.processTransactionAuthorized(transactionAuthorizedEvent);

        // Then: Hold status should be updated to CAPTURED
        ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
        verify(holdRepository).save(holdCaptor.capture());
        
        Hold updatedHold = holdCaptor.getValue();
        assertThat(updatedHold.getStatus()).isEqualTo(HoldStatus.CAPTURED);
    }

    @Test
    void processTransactionAuthorized_preservesOtherFields() throws JsonProcessingException {
        // Given: Valid event and existing hold, deduplication checks pass
        given(holdRepository.findById(12345L)).willReturn(Optional.of(hold));
        given(processedEventService.isEventProcessed(any())).willReturn(false);
        given(processedEventService.isPayloadProcessed(any())).willReturn(false);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"test\":\"payload\"}");

        // When: Processing transaction authorized event
        transactionEventService.processTransactionAuthorized(transactionAuthorizedEvent);

        // Then: Only status should change, other fields preserved
        ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
        verify(holdRepository).save(holdCaptor.capture());
        
        Hold updatedHold = holdCaptor.getValue();
        assertThat(updatedHold.getStatus()).isEqualTo(HoldStatus.CAPTURED);
        assertThat(updatedHold.getHoldId()).isEqualTo(hold.getHoldId());
        assertThat(updatedHold.getTransactionId()).isEqualTo(hold.getTransactionId());
        assertThat(updatedHold.getAccountId()).isEqualTo(hold.getAccountId());
        assertThat(updatedHold.getAmount()).isEqualByComparingTo(hold.getAmount());
        assertThat(updatedHold.getExpiresAt()).isEqualTo(hold.getExpiresAt());
    }
}
