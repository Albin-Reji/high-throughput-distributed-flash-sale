package com.aegis.payment_service.controller;

import com.aegis.payment_service.config.SecurityConfig;
import com.aegis.payment_service.exception.GlobalExceptionHandler;
import com.aegis.payment_service.exception.InvalidWebhookSignatureException;
import com.aegis.payment_service.security.KeycloakJwtAuthenticationConverter;
import com.aegis.payment_service.security.SecurityAccessDeniedHandler;
import com.aegis.payment_service.security.SecurityAuthenticationEntryPoint;
import com.aegis.payment_service.service.RazorpayWebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RazorpayWebhookController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        KeycloakJwtAuthenticationConverter.class,
        SecurityAuthenticationEntryPoint.class,
        SecurityAccessDeniedHandler.class
})
@DisplayName("RazorpayWebhookController Web Layer Tests")
class RazorpayWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RazorpayWebhookService razorpayWebhookService;

    @Test
    @DisplayName("POST /api/v1/payments/webhooks/razorpay - returns 200 OK when signature is valid")
    void shouldReturn200WhenSignatureIsValid() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";
        String signature = "valid_hex_signature";

        doNothing().when(razorpayWebhookService).processWebhook(eq(payload), eq(signature));

        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhooks/razorpay - returns 400 Bad Request when signature is invalid")
    void shouldReturn400WhenSignatureIsInvalid() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";
        String signature = "tampered_signature";

        doThrow(new InvalidWebhookSignatureException("Signature does not match payload"))
                .when(razorpayWebhookService).processWebhook(eq(payload), eq(signature));

        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature: Signature does not match payload"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhooks/razorpay - returns 400 Bad Request when X-Razorpay-Signature header is missing")
    void shouldReturn400WhenSignatureHeaderIsMissing() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";

        doThrow(new InvalidWebhookSignatureException("Missing X-Razorpay-Signature header"))
                .when(razorpayWebhookService).processWebhook(eq(payload), eq(null));

        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature: Missing X-Razorpay-Signature header"));
    }
}
