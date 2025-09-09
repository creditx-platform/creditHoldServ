package com.creditx.hold.tracing;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionSpanTaggerTest {

  @Mock
  Tracer tracer;
  @Mock
  Span span;

  @InjectMocks
  TransactionSpanTagger tagger;

  @Test
  void tagsWhenSpanPresent() {
    when(tracer.currentSpan()).thenReturn(span);
    tagger.tagTransactionId(5L);
    verify(span, times(1)).tag("transactionId", "5");
  }
}
