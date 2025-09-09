package com.creditx.hold.service;

import com.creditx.hold.model.OutboxEvent;
import java.util.List;

public interface OutboxEventService {

  OutboxEvent saveEvent(String eventType, Long aggregateId, String payload);

  List<OutboxEvent> fetchPendingEvents(int limit);

  void markAsPublished(OutboxEvent event);

  void markAsFailed(OutboxEvent event);
}