package com.creditx.hold.scheduler;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.creditx.hold.service.HoldService;

@ExtendWith(MockitoExtension.class)
class HoldExpirySchedulerTest {

    @Mock
    private HoldService holdService;

    @InjectMocks
    private HoldExpiryScheduler holdExpiryScheduler;

    @BeforeEach
    void setup() {
    }

    @Test
    void shouldExpireHolds() {
        // when
        holdExpiryScheduler.expireHolds();

        // then
        verify(holdService, times(1)).expireHolds();
    }

    @Test
    void shouldHandleExceptionDuringHoldExpiry() {
        // given
        doThrow(new RuntimeException("Database connection failed")).when(holdService).expireHolds();

        // when
        holdExpiryScheduler.expireHolds();

        // then
        verify(holdService, times(1)).expireHolds();
        // The scheduler should log the error but not throw it (fire and forget pattern)
    }
}