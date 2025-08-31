package com.creditx.hold.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import com.creditx.hold.dto.CreateHoldRequest;
import com.creditx.hold.dto.CreateHoldResponse;
import com.creditx.hold.model.Hold;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.repository.HoldRepository;
import com.creditx.hold.service.HoldService;
import com.creditx.hold.service.OutboxEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@Service
@Slf4j
public class HoldServiceImpl implements HoldService {

    private final HoldRepository holdRepository;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;

    public HoldServiceImpl(HoldRepository holdRepository, OutboxEventService outboxEventService) {
        this.holdRepository = holdRepository;
        this.outboxEventService = outboxEventService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // This enables JSR310 module for Instant serialization
    }

    @Override
    @Transactional
    public CreateHoldResponse createHold(CreateHoldRequest request) {
        // Check for idempotency - if hold already exists for this transaction
        var existingHold = holdRepository.findByTransactionId(request.getTransactionId());
        if (existingHold.isPresent()) {
            Hold hold = existingHold.get();
            return CreateHoldResponse.builder()
                    .holdId(hold.getHoldId())
                    .status(hold.getStatus())
                    .build();
        }

        // Run fraud checks
        validateFraud(request);

        // Create hold with 7-day expiration
        Hold hold = Hold.builder()
                .transactionId(request.getTransactionId())
                .accountId(request.getIssuerAccountId())
                .amount(request.getAmount())
                .status(HoldStatus.AUTHORIZED)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        hold = holdRepository.save(hold);

        // Create outbox event for hold.created
        recordHoldCreatedEvent(hold, request);

        return CreateHoldResponse.builder()
                .holdId(hold.getHoldId())
                .status(hold.getStatus())
                .build();
    }

    private void validateFraud(CreateHoldRequest request) {
        // TODO: Implement a fraud rulebook
        // Simple fraud rule - reject if amount is larger than 10000
        if (request.getAmount().compareTo(new BigDecimal("10000.00")) > 0) {
            throw new IllegalArgumentException("Transaction amount exceeds fraud limit");
        }
    }

    private void recordHoldCreatedEvent(Hold hold, CreateHoldRequest request) {
        var payload = new HoldCreatedPayload(
                hold.getHoldId(),
                request.getTransactionId(),
                request.getIssuerAccountId(),
                request.getMerchantAccountId(),
                request.getAmount(),
                request.getCurrency(),
                hold.getStatus().toString(),
                hold.getExpiresAt()
        );

        try {
            outboxEventService.saveEvent(
                    "hold.created",
                    hold.getHoldId(),
                    objectMapper.writeValueAsString(payload)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize hold created event payload", e);
        }
    }

    @Override
    @Transactional
    public void expireHolds() {
        Instant currentTime = Instant.now();
        List<Hold> expiredHolds = holdRepository.findExpiredHolds(HoldStatus.AUTHORIZED, currentTime);
        
        log.info("Found {} expired holds to process", expiredHolds.size());
        
        for (Hold hold : expiredHolds) {
            try {
                // Update hold status to EXPIRED
                hold.setStatus(HoldStatus.EXPIRED);
                holdRepository.save(hold);
                
                // Publish hold.expired outbox event
                recordHoldExpiredEvent(hold);
                
                log.info("Successfully expired hold with ID: {}", hold.getHoldId());
            } catch (Exception e) {
                log.error("Failed to expire hold with ID: {}", hold.getHoldId(), e);
            }
        }
    }

    private void recordHoldExpiredEvent(Hold hold) {
        var payload = new HoldExpiredPayload(
                hold.getHoldId(),
                hold.getTransactionId(),
                hold.getAccountId(),
                hold.getAmount(),
                hold.getStatus().toString(),
                hold.getExpiresAt()
        );

        try {
            outboxEventService.saveEvent(
                    "hold.expired",
                    hold.getHoldId(),
                    objectMapper.writeValueAsString(payload)
            );
            log.debug("Created hold.expired outbox event for hold ID: {}", hold.getHoldId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize hold expired event payload", e);
        }
    }

    // Simple record for JSON serialization
    private record HoldCreatedPayload(
            Long holdId,
            Long transactionId,
            Long issuerAccountId,
            Long merchantAccountId,
            BigDecimal amount,
            String currency,
            String status,
            Instant expiresAt
    ) {}

    // Simple record for JSON serialization
    private record HoldExpiredPayload(
            Long holdId,
            Long transactionId,
            Long accountId,
            BigDecimal amount,
            String status,
            Instant expiresAt
    ) {}
}
