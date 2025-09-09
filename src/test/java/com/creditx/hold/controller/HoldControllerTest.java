package com.creditx.hold.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creditx.hold.dto.CreateHoldResponse;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.service.HoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HoldController.class)
class HoldControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  HoldService holdService;

  @Test
  void createHold_success() throws Exception {
    given(holdService.createHold(any())).willReturn(
        CreateHoldResponse.builder().holdId(12345L).status(HoldStatus.AUTHORIZED).build());

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 100.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.holdId").value(12345))
        .andExpect(jsonPath("$.status").value("AUTHORIZED"));
  }

  @Test
  void createHold_validationError_missingTransactionId() throws Exception {
    String requestBody = """
        {
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 100.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createHold_validationError_missingIssuerAccountId() throws Exception {
    String requestBody = """
        {
            "transactionId": 999,
            "merchantAccountId": 2,
            "amount": 100.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createHold_validationError_missingMerchantAccountId() throws Exception {
    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "amount": 100.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createHold_validationError_missingAmount() throws Exception {
    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createHold_validationError_invalidAmount() throws Exception {
    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 0.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createHold_validationError_negativeAmount() throws Exception {
    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": -50.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createHold_validationError_invalidCurrency() throws Exception {
    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 100.00,
            "currency": "INVALID"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createHold_validationError_emptyCurrency() throws Exception {
    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 100.00,
            "currency": ""
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createHold_serviceException() throws Exception {
    given(holdService.createHold(any())).willThrow(
        new IllegalArgumentException("Account not found"));

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 100.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest()).andExpect(content().string("Account not found"));
  }

  @Test
  void createHold_insufficientBalanceException() throws Exception {
    given(holdService.createHold(any())).willThrow(
        new IllegalArgumentException("Insufficient available balance"));

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 1000.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Insufficient available balance"));
  }

  @Test
  void createHold_duplicateTransactionException() throws Exception {
    given(holdService.createHold(any())).willThrow(
        new IllegalArgumentException("Hold already exists for transaction: 999"));

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 100.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Hold already exists for transaction: 999"));
  }

  @Test
  void createHold_withDefaultCurrency() throws Exception {
    given(holdService.createHold(any())).willReturn(
        CreateHoldResponse.builder().holdId(12345L).status(HoldStatus.AUTHORIZED).build());

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 100.00
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.holdId").value(12345))
        .andExpect(jsonPath("$.status").value("AUTHORIZED"));
  }

  @Test
  void createHold_withLargeAmount() throws Exception {
    given(holdService.createHold(any())).willReturn(
        CreateHoldResponse.builder().holdId(12345L).status(HoldStatus.AUTHORIZED).build());

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 9999.99,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.holdId").value(12345))
        .andExpect(jsonPath("$.status").value("AUTHORIZED"));
  }

  @Test
  void createHold_withDecimalPrecision() throws Exception {
    given(holdService.createHold(any())).willReturn(
        CreateHoldResponse.builder().holdId(12345L).status(HoldStatus.AUTHORIZED).build());

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 123.456,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.holdId").value(12345))
        .andExpect(jsonPath("$.status").value("AUTHORIZED"));
  }

  @Test
  void createHold_transactionNotFound() throws Exception {
    given(holdService.createHold(any())).willThrow(
        new IllegalArgumentException("Transaction not found: 999"));

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 100.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Transaction not found: 999"));
  }

  @Test
  void createHold_accountMismatch() throws Exception {
    given(holdService.createHold(any())).willThrow(
        new IllegalArgumentException("Account IDs do not match transaction"));

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 99,
            "merchantAccountId": 88,
            "amount": 100.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Account IDs do not match transaction"));
  }

  @Test
  void createHold_amountMismatch() throws Exception {
    given(holdService.createHold(any())).willThrow(
        new IllegalArgumentException("Hold amount does not match transaction amount"));

    String requestBody = """
        {
            "transactionId": 999,
            "issuerAccountId": 1,
            "merchantAccountId": 2,
            "amount": 200.00,
            "currency": "USD"
        }
        """;

    mockMvc.perform(post("/api/holds").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Hold amount does not match transaction amount"));
  }
}