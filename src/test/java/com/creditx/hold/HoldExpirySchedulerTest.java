package com.creditx.hold;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.creditx.hold.scheduler.HoldExpiryScheduler;
import com.creditx.hold.service.HoldService;

@ExtendWith(MockitoExtension.class)
class HoldExpirySchedulerTest {

    @Mock
    private HoldService holdService;

    @InjectMocks
    private HoldExpiryScheduler holdExpiryScheduler;

    @Test
    void expireHolds_shouldCallHoldServiceExpireHolds() {
        // When
        holdExpiryScheduler.expireHolds();

        // Then
        verify(holdService).expireHolds();
    }
}
