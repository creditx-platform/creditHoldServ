package com.creditx.hold.tracing;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionSpanTagger {

  private final Tracer tracer;

  public void tagTransactionId(Long transactionId) {
    if (transactionId == null) {
      return;
    }
    try {
      var span = tracer.currentSpan();
      if (span != null) {
        span.tag("transactionId", String.valueOf(transactionId));
        if (log.isDebugEnabled()) {
          log.debug("Tagged span with transactionId={}", transactionId);
        }
      }
    } catch (Exception e) {
      log.trace("Failed to tag span: {}", e.getMessage());
    }
  }
}
