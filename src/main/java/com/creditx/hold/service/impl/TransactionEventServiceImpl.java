package com.creditx.hold.service.impl;

import com.creditx.hold.dto.TransactionAuthorizedEvent;
import com.creditx.hold.dto.TransactionFailedEvent;
import com.creditx.hold.dto.TransactionPostedEvent;
import com.creditx.hold.model.Hold;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.repository.HoldRepository;
import com.creditx.hold.service.ProcessedEventService;
import com.creditx.hold.service.TransactionEventService;
import com.creditx.hold.tracing.TransactionSpanTagger;
import com.creditx.hold.util.EventIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventServiceImpl implements TransactionEventService {

  private final HoldRepository holdRepository;
  private final ProcessedEventService processedEventService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final TransactionSpanTagger transactionSpanTagger;

  @Override
  @Transactional
  public void processTransactionAuthorized(TransactionAuthorizedEvent event) {
    // Tag current span for trace correlation
    transactionSpanTagger.tagTransactionId(event.getTransactionId());

    // Generate unique event ID for deduplication
    String eventId = EventIdGenerator.generateEventId("transaction.authorized",
        event.getTransactionId());

    // Check if event has already been processed
    if (processedEventService.isEventProcessed(eventId)) {
      log.info("Event {} has already been processed, skipping", eventId);
      return;
    }

    try {
      // Generate payload hash for additional deduplication
      String payload = objectMapper.writeValueAsString(event);
      String payloadHash = EventIdGenerator.generatePayloadHash(payload);

      // Check if payload has already been processed
      if (processedEventService.isPayloadProcessed(payloadHash)) {
        log.info("Payload with hash {} has already been processed, skipping", payloadHash);
        return;
      }

      // Find the hold by holdId and update status to CAPTURED
      Hold hold = holdRepository.findById(event.getHoldId())
          .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + event.getHoldId()));

      // Update hold status to CAPTURED
      hold.setStatus(HoldStatus.CAPTURED);
      holdRepository.save(hold);

      // Mark event as processed
      processedEventService.markEventAsProcessed(eventId, payloadHash, "SUCCESS");
      log.info("Successfully processed transaction.authorized event for transaction: {}",
          event.getTransactionId());

    } catch (JsonProcessingException e) {
      log.error("Failed to serialize event payload for transaction: {}", event.getTransactionId(),
          e);
      processedEventService.markEventAsProcessed(eventId, "", "FAILED");
      throw new RuntimeException("Failed to serialize event payload", e);
    } catch (Exception e) {
      log.error("Failed to process transaction.authorized event for transaction: {}",
          event.getTransactionId(), e);
      // Mark event as processed with failed status
      processedEventService.markEventAsProcessed(eventId, "", "FAILED");
      throw e;
    }
  }

  @Override
  @Transactional
  public void processTransactionPosted(TransactionPostedEvent event) {
    // Tag current span for trace correlation
    transactionSpanTagger.tagTransactionId(event.getTransactionId());

    // Generate unique event ID for deduplication
    String eventId = EventIdGenerator.generateEventId("transaction.posted",
        event.getTransactionId());

    // Check if event has already been processed
    if (processedEventService.isEventProcessed(eventId)) {
      log.info("Event {} has already been processed, skipping", eventId);
      return;
    }

    try {
      // Generate payload hash for additional deduplication
      String payload = objectMapper.writeValueAsString(event);
      String payloadHash = EventIdGenerator.generatePayloadHash(payload);

      // Check if payload has already been processed
      if (processedEventService.isPayloadProcessed(payloadHash)) {
        log.info("Payload with hash {} has already been processed, skipping", payloadHash);
        return;
      }

      // Find the hold by holdId and update status to CAPTURED (if not already)
      Hold hold = holdRepository.findById(event.getHoldId())
          .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + event.getHoldId()));

      // Only update if hold is not already CAPTURED
      if (hold.getStatus() != HoldStatus.CAPTURED) {
        hold.setStatus(HoldStatus.CAPTURED);
        holdRepository.save(hold);
        log.info("Updated hold {} status to CAPTURED", event.getHoldId());
      } else {
        log.info("Hold {} is already CAPTURED, no update needed", event.getHoldId());
      }

      // Mark event as processed
      processedEventService.markEventAsProcessed(eventId, payloadHash, "SUCCESS");
      log.info("Successfully processed transaction.posted event for transaction: {}",
          event.getTransactionId());

    } catch (JsonProcessingException e) {
      log.error("Failed to serialize event payload for transaction: {}", event.getTransactionId(),
          e);
      processedEventService.markEventAsProcessed(eventId, "", "FAILED");
      throw new RuntimeException("Failed to serialize event payload", e);
    } catch (Exception e) {
      log.error("Failed to process transaction.posted event for transaction: {}",
          event.getTransactionId(), e);
      // Mark event as processed with failed status
      processedEventService.markEventAsProcessed(eventId, "", "FAILED");
      throw e;
    }
  }

  @Override
  @Transactional
  public void processTransactionFailed(TransactionFailedEvent event) {
    // Tag current span for trace correlation
    transactionSpanTagger.tagTransactionId(event.getTransactionId());

    // Generate unique event ID for deduplication
    String eventId = EventIdGenerator.generateEventId("transaction.failed",
        event.getTransactionId());

    // Check if event has already been processed
    if (processedEventService.isEventProcessed(eventId)) {
      log.info("Event {} has already been processed, skipping", eventId);
      return;
    }

    try {
      // Generate payload hash for additional deduplication
      String payload = objectMapper.writeValueAsString(event);
      String payloadHash = EventIdGenerator.generatePayloadHash(payload);

      // Check if payload has already been processed
      if (processedEventService.isPayloadProcessed(payloadHash)) {
        log.info("Payload with hash {} has already been processed, skipping", payloadHash);
        return;
      }

      // Find the hold by holdId and update status to VOIDED (if not already)
      Hold hold = holdRepository.findById(event.getHoldId())
          .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + event.getHoldId()));

      // Only update if hold is not already VOIDED or EXPIRED
      if (hold.getStatus() != HoldStatus.VOIDED && hold.getStatus() != HoldStatus.EXPIRED) {
        hold.setStatus(HoldStatus.VOIDED);
        holdRepository.save(hold);
        log.info("Updated hold {} status to VOIDED due to transaction failure", event.getHoldId());
      } else {
        log.info("Hold {} is already in final state {}, no update needed", event.getHoldId(),
            hold.getStatus());
      }

      // Mark event as processed
      processedEventService.markEventAsProcessed(eventId, payloadHash, "SUCCESS");
      log.info("Successfully processed transaction.failed event for transaction: {}",
          event.getTransactionId());

    } catch (JsonProcessingException e) {
      log.error("Failed to serialize event payload for transaction: {}", event.getTransactionId(),
          e);
      processedEventService.markEventAsProcessed(eventId, "", "FAILED");
      throw new RuntimeException("Failed to serialize event payload", e);
    } catch (Exception e) {
      log.error("Failed to process transaction.failed event for transaction: {}",
          event.getTransactionId(), e);
      // Mark event as processed with failed status
      processedEventService.markEventAsProcessed(eventId, "", "FAILED");
      throw e;
    }
  }
}
