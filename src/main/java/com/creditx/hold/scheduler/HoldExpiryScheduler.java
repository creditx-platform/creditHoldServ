package com.creditx.hold.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.creditx.hold.service.HoldService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoldExpiryScheduler {
    
    private final HoldService holdService;

    @Scheduled(fixedDelayString = "${app.hold.expiry-check-interval:300000}") // Default to 5 minutes
    public void expireHolds() {
        log.debug("Starting hold expiry check");
        try {
            holdService.expireHolds();
        } catch (Exception e) {
            log.error("Error occurred during hold expiry processing", e);
        }
        log.debug("Completed hold expiry check");
    }
}
