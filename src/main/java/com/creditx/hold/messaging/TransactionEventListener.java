package com.creditx.hold.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.creditx.hold.constants.EventTypes;
import com.creditx.hold.dto.TransactionAuthorizedEvent;
import com.creditx.hold.dto.TransactionPostedEvent;
import com.creditx.hold.dto.TransactionFailedEvent;
import com.creditx.hold.service.TransactionEventService;
import com.creditx.hold.util.EventValidationUtils;
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
            
            // Validate event type before processing
            if (!EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_AUTHORIZED)) {
                log.warn("Skipping message with invalid event type. Expected: {}, Headers: {}, Payload: {}", 
                        EventTypes.TRANSACTION_AUTHORIZED, message.getHeaders(), payload);
                return;
            }
            
            Span span = tracer.nextSpan()
                    .name("transaction-authorized-listener")
                    .tag("service", "creditHoldServ")
                    .tag("event.type", EventTypes.TRANSACTION_AUTHORIZED);
                    
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

    @Bean
    public Consumer<Message<String>> transactionPosted() {
        return message -> {
            String payload = message.getPayload();
            String traceId = (String) message.getHeaders().get("X-Trace-Id");
            
            // Validate event type before processing
            if (!EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_POSTED)) {
                log.warn("Skipping message with invalid event type. Expected: {}, Headers: {}, Payload: {}", 
                        EventTypes.TRANSACTION_POSTED, message.getHeaders(), payload);
                return;
            }
            
            Span span = tracer.nextSpan()
                    .name("transaction-posted-listener")
                    .tag("service", "creditHoldServ")
                    .tag("event.type", EventTypes.TRANSACTION_POSTED);
                    
            if (traceId != null) {
                span.tag("trace.parent.id", traceId);
            }
            
            try {
                span.start();
                log.info("Received transaction.posted event: {}", payload);
                TransactionPostedEvent event = objectMapper.readValue(payload, TransactionPostedEvent.class);
                
                // Validate that the event has a holdId
                if (event.getHoldId() == null) {
                    log.warn("Skipping transaction.posted event without holdId for transaction: {} - payload: {}", 
                            event.getTransactionId(), payload);
                    return;
                }
                
                transactionEventService.processTransactionPosted(event);
                log.info("Successfully processed transaction.posted for hold: {}", event.getHoldId());
            } catch (Exception e) {
                span.tag("error", e.getMessage());
                log.error("Failed to process transaction.posted event: {}", payload, e);
                throw new RuntimeException("Failed to process transaction.posted event", e);
            } finally {
                span.end();
            }
        };
    }

    @Bean
    public Consumer<Message<String>> transactionFailed() {
        return message -> {
            String payload = message.getPayload();
            String traceId = (String) message.getHeaders().get("X-Trace-Id");
            
            // Validate event type before processing
            if (!EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_FAILED)) {
                log.warn("Skipping message with invalid event type. Expected: {}, Headers: {}, Payload: {}", 
                        EventTypes.TRANSACTION_FAILED, message.getHeaders(), payload);
                return;
            }
            
            Span span = tracer.nextSpan()
                    .name("transaction-failed-listener")
                    .tag("service", "creditHoldServ")
                    .tag("event.type", EventTypes.TRANSACTION_FAILED);
                    
            if (traceId != null) {
                span.tag("trace.parent.id", traceId);
            }
            
            try {
                span.start();
                log.info("Received transaction.failed event: {}", payload);
                TransactionFailedEvent event = objectMapper.readValue(payload, TransactionFailedEvent.class);
                
                // Validate that the event has a holdId
                if (event.getHoldId() == null) {
                    log.warn("Skipping transaction.failed event without holdId for transaction: {} - payload: {}", 
                            event.getTransactionId(), payload);
                    return;
                }
                
                transactionEventService.processTransactionFailed(event);
                log.info("Successfully processed transaction.failed for hold: {}", event.getHoldId());
            } catch (Exception e) {
                span.tag("error", e.getMessage());
                log.error("Failed to process transaction.failed event: {}", payload, e);
                throw new RuntimeException("Failed to process transaction.failed event", e);
            } finally {
                span.end();
            }
        };
    }
}
