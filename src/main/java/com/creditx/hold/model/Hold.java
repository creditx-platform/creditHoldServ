package com.creditx.hold.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CHS_HOLDS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hold {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hold_seq_gen")
    @SequenceGenerator(name = "hold_seq_gen", sequenceName = "CHS_HOLD_SEQ", allocationSize = 1)
    @Column(name = "HOLD_ID")
    private Long holdId;

    @Column(name = "TRANSACTION_ID", nullable = false)
    private Long transactionId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "AMOUNT", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private HoldStatus status;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_AT", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

}
