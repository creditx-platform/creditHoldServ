package com.creditx.hold.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditx.hold.model.Hold;

public interface HoldRepository extends JpaRepository<Hold, Long> {
    Optional<Hold> findByTransactionId(Long transactionId);
}
