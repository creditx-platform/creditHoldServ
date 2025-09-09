package com.creditx.hold.dto;

import jakarta.validation.constraints.NotNull;
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

  @NotNull
  private Long transactionId;
  @NotNull
  private Long holdId;
  @NotNull
  private Long issuerAccountId;
  @NotNull
  private Long merchantAccountId;
  @NotNull
  private BigDecimal amount;
  @NotNull
  private String currency;
  private String status;
}
