package com.creditx.hold.repository;

import com.creditx.hold.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

  boolean existsByEventId(String eventId);

  boolean existsByPayloadHash(String payloadHash);
}
