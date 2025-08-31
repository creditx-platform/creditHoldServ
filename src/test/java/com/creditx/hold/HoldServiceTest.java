package com.creditx.hold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.creditx.hold.dto.CreateHoldRequest;
import com.creditx.hold.dto.CreateHoldResponse;
import com.creditx.hold.model.Hold;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.repository.HoldRepository;
import com.creditx.hold.service.OutboxEventService;
import com.creditx.hold.service.impl.HoldServiceImpl;

@ExtendWith(MockitoExtension.class)
class HoldServiceTest {

    @Mock
    private HoldRepository holdRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private HoldServiceImpl holdService;

    private CreateHoldRequest validRequest;
    private Hold savedHold;

    @BeforeEach
    void setUp() throws Exception {
        validRequest = CreateHoldRequest.builder()
                .transactionId(100L)
                .issuerAccountId(1L)
                .merchantAccountId(2L)
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .build();

        savedHold = Hold.builder()
                .holdId(999L)
                .transactionId(100L)
                .accountId(1L)
                .amount(new BigDecimal("250.00"))
                .status(HoldStatus.AUTHORIZED)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60)) // 7 days
                .build();
    }

    @Test
    void createHold_success() {
        // Given: No existing hold and valid request
        given(holdRepository.findByTransactionId(100L)).willReturn(Optional.empty());
        given(holdRepository.save(any(Hold.class))).willReturn(savedHold);

        // When: Creating hold
        CreateHoldResponse response = holdService.createHold(validRequest);

        // Then: Response should be successful
        assertThat(response).isNotNull();
        assertThat(response.getHoldId()).isEqualTo(999L);
        assertThat(response.getStatus()).isEqualTo(HoldStatus.AUTHORIZED);

        // Verify hold was saved with correct details
        ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
        verify(holdRepository).save(holdCaptor.capture());
        
        Hold capturedHold = holdCaptor.getValue();
        assertThat(capturedHold.getTransactionId()).isEqualTo(100L);
        assertThat(capturedHold.getAccountId()).isEqualTo(1L);
        assertThat(capturedHold.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(capturedHold.getStatus()).isEqualTo(HoldStatus.AUTHORIZED);
        assertThat(capturedHold.getExpiresAt()).isAfter(Instant.now());

        // Verify outbox event was created
        verify(outboxEventService).saveEvent(eq("hold.created"), eq(999L), anyString());
    }

    @Test
    void createHold_idempotency() {
        // Given: Hold already exists for transaction
        Hold existingHold = Hold.builder()
                .holdId(888L)
                .transactionId(100L)
                .accountId(1L)
                .amount(new BigDecimal("250.00"))
                .status(HoldStatus.AUTHORIZED)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .build();

        given(holdRepository.findByTransactionId(100L)).willReturn(Optional.of(existingHold));

        // When: Creating hold again
        CreateHoldResponse response = holdService.createHold(validRequest);

        // Then: Should return existing hold
        assertThat(response).isNotNull();
        assertThat(response.getHoldId()).isEqualTo(888L);
        assertThat(response.getStatus()).isEqualTo(HoldStatus.AUTHORIZED);

        // Verify no new hold was saved
        verify(holdRepository, never()).save(any(Hold.class));
        verify(outboxEventService, never()).saveEvent(anyString(), any(), anyString());
    }

    @Test
    void createHold_fraudLimitExceeded() {
        // Given: Request with amount exceeding fraud limit
        CreateHoldRequest largeAmountRequest = CreateHoldRequest.builder()
                .transactionId(100L)
                .issuerAccountId(1L)
                .merchantAccountId(2L)
                .amount(new BigDecimal("15000.00")) // Exceeds $10,000 limit
                .currency("USD")
                .build();

        given(holdRepository.findByTransactionId(100L)).willReturn(Optional.empty());

        // When & Then: Should throw exception
        assertThatThrownBy(() -> holdService.createHold(largeAmountRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction amount exceeds fraud limit");

        // Verify no hold was saved
        verify(holdRepository, never()).save(any(Hold.class));
        verify(outboxEventService, never()).saveEvent(anyString(), any(), anyString());
    }

    @Test
    void createHold_verifyOutboxEventPayload() {
        // Given: Valid setup
        given(holdRepository.findByTransactionId(100L)).willReturn(Optional.empty());
        given(holdRepository.save(any(Hold.class))).willReturn(savedHold);

        // When: Creating hold
        holdService.createHold(validRequest);

        // Then: Verify outbox event contains correct payload structure
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxEventService).saveEvent(
                eq("hold.created"), 
                eq(999L), 
                payloadCaptor.capture()
        );

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"holdId\":999");
        assertThat(payload).contains("\"transactionId\":100");
        assertThat(payload).contains("\"issuerAccountId\":1");
        assertThat(payload).contains("\"merchantAccountId\":2");
        assertThat(payload).contains("\"amount\":250.00");
        assertThat(payload).contains("\"currency\":\"USD\"");
        assertThat(payload).contains("\"status\":\"AUTHORIZED\"");
        assertThat(payload).contains("\"expiresAt\"");
    }

    @Test
    void createHold_edgeCaseAmounts() {
        // Given: Request with boundary amount (exactly $10,000)
        CreateHoldRequest boundaryRequest = CreateHoldRequest.builder()
                .transactionId(100L)
                .issuerAccountId(1L)
                .merchantAccountId(2L)
                .amount(new BigDecimal("10000.00")) // Exactly at limit
                .currency("USD")
                .build();

        given(holdRepository.findByTransactionId(100L)).willReturn(Optional.empty());
        given(holdRepository.save(any(Hold.class))).willReturn(savedHold);

        // When: Creating hold
        CreateHoldResponse response = holdService.createHold(boundaryRequest);

        // Then: Should succeed (exactly at limit is allowed)
        assertThat(response).isNotNull();
        assertThat(response.getHoldId()).isEqualTo(999L);
        assertThat(response.getStatus()).isEqualTo(HoldStatus.AUTHORIZED);

        // Verify hold was saved
        verify(holdRepository).save(any(Hold.class));
        verify(outboxEventService).saveEvent(anyString(), any(), anyString());
    }

    @Test
    void createHold_minimumAmount() {
        // Given: Request with very small amount
        CreateHoldRequest minAmountRequest = CreateHoldRequest.builder()
                .transactionId(100L)
                .issuerAccountId(1L)
                .merchantAccountId(2L)
                .amount(new BigDecimal("0.01")) // Minimum amount
                .currency("USD")
                .build();

        given(holdRepository.findByTransactionId(100L)).willReturn(Optional.empty());
        given(holdRepository.save(any(Hold.class))).willReturn(savedHold);

        // When: Creating hold
        CreateHoldResponse response = holdService.createHold(minAmountRequest);

        // Then: Should succeed
        assertThat(response).isNotNull();
        assertThat(response.getHoldId()).isEqualTo(999L);
        assertThat(response.getStatus()).isEqualTo(HoldStatus.AUTHORIZED);

        // Verify hold was saved
        verify(holdRepository).save(any(Hold.class));
        verify(outboxEventService).saveEvent(anyString(), any(), anyString());
    }
}
