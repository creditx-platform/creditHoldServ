package com.creditx.hold.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditx.hold.model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long>{
    
}
