package com.creditx.hold.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.creditx.hold.dto.TransactionAuthorizedEvent;
import com.creditx.hold.service.TransactionEventService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final TransactionEventService transactionEventService;
    private final Tracer tracer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public Consumer<Message<String>> transactionAuthorized() {
        return message -> {
            String payload = message.getPayload();
            String traceId = (String) message.getHeaders().get("X-Trace-Id");
            
            Span span = tracer.nextSpan()
                    .name("transaction-authorized-listener")
                    .tag("service", "creditHoldServ")
                    .tag("event.type", "transaction.authorized");
                    
            if (traceId != null) {
                span.tag("trace.parent.id", traceId);
            }
            
            try {
                span.start();
                log.info("Received transaction.authorized event: {}", payload);
                TransactionAuthorizedEvent event = objectMapper.readValue(payload, TransactionAuthorizedEvent.class);
                
                // Validate that the event has a holdId - skip events without holdId
                if (event.getHoldId() == null) {
                    log.warn("Skipping transaction.authorized event without holdId for transaction: {} - payload: {}", 
                            event.getTransactionId(), payload);
                    return;
                }
                
                transactionEventService.processTransactionAuthorized(event);
                log.info("Successfully processed transaction.authorized for hold: {}", event.getHoldId());
            } catch (Exception e) {
                span.tag("error", e.getMessage());
                log.error("Failed to process transaction.authorized event: {}", payload, e);
                throw new RuntimeException("Failed to process transaction.authorized event", e);
            } finally {
                span.end();
            }
        };
    }
}
