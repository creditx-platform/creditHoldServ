package com.creditx.hold.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.creditx.hold.dto.TransactionAuthorizedEvent;
import com.creditx.hold.service.TransactionEventService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final TransactionEventService transactionEventService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public Consumer<String> transactionAuthorized() {
        return payload -> {
            try {
                log.info("Received transaction.authorized event: {}", payload);
                TransactionAuthorizedEvent event = objectMapper.readValue(payload, TransactionAuthorizedEvent.class);
                transactionEventService.processTransactionAuthorized(event);
                log.info("Successfully processed transaction.authorized for hold: {}", event.getHoldId());
            } catch (Exception e) {
                log.error("Failed to process transaction.authorized event: {}", payload, e);
                throw new RuntimeException("Failed to process transaction.authorized event", e);
            }
        };
    }
}
