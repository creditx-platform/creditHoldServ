package com.creditx.hold.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHoldRequest {

  @NotNull
  private Long transactionId;
  @NotNull
  private Long issuerAccountId;
  @NotNull
  private Long merchantAccountId;
  @NotNull
  @Min(1)
  private BigDecimal amount;
  @Size(min = 3, max = 3)
  @Builder.Default
  private String currency = "USD";
}
