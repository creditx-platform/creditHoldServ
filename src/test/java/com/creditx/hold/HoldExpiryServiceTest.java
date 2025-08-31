package com.creditx.hold;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.creditx.hold.model.Hold;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.repository.HoldRepository;
import com.creditx.hold.service.OutboxEventService;
import com.creditx.hold.service.impl.HoldServiceImpl;

@ExtendWith(MockitoExtension.class)
class HoldExpiryServiceTest {

    @Mock
    private HoldRepository holdRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private HoldServiceImpl holdService;

    @Test
    void expireHolds_shouldExpireAuthorizontHoldsAndPublishEvents() {
        // Given
        Hold expiredHold = Hold.builder()
                .holdId(1L)
                .transactionId(100L)
                .accountId(200L)
                .amount(new BigDecimal("50.00"))
                .status(HoldStatus.AUTHORIZED)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(holdRepository.findExpiredHolds(eq(HoldStatus.AUTHORIZED), any(Instant.class)))
                .thenReturn(List.of(expiredHold));

        // When
        holdService.expireHolds();

        // Then
        verify(holdRepository).save(expiredHold);
        verify(outboxEventService).saveEvent(eq("hold.expired"), eq(1L), any(String.class));
        
        // Verify the hold status was updated
        assert expiredHold.getStatus() == HoldStatus.EXPIRED;
    }
}
