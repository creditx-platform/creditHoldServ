package com.creditx.hold.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAuthorizedEvent {
    private Long transactionId;
    private Long holdId;
    private Long issuerAccountId;
    private Long merchantAccountId;
    private BigDecimal amount;
    private String currency;
    private String status;
}
