package com.creditx.hold.controller;

import com.creditx.hold.dto.CreateHoldRequest;
import com.creditx.hold.dto.CreateHoldResponse;
import com.creditx.hold.service.HoldService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holds")
@RequiredArgsConstructor
@Slf4j
public class HoldController {

  private final HoldService holdService;

  @PostMapping
  @Operation(summary = "Create a hold", description = "Creates a new hold (authorization) on a payer account", tags = {
      "internal"})
  public ResponseEntity<CreateHoldResponse> createHold(
      @Validated @RequestBody CreateHoldRequest request) {
    log.info("Creating hold for transaction: {}, issuer: {}, merchant: {}, amount: {}",
        request.getTransactionId(), request.getIssuerAccountId(), request.getMerchantAccountId(),
        request.getAmount());
    var response = holdService.createHold(request);
    log.info("Hold created with ID: {}, status: {}", response.getHoldId(), response.getStatus());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
    log.error("Invalid request: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
  }
}
