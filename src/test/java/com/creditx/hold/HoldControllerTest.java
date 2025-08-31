package com.creditx.hold;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.creditx.hold.controller.HoldController;
import com.creditx.hold.dto.CreateHoldResponse;
import com.creditx.hold.model.HoldStatus;
import com.creditx.hold.service.HoldService;

import static org.mockito.BDDMockito.given;

@WebMvcTest(controllers = HoldController.class)
class HoldControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HoldService holdService;

    @Test
    void createHold_success() throws Exception {
        given(holdService.createHold(any())).willReturn(
                CreateHoldResponse.builder()
                        .holdId(123L)
                        .status(HoldStatus.AUTHORIZED)
                        .build());

        String requestBody = """
            {
                "transactionId": 100,
                "issuerAccountId": 1,
                "merchantAccountId": 2,
                "amount": 250.00,
                "currency": "USD"
            }
            """;

        mockMvc.perform(post("/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holdId").value(123))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void createHold_validationError_missingTransactionId() throws Exception {
        String requestBody = """
            {
                "issuerAccountId": 1,
                "merchantAccountId": 2,
                "amount": 250.00,
                "currency": "USD"
            }
            """;

        mockMvc.perform(post("/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createHold_validationError_invalidAmount() throws Exception {
        String requestBody = """
            {
                "transactionId": 100,
                "issuerAccountId": 1,
                "merchantAccountId": 2,
                "amount": -50.00,
                "currency": "USD"
            }
            """;

        mockMvc.perform(post("/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createHold_validationError_invalidCurrency() throws Exception {
        String requestBody = """
            {
                "transactionId": 100,
                "issuerAccountId": 1,
                "merchantAccountId": 2,
                "amount": 250.00,
                "currency": "INVALID"
            }
            """;

        mockMvc.perform(post("/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createHold_fraudException() throws Exception {
        given(holdService.createHold(any())).willThrow(
                new IllegalArgumentException("Transaction amount exceeds fraud limit"));

        String requestBody = """
            {
                "transactionId": 100,
                "issuerAccountId": 1,
                "merchantAccountId": 2,
                "amount": 15000.00,
                "currency": "USD"
            }
            """;

        mockMvc.perform(post("/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
