package com.creditx.hold.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creditx.hold.dto.CreateHoldRequest;
import com.creditx.hold.dto.CreateHoldResponse;
import com.creditx.hold.model.Hold;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.repository.HoldRepository;
import com.creditx.hold.service.OutboxEventService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HoldServiceImplTest {

  @Mock
  private HoldRepository holdRepository;

  @Mock
  private OutboxEventService outboxEventService;

  @InjectMocks
  private HoldServiceImpl holdService;

  @BeforeEach
  void setup() {
  }

  @Test
  void shouldCreateNewHold() {
    // given
    CreateHoldRequest request = createHoldRequest(123L, new BigDecimal("100.00"));
    Hold savedHold = createHold(456L, 123L, HoldStatus.AUTHORIZED);

    when(holdRepository.findByTransactionId(123L)).thenReturn(Optional.empty());
    when(holdRepository.save(any(Hold.class))).thenReturn(savedHold);

    // when
    CreateHoldResponse response = holdService.createHold(request);

    // then
    ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
    verify(holdRepository, times(1)).save(holdCaptor.capture());
    verify(outboxEventService, times(1)).saveEvent(eq("hold.created"), eq(456L), anyString());

    Hold capturedHold = holdCaptor.getValue();
    assertThat(capturedHold.getTransactionId()).isEqualTo(123L);
    assertThat(capturedHold.getAmount()).isEqualTo(new BigDecimal("100.00"));
    assertThat(capturedHold.getStatus()).isEqualTo(HoldStatus.AUTHORIZED);
    assertThat(capturedHold.getExpiresAt()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));

    assertThat(response.getHoldId()).isEqualTo(456L);
    assertThat(response.getStatus()).isEqualTo(HoldStatus.AUTHORIZED);
  }

  @Test
  void shouldReturnExistingHoldForIdempotency() {
    // given
    CreateHoldRequest request = createHoldRequest(123L, new BigDecimal("100.00"));
    Hold existingHold = createHold(456L, 123L, HoldStatus.AUTHORIZED);

    when(holdRepository.findByTransactionId(123L)).thenReturn(Optional.of(existingHold));

    // when
    CreateHoldResponse response = holdService.createHold(request);

    // then
    verify(holdRepository, times(0)).save(any()); // Should not save new hold
    verify(outboxEventService, times(0)).saveEvent(anyString(), any(),
        anyString()); // Should not create event

    assertThat(response.getHoldId()).isEqualTo(456L);
    assertThat(response.getStatus()).isEqualTo(HoldStatus.AUTHORIZED);
  }

  @Test
  void shouldRejectHighAmountTransactionForFraud() {
    // given
    CreateHoldRequest request = createHoldRequest(123L,
        new BigDecimal("15000.00")); // Exceeds fraud limit

    when(holdRepository.findByTransactionId(123L)).thenReturn(Optional.empty());

    // when & then
    assertThrows(IllegalArgumentException.class, () -> {
      holdService.createHold(request);
    });

    verify(holdRepository, times(0)).save(any());
    verify(outboxEventService, times(0)).saveEvent(anyString(), any(), anyString());
  }

  @Test
  void shouldExpireHolds() {
    // given
    Instant currentTime = Instant.now();
    Hold expiredHold1 = createHold(456L, 123L, HoldStatus.AUTHORIZED);
    Hold expiredHold2 = createHold(789L, 456L, HoldStatus.AUTHORIZED);
    List<Hold> expiredHolds = Arrays.asList(expiredHold1, expiredHold2);

    when(holdRepository.findExpiredHolds(eq(HoldStatus.AUTHORIZED), any(Instant.class))).thenReturn(
        expiredHolds);
    when(holdRepository.save(any(Hold.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    holdService.expireHolds();

    // then
    verify(holdRepository, times(2)).save(any(Hold.class));
    verify(outboxEventService, times(2)).saveEvent(eq("hold.expired"), any(), anyString());

    assertThat(expiredHold1.getStatus()).isEqualTo(HoldStatus.EXPIRED);
    assertThat(expiredHold2.getStatus()).isEqualTo(HoldStatus.EXPIRED);
  }

  @Test
  void shouldHandleNoExpiredHolds() {
    // given
    when(holdRepository.findExpiredHolds(eq(HoldStatus.AUTHORIZED), any(Instant.class))).thenReturn(
        List.of());

    // when
    holdService.expireHolds();

    // then
    verify(holdRepository, times(0)).save(any());
    verify(outboxEventService, times(0)).saveEvent(anyString(), any(), anyString());
  }

  private CreateHoldRequest createHoldRequest(Long transactionId, BigDecimal amount) {
    return CreateHoldRequest.builder().transactionId(transactionId).issuerAccountId(1L)
        .merchantAccountId(2L).amount(amount).currency("USD").build();
  }

  private Hold createHold(Long holdId, Long transactionId, HoldStatus status) {
    return Hold.builder().holdId(holdId).transactionId(transactionId).accountId(1L)
        .amount(new BigDecimal("100.00")).status(status)
        .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)).build();
  }
}