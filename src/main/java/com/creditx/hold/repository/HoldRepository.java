package com.creditx.hold.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.creditx.hold.model.Hold;
import com.creditx.hold.model.HoldStatus;

public interface HoldRepository extends JpaRepository<Hold, Long> {
    Optional<Hold> findByTransactionId(Long transactionId);
    
    @Query("SELECT h FROM Hold h WHERE h.status = :status AND h.expiresAt < :currentTime")
    List<Hold> findExpiredHolds(@Param("status") HoldStatus status, @Param("currentTime") Instant currentTime);
}
